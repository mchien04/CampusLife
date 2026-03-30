package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ActivityBudget;

import java.util.Optional;

@Repository
public interface ActivityBudgetRepository extends JpaRepository<ActivityBudget, Long> {
    Optional<ActivityBudget> findByActivityId(Long activityId);
}

