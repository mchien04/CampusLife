package vn.campuslife.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import vn.campuslife.enumeration.ParticipationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityParticipationResponse {

    private Long id;
    private Long activityId;
    private String activityName;
    private Long studentId;
    private String studentName;
    private String studentCode;
    private ParticipationType participationType;
    private BigDecimal pointsEarned;
    private LocalDateTime date;
    private Boolean isCompleted; // null = chưa chấm, true = đạt, false = không đạt
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
}
