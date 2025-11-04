package vn.campuslife.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.ActivityRatingRequest;
import vn.campuslife.model.ActivityRegistrationRequest;
import vn.campuslife.model.Response;
import vn.campuslife.service.ActivityRatingService;
import vn.campuslife.service.ActivityRegistrationService;
import vn.campuslife.service.StudentService;

import java.util.Optional;

@RestController
@RequestMapping("/api/ratings")

@RequiredArgsConstructor
public class ActivityRatingController {
    @Autowired
    private ActivityRatingService ratingService;
    @PostMapping("/create")
    public ResponseEntity<Response> createRating(
            @RequestParam Long activityId,
            @RequestParam Long studentId,
            @RequestParam float rating,
            @RequestParam(required = false) String comment
    ) {
        return ResponseEntity.ok(
                ratingService.createActivityRating(activityId, studentId, rating, comment)
        );
    }
    @GetMapping("/by")
    public ResponseEntity<?> getActivityRatingByActivityIdAndStudent(
            @RequestParam Long activityId,
            @RequestParam Long studentId) {

        return ratingService.getActivityRatingByActivityIdAndStudent(activityId, studentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/stats/{activityId}")
    public ResponseEntity<Response> getActivityRatingStats(@PathVariable Long activityId) {
        return ResponseEntity.ok(ratingService.getActivityRatingStats(activityId));
    }


}
