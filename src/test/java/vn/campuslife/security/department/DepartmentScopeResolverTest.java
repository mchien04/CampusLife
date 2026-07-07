package vn.campuslife.security.department;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.campuslife.config.DepartmentScopeProperties;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.Role;
import vn.campuslife.exception.ForbiddenException;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.UserDepartmentRepository;
import vn.campuslife.repository.UserRepository;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentScopeResolverTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDepartmentRepository userDepartmentRepository;

    @Mock
    private StudentRepository studentRepository;

    private DepartmentScopeProperties properties;
    private DepartmentScopeResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new DepartmentScopeProperties();
        resolver = new DepartmentScopeResolver(userRepository, userDepartmentRepository, studentRepository, properties);
    }

    @Test
    void resolve_Admin_ReturnsUnrestrictedScope() {
        when(userRepository.findByUsernameAndIsDeletedFalse("admin")).thenReturn(Optional.of(user(1L, "admin", Role.ADMIN)));

        DepartmentScope scope = resolver.resolve("admin");

        assertTrue(scope.admin());
        assertFalse(scope.student());
        assertTrue(scope.departmentIds().isEmpty());
        verifyNoInteractions(userDepartmentRepository, studentRepository);
    }

    @Test
    void resolve_Manager_ReturnsDbDepartmentIdsWithoutTypeFiltering() {
        User manager = user(2L, "manager", Role.MANAGER);
        when(userRepository.findByUsernameAndIsDeletedFalse("manager")).thenReturn(Optional.of(manager));
        when(userDepartmentRepository.findActiveDepartmentIdsByUserId(2L)).thenReturn(Set.of(10L, 20L));

        DepartmentScope scope = resolver.resolve("manager");

        assertTrue(scope.manager());
        assertEquals(Set.of(10L, 20L), scope.departmentIds());
    }

    @Test
    void resolve_ManagerWithoutDepartment_WhenEnforcementEnabled_ThrowsForbidden() {
        properties.getEnforcement().setEnabled(true);
        User manager = user(2L, "manager", Role.MANAGER);
        when(userRepository.findByUsernameAndIsDeletedFalse("manager")).thenReturn(Optional.of(manager));
        when(userDepartmentRepository.findActiveDepartmentIdsByUserId(2L)).thenReturn(Set.of());

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> resolver.resolve("manager"));

        assertEquals(DepartmentScopeResolver.MANAGER_UNASSIGNED_MESSAGE, exception.getMessage());
    }

    @Test
    void resolve_ManagerWithoutDepartment_WhenAuditOnly_ReturnsEmptyScope() {
        properties.getEnforcement().setEnabled(true);
        properties.setAuditOnly(true);
        User manager = user(2L, "manager", Role.MANAGER);
        when(userRepository.findByUsernameAndIsDeletedFalse("manager")).thenReturn(Optional.of(manager));
        when(userDepartmentRepository.findActiveDepartmentIdsByUserId(2L)).thenReturn(Set.of());

        DepartmentScope scope = resolver.resolve("manager");

        assertTrue(scope.manager());
        assertTrue(scope.departmentIds().isEmpty());
    }

    @Test
    void resolve_Student_ReturnsSelfScopeOnly() {
        Student student = new Student();
        student.setId(30L);
        when(userRepository.findByUsernameAndIsDeletedFalse("student")).thenReturn(Optional.of(user(3L, "student", Role.STUDENT)));
        when(studentRepository.findByUserUsernameAndIsDeletedFalse("student")).thenReturn(Optional.of(student));

        DepartmentScope scope = resolver.resolve("student");

        assertTrue(scope.student());
        assertEquals(30L, scope.studentId());
        assertTrue(scope.departmentIds().isEmpty());
        verifyNoInteractions(userDepartmentRepository);
    }

    private User user(Long id, String username, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }
}
