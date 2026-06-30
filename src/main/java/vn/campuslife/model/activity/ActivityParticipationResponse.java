package vn.campuslife.model.activity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.model.score.AppliedScoreAward;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private List<AppliedScoreAward> scoreAwards;

    // Custom constructor for backward compatibility
    public ActivityParticipationResponse(Long id, Long activityId, String activityName, Long studentId,
                                        String studentName, String studentCode, ParticipationType participationType,
                                        BigDecimal pointsEarned, LocalDateTime date, Boolean isCompleted,
                                        LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        this.id = id;
        this.activityId = activityId;
        this.activityName = activityName;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentCode = studentCode;
        this.participationType = participationType;
        this.pointsEarned = pointsEarned;
        this.date = date;
        this.isCompleted = isCompleted;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
    }
}

