package vn.campuslife.model.activity.series;

import lombok.Data;
import vn.campuslife.enumeration.ScoreRuleAudience;
import vn.campuslife.enumeration.ScoreType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class SeriesPresetConfig {
    private ScoreType primaryScoreType;
    private Map<Integer, Integer> milestonePoints = new LinkedHashMap<>();
    private Boolean minimumRequirementEnabled;
    private Integer minimumRequiredEvents;
    private Integer minimumPenaltyPoints;
    private ScoreRuleAudience audience;
    private List<Long> departmentIds;
}
