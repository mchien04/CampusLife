package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.PreparationTask;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreparationTaskRepository extends JpaRepository<PreparationTask, Long> {
    List<PreparationTask> findByActivityIdOrderByDeadlineAscIdAsc(Long activityId);

    Optional<PreparationTask> findByIdAndAssigneeId(Long id, Long assigneeId);
}
