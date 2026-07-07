package vn.campuslife.controller.activity;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.minigame.MinigameActivityCreateRequest;
import vn.campuslife.model.activity.minigame.MinigameActivityUpdateRequest;
import vn.campuslife.security.department.DepartmentRequestScope;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.service.MinigameActivityService;

@RestController
@RequestMapping("/api/activities/minigame")
@RequiredArgsConstructor
public class MinigameActivityController {

    private final MinigameActivityService minigameActivityService;

    @PostMapping
    public ResponseEntity<Response> createMinigame(
            @RequestBody MinigameActivityCreateRequest request,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = hasManagerScope(scope)
                ? minigameActivityService.createMinigame(request, scope)
                : minigameActivityService.createMinigame(request);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Response> updateMinigame(
            @PathVariable Long id,
            @RequestBody MinigameActivityUpdateRequest request,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = hasManagerScope(scope)
                ? minigameActivityService.updateMinigame(id, request, scope)
                : minigameActivityService.updateMinigame(id, request);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getMinigame(@PathVariable Long id, HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = hasManagerScope(scope)
                ? minigameActivityService.getMinigame(id, scope)
                : minigameActivityService.getMinigame(id);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    private DepartmentScope currentScope(HttpServletRequest request) {
        return DepartmentRequestScope.get(request).orElse(null);
    }

    private boolean hasManagerScope(DepartmentScope scope) {
        return scope != null && scope.manager() && !scope.departmentIds().isEmpty();
    }
}
