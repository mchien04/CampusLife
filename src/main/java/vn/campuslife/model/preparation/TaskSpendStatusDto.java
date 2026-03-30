package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskSpendStatusDto {
    private Long taskId;
    private String title;
    private BigDecimal allocatedAmount;
    private BigDecimal committedAmount;
    private BigDecimal approvedSpent;
    private Double usedPercent;
}

