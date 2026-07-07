package vn.campuslife.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.ActivitySeries;
import vn.campuslife.entity.Department;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.StudentSeriesProgress;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.series.SeriesResponse;
import vn.campuslife.model.activity.series.SeriesOverviewResponse;
import vn.campuslife.model.activity.series.SeriesProgressItemResponse;
import vn.campuslife.model.activity.series.SeriesProgressListResponse;
import vn.campuslife.repository.ActivityParticipationRepository;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.ActivitySeriesRepository;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.StudentSeriesProgressRepository;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.security.department.DepartmentScopeSpec;
import vn.campuslife.service.ActivityRegistrationAutoService;
import vn.campuslife.service.ActivitySeriesService;
import vn.campuslife.service.ReminderScheduleService;
import vn.campuslife.service.ScoreRuleEngine;
import vn.campuslife.service.SemesterHelperService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final ActivityParticipationRepository participationRepository;
    private final ActivityRegistrationRepository registrationRepository;
    private final DepartmentRepository departmentRepository;
    private final SemesterHelperService semesterHelperService;
    private final ScoreRuleEngine scoreRuleEngine;
    private final ReminderScheduleService reminderScheduleService;
    private final ActivityRegistrationAutoService autoRegisterService;
    private final vn.campuslife.repository.SemesterRepository semesterRepository;
    private final vn.campuslife.service.validator.SeriesChildActivityValidator seriesChildValidator;
    private final vn.campuslife.service.mapper.SeriesChildActivityMapper seriesChildMapper;
    private final DepartmentAuthorizationService departmentAuthorizationService;

    @Override
    @Transactional
    public Response createSeries(String name, String description, String milestonePointsJson,
            vn.campuslife.enumeration.ScoreType scoreType, Long mainActivityId,
            LocalDateTime registrationStartDate, LocalDateTime registrationDeadline,
            Boolean requiresApproval, Integer ticketQuantity,
            Boolean minimumRequirementEnabled, Integer minimumRequiredEvents, Integer minimumPenaltyPoints, Long targetSemesterId,
            vn.campuslife.enumeration.ScoreRuleAudience audience, java.util.List<Long> departmentIds,
            Boolean isImportant, Boolean mandatoryForFacultyStudents,
            Boolean isDraft,
            vn.campuslife.enumeration.SeriesPresetCode presetCode) {
        // Validate required fields
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Series name is required");
        }
        if (scoreType == null) {
            throw new IllegalArgumentException("ScoreType is required");
        }
        validateMinimumRequirementConfig(minimumRequirementEnabled, minimumRequiredEvents, minimumPenaltyPoints);

        ActivitySeries series = new ActivitySeries();
        series.setName(name);
        series.setDescription(description);
        series.setMilestonePoints(milestonePointsJson);
        series.setScoreType(scoreType);
        series.setRegistrationStartDate(registrationStartDate);
        series.setRegistrationDeadline(registrationDeadline);
        series.setRequiresApproval(requiresApproval != null ? requiresApproval : true);
        series.setTicketQuantity(ticketQuantity);
        series.setMinimumRequirementEnabled(Boolean.TRUE.equals(minimumRequirementEnabled));
        series.setMinimumRequiredEvents(minimumRequiredEvents);
        series.setMinimumPenaltyPoints(minimumPenaltyPoints);
        if (targetSemesterId != null) {
            semesterRepository.findById(targetSemesterId).ifPresent(series::setTargetSemester);
        }
        series.setAudience(audience != null ? audience : vn.campuslife.enumeration.ScoreRuleAudience.ALL_PARTICIPANTS);
        if (departmentIds != null && !departmentIds.isEmpty()) {
            java.util.List<Department> depts = departmentRepository.findAllById(departmentIds);
            series.setTargetDepartments(new LinkedHashSet<>(depts));
        }
        series.setCreatedAt(LocalDateTime.now());
        series.setDeleted(false); // Set default value for isDeleted
