package vn.campuslife.security.department;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.config.DepartmentScopeProperties;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.Role;
import vn.campuslife.exception.ForbiddenException;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.UserDepartmentRepository;
import vn.campuslife.repository.UserRepository;

import java.util.Set;

@Service
public class DepartmentScopeResolver {
    public static final String MANAGER_UNASSIGNED_MESSAGE = "Manager chưa được phân công Khoa";

    private static final Logger logger = LoggerFactory.getLogger(DepartmentScopeResolver.class);

    private final UserRepository userRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final StudentRepository studentRepository;
    private final DepartmentScopeProperties properties;

    public DepartmentScopeResolver(
            UserRepository userRepository,
            UserDepartmentRepository userDepartmentRepository,
            StudentRepository studentRepository,
            DepartmentScopeProperties properties) {
        this.userRepository = userRepository;
        this.userDepartmentRepository = userDepartmentRepository;
        this.studentRepository = studentRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public DepartmentScope resolve(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UsernameNotFoundException("Authenticated user is required to resolve department scope");
        }
        return resolve(authentication.getName());
    }

    @Transactional(readOnly = true)
    public DepartmentScope resolve(String username) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        Role role = user.getRole();
        if (role == Role.ADMIN) {
            return DepartmentScope.adminScope();
        }
        if (role == Role.MANAGER) {
            Set<Long> departmentIds = userDepartmentRepository.findActiveDepartmentIdsByUserId(user.getId());
            if (departmentIds == null || departmentIds.isEmpty()) {
                handleUnassignedManager(username);
                return DepartmentScope.manager(Set.of());
            }
            return DepartmentScope.manager(departmentIds);
        }
        if (role == Role.STUDENT) {
            Long studentId = studentRepository.findByUserUsernameAndIsDeletedFalse(username)
                    .map(Student::getId)
                    .orElse(null);
            return DepartmentScope.student(studentId);
        }
        return new DepartmentScope(false, false, null, Set.of());
    }

    private void handleUnassignedManager(String username) {
        if (properties.isAuditOnly()) {
            logger.warn("Department scope audit-only violation: manager {} has no assigned department", username);
            return;
        }
        if (properties.isEnforcementEnabled()) {
            throw new ForbiddenException(MANAGER_UNASSIGNED_MESSAGE);
        }
        logger.debug("Manager {} has no assigned department; enforcement disabled", username);
    }
}
