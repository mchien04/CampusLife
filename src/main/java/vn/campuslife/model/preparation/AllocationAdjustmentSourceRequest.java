package vn.campuslife.model.preparation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllocationAdjustmentSourceRequest {
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Amount is required")
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "Amount must be a non-negative number")
    private String amount;
}

