package vn.campuslife.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentRankingResponse {
    private Integer rank; // Thứ hạng (1, 2, 3, ...)
    private Long studentId;
    private String studentCode;
    private String studentName;
    private Long departmentId;
    private String departmentName;
    private Long classId;
    private String className;
    private Long semesterId;
    private String semesterName;
    private ScoreType scoreType; // null nếu là tổng điểm
    private BigDecimal score; // Điểm số
    private String scoreTypeLabel; // Label cho scoreType hoặc "Tổng điểm"
}

