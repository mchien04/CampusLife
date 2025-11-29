package vn.campuslife.model.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeBasedStatisticsResponse {
    private List<TimeSeriesDataPoint> registrationsOverTime;
    private List<TimeSeriesDataPoint> participationsOverTime;
    private List<TimeSeriesDataPoint> scoresOverTime;
    private Map<Integer, Long> peakHours; // hour (0-23) -> count
    private String groupBy; // "day", "week", "month", "quarter", "year"

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesDataPoint {
        private String period; // "2025-01", "2025-W01", etc.
        private Long count;
        private Double value; // For scores
    }
}

