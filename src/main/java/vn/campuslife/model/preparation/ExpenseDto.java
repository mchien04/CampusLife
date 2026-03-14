package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDto {
    private Long id;
    private Long activityId;
    private Long budgetId;
    private BigDecimal amount;
    private String description;
    private String evidenceUrl;
    private Long reportedById;
    private String reportedByName;
    private Boolean approved;
    private LocalDateTime createdAt;
}
