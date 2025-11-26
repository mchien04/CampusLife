package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.service.MiniGameService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/minigames")
@RequiredArgsConstructor
public class MiniGameController {

    private static final Logger logger = LoggerFactory.getLogger(MiniGameController.class);

    private final MiniGameService miniGameService;

    /**
     * Tạo minigame với quiz
     */
    @PostMapping
    public ResponseEntity<Response> createMiniGame(@RequestBody Map<String, Object> request) {
        try {
            Long activityId = Long.valueOf(request.get("activityId").toString());
            String title = (String) request.get("title");
            String description = (String) request.get("description");
            Integer questionCount = Integer.valueOf(request.get("questionCount").toString());
            Integer timeLimit = request.get("timeLimit") != null 
                    ? Integer.valueOf(request.get("timeLimit").toString()) : null;
            Integer requiredCorrectAnswers = request.get("requiredCorrectAnswers") != null 
                    ? Integer.valueOf(request.get("requiredCorrectAnswers").toString()) : null;
            BigDecimal rewardPoints = request.get("rewardPoints") != null 
                    ? new BigDecimal(request.get("rewardPoints").toString()) : null;
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questions = (List<Map<String, Object>>) request.get("questions");

            Response response = miniGameService.createMiniGame(activityId, title, description, questionCount,
                    timeLimit, requiredCorrectAnswers, rewardPoints, questions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to create minigame: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to create minigame: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy minigame theo activity ID
     */
    @GetMapping("/activity/{activityId}")
    public ResponseEntity<Response> getMiniGameByActivity(@PathVariable Long activityId) {
        try {
            Response response = miniGameService.getMiniGameByActivity(activityId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get minigame: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get minigame: " + e.getMessage(), null));
        }
    }

    /**
     * Student bắt đầu làm quiz
     */
    @PostMapping("/{miniGameId}/start")
    public ResponseEntity<Response> startAttempt(
            @PathVariable Long miniGameId,
            Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = miniGameService.startAttempt(miniGameId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to start attempt: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to start attempt: " + e.getMessage(), null));
        }
    }

    /**
     * Student nộp bài quiz
     */
    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<Response> submitAttempt(
            @PathVariable Long attemptId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> answersMap = (Map<String, Object>) request.get("answers");
            Map<Long, Long> answers = new java.util.HashMap<>();
            if (answersMap != null) {
                for (Map.Entry<String, Object> entry : answersMap.entrySet()) {
                    Long questionId = Long.valueOf(entry.getKey());
                    Long optionId = Long.valueOf(entry.getValue().toString());
                    answers.put(questionId, optionId);
                }
            }

            Response response = miniGameService.submitAttempt(attemptId, studentId, answers);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to submit attempt: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to submit attempt: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy lịch sử attempts của student
     */
    @GetMapping("/{miniGameId}/attempts/my")
    public ResponseEntity<Response> getMyAttempts(
            @PathVariable Long miniGameId,
            Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }

            Response response = miniGameService.getStudentAttempts(studentId, miniGameId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get attempts: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get attempts: " + e.getMessage(), null));
        }
    }

    private final vn.campuslife.service.StudentService studentService;

    /**
     * Helper method to get student ID from authentication
     */
    private Long getStudentIdFromAuth(Authentication authentication) {
        try {
            String username = authentication.getName();
            return studentService.getStudentIdByUsername(username);
        } catch (Exception e) {
            return null;
        }
    }
}

