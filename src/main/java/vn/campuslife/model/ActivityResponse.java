package vn.campuslife.model;

import vn.campuslife.enumeration.ActivityType;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ActivityResponse {
    private Long id;
    private String name;
    private ActivityType type;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long departmentId;
    private boolean requiresSubmission;
    private Double maxPoints;
    private LocalDate registrationDeadline;
    private String shareLink;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}