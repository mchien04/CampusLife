package vn.campuslife.controller.activity;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.StandardActivityCreateRequest;
import vn.campuslife.model.activity.StandardActivityUpdateRequest;
import vn.campuslife.security.department.DepartmentRequestScope;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.security.department.DepartmentScopeRouting;
import vn.campuslife.service.StandardActivityService;

@RestController
@RequestMapping("/api/activities/standard")
@RequiredArgsConstructor
public class StandardActivityController {

    private final StandardActivityService standardActivityService;
    private final DepartmentScopeRouting departmentScopeRouting;

    @PostMapping
    public ResponseEntity<Response> createStandardActivity(
            @RequestBody StandardActivityCreateRequest request,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? standardActivityService.createActivity(request, scope)
                : standardActivityService.createActivity(request);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Response> updateStandardActivity(
            @PathVariable Long id,
            @RequestBody StandardActivityUpdateRequest request,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? standardActivityService.updateActivity(id, request, scope)
                : standardActivityService.updateActivity(id, request);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getStandardActivity(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? standardActivityService.getActivity(id, scope)
                : standardActivityService.getActivity(id);
        return response.isStatus() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    private DepartmentScope currentScope(HttpServletRequest request) {
        return DepartmentRequestScope.get(request).orElse(null);
    }
}
