package vn.campuslife.controller;

import vn.campuslife.model.CreateActivityRequest;
import vn.campuslife.model.Response;
import vn.campuslife.service.ActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            // Log request data for debugging
            logger.info("=== CREATE ACTIVITY REQUEST ===");
            logger.info("Name: {}", request.getName());
            logger.info("Type: {}", request.getType());
            logger.info("BannerUrl: {}", request.getBannerUrl());
            logger.info("BannerUrl type: {}",
                    request.getBannerUrl() != null ? request.getBannerUrl().getClass().getName() : "null");
            logger.info("===============================");

            Response response = activityService.createActivity(request);
            if (response.isStatus()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Error creating activity: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new Response(false, "Server error occurred", null));
        }
    }

    @GetMapping
    public ResponseEntity<Response> getAllActivities() {
        try {
            Response response = activityService.getAllActivities();
            if (response.isStatus()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Error retrieving activities: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new Response(false, "Server error occurred", null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getActivityById(@PathVariable Long id) {
        try {
            Response response = activityService.getActivityById(id);
            if (response.isStatus()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving activity with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new Response(false, "Server error occurred", null));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Response> updateActivity(@PathVariable Long id, @RequestBody CreateActivityRequest request) {
        try {
            Response response = activityService.updateActivity(id, request);
            if (response.isStatus()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Error updating activity with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new Response(false, "Server error occurred", null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response> deleteActivity(@PathVariable Long id) {
        try {
            Response response = activityService.deleteActivity(id);
            if (response.isStatus()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Error deleting activity with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new Response(false, "Server error occurred", null));
        }
    }
}