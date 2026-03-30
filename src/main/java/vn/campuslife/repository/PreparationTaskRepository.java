package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.PreparationTask;
import vn.campuslife.model.TaskStatsRespone;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PreparationTaskRepository extends JpaRepository<PreparationTask, Long> {
    List<PreparationTask> findByActivityIdOrderByDeadlineAscIdAsc(Long activityId);

    Optional<PreparationTask> findByIdAndOwnerId(Long id, Long ownerId);

    @Query("SELECT new vn.campuslife.model.TaskStatsRespone(" +
            "COUNT(t), " +
            "SUM(CASE WHEN t.status = 'COMPLETED' THEN 1L ELSE 0L END), " +
            "SUM(CASE WHEN t.status <> 'COMPLETED' THEN 1L ELSE 0L END)) " +
            "FROM PreparationTask t WHERE t.owner.id = :studentId")
    TaskStatsRespone getStatsByStudentId(@Param("studentId") Long studentId);
    List<PreparationTask> findByActivityIdAndAssigneeIdOrderByDeadlineAscIdAsc(Long activityId, Long assigneeId);
    Optional<PreparationTask> findById(Long id);

    @Query("""
            select coalesce(sum(t.allocatedAmount), 0)
            from PreparationTask t
            where t.activity.id = :activityId
            """)
    BigDecimal sumAllocatedAmountByActivityId(@Param("activityId") Long activityId);

}
