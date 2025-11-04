package vn.campuslife.model;

import lombok.Data;

@Data
public class MiniGameSummary {
    private String gameType;
    private Integer totalQuestions;
    private Integer timeLimit;
    private boolean isActive;
}
