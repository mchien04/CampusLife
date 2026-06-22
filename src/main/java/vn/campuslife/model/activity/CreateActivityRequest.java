package vn.campuslife.model.activity;

import lombok.Data;
import vn.campuslife.enumeration.ActivityPresetCode;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.model.score.ActivityScoreRuleRequest;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateActivityRequest {
    private String name;
    private ActivityType type;
    private ActivityPresetCode presetCode;
    private ActivityPresetConfig presetConfig;

    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean requiresSubmission;
    private List<ActivityScoreRuleRequest> scoreRules;
    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationDeadline;
    private String shareLink;
    private Boolean isImportant;
    private Boolean isDraft;
    private String bannerUrl;
    private String location;
    private Integer ticketQuantity;
    private String benefits;
    private String requirements;
    private String contactInfo;
    private Boolean requiresApproval;
    private Boolean mandatoryForFacultyStudents;
    private List<Long> organizerIds;

}

