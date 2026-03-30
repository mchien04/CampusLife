package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskAllocationSourceDto {
    private Long categoryId;
    private String categoryName;
    private BigDecimal allocatedAmount;
    private BigDecimal holdingAdvanceAmount;
    private BigDecimal approvedSpentAmount;
    private BigDecimal allocationRemainingAmount;
}

