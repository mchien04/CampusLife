package vn.campuslife.security.department;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record DepartmentScope(
        boolean admin,
        boolean student,
        boolean managerRole,
        Long studentId,
        Set<Long> departmentIds) {

    public DepartmentScope {
        departmentIds = departmentIds == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(departmentIds));
    }

    public static DepartmentScope adminScope() {
        return new DepartmentScope(true, false, false, null, Set.of());
    }

    public static DepartmentScope manager(Set<Long> departmentIds) {
        return new DepartmentScope(false, false, true, null, departmentIds);
    }

    public static DepartmentScope student(Long studentId) {
        return new DepartmentScope(false, true, false, studentId, Set.of());
    }

    public static DepartmentScope unscoped() {
        return new DepartmentScope(false, false, false, null, Set.of());
    }

    public boolean manager() {
        return managerRole;
    }
}
