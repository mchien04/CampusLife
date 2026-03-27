package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget_categories", uniqueConstraints = @UniqueConstraint(columnNames = { "activity_budget_id", "name" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class BudgetCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_budget_id", nullable = false)
    private ActivityBudget activityBudget;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "allocated_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    @Column(name = "used_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal usedAmount = BigDecimal.ZERO;

    @CreatedDate
    private LocalDateTime createdAt;
}

