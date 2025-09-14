package vn.campuslife.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import vn.campuslife.enumeration.ParticipationType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityParticipationRequest {

    @NotNull(message = "Activity ID is required")
    private Long activityId;

    @NotNull(message = "Participation type is required")
    private ParticipationType participationType;

    private BigDecimal pointsEarned;

    private String notes;
}
