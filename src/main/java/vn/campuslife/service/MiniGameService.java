package vn.campuslife.service;


import vn.campuslife.entity.*;
import vn.campuslife.model.*;

import java.util.List;

public interface MiniGameService {
    MiniGameResponse create(MiniGameRequest request);
    List<MiniGameResponse> getAll();
    MiniGame createMiniGameForActivity(Activity activity, MiniGameConfig config);

    void updateMiniGameByActivity(Long activityId, MiniGameRequest request);
    StartAttemptResponse startAttempt(Long activityId, Long studentId);
    SubmitResultResponse submitMiniGame(Long activityId, Long studentId, SubmitRequest req);

}