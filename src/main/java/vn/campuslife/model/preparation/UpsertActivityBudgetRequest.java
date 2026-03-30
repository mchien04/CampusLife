package vn.campuslife.model.preparation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpsertActivityBudgetRequest {
    @NotBlank(message = "Total amount is required")
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "Total amount must be a non-negative number")
    private String totalAmount;

    @Valid
    private List<UpsertBudgetCategoryRequest> categories;
}

