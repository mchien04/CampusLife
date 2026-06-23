package vn.campuslife.service;

import vn.campuslife.model.Response;
import vn.campuslife.model.activity.minigame.MinigameActivityCreateRequest;
import vn.campuslife.model.activity.minigame.MinigameActivityUpdateRequest;

public interface MinigameActivityService {
    Response createMinigame(MinigameActivityCreateRequest request);
    Response updateMinigame(Long id, MinigameActivityUpdateRequest request);
    Response getMinigame(Long id);
}
