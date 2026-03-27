package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.BudgetCategory;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, Long> {
    List<BudgetCategory> findByActivityBudgetIdOrderByIdAsc(Long activityBudgetId);

    Optional<BudgetCategory> findByIdAndActivityBudgetActivityId(Long id, Long activityId);
}

