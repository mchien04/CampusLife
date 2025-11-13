package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.Role;
import vn.campuslife.model.*;
import vn.campuslife.repository.*;
import vn.campuslife.service.ActivityRegistrationService;
import vn.campuslife.util.TicketCodeUtils;

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

            // 2) Kiểm tra student
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }
            Student student = studentOpt.get();

            // 3) Đã đăng ký chưa?
            if (registrationRepository.existsByActivityIdAndStudentId(request.getActivityId(), studentId)) {
                return new Response(false, "Already registered for this activity", null);
            }

            // 4) Thời gian mở/đóng đăng ký
            if (activity.getRegistrationDeadline() != null &&
                    LocalDateTime.now().isAfter(activity.getRegistrationDeadline().atStartOfDay())) {
                return new Response(false, "Registration deadline has passed", null);
            }
            if (activity.getRegistrationStartDate() != null &&
                    LocalDateTime.now().isBefore(activity.getRegistrationStartDate().atStartOfDay())) {
                return new Response(false, "Registration is not yet open", null);
            }

            // 5) Kiểm tra số lượng vé (nếu giới hạn theo APPROVED)
            if (activity.getTicketQuantity() != null) {
                Long current = registrationRepository
                        .countByActivityIdAndStatus(request.getActivityId(), RegistrationStatus.APPROVED);
                if (current >= activity.getTicketQuantity()) {
                    return new Response(false, "Activity is full", null);
                }
            }

            // 6) Tạo đăng ký + MÃ VÉ
            ActivityRegistration registration = new ActivityRegistration();
            registration.setActivity(activity);
            registration.setStudent(student);
            registration.setRegisteredDate(LocalDateTime.now());
            registration.setStatus(RegistrationStatus.PENDING);

            String code;
            int attempts = 0;
            do {
                code = TicketCodeUtils.newTicketCode();
                attempts++;
            } while (registrationRepository.existsByTicketCode(code) && attempts < 3);
            registration.setTicketCode(code);

            ActivityRegistration saved = registrationRepository.save(registration);
            ActivityRegistrationResponse payload = toRegistrationResponse(saved);

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

        // Lấy participation đã tạo khi duyệt
        ActivityParticipation participation = participationRepository.findByRegistration(registration)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy participation cho đăng ký này"));

        ParticipationType currentType = participation.getParticipationType();

        //CHECK-IN LẦN 1 — REGISTERED → CHECKED_IN

        if (currentType == ParticipationType.REGISTERED) {

            participation.setParticipationType(ParticipationType.CHECKED_IN);
            participation.setCheckInTime(LocalDateTime.now());
            participation.setDate(LocalDateTime.now());
            participationRepository.save(participation);

            ActivityParticipationResponse resp = new ActivityParticipationResponse(
                    participation.getId(),
                    registration.getActivity().getId(),
                    registration.getActivity().getName(),
                    registration.getStudent().getId(),
                    registration.getStudent().getFullName(),
                    registration.getStudent().getStudentCode(),
                    participation.getParticipationType(),
                    participation.getPointsEarned(),
                    participation.getDate(),
                    request.getNotes()
            );

            return Response.success("Check-in thành công. Vui lòng check-out khi rời khỏi sự kiện.", resp);
        }

        //CHECK-OUT LẦN 2 — CHECKED_IN → CHECKED_OUT → ATTENDED

        else if (currentType == ParticipationType.CHECKED_IN) {

            // Đánh dấu check-out
            participation.setParticipationType(ParticipationType.CHECKED_OUT);
            participation.setCheckOutTime(LocalDateTime.now());
            participationRepository.save(participation);

            // Cập nhật status registration
            registration.setStatus(RegistrationStatus.ATTENDED);
            registrationRepository.save(registration);

            // Chuyển participation sang ATTENDED
            participation.setParticipationType(ParticipationType.ATTENDED);
            participation.setPointsEarned(registration.getActivity().getMaxPoints());
            participation.setDate(LocalDateTime.now());
            participationRepository.save(participation);

            ActivityParticipationResponse resp = new ActivityParticipationResponse(
                    participation.getId(),
                    registration.getActivity().getId(),
                    registration.getActivity().getName(),
                    registration.getStudent().getId(),
                    registration.getStudent().getFullName(),
                    registration.getStudent().getStudentCode(),
                    participation.getParticipationType(),
                    participation.getPointsEarned(),
                    participation.getDate(),
                    request.getNotes()
            );

            return Response.success("Check-out thành công. Đã hoàn thành tham gia sự kiện.", resp);
        }

        //Đã tham dự rồi — không cho check-in/check-out nữa

        else {
            return Response.error("Đã hoàn thành check-in/check-out trước đó");
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

    /**
     * BỎ METHOD NÀY - Không còn dùng nữa
     * Tự động tạo StudentScore từ check-in thành công
     */
    private void createScoreFromCheckIn_BACKUP(ActivityRegistration registration, ActivityParticipation participation) {
        try {
            Activity activity = registration.getActivity();
            Student student = registration.getStudent();

            // Lấy học kỳ hiện tại (có thể cần logic phức tạp hơn)
            // Tạm thời lấy học kỳ đầu tiên, cần cải thiện sau
            Optional<Semester> currentSemester = semesterRepository.findAll().stream().findFirst();
            if (currentSemester.isEmpty()) {
                logger.warn("No semester found for score creation");
                return;
            }

            // Tìm bản ghi điểm tổng hợp theo scoreType của activity
            Optional<StudentScore> scoreOpt = studentScoreRepository
                    .findByStudentIdAndSemesterIdAndScoreType(
                            student.getId(),
                            currentSemester.get().getId(),
                            activity.getScoreType());

            if (scoreOpt.isEmpty()) {
                logger.warn("No aggregate score record found for student {} scoreType {} in semester {}",
                        student.getId(), activity.getScoreType(), currentSemester.get().getId());
                return;
            }

            StudentScore score = scoreOpt.get();

            // Parse activityIds JSON và kiểm tra nếu đã có activity này
            String activityIdsJson = score.getActivityIds() != null ? score.getActivityIds() : "[]";
            if (activityIdsJson.isEmpty()) {
                activityIdsJson = "[]";
            }

            java.util.List<Long> activityIds = new java.util.ArrayList<>();
            try {
                // Simple JSON parsing: remove brackets and split by comma
                String content = activityIdsJson.replaceAll("[\\[\\]]", "").trim();
                if (!content.isEmpty()) {
                    String[] ids = content.split(",");
                    for (String id : ids) {
                        activityIds.add(Long.parseLong(id.trim()));
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to parse activityIds: {}", activityIdsJson, e);
            }

            // Kiểm tra nếu đã có điểm từ activity này
            if (activityIds.contains(activity.getId())) {
                logger.info("Score already exists for activity {} and student {}", activity.getId(), student.getId());
                return;
            }

            // Thêm activityId vào list
            activityIds.add(activity.getId());

            // Cộng điểm
            BigDecimal oldScore = score.getScore();
            BigDecimal pointsToAdd = activity.getMaxPoints() != null ? activity.getMaxPoints() : BigDecimal.ZERO;
            BigDecimal newScore = oldScore.add(pointsToAdd);

            // Cập nhật
            score.setScore(newScore);
            String updatedActivityIds = "["
                    + activityIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + "]";
            score.setActivityIds(updatedActivityIds);

            studentScoreRepository.save(score);

            // Tạo ScoreHistory - find system user for changedBy
            User systemUser = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER)
                    .findFirst()
                    .orElse(null);

            ScoreHistory history = new ScoreHistory();
            history.setScore(score);
            history.setOldScore(oldScore);
            history.setNewScore(newScore);
            history.setChangedBy(systemUser != null ? systemUser : userRepository.findById(1L).orElse(null));
            history.setChangeDate(LocalDateTime.now());
            history.setReason("Added points from activity check-in: " + activity.getName());
            history.setActivityId(activity.getId());

            scoreHistoryRepository.save(history);

            logger.info("Added score {} (total: {}) for student {} from activity check-in {}",
                    pointsToAdd, newScore, student.getId(), activity.getId());

        } catch (Exception e) {
            logger.error("Failed to create score from check-in: {}", e.getMessage(), e);
        }
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
        res.setScoreType(a.getScoreType());
        res.setImportant(a.isImportant());
        res.setMandatoryForFacultyStudents(a.isMandatoryForFacultyStudents());

        res.setRegisteredDate(r.getRegisteredDate());
        res.setCreatedAt(r.getCreatedAt());
        res.setTicketCode(r.getTicketCode());
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
}
