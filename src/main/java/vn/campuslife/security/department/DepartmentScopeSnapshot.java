package vn.campuslife.security.department;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record DepartmentScopeSnapshot(
        String username,
        String role,
        boolean admin,
        boolean student,
        Long studentId,
        Set<Long> departmentIds,
        LocalDateTime capturedAt) {

    public DepartmentScopeSnapshot {
        departmentIds = departmentIds == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(departmentIds));
        capturedAt = capturedAt == null ? LocalDateTime.now() : capturedAt;
    }

    public static DepartmentScopeSnapshot from(String username, String role, DepartmentScope scope) {
        return new DepartmentScopeSnapshot(
                username,
                role,
                scope.admin(),
                scope.student(),
                scope.studentId(),
                scope.departmentIds(),
                LocalDateTime.now());
    }
}
