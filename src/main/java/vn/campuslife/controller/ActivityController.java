package vn.campuslife.controller;

import vn.campuslife.entity.Activity;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.CreateActivityRequest;
import vn.campuslife.model.Response;
import vn.campuslife.service.ActivityPhotoService;
import vn.campuslife.service.ActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private static final Logger logger = LoggerFactory.getLogger(ActivityController.class);

    private final ActivityService activityService;
    private final ActivityPhotoService photoService;
    public ActivityController(ActivityService activityService, ActivityPhotoService photoService) {
        this.activityService = activityService;
        this.photoService = photoService;
    }
    @PostMapping
    public ResponseEntity<Response> createActivity(@RequestBody CreateActivityRequest request) {
        try {
            logger.info("=== CREATE ACTIVITY REQUEST ===");
            logger.info("Name: {}", request.getName());
            logger.info("Type: {}", request.getType());
            logger.info("ScoreType: {}", request.getScoreType());
            logger.info("BannerUrl: {}", request.getBannerUrl());
            logger.info("===============================");
            Response response = activityService.createActivity(request);

            return response.isStatus()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error creating activity: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new Response(false, "Server error occurred", null));
        }
    }

    @GetMapping
    public ResponseEntity<Response> getAllActivities(org.springframework.security.core.Authentication auth) {
        try {
            String username = (auth != null) ? auth.getName() : null;
            Response response = activityService.getAllActivities(username);
            return response.isStatus()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error retrieving activities: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new Response(false, "Server error occurred", null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getActivityById(@PathVariable Long id, 
            org.springframework.security.core.Authentication auth) {
        try {
            String username = (auth != null) ? auth.getName() : null;
            Response response = activityService.getActivityById(id, username);
            return response.isStatus()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error retrieving activity with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new Response(false, "Server error occurred", null));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Response> updateActivity(@PathVariable Long id,
            @RequestBody CreateActivityRequest request) {
        try {
            Response response = activityService.updateActivity(id, request);
            return response.isStatus()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error updating activity with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new Response(false, "Server error occurred", null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response> deleteActivity(@PathVariable Long id) {
        try {
            Response response = activityService.deleteActivity(id);
            return response.isStatus()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error deleting activity with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new Response(false, "Server error occurred", null));
        }
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<Response> publish(@PathVariable Long id) {
        Response response = activityService.publishActivity(id);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/unpublish")
    public ResponseEntity<Response> unpublish(@PathVariable Long id) {
        Response response = activityService.unpublishActivity(id);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/{id}/copy")
    public ResponseEntity<Response> copy(@PathVariable Long id, @RequestParam(required = false) Integer offsetDays) {
        Response response = activityService.copyActivity(id, offsetDays);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/score-type/{scoreType}")
    public ResponseEntity<List<Activity>> getByScoreType(@PathVariable ScoreType scoreType) {
        return ResponseEntity.ok(activityService.getActivitiesByScoreType(scoreType));
    }


    @GetMapping("/department/{deptId}")
    public List<Activity> byDepartment(@PathVariable Long deptId) {
        return activityService.getActivitiesForDepartment(deptId);
    }

    @GetMapping("/my")
    public List<Activity> myActivities(org.springframework.security.core.Authentication auth) {
        String username = (auth != null) ? auth.getName() : null;
        if (username == null)
            return List.of();
        return activityService.listForCurrentUser(username);
    }

    /**
     * Kiểm tra activity có yêu cầu nộp bài không
     */
    @GetMapping("/{activityId}/requires-submission")
    public ResponseEntity<Response> checkRequiresSubmission(@PathVariable Long activityId) {
        try {
            Response response = activityService.checkRequiresSubmission(activityId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking submission requirement for activity {}: {}", activityId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new Response(false, "Server error occurred", null));
        }
    }

    /**
     * Kiểm tra trạng thái đăng ký của student cho activity
     */
    @GetMapping("/{activityId}/registration-status")
    public ResponseEntity<Response> checkRegistrationStatus(@PathVariable Long activityId,
            org.springframework.security.core.Authentication auth) {
        try {
            String username = (auth != null) ? auth.getName() : null;
            if (username == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Authentication required", null));
            }

            Response response = activityService.checkRegistrationStatus(activityId, username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking registration status for activity {}: {}", activityId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new Response(false, "Server error occurred", null));
        }
    }

    /**
     * Debug endpoint để kiểm tra thông tin user hiện tại
     */
    @GetMapping("/debug/user-info")
    public ResponseEntity<Response> debugUserInfo(org.springframework.security.core.Authentication auth) {
        try {
            if (auth == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "No authentication found", null));
            }

            java.util.Map<String, Object> userInfo = new java.util.HashMap<>();
            userInfo.put("username", auth.getName());
            userInfo.put("authorities", auth.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .collect(java.util.stream.Collectors.toList()));
            userInfo.put("isAuthenticated", auth.isAuthenticated());
            userInfo.put("principal", auth.getPrincipal().getClass().getSimpleName());

            return ResponseEntity.ok(new Response(true, "User info retrieved", userInfo));
        } catch (Exception e) {
            logger.error("Error getting user info: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new Response(false, "Server error occurred", null));
        }
    }
    //Tìm kiếm sự kiện
    @GetMapping("/upcoming")
    public ResponseEntity<List<Activity>> search(
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        List<Activity> list = activityService.searchUpcomingEvents(keyword);
        return ResponseEntity.ok(list);
    }
    //Tìm sự kiện trong tháng
    @GetMapping("/month")
    public List<Activity> getByMonth(@RequestParam(required = false) Integer year,
                                     @RequestParam(required = false) Integer month) {

        YearMonth ym = (year == null || month == null)
                ? YearMonth.now()
                : YearMonth.of(year, month);

        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.plusMonths(1).atDay(1);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atStartOfDay();

        return activityService.getActivitiesByMonth(start, end);
    }
    //Hien tat ca hinh anh
    @GetMapping("/photos/all")
    public ResponseEntity<Response> getAllPhotos() {
        return ResponseEntity.ok(photoService.getAllPhotos());
    }
}