series.setPresetCode(presetCode);
        series.setImportant(Boolean.TRUE.equals(isImportant));
        series.setMandatoryForFacultyStudents(Boolean.TRUE.equals(mandatoryForFacultyStudents));
        series.setDraft(Boolean.TRUE.equals(isDraft));

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

        // Auto-register students to main activity if series flags require it (only when non-draft)
        if (!saved.isDraft() && saved.getMainActivity() != null) {
            autoRegisterService.autoRegisterStudents(
                    saved.getMainActivity(),
                    saved.isImportant(),
                    saved.isMandatoryForFacultyStudents(),
                    saved.getTargetDepartments());
        }

        return Response.success("Activity series created successfully", toSeriesResponse(saved));
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

        activity.setRegistrationStartDate(series.getRegistrationStartDate()); // Lấy từ series
        activity.setRegistrationDeadline(series.getRegistrationDeadline()); // Lấy từ series
        activity.setRequiresApproval(series.isRequiresApproval()); // Lấy từ series
        activity.setTicketQuantity(series.getTicketQuantity()); // Lấy từ series
        activity.setImportant(false); // Không cần
        activity.setMandatoryForFacultyStudents(false); // Không cần

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
        reminderScheduleService.syncSeriesMinimumRequirementReminders(series);

        return Response.success("Activity created in series successfully", seriesChildMapper.toResponse(saved, series.getName()));
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

            // Kiểm tra ticketQuantity (đếm số student APPROVED trong series)
            if (series.getTicketQuantity() != null) {
                long approvedCount = registrationRepository.countDistinctStudentBySeriesIdAndStatus(
                        seriesId, RegistrationStatus.APPROVED);
                if (approvedCount >= series.getTicketQuantity()) {
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

            if (registrations.stream().anyMatch(reg -> reg.getStatus() == RegistrationStatus.APPROVED)) {
                reminderScheduleService.syncSeriesMinimumRequirementReminder(series, student);
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
    public Response registerForSeriesWaitlist(Long seriesId, Long studentId) {
        try {
            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }
            ActivitySeries series = seriesOpt.get();

            Optional<Student> studentOpt = studentRepository.findById(studentId);
            if (studentOpt.isEmpty()) {
                return Response.error("Student not found");
            }
            Student student = studentOpt.get();

            if (series.getRegistrationDeadline() != null &&
                    LocalDateTime.now().isAfter(series.getRegistrationDeadline())) {
                return Response.error("Registration deadline has passed");
            }

            if (registrationRepository.existsBySeriesIdAndStudentId(seriesId, studentId)) {
                return Response.error("Already registered or in waitlist for this series");
            }

            // Only allow waitlist if series is full
            if (series.getTicketQuantity() != null) {
                long approvedCount = registrationRepository.countDistinctStudentBySeriesIdAndStatus(
                        seriesId, RegistrationStatus.APPROVED);
                if (approvedCount < series.getTicketQuantity()) {
                    return Response.error("Series still has slots. Please register normally.");
                }
            } else {
                return Response.error("Series has unlimited slots. Please register normally.");
            }

            List<Activity> activities = activityRepository.findBySeriesIdAndIsDeletedFalse(seriesId);
            if (activities.isEmpty()) {
                return Response.error("No activities found in series");
            }

            List<ActivityRegistration> registrations = new ArrayList<>();
            for (Activity activity : activities) {
                if (registrationRepository.existsByActivityIdAndStudentId(activity.getId(), studentId)) {
                    continue;
                }
                ActivityRegistration registration = new ActivityRegistration();
                registration.setActivity(activity);
                registration.setStudent(student);
                registration.setRegisteredDate(LocalDateTime.now());
                registration.setSeriesId(seriesId);
                registration.setTicketCode(java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                registration.setStatus(RegistrationStatus.WAITLIST);
                registrations.add(registration);
            }

            if (registrations.isEmpty()) {
                return Response.error("Already registered for all activities in series");
            }

            registrationRepository.saveAll(registrations);
            return Response.success("Successfully joined series waitlist", registrations);
        } catch (Exception e) {
            logger.error("Failed to join series waitlist: {}", e.getMessage(), e);
            return Response.error("Failed to join series waitlist: " + e.getMessage());
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
                reminderScheduleService.syncSeriesMinimumRequirementReminders(series);
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
            if (series.isDraft()) {
                logger.info("Skipping auto-registration for activity in draft series (seriesId={})", series.getId());
                return;
            }

            // First: propagate existing series registrations (students who registered
            // for any sibling activity) to the new activity.
            Long seriesId = series.getId();

            // Thu thập tất cả student đã đăng ký ít nhất 1 activity trong series
            List<ActivityRegistration> allRegistrations = registrationRepository.findBySeriesId(seriesId);
            Set<Long> studentIds = allRegistrations.stream()
                    .filter(reg -> reg.getStudent() != null && reg.getStudent().getId() != null)
                    .map(reg -> reg.getStudent().getId())
                    .collect(Collectors.toSet());

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

            // Second: if the series itself is marked isImportant or mandatoryForFacultyStudents,
            // auto-register ALL/faculty students to the new activity (regardless of whether
            // they already have a registration in the series — they may not if the series
            // was created without a main activity or the main activity was added later).
            if (series.isImportant() || series.isMandatoryForFacultyStudents()) {
                autoRegisterService.autoRegisterStudents(
                        newActivity,
                        series.isImportant(),
                        series.isMandatoryForFacultyStudents(),
                        series.getTargetDepartments());
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
                progress = progressRepository.save(progress);

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

            scoreRuleEngine.applySeriesMilestone(progress, progress.getStudent().getUser());
            progress = progressRepository.findByStudentIdAndSeriesId(studentId, seriesId).orElse(progress);
            logger.info("Applied series milestone via engine for student {} in series {}", studentId, seriesId);

            return Response.success("Milestone points calculated", progress);
        } catch (Exception e) {
            logger.error("Failed to calculate milestone points: {}", e.getMessage(), e);
            return Response.error("Failed to calculate milestone points: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response checkMinimumRequirement(Long studentId, Long seriesId) {
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

            if (!series.isMinimumRequirementEnabled()) {
                return Response.success("Series minimum requirement is disabled", null);
            }

            int completedCount = progressRepository.findByStudentIdAndSeriesId(studentId, seriesId)
                    .map(StudentSeriesProgress::getCompletedCount)
                    .orElse(0);

            scoreRuleEngine.applySeriesMinimumRequirement(series, student, completedCount, student.getUser());

            Map<String, Object> result = new HashMap<>();
            result.put("studentId", studentId);
            result.put("seriesId", seriesId);
            result.put("completedCount", completedCount);
            result.put("minimumRequiredEvents", series.getMinimumRequiredEvents());
            result.put("minimumPenaltyPoints", series.getMinimumPenaltyPoints());
            result.put("minimumRequirementMet",
                    series.getMinimumRequiredEvents() == null || completedCount >= series.getMinimumRequiredEvents());
            return Response.success("Series minimum requirement checked", result);
        } catch (Exception e) {
            logger.error("Failed to check series minimum requirement: {}", e.getMessage(), e);
            return Response.error("Failed to check series minimum requirement: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getAllSeries() {
        return getAllSeriesInternal(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getAllSeries(DepartmentScope scope) {
        return getAllSeriesInternal(scope);
    }

    private Response getAllSeriesInternal(DepartmentScope scope) {
        try {
            List<ActivitySeries> seriesList = scope != null && scope.manager() && !scope.admin()
                    ? seriesRepository.findAll(DepartmentScopeSpec.activitySeries(scope.departmentIds()))
                    : seriesRepository.findByIsDeletedFalse();

            // Thêm totalActivities vào mỗi series
            List<Map<String, Object>> seriesWithCount = seriesList.stream()
                    .map(series -> {
                        Map<String, Object> seriesMap = new HashMap<>();
                        seriesMap.put("id", series.getId());
                        seriesMap.put("name", series.getName());
                        seriesMap.put("description", series.getDescription());
                        seriesMap.put("milestonePoints", series.getMilestonePoints());
                        seriesMap.put("scoreType", series.getScoreType());
                        seriesMap.put("mainActivity", series.getMainActivity());
                        seriesMap.put("targetSemesterId", series.getTargetSemester() != null ? series.getTargetSemester().getId() : null);
                        seriesMap.put("registrationStartDate", series.getRegistrationStartDate());
                        seriesMap.put("registrationDeadline", series.getRegistrationDeadline());
                        seriesMap.put("requiresApproval", series.isRequiresApproval());
                        seriesMap.put("ticketQuantity", series.getTicketQuantity());
                        seriesMap.put("minimumRequirementEnabled", series.isMinimumRequirementEnabled());
                        seriesMap.put("minimumRequiredEvents", series.getMinimumRequiredEvents());
                        seriesMap.put("minimumPenaltyPoints", series.getMinimumPenaltyPoints());
                        seriesMap.put("createdAt", series.getCreatedAt());
                        seriesMap.put("isDeleted", series.isDeleted());
                        seriesMap.put("isImportant", series.isImportant());
                        seriesMap.put("mandatoryForFacultyStudents", series.isMandatoryForFacultyStudents());
                        seriesMap.put("isDraft", series.isDraft());

                        // Đếm số activities trong series
                        Long totalActivities = activityRepository.countBySeriesId(series.getId());
                        seriesMap.put("totalActivities", totalActivities != null ? totalActivities.intValue() : 0);

                        return seriesMap;
                    })
                    .collect(Collectors.toList());

            return Response.success("Series retrieved successfully", seriesWithCount);
        } catch (Exception e) {
            logger.error("Failed to get all series: {}", e.getMessage(), e);
            return Response.error("Failed to get all series: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getSeriesById(Long seriesId) {
        return getSeriesByIdInternal(seriesId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getSeriesById(Long seriesId, DepartmentScope scope) {
        return getSeriesByIdInternal(seriesId, scope);
    }

    private Response getSeriesByIdInternal(Long seriesId, DepartmentScope scope) {
        try {
            if (scope != null && scope.manager() && !scope.admin()) {
                departmentAuthorizationService.requireSeriesAccess(seriesId, scope);
            }
            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }
            return Response.success("Series retrieved successfully", toSeriesResponse(seriesOpt.get()));
        } catch (Exception e) {
            logger.error("Failed to get series: {}", e.getMessage(), e);
            return Response.error("Failed to get series: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getActivitiesInSeries(Long seriesId) {
        return getActivitiesInSeriesInternal(seriesId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getActivitiesInSeries(Long seriesId, DepartmentScope scope) {
        return getActivitiesInSeriesInternal(seriesId, scope);
    }

    private Response getActivitiesInSeriesInternal(Long seriesId, DepartmentScope scope) {
        try {
            if (scope != null && scope.manager() && !scope.admin()) {
                departmentAuthorizationService.requireSeriesAccess(seriesId, scope);
            }
            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }
            ActivitySeries series = seriesOpt.get();

            List<Activity> activities = activityRepository.findBySeriesIdAndIsDeletedFalse(seriesId);
            activities.sort((a1, a2) -> {
                Integer order1 = a1.getSeriesOrder() != null ? a1.getSeriesOrder() : Integer.MAX_VALUE;
                Integer order2 = a2.getSeriesOrder() != null ? a2.getSeriesOrder() : Integer.MAX_VALUE;
                return order1.compareTo(order2);
            });

            List<vn.campuslife.model.activity.series.SeriesChildActivityResponse> responses = activities.stream()
                .map(a -> seriesChildMapper.toResponse(a, series.getName()))
                .collect(Collectors.toList());

            return Response.success("Activities in series retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to get activities in series: {}", e.getMessage(), e);
            return Response.error("Failed to get activities in series: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response createSeriesActivity(Long seriesId, vn.campuslife.model.activity.series.SeriesChildActivityCreateRequest request) {
        try {
            seriesChildValidator.validate(request, seriesId);
            ActivitySeries series = seriesRepository.findById(seriesId).orElseThrow();
            
            Set<Department> organizers = resolveOrganizers(request.getOrganizerIds());
            Activity entity = seriesChildMapper.toEntity(request, series);
            entity.setOrganizers(organizers);
            
            Activity saved = activityRepository.save(entity);
            if (saved.getCheckInCode() == null || saved.getCheckInCode().isBlank()) {
                String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase().replace("-", "");
                String checkInCode = String.format("ACT-%06d-%s", saved.getId(), random);
                saved.setCheckInCode(checkInCode);
                saved = activityRepository.save(saved);
            }
            return Response.success("Series activity created successfully", seriesChildMapper.toResponse(saved, series.getName()));
        } catch (IllegalArgumentException e) {
            return Response.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to create series activity: {}", e.getMessage(), e);
            return Response.error("Failed to create series activity");
        }
    }

    @Override
    @Transactional
    public Response updateSeriesActivity(Long seriesId, Long activityId, vn.campuslife.model.activity.series.SeriesChildActivityUpdateRequest request) {
        try {
            ActivitySeries series = seriesRepository.findById(seriesId).orElseThrow(() -> new IllegalArgumentException("Series not found"));
            Activity activity = activityRepository.findByIdAndIsDeletedFalse(activityId).orElseThrow(() -> new IllegalArgumentException("Activity not found"));
            
            if (!activity.getSeriesId().equals(seriesId)) {
                return Response.error("Activity does not belong to this series");
            }
            
            seriesChildMapper.applyUpdate(activity, request);
            
            if (request.getOrganizerIds() != null && !request.getOrganizerIds().isEmpty()) {
                activity.setOrganizers(resolveOrganizers(request.getOrganizerIds()));
            }
            
            Activity saved = activityRepository.save(activity);
            return Response.success("Series activity updated successfully", seriesChildMapper.toResponse(saved, series.getName()));
        } catch (IllegalArgumentException e) {
            return Response.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to update series activity: {}", e.getMessage(), e);
            return Response.error("Failed to update series activity");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getSeriesActivity(Long seriesId, Long activityId) {
        try {
            ActivitySeries series = seriesRepository.findById(seriesId).orElseThrow(() -> new IllegalArgumentException("Series not found"));
            Activity activity = activityRepository.findByIdAndIsDeletedFalse(activityId).orElseThrow(() -> new IllegalArgumentException("Activity not found"));
            
            if (!activity.getSeriesId().equals(seriesId)) {
                return Response.error("Activity does not belong to this series");
            }
            
            return Response.success("Series activity retrieved successfully", seriesChildMapper.toResponse(activity, series.getName()));
        } catch (IllegalArgumentException e) {
            return Response.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to get series activity: {}", e.getMessage(), e);
            return Response.error("Failed to get series activity");
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
            responseData.put("minimumRequirementEnabled", series.isMinimumRequirementEnabled());
            responseData.put("minimumRequiredEvents", series.getMinimumRequiredEvents());
            responseData.put("minimumPenaltyPoints", series.getMinimumPenaltyPoints());
            responseData.put("minimumRequirementMet",
                    !series.isMinimumRequirementEnabled()
                            || series.getMinimumRequiredEvents() == null
                            || completedCount >= series.getMinimumRequiredEvents());
            responseData.put("remainingToAvoidPenalty",
                    !series.isMinimumRequirementEnabled() || series.getMinimumRequiredEvents() == null
                            ? 0
                            : Math.max(series.getMinimumRequiredEvents() - completedCount, 0));

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

            ActivitySeries series = seriesOpt.get();
            boolean isRegistered = registrationRepository.existsBySeriesIdAndStudentId(seriesId, studentId);

            boolean canCancel = true;
            String cancelReason = null;
            if (!isRegistered) {
                canCancel = false;
                cancelReason = "Bạn chưa đăng ký chuỗi sự kiện này.";
            } else if (series.isImportant()) {
                canCancel = false;
                cancelReason = "Không thể huỷ đăng ký chuỗi sự kiện quan trọng.";
            } else if (series.isMandatoryForFacultyStudents()) {
                canCancel = false;
                cancelReason = "Không thể huỷ đăng ký chuỗi bắt buộc cho sinh viên khoa.";
            } else {
                List<ActivityRegistration> seriesRegs = registrationRepository.findBySeriesIdAndStudentId(seriesId, studentId);
                for (ActivityRegistration reg : seriesRegs) {
                    if (reg.getStatus() == RegistrationStatus.ATTENDED) {
                        canCancel = false;
                        cancelReason = "Không thể huỷ vì bạn đã tham gia sự kiện '" + reg.getActivity().getName() + "'.";
                        break;
                    }
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("seriesId", seriesId);
            data.put("studentId", studentId);
            data.put("isRegistered", isRegistered);
            data.put("canCancel", canCancel);
            data.put("cancelReason", cancelReason);

            return Response.success("Series registration status retrieved", data);
        } catch (Exception e) {
            logger.error("Failed to check series registration: {}", e.getMessage(), e);
            return Response.error("Failed to check series registration: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getSeriesProgress(Long seriesId, Integer page, Integer size, String keyword) {
        try {
            // Validate series exists
            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }

            ActivitySeries series = seriesOpt.get();

            // Get total activities in series
            List<Activity> allActivities = activityRepository.findBySeriesIdAndIsDeletedFalse(seriesId);
            Integer totalActivities = allActivities.size();

            // Get all registered students (distinct)
            List<ActivityRegistration> allRegistrations = registrationRepository.findBySeriesId(seriesId);
            Set<Long> registeredStudentIds = allRegistrations.stream()
                    .map(reg -> reg.getStudent().getId())
                    .collect(Collectors.toSet());
            Long totalRegistered = (long) registeredStudentIds.size();

            // Parse milestone points
            Map<String, Integer> milestonePoints = null;
            if (series.getMilestonePoints() != null && !series.getMilestonePoints().isEmpty()) {
                try {
                    milestonePoints = objectMapper.readValue(series.getMilestonePoints(),
                            new TypeReference<Map<String, Integer>>() {
                            });
                } catch (Exception e) {
                    logger.warn("Failed to parse milestonePoints: {}", series.getMilestonePoints(), e);
                }
            }

            // Setup pagination
            if (page == null || page < 0) {
                page = 0;
            }
            if (size == null || size < 1) {
                size = 20;
            }
            Pageable pageable = PageRequest.of(page, size);

            // Query progress with or without keyword
            Page<StudentSeriesProgress> progressPage;
            if (keyword != null && !keyword.trim().isEmpty()) {
                progressPage = progressRepository.findBySeriesIdAndStudentNameOrCode(seriesId, keyword.trim(),
                        pageable);
            } else {
                progressPage = progressRepository.findBySeriesId(seriesId, pageable);
            }

            // Map progress to DTOs
            List<SeriesProgressItemResponse> progressList = new ArrayList<>();
            Set<Long> progressStudentIds = new HashSet<>();

            for (StudentSeriesProgress progress : progressPage.getContent()) {
                progressStudentIds.add(progress.getStudent().getId());
                SeriesProgressItemResponse item = mapProgressToResponse(progress, totalActivities, milestonePoints);
                progressList.add(item);
            }

            // Add registered students without progress (if they're in the current page
            // range)
            // This is a simplified approach - in a real scenario, you might want to handle
            // this differently
            // For now, we'll only show students with progress records

            // Build response
            SeriesProgressListResponse response = new SeriesProgressListResponse();
            response.setSeriesId(seriesId);
            response.setSeriesName(series.getName());
            response.setTotalActivities(totalActivities);
            response.setTotalRegistered(totalRegistered);
            response.setProgressList(progressList);
            response.setPage(page);
            response.setSize(size);
            response.setTotalPages(progressPage.getTotalPages());
            response.setTotalElements(progressPage.getTotalElements());

            return Response.success("Series progress retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Failed to get series progress: {}", e.getMessage(), e);
            return Response.error("Failed to get series progress: " + e.getMessage());
        }
    }

    /**
     * Map StudentSeriesProgress to SeriesProgressItemResponse
     */
    private SeriesProgressItemResponse mapProgressToResponse(StudentSeriesProgress progress, Integer totalActivities,
            Map<String, Integer> milestonePoints) {
        SeriesProgressItemResponse item = new SeriesProgressItemResponse();
        Student student = progress.getStudent();

        item.setStudentId(student.getId());
        item.setStudentCode(student.getStudentCode());
        item.setStudentName(student.getFullName());
        item.setCompletedCount(progress.getCompletedCount());
        item.setTotalActivities(totalActivities);
        item.setPointsEarned(progress.getPointsEarned());
        item.setLastUpdated(progress.getLastUpdated());
        item.setIsRegistered(true); // Có progress record = đã đăng ký

        // Class info
        if (student.getStudentClass() != null) {
            item.setClassName(student.getStudentClass().getClassName());
        }

        // Department info
        if (student.getDepartment() != null) {
            item.setDepartmentName(student.getDepartment().getName());
        }

        // Parse completed activity IDs
        List<Long> completedActivityIds = new ArrayList<>();
        String completedIdsJson = progress.getCompletedActivityIds();
        if (completedIdsJson != null && !completedIdsJson.isEmpty()) {
            try {
                completedActivityIds = objectMapper.readValue(completedIdsJson,
                        new TypeReference<List<Long>>() {
                        });
            } catch (Exception e) {
                logger.warn("Failed to parse completedActivityIds: {}", completedIdsJson, e);
            }
        }
        item.setCompletedActivityIds(completedActivityIds);

        // Calculate current milestone
        String currentMilestone = null;
        if (milestonePoints != null && !milestonePoints.isEmpty()) {
            int maxMilestone = 0;
            for (Map.Entry<String, Integer> entry : milestonePoints.entrySet()) {
                int milestoneCount = Integer.parseInt(entry.getKey());
                if (progress.getCompletedCount() >= milestoneCount && milestoneCount > maxMilestone) {
                    maxMilestone = milestoneCount;
                    currentMilestone = entry.getKey();
                }
            }
        }
        item.setCurrentMilestone(currentMilestone);

        return item;
    }

    @Override
    @Transactional(readOnly = true)
    public Response getSeriesOverview(Long seriesId) {
        try {
            // Validate series exists
            Optional<ActivitySeries> seriesOpt = seriesRepository.findById(seriesId);
            if (seriesOpt.isEmpty()) {
                return Response.error("Series not found");
            }

            ActivitySeries series = seriesOpt.get();

            // Build basic info
            SeriesOverviewResponse response = new SeriesOverviewResponse();
            response.setSeriesId(series.getId());
            response.setSeriesName(series.getName());
            response.setDescription(series.getDescription());
            response.setScoreType(series.getScoreType());
            response.setMilestonePoints(series.getMilestonePoints());
            response.setRegistrationStartDate(series.getRegistrationStartDate());
            response.setRegistrationDeadline(series.getRegistrationDeadline());
            response.setRequiresApproval(series.isRequiresApproval());
            response.setTicketQuantity(series.getTicketQuantity());
            response.setMinimumRequirementEnabled(series.isMinimumRequirementEnabled());
            response.setMinimumRequiredEvents(series.getMinimumRequiredEvents());
            response.setMinimumPenaltyPoints(series.getMinimumPenaltyPoints());
            response.setCreatedAt(series.getCreatedAt());

            // Parse milestone points
            Map<String, Integer> milestonePointsMap = null;
            if (series.getMilestonePoints() != null && !series.getMilestonePoints().isEmpty()) {
                try {
                    milestonePointsMap = objectMapper.readValue(series.getMilestonePoints(),
                            new TypeReference<Map<String, Integer>>() {
                            });
                    response.setMilestonePointsMap(milestonePointsMap);
                } catch (Exception e) {
                    logger.warn("Failed to parse milestonePoints: {}", series.getMilestonePoints(), e);
                }
            }

            // Get total activities
            List<Activity> allActivities = activityRepository.findBySeriesIdAndIsDeletedFalse(seriesId);
            response.setTotalActivities(allActivities.size());

            // Get total registered students
            Long totalRegistered = seriesRepository.countStudentsBySeriesId(seriesId);
            response.setTotalRegisteredStudents(totalRegistered);

            // Get total completed students (completed all activities)
            Long totalCompleted = progressRepository.countCompletedStudentsBySeriesId(seriesId);
            response.setTotalCompletedStudents(totalCompleted);

            // Get count of students who met minimum requirement
            Integer metCount = 0;
            if (Boolean.TRUE.equals(series.isMinimumRequirementEnabled()) && series.getMinimumRequiredEvents() != null) {
                metCount = progressRepository.countStudentsMeetingRequirement(seriesId, series.getMinimumRequiredEvents());
            }
            response.setMinimumRequirementMetCount(metCount);

            // Calculate completion rate
            Double completionRate = totalRegistered > 0 ? (double) totalCompleted / totalRegistered : 0.0;
            response.setCompletionRate(completionRate);

            // Calculate total milestone points awarded
            Page<StudentSeriesProgress> progressPage = progressRepository.findBySeriesId(seriesId, Pageable.unpaged());
            List<StudentSeriesProgress> allProgress = progressPage.getContent();
            BigDecimal totalMilestonePoints = allProgress.stream()
                    .map(progress -> progress.getPointsEarned() != null ? progress.getPointsEarned() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            response.setTotalMilestonePointsAwarded(totalMilestonePoints);

            // Calculate milestone progress distribution
            List<SeriesOverviewResponse.MilestoneProgressItem> milestoneProgress = new ArrayList<>();
            if (milestonePointsMap != null && !milestonePointsMap.isEmpty()) {
                for (Map.Entry<String, Integer> entry : milestonePointsMap.entrySet()) {
                    String milestoneKey = entry.getKey();
                    Integer milestoneCount = Integer.parseInt(milestoneKey);
                    Integer milestonePointsValue = entry.getValue();

                    // Count students who have reached this milestone
                    long studentCount = allProgress.stream()
                            .filter(p -> p.getCompletedCount() >= milestoneCount)
                            .count();

                    Double percentage = totalRegistered > 0 ? (double) studentCount / totalRegistered * 100 : 0.0;

                    SeriesOverviewResponse.MilestoneProgressItem item = new SeriesOverviewResponse.MilestoneProgressItem();
                    item.setMilestoneKey(milestoneKey);
                    item.setMilestoneCount(milestoneCount);
                    item.setMilestonePoints(milestonePointsValue);
                    item.setStudentCount(studentCount);
                    item.setPercentage(percentage);
                    milestoneProgress.add(item);
                }
                // Sort by milestone count ascending
                milestoneProgress.sort((a, b) -> Integer.compare(a.getMilestoneCount(), b.getMilestoneCount()));
            }
            response.setMilestoneProgress(milestoneProgress);

            // Calculate activity statistics
            List<SeriesOverviewResponse.ActivityStatItem> activityStats = new ArrayList<>();
            for (Activity activity : allActivities) {
                Long activityId = activity.getId();

                // Count registrations
                Long registrationCount = registrationRepository.countByActivityId(activityId);

                // Count participations (COMPLETED)
                Long participationCount = participationRepository
                        .countByActivityIdAndParticipationType(activityId, ParticipationType.COMPLETED);

                Double participationRate = registrationCount > 0
                        ? (double) participationCount / registrationCount
                        : 0.0;

                SeriesOverviewResponse.ActivityStatItem item = new SeriesOverviewResponse.ActivityStatItem();
                item.setActivityId(activityId);
                item.setActivityName(activity.getName());
                item.setOrder(activity.getSeriesOrder());
                item.setRegistrationCount(registrationCount);
                item.setParticipationCount(participationCount);
                item.setParticipationRate(participationRate);
                activityStats.add(item);
            }
            // Sort by order
            activityStats.sort((a, b) -> {
                if (a.getOrder() == null && b.getOrder() == null)
                    return 0;
                if (a.getOrder() == null)
                    return 1;
                if (b.getOrder() == null)
                    return -1;
                return Integer.compare(a.getOrder(), b.getOrder());
            });
            response.setActivityStats(activityStats);

            return Response.success("Series overview retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Failed to get series overview: {}", e.getMessage(), e);
            return Response.error("Failed to get series overview: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response updateSeries(Long seriesId, String name, String description, String milestonePointsJson,
            vn.campuslife.enumeration.ScoreType scoreType, Long mainActivityId,
            LocalDateTime registrationStartDate, LocalDateTime registrationDeadline,
            Boolean requiresApproval, Integer ticketQuantity,
            Boolean minimumRequirementEnabled, Integer minimumRequiredEvents, Integer minimumPenaltyPoints, Long targetSemesterId,
            vn.campuslife.enumeration.ScoreRuleAudience audience, java.util.List<Long> departmentIds,
            Boolean isImportant, Boolean mandatoryForFacultyStudents,
            Boolean isDraft,
            vn.campuslife.enumeration.SeriesPresetCode presetCode) {
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
            validateMinimumRequirementConfig(minimumRequirementEnabled, minimumRequiredEvents, minimumPenaltyPoints);

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
            if (minimumRequirementEnabled != null) {
                series.setMinimumRequirementEnabled(minimumRequirementEnabled);
            }
            if (minimumRequiredEvents != null) {
                series.setMinimumRequiredEvents(minimumRequiredEvents);
            }
            if (minimumPenaltyPoints != null) {
                series.setMinimumPenaltyPoints(minimumPenaltyPoints);
            }
            if (targetSemesterId != null) {
                semesterRepository.findById(targetSemesterId).ifPresent(series::setTargetSemester);
            }
            if (audience != null) {
                series.setAudience(audience);
                if (departmentIds != null) {
                    java.util.List<Department> depts = departmentRepository.findAllById(departmentIds);
                    series.getTargetDepartments().clear();
                    series.getTargetDepartments().addAll(depts);
                } else {
                    series.getTargetDepartments().clear();
                }
            }
if (presetCode != null) {
                series.setPresetCode(presetCode);
            }
            if (isImportant != null) {
                series.setImportant(isImportant);
            }
            if (mandatoryForFacultyStudents != null) {
                series.setMandatoryForFacultyStudents(mandatoryForFacultyStudents);
            }
            if (isDraft != null) {
                series.setDraft(isDraft);
            }

            ActivitySeries saved = seriesRepository.save(series);
            reminderScheduleService.syncSeriesMinimumRequirementReminders(saved);
            logger.info("Updated activity series: {} with scoreType: {}", saved.getId(), saved.getScoreType());

            // Auto-register students to main activity if series flags require it (only when non-draft)
            if (!saved.isDraft() && saved.getMainActivity() != null) {
                autoRegisterService.autoRegisterStudents(
                        saved.getMainActivity(),
                        saved.isImportant(),
                        saved.isMandatoryForFacultyStudents(),
                        saved.getTargetDepartments());
            }

            return Response.success("Activity series updated successfully", toSeriesResponse(saved));
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

    @Override
    @Transactional
    public Response createSeries(String name, String description, String milestonePointsJson, ScoreType scoreType,
            Long mainActivityId, LocalDateTime registrationStartDate, LocalDateTime registrationDeadline,
            Boolean requiresApproval, Integer ticketQuantity, Boolean minimumRequirementEnabled,
            Integer minimumRequiredEvents, Integer minimumPenaltyPoints, Long targetSemesterId,
            vn.campuslife.enumeration.ScoreRuleAudience audience, List<Long> departmentIds,
            Boolean isImportant, Boolean mandatoryForFacultyStudents, Boolean isDraft,
            vn.campuslife.enumeration.SeriesPresetCode presetCode, DepartmentScope scope) {
        validateSeriesDepartmentsForScope(departmentIds, scope);
        if (scope != null && scope.manager() && !scope.admin() && mainActivityId != null) {
            departmentAuthorizationService.requireActivityAccess(mainActivityId, scope);
        }
        return createSeries(name, description, milestonePointsJson, scoreType, mainActivityId,
                registrationStartDate, registrationDeadline, requiresApproval, ticketQuantity,
                minimumRequirementEnabled, minimumRequiredEvents, minimumPenaltyPoints, targetSemesterId,
                audience, departmentIds, isImportant, mandatoryForFacultyStudents, isDraft, presetCode);
    }

    @Override
    @Transactional
    public Response createActivityInSeries(Long seriesId, String name, String description, LocalDateTime startDate,
            LocalDateTime endDate, String location, Integer order, String shareLink, String bannerUrl,
            String benefits, String requirements, String contactInfo, List<Long> organizerIds,
            vn.campuslife.enumeration.ActivityType type, DepartmentScope scope) {
        departmentAuthorizationService.requireSeriesAccess(seriesId, scope);
        List<Long> scopedOrganizerIds = normalizeOrganizerIds(organizerIds, scope);
        return createActivityInSeries(seriesId, name, description, startDate, endDate, location, order, shareLink,
                bannerUrl, benefits, requirements, contactInfo, scopedOrganizerIds, type);
    }

    @Override
    @Transactional
    public Response addActivityToSeries(Long activityId, Long seriesId, Integer order, DepartmentScope scope) {
        departmentAuthorizationService.requireSeriesAccess(seriesId, scope);
        departmentAuthorizationService.requireActivityAccess(activityId, scope);
        return addActivityToSeries(activityId, seriesId, order);
    }

    @Override
    @Transactional
    public Response calculateMilestonePoints(Long studentId, Long seriesId, DepartmentScope scope) {
        departmentAuthorizationService.requireSeriesAccess(seriesId, scope);
        departmentAuthorizationService.requireStudentAccess(studentId, scope);
        return calculateMilestonePoints(studentId, seriesId);
    }

    @Override
    @Transactional
    public Response createSeriesActivity(Long seriesId, vn.campuslife.model.activity.series.SeriesChildActivityCreateRequest request,
            DepartmentScope scope) {
        departmentAuthorizationService.requireSeriesAccess(seriesId, scope);
        if (request.getOrganizerIds() != null && !request.getOrganizerIds().isEmpty()) {
            validateOrganizerIdsInScope(request.getOrganizerIds(), scope);
        } else {
            request.setOrganizerIds(new ArrayList<>(normalizeOrganizerIds(null, scope)));
        }
        return createSeriesActivity(seriesId, request);
    }

    @Override
    @Transactional
    public Response updateSeriesActivity(Long seriesId, Long activityId,
            vn.campuslife.model.activity.series.SeriesChildActivityUpdateRequest request, DepartmentScope scope) {
        departmentAuthorizationService.requireSeriesAccess(seriesId, scope);
        departmentAuthorizationService.requireActivityAccess(activityId, scope);
        if (request.getOrganizerIds() != null && !request.getOrganizerIds().isEmpty()) {
            validateOrganizerIdsInScope(request.getOrganizerIds(), scope);
        }
        return updateSeriesActivity(seriesId, activityId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getSeriesActivity(Long seriesId, Long activityId, DepartmentScope scope) {
        departmentAuthorizationService.requireSeriesAccess(seriesId, scope);
        departmentAuthorizationService.requireActivityAccess(activityId, scope);
        return getSeriesActivity(seriesId, activityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getStudentProgress(Long seriesId, Long studentId, DepartmentScope scope) {
        departmentAuthorizationService.requireSeriesAccess(seriesId, scope);
        departmentAuthorizationService.requireStudentAccess(studentId, scope);
        return getStudentProgress(seriesId, studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getSeriesProgress(Long seriesId, Integer page, Integer size, String keyword, DepartmentScope scope) {
        departmentAuthorizationService.requireSeriesAccess(seriesId, scope);
        return getSeriesProgress(seriesId, page, size, keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getSeriesOverview(Long seriesId, DepartmentScope scope) {
        departmentAuthorizationService.requireSeriesAccess(seriesId, scope);
        return getSeriesOverview(seriesId);
    }

    @Override
    @Transactional
    public Response updateSeries(Long seriesId, String name, String description, String milestonePointsJson,
            vn.campuslife.enumeration.ScoreType scoreType, Long mainActivityId, LocalDateTime registrationStartDate,
            LocalDateTime registrationDeadline, Boolean requiresApproval, Integer ticketQuantity,
            Boolean minimumRequirementEnabled, Integer minimumRequiredEvents, Integer minimumPenaltyPoints,
            Long targetSemesterId, vn.campuslife.enumeration.ScoreRuleAudience audience, List<Long> departmentIds,
            Boolean isImportant, Boolean mandatoryForFacultyStudents, Boolean isDraft,
            vn.campuslife.enumeration.SeriesPresetCode presetCode, DepartmentScope scope) {
        departmentAuthorizationService.requireSeriesAccess(seriesId, scope);
        validateSeriesDepartmentsForScope(departmentIds, scope);
        if (scope != null && scope.manager() && !scope.admin() && mainActivityId != null) {
            departmentAuthorizationService.requireActivityAccess(mainActivityId, scope);
        }
        return updateSeries(seriesId, name, description, milestonePointsJson, scoreType, mainActivityId,
                registrationStartDate, registrationDeadline, requiresApproval, ticketQuantity,
                minimumRequirementEnabled, minimumRequiredEvents, minimumPenaltyPoints, targetSemesterId,
                audience, departmentIds, isImportant, mandatoryForFacultyStudents, isDraft, presetCode);
    }

    @Override
    @Transactional
    public Response deleteSeries(Long seriesId, DepartmentScope scope) {
        departmentAuthorizationService.requireSeriesAccess(seriesId, scope);
        return deleteSeries(seriesId);
    }

    private void validateSeriesDepartmentsForScope(List<Long> departmentIds, DepartmentScope scope) {
        if (scope == null || !scope.manager() || scope.admin()) {
            return;
        }
        if (departmentIds == null || departmentIds.isEmpty()) {
            if (scope.departmentIds().size() == 1) {
                return;
            }
            throw new IllegalArgumentException("Manager with multiple departments must specify departmentIds within scope");
        }
        if (!scope.departmentIds().containsAll(new HashSet<>(departmentIds))) {
            throw new IllegalArgumentException("Target departments must be within manager scope");
        }
    }

    private void validateOrganizerIdsInScope(List<Long> organizerIds, DepartmentScope scope) {
        if (scope == null || !scope.manager() || scope.admin()) {
            return;
        }
        if (organizerIds == null || organizerIds.isEmpty()) {
            return;
        }
        if (!scope.departmentIds().containsAll(new HashSet<>(organizerIds))) {
            throw new IllegalArgumentException("Organizer departments must be within manager scope");
        }
    }

    private List<Long> normalizeOrganizerIds(List<Long> organizerIds, DepartmentScope scope) {
        if (scope == null || !scope.manager() || scope.admin()) {
            return organizerIds;
        }
        if (organizerIds != null && !organizerIds.isEmpty()) {
            validateOrganizerIdsInScope(organizerIds, scope);
            return organizerIds;
        }
        if (scope.departmentIds().size() == 1) {
            return List.copyOf(scope.departmentIds());
        }
        throw new IllegalArgumentException("Manager with multiple departments must specify organizerIds within scope");
    }

    private SeriesResponse toSeriesResponse(ActivitySeries series) {
        SeriesResponse response = new SeriesResponse();
        response.setId(series.getId());
        response.setName(series.getName());
        response.setDescription(series.getDescription());
        response.setScoreType(series.getScoreType());
        response.setMainActivityId(series.getMainActivity() != null ? series.getMainActivity().getId() : null);
        response.setTargetSemesterId(series.getTargetSemester() != null ? series.getTargetSemester().getId() : null);
        response.setRegistrationStartDate(series.getRegistrationStartDate());
        response.setRegistrationDeadline(series.getRegistrationDeadline());
        response.setRequiresApproval(series.isRequiresApproval());
        response.setTicketQuantity(series.getTicketQuantity());
        response.setMinimumRequirementEnabled(series.isMinimumRequirementEnabled());
        response.setMinimumRequiredEvents(series.getMinimumRequiredEvents());
        response.setMinimumPenaltyPoints(series.getMinimumPenaltyPoints());
        response.setAudience(series.getAudience());
        response.setTargetDepartmentIds(series.getTargetDepartments().stream()
                .map(Department::getId)
                .collect(Collectors.toList()));
        response.setImportant(series.isImportant());
        response.setMandatoryForFacultyStudents(series.isMandatoryForFacultyStudents());
        response.setDraft(series.isDraft());
        response.setPresetCode(series.getPresetCode());
        response.setPresetConfig(null);
        response.setCreatedAt(series.getCreatedAt());
        if (series.getMilestonePoints() != null && !series.getMilestonePoints().isBlank()) {
            try {
                response.setMilestonePoints(objectMapper.readValue(
                        series.getMilestonePoints(),
                        new TypeReference<Map<Integer, Integer>>() {
                        }));
            } catch (Exception e) {
                logger.warn("Failed to parse milestonePoints for series response {}", series.getId(), e);
            }
        }
        return response;
    }

    private void validateMinimumRequirementConfig(Boolean minimumRequirementEnabled, Integer minimumRequiredEvents,
            Integer minimumPenaltyPoints) {
        if (!Boolean.TRUE.equals(minimumRequirementEnabled)) {
            return;
        }
        if (minimumRequiredEvents == null || minimumRequiredEvents <= 0) {
            throw new IllegalArgumentException("minimumRequiredEvents must be greater than 0 when minimum requirement is enabled");
        }
        if (minimumPenaltyPoints == null || minimumPenaltyPoints <= 0) {
            throw new IllegalArgumentException("minimumPenaltyPoints must be greater than 0 when minimum requirement is enabled");
        }
    }

}





