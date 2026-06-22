package vn.campuslife.model.activity;

import lombok.Data;
import vn.campuslife.enumeration.ActivityPresetCode;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.model.score.ActivityScoreRuleRequest;

import java.util.ArrayList;
import java.util.List;

@Data
public class ActivityPresetPreviewResponse {
    private ActivityPresetCode presetCode;
    private ActivityType activityType;
    private boolean requiresSubmission;
    private List<ActivityScoreRuleRequest> scoreRules = new ArrayList<>();
    private List<String> notes = new ArrayList<>();
}
