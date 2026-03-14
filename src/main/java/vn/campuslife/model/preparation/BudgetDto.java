package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetDto {
    private Long id;
    private Long activityId;
    private BigDecimal totalAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private String description;
}

