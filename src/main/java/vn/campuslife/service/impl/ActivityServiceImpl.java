package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.service.ActivityService;
import vn.campuslife.util.TicketCodeUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityServiceImpl.class);

    private final ActivityRepository activityRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
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
            // Auto-register students based on flags
            autoRegisterStudents(saved);
            if (request.getIsImportant()) {
                try {
                    registerAllStudents(saved.getId());
                } catch (Exception ex) {
                    logger.error("Auto register all students failed", ex);
                }
            }

            if (request.getMandatoryForFacultyStudents()) {
                try {
                    registerFacultyStudents(saved.getId(), request.getOrganizerIds());
                } catch (Exception ex) {
                    logger.error("Auto register faculty students failed", ex);
                }
            }



            return new Response(true, "Activity created successfully", toResponse(saved));
        } catch (Exception e) {
            logger.error("Failed to create activity: {}", e.getMessage(), e);
            return new Response(false, "Failed to create activity due to server error", null);
        }
    }

    @Override
    public Response getAllActivities() {
        try {
            var list = activityRepository.findByIsDeletedFalseOrderByStartDateAsc();
            var data = list.stream().map(this::toResponse).toList();
            return new Response(true, "Activities retrieved successfully", data);
        } catch (Exception e) {
            logger.error("Failed to retrieve activities: {}", e.getMessage(), e);
            return new Response(false, "Failed to retrieve activities due to server error", null);
        }
    }

    @Override
    public Response getActivityById(Long id) {
        try {
            var opt = activityRepository.findByIdAndIsDeletedFalse(id);
            if (opt.isEmpty())
                return new Response(false, "Activity not found", null);
            return new Response(true, "Activity retrieved successfully", toResponse(opt.get()));
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
        a.setBannerUrl(req.getBannerUrl());
        a.setLocation(req.getLocation());

        a.setTicketQuantity(req.getTicketQuantity());
        a.setBenefits(req.getBenefits());
        a.setRequirements(req.getRequirements());
        a.setContactInfo(req.getContactInfo());
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
        dto.setBannerUrl(a.getBannerUrl());
        dto.setLocation(a.getLocation());

        dto.setTicketQuantity(a.getTicketQuantity());
        dto.setBenefits(a.getBenefits());
        dto.setRequirements(a.getRequirements());
        dto.setContactInfo(a.getContactInfo());
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
                            return registration;
                        })
                        .collect(Collectors.toList());

                if (!registrations.isEmpty()) {
                    activityRegistrationRepository.saveAll(registrations);
                    logger.info("Successfully auto-registered {} students for activity: {}", registrations.size(),
                            activity.getName());
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

        List<Student> allStudents = studentRepository.findAll();

        List<ActivityRegistration> registrations = allStudents.stream()
                .map(student -> {
                    ActivityRegistration reg = new ActivityRegistration();
                    reg.setActivity(activity);
                    reg.setStudent(student);
                    reg.setRegisteredDate(LocalDateTime.now());
                    reg.setStatus(RegistrationStatus.PENDING);

                    String code;
                    int attempts = 0;
                    do {
                        code = TicketCodeUtils.newTicketCode();
                        attempts++;
                    } while (activityRegistrationRepository.existsByTicketCode(code) && attempts < 3);
                    reg.setTicketCode(code);

                    return reg;
                })
                .toList();

        activityRegistrationRepository.saveAll(registrations);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFacultyStudents(Long activityId, Collection<Long> departmentIds) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        List<Student> students = studentRepository.findByDepartment_IdIn(departmentIds);

        List<ActivityRegistration> registrations = students.stream()
                .map(student -> {
                    ActivityRegistration reg = new ActivityRegistration();
                    reg.setActivity(activity);
                    reg.setStudent(student);
                    reg.setRegisteredDate(LocalDateTime.now());
                    reg.setStatus(RegistrationStatus.PENDING);

                    String code;
                    int attempts = 0;
                    do {
                        code = TicketCodeUtils.newTicketCode();
                        attempts++;
                    } while (activityRegistrationRepository.existsByTicketCode(code) && attempts < 3);
                    reg.setTicketCode(code);

                    return reg;
                })
                .toList();

        activityRegistrationRepository.saveAll(registrations);
        logger.info("Auto registered {} students of departments {} for mandatory activity {}",
                registrations.size(), departmentIds, activityId);
    }


}
