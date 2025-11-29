package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.MiniGameAttempt;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO cho API GET /api/minigames/{miniGameId}/attempts/my
 * Trả về danh sách attempts của student (không có chi tiết questions)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniGameAttemptResponse {
    private Long id;
    private String status;
    private Integer correctCount;
    private Integer totalQuestions;
    private BigDecimal pointsEarned;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    public static MiniGameAttemptResponse fromEntity(MiniGameAttempt attempt, BigDecimal pointsEarned) {
        MiniGameAttemptResponse response = new MiniGameAttemptResponse();
        response.setId(attempt.getId());
        response.setStatus(attempt.getStatus().toString());
        response.setCorrectCount(attempt.getCorrectCount());
        response.setTotalQuestions(attempt.getMiniGame().getQuestionCount());
        response.setPointsEarned(pointsEarned);
        response.setStartedAt(attempt.getStartedAt());
        response.setSubmittedAt(attempt.getSubmittedAt());
        return response;
    }
}

