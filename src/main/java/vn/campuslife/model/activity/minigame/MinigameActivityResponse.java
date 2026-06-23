package vn.campuslife.model.activity.minigame;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.model.score.ActivityScoreRuleResponse;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinigameActivityResponse {
    // Shell fields
    private Long id;
    private String name;
    private ActivityType type = ActivityType.MINIGAME;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isDraft;
    private String bannerUrl;
    private String shareLink;
    private Boolean isImportant;
    private String checkInCode;
    private List<ActivityScoreRuleResponse> scoreRules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Quiz fields
    private QuizConfigResponse quiz;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizConfigResponse {
        private Long id;
        private String title;
        private Integer questionCount;
        private Integer timeLimit;
        private Integer requiredCorrectAnswers;
        private Integer maxAttempts;
        private Boolean showAnswers;
        private Boolean isActive;
    }
}
