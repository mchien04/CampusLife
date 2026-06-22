package vn.campuslife.model.activity.series;

import lombok.Data;
import vn.campuslife.enumeration.SeriesPresetCode;

import java.util.ArrayList;
import java.util.List;

@Data
public class SeriesPresetDefinitionResponse {
    private SeriesPresetCode code;
    private String displayName;
    private String description;
    private List<String> notes = new ArrayList<>();
}
