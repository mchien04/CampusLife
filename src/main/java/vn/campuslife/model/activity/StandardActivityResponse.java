package vn.campuslife.model.activity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ActivityType;

import java.time.LocalDateTime;
import vn.campuslife.model.score.ActivityScoreRuleRequest;
import vn.campuslife.model.score.ActivityScoreRuleResponse;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StandardActivityResponse {
    private Long id;
    private String name;
    private ActivityType type;
    private String description;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String location;
    private List<Long> organizerIds;
    
    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationDeadline;
    
    private Boolean hasPreparation;
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
    private String checkInCode;
    
    private List<ActivityScoreRuleResponse> scoreRules;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String lastModifiedBy;
}
