package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverBudgetInfoDto {
    private Long taskId;
    private BigDecimal requiredAdditionalAmount;
    private BigDecimal currentAllocatedAmount;
    private BigDecimal committedAmount;
    private List<AllocationSourceSuggestionDto> suggestedSources;
}

