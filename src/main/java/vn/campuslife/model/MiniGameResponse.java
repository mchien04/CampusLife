package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.MiniGame;
import vn.campuslife.enumeration.MiniGameType;

import java.math.BigDecimal;

/**
 * Response DTO cho MiniGame
 * Chỉ trả về các field cần thiết, tránh overfetching
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniGameResponse {
    private Long id;
    private String title;
    private String description;
    private Integer questionCount;
    private Integer timeLimit;
    private Integer requiredCorrectAnswers;
    private BigDecimal rewardPoints;
    private Boolean isActive;
    private MiniGameType type;
    private Long activityId;

    public static MiniGameResponse fromEntity(MiniGame miniGame) {
        MiniGameResponse response = new MiniGameResponse();
        response.setId(miniGame.getId());
        response.setTitle(miniGame.getTitle());
        response.setDescription(miniGame.getDescription());
        response.setQuestionCount(miniGame.getQuestionCount());
        response.setTimeLimit(miniGame.getTimeLimit());
        response.setRequiredCorrectAnswers(miniGame.getRequiredCorrectAnswers());
        response.setRewardPoints(miniGame.getRewardPoints());
        response.setIsActive(miniGame.isActive());
        response.setType(miniGame.getType());
        if (miniGame.getActivity() != null) {
            response.setActivityId(miniGame.getActivity().getId());
        }
        return response;
    }
}

