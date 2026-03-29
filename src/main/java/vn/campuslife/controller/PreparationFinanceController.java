package vn.campuslife.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.enumeration.AllocationAdjustmentStatus;
import vn.campuslife.enumeration.ExpenseStatus;
import vn.campuslife.model.Response;
import vn.campuslife.model.preparation.*;
import vn.campuslife.service.FileUploadService;
import vn.campuslife.service.PreparationFinanceService;

import java.util.List;

@RestController
@RequestMapping("/api/preparation")
@RequiredArgsConstructor
public class PreparationFinanceController {
    private final PreparationFinanceService financeService;
    private final FileUploadService fileUploadService;

    @PutMapping("/activities/{activityId}/budget")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> upsertActivityBudget(
            @PathVariable Long activityId,
            @RequestBody @Valid UpsertActivityBudgetRequest request) {
        ActivityBudgetDto dto = financeService.upsertActivityBudget(activityId, request);
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/activities/{activityId}/budget")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> getActivityBudget(@PathVariable Long activityId) {
        ActivityBudgetDto dto = financeService.getActivityBudget(activityId);
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PutMapping("/tasks/{taskId}/allocation")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> allocateTaskAmount(
            @PathVariable Long taskId,
            @RequestBody @Valid AllocateTaskAmountRequest request) {
        PreparationTaskDto dto = financeService.allocateTaskAmount(taskId, request);
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PostMapping("/tasks/{taskId}/allocation-adjustments")
    @PreAuthorize("@preparationFinanceSecurity.isTaskMember(#taskId, authentication)")
    public ResponseEntity<Response> createAllocationAdjustmentRequest(
            @PathVariable Long taskId,
            @RequestBody @Valid CreateAllocationAdjustmentRequest request,
            Authentication authentication) {
        AllocationAdjustmentRequestDto dto = financeService.createAllocationAdjustmentRequest(taskId, request, authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/activities/{activityId}/allocation-adjustments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> listAllocationAdjustmentRequests(
            @PathVariable Long activityId,
            @RequestParam(required = false) AllocationAdjustmentStatus status) {
        List<AllocationAdjustmentRequestDto> dtos = financeService.listAllocationAdjustmentRequests(activityId, status);
        return ResponseEntity.ok(Response.success("OK", dtos));
    }

    @PutMapping("/allocation-adjustments/{requestId}/admin-decision")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> adminDecisionAllocationAdjustment(
            @PathVariable Long requestId,
            @RequestBody @Valid AdminDecisionAllocationAdjustmentRequest request,
            Authentication authentication) {
        AllocationAdjustmentRequestDto dto = financeService.adminDecisionAllocationAdjustment(
                requestId,
                Boolean.TRUE.equals(request.getApproved()),
                request.getCategoryId(),
                authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PostMapping("/tasks/{taskId}/members/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isTaskLeader(#taskId, authentication)")
    public ResponseEntity<Response> addTaskMember(@PathVariable Long taskId, @PathVariable Long studentId) {
        financeService.addTaskMember(taskId, studentId);
        return ResponseEntity.ok(Response.success("OK"));
    }

    @PostMapping("/tasks/{taskId}/fund-advances")
    @PreAuthorize("@preparationFinanceSecurity.isTaskLeader(#taskId, authentication)")
    public ResponseEntity<Response> requestFundAdvance(
            @PathVariable Long taskId,
            @RequestBody @Valid CreateFundAdvanceRequest request,
            Authentication authentication) {
        FundAdvanceDto dto = financeService.requestFundAdvance(taskId, request, authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PutMapping("/fund-advances/{fundAdvanceId}/admin-decision")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> adminDecisionFundAdvance(
            @PathVariable Long fundAdvanceId,
            @RequestBody @Valid ApproveFundAdvanceRequest request,
            Authentication authentication) {
        FundAdvanceDto dto = financeService.adminDecisionFundAdvance(
                fundAdvanceId,
                Boolean.TRUE.equals(request.getApproved()),
                authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PutMapping("/fund-advances/{fundAdvanceId}/return")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> adminReturnFundAdvance(
            @PathVariable Long fundAdvanceId,
            Authentication authentication) {
        FundAdvanceDto dto = financeService.adminReturnFundAdvance(fundAdvanceId, authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/tasks/{taskId}/fund-advances")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isTaskLeader(#taskId, authentication)")
    public ResponseEntity<Response> listFundAdvances(@PathVariable Long taskId) {
        List<FundAdvanceDto> dtos = financeService.listFundAdvancesByTask(taskId);
        return ResponseEntity.ok(Response.success("OK", dtos));
    }

    @GetMapping("/tasks/{taskId}/fund-advance-source-suggestions")
    @PreAuthorize("@preparationFinanceSecurity.isTaskLeader(#taskId, authentication)")
    public ResponseEntity<Response> suggestFundAdvanceSources(
            @PathVariable Long taskId,
            @RequestParam(required = false) String amount) {
        List<FundAdvanceSourceSuggestionDto> dtos = financeService.suggestFundAdvanceSources(taskId, amount);
        return ResponseEntity.ok(Response.success("OK", dtos));
    }

    @GetMapping("/activities/{activityId}/fund-advance-debts")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> listFundAdvanceDebts(
            @PathVariable Long activityId,
            @RequestParam(required = false) Long studentId) {
        List<FundAdvanceDebtDto> dtos = financeService.listFundAdvanceDebts(activityId, studentId);
        return ResponseEntity.ok(Response.success("OK", dtos));
    }

    @PostMapping("/tasks/{taskId}/expenses/evidence")
    @PreAuthorize("@preparationFinanceSecurity.isTaskMember(#taskId, authentication)")
    public ResponseEntity<Response> uploadExpenseEvidence(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file) {
        String url = fileUploadService.uploadFile(file);
        return ResponseEntity.ok(Response.success("OK", new UploadResultDto(url)));
    }

    @PostMapping("/tasks/{taskId}/expenses")
    @PreAuthorize("@preparationFinanceSecurity.isTaskMember(#taskId, authentication)")
    public ResponseEntity<Response> createExpense(
            @PathVariable Long taskId,
            @RequestBody @Valid CreateExpenseRequest request,
            Authentication authentication) {
        ExpenseDto dto = financeService.createExpense(
                new CreateExpenseRequest(taskId, request.getCategoryId(), request.getAmount(), request.getDescription(), request.getEvidenceUrl()),
                authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PutMapping("/expenses/{expenseId}/leader-decision")
    @PreAuthorize("@preparationFinanceSecurity.canLeaderDecideExpense(#expenseId, authentication)")
    public ResponseEntity<Response> leaderDecision(
            @PathVariable Long expenseId,
            @RequestBody @Valid ApproveExpenseRequest request,
            Authentication authentication) {
        ExpenseDto dto = financeService.leaderDecision(expenseId, Boolean.TRUE.equals(request.getApproved()), authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PutMapping("/expenses/{expenseId}/admin-decision")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> adminDecision(
            @PathVariable Long expenseId,
            @RequestBody @Valid ApproveExpenseRequest request,
            Authentication authentication) {
        ExpenseDto dto = financeService.adminDecision(expenseId, Boolean.TRUE.equals(request.getApproved()), authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/activities/{activityId}/expenses")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> listExpenses(
            @PathVariable Long activityId,
            @RequestParam(required = false) ExpenseStatus status) {
        List<ExpenseDto> dtos = financeService.listExpensesByActivity(activityId, status);
        return ResponseEntity.ok(Response.success("OK", dtos));
    }

    @GetMapping("/activities/{activityId}/financial-report")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> getFinancialReport(@PathVariable Long activityId) {
        FinancialReportDto dto = financeService.getFinancialReport(activityId);
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/activities/{activityId}/reports/finance-overview")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> getFinanceOverviewReport(@PathVariable Long activityId) {
        FinanceOverviewReportDto dto = financeService.getFinanceOverviewReport(activityId);
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/activities/{activityId}/reports/cash-flow")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> getCashFlowReport(@PathVariable Long activityId) {
        CashFlowReportDto dto = financeService.getCashFlowReport(activityId);
        return ResponseEntity.ok(Response.success("OK", dto));
    }
}
