package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundAdvanceSourceSuggestionDto {
    private Long categoryId;
    private String categoryName;
    private BigDecimal allocationRemainingAmount;
    private BigDecimal cashAvailableAmount;
    private BigDecimal maxAdvanceAmount;
}

