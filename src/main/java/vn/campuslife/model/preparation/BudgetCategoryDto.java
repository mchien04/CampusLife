package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetCategoryDto {
    private Long id;
    private String name;
    private BigDecimal allocatedAmount;
    private BigDecimal allocatedToTasksAmount;
    private BigDecimal availableToAllocateAmount;
    private BigDecimal cashOutsideAmount;
    private BigDecimal cashAvailableAmount;
    private BigDecimal usedAmount;
    private BigDecimal remainingAmount;
    private Double usedPercent;
}
