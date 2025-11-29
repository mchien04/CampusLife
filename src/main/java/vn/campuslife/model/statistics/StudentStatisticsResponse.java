package vn.campuslife.model.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentStatisticsResponse {
    private Long totalStudents;
    private Map<Long, Long> countByDepartment; // departmentId -> count
    private Map<Long, Long> countByClass; // classId -> count
    private List<TopParticipantItem> topParticipants;
    private List<InactiveStudentItem> inactiveStudents; // Chưa tham gia activity nào
    private List<LowParticipationRateItem> lowParticipationRateStudents; // Đăng ký nhiều nhưng tham gia ít

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopParticipantItem {
        private Long studentId;
        private String studentName;
        private String studentCode;
        private Long participationCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InactiveStudentItem {
        private Long studentId;
        private String studentName;
        private String studentCode;
        private String departmentName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowParticipationRateItem {
        private Long studentId;
        private String studentName;
        private String studentCode;
        private Long registrationCount;
        private Long participationCount;
        private Double participationRate;
    }
}

