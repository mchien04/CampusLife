package vn.campuslife.model.preparation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpsertBudgetCategoryRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Allocated amount is required")
    @PositiveOrZero(message = "Allocated amount must be >= 0")
    private BigDecimal allocatedAmount;
}

