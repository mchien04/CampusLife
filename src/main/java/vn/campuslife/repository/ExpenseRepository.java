package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Expense;

import java.math.BigDecimal;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
            select coalesce(sum(e.amount), 0)
            from Expense e
            where e.budget.id = :budgetId
              and e.approved = true
            """)
    BigDecimal sumApprovedAmountByBudgetId(@Param("budgetId") Long budgetId);

    java.util.List<Expense> findByBudgetActivityIdOrderByCreatedAtDesc(Long activityId);

    java.util.List<Expense> findByBudgetActivityIdAndApprovedOrderByCreatedAtDesc(Long activityId, Boolean approved);

    java.util.List<Expense> findByBudgetActivityIdAndApprovedIsNullOrderByCreatedAtDesc(Long activityId);
}
