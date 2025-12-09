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
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.Department;
import vn.campuslife.entity.Student;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.ActivityResponse;
import vn.campuslife.model.CreateActivityRequest;
import vn.campuslife.model.Response;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.ActivityParticipationRepository;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.Role;
import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.enumeration.ParticipationType;
import java.math.BigDecimal;
import vn.campuslife.service.ActivityService;
import vn.campuslife.service.NotificationService;
import vn.campuslife.util.TicketCodeUtils;
import vn.campuslife.enumeration.NotificationType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityServiceImpl.class);

    private final ActivityRepository activityRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ActivityParticipationRepository activityParticipationRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Response createActivity(CreateActivityRequest request) {
        try {

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
            
            // Auto-register students based on flags (this handles both isImportant and mandatoryForFacultyStudents)
            // Note: autoRegisterStudents will skip if activity is draft
            logger.debug("Activity created (id={}, name={}, isDraft={}, isImportant={}, mandatoryForFacultyStudents={})", 
                saved.getId(), saved.getName(), saved.isDraft(), saved.isImportant(), saved.isMandatoryForFacultyStudents());
            autoRegisterStudents(saved);

            return new Response(true, "Activity created successfully", toResponse(saved));
        } catch (Exception e) {
            logger.error("Failed to create activity: {}", e.getMessage(), e);
            return new Response(false, "Failed to create activity due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response publishActivity(Long id) {
        var opt = activityRepository.findByIdAndIsDeletedFalse(id);
        if (opt.isEmpty()) return new Response(false, "Activity not found", null);
        Activity a = opt.get();
        
        // Kiểm tra xem activity có đang là draft không
        boolean wasDraft = a.isDraft();
        
        a.setDraft(false);
        Activity saved = activityRepository.save(a);
        
        // Nếu activity vừa được publish (chuyển từ draft sang published) và có flag auto-register,
        // thì tự động đăng ký cho sinh viên
        if (wasDraft && (saved.isImportant() || saved.isMandatoryForFacultyStudents())) {
            try {
                autoRegisterStudents(saved);
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
        if (opt.isEmpty()) return new Response(false, "Activity not found", null);
        Activity a = opt.get();
        a.setDraft(true);
        Activity saved = activityRepository.save(a);
        return new Response(true, "Activity unpublished", toResponse(saved));
    }

    @Override
    @Transactional
    public Response copyActivity(Long id, Integer offsetDays) {
        var opt = activityRepository.findByIdAndIsDeletedFalse(id);
        if (opt.isEmpty()) return new Response(false, "Activity not found", null);
        Activity src = opt.get();
        int days = (offsetDays == null) ? 0 : offsetDays.intValue();

        Activity copy = new Activity();
        copy.setName(src.getName() + " (Copy)");
        copy.setType(src.getType());
        copy.setScoreType(src.getScoreType());
        copy.setDescription(src.getDescription());
        copy.setStartDate(src.getStartDate() == null ? null : src.getStartDate().plusDays(days));
        copy.setEndDate(src.getEndDate() == null ? null : src.getEndDate().plusDays(days));
        copy.setRequiresSubmission(src.isRequiresSubmission());
        copy.setMaxPoints(src.getMaxPoints());
        copy.setRegistrationStartDate(src.getRegistrationStartDate() == null ? null : src.getRegistrationStartDate().plusDays(days));
        copy.setRegistrationDeadline(src.getRegistrationDeadline() == null ? null : src.getRegistrationDeadline().plusDays(days));
        copy.setShareLink(src.getShareLink());
        copy.setImportant(src.isImportant());
        copy.setBannerUrl(src.getBannerUrl());
        copy.setLocation(src.getLocation());
        copy.setTicketQuantity(src.getTicketQuantity());
        copy.setBenefits(src.getBenefits());
        copy.setRequirements(src.getRequirements());
        copy.setContactInfo(src.getContactInfo());
        copy.setMandatoryForFacultyStudents(src.isMandatoryForFacultyStudents());
        copy.setPenaltyPointsIncomplete(src.getPenaltyPointsIncomplete());
        copy.setRequiresApproval(src.isRequiresApproval());
        copy.setDraft(true); // new copy starts as draft

        if (src.getOrganizers() != null && !src.getOrganizers().isEmpty()) {
            copy.setOrganizers(new java.util.LinkedHashSet<>(src.getOrganizers()));
        }

        Activity saved = activityRepository.save(copy);
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
                isAdminOrManager = userOpt.map(user -> 
                    user.getRole() == Role.ADMIN || 
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
                isAdminOrManager = userOpt.map(user -> 
                    user.getRole() == Role.ADMIN || 
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

            String err = validateRequest(request);
            if (err != null)
                return new Response(false, err, null);

            Activity a = opt.get();

            applyRequestToEntity(request, a);

            Set<Department> organizers = resolveOrganizers(request.getOrganizerIds());
            a.getOrganizers().clear();
            a.getOrganizers().addAll(organizers);

            Activity saved = activityRepository.save(a);

            // Auto-register students if flags changed
            // Note: autoRegisterStudents will skip if activity is draft
            logger.debug("Activity updated (id={}, name={}, isDraft={}, isImportant={}, mandatoryForFacultyStudents={})", 
                saved.getId(), saved.getName(), saved.isDraft(), saved.isImportant(), saved.isMandatoryForFacultyStudents());
            autoRegisterStudents(saved);

            return new Response(true, "Activity updated successfully", toResponse(saved));
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
    public List<Activity> getActivitiesByScoreType(ScoreType scoreType) {
        return activityRepository.findByScoreTypeAndIsDeletedFalseOrderByStartDateAsc(scoreType);
    }

    @Override
    public List<Activity> getActivitiesByMonth(LocalDate start, LocalDate end) {
        return activityRepository.findInMonth(start, end);
    }

    @Override
    public List<Activity> getActivitiesForDepartment(Long departmentId) {
        return activityRepository.findForDepartment(departmentId);
    }

    @Override
    public List<Activity> listForCurrentUser(String username) {
        Long deptId = studentRepository.findDepartmentIdByUsername(username);
        if (deptId == null)
            return Collections.emptyList();
        return activityRepository.findForDepartment(deptId);
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
            result.put("maxPoints", activity.getMaxPoints());
            result.put("scoreType", activity.getScoreType());

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
                result.put("registrationId", registration.getId());
                result.put("status", registration.getStatus());
                result.put("registeredDate", registration.getRegisteredDate());
                result.put("canCancel",
                        !registration.getStatus().equals(vn.campuslife.enumeration.RegistrationStatus.APPROVED));
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
        if (r.getScoreType() == null)
            return "Score type is required";
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
        a.setScoreType(req.getScoreType());
        a.setDescription(req.getDescription());
        a.setStartDate(req.getStartDate());
        a.setEndDate(req.getEndDate());

        a.setRequiresSubmission(Boolean.TRUE.equals(req.getRequiresSubmission()));
        a.setMaxPoints(req.getMaxPoints());

        a.setRegistrationStartDate(req.getRegistrationStartDate());
        a.setRegistrationDeadline(req.getRegistrationDeadline());

        a.setShareLink(req.getShareLink());
        a.setImportant(Boolean.TRUE.equals(req.getIsImportant()));
        // Set isDraft: if explicitly provided, use it; otherwise default to true (draft)
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
        if (req.getRequiresApproval() != null) a.setRequiresApproval(req.getRequiresApproval());
        a.setMandatoryForFacultyStudents(Boolean.TRUE.equals(req.getMandatoryForFacultyStudents()));
        a.setPenaltyPointsIncomplete(req.getPenaltyPointsIncomplete());
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
        dto.setScoreType(a.getScoreType());
        dto.setDescription(a.getDescription());
        dto.setStartDate(a.getStartDate());
        dto.setEndDate(a.getEndDate());

        dto.setRequiresSubmission(a.isRequiresSubmission());
        dto.setMaxPoints(a.getMaxPoints());

        dto.setRegistrationStartDate(a.getRegistrationStartDate());
        dto.setRegistrationDeadline(a.getRegistrationDeadline());

        dto.setShareLink(a.getShareLink());
        dto.setImportant(a.isImportant());
        dto.setDraft(a.isDraft());
        dto.setBannerUrl(a.getBannerUrl());
        dto.setLocation(a.getLocation());

        dto.setTicketQuantity(a.getTicketQuantity());
        dto.setBenefits(a.getBenefits());
        dto.setRequirements(a.getRequirements());
        dto.setContactInfo(a.getContactInfo());
        dto.setCheckInCode(a.getCheckInCode());
        dto.setRequiresApproval(a.isRequiresApproval());
        dto.setMandatoryForFacultyStudents(a.isMandatoryForFacultyStudents());
        dto.setPenaltyPointsIncomplete(a.getPenaltyPointsIncomplete());
        dto.setOrganizerIds(a.getOrganizers() == null ? List.of()
                : a.getOrganizers().stream().map(Department::getId).toList());

        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        dto.setCreatedBy(a.getCreatedBy());
        dto.setLastModifiedBy(a.getLastModifiedBy());
        return dto;
    }

    /**
     * Tự động đăng ký sinh viên cho activity dựa trên các flag
     */
    private void autoRegisterStudents(Activity activity) {
        try {
            // Không tự động đăng ký nếu activity là draft (chưa công bố)
            if (activity.isDraft()) {
                logger.info("Skipping auto-registration for draft activity (id={}, name={}, isDraft={})", 
                    activity.getId(), activity.getName(), activity.isDraft());
                return;
            }
            
            logger.debug("Checking auto-registration for published activity (id={}, name={}, isImportant={}, mandatoryForFacultyStudents={})", 
                activity.getId(), activity.getName(), activity.isImportant(), activity.isMandatoryForFacultyStudents());

            List<Student> studentsToRegister = new ArrayList<>();

            // Nếu isImportant = true: đăng ký cho tất cả sinh viên
            if (activity.isImportant()) {
                List<Student> allStudents = studentRepository.findAll().stream()
                        .filter(student -> !student.isDeleted())
                        .collect(Collectors.toList());
                studentsToRegister.addAll(allStudents);
                logger.info("Auto-registering {} students for important activity: {}", allStudents.size(),
                        activity.getName());
            }

            // Nếu mandatoryForFacultyStudents = true: đăng ký cho sinh viên thuộc khoa tổ
            // chức
            if (activity.isMandatoryForFacultyStudents() && !activity.getOrganizers().isEmpty()) {
                List<Long> departmentIds = activity.getOrganizers().stream()
                        .map(Department::getId)
                        .collect(Collectors.toList());

                List<Student> facultyStudents = studentRepository.findByDepartmentIdInAndIsDeletedFalse(departmentIds);
                studentsToRegister.addAll(facultyStudents);
                logger.info("Auto-registering {} faculty students for mandatory activity: {}", facultyStudents.size(),
                        activity.getName());
            }

            // Tạo registrations cho các sinh viên (chỉ những sinh viên chưa đăng ký)
            if (!studentsToRegister.isEmpty()) {
                List<ActivityRegistration> registrations = studentsToRegister.stream()
                        .distinct() // Remove duplicates
                        .filter(student -> !activityRegistrationRepository
                                .existsByActivityIdAndStudentId(activity.getId(), student.getId()))
                        .map(student -> {
                            ActivityRegistration registration = new ActivityRegistration();
                            registration.setActivity(activity);
                            registration.setStudent(student);
                            registration.setStatus(vn.campuslife.enumeration.RegistrationStatus.APPROVED);
                            registration.setRegisteredDate(java.time.LocalDateTime.now());
                            // Nếu activity thuộc series, lưu seriesId để nhận diện đăng ký chuỗi
                            if (activity.getSeriesId() != null) {
                                registration.setSeriesId(activity.getSeriesId());
                            }
                            // Tạo ticketCode cho registration
                            String code;
                            int attempts = 0;
                            do {
                                code = TicketCodeUtils.newTicketCode();
                                attempts++;
                            } while (activityRegistrationRepository.existsByTicketCode(code) && attempts < 3);
                            registration.setTicketCode(code);
                            return registration;
                        })
                        .collect(Collectors.toList());

                if (!registrations.isEmpty()) {
                    // Lưu registrations trước
                    activityRegistrationRepository.saveAll(registrations);
                    logger.info("Successfully auto-registered {} students for activity: {}", registrations.size(),
                            activity.getName());

                    // Tạo ActivityParticipation ban đầu cho mỗi registration
                    List<ActivityParticipation> participations = registrations.stream()
                            .map(reg -> {
                                ActivityParticipation participation = new ActivityParticipation();
                                participation.setRegistration(reg);
                                participation.setParticipationType(ParticipationType.REGISTERED);
                                participation.setPointsEarned(BigDecimal.ZERO);
                                participation.setDate(LocalDateTime.now());
                                return participation;
                            })
                            .collect(Collectors.toList());

                    activityParticipationRepository.saveAll(participations);
                    logger.info("Created {} initial participations for activity: {}", participations.size(),
                            activity.getName());

                    // Send notifications to each auto-registered student
                    // Wrap in try-catch to not fail auto-registration if notification fails
                    try {
                        String title;
                        String content;
                        if (activity.isImportant()) {
                            title = "Đăng ký tự động - Sự kiện quan trọng";
                            content = String.format("Bạn đã được tự động đăng ký sự kiện quan trọng: %s", activity.getName());
                        } else if (activity.isMandatoryForFacultyStudents()) {
                            title = "Đăng ký tự động - Sự kiện bắt buộc";
                            content = String.format("Bạn đã được tự động đăng ký sự kiện bắt buộc: %s", activity.getName());
                        } else {
                            title = "Đăng ký tự động";
                            content = String.format("Bạn đã được tự động đăng ký sự kiện: %s", activity.getName());
                        }

                        for (ActivityRegistration registration : registrations) {
                            try {
                                Long userId = registration.getStudent().getUser().getId();
                                Map<String, Object> metadata = new HashMap<>();
                                metadata.put("activityId", activity.getId());
                                metadata.put("activityName", activity.getName());
                                metadata.put("registrationId", registration.getId());
                                metadata.put("ticketCode", registration.getTicketCode());
                                metadata.put("isAutoRegistered", true);

                                notificationService.sendNotification(
                                        userId,
                                        title,
                                        content,
                                        NotificationType.ACTIVITY_REGISTRATION,
                                        "/activities/" + activity.getId(),
                                        metadata
                                );
                            } catch (Exception e) {
                                logger.error("Failed to send auto-registration notification to user {} for activity {}: {}", 
                                        registration.getStudent().getUser().getId(), activity.getId(), e.getMessage());
                                // Continue with next registration
                            }
                        }
                        logger.info("Sent auto-registration notifications to {} students for activity: {}", 
                                registrations.size(), activity.getName());
                    } catch (Exception e) {
                        logger.error("Failed to send auto-registration notifications for activity {}: {}", 
                                activity.getId(), e.getMessage(), e);
                        // Don't fail auto-registration if notification fails
                    }
                } else {
                    logger.info("All students already registered for activity: {}", activity.getName());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to auto-register students for activity {}: {}", activity.getId(), e.getMessage(), e);
        }
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
    //Tìm kiếm sự kiện
    @Override
    public List<Activity> searchUpcomingEvents(String keyword) {
        Specification<Activity> spec = (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            // chỉ lấy sự kiện chưa diễn ra
            predicates.add(cb.greaterThanOrEqualTo(
                    root.get("startDate"),
                    LocalDateTime.now()
            ));

            if (keyword != null && !keyword.trim().isEmpty()) {
                String k = "%" + keyword.toLowerCase() + "%";

                Join<Activity, Department> deptJoin =
                        root.join("organizers", JoinType.LEFT);

                Predicate keywordPredicate = cb.or(
                        cb.like(cb.lower(root.get("name")), k),
                        cb.like(cb.lower(root.get("description")), k),
                        cb.like(cb.lower(deptJoin.get("name")), k)
                );

                predicates.add(keywordPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return activityRepository.findAll(spec);
    }
    //sự kiện trong tháng
    @Override
    public List<Activity> getActivitiesByMonth(LocalDateTime start, LocalDateTime end) {
        return activityRepository.findInMonth(start, end);
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
                    Map.of("updatedCount", updatedCount, "totalActivities", activitiesWithoutCode.size())
            );
        } catch (Exception e) {
            logger.error("Failed to backfill checkInCodes: {}", e.getMessage(), e);
            return Response.error("Failed to backfill checkInCodes: " + e.getMessage());
        }
    }
}
