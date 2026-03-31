package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCategorySuggestionDto {
    private Long categoryId;
    private String categoryName;
    private BigDecimal allocationRemainingAmount;
    private BigDecimal walletRemainingAmount;
    private BigDecimal myFundAdvanceRemainingAmount;
    private BigDecimal maxExpenseAmount;
}
