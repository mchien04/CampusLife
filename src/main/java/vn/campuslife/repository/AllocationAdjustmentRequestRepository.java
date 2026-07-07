package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.AllocationAdjustmentRequest;
import vn.campuslife.enumeration.AllocationAdjustmentStatus;

import java.util.List;

@Repository
public interface AllocationAdjustmentRequestRepository extends JpaRepository<AllocationAdjustmentRequest, Long>, JpaSpecificationExecutor<AllocationAdjustmentRequest> {
    List<AllocationAdjustmentRequest> findByTaskActivityIdOrderByCreatedAtDesc(Long activityId);

    List<AllocationAdjustmentRequest> findByTaskActivityIdAndStatusOrderByCreatedAtDesc(Long activityId,
            AllocationAdjustmentStatus status);
}

