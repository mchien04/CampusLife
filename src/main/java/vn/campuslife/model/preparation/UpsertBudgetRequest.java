package vn.campuslife.model.preparation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpsertBudgetRequest {
    private Long activityId;

    @NotNull(message = "Total amount is required")
    @PositiveOrZero(message = "Total amount must be >= 0")
    private BigDecimal totalAmount;

    private String description;
}
