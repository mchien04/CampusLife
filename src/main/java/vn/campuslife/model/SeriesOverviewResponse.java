package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeriesOverviewResponse {
    private Long seriesId;
    private String seriesName;
    private String description;
    private ScoreType scoreType;
    private String milestonePoints; // JSON string
    private Map<String, Integer> milestonePointsMap; // Parsed milestone points
    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationDeadline;
    private Boolean requiresApproval;
    private Integer ticketQuantity;
    private LocalDateTime createdAt;

    // Statistics
    private Integer totalActivities;
    private Long totalRegisteredStudents;
    private Long totalCompletedStudents; // Hoàn thành tất cả activities
    private Double completionRate; // completedStudents / registeredStudents
    private BigDecimal totalMilestonePointsAwarded; // Tổng điểm milestone đã trao

    // Progress distribution by milestone
    private List<MilestoneProgressItem> milestoneProgress; // Số SV ở mỗi milestone

    // Activity statistics
    private List<ActivityStatItem> activityStats; // Thống kê từng activity trong series

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MilestoneProgressItem {
        private String milestoneKey; // "3", "4", "5"
        private Integer milestoneCount; // Số activity cần để đạt milestone này
        private Integer milestonePoints; // Điểm thưởng
        private Long studentCount; // Số SV đã đạt milestone này
        private Double percentage; // % so với tổng số SV đã đăng ký
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityStatItem {
        private Long activityId;
        private String activityName;
        private Integer order;
        private Long registrationCount;
        private Long participationCount;
        private Double participationRate; // participationCount / registrationCount
    }
}

