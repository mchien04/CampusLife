package vn.campuslife.model.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeriesStatisticsResponse {
    private Long totalSeries;
    private List<SeriesDetailItem> seriesDetails;
    private Map<Long, Long> studentsPerSeries; // seriesId -> student count
    private Map<Long, BigDecimal> milestonePointsAwarded; // seriesId -> total points awarded
    private List<PopularSeriesItem> popularSeries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeriesDetailItem {
        private Long seriesId;
        private String seriesName;
        private Long totalActivities;
        private Long registeredStudents;
        private Long completedStudents; // Hoàn thành tất cả activities
        private Double completionRate; // completedStudents / registeredStudents
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopularSeriesItem {
        private Long seriesId;
        private String seriesName;
        private Long studentCount;
        private Long totalActivities;
    }
}

