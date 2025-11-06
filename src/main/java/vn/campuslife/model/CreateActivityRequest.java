package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateActivityRequest {
    private String name;
    private ActivityType type;
    private ScoreType scoreType;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean requiresSubmission;
    private BigDecimal maxPoints;
    private BigDecimal penaltyPointsIncomplete;
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
