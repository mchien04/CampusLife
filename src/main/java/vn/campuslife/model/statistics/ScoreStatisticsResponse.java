package vn.campuslife.model.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreStatisticsResponse {
    private Map<ScoreType, ScoreTypeStatistics> statisticsByType;
    private List<TopStudentScoreItem> topStudents;
    private Map<Long, BigDecimal> averageByDepartment; // departmentId -> average score
    private Map<Long, BigDecimal> averageByClass; // classId -> average score
    private Map<Long, BigDecimal> averageBySemester; // semesterId -> average score
    private Map<String, Long> scoreDistribution; // "0-10", "10-20", etc. -> count

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreTypeStatistics {
        private ScoreType scoreType;
        private BigDecimal averageScore;
        private BigDecimal maxScore;
        private BigDecimal minScore;
        private Long totalStudents;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopStudentScoreItem {
        private Long studentId;
        private String studentName;
        private String studentCode;
        private ScoreType scoreType;
        private BigDecimal score;
        private Long semesterId;
        private String semesterName;
    }
}

