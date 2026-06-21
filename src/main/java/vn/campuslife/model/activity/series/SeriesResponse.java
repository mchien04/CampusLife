package vn.campuslife.model.activity.series;

import lombok.Data;
import vn.campuslife.enumeration.ScoreType;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class SeriesResponse {
    private Long id;
    private String name;
    private String description;
    private Map<Integer, Integer> milestonePoints = new LinkedHashMap<>();
    private ScoreType scoreType;
    private Long mainActivityId;
    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationDeadline;
    private boolean requiresApproval;
    private Integer ticketQuantity;
    private boolean minimumRequirementEnabled;
    private Integer minimumRequiredEvents;
    private Integer minimumPenaltyPoints;
    private LocalDateTime createdAt;
}
