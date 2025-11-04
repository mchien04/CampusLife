package vn.campuslife.service;


import vn.campuslife.entity.*;
import vn.campuslife.model.MiniGameConfig;
import vn.campuslife.model.MiniGameRequest;
import vn.campuslife.model.MiniGameResponse;

import java.util.List;

public interface MiniGameService {
    MiniGameResponse create(MiniGameRequest request);
    List<MiniGameResponse> getAll();
    MiniGame createMiniGameForActivity(Activity activity, MiniGameConfig config);

    void updateMiniGameByActivity(Long activityId, MiniGameRequest request);

}