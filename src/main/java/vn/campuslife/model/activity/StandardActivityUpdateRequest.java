package vn.campuslife.model.activity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ActivityPresetCode;
import vn.campuslife.model.activity.ActivityPresetConfig;

import java.time.LocalDateTime;
import vn.campuslife.model.score.ActivityScoreRuleRequest;
import vn.campuslife.model.score.ActivityScoreRuleResponse;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StandardActivityUpdateRequest {
    private String name;
    // type cannot be changed
    private String description;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String location;
    private List<Long> organizerIds;
    
    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationDeadline;
    
    private Boolean requiresSubmission;
    private Boolean requiresApproval;
    private Integer ticketQuantity;
    private Boolean isImportant;
    private Boolean mandatoryForFacultyStudents;
    private Boolean isDraft;
    
    private String bannerUrl;
    private String shareLink;
    private String benefits;
    private String requirements;
    private String contactInfo;
    
    private List<ActivityScoreRuleRequest> scoreRules;
    
    private ActivityPresetCode presetCode;
    private ActivityPresetConfig presetConfig;
}
