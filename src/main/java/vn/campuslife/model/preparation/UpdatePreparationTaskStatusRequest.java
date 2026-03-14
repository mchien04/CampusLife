package vn.campuslife.model.preparation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.PreparationTaskStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePreparationTaskStatusRequest {
    @NotNull(message = "Status is required")
    private PreparationTaskStatus status;
}

