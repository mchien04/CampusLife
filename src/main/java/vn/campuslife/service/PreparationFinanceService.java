package vn.campuslife.service;

import vn.campuslife.model.preparation.*;
import vn.campuslife.enumeration.ExpenseStatus;

public interface PreparationFinanceService {
    ActivityBudgetDto upsertActivityBudget(Long activityId, UpsertActivityBudgetRequest request);

    PreparationTaskDto allocateTaskAmount(Long taskId, AllocateTaskAmountRequest request);

    void addTaskMember(Long taskId, Long studentId);

    FundAdvanceDto createFundAdvance(Long taskId, CreateFundAdvanceRequest request);

    ExpenseDto createExpense(CreateExpenseRequest request, String username);

    ExpenseDto leaderDecision(Long expenseId, boolean approved, String username);

    ExpenseDto adminDecision(Long expenseId, boolean approved, String username);

    java.util.List<ExpenseDto> listExpensesByActivity(Long activityId, ExpenseStatus status);

    FinancialReportDto getFinancialReport(Long activityId);
}
