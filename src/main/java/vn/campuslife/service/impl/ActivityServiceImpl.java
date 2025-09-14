package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Department;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.ActivityResponse;
import vn.campuslife.model.CreateActivityRequest;
import vn.campuslife.model.Response;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.service.ActivityService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityServiceImpl.class);

    private final ActivityRepository activityRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;


    @Override
    @Transactional
    public Response createActivity(CreateActivityRequest request) {
        try {

            String err = validateRequest(request);
            if (err != null) return new Response(false, err, null);

            Set<Department> organizers = resolveOrganizers(request.getOrganizerIds());

            Activity a = new Activity();
            applyRequestToEntity(request, a);
            a.setOrganizers(organizers);

            Activity saved = activityRepository.save(a);
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
            if (opt.isEmpty()) return new Response(false, "Activity not found", null);
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
            if (opt.isEmpty()) return new Response(false, "Activity not found", null);

            String err = validateRequest(request);
            if (err != null) return new Response(false, err, null);

            Activity a = opt.get();

            applyRequestToEntity(request, a);


            Set<Department> organizers = resolveOrganizers(request.getOrganizerIds());
            a.getOrganizers().clear();
            a.getOrganizers().addAll(organizers);

            Activity saved = activityRepository.save(a);
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
            if (opt.isEmpty()) return new Response(false, "Activity not found", null);

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
        if (deptId == null) return Collections.emptyList();
        return activityRepository.findForDepartment(deptId);
    }


    private String validateRequest(CreateActivityRequest r) {
        if (r.getName() == null || r.getName().isBlank()) return "Activity name is required";
        if (r.getType() == null) return "Activity type is required";
        if (r.getScoreType() == null) return "Score type is required";
        if (r.getStartDate() == null || r.getEndDate() == null) return "Start date and end date are required";
        if (r.getStartDate().isAfter(r.getEndDate())) return "Start date must be before end date";
        if (r.getLocation() == null || r.getLocation().isBlank()) return "Location is required";
        if (r.getOrganizerIds() == null || r.getOrganizerIds().isEmpty()) return "Organizer ids are required";
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
        if (organizerIds == null || organizerIds.isEmpty()) return new LinkedHashSet<>();
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
}
