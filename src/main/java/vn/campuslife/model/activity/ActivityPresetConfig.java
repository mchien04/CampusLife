package vn.campuslife.model.activity;

import lombok.Data;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;

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
}
