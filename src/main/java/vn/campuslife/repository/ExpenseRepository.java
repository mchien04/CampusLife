package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Expense;
import vn.campuslife.enumeration.ExpenseStatus;

import java.math.BigDecimal;
import java.util.List;

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

    List<Expense> findByTaskIdOrderByCreatedAtDesc(Long taskId);

    List<Expense> findByTaskActivityIdOrderByCreatedAtDesc(Long activityId);

    List<Expense> findByTaskActivityIdAndStatusOrderByCreatedAtDesc(Long activityId, ExpenseStatus status);
}
