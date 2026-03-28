package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Expense;
import vn.campuslife.enumeration.ExpenseStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
            select coalesce(sum(e.amount), 0)
            from Expense e
            where e.category.id = :categoryId
              and e.status = 'APPROVED'
            """)
    BigDecimal sumApprovedAmountByCategoryId(@Param("categoryId") Long categoryId);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from Expense e
            where e.task.id = :taskId
              and e.status = 'APPROVED'
            """)
    BigDecimal sumApprovedAmountByTaskId(@Param("taskId") Long taskId);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from Expense e
            where e.task.id = :taskId
              and e.status in :statuses
            """)
    BigDecimal sumAmountByTaskIdAndStatusIn(@Param("taskId") Long taskId, @Param("statuses") Set<ExpenseStatus> statuses);

    List<Expense> findByTaskIdOrderByCreatedAtDesc(Long taskId);

    List<Expense> findByTaskActivityIdOrderByCreatedAtDesc(Long activityId);

    List<Expense> findByTaskActivityIdAndStatusOrderByCreatedAtDesc(Long activityId, ExpenseStatus status);

    default BigDecimal sumCommittedAmountByTaskId(Long taskId) {
        return sumAmountByTaskIdAndStatusIn(taskId, Set.of(ExpenseStatus.PENDING_LEADER, ExpenseStatus.PENDING_ADMIN, ExpenseStatus.APPROVED));
    }

    @Query("""
            select coalesce(sum(e.amount), 0)
            from Expense e
            where e.task.activity.id = :activityId
              and e.status = 'APPROVED'
            """)
    BigDecimal sumApprovedAmountByActivityId(@Param("activityId") Long activityId);

    @Query("""
            select e.task.id as taskId, coalesce(sum(e.amount), 0) as approvedSpent
            from Expense e
            where e.task.activity.id = :activityId
              and e.status = 'APPROVED'
            group by e.task.id
            """)
    java.util.List<TaskApprovedSpentView> sumApprovedSpentByTaskInActivity(@Param("activityId") Long activityId);

    @Query("""
            select e.task.id as taskId, coalesce(sum(e.amount), 0) as committedAmount
            from Expense e
            where e.task.activity.id = :activityId
              and e.status in ('PENDING_LEADER','PENDING_ADMIN','APPROVED')
            group by e.task.id
            """)
    java.util.List<TaskCommittedSpentView> sumCommittedSpentByTaskInActivity(@Param("activityId") Long activityId);

    @Query("""
            select e.status as status, count(e.id) as count, coalesce(sum(e.amount), 0) as totalAmount
            from Expense e
            where e.task.activity.id = :activityId
            group by e.status
            """)
    java.util.List<InvoiceStatusSummaryView> summarizeInvoiceStatusesByActivity(@Param("activityId") Long activityId);

    interface TaskApprovedSpentView {
        Long getTaskId();
        BigDecimal getApprovedSpent();
    }

    interface TaskCommittedSpentView {
        Long getTaskId();
        BigDecimal getCommittedAmount();
    }

    interface InvoiceStatusSummaryView {
        ExpenseStatus getStatus();
        Long getCount();
        BigDecimal getTotalAmount();
    }
}
