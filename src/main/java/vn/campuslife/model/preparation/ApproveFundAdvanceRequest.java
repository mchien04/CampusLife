package vn.campuslife.model.preparation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveFundAdvanceRequest {
    @NotNull(message = "Approved is required")
    private Boolean approved;
}

