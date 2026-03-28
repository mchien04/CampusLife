package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceOverviewReportDto {
    private Long activityId;
    private BigDecimal totalBudget;
    private BigDecimal totalAllocatedToTasks;
    private BigDecimal totalApprovedSpent;
    private BigDecimal varianceAllocatedVsApproved;
    private List<BudgetCategoryDto> wallets;
    private List<TaskSpendStatusDto> tasks;
}

