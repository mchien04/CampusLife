package vn.campuslife.model.preparation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDecisionAllocationAdjustmentRequest {
    @NotNull(message = "Approved is required")
    private Boolean approved;

    private Long categoryId;

    @Valid
    private List<AllocationAdjustmentSourceRequest> sources;
}
