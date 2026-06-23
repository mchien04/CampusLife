package vn.campuslife.controller.activity;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.minigame.MinigameActivityCreateRequest;
import vn.campuslife.model.activity.minigame.MinigameActivityUpdateRequest;
import vn.campuslife.service.MinigameActivityService;

@RestController
@RequestMapping("/api/activities/minigame")
@RequiredArgsConstructor
public class MinigameActivityController {

    private final MinigameActivityService minigameActivityService;

    @PostMapping
    public ResponseEntity<Response> createMinigame(@RequestBody MinigameActivityCreateRequest request) {
        Response response = minigameActivityService.createMinigame(request);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Response> updateMinigame(@PathVariable Long id, @RequestBody MinigameActivityUpdateRequest request) {
        Response response = minigameActivityService.updateMinigame(id, request);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getMinigame(@PathVariable Long id) {
        Response response = minigameActivityService.getMinigame(id);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
}
