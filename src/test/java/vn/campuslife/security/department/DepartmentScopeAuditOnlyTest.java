package vn.campuslife.security.department;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import vn.campuslife.config.DepartmentScopeProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.Role;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Department;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.DepartmentType;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "department.scope.enforcement.enabled=true",
        "department.scope.auditOnly=true"
})
@EnableConfigurationProperties(DepartmentScopeProperties.class)
@Import({DepartmentAuthorizationService.class, DepartmentScopeAuditService.class})
class DepartmentScopeAuditOnlyTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DepartmentAuthorizationService authorizationService;

    private Department deptA;
    private Department deptB;
    private Activity activityDeptB;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        User manager = persistUser("manager-audit", Role.MANAGER);
        entityManager.flush();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(manager.getUsername(), "password", java.util.List.of()));

        deptA = persistDepartment("Dept A");
        deptB = persistDepartment("Dept B");
        activityDeptB = persistActivity("Activity B", deptB);
    }

    @Test
    void auditOnlyMode_LogsViolationWithoutBlocking() {
        DepartmentScope managerScope = DepartmentScope.manager(Set.of(deptA.getId()));

        assertDoesNotThrow(() -> authorizationService.requireActivityAccess(activityDeptB.getId(), managerScope));
    }

    private Department persistDepartment(String name) {
        Department department = new Department();
        department.setName(name);
        department.setType(DepartmentType.KHOA);
        entityManager.persist(department);
        return department;
    }

    private Activity persistActivity(String name, Department department) {
        Activity activity = new Activity();
        activity.setName(name);
        activity.setType(ActivityType.SUKIEN);
        activity.getOrganizers().add(department);
        entityManager.persist(activity);
        return activity;
    }

    private User persistUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@campuslife.test");
        user.setPassword("password");
        user.setRole(role);
        user.setActivated(true);
        entityManager.persist(user);
        return user;
    }
}
