package vn.campuslife.controller.activity.series;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.series.AddActivityToSeriesRequest;
import vn.campuslife.model.activity.series.CreateSeriesActivityRequest;
import vn.campuslife.model.activity.series.CreateSeriesRequest;
import vn.campuslife.model.activity.series.SeriesPresetPreviewRequest;
import vn.campuslife.model.activity.series.SeriesPresetPreviewResponse;
import vn.campuslife.model.activity.series.UpdateSeriesRequest;
import vn.campuslife.security.department.DepartmentRequestScope;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.security.department.DepartmentScopeRouting;
import vn.campuslife.service.ActivityRegistrationService;
import vn.campuslife.service.ActivitySeriesService;
import vn.campuslife.service.ScorePresetService;

import java.util.Map;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
public class ActivitySeriesController {

    private static final Logger logger = LoggerFactory.getLogger(ActivitySeriesController.class);

    private final ActivitySeriesService seriesService;
    private final DepartmentScopeRouting departmentScopeRouting;
    private final ActivityRegistrationService activityRegistrationService;
    private final vn.campuslife.service.StudentService studentService;
    private final ScorePresetService scorePresetService;
    private final ObjectMapper objectMapper;

    /**
     * Tạo chuỗi sự kiện mới
     */
    @GetMapping("/presets")
    public ResponseEntity<Response> getSeriesPresets() {
        return ResponseEntity.ok(
                new Response(true, "Series presets retrieved successfully", scorePresetService.getSeriesPresetDefinitions()));
    }

    @PostMapping("/presets/preview")
    public ResponseEntity<Response> previewSeriesPreset(@RequestBody SeriesPresetPreviewRequest request) {
        return ResponseEntity.ok(new Response(true, "Series preset preview generated successfully",
                scorePresetService.previewSeriesPreset(request)));
    }

