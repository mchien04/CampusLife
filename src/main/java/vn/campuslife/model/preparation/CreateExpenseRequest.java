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
public class CreateExpenseRequest {
    private Long taskId;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Amount is required")
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "Amount must be a positive number")
    private String amount;

    private String description;

    private String evidenceUrl;
}
