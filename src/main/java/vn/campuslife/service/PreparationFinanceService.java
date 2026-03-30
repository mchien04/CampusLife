package vn.campuslife.service;

import vn.campuslife.model.preparation.*;
import vn.campuslife.enumeration.ExpenseStatus;

public interface PreparationFinanceService {
        ActivityBudgetDto upsertActivityBudget(Long activityId, UpsertActivityBudgetRequest request);

        ActivityBudgetDto getActivityBudget(Long activityId);

        PreparationTaskDto allocateTaskAmount(Long taskId, AllocateTaskAmountRequest request);

        void addTaskMember(Long taskId, Long studentId);

        FundAdvanceDto requestFundAdvance(Long taskId, CreateFundAdvanceRequest request, String username);

        FundAdvanceDto adminDecisionFundAdvance(Long fundAdvanceId, boolean approved, String username);

        FundAdvanceDto adminReturnFundAdvance(Long fundAdvanceId, String username);

        java.util.List<FundAdvanceDto> listFundAdvancesByTask(Long taskId);

        java.util.List<FundAdvanceSourceSuggestionDto> suggestFundAdvanceSources(Long taskId, String amount);

        ExpenseDto createExpense(CreateExpenseRequest request, String username);

        ExpenseDto leaderDecision(Long expenseId, boolean approved, String username);

        ExpenseDto adminDecision(Long expenseId, boolean approved, String username);

        java.util.List<ExpenseDto> listExpensesByActivity(Long activityId, ExpenseStatus status);

        AllocationAdjustmentRequestDto createAllocationAdjustmentRequest(Long taskId,
                        CreateAllocationAdjustmentRequest request, String username);

        java.util.List<AllocationAdjustmentRequestDto> listAllocationAdjustmentRequests(Long activityId,
                        vn.campuslife.enumeration.AllocationAdjustmentStatus status);

        AllocationAdjustmentRequestDto adminDecisionAllocationAdjustment(Long requestId, boolean approved,
                        Long categoryId,
                        String username);

        AllocationAdjustmentRequestDto adminDecisionAllocationAdjustmentMulti(Long requestId,
                        java.util.List<AllocationAdjustmentSourceRequest> sources, String username);

        java.util.List<AllocationSourceSuggestionDto> suggestAllocationAdjustmentSources(Long requestId);

        java.util.List<AllocationAdjustmentSourcePlanDto> planAllocationAdjustmentSources(Long requestId);

        java.util.List<FundAdvanceDebtDto> listFundAdvanceDebts(Long activityId, Long studentId);

    java.util.List<TaskAllocationSourceDto> listTaskAllocationSources(Long taskId);

        FinanceOverviewReportDto getFinanceOverviewReport(Long activityId);

        CashFlowReportDto getCashFlowReport(Long activityId);

        FinancialReportDto getFinancialReport(Long activityId);
}
