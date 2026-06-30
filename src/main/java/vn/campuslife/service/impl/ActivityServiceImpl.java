package vn.campuslife.service.impl;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.Department;
import vn.campuslife.entity.Student;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.ScoreEntryStatus;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.activity.ActivityPresetDefinitionResponse;
import vn.campuslife.model.activity.ActivityPresetPreviewRequest;
import vn.campuslife.model.activity.ActivityPresetPreviewResponse;
import vn.campuslife.model.activity.ActivityResponse;
import vn.campuslife.model.activity.CreateActivityRequest;
import vn.campuslife.model.Response;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.ActivitySeriesRepository;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.repository.ScoreEntryRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.Role;
import vn.campuslife.service.ActivityRegistrationAutoService;
import vn.campuslife.service.ActivityService;
import vn.campuslife.service.ActivityScoreRuleService;
import vn.campuslife.service.ReminderScheduleService;
import vn.campuslife.service.ScorePresetService;
import vn.campuslife.util.TicketCodeUtils;
import vn.campuslife.util.UrlUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityServiceImpl.class);

    private final ActivityRepository activityRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ActivitySeriesRepository activitySeriesRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ReminderScheduleService reminderScheduleService;
    private final ActivityScoreRuleService activityScoreRuleService;
    private final ScorePresetService scorePresetService;
    private final ScoreEntryRepository scoreEntryRepository;
    private final ActivityRegistrationAutoService autoRegisterService;
    private final UploadProperties uploadProperties;

    @Override
    @Transactional
    public Response createActivity(CreateActivityRequest request) {
        try {
            scorePresetService.applyActivityPreset(request);

            String err = validateRequest(request);
            if (err != null)
                return new Response(false, err, null);

            Set<Department> organizers = resolveOrganizers(request.getOrganizerIds());

            Activity a = new Activity();
            applyRequestToEntity(request, a);
            a.setOrganizers(organizers);
            Activity saved = activityRepository.save(a);

            // Auto-generate checkInCode if not provided
            if (saved.getCheckInCode() == null || saved.getCheckInCode().isBlank()) {
                String checkInCode = generateCheckInCode(saved.getId());
                saved.setCheckInCode(checkInCode);
                saved = activityRepository.save(saved);
                logger.debug("Auto-generated checkInCode for activity {}: {}", saved.getId(), checkInCode);
            }

            // Persist score rules if provided
            if (request.getScoreRules() != null && !request.getScoreRules().isEmpty()) {
                activityScoreRuleService.replaceRules(saved.getId(), request.getScoreRules());
                logger.debug("Persisted {} score rules for activity {}", request.getScoreRules().size(), saved.getId());
            }

            // Auto-register students based on flags (this handles both isImportant and
            // mandatoryForFacultyStudents)
            // Note: autoRegisterStudents will skip if activity is draft
            logger.debug(
                    "Activity created (id={}, name={}, isDraft={}, isImportant={}, mandatoryForFacultyStudents={})",
                    saved.getId(), saved.getName(), saved.isDraft(), saved.isImportant(),
                    saved.isMandatoryForFacultyStudents());
            autoRegisterService.autoRegisterStudents(saved);
            reminderScheduleService.syncEventRemindersForActivity(saved);
            syncSeriesMinimumRequirementReminders(saved);

            return new Response(true, "Activity created successfully", toResponse(saved));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid create activity request: {}", e.getMessage());
            return new Response(false, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Failed to create activity: {}", e.getMessage(), e);
            return new Response(false, "Failed to create activity due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response publishActivity(Long id) {
        var opt = activityRepository.findByIdAndIsDeletedFalse(id);
        if (opt.isEmpty())
            return new Response(false, "Activity not found", null);
        Activity a = opt.get();

        // Kiểm tra xem activity có đang là draft không
        boolean wasDraft = a.isDraft();

        a.setDraft(false);
        Activity saved = activityRepository.save(a);

        // Nếu activity vừa được publish (chuyển từ draft sang published) và có flag
        // auto-register,
        // thì tự động đăng ký cho sinh viên
        if (wasDraft && (saved.isImportant() || saved.isMandatoryForFacultyStudents())) {
            try {
                autoRegisterService.autoRegisterStudents(saved);
                reminderScheduleService.syncEventRemindersForActivity(saved);
                logger.info("Auto-registered students after publishing activity: {}", saved.getName());
            } catch (Exception e) {
                logger.error("Failed to auto-register students after publishing activity {}: {}",
                        saved.getId(), e.getMessage(), e);
                // Không fail publish nếu auto-register lỗi, chỉ log
            }
        }

        return new Response(true, "Activity published", toResponse(saved));
    }

    @Override
    @Transactional
    public Response unpublishActivity(Long id) {
        var opt = activityRepository.findByIdAndIsDeletedFalse(id);
        if (opt.isEmpty())
            return new Response(false, "Activity not found", null);
        Activity a = opt.get();
        a.setDraft(true);
        Activity saved = activityRepository.save(a);
        return new Response(true, "Activity unpublished", toResponse(saved));
    }

    @Override
    @Transactional
    public Response copyActivity(Long id, Integer offsetDays) {
        var opt = activityRepository.findByIdAndIsDeletedFalse(id);
        if (opt.isEmpty())
            return new Response(false, "Activity not found", null);
        Activity src = opt.get();
        int days = (offsetDays == null) ? 0 : offsetDays.intValue();

        Activity copy = new Activity();
        copy.setName(src.getName() + " (Copy)");
        copy.setType(src.getType());

        copy.setDescription(src.getDescription());
        copy.setStartDate(src.getStartDate() == null ? null : src.getStartDate().plusDays(days));
        copy.setEndDate(src.getEndDate() == null ? null : src.getEndDate().plusDays(days));
        copy.setRequiresSubmission(src.isRequiresSubmission());

        copy.setRegistrationStartDate(
                src.getRegistrationStartDate() == null ? null : src.getRegistrationStartDate().plusDays(days));
        copy.setRegistrationDeadline(
                src.getRegistrationDeadline() == null ? null : src.getRegistrationDeadline().plusDays(days));
        copy.setShareLink(src.getShareLink());
        copy.setImportant(src.isImportant());
        copy.setBannerUrl(src.getBannerUrl());
        copy.setLocation(src.getLocation());
        copy.setTicketQuantity(src.getTicketQuantity());
        copy.setBenefits(src.getBenefits());
        copy.setRequirements(src.getRequirements());
        copy.setContactInfo(src.getContactInfo());
        copy.setMandatoryForFacultyStudents(src.isMandatoryForFacultyStudents());

        copy.setRequiresApproval(src.isRequiresApproval());
        copy.setDraft(true); // new copy starts as draft

        if (src.getOrganizers() != null && !src.getOrganizers().isEmpty()) {
            copy.setOrganizers(new java.util.LinkedHashSet<>(src.getOrganizers()));
        }

        Activity saved = activityRepository.save(copy);

        // Copy score rules from source activity with ACTIVITY_SEMESTER policy
        var srcRules = activityScoreRuleService.getRuleResponses(src.getId());
        if (srcRules != null && !srcRules.isEmpty()) {
            List<vn.campuslife.model.score.ActivityScoreRuleRequest> copiedRules = srcRules.stream()
                    .map(rule -> {
                        vn.campuslife.model.score.ActivityScoreRuleRequest req = new vn.campuslife.model.score.ActivityScoreRuleRequest();
                        req.setScoreType(rule.getScoreType());
                        req.setTriggerType(rule.getTriggerType());
                        req.setCalculation(rule.getCalculation());
                        req.setPoints(rule.getPoints());
                        req.setFailPoints(rule.getFailPoints());
                        req.setAudience(rule.getAudience());
                        req.setSemesterPolicy(vn.campuslife.enumeration.ScoreSemesterPolicy.ACTIVITY_SEMESTER);
                        req.setExplicitSemesterId(null); // Clear explicit semester on copy
                        req.setDepartmentIds(rule.getTargetDepartmentIds());
                        req.setEnabled(rule.getEnabled());
                        return req;
                    })
                    .collect(java.util.stream.Collectors.toList());
            activityScoreRuleService.replaceRules(saved.getId(), copiedRules);
            logger.debug("Copied {} score rules for activity {} with ACTIVITY_SEMESTER policy", copiedRules.size(),
                    saved.getId());
        }

        return new Response(true, "Activity copied", toResponse(saved));
    }

    public Response getAllActivities() {
        return getAllActivities(null);
    }

    @Override
    public Response getAllActivities(String username) {
        try {
            var list = activityRepository.findByIsDeletedFalseOrderByStartDateAsc();

            // Filter drafts for students (non-admin/manager users)
            boolean isAdminOrManager = false;
            if (username != null) {
                Optional<User> userOpt = userRepository.findByUsername(username);
                isAdminOrManager = userOpt.map(user -> user.getRole() == Role.ADMIN ||
                        user.getRole() == Role.MANAGER)
                        .orElse(false);
            }

            final boolean filterDrafts = !isAdminOrManager;
            var filteredList = list.stream()
                    .filter(activity -> !filterDrafts || !activity.isDraft())
                    .collect(Collectors.toList());

            var data = filteredList.stream().map(this::toResponse).toList();
            return new Response(true, "Activities retrieved successfully", data);
        } catch (Exception e) {
            logger.error("Failed to retrieve activities: {}", e.getMessage(), e);
            return new Response(false, "Failed to retrieve activities due to server error", null);
        }
    }

    @Override
    public Response getActivityById(Long id) {
        return getActivityById(id, null);
    }

    @Override
    public Response getActivityById(Long id, String username) {
        try {
            var opt = activityRepository.findByIdAndIsDeletedFalse(id);
            if (opt.isEmpty())
                return new Response(false, "Activity not found", null);

            Activity activity = opt.get();

            // Block students from viewing drafts
            boolean isAdminOrManager = false;
            if (username != null) {
                Optional<User> userOpt = userRepository.findByUsername(username);
                isAdminOrManager = userOpt.map(user -> user.getRole() == Role.ADMIN ||
                        user.getRole() == Role.MANAGER)
                        .orElse(false);
            }

            if (activity.isDraft() && !isAdminOrManager) {
                return new Response(false, "Activity not found", null);
            }

            return new Response(true, "Activity retrieved successfully", toResponse(activity));
        } catch (Exception e) {
            logger.error("Failed to retrieve activity {}: {}", id, e.getMessage(), e);
            return new Response(false, "Failed to retrieve activity due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response updateActivity(Long id, CreateActivityRequest request) {
        try {
            var opt = activityRepository.findByIdAndIsDeletedFalse(id);
            if (opt.isEmpty())
                return new Response(false, "Activity not found", null);

            Activity a = opt.get();

            if (request.getType() != null && request.getType() != a.getType()) {
                long activeEntries = scoreEntryRepository.countByActivityIdAndStatus(id, ScoreEntryStatus.ACTIVE);
                if (activeEntries > 0 && !a.isDraft()) {
                    return new Response(false,
                            "Cannot change type when activity has " + activeEntries
                            + " active score entries and is not draft. "
                            + "Unpublish the activity first.", null);
                }
            }

            scorePresetService.applyActivityPreset(request);
            String err = validateRequest(request);
            if (err != null)
                return new Response(false, err, null);

            applyRequestToEntity(request, a);

            Set<Department> organizers = resolveOrganizers(request.getOrganizerIds());
            a.getOrganizers().clear();
            a.getOrganizers().addAll(organizers);

            Activity saved = activityRepository.save(a);

            // Replace score rules if provided
            if (request.getScoreRules() != null) {
                activityScoreRuleService.replaceRules(saved.getId(), request.getScoreRules());
                logger.debug("Replaced score rules for activity {}", saved.getId());
            }

            // Auto-register students if flags changed
            // Note: autoRegisterStudents will skip if activity is draft
            logger.debug(
                    "Activity updated (id={}, name={}, isDraft={}, isImportant={}, mandatoryForFacultyStudents={})",
                    saved.getId(), saved.getName(), saved.isDraft(), saved.isImportant(),
                    saved.isMandatoryForFacultyStudents());
            autoRegisterService.autoRegisterStudents(saved);
            reminderScheduleService.syncEventRemindersForActivity(saved);
            syncSeriesMinimumRequirementReminders(saved);

            return new Response(true, "Activity updated successfully", toResponse(saved));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid update activity request {}: {}", id, e.getMessage());
            return new Response(false, e.getMessage(), null);
        } catch (IllegalStateException e) {
            logger.warn("Cannot modify score rules for activity {}: {}", id, e.getMessage());
            return new Response(false, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Failed to update activity {}: {}", id, e.getMessage(), e);
            return new Response(false, "Failed to update activity due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response deleteActivity(Long id) {
        try {
            var opt = activityRepository.findByIdAndIsDeletedFalse(id);
            if (opt.isEmpty())
                return new Response(false, "Activity not found", null);

            Activity a = opt.get();
            a.setDeleted(true);
            activityRepository.save(a);
            return new Response(true, "Activity deleted successfully", null);
        } catch (Exception e) {
            logger.error("Failed to delete activity {}: {}", id, e.getMessage(), e);
            return new Response(false, "Failed to delete activity due to server error", null);
        }
    }

    @Override
    public List<ActivityPresetDefinitionResponse> getActivityPresetDefinitions() {
        return scorePresetService.getActivityPresetDefinitions();
    }

    @Override
    public ActivityPresetPreviewResponse previewActivityPreset(ActivityPresetPreviewRequest request) {
        return scorePresetService.previewActivityPreset(request);
    }

    @Override
    public List<ActivityResponse> getActivitiesByScoreType(ScoreType scoreType) {
        return activityRepository.findByScoreTypeAndIsDeletedFalseOrderByStartDateAsc(scoreType).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActivityResponse> getActivitiesByMonth(LocalDate start, LocalDate end) {
        return activityRepository.findInMonth(start, end).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActivityResponse> getActivitiesForDepartment(Long departmentId) {
        return activityRepository.findForDepartment(departmentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActivityResponse> listForCurrentUser(String username) {
        Long deptId = studentRepository.findDepartmentIdByUsername(username);
        if (deptId == null)
            return Collections.emptyList();

        List<Activity> all = activityRepository.findForDepartment(deptId);

        LocalDate today = LocalDate.now();

        return all.stream()
                .filter(a -> a.getEndDate() != null &&
                        !a.getEndDate().toLocalDate().isBefore(today))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Response checkRequiresSubmission(Long activityId) {
        try {
            Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(activityId);
            if (activityOpt.isEmpty()) {
                return new Response(false, "Activity not found", null);
            }

            Activity activity = activityOpt.get();
            Map<String, Object> result = new HashMap<>();
            result.put("activityId", activity.getId());
            result.put("activityName", activity.getName());
            result.put("requiresSubmission", activity.isRequiresSubmission());
            result.put("isImportant", activity.isImportant());
            result.put("mandatoryForFacultyStudents", activity.isMandatoryForFacultyStudents());

            return new Response(true, "Submission requirement checked successfully", result);
        } catch (Exception e) {
            logger.error("Failed to check submission requirement for activity {}: {}", activityId, e.getMessage(), e);
            return new Response(false, "Failed to check submission requirement", null);
        }
    }

    @Override
    public Response checkRegistrationStatus(Long activityId, String username) {
        try {
            // Get student by username
            Optional<Student> studentOpt = studentRepository.findByUserUsernameAndIsDeletedFalse(username);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }

            // Check if activity exists
            Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(activityId);
            if (activityOpt.isEmpty()) {
                return new Response(false, "Activity not found", null);
            }

            // Check registration status
            Optional<ActivityRegistration> registrationOpt = activityRegistrationRepository
                    .findByActivityIdAndStudentId(activityId, studentOpt.get().getId());

            Map<String, Object> result = new HashMap<>();
            result.put("activityId", activityId);
            result.put("studentId", studentOpt.get().getId());
            result.put("isRegistered", registrationOpt.isPresent());

            if (registrationOpt.isPresent()) {
                ActivityRegistration registration = registrationOpt.get();
                Activity activity = registration.getActivity();
                result.put("registrationId", registration.getId());
                result.put("status", registration.getStatus());
                result.put("registeredDate", registration.getRegisteredDate());

                boolean canCancel;
                if (registration.getStatus() == RegistrationStatus.CANCELLED) {
                    canCancel = false;
                } else if (registration.getStatus() == RegistrationStatus.ATTENDED) {
                    canCancel = false;
                } else if (registration.getStatus() == RegistrationStatus.APPROVED) {
                    if (activity.isRequiresApproval()) {
                        canCancel = false;
                    } else if (registration.isHasCancelledBefore()) {
                        canCancel = false;
                    } else if (activity.getRegistrationDeadline() != null
                            && LocalDateTime.now().isAfter(activity.getRegistrationDeadline().minusDays(1))) {
                        canCancel = false;
                    } else {
                        canCancel = true;
                    }
                } else {
                    canCancel = true;
                }
                result.put("canCancel", canCancel);
            }

            return new Response(true, "Registration status checked successfully", result);
        } catch (Exception e) {
            logger.error("Failed to check registration status for activity {} and user {}: {}", activityId, username,
                    e.getMessage(), e);
            return new Response(false, "Failed to check registration status", null);
        }
    }

    private String validateRequest(CreateActivityRequest r) {
        if (r.getName() == null || r.getName().isBlank())
            return "Activity name is required";
        if (r.getType() == null)
            return "Activity type is required";
        if (r.getStartDate() == null || r.getEndDate() == null)
            return "Start date and end date are required";
        if (r.getStartDate().isAfter(r.getEndDate()))
            return "Start date must be before end date";
        if (r.getLocation() == null || r.getLocation().isBlank())
            return "Location is required";
        if (r.getOrganizerIds() == null || r.getOrganizerIds().isEmpty())
            return "Organizer ids are required";
        return null;
    }

    private void applyRequestToEntity(CreateActivityRequest req, Activity a) {
        a.setName(req.getName());
        a.setType(req.getType());

        a.setDescription(req.getDescription());
        a.setStartDate(req.getStartDate());
        a.setEndDate(req.getEndDate());

        a.setRequiresSubmission(Boolean.TRUE.equals(req.getRequiresSubmission()));

        a.setRegistrationStartDate(req.getRegistrationStartDate());
        a.setRegistrationDeadline(req.getRegistrationDeadline());

        a.setShareLink(req.getShareLink());
        a.setImportant(Boolean.TRUE.equals(req.getIsImportant()));
        // Set isDraft: if explicitly provided, use it; otherwise default to true
        // (draft)
        if (req.getIsDraft() != null) {
            a.setDraft(req.getIsDraft());
        } else {
            // Default to draft if not specified
            a.setDraft(true);
        }
        a.setBannerUrl(req.getBannerUrl());
        a.setLocation(req.getLocation());

        a.setTicketQuantity(req.getTicketQuantity());
        a.setBenefits(req.getBenefits());
        a.setRequirements(req.getRequirements());
        a.setContactInfo(req.getContactInfo());
        if (req.getRequiresApproval() != null)
            a.setRequiresApproval(req.getRequiresApproval());
        a.setMandatoryForFacultyStudents(Boolean.TRUE.equals(req.getMandatoryForFacultyStudents()));
        a.setPresetCode(req.getPresetCode());

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

    private ActivityResponse toResponse(Activity a) {
        ActivityResponse dto = new ActivityResponse();
        dto.setId(a.getId());
        dto.setName(a.getName());
        dto.setType(a.getType());

        dto.setDescription(a.getDescription());
        dto.setStartDate(a.getStartDate());
        dto.setEndDate(a.getEndDate());

        dto.setHasPreparation(a.isHasPreparation());

        dto.setRequiresSubmission(a.isRequiresSubmission());

        dto.setRegistrationStartDate(a.getRegistrationStartDate());
        dto.setRegistrationDeadline(a.getRegistrationDeadline());

        dto.setShareLink(a.getShareLink());
        dto.setImportant(a.isImportant());
        dto.setDraft(a.isDraft());
        // Convert relative path to full URL for API response
        dto.setBannerUrl(UrlUtils.toFullUrl(a.getBannerUrl(), uploadProperties.getPublicUrl()));
        dto.setLocation(a.getLocation());

        dto.setTicketQuantity(a.getTicketQuantity());
        dto.setBenefits(a.getBenefits());
        dto.setRequirements(a.getRequirements());
        dto.setContactInfo(a.getContactInfo());
        dto.setCheckInCode(a.getCheckInCode());
        dto.setRequiresApproval(a.isRequiresApproval());
        dto.setMandatoryForFacultyStudents(a.isMandatoryForFacultyStudents());

        dto.setOrganizerIds(a.getOrganizers() == null ? List.of()
                : a.getOrganizers().stream().map(Department::getId).toList());

        dto.setSeriesId(a.getSeriesId());
        dto.setSeriesOrder(a.getSeriesOrder());

        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        dto.setCreatedBy(a.getCreatedBy());
        dto.setLastModifiedBy(a.getLastModifiedBy());

        // Map score rules from ActivityScoreRuleService
        dto.setScoreRules(activityScoreRuleService.getRuleResponses(a.getId()));

        dto.setPresetCode(a.getPresetCode());
        // presetConfig is not persisted on entity; set to null
        dto.setPresetConfig(null);
        dto.setActiveScoreEntryCount(activityScoreRuleService.countActiveEntries(a.getId()));

        return dto;
    }

    @Override
    public void registerAllStudents(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        List<Student> allStudents = studentRepository.findAll().stream()
                .filter(student -> !student.isDeleted())
                .collect(Collectors.toList());

        List<ActivityRegistration> registrationsToCreate = new ArrayList<>();
        List<ActivityRegistration> registrationsToUpdate = new ArrayList<>();

        for (Student student : allStudents) {
            Optional<ActivityRegistration> existingOpt = activityRegistrationRepository
                    .findByActivityIdAndStudentId(activityId, student.getId());

            if (existingOpt.isPresent()) {
                // Update existing registration to APPROVED if not already
                ActivityRegistration existing = existingOpt.get();
                if (existing.getStatus() != RegistrationStatus.APPROVED) {
                    existing.setStatus(RegistrationStatus.APPROVED);
                    existing.setRegisteredDate(LocalDateTime.now());
                    registrationsToUpdate.add(existing);
                }
            } else {
                // Create new APPROVED registration
                ActivityRegistration reg = new ActivityRegistration();
                reg.setActivity(activity);
                reg.setStudent(student);
                reg.setRegisteredDate(LocalDateTime.now());
                reg.setStatus(RegistrationStatus.APPROVED);

                String code;
                int attempts = 0;
                do {
                    code = TicketCodeUtils.newTicketCode();
                    attempts++;
                } while (activityRegistrationRepository.existsByTicketCode(code) && attempts < 3);
                reg.setTicketCode(code);

                registrationsToCreate.add(reg);
            }
        }

        if (!registrationsToCreate.isEmpty()) {
            activityRegistrationRepository.saveAll(registrationsToCreate);
        }
        if (!registrationsToUpdate.isEmpty()) {
            activityRegistrationRepository.saveAll(registrationsToUpdate);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFacultyStudents(Long activityId, Collection<Long> departmentIds) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        List<Student> students = studentRepository.findByDepartment_IdIn(departmentIds).stream()
                .filter(student -> !student.isDeleted())
                .collect(Collectors.toList());

        List<ActivityRegistration> registrationsToCreate = new ArrayList<>();
        List<ActivityRegistration> registrationsToUpdate = new ArrayList<>();

        for (Student student : students) {
            Optional<ActivityRegistration> existingOpt = activityRegistrationRepository
                    .findByActivityIdAndStudentId(activityId, student.getId());

            if (existingOpt.isPresent()) {
                // Update existing registration to APPROVED if not already
                ActivityRegistration existing = existingOpt.get();
                if (existing.getStatus() != RegistrationStatus.APPROVED) {
                    existing.setStatus(RegistrationStatus.APPROVED);
                    existing.setRegisteredDate(LocalDateTime.now());
                    registrationsToUpdate.add(existing);
                }
            } else {
                // Create new APPROVED registration
                ActivityRegistration reg = new ActivityRegistration();
                reg.setActivity(activity);
                reg.setStudent(student);
                reg.setRegisteredDate(LocalDateTime.now());
                reg.setStatus(RegistrationStatus.APPROVED);

                String code;
                int attempts = 0;
                do {
                    code = TicketCodeUtils.newTicketCode();
                    attempts++;
                } while (activityRegistrationRepository.existsByTicketCode(code) && attempts < 3);
                reg.setTicketCode(code);

                registrationsToCreate.add(reg);
            }
        }

        if (!registrationsToCreate.isEmpty()) {
            activityRegistrationRepository.saveAll(registrationsToCreate);
        }
        if (!registrationsToUpdate.isEmpty()) {
            activityRegistrationRepository.saveAll(registrationsToUpdate);
        }

        logger.info("Auto registered {} students of departments {} for mandatory activity {}",
                registrationsToCreate.size() + registrationsToUpdate.size(), departmentIds, activityId);
    }

    // Tìm kiếm sự kiện
    @Override
    public List<ActivityResponse> searchUpcomingEvents(String keyword) {
        Specification<Activity> spec = (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            // chỉ lấy sự kiện chưa diễn ra
            predicates.add(cb.greaterThanOrEqualTo(
                    root.get("startDate"),
                    LocalDateTime.now()));

            if (keyword != null && !keyword.trim().isEmpty()) {
                String k = "%" + keyword.toLowerCase() + "%";

                Join<Activity, Department> deptJoin = root.join("organizers", JoinType.LEFT);

                Predicate keywordPredicate = cb.or(
                        cb.like(cb.lower(root.get("name")), k),
                        cb.like(cb.lower(root.get("description")), k),
                        cb.like(cb.lower(deptJoin.get("name")), k));

                predicates.add(keywordPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return activityRepository.findAll(spec).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // sự kiện trong tháng
    @Override
    public List<ActivityResponse> getActivitiesByMonth(LocalDateTime start, LocalDateTime end) {
        return activityRepository.findActivitiesInMonth(start, end).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Generate check-in code for activity
     * Format: ACT-{activityId padded to 6 digits}-{8 random uppercase characters}
     * Example: ACT-000123-A7B9C2D1
     */
    private String generateCheckInCode(Long activityId) {
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase().replace("-", "");
        return String.format("ACT-%06d-%s", activityId, random);
    }

    @Override
    @Transactional
    public Response backfillCheckInCodes() {
        try {
            // Lấy tất cả activities chưa có checkInCode
            List<Activity> activitiesWithoutCode = activityRepository.findAll().stream()
                    .filter(a -> !a.isDeleted())
                    .filter(a -> a.getCheckInCode() == null || a.getCheckInCode().isBlank())
                    .collect(Collectors.toList());

            if (activitiesWithoutCode.isEmpty()) {
                return Response.success("Tất cả activities đã có checkInCode", null);
            }

            int updatedCount = 0;
            for (Activity activity : activitiesWithoutCode) {
                String checkInCode = generateCheckInCode(activity.getId());
                activity.setCheckInCode(checkInCode);
                activityRepository.save(activity);
                updatedCount++;
                logger.info("Generated checkInCode for activity {}: {}", activity.getId(), checkInCode);
            }

            return Response.success(
                    String.format("Đã tạo checkInCode cho %d activity", updatedCount),
                    Map.of("updatedCount", updatedCount, "totalActivities", activitiesWithoutCode.size()));
        } catch (Exception e) {
            logger.error("Failed to backfill checkInCodes: {}", e.getMessage(), e);
            return Response.error("Failed to backfill checkInCodes: " + e.getMessage());
        }
    }

    private void syncSeriesMinimumRequirementReminders(Activity activity) {
        if (activity == null || activity.getSeriesId() == null) {
            return;
        }
        activitySeriesRepository.findById(activity.getSeriesId())
                .ifPresent(reminderScheduleService::syncSeriesMinimumRequirementReminders);
    }
}
