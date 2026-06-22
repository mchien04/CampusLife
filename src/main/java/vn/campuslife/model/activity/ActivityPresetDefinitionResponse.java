package vn.campuslife.model.activity;

import lombok.Data;
import vn.campuslife.enumeration.ActivityPresetCode;
import vn.campuslife.enumeration.ActivityType;

import java.util.ArrayList;
import java.util.List;

@Data
public class ActivityPresetDefinitionResponse {
    private ActivityPresetCode code;
    private String displayName;
    private String description;
    private List<ActivityType> recommendedActivityTypes = new ArrayList<>();
    private Boolean defaultRequiresSubmission;
    private List<String> notes = new ArrayList<>();
}
