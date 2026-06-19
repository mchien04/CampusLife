package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.enumeration.ScoreRuleTrigger;
import vn.campuslife.enumeration.ScoreRuleCalculation;
import vn.campuslife.enumeration.ScoreRuleAudience;
import vn.campuslife.enumeration.ScoreSemesterPolicy;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ActivityScoreRuleRequest {
    private ScoreType scoreType;
    private ScoreRuleTrigger triggerType;
    private ScoreRuleCalculation calculation;
    private BigDecimal points;
    private BigDecimal failPoints;
    private ScoreRuleAudience audience;
    private ScoreSemesterPolicy semesterPolicy;
    private Long explicitSemesterId;
    private List<Long> departmentIds;
    private Boolean enabled;
}
