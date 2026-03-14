package vn.campuslife.model.preparation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpenseRequest {
    private Long activityId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be > 0")
    private BigDecimal amount;

    private String description;

    private String evidenceUrl;
}
