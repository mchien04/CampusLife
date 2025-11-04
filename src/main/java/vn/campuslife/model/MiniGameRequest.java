package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.MiniGameType;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MiniGameRequest {
    private String title;
    private String description;
    private MiniGameType type;
    private Integer questionCount;
    private Integer timeLimit;
    private BigDecimal rewardPoints;
    private Boolean isActive;
    private Long activityId;
    private Integer requiredCorrectAnswers;
    private List<MiniGameQuizQuestionRequest> questions;
}