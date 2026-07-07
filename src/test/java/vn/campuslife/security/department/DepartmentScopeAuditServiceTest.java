package vn.campuslife.security.department;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.campuslife.config.DepartmentScopeProperties;
import vn.campuslife.entity.AuditLog;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.Role;
import vn.campuslife.repository.AuditLogRepository;
import vn.campuslife.repository.UserRepository;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentScopeAuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    private DepartmentScopeProperties properties;
    private DepartmentScopeAuditService auditService;

    @BeforeEach
    void setUp() {
        properties = new DepartmentScopeProperties();
        properties.getEnforcement().setEnabled(true);
        auditService = new DepartmentScopeAuditService(auditLogRepository, userRepository, properties);
        SecurityContextHolder.clearContext();
    }

    @Test
    void logScopeViolation_PersistsAuditLogForAuthenticatedUser() {
        User manager = user(Role.MANAGER, "manager-a");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("manager-a", "password", java.util.List.of()));
        when(userRepository.findByUsernameAndIsDeletedFalse("manager-a")).thenReturn(Optional.of(manager));

        auditService.logScopeViolation("Activity", 99L, DepartmentScope.manager(Set.of(1L)), "activity access denied");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(DepartmentScopeAuditService.ACTION_SCOPE_VIOLATION, captor.getValue().getAction());
        assertEquals("Activity", captor.getValue().getEntityType());
        assertEquals(99L, captor.getValue().getEntityId());
    }

    @Test
    void logAdminBypass_SkipsWhenEnforcementDisabled() {
        properties.getEnforcement().setEnabled(false);
        auditService.logAdminBypass("Activity", 1L, Set.of());

        verify(auditLogRepository, never()).save(any());
    }

    private User user(Role role, String username) {
        User user = new User();
        user.setId(10L);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }
}
