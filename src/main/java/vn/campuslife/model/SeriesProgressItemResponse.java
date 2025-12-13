package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeriesProgressItemResponse {
    private Long studentId;
    private String studentCode;
    private String studentName;
    private String className; // optional, từ student.studentClass
    private String departmentName; // optional, từ student.department
    private Integer completedCount;
    private Integer totalActivities;
    private BigDecimal pointsEarned;
    private String currentMilestone; // optional, key của milestone hiện tại
    private List<Long> completedActivityIds;
    private LocalDateTime lastUpdated;
    private Boolean isRegistered; // có đăng ký series chưa
}

