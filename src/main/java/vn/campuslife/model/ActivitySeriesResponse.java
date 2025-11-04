package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import vn.campuslife.entity.Activity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivitySeriesResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer requiredParticipationCount;
    private BigDecimal bonusPoints;
    private String createdBy;
    private Set<Activity> activities;
}
