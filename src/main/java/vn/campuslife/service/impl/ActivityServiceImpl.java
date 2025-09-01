package vn.campuslife.service.impl;

import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Department;
import vn.campuslife.model.CreateActivityRequest;
import vn.campuslife.model.ActivityResponse;
import vn.campuslife.model.Response;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.DepartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.service.ActivityService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityServiceImpl implements ActivityService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityServiceImpl.class);

    private final ActivityRepository activityRepository;
    private final DepartmentRepository departmentRepository;

    public ActivityServiceImpl(ActivityRepository activityRepository, DepartmentRepository departmentRepository) {
        this.activityRepository = activityRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    @Transactional
    public Response createActivity(CreateActivityRequest request) {
        try {
            // Validate required fields
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return new Response(false, "Activity name is required", null);
            }
            if (request.getType() == null) {
                return new Response(false, "Activity type is required", null);
            }
            if (request.getStartDate() == null || request.getEndDate() == null) {
                return new Response(false, "Start date and end date are required", null);
            }
            if (request.getStartDate().isAfter(request.getEndDate())) {
                return new Response(false, "Start date must be before end date", null);
            }
            if (request.getDepartmentId() == null) {
                return new Response(false, "Department ID is required", null);
            }
            if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
                return new Response(false, "Location is required", null);
            }

            // Verify department exists
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseGet(() -> null);
            if (department == null) {
                return new Response(false, "Department not found", null);
            }

            // Create activity entity
            Activity activity = new Activity();
            activity.setName(request.getName());
            activity.setType(request.getType());
            activity.setDescription(request.getDescription());
            activity.setStartDate(request.getStartDate());
            activity.setEndDate(request.getEndDate());
            activity.setDepartment(department);
            activity.setRequiresSubmission(request.isRequiresSubmission());
            activity.setMaxPoints(request.getMaxPoints() != null ? BigDecimal.valueOf(request.getMaxPoints()) : null);
            activity.setRegistrationDeadline(request.getRegistrationDeadline());
            activity.setShareLink(request.getShareLink());
            activity.setImportant(request.isImportant());
            activity.setBannerUrl(request.getBannerUrl());
            activity.setLocation(request.getLocation());

            // Save activity
            Activity savedActivity = activityRepository.save(activity);

            // Map to response DTO
            ActivityResponse activityResponse = new ActivityResponse();
            activityResponse.setId(savedActivity.getId());
            activityResponse.setName(savedActivity.getName());
            activityResponse.setType(savedActivity.getType());
            activityResponse.setDescription(savedActivity.getDescription());
            activityResponse.setStartDate(savedActivity.getStartDate());
            activityResponse.setEndDate(savedActivity.getEndDate());
            activityResponse.setDepartmentId(savedActivity.getDepartment().getId());
            activityResponse.setRequiresSubmission(savedActivity.isRequiresSubmission());
            activityResponse.setMaxPoints(
                    savedActivity.getMaxPoints() != null ? savedActivity.getMaxPoints().doubleValue() : null);
            activityResponse.setRegistrationDeadline(savedActivity.getRegistrationDeadline());
            activityResponse.setShareLink(savedActivity.getShareLink());
            activityResponse.setImportant(savedActivity.isImportant());
            activityResponse.setBannerUrl(savedActivity.getBannerUrl());
            activityResponse.setLocation(savedActivity.getLocation());
            activityResponse.setCreatedAt(savedActivity.getCreatedAt());
            activityResponse.setUpdatedAt(savedActivity.getUpdatedAt());
            activityResponse.setCreatedBy(savedActivity.getCreatedBy());
            activityResponse.setLastModifiedBy(savedActivity.getLastModifiedBy());

            return new Response(true, "Activity created successfully", activityResponse);
        } catch (Exception e) {
            logger.error("Failed to create activity: {}", e.getMessage(), e);
            return new Response(false, "Failed to create activity due to server error", null);
        }
    }

    @Override
    public Response getAllActivities() {
        try {
            List<Activity> activities = activityRepository.findByIsDeletedFalse();
            List<ActivityResponse> activityResponses = activities.stream()
                    .map(this::mapToActivityResponse)
                    .collect(Collectors.toList());

            return new Response(true, "Activities retrieved successfully", activityResponses);
        } catch (Exception e) {
            logger.error("Failed to retrieve activities: {}", e.getMessage(), e);
            return new Response(false, "Failed to retrieve activities due to server error", null);
        }
    }

    @Override
    public Response getActivityById(Long id) {
        try {
            Activity activity = activityRepository.findByIdAndIsDeletedFalse(id)
                    .orElse(null);

            if (activity == null) {
                return new Response(false, "Activity not found", null);
            }

            ActivityResponse activityResponse = mapToActivityResponse(activity);
            return new Response(true, "Activity retrieved successfully", activityResponse);
        } catch (Exception e) {
            logger.error("Failed to retrieve activity with id {}: {}", id, e.getMessage(), e);
            return new Response(false, "Failed to retrieve activity due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response updateActivity(Long id, CreateActivityRequest request) {
        try {
            // Validate required fields
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return new Response(false, "Activity name is required", null);
            }
            if (request.getType() == null) {
                return new Response(false, "Activity type is required", null);
            }
            if (request.getStartDate() == null || request.getEndDate() == null) {
                return new Response(false, "Start date and end date are required", null);
            }
            if (request.getStartDate().isAfter(request.getEndDate())) {
                return new Response(false, "Start date must be before end date", null);
            }
            if (request.getDepartmentId() == null) {
                return new Response(false, "Department ID is required", null);
            }
            if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
                return new Response(false, "Location is required", null);
            }

            // Find existing activity
            Activity activity = activityRepository.findByIdAndIsDeletedFalse(id)
                    .orElse(null);
            if (activity == null) {
                return new Response(false, "Activity not found", null);
            }

            // Verify department exists
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElse(null);
            if (department == null) {
                return new Response(false, "Department not found", null);
            }

            // Update activity fields
            activity.setName(request.getName());
            activity.setType(request.getType());
            activity.setDescription(request.getDescription());
            activity.setStartDate(request.getStartDate());
            activity.setEndDate(request.getEndDate());
            activity.setDepartment(department);
            activity.setRequiresSubmission(request.isRequiresSubmission());
            activity.setMaxPoints(request.getMaxPoints() != null ? BigDecimal.valueOf(request.getMaxPoints()) : null);
            activity.setRegistrationDeadline(request.getRegistrationDeadline());
            activity.setShareLink(request.getShareLink());
            activity.setImportant(request.isImportant());
            activity.setBannerUrl(request.getBannerUrl());
            activity.setLocation(request.getLocation());

            // Save updated activity
            Activity savedActivity = activityRepository.save(activity);
            ActivityResponse activityResponse = mapToActivityResponse(savedActivity);

            return new Response(true, "Activity updated successfully", activityResponse);
        } catch (Exception e) {
            logger.error("Failed to update activity with id {}: {}", id, e.getMessage(), e);
            return new Response(false, "Failed to update activity due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response deleteActivity(Long id) {
        try {
            Activity activity = activityRepository.findByIdAndIsDeletedFalse(id)
                    .orElse(null);

            if (activity == null) {
                return new Response(false, "Activity not found", null);
            }

            // Soft delete
            activity.setDeleted(true);
            activityRepository.save(activity);

            return new Response(true, "Activity deleted successfully", null);
        } catch (Exception e) {
            logger.error("Failed to delete activity with id {}: {}", id, e.getMessage(), e);
            return new Response(false, "Failed to delete activity due to server error", null);
        }
    }

    private ActivityResponse mapToActivityResponse(Activity activity) {
        ActivityResponse activityResponse = new ActivityResponse();
        activityResponse.setId(activity.getId());
        activityResponse.setName(activity.getName());
        activityResponse.setType(activity.getType());
        activityResponse.setDescription(activity.getDescription());
        activityResponse.setStartDate(activity.getStartDate());
        activityResponse.setEndDate(activity.getEndDate());
        activityResponse.setDepartmentId(activity.getDepartment().getId());
        activityResponse.setRequiresSubmission(activity.isRequiresSubmission());
        activityResponse.setMaxPoints(activity.getMaxPoints() != null ? activity.getMaxPoints().doubleValue() : null);
        activityResponse.setRegistrationDeadline(activity.getRegistrationDeadline());
        activityResponse.setShareLink(activity.getShareLink());
        activityResponse.setImportant(activity.isImportant());
        activityResponse.setBannerUrl(activity.getBannerUrl());
        activityResponse.setLocation(activity.getLocation());
        activityResponse.setCreatedAt(activity.getCreatedAt());
        activityResponse.setUpdatedAt(activity.getUpdatedAt());
        activityResponse.setCreatedBy(activity.getCreatedBy());
        activityResponse.setLastModifiedBy(activity.getLastModifiedBy());
        return activityResponse;
    }
}