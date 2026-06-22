package vn.campuslife.model.activity.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.MiniGameAttempt;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO cho API POST /api/minigames/attempts/{attemptId}/submit
 * Trả về kết quả sau khi submit attempt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAttemptResponse {
    private Long id;
    private String status;
    private Integer correctCount;
    private Integer totalQuestions;
    private BigDecimal pointsEarned;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Integer requiredCorrectAnswers;
    private Object participation; // ActivityParticipation nếu PASSED, null nếu FAILED

    public static SubmitAttemptResponse fromEntity(MiniGameAttempt attempt, Object participation, BigDecimal pointsEarned) {
        SubmitAttemptResponse response = new SubmitAttemptResponse();
        response.setId(attempt.getId());
        response.setStatus(attempt.getStatus().toString());
        response.setCorrectCount(attempt.getCorrectCount());
        response.setTotalQuestions(attempt.getMiniGame().getQuestionCount());
        response.setStartedAt(attempt.getStartedAt());
        response.setSubmittedAt(attempt.getSubmittedAt());
        response.setRequiredCorrectAnswers(attempt.getMiniGame().getRequiredCorrectAnswers());
        response.setParticipation(participation);
        response.setPointsEarned(pointsEarned);

        return response;
    }
}


