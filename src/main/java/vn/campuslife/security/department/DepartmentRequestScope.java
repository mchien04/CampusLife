package vn.campuslife.security.department;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public final class DepartmentRequestScope {
    public static final String ATTRIBUTE_NAME = "departmentScope";

    private DepartmentRequestScope() {
    }

    public static Optional<DepartmentScope> get(HttpServletRequest request) {
        Object value = request.getAttribute(ATTRIBUTE_NAME);
        if (value instanceof DepartmentScope scope) {
            return Optional.of(scope);
        }
        return Optional.empty();
    }

    public static void set(HttpServletRequest request, DepartmentScope scope) {
        request.setAttribute(ATTRIBUTE_NAME, scope);
    }

    public static void clear(HttpServletRequest request) {
        request.removeAttribute(ATTRIBUTE_NAME);
    }
}
