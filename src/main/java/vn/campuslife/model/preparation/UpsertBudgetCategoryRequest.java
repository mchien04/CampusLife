package vn.campuslife.model.preparation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpsertBudgetCategoryRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Allocated amount is required")
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "Allocated amount must be a non-negative number")
    private String allocatedAmount;
}

