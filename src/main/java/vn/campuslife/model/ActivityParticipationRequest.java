package vn.campuslife.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import vn.campuslife.enumeration.ParticipationType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityParticipationRequest {

    private String ticketCode;

    private Long studentId;

    // ParticipationType is optional - check-in logic automatically determines the type
    // based on current participation status (REGISTERED -> CHECKED_IN -> ATTENDED)
    private ParticipationType participationType;

    private BigDecimal pointsEarned;
}
