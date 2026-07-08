package vn.campuslife.security.department;

import org.springframework.stereotype.Component;

@Component
public class DepartmentScopeRouting {

    public boolean useManagerScopedPath(DepartmentScope scope) {
        return scope != null && scope.manager();
    }

    public boolean useScopedPath(DepartmentScope scope) {
        return scope != null;
    }
}
