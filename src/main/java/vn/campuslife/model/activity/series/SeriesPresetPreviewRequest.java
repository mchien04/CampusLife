package vn.campuslife.model.activity.series;

import lombok.Data;
import vn.campuslife.enumeration.SeriesPresetCode;

@Data
public class SeriesPresetPreviewRequest {
    private SeriesPresetCode presetCode;
    private SeriesPresetConfig presetConfig;
}
