package vn.campuslife.model.activity.minigame;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.model.score.ActivityScoreRuleRequest;
import vn.campuslife.model.activity.quiz.CreateMiniGameRequest;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinigameActivityUpdateRequest {
    // Shell fields
    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<Long> organizerIds;
    private Boolean requiresApproval;
    private Integer ticketQuantity;
    private Boolean isImportant;
    private Boolean mandatoryForFacultyStudents;
    private Boolean isDraft;
    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationDeadline;
    private String bannerUrl;
    private String shareLink;
    private List<ActivityScoreRuleRequest> scoreRules;

    // Quiz fields
    private QuizConfigRequest quiz;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizConfigRequest {
        private String title;
        private String description;
        private Integer questionCount;
        private Integer timeLimit;
        private Integer requiredCorrectAnswers;
        private Integer maxAttempts;
        private Boolean showAnswers;
        private List<CreateMiniGameRequest.QuestionRequest> questions;
    }
}
