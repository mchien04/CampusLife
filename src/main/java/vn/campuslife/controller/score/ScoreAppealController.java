package vn.campuslife.controller.score;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.ScoreAppealStatus;
import vn.campuslife.exception.BadRequestException;
import vn.campuslife.exception.ForbiddenException;
import vn.campuslife.model.Response;
import vn.campuslife.model.score.CreateScoreAppealRequest;
import vn.campuslife.model.score.ScoreAppealDecisionRequest;
import vn.campuslife.model.score.ScoreAppealMessageRequest;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.security.department.DepartmentRequestScope;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.service.ScoreAppealService;

import java.util.List;

@RestController
@RequestMapping("/api/scores/appeals")
@RequiredArgsConstructor
public class ScoreAppealController {

    private final ScoreAppealService scoreAppealService;
    private final UserRepository userRepository;

    /**
     * Upload ảnh minh chứng (student). Trả về public URLs để gắn vào create appeal.
     */
    @PostMapping(value = "/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response> uploadEvidence(
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication) {
        User actor = resolveUser(authentication);
        return ResponseEntity.ok(scoreAppealService.uploadEvidence(files, actor));
    }

    @PostMapping
    public ResponseEntity<Response> createAppeal(
            @Valid @RequestBody CreateScoreAppealRequest request,
            Authentication authentication) {
        User actor = resolveUser(authentication);
        return ResponseEntity.ok(scoreAppealService.createAppeal(request, actor));
    }

    @GetMapping("/my")
    public ResponseEntity<Response> listMyAppeals(Authentication authentication) {
        User actor = resolveUser(authentication);
        return ResponseEntity.ok(scoreAppealService.listMyAppeals(actor));
    }

    @GetMapping
    public ResponseEntity<Response> listAppeals(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            HttpServletRequest request) {
        ScoreAppealStatus statusEnum = parseStatus(status);
        DepartmentScope scope = currentScope(request);
        return ResponseEntity.ok(scoreAppealService.listAppeals(
                statusEnum, semesterId, studentId, page, size, scope));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getAppeal(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        User actor = resolveUser(authentication);
        DepartmentScope scope = currentScope(request);
        return ResponseEntity.ok(scoreAppealService.getAppeal(id, actor, scope));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<Response> addMessage(
            @PathVariable Long id,
            @Valid @RequestBody ScoreAppealMessageRequest requestBody,
            Authentication authentication,
            HttpServletRequest request) {
        User actor = resolveUser(authentication);
        DepartmentScope scope = currentScope(request);
        return ResponseEntity.ok(scoreAppealService.addMessage(id, requestBody, actor, scope));
    }

    /**
     * Xem trước điểm sẽ thay đổi như thế nào nếu quyết định với adjustedPoints (không ghi DB).
     */
    @PostMapping("/{id}/decide/preview")
    public ResponseEntity<Response> previewDecision(
            @PathVariable Long id,
            @Valid @RequestBody ScoreAppealDecisionRequest requestBody,
            Authentication authentication,
            HttpServletRequest request) {
        User actor = resolveUser(authentication);
        DepartmentScope scope = currentScope(request);
        return ResponseEntity.ok(scoreAppealService.previewDecision(id, requestBody, actor, scope));
    }

    @PutMapping("/{id}/decide")
    public ResponseEntity<Response> decide(
            @PathVariable Long id,
            @Valid @RequestBody ScoreAppealDecisionRequest requestBody,
            Authentication authentication,
            HttpServletRequest request) {
        User actor = resolveUser(authentication);
        DepartmentScope scope = currentScope(request);
        return ResponseEntity.ok(scoreAppealService.decide(id, requestBody, actor, scope));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<Response> close(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        User actor = resolveUser(authentication);
        DepartmentScope scope = currentScope(request);
        return ResponseEntity.ok(scoreAppealService.close(id, actor, scope));
    }

    @PutMapping("/{id}/withdraw")
    public ResponseEntity<Response> withdraw(
            @PathVariable Long id,
            Authentication authentication) {
        User actor = resolveUser(authentication);
        return ResponseEntity.ok(scoreAppealService.withdraw(id, actor));
    }

    private ScoreAppealStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ScoreAppealStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + status);
        }
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
