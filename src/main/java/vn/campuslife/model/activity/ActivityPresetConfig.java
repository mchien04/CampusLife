package vn.campuslife.model.activity;

import lombok.Data;
import vn.campuslife.enumeration.ScoreRuleAudience;
import vn.campuslife.enumeration.ScoreSemesterPolicy;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ActivityPresetConfig {
    private ScoreType primaryScoreType;
    private BigDecimal participationPoints;
    private BigDecimal participationFailPoints;
    private Boolean noShowPenaltyEnabled;
    private BigDecimal noShowPenaltyPoints;
    private ScoreType noShowPenaltyScoreType;
    private BigDecimal submissionPassPoints;
    private BigDecimal submissionFailPoints;
    private BigDecimal taskOverduePenaltyPoints;
    private BigDecimal minigameExhaustedPenaltyPoints;
    private ScoreType bonusScoreType;
    private BigDecimal bonusPoints;
    private ScoreRuleAudience audience;
    private ScoreSemesterPolicy semesterPolicy;
    private Long explicitSemesterId;
    private List<Long> departmentIds;
}
