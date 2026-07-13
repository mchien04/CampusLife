package vn.campuslife.model.activity.series;

import lombok.Data;
import vn.campuslife.enumeration.ScoreRuleAudience;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.enumeration.SeriesPresetCode;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class SeriesResponse {
    private Long id;
    private String name;
    private String description;
    private Map<Integer, Integer> milestonePoints = new LinkedHashMap<>();
    private ScoreType scoreType;
    private Long mainActivityId;
    private Long targetSemesterId;
    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationDeadline;
    private boolean requiresApproval;
    private Integer ticketQuantity;
    private boolean minimumRequirementEnabled;
    private Integer minimumRequiredEvents;
    private Integer minimumPenaltyPoints;
    private ScoreRuleAudience audience;
    private List<Long> targetDepartmentIds;
    private List<Long> organizerIds;
    private boolean isImportant;
    private boolean mandatoryForFacultyStudents;
    private boolean isDraft;
    private SeriesPresetCode presetCode;
    private SeriesPresetConfig presetConfig;
    private LocalDateTime createdAt;
    /** Thời gian kết thúc muộn nhất trong các sự kiện con (null nếu chưa có child / chưa có endDate). */
    private LocalDateTime latestEndDate;
    /** true khi latestEndDate đã qua — chuỗi coi như đã kết thúc (mốc trừ điểm tối thiểu). */
    private boolean ended;
}
