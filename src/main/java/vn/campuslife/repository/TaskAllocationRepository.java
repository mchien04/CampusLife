package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.TaskAllocation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskAllocationRepository extends JpaRepository<TaskAllocation, Long> {
    Optional<TaskAllocation> findByTaskIdAndCategoryId(Long taskId, Long categoryId);

    List<TaskAllocation> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    @Query("""
            select coalesce(sum(a.amount), 0)
            from TaskAllocation a
            where a.task.id = :taskId
            """)
    BigDecimal sumAmountByTaskId(@Param("taskId") Long taskId);

    @Query("""
            select coalesce(sum(a.amount), 0)
            from TaskAllocation a
            where a.category.id = :categoryId
            """)
    BigDecimal sumAmountByCategoryId(@Param("categoryId") Long categoryId);

    @Query("""
            select a.category.id as categoryId, coalesce(sum(a.amount), 0) as allocatedToTasksAmount
            from TaskAllocation a
            where a.task.activity.id = :activityId
            group by a.category.id
            """)
    List<CategoryAllocationSumView> sumAllocatedToTasksByActivity(@Param("activityId") Long activityId);

    interface CategoryAllocationSumView {
        Long getCategoryId();
        BigDecimal getAllocatedToTasksAmount();
    }
}

