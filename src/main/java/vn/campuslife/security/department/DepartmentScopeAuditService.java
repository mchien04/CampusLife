package vn.campuslife.security.department;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.config.DepartmentScopeProperties;
import vn.campuslife.entity.AuditLog;
import vn.campuslife.entity.User;
import vn.campuslife.repository.AuditLogRepository;
import vn.campuslife.repository.UserRepository;

import java.util.Set;

@Service
public class DepartmentScopeAuditService {

    public static final String ACTION_SCOPE_VIOLATION = "DEPT_SCOPE_VIOLATION";
    public static final String ACTION_ADMIN_BYPASS = "DEPT_ADMIN_BYPASS";

    private static final Logger logger = LoggerFactory.getLogger(DepartmentScopeAuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final DepartmentScopeProperties properties;

    public DepartmentScopeAuditService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            DepartmentScopeProperties properties) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logScopeViolation(String entityType, Long entityId, DepartmentScope scope, String reason) {
        String username = currentUsername();
        logger.warn(
                "Department scope violation: user={} entityType={} entityId={} scope={} reason={}",
                username,
                entityType,
                entityId,
                scope == null ? null : scope.departmentIds(),
                reason);
        persist(username, ACTION_SCOPE_VIOLATION, entityType, entityId, reason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAdminBypass(String entityType, Long entityId, Set<Long> targetDepartmentIds) {
        if (!properties.isEnforcementEnabled()) {
            return;
        }
        String username = currentUsername();
        String detail = targetDepartmentIds == null || targetDepartmentIds.isEmpty()
                ? "full-access"
                : "departments=" + targetDepartmentIds;
        logger.info(
                "Admin department scope bypass: user={} entityType={} entityId={} {}",
                username,
                entityType,
                entityId,
                detail);
        persist(username, ACTION_ADMIN_BYPASS, entityType, entityId, detail);
    }

    private void persist(String username, String action, String entityType, Long entityId, String detail) {
        if (username == null) {
            return;
        }
        userRepository.findByUsernameAndIsDeletedFalse(username).ifPresent(user -> persist(user, action, entityType, entityId, detail));
    }

    private void persist(User actor, String action, String entityType, Long entityId, String detail) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId == null ? 0L : entityId);
        log.setDetail(detail);
        auditLogRepository.save(log);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return authentication.getName();
    }
}
