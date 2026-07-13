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
public class CreateSeriesRequest {
    private String name;
    private String description;
    private Map<Integer, Integer> milestonePoints = new LinkedHashMap<>();
    
    private ScoreType scoreType;
    private Long targetSemesterId;
    private Long mainActivityId;
    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationDeadline;
    private Boolean requiresApproval;
    private Integer ticketQuantity;
    private Boolean minimumRequirementEnabled;
    private Integer minimumRequiredEvents;
    private Integer minimumPenaltyPoints;
    private ScoreRuleAudience audience;
    private List<Long> departmentIds;
    /** Khoa tổ chức của chuỗi — sự kiện con kế thừa, không chỉnh riêng. */
    private List<Long> organizerIds;
    private Boolean isImportant;
    private Boolean mandatoryForFacultyStudents;
    private Boolean isDraft;
    private SeriesPresetCode presetCode;
    private SeriesPresetConfig presetConfig;
}


