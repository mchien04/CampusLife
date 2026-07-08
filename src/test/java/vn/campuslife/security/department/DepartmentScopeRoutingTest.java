package vn.campuslife.security.department;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepartmentScopeRoutingTest {

    private final DepartmentScopeRouting routing = new DepartmentScopeRouting();

    @Test
    void useManagerScopedPath_trueForManagerEvenWhenDepartmentsEmpty() {
        DepartmentScope scope = DepartmentScope.manager(Set.of());

        assertTrue(routing.useManagerScopedPath(scope));
    }

    @Test
    void useManagerScopedPath_falseForAdminAndStudent() {
        assertFalse(routing.useManagerScopedPath(DepartmentScope.adminScope()));
        assertFalse(routing.useManagerScopedPath(DepartmentScope.student(1L)));
        assertFalse(routing.useManagerScopedPath(null));
    }

    @Test
    void useScopedPath_trueWhenScopePresent() {
        assertTrue(routing.useScopedPath(DepartmentScope.manager(Set.of(1L))));
        assertTrue(routing.useScopedPath(DepartmentScope.student(2L)));
        assertFalse(routing.useScopedPath(null));
    }
}
