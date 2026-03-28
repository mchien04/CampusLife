package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreparationDashboardDto {
    private Long activityId;
    private boolean hasPreparation;
    private List<PreparationTaskDto> tasks;
    private ActivityBudgetDto activityBudget;
    private String financeMessage;
}
