package vn.campuslife.model;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SeriesConfig {
    private Integer requiredParticipationCount;
    private BigDecimal bonusPoints;
}
