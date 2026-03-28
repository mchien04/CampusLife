package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "activity_budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "activity", "categories" })
@ToString(exclude = { "activity", "categories" })
@EntityListeners(AuditingEntityListener.class)
public class ActivityBudget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", unique = true, nullable = false)
    private Activity activity;

    @Column(name = "total_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "activityBudget", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BudgetCategory> categories = new LinkedHashSet<>();

    @CreatedDate
    private LocalDateTime createdAt;
}
