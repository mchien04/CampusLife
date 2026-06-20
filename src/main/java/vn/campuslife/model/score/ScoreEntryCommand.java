package vn.campuslife.model.score;

import lombok.Builder;
import lombok.Data;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.ScoreEntrySourceType;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;

@Data
@Builder
public class ScoreEntryCommand {
    private Long studentId;
    private Long activityId;
    private Long ruleId;
    private Long semesterId;
    private ScoreType scoreType;
    private ScoreEntrySourceType sourceType;
    private Long sourceId;
    private BigDecimal points;
    private String reason;
    private User actor;
}
