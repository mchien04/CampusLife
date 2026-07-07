package vn.campuslife.service;

import vn.campuslife.model.preparation.*;
import vn.campuslife.enumeration.ExpenseStatus;
import vn.campuslife.security.department.DepartmentScope;

public interface PreparationFinanceService {
        ActivityBudgetDto upsertActivityBudget(Long activityId, UpsertActivityBudgetRequest request);

        ActivityBudgetDto upsertActivityBudget(Long activityId, UpsertActivityBudgetRequest request, DepartmentScope scope);

        ActivityBudgetDto getActivityBudget(Long activityId);

        ActivityBudgetDto getActivityBudget(Long activityId, DepartmentScope scope);

        PreparationTaskDto allocateTaskAmount(Long taskId, AllocateTaskAmountRequest request);

        PreparationTaskDto allocateTaskAmount(Long taskId, AllocateTaskAmountRequest request, DepartmentScope scope);

        void addTaskMember(Long taskId, Long studentId);

        void addTaskMember(Long taskId, Long studentId, DepartmentScope scope);

        FundAdvanceDto requestFundAdvance(Long taskId, CreateFundAdvanceRequest request, String username);

        FundAdvanceDto adminDecisionFundAdvance(Long fundAdvanceId, boolean approved, String username);

        FundAdvanceDto adminDecisionFundAdvance(Long fundAdvanceId, boolean approved, String username, DepartmentScope scope);

        FundAdvanceDto adminReturnFundAdvance(Long fundAdvanceId, String username);

        FundAdvanceDto adminReturnFundAdvance(Long fundAdvanceId, String username, DepartmentScope scope);

        java.util.List<FundAdvanceDto> listFundAdvancesByTask(Long taskId);

        java.util.List<FundAdvanceDto> listFundAdvancesByTask(Long taskId, DepartmentScope scope);

        java.util.List<FundAdvanceSourceSuggestionDto> suggestFundAdvanceSources(Long taskId, String amount);

        ExpenseDto createExpense(CreateExpenseRequest request, String username);

        ExpenseDto leaderDecision(Long expenseId, boolean approved, String username);

        ExpenseDto adminDecision(Long expenseId, boolean approved, String username);

        ExpenseDto adminDecision(Long expenseId, boolean approved, String username, DepartmentScope scope);

        java.util.List<ExpenseDto> listExpensesByActivity(Long activityId, ExpenseStatus status);

        java.util.List<ExpenseDto> listExpensesByActivity(Long activityId, ExpenseStatus status, DepartmentScope scope);

        AllocationAdjustmentRequestDto createAllocationAdjustmentRequest(Long taskId,
                        CreateAllocationAdjustmentRequest request, String username);

        java.util.List<AllocationAdjustmentRequestDto> listAllocationAdjustmentRequests(Long activityId,
                        vn.campuslife.enumeration.AllocationAdjustmentStatus status);

        java.util.List<AllocationAdjustmentRequestDto> listAllocationAdjustmentRequests(Long activityId,
                        vn.campuslife.enumeration.AllocationAdjustmentStatus status, DepartmentScope scope);

        AllocationAdjustmentRequestDto adminDecisionAllocationAdjustment(Long requestId, boolean approved,
                        Long categoryId,
                        String username);

        AllocationAdjustmentRequestDto adminDecisionAllocationAdjustment(Long requestId, boolean approved,
                        Long categoryId, String username, DepartmentScope scope);

        AllocationAdjustmentRequestDto adminDecisionAllocationAdjustmentMulti(Long requestId,
                        java.util.List<AllocationAdjustmentSourceRequest> sources, String username);

        AllocationAdjustmentRequestDto adminDecisionAllocationAdjustmentMulti(Long requestId,
                        java.util.List<AllocationAdjustmentSourceRequest> sources, String username, DepartmentScope scope);

        java.util.List<AllocationSourceSuggestionDto> suggestAllocationAdjustmentSources(Long requestId);

        java.util.List<AllocationSourceSuggestionDto> suggestAllocationAdjustmentSources(Long requestId, DepartmentScope scope);

        java.util.List<AllocationAdjustmentSourcePlanDto> planAllocationAdjustmentSources(Long requestId);

        java.util.List<AllocationAdjustmentSourcePlanDto> planAllocationAdjustmentSources(Long requestId, DepartmentScope scope);

        java.util.List<FundAdvanceDebtDto> listFundAdvanceDebts(Long activityId, Long studentId);

        java.util.List<FundAdvanceDebtDto> listFundAdvanceDebts(Long activityId, Long studentId, DepartmentScope scope);

    java.util.List<TaskAllocationSourceDto> listTaskAllocationSources(Long taskId);

    java.util.List<TaskAllocationSourceDto> listTaskAllocationSources(Long taskId, DepartmentScope scope);

    java.util.List<ExpenseCategorySuggestionDto> suggestExpenseCategories(Long taskId, String amount, String username);

    java.util.List<FundAdvanceDto> listMyFundAdvances(Long activityId, Long taskId, String username);

        FinanceOverviewReportDto getFinanceOverviewReport(Long activityId);

        FinanceOverviewReportDto getFinanceOverviewReport(Long activityId, DepartmentScope scope);

        CashFlowReportDto getCashFlowReport(Long activityId);

        CashFlowReportDto getCashFlowReport(Long activityId, DepartmentScope scope);

        FinancialReportDto getFinancialReport(Long activityId);

        FinancialReportDto getFinancialReport(Long activityId, DepartmentScope scope);
}
