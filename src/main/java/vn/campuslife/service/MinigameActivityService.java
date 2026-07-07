package vn.campuslife.service;

import vn.campuslife.model.Response;
import vn.campuslife.model.activity.minigame.MinigameActivityCreateRequest;
import vn.campuslife.model.activity.minigame.MinigameActivityUpdateRequest;
import vn.campuslife.security.department.DepartmentScope;

public interface MinigameActivityService {
    Response createMinigame(MinigameActivityCreateRequest request);

    Response createMinigame(MinigameActivityCreateRequest request, DepartmentScope scope);

    Response updateMinigame(Long id, MinigameActivityUpdateRequest request);

    Response updateMinigame(Long id, MinigameActivityUpdateRequest request, DepartmentScope scope);

    Response getMinigame(Long id);

    Response getMinigame(Long id, DepartmentScope scope);
}
