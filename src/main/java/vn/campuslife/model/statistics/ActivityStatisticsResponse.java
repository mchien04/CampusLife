package vn.campuslife.model.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ActivityType;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityStatisticsResponse {
    private Long totalActivities;
    private Map<ActivityType, Long> countByType;
    private Map<String, Long> countByStatus; // draft, published, deleted
    private List<TopActivityItem> topActivitiesByRegistrations;
    private List<ActivityParticipationRate> participationRates;
    private Map<Long, Long> countByDepartment; // departmentId -> count
    private Long activitiesInSeries;
    private Long standaloneActivities;

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
    public static class ActivityParticipationRate {
        private Long activityId;
        private String activityName;
        private Long registrationCount;
        private Long participationCount;
        private Double participationRate; // participationCount / registrationCount
    }
}

