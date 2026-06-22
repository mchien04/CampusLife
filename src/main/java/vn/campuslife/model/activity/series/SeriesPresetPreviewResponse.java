package vn.campuslife.model.activity.series;

import lombok.Data;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.enumeration.SeriesPresetCode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class SeriesPresetPreviewResponse {
    private SeriesPresetCode presetCode;
    private ScoreType scoreType;
    private Map<Integer, Integer> milestonePoints = new LinkedHashMap<>();
    private Boolean minimumRequirementEnabled;
    private Integer minimumRequiredEvents;
    private Integer minimumPenaltyPoints;
    private List<String> notes = new ArrayList<>();
}
