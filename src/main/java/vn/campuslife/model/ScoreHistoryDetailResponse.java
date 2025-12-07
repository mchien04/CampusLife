package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreHistoryDetailResponse {
    private Long id;
    private BigDecimal oldScore;
    private BigDecimal newScore;
    private LocalDateTime changeDate;
    private String reason;
    private Long activityId;
    private String activityName;
    private Long seriesId;
    private String seriesName;
    private String sourceType; // "ACTIVITY", "MINIGAME", "MILESTONE", "RECALCULATED"
    private String changedByUsername;
    private String changedByFullName;
}

