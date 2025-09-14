package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityResponse {
    private Long id;
    private String name;
    private ActivityType type;
    private ScoreType scoreType;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;

    private boolean requiresSubmission;
    private BigDecimal maxPoints;

    private LocalDate registrationStartDate;
    private LocalDate registrationDeadline;

    private String shareLink;
    private boolean isImportant;
    private String bannerUrl;
    private String location;

    private Integer ticketQuantity;
    private String benefits;
    private String requirements;
    private String contactInfo;
    private boolean mandatoryForFacultyStudents;
    private BigDecimal penaltyPointsIncomplete;

    private List<Long> organizerIds;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String lastModifiedBy;
}
