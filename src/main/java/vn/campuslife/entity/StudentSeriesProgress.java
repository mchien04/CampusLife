package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_series_progress", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "series_id"}),
       indexes = {
           @Index(name = "idx_student_series", columnList = "student_id, series_id"),
           @Index(name = "idx_completed_count", columnList = "completed_count")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentSeriesProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Khóa chính")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    @Comment("Sinh viên")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "series_id", nullable = false)
    @Comment("Chuỗi sự kiện")
    private ActivitySeries series;

    @Column(columnDefinition = "TEXT")
    @Comment("JSON array: [1,3,5] - Danh sách activityId đã tham gia")
    private String completedActivityIds;

    @Column(nullable = false)
    @Comment("Số sự kiện đã tham gia")
    private Integer completedCount = 0;

    @Column(nullable = false, precision = 10, scale = 2)
    @Comment("Điểm đã nhận từ milestone")
    private BigDecimal pointsEarned = BigDecimal.ZERO;

    @Column(nullable = false)
    @Comment("Ngày cập nhật")
    private LocalDateTime lastUpdated = LocalDateTime.now();
}