    @PostMapping
    public ResponseEntity<Response> createSeries(@RequestBody CreateSeriesRequest request,
            HttpServletRequest httpRequest) {
        try {
            String name = request.getName();
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Series name is required", null));
            }

            scorePresetService.applySeriesPreset(request);

            String description = request.getDescription();
            String milestonePoints = resolveMilestonePointsJson(request.getMilestonePoints());
            
            String scoreTypeStr = request.getScoreType() != null
                    ? request.getScoreType().name()
                    : "REN_LUYEN";
            vn.campuslife.enumeration.ScoreType scoreType;
            try {
                scoreType = vn.campuslife.enumeration.ScoreType.valueOf(scoreTypeStr);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid ScoreType: {}", scoreTypeStr);
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Invalid ScoreType: " + scoreTypeStr, null));
            }

            Long mainActivityId = request.getMainActivityId();
            java.time.LocalDateTime registrationStartDate = request.getRegistrationStartDate();
            java.time.LocalDateTime registrationDeadline = request.getRegistrationDeadline();
            Boolean requiresApproval = request.getRequiresApproval() != null
                    ? request.getRequiresApproval()
                    : true;
            Integer ticketQuantity = request.getTicketQuantity();
            Boolean minimumRequirementEnabled = request.getMinimumRequirementEnabled();
            Integer minimumRequiredEvents = request.getMinimumRequiredEvents();
            Integer minimumPenaltyPoints = request.getMinimumPenaltyPoints();

            DepartmentScope scope = currentScope(httpRequest);
            Response response = departmentScopeRouting.useManagerScopedPath(scope)
                    ? seriesService.createSeries(name, description, milestonePoints, scoreType,
                            mainActivityId,
                            registrationStartDate, registrationDeadline, requiresApproval, ticketQuantity,
                            minimumRequirementEnabled, minimumRequiredEvents, minimumPenaltyPoints, request.getTargetSemesterId(),
                            request.getAudience(), request.getDepartmentIds(), request.getOrganizerIds(),
                            request.getIsImportant(), request.getMandatoryForFacultyStudents(),
                            request.getIsDraft(),
                            request.getPresetCode(), scope)
                    : seriesService.createSeries(name, description, milestonePoints, scoreType,
                            mainActivityId,
                            registrationStartDate, registrationDeadline, requiresApproval, ticketQuantity,
                            minimumRequirementEnabled, minimumRequiredEvents, minimumPenaltyPoints, request.getTargetSemesterId(),
                            request.getAudience(), request.getDepartmentIds(), request.getOrganizerIds(),
                            request.getIsImportant(), request.getMandatoryForFacultyStudents(),
                            request.getIsDraft(),
                            request.getPresetCode());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument when creating series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Failed to create series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to create series: " + e.getMessage(), null));
        }
    }

    /**
     * Tạo activity trong series với các thuộc tính tối giản
     * @deprecated Use POST /{seriesId}/activities instead
     */
    @Deprecated
    @PostMapping("/{seriesId}/activities/create")
    public ResponseEntity<Response> createActivityInSeries(
            @PathVariable Long seriesId,
            @RequestBody CreateSeriesActivityRequest request,
            HttpServletRequest httpRequest) {
        try {
            String name = request.getName();
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Activity name is required", null));
            }

            DepartmentScope scope = currentScope(httpRequest);
            Response response = departmentScopeRouting.useManagerScopedPath(scope)
                    ? seriesService.createActivityInSeries(seriesId, name, request.getDescription(),
                            request.getStartDate(), request.getEndDate(), request.getLocation(), request.getOrder(),
                            request.getShareLink(), request.getBannerUrl(), request.getBenefits(), request.getRequirements(),
                            request.getContactInfo(), request.getOrganizerIds(), request.getType(), scope)
                    : seriesService.createActivityInSeries(seriesId, name, request.getDescription(),
                            request.getStartDate(), request.getEndDate(), request.getLocation(), request.getOrder(),
                            request.getShareLink(), request.getBannerUrl(), request.getBenefits(), request.getRequirements(),
                            request.getContactInfo(), request.getOrganizerIds(), request.getType());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument when creating activity in series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Failed to create activity in series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to create activity in series: " + e.getMessage(), null));
        }
    }

    @PostMapping("/{seriesId}/activities")
    public ResponseEntity<Response> createSeriesActivity(
            @PathVariable Long seriesId,
            @RequestBody vn.campuslife.model.activity.series.SeriesChildActivityCreateRequest request,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? seriesService.createSeriesActivity(seriesId, request, scope)
                : seriesService.createSeriesActivity(seriesId, request);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{seriesId}/activities/{activityId}")
    public ResponseEntity<Response> updateSeriesActivity(
            @PathVariable Long seriesId,
            @PathVariable Long activityId,
            @RequestBody vn.campuslife.model.activity.series.SeriesChildActivityUpdateRequest request,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? seriesService.updateSeriesActivity(seriesId, activityId, request, scope)
                : seriesService.updateSeriesActivity(seriesId, activityId, request);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/{seriesId}/activities/{activityId}")
    public ResponseEntity<Response> getSeriesActivity(
            @PathVariable Long seriesId,
            @PathVariable Long activityId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? seriesService.getSeriesActivity(seriesId, activityId, scope)
                : seriesService.getSeriesActivity(seriesId, activityId);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /**
     * Student đăng ký series (tự động đăng ký tất cả activities trong series)
     */
    @PostMapping("/{seriesId}/register")
    public ResponseEntity<Response> registerForSeries(
            @PathVariable Long seriesId,
            org.springframework.security.core.Authentication authentication) {
        try {
            // Get student ID from authentication
            String username = authentication.getName();
            Long studentId = studentService.getStudentIdByUsername(username);

            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = seriesService.registerForSeries(seriesId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to register for series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to register for series: " + e.getMessage(), null));
        }
    }

    @PostMapping("/{seriesId}/waitlist")
    public ResponseEntity<Response> registerForSeriesWaitlist(
            @PathVariable Long seriesId,
            org.springframework.security.core.Authentication authentication) {
        try {
            String username = authentication.getName();
            Long studentId = studentService.getStudentIdByUsername(username);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }
            Response response = seriesService.registerForSeriesWaitlist(seriesId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to join series waitlist: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to join series waitlist: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/{seriesId}/register")
    public ResponseEntity<Response> cancelSeriesRegistration(
            @PathVariable Long seriesId,
            org.springframework.security.core.Authentication authentication) {
        try {
            String username = authentication.getName();
            Long studentId = studentService.getStudentIdByUsername(username);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }
            Response response = activityRegistrationService.cancelSeriesRegistration(seriesId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to cancel series registration: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to cancel series registration: " + e.getMessage(), null));
        }
    }

    /**
     * Thêm activity vào chuỗi
     */
    @PostMapping("/{seriesId}/activities/attach")
    public ResponseEntity<Response> addActivityToSeries(
            @PathVariable Long seriesId,
            @RequestBody AddActivityToSeriesRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long activityId = request.getActivityId();
            Integer order = request.getOrder();

            DepartmentScope scope = currentScope(httpRequest);
            Response response = departmentScopeRouting.useManagerScopedPath(scope)
                    ? seriesService.addActivityToSeries(activityId, seriesId, order, scope)
                    : seriesService.addActivityToSeries(activityId, seriesId, order);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to add activity to series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to add activity to series: " + e.getMessage(), null));
        }
    }

    /**
     * Tính điểm milestone cho student
     */
    @PostMapping("/{seriesId}/students/{studentId}/calculate-milestone")
    public ResponseEntity<Response> calculateMilestone(
            @PathVariable Long seriesId,
            @PathVariable Long studentId,
            HttpServletRequest httpRequest) {
        try {
            DepartmentScope scope = currentScope(httpRequest);
            Response response = departmentScopeRouting.useManagerScopedPath(scope)
                    ? seriesService.calculateMilestonePoints(studentId, seriesId, scope)
                    : seriesService.calculateMilestonePoints(studentId, seriesId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to calculate milestone: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to calculate milestone: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy tất cả chuỗi sự kiện
     */
    @GetMapping
    public ResponseEntity<Response> getAllSeries(HttpServletRequest httpRequest) {
        try {
            DepartmentScope scope = currentScope(httpRequest);
            Response response = departmentScopeRouting.useManagerScopedPath(scope)
                    ? seriesService.getAllSeries(scope)
                    : seriesService.getAllSeries();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get all series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get all series: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy chuỗi sự kiện theo ID
     */
    @GetMapping("/{seriesId}")
    public ResponseEntity<Response> getSeriesById(@PathVariable Long seriesId, HttpServletRequest httpRequest) {
        try {
            DepartmentScope scope = currentScope(httpRequest);
            Response response = departmentScopeRouting.useManagerScopedPath(scope)
                    ? seriesService.getSeriesById(seriesId, scope)
                    : seriesService.getSeriesById(seriesId);
            return response.isStatus()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to get series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get series: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy danh sách activities trong series
     */
    @GetMapping("/{seriesId}/activities")
    public ResponseEntity<Response> getActivitiesInSeries(@PathVariable Long seriesId, HttpServletRequest httpRequest) {
        try {
            DepartmentScope scope = currentScope(httpRequest);
            Response response = departmentScopeRouting.useManagerScopedPath(scope)
                    ? seriesService.getActivitiesInSeries(seriesId, scope)
                    : seriesService.getActivitiesInSeries(seriesId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get activities in series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get activities in series: " + e.getMessage(), null));
        }
    }

    /**
     * Student xem progress của chính mình trong series
     */
    @GetMapping("/{seriesId}/progress/my")
    public ResponseEntity<Response> getMyProgress(
            @PathVariable Long seriesId,
            org.springframework.security.core.Authentication authentication) {
        try {
            // Get student ID from authentication
            String username = authentication.getName();
            Long studentId = studentService.getStudentIdByUsername(username);

            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = seriesService.getStudentProgress(seriesId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get my progress: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get my progress: " + e.getMessage(), null));
        }
    }

    /**
     * Student xem trạng thái đã đăng ký chuỗi của chính mình
     */
    @GetMapping("/{seriesId}/registration/my")
    public ResponseEntity<Response> checkMySeriesRegistration(
            @PathVariable Long seriesId,
            org.springframework.security.core.Authentication authentication) {
        try {
            String username = authentication.getName();
            Long studentId = studentService.getStudentIdByUsername(username);

            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = seriesService.checkSeriesRegistration(seriesId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to check series registration: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to check series registration: " + e.getMessage(), null));
        }
    }

    /**
     * Admin/Manager xem progress của student trong series
     */
    @GetMapping("/{seriesId}/students/{studentId}/progress")
    public ResponseEntity<Response> getStudentProgress(
            @PathVariable Long seriesId,
            @PathVariable Long studentId,
            HttpServletRequest httpRequest) {
        try {
            DepartmentScope scope = currentScope(httpRequest);
            Response response = departmentScopeRouting.useManagerScopedPath(scope)
                    ? seriesService.getStudentProgress(seriesId, studentId, scope)
                    : seriesService.getStudentProgress(seriesId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get student progress: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get student progress: " + e.getMessage(), null));
        }
    }

    /**
     * Admin/Manager xem tiến độ tham gia của tất cả sinh viên trong series
     */
    @GetMapping("/{seriesId}/progress")
    public ResponseEntity<Response> getSeriesProgress(
            @PathVariable Long seriesId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            HttpServletRequest httpRequest) {
        try {
            DepartmentScope scope = currentScope(httpRequest);
            Response response = departmentScopeRouting.useManagerScopedPath(scope)
                    ? seriesService.getSeriesProgress(seriesId, page, size, keyword, scope)
                    : seriesService.getSeriesProgress(seriesId, page, size, keyword);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get series progress: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get series progress: " + e.getMessage(), null));
        }
    }

    /**
     * Admin/Manager xem tổng quan thống kê của chuỗi sự kiện
     */
    @GetMapping("/{seriesId}/overview")
    public ResponseEntity<Response> getSeriesOverview(@PathVariable Long seriesId, HttpServletRequest httpRequest) {
        try {
            DepartmentScope scope = currentScope(httpRequest);
            Response response = departmentScopeRouting.useManagerScopedPath(scope)
                    ? seriesService.getSeriesOverview(seriesId, scope)
                    : seriesService.getSeriesOverview(seriesId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get series overview: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get series overview: " + e.getMessage(), null));
        }
    }

    /**
     * Cập nhật thông tin chuỗi sự kiện
     */
    @PutMapping("/{seriesId}")
    public ResponseEntity<Response> updateSeries(
            @PathVariable Long seriesId,
            @RequestBody UpdateSeriesRequest request,
            HttpServletRequest httpRequest) {
        try {
            scorePresetService.applySeriesPreset(request);

            String name = request.getName();
            String description = request.getDescription();
            String milestonePoints = resolveMilestonePointsJson(request.getMilestonePoints());
            
            String scoreTypeStr = request.getScoreType() != null
                    ? request.getScoreType().name()
                    : null;
            vn.campuslife.enumeration.ScoreType scoreType = null;
            if (scoreTypeStr != null) {
                try {
                    scoreType = vn.campuslife.enumeration.ScoreType.valueOf(scoreTypeStr);
                } catch (IllegalArgumentException e) {
                    logger.error("Invalid ScoreType: {}", scoreTypeStr);
                    return ResponseEntity.badRequest()
                            .body(new Response(false, "Invalid ScoreType: " + scoreTypeStr, null));
                }
            }

            Long mainActivityId = request.getMainActivityId();
            java.time.LocalDateTime registrationStartDate = request.getRegistrationStartDate();
            java.time.LocalDateTime registrationDeadline = request.getRegistrationDeadline();
            Boolean requiresApproval = request.getRequiresApproval();
            Integer ticketQuantity = request.getTicketQuantity();
            Boolean minimumRequirementEnabled = request.getMinimumRequirementEnabled();
            Integer minimumRequiredEvents = request.getMinimumRequiredEvents();
            Integer minimumPenaltyPoints = request.getMinimumPenaltyPoints();

            DepartmentScope scope = currentScope(httpRequest);
            Response response = departmentScopeRouting.useManagerScopedPath(scope)
                    ? seriesService.updateSeries(seriesId, name, description, milestonePoints, scoreType,
                            mainActivityId, registrationStartDate, registrationDeadline, requiresApproval, ticketQuantity,
                            minimumRequirementEnabled, minimumRequiredEvents, minimumPenaltyPoints, request.getTargetSemesterId(),
                            request.getAudience(), request.getDepartmentIds(), request.getOrganizerIds(),
                            request.getIsImportant(), request.getMandatoryForFacultyStudents(),
                            request.getIsDraft(),
                            request.getPresetCode(), scope)
                    : seriesService.updateSeries(seriesId, name, description, milestonePoints, scoreType,
                            mainActivityId, registrationStartDate, registrationDeadline, requiresApproval, ticketQuantity,
                            minimumRequirementEnabled, minimumRequiredEvents, minimumPenaltyPoints, request.getTargetSemesterId(),
                            request.getAudience(), request.getDepartmentIds(), request.getOrganizerIds(),
                            request.getIsImportant(), request.getMandatoryForFacultyStudents(),
                            request.getIsDraft(),
                            request.getPresetCode());
            if (response.isStatus()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument when updating series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Failed to update series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to update series: " + e.getMessage(), null));
        }
    }

    private String resolveMilestonePointsJson(Map<Integer, Integer> milestonePointsMap) {
        if (milestonePointsMap != null && !milestonePointsMap.isEmpty()) {
            try {
                return objectMapper.writeValueAsString(milestonePointsMap);
            } catch (Exception e) {
                logger.warn("Failed to serialize milestone points", e);
            }
        }
        return null;
    }

    /**
     * Xóa chuỗi sự kiện (soft delete)
     */
    @DeleteMapping("/{seriesId}")
    public ResponseEntity<Response> deleteSeries(@PathVariable Long seriesId, HttpServletRequest httpRequest) {
        try {
            DepartmentScope scope = currentScope(httpRequest);
            Response response = departmentScopeRouting.useManagerScopedPath(scope)
                    ? seriesService.deleteSeries(seriesId, scope)
                    : seriesService.deleteSeries(seriesId);
            if (response.isStatus()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Failed to delete series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to delete series: " + e.getMessage(), null));
        }
    }

    private DepartmentScope currentScope(HttpServletRequest request) {
        return DepartmentRequestScope.get(request).orElse(null);
    }
}



