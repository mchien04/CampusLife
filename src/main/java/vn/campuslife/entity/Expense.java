package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import vn.campuslife.enumeration.ExpenseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "preparation_expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private PreparationTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private BudgetCategory category;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String evidenceUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private Student createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseStatus status = ExpenseStatus.PENDING_LEADER;

    @CreatedDate
    private LocalDateTime createdAt;
}
