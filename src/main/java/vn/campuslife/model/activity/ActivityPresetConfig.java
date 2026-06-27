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

    // Per-rule audience overrides (fallback to top-level audience/semesterPolicy/departmentIds)
    private ScoreRuleAudience submissionAudience;
    private ScoreSemesterPolicy submissionSemesterPolicy;
    private Long submissionExplicitSemesterId;
    private List<Long> submissionDepartmentIds;

    private ScoreRuleAudience participationAudience;
    private ScoreSemesterPolicy participationSemesterPolicy;
    private Long participationExplicitSemesterId;
    private List<Long> participationDepartmentIds;

    private ScoreRuleAudience noShowAudience;
    private ScoreSemesterPolicy noShowSemesterPolicy;
    private Long noShowExplicitSemesterId;
    private List<Long> noShowDepartmentIds;

    private ScoreRuleAudience taskOverdueAudience;
    private ScoreSemesterPolicy taskOverdueSemesterPolicy;
    private Long taskOverdueExplicitSemesterId;
    private List<Long> taskOverdueDepartmentIds;

    private ScoreRuleAudience bonusAudience;
    private ScoreSemesterPolicy bonusSemesterPolicy;
    private Long bonusExplicitSemesterId;
    private List<Long> bonusDepartmentIds;

    private ScoreRuleAudience minigamePassedAudience;
    private ScoreSemesterPolicy minigamePassedSemesterPolicy;
    private Long minigamePassedExplicitSemesterId;
    private List<Long> minigamePassedDepartmentIds;

    private ScoreRuleAudience minigameExhaustedAudience;
    private ScoreSemesterPolicy minigameExhaustedSemesterPolicy;
    private Long minigameExhaustedExplicitSemesterId;
    private List<Long> minigameExhaustedDepartmentIds;
}
