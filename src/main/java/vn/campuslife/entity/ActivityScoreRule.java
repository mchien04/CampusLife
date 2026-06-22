package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.enumeration.ScoreRuleTrigger;
import vn.campuslife.enumeration.ScoreRuleCalculation;
import vn.campuslife.enumeration.ScoreRuleAudience;
import vn.campuslife.enumeration.ScoreSemesterPolicy;

@Entity
@Table(name = "activity_score_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ActivityScoreRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_type", nullable = false)
    private ScoreType scoreType;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private ScoreRuleTrigger triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation", nullable = false)
    private ScoreRuleCalculation calculation;

    @Column(nullable = false)
    private BigDecimal points = BigDecimal.ZERO;

    @Column(name = "fail_points", nullable = false)
    private BigDecimal failPoints = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScoreRuleAudience audience;

    @Enumerated(EnumType.STRING)
    @Column(name = "semester_policy", nullable = false)
    private ScoreSemesterPolicy semesterPolicy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "explicit_semester_id")
    private Semester explicitSemester;

    @Column(nullable = false)
    private boolean enabled = true;

    @ManyToMany
    @JoinTable(name = "activity_score_rule_departments", 
        joinColumns = @JoinColumn(name = "rule_id"), 
        inverseJoinColumns = @JoinColumn(name = "department_id"))
    private Set<Department> targetDepartments = new LinkedHashSet<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
