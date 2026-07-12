package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ManualScoreAdjustment;

@Repository
public interface ManualScoreAdjustmentRepository extends JpaRepository<ManualScoreAdjustment, Long> {
}
