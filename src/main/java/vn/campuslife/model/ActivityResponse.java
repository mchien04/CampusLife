package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.entity.Department;
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
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private boolean requiresSubmission;
    private BigDecimal maxPoints;

    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationDeadline;

    private String shareLink;
    private boolean isImportant;
    private boolean mandatoryForFacultyStudents;
    private String bannerUrl;
    private String location;

    private Integer ticketQuantity;
    private String benefits;
    private String requirements;
    private String contactInfo;

    private BigDecimal penaltyPointsIncomplete;

    private List<Department> organizers;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String lastModifiedBy;

    private long participantCount;
    private long remainingDays;
    private Long seriesId;
    private String seriesName;


}
