package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.service.StatisticsService;
import vn.campuslife.service.StudentService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    private final StatisticsService statisticsService;
    private final StudentService studentService;

    /**
     * Dashboard tổng quan
     * ADMIN, MANAGER: Xem toàn bộ
     * STUDENT: Xem thống kê cá nhân
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Response> getDashboardOverview(Authentication authentication) {
        try {
            Long studentId = null;
            if (authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
                try {
                    studentId = studentService.getStudentIdByUsername(authentication.getName());
                } catch (Exception e) {
                    logger.warn("Could not get student ID for user: {}", authentication.getName());
                }
            }
            Response response = statisticsService.getDashboardOverview(studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in getDashboardOverview: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Response.error("Failed to get dashboard overview: " + e.getMessage()));
        }
    }

    /**
     * Thống kê Activities
     * ADMIN, MANAGER only
     */
    @GetMapping("/activities")
    public ResponseEntity<Response> getActivityStatistics(
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) String scoreType,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDateTime start = null;
            LocalDateTime end = null;
            if (startDate != null && !startDate.isEmpty()) {
                start = LocalDateTime.parse(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                end = LocalDateTime.parse(endDate);
            }

            Response response = statisticsService.getActivityStatistics(
                    activityType, scoreType, departmentId, start, end);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in getActivityStatistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Response.error("Failed to get activity statistics: " + e.getMessage()));
        }
    }

    /**
     * Thống kê Students
     * ADMIN, MANAGER only
     */
    @GetMapping("/students")
    public ResponseEntity<Response> getStudentStatistics(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long semesterId) {
        try {
            Response response = statisticsService.getStudentStatistics(departmentId, classId, semesterId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in getStudentStatistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Response.error("Failed to get student statistics: " + e.getMessage()));
        }
    }

    /**
     * Thống kê Scores
     * ADMIN, MANAGER: Xem tất cả
     * STUDENT: Chỉ xem điểm của mình
     */
    @GetMapping("/scores")
    public ResponseEntity<Response> getScoreStatistics(
            @RequestParam(required = false) String scoreType,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long classId,
            Authentication authentication) {
        try {
            Long studentId = null;
            if (authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
                try {
                    studentId = studentService.getStudentIdByUsername(authentication.getName());
                } catch (Exception e) {
                    logger.warn("Could not get student ID for user: {}", authentication.getName());
                }
            }

            Response response = statisticsService.getScoreStatistics(
                    scoreType, semesterId, departmentId, classId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in getScoreStatistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Response.error("Failed to get score statistics: " + e.getMessage()));
        }
    }

    /**
     * Thống kê Series
     * ADMIN, MANAGER, STUDENT
     */
    @GetMapping("/series")
    public ResponseEntity<Response> getSeriesStatistics(
            @RequestParam(required = false) Long seriesId,
            @RequestParam(required = false) Long semesterId) {
        try {
            Response response = statisticsService.getSeriesStatistics(seriesId, semesterId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in getSeriesStatistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Response.error("Failed to get series statistics: " + e.getMessage()));
        }
    }

    /**
     * Thống kê MiniGames
     * ADMIN, MANAGER, STUDENT
     */
    @GetMapping("/minigames")
    public ResponseEntity<Response> getMiniGameStatistics(
            @RequestParam(required = false) Long miniGameId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDateTime start = null;
            LocalDateTime end = null;
            if (startDate != null && !startDate.isEmpty()) {
                start = LocalDateTime.parse(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                end = LocalDateTime.parse(endDate);
            }

            Response response = statisticsService.getMiniGameStatistics(miniGameId, start, end);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in getMiniGameStatistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Response.error("Failed to get minigame statistics: " + e.getMessage()));
        }
    }

    /**
     * Thống kê theo thời gian
     * ADMIN, MANAGER only
     */
    @GetMapping("/timeline")
    public ResponseEntity<Response> getTimeBasedStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String groupBy) {
        try {
            LocalDateTime start = null;
            LocalDateTime end = null;
            if (startDate != null && !startDate.isEmpty()) {
                start = LocalDateTime.parse(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                end = LocalDateTime.parse(endDate);
            }

            Response response = statisticsService.getTimeBasedStatistics(start, end, groupBy);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in getTimeBasedStatistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Response.error("Failed to get time-based statistics: " + e.getMessage()));
        }
    }
}

