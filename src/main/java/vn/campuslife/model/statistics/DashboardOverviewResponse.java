package vn.campuslife.model.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse {
    private Long totalActivities;
    private Long totalStudents;
    private Long totalSeries;
    private Long totalMiniGames;
    private Long monthlyRegistrations;
    private Long monthlyParticipations;
    private Double averageParticipationRate;
    private List<TopActivityItem> topActivities;
    private List<TopStudentItem> topStudents;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopActivityItem {
        private Long activityId;
        private String activityName;
        private Long registrationCount;
        private Long participationCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopStudentItem {
        private Long studentId;
        private String studentName;
        private String studentCode;
        private Long participationCount;
    }
}

