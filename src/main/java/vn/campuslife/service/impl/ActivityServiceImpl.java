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

            // Verify department exists
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseGet(() -> {
                        return null;
                    });
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
            activityResponse.setMaxPoints(savedActivity.getMaxPoints() != null ? savedActivity.getMaxPoints().doubleValue() : null);
            activityResponse.setRegistrationDeadline(savedActivity.getRegistrationDeadline());
            activityResponse.setShareLink(savedActivity.getShareLink());
            activityResponse.setCreatedAt(savedActivity.getCreatedAt());
            activityResponse.setUpdatedAt(savedActivity.getUpdatedAt());

            return new Response(true, "Activity created successfully", activityResponse);
        } catch (Exception e) {
            logger.error("Failed to create activity: {}", e.getMessage(), e);
            return new Response(false, "Failed to create activity due to server error", null);
        }
    }
}