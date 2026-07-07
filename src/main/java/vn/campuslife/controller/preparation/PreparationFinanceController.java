package vn.campuslife.controller.preparation;

import jakarta.servlet.http.HttpServletRequest;
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
import vn.campuslife.security.department.DepartmentRequestScope;
import vn.campuslife.security.department.DepartmentScope;
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
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isActivityPrepSupervisor(#activityId, authentication)")
    public ResponseEntity<Response> upsertActivityBudget(
            @PathVariable Long activityId,
            @RequestBody @Valid UpsertActivityBudgetRequest request,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        ActivityBudgetDto dto = hasManagerScope(scope)
                ? financeService.upsertActivityBudget(activityId, request, scope)
                : financeService.upsertActivityBudget(activityId, request);
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/activities/{activityId}/budget")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isActivityPrepSupervisor(#activityId, authentication) or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> getActivityBudget(
            @PathVariable Long activityId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        ActivityBudgetDto dto = hasManagerScope(scope)
                ? financeService.getActivityBudget(activityId, scope)
                : financeService.getActivityBudget(activityId);
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PutMapping("/tasks/{taskId}/allocation")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isTaskPrepSupervisor(#taskId, authentication)")
    public ResponseEntity<Response> allocateTaskAmount(
            @PathVariable Long taskId,
            @RequestBody @Valid AllocateTaskAmountRequest request,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        PreparationTaskDto dto = hasManagerScope(scope)
                ? financeService.allocateTaskAmount(taskId, request, scope)
                : financeService.allocateTaskAmount(taskId, request);
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PostMapping("/tasks/{taskId}/allocation-adjustments")
    @PreAuthorize("@preparationFinanceSecurity.isTaskPrepSupervisor(#taskId, authentication) or @preparationFinanceSecurity.isTaskMember(#taskId, authentication)")
    public ResponseEntity<Response> createAllocationAdjustmentRequest(
            @PathVariable Long taskId,
            @RequestBody @Valid CreateAllocationAdjustmentRequest request,
            Authentication authentication) {
        AllocationAdjustmentRequestDto dto = financeService.createAllocationAdjustmentRequest(taskId, request,
                authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/activities/{activityId}/allocation-adjustments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isActivityPrepSupervisor(#activityId, authentication)")
    public ResponseEntity<Response> listAllocationAdjustmentRequests(
            @PathVariable Long activityId,
            @RequestParam(required = false) AllocationAdjustmentStatus status,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        List<AllocationAdjustmentRequestDto> dtos = hasManagerScope(scope)
                ? financeService.listAllocationAdjustmentRequests(activityId, status, scope)
                : financeService.listAllocationAdjustmentRequests(activityId, status);
        return ResponseEntity.ok(Response.success("OK", dtos));
    }

    @PutMapping("/allocation-adjustments/{requestId}/admin-decision")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isAllocationAdjustmentPrepSupervisor(#requestId, authentication)")
    public ResponseEntity<Response> adminDecisionAllocationAdjustment(
            @PathVariable Long requestId,
            @RequestBody @Valid AdminDecisionAllocationAdjustmentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        AllocationAdjustmentRequestDto dto;
        if (Boolean.TRUE.equals(request.getApproved())
                && request.getSources() != null
                && !request.getSources().isEmpty()) {
            dto = hasManagerScope(scope)
                    ? financeService.adminDecisionAllocationAdjustmentMulti(
                            requestId, request.getSources(), authentication.getName(), scope)
                    : financeService.adminDecisionAllocationAdjustmentMulti(
                            requestId, request.getSources(), authentication.getName());
        } else {
            dto = hasManagerScope(scope)
                    ? financeService.adminDecisionAllocationAdjustment(
                            requestId,
                            Boolean.TRUE.equals(request.getApproved()),
                            request.getCategoryId(),
                            authentication.getName(),
                            scope)
                    : financeService.adminDecisionAllocationAdjustment(
                            requestId,
                            Boolean.TRUE.equals(request.getApproved()),
                            request.getCategoryId(),
                            authentication.getName());
        }
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/allocation-adjustments/{requestId}/source-suggestions")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isAllocationAdjustmentPrepSupervisor(#requestId, authentication)")
    public ResponseEntity<Response> suggestAllocationAdjustmentSources(
            @PathVariable Long requestId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        List<AllocationSourceSuggestionDto> dtos = hasManagerScope(scope)
                ? financeService.suggestAllocationAdjustmentSources(requestId, scope)
                : financeService.suggestAllocationAdjustmentSources(requestId);
        return ResponseEntity.ok(Response.success("OK", dtos));
    }

    @GetMapping("/allocation-adjustments/{requestId}/source-plan")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isAllocationAdjustmentPrepSupervisor(#requestId, authentication)")
    public ResponseEntity<Response> planAllocationAdjustmentSources(
            @PathVariable Long requestId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        List<AllocationAdjustmentSourcePlanDto> dtos = hasManagerScope(scope)
                ? financeService.planAllocationAdjustmentSources(requestId, scope)
                : financeService.planAllocationAdjustmentSources(requestId);
        return ResponseEntity.ok(Response.success("OK", dtos));
    }

    @PostMapping("/tasks/{taskId}/members/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isTaskPrepSupervisor(#taskId, authentication) or @preparationFinanceSecurity.isTaskLeader(#taskId, authentication)")
    public ResponseEntity<Response> addTaskMember(
            @PathVariable Long taskId,
            @PathVariable Long studentId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        if (hasManagerScope(scope)) {
            financeService.addTaskMember(taskId, studentId, scope);
        } else {
            financeService.addTaskMember(taskId, studentId);
        }
        return ResponseEntity.ok(Response.success("OK"));
    }

    @PostMapping("/tasks/{taskId}/fund-advances")
    @PreAuthorize("@preparationFinanceSecurity.isTaskPrepSupervisor(#taskId, authentication) or @preparationFinanceSecurity.isTaskLeader(#taskId, authentication)")
    public ResponseEntity<Response> requestFundAdvance(
            @PathVariable Long taskId,
            @RequestBody @Valid CreateFundAdvanceRequest request,
            Authentication authentication) {
        FundAdvanceDto dto = financeService.requestFundAdvance(taskId, request, authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PutMapping("/fund-advances/{fundAdvanceId}/admin-decision")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isFundAdvancePrepSupervisor(#fundAdvanceId, authentication)")
    public ResponseEntity<Response> adminDecisionFundAdvance(
            @PathVariable Long fundAdvanceId,
            @RequestBody @Valid ApproveFundAdvanceRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        FundAdvanceDto dto = hasManagerScope(scope)
                ? financeService.adminDecisionFundAdvance(
                        fundAdvanceId, Boolean.TRUE.equals(request.getApproved()), authentication.getName(), scope)
                : financeService.adminDecisionFundAdvance(
                        fundAdvanceId, Boolean.TRUE.equals(request.getApproved()), authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PutMapping("/fund-advances/{fundAdvanceId}/return")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isFundAdvancePrepSupervisor(#fundAdvanceId, authentication)")
    public ResponseEntity<Response> adminReturnFundAdvance(
            @PathVariable Long fundAdvanceId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        FundAdvanceDto dto = hasManagerScope(scope)
                ? financeService.adminReturnFundAdvance(fundAdvanceId, authentication.getName(), scope)
                : financeService.adminReturnFundAdvance(fundAdvanceId, authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/tasks/{taskId}/fund-advances")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isTaskPrepSupervisor(#taskId, authentication) or @preparationFinanceSecurity.isTaskLeader(#taskId, authentication)")
    public ResponseEntity<Response> listFundAdvances(
            @PathVariable Long taskId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        List<FundAdvanceDto> dtos = hasManagerScope(scope)
                ? financeService.listFundAdvancesByTask(taskId, scope)
                : financeService.listFundAdvancesByTask(taskId);
        return ResponseEntity.ok(Response.success("OK", dtos));
    }

    @GetMapping("/tasks/{taskId}/fund-advance-source-suggestions")
    @PreAuthorize("@preparationFinanceSecurity.isTaskPrepSupervisor(#taskId, authentication) or @preparationFinanceSecurity.isTaskLeader(#taskId, authentication)")
    public ResponseEntity<Response> suggestFundAdvanceSources(
            @PathVariable Long taskId,
            @RequestParam(required = false) String amount) {
        List<FundAdvanceSourceSuggestionDto> dtos = financeService.suggestFundAdvanceSources(taskId, amount);
        return ResponseEntity.ok(Response.success("OK", dtos));
    }

    @GetMapping("/tasks/{taskId}/allocation-sources")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isTaskPrepSupervisor(#taskId, authentication) or @preparationFinanceSecurity.isTaskMember(#taskId, authentication)")
    public ResponseEntity<Response> listTaskAllocationSources(
            @PathVariable Long taskId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        List<TaskAllocationSourceDto> dtos = hasManagerScope(scope)
                ? financeService.listTaskAllocationSources(taskId, scope)
                : financeService.listTaskAllocationSources(taskId);
        return ResponseEntity.ok(Response.success("OK", dtos));
    }

    @GetMapping("/tasks/{taskId}/expense-category-suggestions")
    @PreAuthorize("@preparationFinanceSecurity.isTaskPrepSupervisor(#taskId, authentication) or @preparationFinanceSecurity.isTaskMember(#taskId, authentication)")
    public ResponseEntity<Response> suggestExpenseCategories(
            @PathVariable Long taskId,
            @RequestParam(required = false) String amount,
            Authentication authentication) {
        List<ExpenseCategorySuggestionDto> dtos = financeService.suggestExpenseCategories(taskId, amount,
                authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dtos));
    }

    @GetMapping("/my/fund-advances")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Response> listMyFundAdvances(
            @RequestParam Long activityId,
            @RequestParam(required = false) Long taskId,
            Authentication authentication) {
        List<FundAdvanceDto> dtos = financeService.listMyFundAdvances(activityId, taskId, authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dtos));
    }

    @GetMapping("/activities/{activityId}/fund-advance-debts")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isActivityPrepSupervisor(#activityId, authentication)")
    public ResponseEntity<Response> listFundAdvanceDebts(
            @PathVariable Long activityId,
            @RequestParam(required = false) Long studentId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        List<FundAdvanceDebtDto> dtos = hasManagerScope(scope)
                ? financeService.listFundAdvanceDebts(activityId, studentId, scope)
                : financeService.listFundAdvanceDebts(activityId, studentId);
        return ResponseEntity.ok(Response.success("OK", dtos));
    }

    @PostMapping("/tasks/{taskId}/expenses/evidence")
    @PreAuthorize("@preparationFinanceSecurity.isTaskPrepSupervisor(#taskId, authentication) or @preparationFinanceSecurity.isTaskMember(#taskId, authentication)")
    public ResponseEntity<Response> uploadExpenseEvidence(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file) {
        String url = fileUploadService.uploadFile(file);
        return ResponseEntity.ok(Response.success("OK", new UploadResultDto(url)));
    }

    @PostMapping("/tasks/{taskId}/expenses")
    @PreAuthorize("@preparationFinanceSecurity.isTaskPrepSupervisor(#taskId, authentication) or @preparationFinanceSecurity.isTaskMember(#taskId, authentication)")
    public ResponseEntity<Response> createExpense(
            @PathVariable Long taskId,
            @RequestBody @Valid CreateExpenseRequest request,
            Authentication authentication) {
        ExpenseDto dto = financeService.createExpense(
                new CreateExpenseRequest(taskId, request.getCategoryId(), request.getAmount(), request.getDescription(),
                        request.getEvidenceUrl()),
                authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PutMapping("/expenses/{expenseId}/leader-decision")
    @PreAuthorize("@preparationFinanceSecurity.isExpensePrepSupervisor(#expenseId, authentication) or @preparationFinanceSecurity.canLeaderDecideExpense(#expenseId, authentication)")
    public ResponseEntity<Response> leaderDecision(
            @PathVariable Long expenseId,
            @RequestBody @Valid ApproveExpenseRequest request,
            Authentication authentication) {
        ExpenseDto dto = financeService.leaderDecision(expenseId, Boolean.TRUE.equals(request.getApproved()),
                authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PutMapping("/expenses/{expenseId}/admin-decision")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isExpensePrepSupervisor(#expenseId, authentication)")
    public ResponseEntity<Response> adminDecision(
            @PathVariable Long expenseId,
            @RequestBody @Valid ApproveExpenseRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        ExpenseDto dto = hasManagerScope(scope)
                ? financeService.adminDecision(expenseId, Boolean.TRUE.equals(request.getApproved()),
                        authentication.getName(), scope)
                : financeService.adminDecision(expenseId, Boolean.TRUE.equals(request.getApproved()),
                        authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/activities/{activityId}/expenses")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isActivityPrepSupervisor(#activityId, authentication) or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> listExpenses(
            @PathVariable Long activityId,
            @RequestParam(required = false) ExpenseStatus status,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        List<ExpenseDto> dtos = hasManagerScope(scope)
                ? financeService.listExpensesByActivity(activityId, status, scope)
                : financeService.listExpensesByActivity(activityId, status);
        return ResponseEntity.ok(Response.success("OK", dtos));
    }

    @GetMapping("/activities/{activityId}/financial-report")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isActivityPrepSupervisor(#activityId, authentication) or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> getFinancialReport(
            @PathVariable Long activityId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        FinancialReportDto dto = hasManagerScope(scope)
                ? financeService.getFinancialReport(activityId, scope)
                : financeService.getFinancialReport(activityId);
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/activities/{activityId}/reports/finance-overview")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isActivityPrepSupervisor(#activityId, authentication) or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> getFinanceOverviewReport(
            @PathVariable Long activityId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        FinanceOverviewReportDto dto = hasManagerScope(scope)
                ? financeService.getFinanceOverviewReport(activityId, scope)
                : financeService.getFinanceOverviewReport(activityId);
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/activities/{activityId}/reports/cash-flow")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationFinanceSecurity.isActivityPrepSupervisor(#activityId, authentication) or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> getCashFlowReport(
            @PathVariable Long activityId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        CashFlowReportDto dto = hasManagerScope(scope)
                ? financeService.getCashFlowReport(activityId, scope)
                : financeService.getCashFlowReport(activityId);
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    private DepartmentScope currentScope(HttpServletRequest request) {
        return DepartmentRequestScope.get(request).orElse(null);
    }

    private boolean hasManagerScope(DepartmentScope scope) {
        return scope != null && scope.manager() && !scope.departmentIds().isEmpty();
    }
}
