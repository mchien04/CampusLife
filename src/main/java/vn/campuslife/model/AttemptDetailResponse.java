package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.MiniGameAttempt;
import vn.campuslife.entity.MiniGameQuiz;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Response DTO cho API GET /api/minigames/attempts/{attemptId}
 * Trả về chi tiết attempt với đáp án đúng và kết quả
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttemptDetailResponse {
    private Long id;
    private String status;
    private Integer correctCount;
    private Integer totalQuestions;
    private BigDecimal pointsEarned;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Integer requiredCorrectAnswers;
    private List<QuizQuestionDetailResponse> questions;

    public static AttemptDetailResponse fromEntities(
            MiniGameAttempt attempt,
            MiniGameQuiz quiz,
            Map<Long, Long> studentAnswers,
            BigDecimal pointsEarned) {
        return fromEntities(attempt, quiz, studentAnswers, pointsEarned, null);
    }

    public static AttemptDetailResponse fromEntities(
            MiniGameAttempt attempt,
            MiniGameQuiz quiz,
            Map<Long, Long> studentAnswers,
            BigDecimal pointsEarned,
            String publicUrl) {
        AttemptDetailResponse response = new AttemptDetailResponse();
        response.setId(attempt.getId());
        response.setStatus(attempt.getStatus().toString());
        response.setCorrectCount(attempt.getCorrectCount());
        response.setTotalQuestions(attempt.getMiniGame().getQuestionCount());
        response.setPointsEarned(pointsEarned);
        response.setStartedAt(attempt.getStartedAt());
        response.setSubmittedAt(attempt.getSubmittedAt());
        response.setRequiredCorrectAnswers(attempt.getMiniGame().getRequiredCorrectAnswers());

        if (quiz != null && quiz.getQuestions() != null && studentAnswers != null) {
            response.setQuestions(quiz.getQuestions().stream()
                    .sorted((q1, q2) -> {
                        Integer order1 = q1.getDisplayOrder() != null ? q1.getDisplayOrder() : 0;
                        Integer order2 = q2.getDisplayOrder() != null ? q2.getDisplayOrder() : 0;
                        return order1.compareTo(order2);
                    })
                    .map(q -> QuizQuestionDetailResponse.fromEntity(q, studentAnswers, publicUrl))
                    .collect(Collectors.toList()));
        }

        return response;
    }
}

