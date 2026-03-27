package vn.campuslife.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.model.Response;
import vn.campuslife.model.TaskStatsRespone;
import vn.campuslife.model.preparation.*;
import vn.campuslife.service.PreparationService;

@RestController
@RequestMapping("/api/preparation")
@RequiredArgsConstructor
public class PreparationController {

    private final PreparationService preparationService;

    @PutMapping("/activities/{activityId}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> togglePreparation(@PathVariable Long activityId, @RequestParam boolean enabled) {
        preparationService.togglePreparation(activityId, enabled);
        return ResponseEntity.ok(Response.success("Updated preparation flag"));
    }

    @GetMapping("/activities/{activityId}/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> getPreparationDashboard(@PathVariable Long activityId) {
        PreparationDashboardDto dashboard = preparationService.getPreparationDashboard(activityId);
        return ResponseEntity.ok(Response.success("OK", dashboard));
    }

    @GetMapping("/my/activity-ids")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Response> listMyPreparationActivityIds(Authentication authentication) {
        return ResponseEntity.ok(Response.success("OK", preparationService.listMyPreparationActivityIds(authentication.getName())));
    }

    @GetMapping("/activities/{activityId}/organizers")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> listOrganizers(@PathVariable Long activityId) {
        return ResponseEntity.ok(Response.success("OK", preparationService.listOrganizers(activityId)));
    }

    @PostMapping("/activities/{activityId}/organizers/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> addOrganizer(@PathVariable Long activityId, @PathVariable Long studentId) {
        preparationService.addOrganizer(activityId, studentId);
        return ResponseEntity.ok(Response.success("Added organizer"));
    }

    @DeleteMapping("/activities/{activityId}/organizers/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> removeOrganizer(@PathVariable Long activityId, @PathVariable Long studentId) {
        preparationService.removeOrganizer(activityId, studentId);
        return ResponseEntity.ok(Response.success("Removed organizer"));
    }

    @PutMapping("/activities/{activityId}/budget")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> upsertBudget(@PathVariable Long activityId, @RequestBody @Valid UpsertBudgetRequest req) {
        BudgetDto dto = preparationService.createOrUpdateBudget(new UpsertBudgetRequest(activityId, req.getTotalAmount(), req.getDescription()));
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PostMapping("/activities/{activityId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> assignTask(@PathVariable Long activityId, @RequestBody @Valid CreatePreparationTaskRequest req) {
        PreparationTaskDto dto = preparationService.assignTask(new CreatePreparationTaskRequest(
                activityId,
                req.getAssigneeId(),
                req.getTitle(),
                req.getDescription(),
                req.getDeadline()
        ));
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PutMapping("/tasks/{taskId}/status")
    @PreAuthorize("@preparationSecurity.isAssignee(#taskId, authentication)")
    public ResponseEntity<Response> updateMyTaskStatus(
            @PathVariable Long taskId,
            @RequestBody @Valid UpdatePreparationTaskStatusRequest req,
            Authentication authentication) {
        PreparationTaskDto dto = preparationService.updateMyTaskStatus(taskId, req.getStatus(), authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PostMapping("/activities/{activityId}/expenses")
    @PreAuthorize("@preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> createExpense(
            @PathVariable Long activityId,
            @RequestBody @Valid CreateExpenseRequest req,
            Authentication authentication) {
        ExpenseDto dto = preparationService.createExpense(
                new CreateExpenseRequest(activityId, req.getAmount(), req.getDescription(), req.getEvidenceUrl()),
                authentication.getName()
        );
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/activities/{activityId}/expenses")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> listExpenses(
            @PathVariable Long activityId,
            @RequestParam(required = false, defaultValue = "ALL") String status) {
        return ResponseEntity.ok(Response.success("OK", preparationService.listExpenses(activityId, status)));
    }

    @PutMapping("/expenses/{expenseId}/approval")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> approveExpense(
            @PathVariable Long expenseId,
            @RequestBody @Valid ApproveExpenseRequest req) {
        ExpenseDto dto = preparationService.approveExpense(expenseId, Boolean.TRUE.equals(req.getApproved()));
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PostMapping("/activities/{activityId}/expenses/evidence")
    @PreAuthorize("@preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> uploadEvidence(@PathVariable Long activityId, @RequestParam("file") MultipartFile file) {
        UploadResultDto dto = preparationService.uploadExpenseEvidence(file);
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/stats/{id}")
    public ResponseEntity<TaskStatsRespone> getStats(@PathVariable Long id) {
        TaskStatsRespone stats = preparationService.getStudentStats(id);
        return ResponseEntity.ok(stats);
    }
}
