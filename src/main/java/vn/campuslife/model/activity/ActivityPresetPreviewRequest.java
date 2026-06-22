package vn.campuslife.model.activity;

import lombok.Data;
import vn.campuslife.enumeration.ActivityPresetCode;
import vn.campuslife.enumeration.ActivityType;

@Data
public class ActivityPresetPreviewRequest {
    private ActivityPresetCode presetCode;
    private ActivityType type;
    private Boolean requiresSubmission;
    private ActivityPresetConfig presetConfig;
}
