package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import vn.campuslife.enumeration.PreparationTaskStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "preparation_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PreparationTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", nullable = false)
    private Student owner;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime deadline;

    @Column(precision = 19, scale = 2)
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean isFinancial = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreparationTaskStatus status = PreparationTaskStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String completionProofUrls;

    @CreatedDate
    private LocalDateTime createdAt;
}
