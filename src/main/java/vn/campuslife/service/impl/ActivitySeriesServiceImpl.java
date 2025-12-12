package vn.campuslife.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.Response;
import vn.campuslife.repository.*;
import vn.campuslife.service.ActivitySeriesService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivitySeriesServiceImpl implements ActivitySeriesService {

    private static final Logger logger = LoggerFactory.getLogger(ActivitySeriesServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ActivitySeriesRepository seriesRepository;
    private final StudentSeriesProgressRepository progressRepository;
    private final ActivityRepository activityRepository;
    private final StudentRepository studentRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final ActivityParticipationRepository participationRepository;
    private final ActivityRegistrationRepository registrationRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public Response createSeries(String name, String description, String milestonePointsJson,
            vn.campuslife.enumeration.ScoreType scoreType, Long mainActivityId,
            LocalDateTime registrationStartDate, LocalDateTime registrationDeadline,
            Boolean requiresApproval, Integer ticketQuantity) {
        // Validate required fields
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Series name is required");
        }
        if (scoreType == null) {
            throw new IllegalArgumentException("ScoreType is required");
        }

        ActivitySeries series = new ActivitySeries();
        series.setName(name);
        series.setDescription(description);
        series.setMilestonePoints(milestonePointsJson);
        series.setScoreType(scoreType);
        series.setRegistrationStartDate(registrationStartDate);
        series.setRegistrationDeadline(registrationDeadline);
        series.setRequiresApproval(requiresApproval != null ? requiresApproval : true);
        series.setTicketQuantity(ticketQuantity);
        series.setCreatedAt(LocalDateTime.now());
        series.setDeleted(false); // Set default value for isDeleted

        if (mainActivityId != null) {
            Optional<Activity> mainActivityOpt = activityRepository.findById(mainActivityId);
            if (mainActivityOpt.isPresent()) {
                series.setMainActivity(mainActivityOpt.get());
            } else {
                logger.warn("Main activity not found: {}", mainActivityId);
            }
        }

        ActivitySeries saved = seriesRepository.save(series);
        logger.info("Created activity series: {} with scoreType: {}", saved.getId(), scoreType);
        return Response.success("Activity series created successfully", saved);
    }

    @Override
    @Transactional
    public Response createActivityInSeries(Long seriesId, String name, String description,
            LocalDateTime startDate, LocalDateTime endDate,
            String location, Integer order, String shareLink, String bannerUrl,
            String benefits, String requirements, String contactInfo, List<Long> organizerIds,
            vn.campuslife.enumeration.ActivityType type) {
        // Validate required fields
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Activity name is required");
        }
        if (seriesId == null) {
            throw new IllegalArgumentException("Series ID is required");
        }

        Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
        if (seriesOpt.isEmpty()) {
            throw new IllegalArgumentException("Series not found: " + seriesId);
        }

        ActivitySeries series = seriesOpt.get();
        if (series.isDeleted()) {
            return Response.error("Series has been deleted");
        }

        // Tạo activity với các thuộc tính tối giản
        Activity activity = new Activity();
        activity.setName(name);
        activity.setDescription(description);
        activity.setStartDate(startDate);
        activity.setEndDate(endDate);
        activity.setLocation(location);
        activity.setSeriesId(seriesId);
        activity.setSeriesOrder(order);

        // Các thuộc tính từ request
        activity.setShareLink(shareLink);
        activity.setBannerUrl(bannerUrl);
        activity.setBenefits(benefits);
        activity.setRequirements(requirements);
        activity.setContactInfo(contactInfo);

        // Xử lý organizers
        Set<Department> organizers = resolveOrganizers(organizerIds);
        activity.setOrganizers(organizers);

        // Cho phép tất cả các type (có thể chỉnh sửa sau)
        // Chỉ validate khi tạo minigame (trong MiniGameServiceImpl)

        // Các thuộc tính không cần (cho phép null)
        if (type != null) {
            activity.setType(type); // MINIGAME - cho phép set type nếu muốn tạo minigame
        } else {
            activity.setType(null); // Mặc định null cho activity thường
        }
        activity.setScoreType(null); // Lấy từ series (không dùng scoreType riêng của activity)
        activity.setMaxPoints(null); // Không dùng để tính điểm
        activity.setRegistrationStartDate(series.getRegistrationStartDate()); // Lấy từ series
        activity.setRegistrationDeadline(series.getRegistrationDeadline()); // Lấy từ series
        activity.setRequiresApproval(series.isRequiresApproval()); // Lấy từ series
        activity.setTicketQuantity(series.getTicketQuantity()); // Lấy từ series
        activity.setImportant(false); // Không cần
        activity.setMandatoryForFacultyStudents(false); // Không cần
        activity.setPenaltyPointsIncomplete(null); // Không cần
        activity.setRequiresSubmission(false);
        activity.setDraft(false); // Mặc định published
        activity.setDeleted(false);

        Activity saved = activityRepository.save(activity);

        // Auto-generate checkInCode if not provided
        if (saved.getCheckInCode() == null || saved.getCheckInCode().isBlank()) {
            String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase().replace("-", "");
            String checkInCode = String.format("ACT-%06d-%s", saved.getId(), random);
            saved.setCheckInCode(checkInCode);
            saved = activityRepository.save(saved);
            logger.debug("Auto-generated checkInCode for activity {} in series {}: {}",
                    saved.getId(), seriesId, checkInCode);
        }
        logger.info("Created activity {} in series {} with order {}", saved.getId(), seriesId, order);

        // Auto-register all students who already registered any activity in this series
        autoRegisterStudentsForNewActivityInSeries(series, saved);

        return Response.success("Activity created in series successfully", saved);
    }

    private Set<Department> resolveOrganizers(List<Long> organizerIds) {
        if (organizerIds == null || organizerIds.isEmpty())
            return new LinkedHashSet<>();
        var deps = departmentRepository.findAllById(organizerIds);
        var found = deps.stream().map(Department::getId).collect(Collectors.toSet());
        var missing = organizerIds.stream().filter(id -> !found.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Department ids not found: " + missing);
        }
        return new LinkedHashSet<>(deps);
    }

    @Override
    @Transactional
    public Response registerForSeries(Long seriesId, Long studentId) {
        try {
            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }

            Optional<Student> studentOpt = studentRepository.findById(studentId);
            if (studentOpt.isEmpty()) {
                return Response.error("Student not found");
            }

            ActivitySeries series = seriesOpt.get();
            Student student = studentOpt.get();

            // Kiểm tra thời gian đăng ký
            if (series.getRegistrationDeadline() != null &&
                    LocalDateTime.now().isAfter(series.getRegistrationDeadline())) {
                return Response.error("Registration deadline has passed");
            }
            if (series.getRegistrationStartDate() != null &&
                    LocalDateTime.now().isBefore(series.getRegistrationStartDate())) {
                return Response.error("Registration has not started yet");
            }

            // Kiểm tra ticketQuantity (đếm số student đã đăng ký ít nhất 1 activity trong
            // series)
            if (series.getTicketQuantity() != null) {
                List<Activity> activities = activityRepository.findBySeriesIdAndIsDeletedFalse(seriesId);
                Set<Long> registeredStudentIds = new HashSet<>();
                for (Activity activity : activities) {
                    List<ActivityRegistration> regs = registrationRepository
                            .findByActivityIdAndActivityIsDeletedFalse(activity.getId());
                    for (ActivityRegistration reg : regs) {
                        registeredStudentIds.add(reg.getStudent().getId());
                    }
                }
                if (registeredStudentIds.size() >= series.getTicketQuantity()) {
                    return Response.error("Series is full");
                }
            }

            // Lấy tất cả activities trong series
            List<Activity> activities = activityRepository.findBySeriesIdAndIsDeletedFalse(seriesId);
            if (activities.isEmpty()) {
                return Response.error("No activities found in series");
            }

            // Tạo registrations cho tất cả activities
            List<ActivityRegistration> registrations = new ArrayList<>();
            for (Activity activity : activities) {
                // Kiểm tra đã đăng ký chưa
                if (registrationRepository.existsByActivityIdAndStudentId(activity.getId(), studentId)) {
                    continue; // Bỏ qua nếu đã đăng ký
                }

                ActivityRegistration registration = new ActivityRegistration();
                registration.setActivity(activity);
                registration.setStudent(student);
                registration.setRegisteredDate(LocalDateTime.now());
                registration.setSeriesId(seriesId);
                registration.setTicketCode(java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());

                // Set status dựa trên requiresApproval của series
                if (series.isRequiresApproval()) {
                    registration.setStatus(RegistrationStatus.PENDING);
                } else {
                    registration.setStatus(RegistrationStatus.APPROVED);
                }

                registrations.add(registration);
            }

            if (registrations.isEmpty()) {
                return Response.error("Already registered for all activities in series");
            }

            registrationRepository.saveAll(registrations);

            // Tạo participation cho các registration có status APPROVED
            List<ActivityParticipation> participationsToCreate = new ArrayList<>();
            for (ActivityRegistration registration : registrations) {
                if (registration.getStatus() == RegistrationStatus.APPROVED) {
                    // Kiểm tra xem đã có participation chưa
                    if (!participationRepository.existsByRegistration(registration)) {
                        ActivityParticipation participation = new ActivityParticipation();
                        participation.setRegistration(registration);
                        participation.setParticipationType(ParticipationType.REGISTERED);
                        participation.setPointsEarned(BigDecimal.ZERO);
                        participation.setDate(LocalDateTime.now());
                        participationsToCreate.add(participation);
                    }
                }
            }

            if (!participationsToCreate.isEmpty()) {
                participationRepository.saveAll(participationsToCreate);
                logger.info("Created {} participations for series registrations", participationsToCreate.size());
            }

            logger.info("Registered student {} for {} activities in series {}",
                    studentId, registrations.size(), seriesId);

            return Response.success("Registered for series successfully. " +
                    registrations.size() + " activities registered.", registrations);
        } catch (Exception e) {
            logger.error("Failed to register for series: {}", e.getMessage(), e);
            return Response.error("Failed to register for series: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response addActivityToSeries(Long activityId, Long seriesId, Integer order) {
        try {
            Optional<Activity> activityOpt = activityRepository.findById(activityId);
            if (activityOpt.isEmpty()) {
                return Response.error("Activity not found");
            }

            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }

            Activity activity = activityOpt.get();
            activity.setSeriesId(seriesId);
            activity.setSeriesOrder(order);
            Activity savedActivity = activityRepository.save(activity);

            // Auto-register all students who already registered any activity in this series
            ActivitySeries series = seriesOpt.get();
            if (!series.isDeleted()) {
                autoRegisterStudentsForNewActivityInSeries(series, savedActivity);
            }

            logger.info("Added activity {} to series {} with order {}", activityId, seriesId, order);
            return Response.success("Activity added to series successfully", activity);
        } catch (Exception e) {
            logger.error("Failed to add activity to series: {}", e.getMessage(), e);
            return Response.error("Failed to add activity to series: " + e.getMessage());
        }
    }

    /**
     * Auto-register all students who already have at least one registration in this
     * series
     * for the newly created/added activity.
     */
    private void autoRegisterStudentsForNewActivityInSeries(ActivitySeries series, Activity newActivity) {
        try {
            Long seriesId = series.getId();

            // Lấy tất cả activities trong series (trừ activity mới nếu cần)
            List<Activity> activitiesInSeries = activityRepository.findBySeriesIdAndIsDeletedFalse(seriesId);

            // Thu thập tất cả student đã đăng ký ít nhất 1 activity trong series
            Set<Long> studentIds = new HashSet<>();
            for (Activity activity : activitiesInSeries) {
                // Không cần bỏ qua newActivity vì tại thời điểm này activity mới chưa có đăng
                // ký
                List<ActivityRegistration> regs = registrationRepository
                        .findByActivityIdAndActivityIsDeletedFalse(activity.getId());
                for (ActivityRegistration reg : regs) {
                    if (reg.getStudent() != null && reg.getStudent().getId() != null) {
                        studentIds.add(reg.getStudent().getId());
                    }
                }
            }

            if (studentIds.isEmpty()) {
                logger.info("No existing registrations in series {} to auto-register for new activity {}", seriesId,
                        newActivity.getId());
                return;
            }

            // Load students từ IDs
            List<Student> students = studentRepository.findAllById(studentIds);

            List<ActivityRegistration> registrationsToCreate = new ArrayList<>();
            for (Student student : students) {
                // Bỏ qua nếu đã có registration cho activity mới
                if (registrationRepository.existsByActivityIdAndStudentId(newActivity.getId(), student.getId())) {
                    continue;
                }

                ActivityRegistration registration = new ActivityRegistration();
                registration.setActivity(newActivity);
                registration.setStudent(student);
                registration.setRegisteredDate(LocalDateTime.now());
                registration.setSeriesId(series.getId());
                registration.setTicketCode(java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());

                // Set status dựa trên requiresApproval của series
                if (series.isRequiresApproval()) {
                    registration.setStatus(RegistrationStatus.PENDING);
                } else {
                    registration.setStatus(RegistrationStatus.APPROVED);
                }

                registrationsToCreate.add(registration);
            }

            if (!registrationsToCreate.isEmpty()) {
                registrationRepository.saveAll(registrationsToCreate);

                // Tạo participation cho các registration có status APPROVED
                List<ActivityParticipation> participationsToCreate = new ArrayList<>();
                for (ActivityRegistration registration : registrationsToCreate) {
                    if (registration.getStatus() == RegistrationStatus.APPROVED) {
                        // Kiểm tra xem đã có participation chưa
                        if (!participationRepository.existsByRegistration(registration)) {
                            ActivityParticipation participation = new ActivityParticipation();
                            participation.setRegistration(registration);
                            participation.setParticipationType(ParticipationType.REGISTERED);
                            participation.setPointsEarned(BigDecimal.ZERO);
                            participation.setDate(LocalDateTime.now());
                            participationsToCreate.add(participation);
                        }
                    }
                }

                if (!participationsToCreate.isEmpty()) {
                    participationRepository.saveAll(participationsToCreate);
                    logger.info("Created {} participations for auto-registered students",
                            participationsToCreate.size());
                }

                logger.info(
                        "Auto-registered {} students for new activity {} in series {} based on existing series registrations",
                        registrationsToCreate.size(), newActivity.getId(), seriesId);
            } else {
                logger.info("No students needed auto-registration for new activity {} in series {}",
                        newActivity.getId(),
                        seriesId);
            }
        } catch (Exception e) {
            logger.error("Failed to auto-register students for new activity in series {}: {}", series.getId(),
                    e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Response updateStudentProgress(Long studentId, Long activityId) {
        try {
            Optional<Activity> activityOpt = activityRepository.findById(activityId);
            if (activityOpt.isEmpty()) {
                return Response.error("Activity not found");
            }

            Activity activity = activityOpt.get();
            if (activity.getSeriesId() == null) {
                return Response.success("Activity is not part of a series", null);
            }

            Optional<Student> studentOpt = studentRepository.findById(studentId);
            if (studentOpt.isEmpty()) {
                return Response.error("Student not found");
            }

            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(activity.getSeriesId());
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }

            Student student = studentOpt.get();
            ActivitySeries series = seriesOpt.get();

            // Tìm hoặc tạo progress record
            Optional<StudentSeriesProgress> progressOpt = progressRepository
                    .findByStudentIdAndSeriesId(studentId, activity.getSeriesId());

            StudentSeriesProgress progress;
            if (progressOpt.isPresent()) {
                progress = progressOpt.get();
            } else {
                progress = new StudentSeriesProgress();
                progress.setStudent(student);
                progress.setSeries(series);
                progress.setCompletedActivityIds("[]");
                progress.setCompletedCount(0);
                progress.setPointsEarned(BigDecimal.ZERO);
            }

            // Parse completed activity IDs
            String completedIdsJson = progress.getCompletedActivityIds() != null
                    ? progress.getCompletedActivityIds()
                    : "[]";
            List<Long> completedIds;
            try {
                completedIds = objectMapper.readValue(completedIdsJson, new TypeReference<List<Long>>() {
                });
            } catch (Exception e) {
                completedIds = new ArrayList<>();
            }

            // Kiểm tra xem activity đã được thêm chưa
            if (!completedIds.contains(activityId)) {
                completedIds.add(activityId);
                progress.setCompletedCount(completedIds.size());
                progress.setCompletedActivityIds(objectMapper.writeValueAsString(completedIds));
                progress.setLastUpdated(LocalDateTime.now());
                progressRepository.save(progress);

                // Tính lại milestone points
                calculateMilestonePoints(studentId, activity.getSeriesId());
            }

            logger.info("Updated progress for student {} in series {}", studentId, activity.getSeriesId());
            return Response.success("Student progress updated", progress);
        } catch (Exception e) {
            logger.error("Failed to update student progress: {}", e.getMessage(), e);
            return Response.error("Failed to update student progress: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response calculateMilestonePoints(Long studentId, Long seriesId) {
        try {
            Optional<StudentSeriesProgress> progressOpt = progressRepository
                    .findByStudentIdAndSeriesId(studentId, seriesId);
            if (progressOpt.isEmpty()) {
                return Response.error("Progress not found");
            }

            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }

            StudentSeriesProgress progress = progressOpt.get();
            ActivitySeries series = seriesOpt.get();

            if (series.getMilestonePoints() == null || series.getMilestonePoints().isEmpty()) {
                return Response.success("No milestone points configured", null);
            }

            // Parse milestone points JSON
            Map<String, Integer> milestonePoints;
            try {
                milestonePoints = objectMapper.readValue(series.getMilestonePoints(),
                        new TypeReference<Map<String, Integer>>() {
                        });
            } catch (Exception e) {
                logger.error("Failed to parse milestone points: {}", e.getMessage());
                return Response.error("Invalid milestone points format");
            }

            // Tìm điểm milestone phù hợp với số sự kiện đã tham gia
            Integer completedCount = progress.getCompletedCount();
            BigDecimal pointsToAward = BigDecimal.ZERO;

            // Tìm milestone cao nhất mà student đã đạt được
            for (Map.Entry<String, Integer> entry : milestonePoints.entrySet()) {
                try {
                    Integer milestoneCount = Integer.parseInt(entry.getKey());
                    if (completedCount >= milestoneCount) {
                        Integer milestonePointsValue = entry.getValue();
                        if (milestonePointsValue > pointsToAward.intValue()) {
                            pointsToAward = BigDecimal.valueOf(milestonePointsValue);
                        }
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Invalid milestone key: {}", entry.getKey());
                }
            }

            // Cập nhật pointsEarned nếu có thay đổi (>= thay vì > để đảm bảo cập nhật khi
            // bằng nhau)
            BigDecimal currentPointsEarned = progress.getPointsEarned() != null
                    ? progress.getPointsEarned()
                    : BigDecimal.ZERO;
            if (pointsToAward.compareTo(currentPointsEarned) >= 0) {
                BigDecimal oldPoints = currentPointsEarned;
                progress.setPointsEarned(pointsToAward);
                progressRepository.save(progress);

                // Cập nhật StudentScore (theo scoreType của series)
                updateRenLuyenScoreFromMilestone(studentId, seriesId, oldPoints, pointsToAward);

                logger.info("Awarded milestone points {} (was {}) to student {} for series {}",
                        pointsToAward, oldPoints, studentId, seriesId);
            } else {
                logger.warn(
                        "Milestone points {} is less than current points {} for student {} in series {}. Skipping update.",
                        pointsToAward, currentPointsEarned, studentId, seriesId);
            }

            return Response.success("Milestone points calculated", progress);
        } catch (Exception e) {
            logger.error("Failed to calculate milestone points: {}", e.getMessage(), e);
            return Response.error("Failed to calculate milestone points: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response checkMinimumRequirement(Long studentId, Long seriesId) {
        // TODO: Implement penalty logic if student doesn't meet minimum requirement
        // This would need additional fields in ActivitySeries: minimumRequired,
        // penaltyPoints
        return Response.success("Minimum requirement check not yet implemented", null);
    }

    /**
     * Cập nhật điểm từ milestone (dùng scoreType từ series)
     */
    private void updateRenLuyenScoreFromMilestone(Long studentId, Long seriesId, BigDecimal oldPoints,
            BigDecimal newPoints) {
        ScoreType scoreType = null;
        try {
            // Lấy series để lấy scoreType
            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                logger.warn("Series not found: {}", seriesId);
                return;
            }
            ActivitySeries series = seriesOpt.get();
            scoreType = series.getScoreType();

            // Lấy semester hiện tại
            Semester currentSemester = semesterRepository.findAll().stream()
                    .filter(Semester::isOpen)
                    .findFirst()
                    .orElse(semesterRepository.findAll().stream().findFirst().orElse(null));

            if (currentSemester == null) {
                logger.warn("No semester found for milestone score update");
                return;
            }

            Optional<StudentScore> scoreOpt = studentScoreRepository
                    .findByStudentIdAndSemesterIdAndScoreType(studentId, currentSemester.getId(), scoreType);

            if (scoreOpt.isEmpty()) {
                logger.warn("No {} score record found for student {}", scoreType, studentId);
                return;
            }

            StudentScore score = scoreOpt.get();
            BigDecimal oldTotalScore = score.getScore() != null ? score.getScore() : BigDecimal.ZERO;

            // QUAN TRỌNG: Logic tính điểm milestone - tính theo mốc cuối đạt, KHÔNG cộng
            // dồn
            // Ví dụ: Mốc 1 = 5đ, Mốc 2 = 10đ
            // - Đạt mốc 1 → tổng = 5đ
            // - Đạt mốc 2 → tổng = 10đ (KHÔNG phải 5+10=15đ)
            //
            // Công thức: newTotal = (oldTotal - oldMilestone) + newMilestone
            // oldTotal đã bao gồm: participations (từ activities đơn lẻ) + oldMilestone
            // newTotal sẽ là: participations (từ activities đơn lẻ) + newMilestone

            // Sử dụng oldPoints đã truyền vào (không lấy lại từ progress vì đã được cập
            // nhật)
            BigDecimal oldMilestonePoints = oldPoints != null ? oldPoints : BigDecimal.ZERO;

            // Tổng điểm MỚI = (tổng điểm cũ - milestone cũ) + milestone mới
            // Đảm bảo không cộng dồn milestone
            BigDecimal updatedScore = oldTotalScore.subtract(oldMilestonePoints).add(newPoints);

            // Đảm bảo điểm không âm
            if (updatedScore.compareTo(BigDecimal.ZERO) < 0) {
                logger.warn("Calculated score is negative: {}. Setting to 0.", updatedScore);
                updatedScore = BigDecimal.ZERO;
            }
            score.setScore(updatedScore);
            studentScoreRepository.save(score);

            // Tạo history
            User systemUser = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == vn.campuslife.enumeration.Role.ADMIN
                            || user.getRole() == vn.campuslife.enumeration.Role.MANAGER)
                    .findFirst()
                    .orElse(null);

            ScoreHistory history = new ScoreHistory();
            history.setScore(score);
            history.setOldScore(oldTotalScore);
            history.setNewScore(updatedScore);
            history.setChangedBy(systemUser != null ? systemUser : userRepository.findById(1L).orElse(null));
            history.setChangeDate(LocalDateTime.now());
            history.setReason("Milestone points from series: " + seriesId);
            scoreHistoryRepository.save(history);

            logger.info("Updated {} score from milestone: {} -> {} for student {} (oldMilestone: {}, newMilestone: {})",
                    scoreType, oldTotalScore, updatedScore, studentId, oldMilestonePoints, newPoints);
        } catch (Exception e) {
            logger.error("Failed to update score from milestone for student {} in series {} (scoreType: {}): {}",
                    studentId, seriesId, scoreType, e.getMessage(), e);
            // Không throw exception để không làm gián đoạn flow chính
            // Nhưng log đầy đủ để debug
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getAllSeries() {
        try {
            List<ActivitySeries> seriesList = seriesRepository.findByIsDeletedFalse();
            return Response.success("Series retrieved successfully", seriesList);
        } catch (Exception e) {
            logger.error("Failed to get all series: {}", e.getMessage(), e);
            return Response.error("Failed to get all series: " + e.getMessage());
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Response getSeriesById(Long seriesId) {
        try {
            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }
            return Response.success("Series retrieved successfully", seriesOpt.get());
        } catch (Exception e) {
            logger.error("Failed to get series: {}", e.getMessage(), e);
            return Response.error("Failed to get series: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getActivitiesInSeries(Long seriesId) {
        try {
            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }

            List<Activity> activities = activityRepository.findBySeriesIdAndIsDeletedFalse(seriesId);
            // Sắp xếp theo seriesOrder
            activities.sort((a1, a2) -> {
                Integer order1 = a1.getSeriesOrder() != null ? a1.getSeriesOrder() : Integer.MAX_VALUE;
                Integer order2 = a2.getSeriesOrder() != null ? a2.getSeriesOrder() : Integer.MAX_VALUE;
                return order1.compareTo(order2);
            });

            return Response.success("Activities in series retrieved successfully", activities);
        } catch (Exception e) {
            logger.error("Failed to get activities in series: {}", e.getMessage(), e);
            return Response.error("Failed to get activities in series: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getStudentProgress(Long seriesId, Long studentId) {
        try {
            // Validate series exists
            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }

            // Validate student exists
            Optional<Student> studentOpt = studentRepository.findById(studentId);
            if (studentOpt.isEmpty()) {
                return Response.error("Student not found");
            }

            ActivitySeries series = seriesOpt.get();

            // Get or create progress (if student hasn't registered, return empty progress)
            Optional<StudentSeriesProgress> progressOpt = progressRepository
                    .findByStudentIdAndSeriesId(studentId, seriesId);

            // Parse completed activity IDs
            List<Long> completedActivityIds = new ArrayList<>();
            Integer completedCount = 0;
            BigDecimal pointsEarned = BigDecimal.ZERO;
            LocalDateTime lastUpdated = null;

            if (progressOpt.isPresent()) {
                StudentSeriesProgress progress = progressOpt.get();
                completedCount = progress.getCompletedCount();
                pointsEarned = progress.getPointsEarned();
                lastUpdated = progress.getLastUpdated();

                // Parse completed activity IDs JSON
                String completedIdsJson = progress.getCompletedActivityIds();
                if (completedIdsJson != null && !completedIdsJson.isEmpty()) {
                    try {
                        completedActivityIds = objectMapper.readValue(completedIdsJson,
                                new TypeReference<List<Long>>() {
                                });
                    } catch (Exception e) {
                        logger.warn("Failed to parse completedActivityIds: {}", completedIdsJson, e);
                        completedActivityIds = new ArrayList<>();
                    }
                }
            }

            // Parse milestone points to determine current milestone
            Map<String, Integer> milestonePoints = null;
            String currentMilestone = null;
            Integer nextMilestoneCount = null;
            Integer nextMilestonePoints = null;

            if (series.getMilestonePoints() != null && !series.getMilestonePoints().isEmpty()) {
                try {
                    milestonePoints = objectMapper.readValue(series.getMilestonePoints(),
                            new TypeReference<Map<String, Integer>>() {
                            });

                    // Find current milestone
                    int maxMilestone = 0;
                    for (Map.Entry<String, Integer> entry : milestonePoints.entrySet()) {
                        int milestoneCount = Integer.parseInt(entry.getKey());
                        if (completedCount >= milestoneCount && milestoneCount > maxMilestone) {
                            maxMilestone = milestoneCount;
                            currentMilestone = entry.getKey();
                        }
                    }

                    // Find next milestone
                    for (Map.Entry<String, Integer> entry : milestonePoints.entrySet()) {
                        int milestoneCount = Integer.parseInt(entry.getKey());
                        if (milestoneCount > completedCount) {
                            nextMilestoneCount = milestoneCount;
                            nextMilestonePoints = entry.getValue();
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse milestonePoints: {}", series.getMilestonePoints(), e);
                }
            }

            // Get total activities in series
            List<Activity> allActivities = activityRepository.findBySeriesIdAndIsDeletedFalse(seriesId);
            Integer totalActivities = allActivities.size();

            // Build response map
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("studentId", studentId);
            responseData.put("seriesId", seriesId);
            responseData.put("seriesName", series.getName());
            responseData.put("completedCount", completedCount);
            responseData.put("totalActivities", totalActivities);
            responseData.put("completedActivityIds", completedActivityIds);
            responseData.put("pointsEarned", pointsEarned);
            responseData.put("lastUpdated", lastUpdated);
            responseData.put("currentMilestone", currentMilestone);
            responseData.put("nextMilestoneCount", nextMilestoneCount);
            responseData.put("nextMilestonePoints", nextMilestonePoints);
            responseData.put("milestonePoints", milestonePoints);
            responseData.put("scoreType", series.getScoreType());

            return Response.success("Student progress retrieved successfully", responseData);
        } catch (Exception e) {
            logger.error("Failed to get student progress: {}", e.getMessage(), e);
            return Response.error("Failed to get student progress: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response checkSeriesRegistration(Long seriesId, Long studentId) {
        try {
            // Validate series exists
            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }

            // Validate student exists
            Optional<Student> studentOpt = studentRepository.findById(studentId);
            if (studentOpt.isEmpty()) {
                return Response.error("Student not found");
            }

            boolean isRegistered = registrationRepository.existsBySeriesIdAndStudentId(seriesId, studentId);

            Map<String, Object> data = new HashMap<>();
            data.put("seriesId", seriesId);
            data.put("studentId", studentId);
            data.put("isRegistered", isRegistered);

            return Response.success("Series registration status retrieved", data);
        } catch (Exception e) {
            logger.error("Failed to check series registration: {}", e.getMessage(), e);
            return Response.error("Failed to check series registration: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response updateSeries(Long seriesId, String name, String description, String milestonePointsJson,
            vn.campuslife.enumeration.ScoreType scoreType, Long mainActivityId,
            LocalDateTime registrationStartDate, LocalDateTime registrationDeadline,
            Boolean requiresApproval, Integer ticketQuantity) {
        try {
            // Find series
            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }

            ActivitySeries series = seriesOpt.get();

            // Check if series is deleted
            if (series.isDeleted()) {
                return Response.error("Series has been deleted");
            }

            // Validate required fields
            if (name != null && name.trim().isEmpty()) {
                return Response.error("Series name cannot be empty");
            }
            if (scoreType == null) {
                return Response.error("ScoreType is required");
            }

            // Update fields (only if provided)
            if (name != null) {
                series.setName(name.trim());
            }
            if (description != null) {
                series.setDescription(description);
            }
            if (milestonePointsJson != null) {
                // Validate JSON format
                try {
                    objectMapper.readValue(milestonePointsJson, new TypeReference<Map<String, Integer>>() {
                    });
                    series.setMilestonePoints(milestonePointsJson);
                } catch (Exception e) {
                    logger.error("Invalid milestonePoints JSON format: {}", milestonePointsJson, e);
                    return Response.error("Invalid milestonePoints JSON format");
                }
            }
            if (scoreType != null) {
                series.setScoreType(scoreType);
            }
            if (mainActivityId != null) {
                Optional<Activity> mainActivityOpt = activityRepository.findById(mainActivityId);
                if (mainActivityOpt.isPresent()) {
                    series.setMainActivity(mainActivityOpt.get());
                } else {
                    logger.warn("Main activity not found: {}", mainActivityId);
                    return Response.error("Main activity not found: " + mainActivityId);
                }
            }
            // Note: If mainActivityId is null, we don't update it (keep existing value)
            if (registrationStartDate != null) {
                series.setRegistrationStartDate(registrationStartDate);
            }
            if (registrationDeadline != null) {
                series.setRegistrationDeadline(registrationDeadline);
            }
            if (requiresApproval != null) {
                series.setRequiresApproval(requiresApproval);
            }
            if (ticketQuantity != null) {
                series.setTicketQuantity(ticketQuantity);
            }

            ActivitySeries saved = seriesRepository.save(series);
            logger.info("Updated activity series: {} with scoreType: {}", saved.getId(), saved.getScoreType());
            return Response.success("Activity series updated successfully", saved);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument when updating series: {}", e.getMessage(), e);
            return Response.error("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to update series: {}", e.getMessage(), e);
            return Response.error("Failed to update series: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response deleteSeries(Long seriesId) {
        try {
            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }

            ActivitySeries series = seriesOpt.get();
            if (series.isDeleted()) {
                return Response.error("Series already deleted");
            }

            // Find all activities in this series (including already deleted ones)
            List<Activity> activities = activityRepository.findBySeriesIdAndIsDeletedFalse(seriesId);

            // Soft delete all activities in the series
            int deletedActivitiesCount = 0;
            for (Activity activity : activities) {
                if (!activity.isDeleted()) {
                    activity.setDeleted(true);
                    activityRepository.save(activity);
                    deletedActivitiesCount++;
                }
            }

            // Soft delete the series
            series.setDeleted(true);
            seriesRepository.save(series);

            logger.info("Deleted activity series: {} and {} activities", seriesId, deletedActivitiesCount);
            return Response.success(
                    String.format("Activity series deleted successfully. %d activities also deleted.",
                            deletedActivitiesCount),
                    null);
        } catch (Exception e) {
            logger.error("Failed to delete series: {}", e.getMessage(), e);
            return Response.error("Failed to delete series: " + e.getMessage());
        }
    }

}
