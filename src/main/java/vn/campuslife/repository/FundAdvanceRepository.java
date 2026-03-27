package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.FundAdvance;
import vn.campuslife.enumeration.FundAdvanceStatus;

import java.util.List;

@Repository
public interface FundAdvanceRepository extends JpaRepository<FundAdvance, Long> {
    List<FundAdvance> findByTaskIdAndStudentIdAndStatusOrderByCreatedAtAsc(Long taskId, Long studentId, FundAdvanceStatus status);
}

