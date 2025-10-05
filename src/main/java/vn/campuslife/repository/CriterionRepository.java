package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Criterion;

import java.util.List;

@Repository
public interface CriterionRepository extends JpaRepository<Criterion, Long> {

    @Query("SELECT c FROM Criterion c WHERE c.group.name IS NOT NULL AND c.isDeleted = false")
    List<Criterion> findAllTrainingCriteria();
}
