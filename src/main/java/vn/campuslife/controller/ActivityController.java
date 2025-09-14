package vn.campuslife.controller;

import vn.campuslife.entity.Activity;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.CreateActivityRequest;
import vn.campuslife.model.Response;
import vn.campuslife.service.ActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private static final Logger logger = LoggerFactory.getLogger(ActivityController.class);

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
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
    public ResponseEntity<Response> getAllActivities() {
        try {
            Response response = activityService.getAllActivities();
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
    public ResponseEntity<Response> getActivityById(@PathVariable Long id) {
        try {
            Response response = activityService.getActivityById(id);
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


    @GetMapping("/score-type/{scoreType}")
    public ResponseEntity<List<Activity>> getByScoreType(@PathVariable ScoreType scoreType) {
        return ResponseEntity.ok(activityService.getActivitiesByScoreType(scoreType));
    }


    @GetMapping("/month")
    public List<Activity> getByMonth(@RequestParam(required = false) Integer year,
                                     @RequestParam(required = false) Integer month) {
        YearMonth ym = (year == null || month == null) ? YearMonth.now() : YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.plusMonths(1).atDay(1);
        return activityService.getActivitiesByMonth(start, end);
    }

    @GetMapping("/department/{deptId}")
    public List<Activity> byDepartment(@PathVariable Long deptId) {
        return activityService.getActivitiesForDepartment(deptId);
    }
    @GetMapping("/my")
    public List<Activity> myActivities(org.springframework.security.core.Authentication auth) {
        String username = (auth != null) ? auth.getName() : null;
        if (username == null) return List.of();
        return activityService.listForCurrentUser(username);
    }
}
