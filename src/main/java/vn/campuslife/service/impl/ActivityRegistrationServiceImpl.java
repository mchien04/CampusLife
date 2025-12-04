package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.Role;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.*;
import vn.campuslife.repository.*;
import vn.campuslife.service.ActivityRegistrationService;
import vn.campuslife.service.NotificationService;
import vn.campuslife.util.TicketCodeUtils;
import vn.campuslife.enumeration.NotificationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityRegistrationServiceImpl implements ActivityRegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityRegistrationServiceImpl.class);

    private final ActivityRegistrationRepository registrationRepository;
    private final ActivityParticipationRepository participationRepository;
    private final ActivityRepository activityRepository;
    private final StudentRepository studentRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final vn.campuslife.service.ActivitySeriesService activitySeriesService;

    @Override
    @Transactional
    public Response registerForActivity(ActivityRegistrationRequest request, Long studentId) {
        try {
            // 1) Kiểm tra activity
            Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(request.getActivityId());
            if (activityOpt.isEmpty()) {
                return new Response(false, "Activity not found", null);
            }
            Activity activity = activityOpt.get();

            // 2) Block manual registration for important/mandatory activities (they are
            // auto-registered)
            if (activity.isImportant() || activity.isMandatoryForFacultyStudents()) {
                return new Response(false,
                        "This activity is automatically registered for eligible students. Manual registration is not allowed.",
                        null);
            }

            // 3) Block registration if activity is draft
            if (activity.isDraft()) {
                return new Response(false, "Activity is not published yet", null);
            }

            // 4) Kiểm tra student
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }
            Student student = studentOpt.get();

            // 5) Đã đăng ký chưa?
            if (registrationRepository.existsByActivityIdAndStudentId(request.getActivityId(), studentId)) {
                return new Response(false, "Already registered for this activity", null);
            }

            // 6) Thời gian mở/đóng đăng ký
            if (activity.getRegistrationDeadline() != null &&
                    LocalDateTime.now().isAfter(activity.getRegistrationDeadline())) {
                return new Response(false, "Registration deadline has passed", null);
            }
            if (activity.getRegistrationStartDate() != null &&
                    LocalDateTime.now().isBefore(activity.getRegistrationStartDate())) {
                return new Response(false, "Registration is not yet open", null);
            }

            // 7) Kiểm tra số lượng vé (nếu giới hạn theo APPROVED)
            if (activity.getTicketQuantity() != null) {
                Long current = registrationRepository
                        .countByActivityIdAndStatus(request.getActivityId(), RegistrationStatus.APPROVED);
                if (current >= activity.getTicketQuantity()) {
                    return new Response(false, "Activity is full", null);
                }
            }

            // 8) Tạo đăng ký + MÃ VÉ
            ActivityRegistration registration = new ActivityRegistration();
            registration.setActivity(activity);
            registration.setStudent(student);
            registration.setRegisteredDate(LocalDateTime.now());
            // Nếu activity thuộc series, lưu luôn seriesId để FE dễ kiểm tra đăng ký chuỗi
            if (activity.getSeriesId() != null) {
                registration.setSeriesId(activity.getSeriesId());
            }
            // Auto-approve if activity does not require approval
            registration.setStatus(
                    activity.isRequiresApproval() ? RegistrationStatus.PENDING : RegistrationStatus.APPROVED);

            String code;
            int attempts = 0;
            do {
                code = TicketCodeUtils.newTicketCode();
                attempts++;
            } while (registrationRepository.existsByTicketCode(code) && attempts < 3);
            registration.setTicketCode(code);

            ActivityRegistration saved = registrationRepository.save(registration);
            ActivityRegistrationResponse payload = toRegistrationResponse(saved);

            // Send notification to student
            try {
                Long userId = student.getUser().getId();
                String title;
                String content;
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("activityId", activity.getId());
                metadata.put("activityName", activity.getName());
                metadata.put("registrationId", saved.getId());
                metadata.put("ticketCode", saved.getTicketCode());

                if (saved.getStatus() == RegistrationStatus.APPROVED) {
                    title = "Đăng ký thành công";
                    content = String.format("Đăng ký thành công cho sự kiện: %s", activity.getName());
                } else {
                    title = "Đăng ký đang chờ duyệt";
                    content = String.format("Đăng ký của bạn đang chờ duyệt: %s", activity.getName());
                }

                notificationService.sendNotification(
                        userId,
                        title,
                        content,
                        NotificationType.ACTIVITY_REGISTRATION,
                        "/activities/" + activity.getId(),
                        metadata);
                logger.info("Sent registration notification to user {} for activity {}", userId, activity.getId());
            } catch (Exception e) {
                logger.error("Failed to send registration notification: {}", e.getMessage(), e);
                // Don't fail registration if notification fails
            }

            return new Response(true, "Successfully registered for activity", payload);
        } catch (Exception e) {
            logger.error("Failed to register for activity: {}", e.getMessage(), e);
            return new Response(false, "Failed to register due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response cancelRegistration(Long activityId, Long studentId) {
        try {
            Optional<ActivityRegistration> registrationOpt = registrationRepository
                    .findByActivityIdAndStudentId(activityId, studentId);

            if (registrationOpt.isEmpty()) {
                return new Response(false, "Registration not found", null);
            }

            ActivityRegistration registration = registrationOpt.get();

            // Check if can cancel (only PENDING can be cancelled, APPROVED cannot be
            // cancelled)
            if (registration.getStatus() == RegistrationStatus.CANCELLED) {
                return new Response(false, "Registration already cancelled", null);
            }

            if (registration.getStatus() == RegistrationStatus.APPROVED) {
                return new Response(false,
                        "Cannot cancel approved registration. This is an auto-approved registration.", null);
            }

            registration.setStatus(RegistrationStatus.CANCELLED);
            registrationRepository.save(registration);

            return new Response(true, "Registration cancelled successfully", null);
        } catch (Exception e) {
            logger.error("Failed to cancel registration: {}", e.getMessage(), e);
            return new Response(false, "Failed to cancel registration due to server error", null);
        }
    }

    @Override
    public Response getStudentRegistrations(Long studentId) {
        try {
            List<ActivityRegistration> registrations = registrationRepository
                    .findByStudentIdAndStudentIsDeletedFalse(studentId);

            List<ActivityRegistrationResponse> responses = registrations.stream()
                    .map(this::toRegistrationResponse)
                    .collect(Collectors.toList());

            return new Response(true, "Student registrations retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to retrieve student registrations: {}", e.getMessage(), e);
            return new Response(false, "Failed to retrieve registrations due to server error", null);
        }
    }

    @Override
    public Response getActivityRegistrations(Long activityId) {
        try {
            List<ActivityRegistration> registrations = registrationRepository
                    .findByActivityIdAndActivityIsDeletedFalse(activityId);

            List<ActivityRegistrationResponse> responses = registrations.stream()
                    .map(this::toRegistrationResponse)
                    .collect(Collectors.toList());

            return new Response(true, "Activity registrations retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to retrieve activity registrations: {}", e.getMessage(), e);
            return new Response(false, "Failed to retrieve registrations due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response updateRegistrationStatus(Long registrationId, String status) {
        try {
            Optional<ActivityRegistration> registrationOpt = registrationRepository.findById(registrationId);
            if (registrationOpt.isEmpty()) {
                return new Response(false, "Registration not found", null);
            }

            ActivityRegistration registration = registrationOpt.get();
            RegistrationStatus newStatus = RegistrationStatus.valueOf(status.toUpperCase());
            registration.setStatus(newStatus);
            ActivityRegistration savedRegistration = registrationRepository.save(registration);

            if (newStatus == RegistrationStatus.APPROVED) {
                boolean exists = participationRepository.existsByRegistration(savedRegistration);
                if (!exists) {
                    ActivityParticipation participation = new ActivityParticipation();
                    participation.setRegistration(savedRegistration);
                    participation.setParticipationType(ParticipationType.REGISTERED);
                    participation.setPointsEarned(BigDecimal.ZERO);
                    participation.setDate(LocalDateTime.now());
                    participationRepository.save(participation);
                }
            }

            // Send notification to student when status is approved or rejected
            try {
                if (newStatus == RegistrationStatus.APPROVED || newStatus == RegistrationStatus.REJECTED) {
                    Long userId = savedRegistration.getStudent().getUser().getId();
                    Activity activity = savedRegistration.getActivity();
                    String title;
                    String content;
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("activityId", activity.getId());
                    metadata.put("activityName", activity.getName());
                    metadata.put("registrationId", savedRegistration.getId());
                    metadata.put("status", newStatus.toString());

                    if (newStatus == RegistrationStatus.APPROVED) {
                        title = "Đăng ký đã được duyệt";
                        content = String.format("Đăng ký của bạn đã được duyệt: %s", activity.getName());
                    } else {
                        title = "Đăng ký đã bị từ chối";
                        content = String.format("Đăng ký của bạn đã bị từ chối: %s", activity.getName());
                    }

                    notificationService.sendNotification(
                            userId,
                            title,
                            content,
                            NotificationType.ACTIVITY_REGISTRATION,
                            "/activities/" + activity.getId(),
                            metadata);
                    logger.info("Sent status update notification to user {} for registration {}",
                            userId, savedRegistration.getId());
                }
            } catch (Exception e) {
                logger.error("Failed to send status update notification: {}", e.getMessage(), e);
                // Don't fail status update if notification fails
            }

            ActivityRegistrationResponse response = toRegistrationResponse(savedRegistration);
            return new Response(true, "Registration status updated successfully", response);

        } catch (IllegalArgumentException e) {
            return new Response(false, "Invalid status: " + status, null);
        } catch (Exception e) {
            logger.error("Failed to update registration status: {}", e.getMessage(), e);
            return new Response(false, "Failed to update status due to server error", null);
        }
    }

    @Override
    public Response getRegistrationById(Long registrationId) {
        try {
            Optional<ActivityRegistration> registrationOpt = registrationRepository.findById(registrationId);
            if (registrationOpt.isEmpty()) {
                return new Response(false, "Registration not found", null);
            }

            ActivityRegistrationResponse response = toRegistrationResponse(registrationOpt.get());
            return new Response(true, "Registration retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Failed to retrieve registration: {}", e.getMessage(), e);
            return new Response(false, "Failed to retrieve registration due to server error", null);
        }
    }

    @Override
    public Response checkRegistrationStatus(Long activityId, Long studentId) {
        try {
            Optional<ActivityRegistration> registrationOpt = registrationRepository
                    .findByActivityIdAndStudentId(activityId, studentId);

            if (registrationOpt.isEmpty()) {
                return new Response(true, "Not registered", null);
            }

            ActivityRegistrationResponse response = toRegistrationResponse(registrationOpt.get());
            return new Response(true, "Registration status retrieved", response);
        } catch (Exception e) {
            logger.error("Failed to check registration status: {}", e.getMessage(), e);
            return new Response(false, "Failed to check status due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response checkIn(ActivityParticipationRequest request) {
        ActivityRegistration registration;

        // Tìm registration theo ticketCode hoặc studentId
        if (request.getTicketCode() != null && !request.getTicketCode().isBlank()) {
            registration = registrationRepository.findByTicketCode(request.getTicketCode())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ticketCode"));
        } else if (request.getStudentId() != null) {
            registration = registrationRepository.findByStudentIdAndStatus(
                    request.getStudentId(),
                    RegistrationStatus.APPROVED)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đăng ký hợp lệ cho sinh viên này"));
        } else {
            return Response.error("Cần cung cấp ticketCode hoặc studentId");
        }

        // Block check-in if activity is draft
        if (registration.getActivity().isDraft()) {
            return Response.error("Activity is not published yet");
        }

        // Lấy participation đã tạo khi duyệt
        ActivityParticipation participation = participationRepository.findByRegistration(registration)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy participation cho đăng ký này"));

        ParticipationType currentType = participation.getParticipationType();

        // CHECK-IN (lần 1) - từ REGISTERED/APPROVED sang CHECKED_IN
        if (currentType == ParticipationType.REGISTERED) {
            participation.setParticipationType(ParticipationType.CHECKED_IN);
            participation.setCheckInTime(LocalDateTime.now());
            participation.setDate(LocalDateTime.now());
            participationRepository.save(participation);

            ActivityParticipationResponse resp = toParticipationResponse(participation);

            return Response.success("Check-in thành công. Vui lòng check-out khi rời khỏi sự kiện.", resp);
        }

        // CHECK-OUT (lần 2) - từ CHECKED_IN sang CHECKED_OUT → ATTENDED
        else if (currentType == ParticipationType.CHECKED_IN) {
            participation.setParticipationType(ParticipationType.CHECKED_OUT);
            participation.setCheckOutTime(LocalDateTime.now());
            participationRepository.save(participation);

            // Đổi registration status sang ATTENDED
            registration.setStatus(RegistrationStatus.ATTENDED);
            registrationRepository.save(registration);

            // Chuyển participation sang ATTENDED
            participation.setParticipationType(ParticipationType.ATTENDED);
            participationRepository.save(participation);

            // Nếu activity không yêu cầu nộp bài, tự động cập nhật điểm và isCompleted
            Activity activity = registration.getActivity();
            if (!activity.isRequiresSubmission()) {
                participation.setIsCompleted(true);
                participation.setParticipationType(ParticipationType.COMPLETED);
                
                // XỬ LÝ ĐIỂM: Phân biệt activity đơn lẻ, activity trong series, và CHUYEN_DE_DOANH_NGHIEP
                if (activity.getSeriesId() != null) {
                    // Activity trong series → KHÔNG tính điểm từ maxPoints
                    participation.setPointsEarned(BigDecimal.ZERO);
                    participationRepository.save(participation);
                    
                    // Chỉ update series progress (điểm milestone sẽ được tính tự động)
                    try {
                        activitySeriesService.updateStudentProgress(
                                registration.getStudent().getId(), 
                                activity.getId());
                        logger.info("Updated series progress for activity {} in series {}", 
                                activity.getName(), activity.getSeriesId());
                    } catch (Exception e) {
                        logger.warn("Failed to update series progress: {}", e.getMessage());
                        // Không fail check-out nếu update series progress lỗi
                    }
                } else if (activity.getType() == ActivityType.CHUYEN_DE_DOANH_NGHIEP) {
                    // CHUYEN_DE_DOANH_NGHIEP: Dual Score Calculation
                    // Lưu maxPoints vào pointsEarned để dùng cho REN_LUYEN
                    BigDecimal points = activity.getMaxPoints() != null ? activity.getMaxPoints() : BigDecimal.ZERO;
                    participation.setPointsEarned(points);
                    participationRepository.save(participation);
                    
                    try {
                        // CHUYEN_DE: Đếm số buổi (không dùng pointsEarned, chỉ đếm số participation)
                        updateChuyenDeScoreCount(participation);
                        
                        // REN_LUYEN: Cộng điểm từ maxPoints (nếu có)
                        if (activity.getMaxPoints() != null) {
                            updateRenLuyenScoreFromParticipation(participation);
                        }
                        
                        logger.info("Auto-completed CHUYEN_DE_DOANH_NGHIEP participation for activity {}. Count: +1, RL Points: {}",
                                activity.getName(), activity.getMaxPoints());
                    } catch (Exception e) {
                        logger.error("Failed to update dual score after auto-completion: {}", e.getMessage(), e);
                        // Không fail check-out nếu update score lỗi, chỉ log
                    }
                } else {
                    // Activity đơn lẻ khác → tính điểm bình thường từ maxPoints
                    BigDecimal points = activity.getMaxPoints() != null ? activity.getMaxPoints() : BigDecimal.ZERO;
                    participation.setPointsEarned(points);
                    participationRepository.save(participation);

                    // Cập nhật StudentScore (tổng hợp)
                    try {
                        updateStudentScoreFromParticipation(participation);
                        logger.info("Auto-completed participation for activity {} (no submission required). Points: {}",
                                activity.getName(), points);
                    } catch (Exception e) {
                        logger.error("Failed to update student score after auto-completion: {}", e.getMessage(), e);
                        // Không fail check-out nếu update score lỗi, chỉ log
                    }
                }
            }

            ActivityParticipationResponse resp = toParticipationResponse(participation);

            String message = activity.isRequiresSubmission()
                    ? "Check-out thành công. Đã hoàn thành tham gia sự kiện."
                    : "Check-out thành công. Đã hoàn thành tham gia sự kiện và được điểm tự động.";

            return Response.success(message, resp);
        }

        // Đã hoàn thành
        else {
            return Response.error("Đã hoàn thành check-in/check-out trước đó");
        }
    }

    @Override
    @Transactional
    public Response checkInByQrCode(String checkInCode, Long studentId) {
        try {
            // 1. Tìm activity theo checkInCode
            Activity activity = activityRepository.findByCheckInCode(checkInCode)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy activity với mã QR này"));

            // 2. Validate activity
            if (activity.isDraft()) {
                return Response.error("Activity chưa được công bố");
            }

            // 3. Tìm registration của sinh viên cho activity
            ActivityRegistration registration = registrationRepository
                    .findByActivityIdAndStudentId(activity.getId(), studentId)
                    .filter(r -> r.getStatus() == RegistrationStatus.APPROVED)
                    .orElseThrow(() -> new RuntimeException("Bạn chưa đăng ký hoặc chưa được duyệt tham gia activity này"));

            // 4. Tìm participation
            ActivityParticipation participation = participationRepository
                    .findByRegistration(registration)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy participation cho đăng ký này"));

            // 5. Kiểm tra đã check-in chưa
            if (participation.getParticipationType() == ParticipationType.ATTENDED
                    || participation.getParticipationType() == ParticipationType.COMPLETED) {
                return Response.error("Bạn đã điểm danh activity này rồi");
            }

            // 6. Set trực tiếp thành ATTENDED (bỏ qua CHECKED_IN và CHECKED_OUT)
            LocalDateTime now = LocalDateTime.now();
            participation.setParticipationType(ParticipationType.ATTENDED);
            participation.setCheckInTime(now);
            participation.setCheckOutTime(now); // Cùng thời điểm
            participation.setDate(now);
            registration.setStatus(RegistrationStatus.ATTENDED);
            registrationRepository.save(registration);

            // 7. Xử lý điểm (giống check-out logic)
            if (!activity.isRequiresSubmission()) {
                participation.setIsCompleted(true);
                participation.setParticipationType(ParticipationType.COMPLETED);

                // XỬ LÝ ĐIỂM: Phân biệt activity đơn lẻ, activity trong series, và CHUYEN_DE_DOANH_NGHIEP
                if (activity.getSeriesId() != null) {
                    // Activity trong series → KHÔNG tính điểm từ maxPoints
                    participation.setPointsEarned(BigDecimal.ZERO);
                    participationRepository.save(participation);

                    // Chỉ update series progress (điểm milestone sẽ được tính tự động)
                    try {
                        activitySeriesService.updateStudentProgress(
                                registration.getStudent().getId(),
                                activity.getId());
                        logger.info("Updated series progress for activity {} in series {} via QR code",
                                activity.getName(), activity.getSeriesId());
                    } catch (Exception e) {
                        logger.warn("Failed to update series progress: {}", e.getMessage());
                        // Không fail check-in nếu update series progress lỗi
                    }
                } else if (activity.getType() == ActivityType.CHUYEN_DE_DOANH_NGHIEP) {
                    // CHUYEN_DE_DOANH_NGHIEP: Dual Score Calculation
                    BigDecimal points = activity.getMaxPoints() != null ? activity.getMaxPoints() : BigDecimal.ZERO;
                    participation.setPointsEarned(points);
                    participationRepository.save(participation);

                    try {
                        // CHUYEN_DE: Đếm số buổi
                        updateChuyenDeScoreCount(participation);

                        // REN_LUYEN: Cộng điểm từ maxPoints (nếu có)
                        if (activity.getMaxPoints() != null) {
                            updateRenLuyenScoreFromParticipation(participation);
                        }

                        logger.info("Auto-completed CHUYEN_DE_DOANH_NGHIEP participation for activity {} via QR code. Count: +1, RL Points: {}",
                                activity.getName(), activity.getMaxPoints());
                    } catch (Exception e) {
                        logger.error("Failed to update dual score after QR code check-in: {}", e.getMessage(), e);
                    }
                } else {
                    // Activity đơn lẻ khác → tính điểm bình thường từ maxPoints
                    BigDecimal points = activity.getMaxPoints() != null ? activity.getMaxPoints() : BigDecimal.ZERO;
                    participation.setPointsEarned(points);
                    participationRepository.save(participation);

                    // Cập nhật StudentScore
                    try {
                        updateStudentScoreFromParticipation(participation);
                        logger.info("Auto-completed participation for activity {} via QR code. Points: {}",
                                activity.getName(), points);
                    } catch (Exception e) {
                        logger.error("Failed to update student score after QR code check-in: {}", e.getMessage(), e);
                    }
                }
            }

            participationRepository.save(participation);

            ActivityParticipationResponse resp = toParticipationResponse(participation);
            return Response.success("Điểm danh thành công bằng QR code", resp);

        } catch (RuntimeException e) {
            logger.error("Failed to check-in by QR code: {}", e.getMessage());
            return Response.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to check-in by QR code: {}", e.getMessage(), e);
            return Response.error("Failed to check-in by QR code: " + e.getMessage());
        }
    }

    /**
     * Chấm điểm completion (đạt/không đạt)
     */
    @Transactional
    public Response gradeCompletion(Long participationId, boolean isCompleted, String notes) {
        try {
            ActivityParticipation participation = participationRepository
                    .findById(participationId)
                    .orElseThrow(() -> new RuntimeException("Participation not found"));

            // Block grading if activity is draft
            if (participation.getRegistration().getActivity().isDraft()) {
                return Response.error("Activity is not published yet");
            }

            // Kiểm tra đã ATTENDED chưa
            if (participation.getParticipationType() != ParticipationType.ATTENDED) {
                return Response.error("Sinh viên chưa hoàn thành check-in/check-out");
            }

            Activity activity = participation.getRegistration().getActivity();

            // Nếu yêu cầu submission, kiểm tra đã nộp và được chấm chưa
            if (activity.isRequiresSubmission()) {
                // Kiểm tra có TaskSubmission đã được grade
                boolean hasGradedSubmission = checkHasGradedSubmission(
                        participation.getRegistration().getStudent().getId(),
                        activity.getId());

                if (!hasGradedSubmission) {
                    return Response.error("Sinh viên chưa nộp bài hoặc chưa được chấm điểm");
                }
            }

            // Tính điểm
            BigDecimal points;
            if (isCompleted) {
                points = activity.getMaxPoints() != null ? activity.getMaxPoints() : BigDecimal.ZERO;
            } else {
                // Điểm trừ
                BigDecimal penalty = activity.getPenaltyPointsIncomplete() != null
                        ? activity.getPenaltyPointsIncomplete()
                        : BigDecimal.ZERO;
                points = penalty.negate(); // Chuyển thành số âm
            }

            // Cập nhật participation
            participation.setIsCompleted(isCompleted);
            participation.setPointsEarned(points);
            participation.setParticipationType(ParticipationType.COMPLETED);
            participationRepository.save(participation);

            // Cập nhật StudentScore (tổng hợp)
            updateStudentScoreFromParticipation(participation);

            return Response.success("Đã chấm điểm completion", participation);
        } catch (Exception e) {
            logger.error("Failed to grade completion: {}", e.getMessage(), e);
            return Response.error("Failed to grade completion: " + e.getMessage());
        }
    }

    /**
     * Helper method để kiểm tra submission đã được grade
     */
    private boolean checkHasGradedSubmission(Long studentId, Long activityId) {
        // TODO: Implement check logic using TaskSubmissionRepository
        // Tạm thời return true để không block
        return true;
    }

    private ActivityRegistrationResponse toRegistrationResponse(ActivityRegistration r) {
        Activity a = r.getActivity();
        Student s = r.getStudent();
        ActivityRegistrationResponse res = new ActivityRegistrationResponse();
        res.setId(r.getId());
        res.setActivityId(a.getId());
        res.setActivityName(a.getName());
        res.setActivityDescription(a.getDescription());
        res.setActivityStartDate(a.getStartDate());
        res.setActivityEndDate(a.getEndDate());
        res.setActivityLocation(a.getLocation());
        res.setStudentId(s.getId());
        res.setStudentName(s.getFullName());
        res.setStudentCode(s.getStudentCode());
        res.setStatus(r.getStatus());
        res.setRegisteredDate(r.getRegisteredDate());
        res.setCreatedAt(r.getCreatedAt());
        res.setTicketCode(r.getTicketCode());
        res.setSeriesId(r.getSeriesId());
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public Response getParticipationReport(Long activityId) {
        // Lấy tất cả registration đã duyệt
        List<ActivityRegistration> approvedRegs = registrationRepository.findByActivityIdAndStatus(activityId,
                RegistrationStatus.APPROVED);

        // Lấy tất cả participation đã CHECKED_IN
        List<ActivityParticipation> checkedInList = participationRepository
                .findByActivityIdAndParticipationType(activityId, ParticipationType.CHECKED_IN);

        Set<Long> checkedInStudentIds = checkedInList.stream()
                .map(ap -> ap.getRegistration().getStudent().getId())
                .collect(Collectors.toSet());

        // Phân loại
        List<StudentResponse> attended = new ArrayList<>();
        List<StudentResponse> notAttended = new ArrayList<>();

        for (ActivityRegistration reg : approvedRegs) {
            Student s = reg.getStudent();
            StudentResponse dto = StudentResponse.fromEntity(s);

            if (checkedInStudentIds.contains(s.getId())) {
                attended.add(dto);
            } else {
                notAttended.add(dto);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("attended", attended);
        result.put("notAttended", notAttended);

        return Response.success("Danh sách tham gia", result);
    }

    /**
     * Tổng hợp điểm StudentScore từ ActivityParticipation
     */
    private void updateStudentScoreFromParticipation(ActivityParticipation participation) {
        try {
            Student student = participation.getRegistration().getStudent();
            Activity activity = participation.getRegistration().getActivity();

            // Lấy semester hiện tại (hoặc lấy semester đang mở)
            Semester currentSemester = semesterRepository.findAll().stream()
                    .filter(Semester::isOpen)
                    .findFirst()
                    .orElse(semesterRepository.findAll().stream().findFirst().orElse(null));

            if (currentSemester == null) {
                logger.warn("No semester found for score aggregation");
                return;
            }

            // Tìm bản ghi StudentScore tổng hợp
            Optional<StudentScore> scoreOpt = studentScoreRepository
                    .findByStudentIdAndSemesterIdAndScoreType(
                            student.getId(),
                            currentSemester.getId(),
                            activity.getScoreType());

            if (scoreOpt.isEmpty()) {
                logger.warn("No aggregate score record found for student {} scoreType {} in semester {}",
                        student.getId(), activity.getScoreType(), currentSemester.getId());
                return;
            }

            StudentScore score = scoreOpt.get();

            // Tính lại tổng điểm từ tất cả ActivityParticipation của sinh viên này
            // Query tất cả participation có COMPLETED status
            List<ActivityParticipation> allParticipations = participationRepository
                    .findAll()
                    .stream()
                    .filter(p -> p.getRegistration().getStudent().getId().equals(student.getId())
                            && p.getRegistration().getActivity().getScoreType().equals(activity.getScoreType())
                            && p.getParticipationType().equals(ParticipationType.COMPLETED))
                    .collect(Collectors.toList());

            BigDecimal total = allParticipations.stream()
                    .map(p -> p.getPointsEarned() != null ? p.getPointsEarned() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Cập nhật
            BigDecimal oldScore = score.getScore();
            score.setScore(total);
            studentScoreRepository.save(score);

            // Tạo history
            User systemUser = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER)
                    .findFirst()
                    .orElse(null);

            ScoreHistory history = new ScoreHistory();
            history.setScore(score);
            history.setOldScore(oldScore);
            history.setNewScore(total);
            history.setChangedBy(systemUser != null ? systemUser : userRepository.findById(1L).orElse(null));
            history.setChangeDate(LocalDateTime.now());
            history.setReason("Recalculated from activity participation: " + activity.getName());
            history.setActivityId(activity.getId());
            scoreHistoryRepository.save(history);

            logger.info("Updated student score from participation: {} -> {} for student {}",
                    oldScore, total, student.getId());

        } catch (Exception e) {
            logger.error("Failed to update student score from participation: {}", e.getMessage(), e);
        }
    }

    /**
     * Validate/lookup ticketCode để preview thông tin trước khi check-in
     */
    @Override
    @Transactional
    public Response validateTicketCode(String ticketCode) {
        try {
            if (ticketCode == null || ticketCode.isBlank()) {
                return Response.error("Ticket code is required");
            }

            Optional<ActivityRegistration> registrationOpt = registrationRepository.findByTicketCode(ticketCode);
            if (registrationOpt.isEmpty()) {
                return Response.error("Không tìm thấy mã vé hợp lệ");
            }

            ActivityRegistration registration = registrationOpt.get();

            // Block if activity is draft
            if (registration.getActivity().isDraft()) {
                return Response.error("Sự kiện chưa được công bố");
            }

            // Check if registration is approved
            if (registration.getStatus() != RegistrationStatus.APPROVED) {
                return Response.error("Đăng ký chưa được duyệt. Trạng thái: " + registration.getStatus());
            }

            // Check if participation exists, if not, create it automatically
            Optional<ActivityParticipation> participationOpt = participationRepository.findByRegistration(registration);
            ActivityParticipation participation;

            if (participationOpt.isEmpty()) {
                // Auto-create participation if registration is APPROVED but participation
                // doesn't exist
                participation = new ActivityParticipation();
                participation.setRegistration(registration);
                participation.setParticipationType(ParticipationType.REGISTERED);
                participation.setPointsEarned(BigDecimal.ZERO);
                participation.setDate(LocalDateTime.now());
                participation = participationRepository.save(participation);
                logger.info("Auto-created participation for registration ID: {}", registration.getId());
            } else {
                participation = participationOpt.get();
            }

            // Build response with student and activity info
            Map<String, Object> info = new HashMap<>();
            info.put("ticketCode", registration.getTicketCode());
            info.put("studentId", registration.getStudent().getId());
            info.put("studentName", registration.getStudent().getFullName());
            info.put("studentCode", registration.getStudent().getStudentCode());
            info.put("activityId", registration.getActivity().getId());
            info.put("activityName", registration.getActivity().getName());
            info.put("currentStatus", participation.getParticipationType().name());
            info.put("canCheckIn", participation.getParticipationType() == ParticipationType.REGISTERED);
            info.put("canCheckOut", participation.getParticipationType() == ParticipationType.CHECKED_IN);

            return Response.success("Mã vé hợp lệ", info);
        } catch (Exception e) {
            logger.error("Error validating ticket code: {}", e.getMessage(), e);
            return Response.error("Lỗi khi xác thực mã vé: " + e.getMessage());
        }
    }

    /**
     * Backfill: Tạo participation cho tất cả registration đã APPROVED nhưng chưa có
     * participation
     */
    @Override
    @Transactional
    public Response backfillMissingParticipations() {
        try {
            List<ActivityRegistration> registrationsWithoutParticipation = registrationRepository
                    .findApprovedRegistrationsWithoutParticipation();

            if (registrationsWithoutParticipation.isEmpty()) {
                return Response.success("Không có registration nào cần backfill", null);
            }

            List<ActivityParticipation> participationsToCreate = new ArrayList<>();
            for (ActivityRegistration registration : registrationsWithoutParticipation) {
                // Double check - có thể đã được tạo bởi concurrent request
                if (!participationRepository.existsByRegistration(registration)) {
                    ActivityParticipation participation = new ActivityParticipation();
                    participation.setRegistration(registration);
                    participation.setParticipationType(ParticipationType.REGISTERED);
                    participation.setPointsEarned(BigDecimal.ZERO);
                    participation.setDate(LocalDateTime.now());
                    participationsToCreate.add(participation);
                }
            }

            if (!participationsToCreate.isEmpty()) {
                participationRepository.saveAll(participationsToCreate);
                logger.info("Backfilled {} missing participations", participationsToCreate.size());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("totalFound", registrationsWithoutParticipation.size());
            result.put("created", participationsToCreate.size());
            result.put("skipped", registrationsWithoutParticipation.size() - participationsToCreate.size());

            return Response.success(
                    String.format("Đã tạo %d participation cho %d registration",
                            participationsToCreate.size(),
                            registrationsWithoutParticipation.size()),
                    result);
        } catch (Exception e) {
            logger.error("Error backfilling missing participations: {}", e.getMessage(), e);
            return Response.error("Lỗi khi backfill participation: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getActivityParticipations(Long activityId) {
        try {
            // Validate activity exists
            Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(activityId);
            if (activityOpt.isEmpty()) {
                return Response.error("Activity not found");
            }

            // Lấy tất cả participations theo activityId
            List<ActivityParticipation> participations = participationRepository.findByActivityId(activityId);

            // Convert to response
            List<ActivityParticipationResponse> responses = participations.stream()
                    .map(this::toParticipationResponse)
                    .collect(Collectors.toList());

            return Response.success("Danh sách participations", responses);
        } catch (Exception e) {
            logger.error("Failed to get activity participations: {}", e.getMessage(), e);
            return Response.error("Failed to get participations: " + e.getMessage());
        }
    }

    /**
     * Helper method để convert ActivityParticipation entity sang Response DTO
     */
    private ActivityParticipationResponse toParticipationResponse(ActivityParticipation participation) {
        ActivityRegistration registration = participation.getRegistration();
        return new ActivityParticipationResponse(
                participation.getId(),
                registration.getActivity().getId(),
                registration.getActivity().getName(),
                registration.getStudent().getId(),
                registration.getStudent().getFullName(),
                registration.getStudent().getStudentCode(),
                participation.getParticipationType(),
                participation.getPointsEarned(),
                participation.getDate(),
                participation.getIsCompleted(),
                participation.getCheckInTime(),
                participation.getCheckOutTime());
    }

    /**
     * Cập nhật điểm REN_LUYEN từ ActivityParticipation (cho dual score calculation)
     * Dùng cho CHUYEN_DE_DOANH_NGHIEP activities
     */
    private void updateRenLuyenScoreFromParticipation(ActivityParticipation participation) {
        try {
            Student student = participation.getRegistration().getStudent();
            Activity activity = participation.getRegistration().getActivity();

            // Lấy semester hiện tại
            Semester currentSemester = semesterRepository.findAll().stream()
                    .filter(Semester::isOpen)
                    .findFirst()
                    .orElse(semesterRepository.findAll().stream().findFirst().orElse(null));

            if (currentSemester == null) {
                logger.warn("No semester found for RL score aggregation");
                return;
            }

            // Tìm bản ghi StudentScore REN_LUYEN
            Optional<StudentScore> scoreOpt = studentScoreRepository
                    .findByStudentIdAndSemesterIdAndScoreType(
                            student.getId(),
                            currentSemester.getId(),
                            ScoreType.REN_LUYEN);

            if (scoreOpt.isEmpty()) {
                logger.warn("No REN_LUYEN score record found for student {} in semester {}",
                        student.getId(), currentSemester.getId());
                return;
            }

            StudentScore score = scoreOpt.get();

            // Tính lại tổng điểm RL từ tất cả ActivityParticipation CHUYEN_DE_DOANH_NGHIEP có maxPoints
            List<ActivityParticipation> allParticipations = participationRepository
                    .findAll()
                    .stream()
                    .filter(p -> p.getRegistration().getStudent().getId().equals(student.getId())
                            && p.getRegistration().getActivity().getType() == ActivityType.CHUYEN_DE_DOANH_NGHIEP
                            && p.getRegistration().getActivity().getMaxPoints() != null
                            && p.getParticipationType().equals(ParticipationType.COMPLETED))
                    .collect(Collectors.toList());

            BigDecimal total = allParticipations.stream()
                    .map(p -> p.getPointsEarned() != null ? p.getPointsEarned() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Cập nhật
            BigDecimal oldScore = score.getScore();
            score.setScore(total);
            studentScoreRepository.save(score);

            // Tạo history
            User systemUser = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER)
                    .findFirst()
                    .orElse(null);

            ScoreHistory history = new ScoreHistory();
            history.setScore(score);
            history.setOldScore(oldScore);
            history.setNewScore(total);
            history.setChangedBy(systemUser != null ? systemUser : userRepository.findById(1L).orElse(null));
            history.setChangeDate(LocalDateTime.now());
            history.setReason("Dual score calculation - RL points from CHUYEN_DE_DOANH_NGHIEP: " + activity.getName());
            history.setActivityId(activity.getId());
            scoreHistoryRepository.save(history);

            logger.info("Updated REN_LUYEN score from CHUYEN_DE_DOANH_NGHIEP participation: {} -> {} for student {}",
                    oldScore, total, student.getId());

        } catch (Exception e) {
            logger.error("Failed to update REN_LUYEN score from participation: {}", e.getMessage(), e);
        }
    }

    /**
     * Cập nhật điểm CHUYEN_DE bằng cách đếm số buổi tham gia
     * Mỗi lần check-out CHUYEN_DE_DOANH_NGHIEP → tăng count lên 1
     */
    private void updateChuyenDeScoreCount(ActivityParticipation participation) {
        try {
            Student student = participation.getRegistration().getStudent();
            Activity activity = participation.getRegistration().getActivity();

            // Lấy semester hiện tại
            Semester currentSemester = semesterRepository.findAll().stream()
                    .filter(Semester::isOpen)
                    .findFirst()
                    .orElse(semesterRepository.findAll().stream().findFirst().orElse(null));

            if (currentSemester == null) {
                logger.warn("No semester found for CHUYEN_DE score count");
                return;
            }

            // Tìm bản ghi StudentScore CHUYEN_DE
            Optional<StudentScore> scoreOpt = studentScoreRepository
                    .findByStudentIdAndSemesterIdAndScoreType(
                            student.getId(),
                            currentSemester.getId(),
                            ScoreType.CHUYEN_DE);

            if (scoreOpt.isEmpty()) {
                logger.warn("No CHUYEN_DE score record found for student {} in semester {}",
                        student.getId(), currentSemester.getId());
                return;
            }

            StudentScore score = scoreOpt.get();

            // Đếm số buổi tham gia CHUYEN_DE_DOANH_NGHIEP đã COMPLETED
            // Mỗi participation COMPLETED = 1 buổi tham gia
            List<ActivityParticipation> allParticipations = participationRepository
                    .findAll()
                    .stream()
                    .filter(p -> p.getRegistration().getStudent().getId().equals(student.getId())
                            && p.getRegistration().getActivity().getType() == ActivityType.CHUYEN_DE_DOANH_NGHIEP
                            && p.getParticipationType().equals(ParticipationType.COMPLETED))
                    .collect(Collectors.toList());

            // Số buổi = số participation đã COMPLETED (mỗi activity = 1 participation = 1 buổi)
            BigDecimal count = BigDecimal.valueOf(allParticipations.size());

            // Cập nhật
            BigDecimal oldScore = score.getScore();
            score.setScore(count);
            studentScoreRepository.save(score);

            // Tạo history
            User systemUser = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER)
                    .findFirst()
                    .orElse(null);

            ScoreHistory history = new ScoreHistory();
            history.setScore(score);
            history.setOldScore(oldScore);
            history.setNewScore(count);
            history.setChangedBy(systemUser != null ? systemUser : userRepository.findById(1L).orElse(null));
            history.setChangeDate(LocalDateTime.now());
            history.setReason("Counted CHUYEN_DE sessions from activity: " + activity.getName());
            history.setActivityId(activity.getId());
            scoreHistoryRepository.save(history);

            logger.info("Updated CHUYEN_DE score count: {} -> {} ({} sessions) for student {}",
                    oldScore, count, allParticipations.size(), student.getId());

        } catch (Exception e) {
            logger.error("Failed to update CHUYEN_DE score count: {}", e.getMessage(), e);
        }
    }
    /**
     * Lấy danh sách Đăng ký của sinh theo status
     */

    @Override
    public Response getStudentRegistrationsStatus(Long studentId, RegistrationStatus status) {
        try {
            List<ActivityRegistration> registrations =
                    registrationRepository.findListByStudentIdAndStatus(studentId, status);

            List<ActivityRegistrationResponse> responses = registrations.stream()
                    .map(this::toRegistrationResponse)
                    .toList();

            return new Response(true, "Student registrations retrieved successfully", responses);

        } catch (Exception e) {
            logger.error("Failed to retrieve student registrations: {}", e.getMessage(), e);
            return new Response(false, "Failed to retrieve registrations due to server error", null);
        }
    }
}
