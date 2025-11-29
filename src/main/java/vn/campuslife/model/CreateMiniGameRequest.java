package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO cho API POST /api/minigames
 * Tạo minigame với quiz
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMiniGameRequest {
    private Long activityId;
    private String title;
    private String description;
    private Integer questionCount;
    private Integer timeLimit;
    private Integer requiredCorrectAnswers;
    private BigDecimal rewardPoints;
    private List<QuestionRequest> questions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionRequest {
        private String questionText;
        private List<OptionRequest> options;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OptionRequest {
            private String text;
            private Boolean isCorrect;
        }
    }
}

