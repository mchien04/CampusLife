package vn.campuslife.controller.score;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.entity.User;
import vn.campuslife.exception.ForbiddenException;
import vn.campuslife.model.Response;
import vn.campuslife.model.score.BulkManualScoreRequest;
import vn.campuslife.model.score.ManualScoreRequest;
import vn.campuslife.model.score.ManualScoreReverseRequest;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.security.department.DepartmentRequestScope;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.service.ManualScoreService;

@RestController
@RequestMapping("/api/scores/manual")
@RequiredArgsConstructor
public class ManualScoreController {

    private final ManualScoreService manualScoreService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<Response> createManualAdjustment(
            @Valid @RequestBody ManualScoreRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        User actor = resolveUser(authentication);
        DepartmentScope scope = currentScope(httpRequest);
        Response resp = manualScoreService.createManualAdjustment(request, actor, scope);
        return ResponseEntity.ok(resp);
    }

    /**
     * Nhập điểm thủ công cho nhiều sinh viên cùng lúc.
     * Bắt buộc chọn {@code semesterId} (học kỳ tích điểm) ở cấp batch.
     */
    @PostMapping("/bulk")
    public ResponseEntity<Response> createBulkManualAdjustments(
            @Valid @RequestBody BulkManualScoreRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        User actor = resolveUser(authentication);
        DepartmentScope scope = currentScope(httpRequest);
        Response resp = manualScoreService.createBulkManualAdjustments(request, actor, scope);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<Response> reverseManualAdjustment(
            @PathVariable Long id,
            @Valid @RequestBody ManualScoreReverseRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        User actor = resolveUser(authentication);
        DepartmentScope scope = currentScope(httpRequest);
        Response resp = manualScoreService.reverseManualAdjustment(id, request.getReason(), actor, scope);
        return ResponseEntity.ok(resp);
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ForbiddenException("Authentication required");
        }
        return userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ForbiddenException("User not found"));
    }

    private DepartmentScope currentScope(HttpServletRequest request) {
        return DepartmentRequestScope.get(request).orElse(null);
    }
}
