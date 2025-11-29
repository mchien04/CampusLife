package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO cho API PUT /api/minigames/{miniGameId}
 * Cập nhật minigame
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMiniGameRequest {
    private String title;
    private String description;
    private Integer questionCount;
    private Integer timeLimit;
    private Integer requiredCorrectAnswers;
    private BigDecimal rewardPoints;
    private List<CreateMiniGameRequest.QuestionRequest> questions;
}

