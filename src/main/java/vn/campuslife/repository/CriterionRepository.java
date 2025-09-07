package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Criterion;

@Repository
public interface CriterionRepository extends JpaRepository<Criterion, Long> {
}
