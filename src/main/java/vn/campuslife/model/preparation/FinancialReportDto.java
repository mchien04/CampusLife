package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialReportDto {
    private Long activityId;
    private BigDecimal totalBudget;
    private List<BudgetCategoryDto> categories;
    private List<TaskOverBudgetDto> overBudgetTasks;
}

