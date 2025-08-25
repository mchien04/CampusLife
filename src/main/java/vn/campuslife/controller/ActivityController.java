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
}