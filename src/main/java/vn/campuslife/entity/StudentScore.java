package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import vn.campuslife.enumeration.ScoreType;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "student_scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class StudentScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScoreType scoreType;

    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String activityIds; // JSON array: "[1,5,10]" - list of activity IDs that contributed to this score

    // Criterion removed; scoring aggregates only from activities

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}