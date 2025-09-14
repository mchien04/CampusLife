package vn.campuslife.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRegistrationRequest {

    @NotNull(message = "Activity ID is required")
    private Long activityId;

    private String feedback;
}
