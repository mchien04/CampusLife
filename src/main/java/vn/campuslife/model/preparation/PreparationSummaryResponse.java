package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreparationSummaryResponse {
    private Long activityId;
    private boolean enabled;
    private long pendingTasks;
    private long waitingExpenses;
    private String remainingAmount;
}
