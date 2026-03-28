package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.FundAdvance;
import vn.campuslife.enumeration.FundAdvanceStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Repository
public interface FundAdvanceRepository extends JpaRepository<FundAdvance, Long> {
    List<FundAdvance> findByTaskIdAndStudentIdAndStatusOrderByCreatedAtAsc(Long taskId, Long studentId, FundAdvanceStatus status);

    List<FundAdvance> findByTaskIdOrderByCreatedAtDesc(Long taskId);

    boolean existsByTaskActivityIdAndStudentIdAndStatusInAndRemainingAmountGreaterThan(Long activityId, Long studentId,
            Set<FundAdvanceStatus> statuses, BigDecimal amount);

    java.util.List<FundAdvance> findByTaskActivityIdAndStatusOrderByCreatedAtDesc(Long activityId, FundAdvanceStatus status);

    @Query("""
            select fa.student.id as studentId, coalesce(sum(fa.remainingAmount), 0) as holdingAmount
            from FundAdvance fa
            where fa.task.activity.id = :activityId
              and fa.status = 'HOLDING'
            group by fa.student.id
            """)
    List<FundAdvanceHoldingView> sumHoldingByActivity(@Param("activityId") Long activityId);

    interface FundAdvanceHoldingView {
        Long getStudentId();
        BigDecimal getHoldingAmount();
    }
}
