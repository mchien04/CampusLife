package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.CriterionGroup;

@Repository
public interface CriterionGroupRepository extends JpaRepository<CriterionGroup, Long> {
}


