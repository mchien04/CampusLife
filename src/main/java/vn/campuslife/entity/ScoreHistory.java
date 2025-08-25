package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "score_histories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ScoreHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "score_id", nullable = false)
    private StudentScore score;

    private BigDecimal oldScore;

    private BigDecimal newScore;

    @ManyToOne
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedBy;

    private LocalDateTime changeDate;
}