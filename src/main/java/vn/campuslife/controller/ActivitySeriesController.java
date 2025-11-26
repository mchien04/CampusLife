package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.service.ActivitySeriesService;
import vn.campuslife.enumeration.ScoreType;

import java.util.Map;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
public class ActivitySeriesController {

    private static final Logger logger = LoggerFactory.getLogger(ActivitySeriesController.class);

    private final ActivitySeriesService seriesService;

    /**
     * Tạo chuỗi sự kiện mới
     */
    @PostMapping
    public ResponseEntity<Response> createSeries(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String milestonePoints = request.get("milestonePoints") != null 
                    ? request.get("milestonePoints").toString() : null;
            String scoreTypeStr = request.get("scoreType") != null 
                    ? request.get("scoreType").toString() : "REN_LUYEN";
            vn.campuslife.enumeration.ScoreType scoreType = vn.campuslife.enumeration.ScoreType.valueOf(scoreTypeStr);
            Long mainActivityId = request.get("mainActivityId") != null 
                    ? Long.valueOf(request.get("mainActivityId").toString()) : null;

            Response response = seriesService.createSeries(name, description, milestonePoints, scoreType, mainActivityId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to create series: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to create series: " + e.getMessage(), null));
        }
    }

    /**
     * Thêm activity vào chuỗi
     */
    @PostMapping("/{seriesId}/activities")
    public ResponseEntity<Response> addActivityToSeries(
            @PathVariable Long seriesId,
            @RequestBody Map<String, Object> request) {
        try {
            Long activityId = Long.valueOf(request.get("activityId").toString());
            Integer order = request.get("order") != null 
                    ? Integer.valueOf(request.get("order").toString()) : null;

            Response response = seriesService.addActivityToSeries(activityId, seriesId, order);
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
            @PathVariable Long studentId) {
        try {
            Response response = seriesService.calculateMilestonePoints(studentId, seriesId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to calculate milestone: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to calculate milestone: " + e.getMessage(), null));
        }
    }
}

