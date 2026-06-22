package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.Role;
import vn.campuslife.model.Response;
import vn.campuslife.model.StudentResponse;
import vn.campuslife.model.activity.ActivityParticipationRequest;
import vn.campuslife.model.activity.ActivityParticipationResponse;
import vn.campuslife.model.activity.ActivityRegistrationRequest;
import vn.campuslife.model.activity.ActivityRegistrationResponse;
import vn.campuslife.repository.*;
import vn.campuslife.service.ActivityRegistrationService;
import vn.campuslife.service.NotificationService;
import vn.campuslife.service.ReminderScheduleService;
import vn.campuslife.service.SemesterHelperService;
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

    private final UploadProperties uploadProperties;
    private final ActivityRegistrationRepository registrationRepository;
    private final ActivityParticipationRepository participationRepository;
    private final ActivityRepository activityRepository;
    private final StudentRepository studentRepository;
    private final NotificationService notificationService;
    private final ReminderScheduleService reminderScheduleService;
    private final vn.campuslife.service.ActivitySeriesService activitySeriesService;
    private final ActivitySeriesRepository activitySeriesRepository;
    private final SemesterHelperService semesterHelperService;
    private final vn.campuslife.service.ScoreRuleEngine scoreRuleEngine;
    private final TaskSubmissionRepository taskSubmissionRepository;

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
            if (!hasRemainingSlots(activity.getId(), activity.getTicketQuantity())) {
                return new Response(false, "Activity is full", null);
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

            if (saved.getStatus() == RegistrationStatus.APPROVED) {
                try {
                    reminderScheduleService.createEventRemindersForApprovedRegistration(saved);
                    syncSeriesMinimumRequirementReminder(activity.getSeriesId(), student);
                } catch (Exception e) {
                    logger.error("Failed to prepare event reminders for registration {}: {}", saved.getId(),
                            e.getMessage(), e);
                }
            }

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
                        null, // Không set actionUrl, để frontend tự route dựa trên metadata.activityId
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
    public Response getSeriesRegistrations(Long seriesId) {
        try {
            List<ActivityRegistration> registrations = registrationRepository.findBySeriesId(seriesId);

            // Lọc unique students (có thể có nhiều registrations cho cùng 1 student trong
            // series)
            Map<Long, ActivityRegistration> uniqueStudentRegistrations = new HashMap<>();
            for (ActivityRegistration reg : registrations) {
                Long studentId = reg.getStudent().getId();
                if (!uniqueStudentRegistrations.containsKey(studentId)) {
                    uniqueStudentRegistrations.put(studentId, reg);
                }
            }

            List<ActivityRegistrationResponse> responses = uniqueStudentRegistrations.values().stream()
                    .map(this::toRegistrationResponse)
                    .collect(Collectors.toList());

            return new Response(true, "Series registrations retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to retrieve series registrations: {}", e.getMessage(), e);
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
            RegistrationStatus previousStatus = registration.getStatus();
            RegistrationStatus newStatus = RegistrationStatus.valueOf(status.toUpperCase());
            if (newStatus == RegistrationStatus.APPROVED
                    && registration.getStatus() != RegistrationStatus.APPROVED
                    && !hasRemainingSlots(registration.getActivity().getId(),
                            registration.getActivity().getTicketQuantity())) {
                return new Response(false, "Activity is full. Cannot approve more registrations.", null);
            }

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

                try {
                    reminderScheduleService.createEventRemindersForApprovedRegistration(savedRegistration);
                    syncSeriesMinimumRequirementReminder(savedRegistration.getSeriesId(),
                            savedRegistration.getStudent());
                } catch (Exception e) {
                    logger.error("Failed to prepare event reminders for registration {}: {}",
                            savedRegistration.getId(), e.getMessage(), e);
                }
            } else if (previousStatus == RegistrationStatus.APPROVED) {
                try {
                    reminderScheduleService.cancelPendingEventRemindersForRegistration(savedRegistration);
                } catch (Exception e) {
                    logger.error("Failed to cancel event reminders for registration {}: {}",
                            savedRegistration.getId(), e.getMessage(), e);
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
            List<ActivityRegistration> approvedRegistrations = registrationRepository.findListByStudentIdAndStatus(
                    request.getStudentId(),
                    RegistrationStatus.APPROVED);
            if (approvedRegistrations.isEmpty()) {
                throw new RuntimeException("Không tìm thấy đăng ký hợp lệ cho sinh viên này");
            }
            if (approvedRegistrations.size() > 1) {
                throw new RuntimeException(
                        "Sinh viên có nhiều đăng ký đã duyệt. Vui lòng dùng ticketCode để check-in đúng sự kiện");
            }
            registration = approvedRegistrations.get(0);
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
        Activity activity = registration.getActivity();
        LocalDateTime now = LocalDateTime.now();

        // CHECK-IN (lần 1) - từ REGISTERED/APPROVED sang CHECKED_IN
        if (currentType == ParticipationType.REGISTERED) {
            participation.setParticipationType(ParticipationType.CHECKED_IN);
            participation.setCheckInTime(now);
            participation.setDate(now);
            participationRepository.save(participation);

            ActivityParticipationResponse resp = toParticipationResponse(participation);

            return Response.success("Check-in thành công. Vui lòng check-out khi rời khỏi sự kiện.", resp);
        }

        // CHECK-OUT (lần 2) - từ CHECKED_IN sang ATTENDED/COMPLETED
        else if (currentType == ParticipationType.CHECKED_IN) {
            markParticipationAsAttended(registration, participation, now, true);
            boolean completed = finalizeAttendanceOutcome(registration, participation,
                    participation.getRegistration().getStudent().getUser());

            ActivityParticipationResponse resp = toParticipationResponse(participation);

            String message = completed
                    ? (activity.getSeriesId() != null
                            ? "Check-out thành công. Đã hoàn thành mốc sự kiện trong chuỗi."
                            : "Check-out thành công. Đã hoàn thành tham gia sự kiện.")
                    : activity.isRequiresSubmission()
                            ? "Check-out thành công. Đã ghi nhận tham gia sự kiện, chờ bài nộp được chấm để hoàn tất."
                            : (activity.getSeriesId() != null
                                    ? "Check-out thành công. Đã ghi nhận tham gia mốc trong chuỗi."
                                    : "Check-out thành công. Đã ghi nhận tham gia sự kiện.");

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
                    .orElseThrow(
                            () -> new RuntimeException("Bạn chưa đăng ký hoặc chưa được duyệt tham gia activity này"));

            // 4. Tìm hoặc tạo participation
            ActivityParticipation participation = participationRepository
                    .findByRegistration(registration)
                    .orElseGet(() -> {
                        // Nếu chưa có participation, tạo mới
                        ActivityParticipation newParticipation = new ActivityParticipation();
                        newParticipation.setRegistration(registration);
                        newParticipation.setParticipationType(ParticipationType.REGISTERED);
                        newParticipation.setPointsEarned(BigDecimal.ZERO);
                        newParticipation.setDate(LocalDateTime.now());
                        return participationRepository.save(newParticipation);
                    });

            // 5. Activity QR: xác nhận ATTENDED nhanh, không mô phỏng flow 2 bước
            LocalDateTime now = LocalDateTime.now();
            if (participation.getParticipationType() == ParticipationType.COMPLETED) {
                return Response.error("Bạn đã hoàn thành điểm danh activity này rồi");
            }

            markParticipationAsAttended(registration, participation, now, false);
            boolean completed = finalizeAttendanceOutcome(registration, participation,
                    participation.getRegistration().getStudent().getUser());

            ActivityParticipationResponse resp = toParticipationResponse(participation);
            String message = completed
                    ? (activity.getSeriesId() != null
                            ? "Điểm danh thành công bằng QR code. Đã ghi nhận hoàn thành mốc trong chuỗi."
                            : "Điểm danh thành công bằng QR code. Đã ghi nhận hoàn thành sự kiện.")
                    : "Điểm danh thành công bằng QR code. Đã ghi nhận tham gia sự kiện.";
            return Response.success(message, resp);

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
            if (participation.getParticipationType() != ParticipationType.ATTENDED
                    && participation.getParticipationType() != ParticipationType.COMPLETED) {
                return Response.error("Sinh viên chưa đạt trạng thái tham dự hợp lệ");
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

                // ✅ SỰ KIỆN CÓ BÀI NỘP: Điểm đã được cộng qua createScoreFromSubmission()
                // Chỉ cần cập nhật status, KHÔNG set pointsEarned (để tránh cộng trùng)
                markParticipationCompleted(participation, isCompleted);

                logger.info(
                        "Completed grading for submission-based activity {}. Points were already added via submission.",
                        activity.getName());

                // Nếu thuộc series, cập nhật tiến trình của sinh viên
                if (activity.getSeriesId() != null && isCompleted) {
                    try {
                        activitySeriesService.updateStudentProgress(
                                participation.getRegistration().getStudent().getId(),
                                activity.getId());
                    } catch (Exception e) {
                        logger.warn("Failed to update series progress: {}", e.getMessage());
                    }
                }

                String message = activity.getSeriesId() != null
                        ? "Đã ghi nhận hoàn thành mốc trong chuỗi sự kiện từ bài nộp"
                        : "Đã chấm điểm completion (điểm đã được tính từ bài nộp)";
                return Response.success(message, participation);
            }

            markParticipationCompleted(participation, isCompleted);

            if (activity.getSeriesId() != null) {
                if (isCompleted) {
                    try {
                        activitySeriesService.updateStudentProgress(
                                participation.getRegistration().getStudent().getId(),
                                activity.getId());
                    } catch (Exception e) {
                        logger.warn("Failed to update series progress: {}", e.getMessage());
                    }
                }
            } else {
                try {
                    scoreRuleEngine.applyActivityCompleted(
                            participation,
                            participation.getRegistration().getStudent().getUser());
                } catch (Exception e) {
                    logger.error("Failed to apply activity rules: {}", e.getMessage(), e);
                }
            }

            String message = activity.getSeriesId() != null
                    ? "Đã ghi nhận hoàn thành mốc trong chuỗi sự kiện"
                    : "Đã chấm điểm completion";
            return Response.success(message, participation);
        } catch (Exception e) {
            logger.error("Failed to grade completion: {}", e.getMessage(), e);
            return Response.error("Failed to grade completion: " + e.getMessage());
        }
    }

    /**
     * Helper method để kiểm tra submission đã được grade
     */
    private boolean checkHasGradedSubmission(Long studentId, Long activityId) {
        return taskSubmissionRepository.existsByActivityAndStudentAndStatus(activityId, studentId,
                vn.campuslife.enumeration.SubmissionStatus.GRADED);
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
        res.setImportant(a.isImportant());
        res.setMandatoryForFacultyStudents(a.isMandatoryForFacultyStudents());
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public Response getParticipationReport(Long activityId) {
        List<ActivityRegistration> eligibleRegs = registrationRepository
                .findByActivityIdAndActivityIsDeletedFalse(activityId)
                .stream()
                .filter(reg -> reg.getStatus() == RegistrationStatus.APPROVED
                        || reg.getStatus() == RegistrationStatus.ATTENDED)
                .toList();

        Set<ParticipationType> attendedStates = EnumSet.of(
                ParticipationType.ATTENDED,
                ParticipationType.COMPLETED);
        Set<Long> attendedStudentIds = participationRepository.findByActivityId(activityId).stream()
                .filter(ap -> attendedStates.contains(ap.getParticipationType()))
                .map(ap -> ap.getRegistration().getStudent().getId())
                .collect(Collectors.toSet());

        // Phân loại
        List<StudentResponse> attended = new ArrayList<>();
        List<StudentResponse> notAttended = new ArrayList<>();

        for (ActivityRegistration reg : eligibleRegs) {
            Student s = reg.getStudent();
            StudentResponse dto = StudentResponse.fromEntity(s, uploadProperties.getPublicUrl());

            if (attendedStudentIds.contains(s.getId())) {
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
     * Lấy danh sách Đăng ký của sinh theo status
     */

    @Override
    public Response getStudentRegistrationsStatus(Long studentId, RegistrationStatus status) {
        try {
            List<ActivityRegistration> registrations = registrationRepository.findListByStudentIdAndStatus(studentId,
                    status);

            List<ActivityRegistrationResponse> responses = registrations.stream()
                    .map(this::toRegistrationResponse)
                    .toList();

            return new Response(true, "Student registrations retrieved successfully", responses);

        } catch (Exception e) {
            logger.error("Failed to retrieve student registrations: {}", e.getMessage(), e);
            return new Response(false, "Failed to retrieve registrations due to server error", null);
        }
    }

    /**
     * Tìm kiếm
     */
    public Response search(String keyword, RegistrationStatus status) {
        List<ActivityRegistration> registrations = registrationRepository.search(keyword, status);

        List<ActivityRegistrationResponse> responses = registrations.stream()
                .map(this::toRegistrationResponse)
                .toList();

        return new Response(true,
                "Student registrations retrieved successfully",
                responses);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getStudentJoinedEventDates(Long studentId) {
        try {
            List<ActivityRegistration> registrations = registrationRepository
                    .findByStudentIdAndStudentIsDeletedFalse(studentId);
            List<Map<String, Object>> dates = registrations.stream()
                    .filter(r -> r.getStatus() == RegistrationStatus.APPROVED
                            || r.getStatus() == RegistrationStatus.ATTENDED)
                    .map(r -> {
                        Activity a = r.getActivity();
                        Map<String, Object> map = new HashMap<>();
                        map.put("activityId", a.getId());
                        map.put("title", a.getName());
                        map.put("startTime", a.getStartDate());
                        map.put("endTime", a.getEndDate());
                        map.put("location", a.getLocation());
                        return map;
                    })
                    .collect(Collectors.toList());
            return new Response(true, "Event dates retrieved", dates);
        } catch (Exception e) {
            logger.error("Error retrieving student event dates: ", e);
            return new Response(false, "Error retrieving event dates", null);
        }
    }

    @Override
    @Transactional
    public Response registerForWaitlist(Long activityId, Long studentId) {
        try {
            Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(activityId);
            if (activityOpt.isEmpty()) {
                return new Response(false, "Activity not found", null);
            }
            Activity activity = activityOpt.get();

            if (activity.isDraft()) {
                return new Response(false, "Activity is not published yet", null);
            }

            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }
            Student student = studentOpt.get();

            if (registrationRepository.existsByActivityIdAndStudentId(activityId, studentId)) {
                return new Response(false, "Already registered or in waitlist for this activity", null);
            }

            if (activity.getRegistrationDeadline() != null &&
                    LocalDateTime.now().isAfter(activity.getRegistrationDeadline())) {
                return new Response(false, "Registration deadline has passed", null);
            }

            // Only allow waitlist if the activity is actually full
            if (hasRemainingSlots(activity.getId(), activity.getTicketQuantity())) {
                return new Response(false, "Activity still has slots. Please register normally.", null);
            }

            ActivityRegistration registration = new ActivityRegistration();
            registration.setActivity(activity);
            registration.setStudent(student);
            registration.setRegisteredDate(LocalDateTime.now());
            registration.setStatus(RegistrationStatus.WAITLIST);
            registration.setTicketCode(TicketCodeUtils.newTicketCode());

            registrationRepository.save(registration);

            // Notify admin/manager about new waitlist entry if needed,
            // but usually waitlist is just for tracking.

            return new Response(true, "Successfully joined the waitlist", null);
        } catch (Exception e) {
            logger.error("Error joining waitlist: ", e);
            return new Response(false, "An error occurred while joining waitlist", null);
        }
    }

    private boolean hasRemainingSlots(Long activityId, Integer ticketQuantity) {
        if (ticketQuantity == null) {
            return true;
        }
        Long approvedCount = registrationRepository.countByActivityIdAndStatus(activityId, RegistrationStatus.APPROVED);
        return approvedCount < ticketQuantity;
    }

    private void markParticipationAsAttended(ActivityRegistration registration,
            ActivityParticipation participation,
            LocalDateTime attendedAt,
            boolean fromTicketFlow) {
        if (participation.getCheckInTime() == null) {
            participation.setCheckInTime(attendedAt);
        }
        if (fromTicketFlow) {
            participation.setCheckOutTime(attendedAt);
        }
        participation.setParticipationType(ParticipationType.ATTENDED);
        participation.setDate(attendedAt);
        participationRepository.save(participation);

        if (registration.getStatus() != RegistrationStatus.ATTENDED) {
            registration.setStatus(RegistrationStatus.ATTENDED);
            registrationRepository.save(registration);
        }
    }

    private boolean finalizeAttendanceOutcome(ActivityRegistration registration,
            ActivityParticipation participation,
            User actor) {
        Activity activity = registration.getActivity();
        if (activity == null || participation.getParticipationType() == ParticipationType.COMPLETED) {
            return participation.getParticipationType() == ParticipationType.COMPLETED;
        }

        if (!activity.isRequiresSubmission()) {
            markParticipationCompleted(participation, true);
            applyStandaloneOrSeriesAttendanceResult(registration, participation, actor);
            return true;
        }

        Optional<TaskSubmission> gradedSubmissionOpt = findLatestGradedSubmission(
                registration.getStudent().getId(),
                activity.getId());
        if (gradedSubmissionOpt.isEmpty()) {
            return false;
        }

        markParticipationCompleted(participation, gradedSubmissionOpt.get().getIsCompleted());
        applyStandaloneOrSeriesSubmissionResult(activity, registration.getStudent(), gradedSubmissionOpt.get(), actor);
        return true;
    }

    private void markParticipationCompleted(ActivityParticipation participation, boolean isCompleted) {
        participation.setIsCompleted(isCompleted);
        participation.setPointsEarned(BigDecimal.ZERO);
        participation.setParticipationType(ParticipationType.COMPLETED);
        participationRepository.save(participation);
    }

    private void applyStandaloneOrSeriesAttendanceResult(ActivityRegistration registration,
            ActivityParticipation participation,
            User actor) {
        Activity activity = registration.getActivity();
        if (activity.getSeriesId() != null) {
            try {
                activitySeriesService.updateStudentProgress(
                        registration.getStudent().getId(),
                        activity.getId());
            } catch (Exception e) {
                logger.warn("Failed to update series progress: {}", e.getMessage());
            }
            return;
        }

        try {
            scoreRuleEngine.applyActivityCompleted(participation, actor);
        } catch (Exception e) {
            logger.error("Failed to apply activity rules: {}", e.getMessage(), e);
        }
    }

    private void applyStandaloneOrSeriesSubmissionResult(Activity activity,
            Student student,
            TaskSubmission submission,
            User actor) {
        if (activity.getSeriesId() != null) {
            if (Boolean.TRUE.equals(submission.getIsCompleted())) {
                try {
                    activitySeriesService.updateStudentProgress(student.getId(), activity.getId());
                } catch (Exception e) {
                    logger.warn("Failed to update series progress: {}", e.getMessage());
                }
            }
            return;
        }

        try {
            scoreRuleEngine.applySubmissionGraded(submission, actor);
        } catch (Exception e) {
            logger.error("Failed to apply submission rules: {}", e.getMessage(), e);
        }
    }

    private Optional<TaskSubmission> findLatestGradedSubmission(Long studentId, Long activityId) {
        return taskSubmissionRepository.findByActivityAndStudentAndStatusOrderByLatest(
                activityId,
                studentId,
                vn.campuslife.enumeration.SubmissionStatus.GRADED).stream().findFirst();
    }

    private void syncSeriesMinimumRequirementReminder(Long seriesId, Student student) {
        if (seriesId == null || student == null) {
            return;
        }
        activitySeriesRepository.findById(seriesId)
                .ifPresent(series -> reminderScheduleService.syncSeriesMinimumRequirementReminder(series, student));
    }

}
