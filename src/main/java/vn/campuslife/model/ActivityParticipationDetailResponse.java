package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ParticipationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityParticipationDetailResponse {
    private Long id;
    private Long activityId;
    private String activityName;
    private ActivityType activityType;
    private Long seriesId;
    private String seriesName;
    private BigDecimal pointsEarned;
    private ParticipationType participationType;
    private LocalDateTime date;
    private Boolean isCompleted;
    private String sourceType; // "ACTIVITY", "MINIGAME"
}

