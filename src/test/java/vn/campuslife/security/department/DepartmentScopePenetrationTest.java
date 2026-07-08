package vn.campuslife.security.department;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import vn.campuslife.config.DepartmentScopeProperties;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Department;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.DepartmentType;
import vn.campuslife.enumeration.Role;
import vn.campuslife.exception.ForbiddenException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "department.scope.enforcement.enabled=true")
@EnableConfigurationProperties(DepartmentScopeProperties.class)
@Import({DepartmentAuthorizationService.class, DepartmentScopeAuditService.class})
class DepartmentScopePenetrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DepartmentAuthorizationService authorizationService;

    private Department deptA;
    private Department deptB;
    private Activity activityDeptA;
    private Activity activityDeptB;
    private Student studentDeptB;
    private User managerUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        deptA = persistDepartment("Dept A");
        deptB = persistDepartment("Dept B");
        activityDeptA = persistActivity("Activity A", deptA);
        activityDeptB = persistActivity("Activity B", deptB);
        studentDeptB = persistStudent("student-b", deptB);
        managerUser = persistUser("manager-a", Role.MANAGER);
        entityManager.flush();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(managerUser.getUsername(), "password", java.util.List.of()));
    }

    @Test
    void managerCannotAccessActivityInOtherDepartment() {
        DepartmentScope managerScope = DepartmentScope.manager(Set.of(deptA.getId()));

        assertThrows(
                ForbiddenException.class,
                () -> authorizationService.requireActivityAccess(activityDeptB.getId(), managerScope));
    }

    @Test
    void managerCanAccessActivityInOwnDepartment() {
        DepartmentScope managerScope = DepartmentScope.manager(Set.of(deptA.getId()));

        assertDoesNotThrow(() -> authorizationService.requireActivityAccess(activityDeptA.getId(), managerScope));
    }

    @Test
    void managerCannotAccessStudentInOtherDepartment() {
        DepartmentScope managerScope = DepartmentScope.manager(Set.of(deptA.getId()));

        assertThrows(
                ForbiddenException.class,
                () -> authorizationService.requireStudentAccess(studentDeptB.getId(), managerScope));
    }

    @Test
    void managerWithEmptyDepartmentsCannotAccessStudentInOtherDepartment() {
        DepartmentScope emptyManagerScope = DepartmentScope.manager(Set.of());

        assertThrows(
                ForbiddenException.class,
                () -> authorizationService.requireStudentAccess(studentDeptB.getId(), emptyManagerScope));
    }

    @Test
    void adminBypassIsAllowedWhenAuthenticated() {
        User admin = persistUser("admin-user", Role.ADMIN);
        entityManager.flush();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin.getUsername(), "password", java.util.List.of()));

        DepartmentScope adminScope = DepartmentScope.adminScope();

        assertDoesNotThrow(() -> authorizationService.requireActivityAccess(activityDeptB.getId(), adminScope));
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

    private Student persistStudent(String username, Department department) {
        User user = persistUser(username, Role.STUDENT);
        Student student = new Student();
        student.setUser(user);
        student.setStudentCode(username.toUpperCase());
        student.setDepartment(department);
        entityManager.persist(student);
        return student;
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
