package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateActivityRequest {
    private String name;
    private ActivityType type;
    private ScoreType scoreType;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean requiresSubmission;
    private BigDecimal maxPoints;
    private BigDecimal penaltyPointsIncomplete;
    private LocalDate registrationStartDate;
    private LocalDate registrationDeadline;
    private String shareLink;
    private Boolean isImportant;
    private String bannerUrl;
    private String location;
    private Integer ticketQuantity;
    private String benefits;
    private String requirements;
    private String contactInfo;
    private Boolean mandatoryForFacultyStudents;
    private List<Long> organizerIds;

}
