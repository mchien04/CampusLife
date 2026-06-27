package vn.campuslife.service;

import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.model.activity.ActivityPresetDefinitionResponse;
import vn.campuslife.model.activity.ActivityPresetPreviewRequest;
import vn.campuslife.model.activity.ActivityPresetPreviewResponse;
import vn.campuslife.model.activity.CreateActivityRequest;
import vn.campuslife.model.activity.StandardActivityCreateRequest;
import vn.campuslife.model.activity.StandardActivityUpdateRequest;
import vn.campuslife.model.activity.series.CreateSeriesRequest;
import vn.campuslife.model.activity.series.SeriesPresetDefinitionResponse;
import vn.campuslife.model.activity.series.SeriesPresetPreviewRequest;
import vn.campuslife.model.activity.series.SeriesPresetPreviewResponse;
import vn.campuslife.model.activity.series.UpdateSeriesRequest;

import java.util.List;

public interface ScorePresetService {
    List<ActivityPresetDefinitionResponse> getActivityPresetDefinitions();

    ActivityPresetPreviewResponse previewActivityPreset(ActivityPresetPreviewRequest request);

    void applyActivityPreset(CreateActivityRequest request);

    void applyActivityPreset(StandardActivityCreateRequest request);

    void applyActivityPreset(StandardActivityUpdateRequest request);

    void applyActivityPreset(StandardActivityUpdateRequest request, ActivityType effectiveType);

    List<SeriesPresetDefinitionResponse> getSeriesPresetDefinitions();

    SeriesPresetPreviewResponse previewSeriesPreset(SeriesPresetPreviewRequest request);

    void applySeriesPreset(CreateSeriesRequest request);

    void applySeriesPreset(UpdateSeriesRequest request);
}
