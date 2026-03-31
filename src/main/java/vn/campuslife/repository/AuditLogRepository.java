package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.AuditLog;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    boolean existsByActionAndEntityTypeAndEntityId(String action, String entityType, Long entityId);

    List<AuditLog> findByEntityTypeAndEntityIdInOrderByCreatedAtDesc(String entityType, List<Long> entityIds);
}
