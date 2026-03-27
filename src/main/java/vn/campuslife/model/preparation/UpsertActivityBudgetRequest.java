package vn.campuslife.model.preparation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpsertActivityBudgetRequest {
    @NotNull(message = "Total amount is required")
    @PositiveOrZero(message = "Total amount must be >= 0")
    private BigDecimal totalAmount;

    @Valid
    private List<UpsertBudgetCategoryRequest> categories;
}

