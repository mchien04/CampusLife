package vn.campuslife.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateActivitySeriesRequest {
    private String name;
    private String description;
    private Integer requiredParticipationCount;
    private BigDecimal bonusPoints;
    private LocalDate startDate;
    private LocalDate endDate;

}
