package vn.campuslife.model.score;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualScoreResponse {
    private Long adjustmentId;
    private Long scoreEntryId;
    private Long studentId;
    private Long semesterId;
    private ScoreType scoreType;
    private BigDecimal points;
    private String reason;
    private Long activityId;
    private Long createdByUserId;
    private LocalDateTime createdAt;
}
