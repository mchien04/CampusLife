package vn.campuslife.model.activity.series;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.model.score.ActivityScoreRuleResponse;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeriesChildActivityResponse {
    // Backward-compatible fields from ActivityResponse
    private Long id;
    private String name;
    private ActivityType type;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private boolean hasPreparation;

    private boolean requiresSubmission;
    private List<ActivityScoreRuleResponse> scoreRules;
    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationDeadline;

    private String shareLink;
    private boolean isImportant;
    private boolean isDraft;
    private String bannerUrl;
    private String location;

    private Integer ticketQuantity;
    private String benefits;
    private String requirements;
    private String contactInfo;
    private String checkInCode;
    private boolean requiresApproval;
    private boolean mandatoryForFacultyStudents;

    private List<Long> organizerIds;

    private Long seriesId;
    private Integer seriesOrder;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String lastModifiedBy;

    // Added context
    private String seriesName;
}
