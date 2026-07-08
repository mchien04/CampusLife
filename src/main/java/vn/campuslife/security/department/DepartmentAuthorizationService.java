package vn.campuslife.security.department;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.config.DepartmentScopeProperties;
import vn.campuslife.exception.ForbiddenException;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.ActivitySeriesRepository;
import vn.campuslife.repository.EventArticleRepository;
import vn.campuslife.repository.MiniGameRepository;
import vn.campuslife.repository.StudentClassRepository;
import vn.campuslife.repository.StudentRepository;

import java.util.Set;

@Service
public class DepartmentAuthorizationService {

    private static final String ACCESS_DENIED = "Access denied";

    private final ActivityRepository activityRepository;
    private final ActivitySeriesRepository activitySeriesRepository;
    private final EventArticleRepository eventArticleRepository;
    private final MiniGameRepository miniGameRepository;
    private final StudentRepository studentRepository;
    private final StudentClassRepository studentClassRepository;
    private final DepartmentScopeProperties properties;
    private final DepartmentScopeAuditService auditService;

    public DepartmentAuthorizationService(
            ActivityRepository activityRepository,
            ActivitySeriesRepository activitySeriesRepository,
            EventArticleRepository eventArticleRepository,
            MiniGameRepository miniGameRepository,
            StudentRepository studentRepository,
            StudentClassRepository studentClassRepository,
            DepartmentScopeProperties properties,
            DepartmentScopeAuditService auditService) {
        this.activityRepository = activityRepository;
        this.activitySeriesRepository = activitySeriesRepository;
        this.eventArticleRepository = eventArticleRepository;
        this.miniGameRepository = miniGameRepository;
        this.studentRepository = studentRepository;
        this.studentClassRepository = studentClassRepository;
        this.properties = properties;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public void requireActivityAccess(Long activityId, DepartmentScope scope) {
        if (scope.admin()) {
            auditService.logAdminBypass("Activity", activityId, Set.of());
            return;
        }
        if (!scope.manager() || !canAccessActivity(activityId, scope)) {
            denyAccess("Activity", activityId, scope, "activity access denied");
        }
    }

    @Transactional(readOnly = true)
    public boolean canAccessActivity(Long activityId, DepartmentScope scope) {
        if (scope.admin()) {
            return true;
        }
        if (!scope.manager() || scope.departmentIds().isEmpty()) {
            return false;
        }
        return activityRepository.existsActiveByIdAndOrganizerDepartmentIds(activityId, scope.departmentIds());
    }

    @Transactional(readOnly = true)
    public void requireStudentAccess(Long studentId, DepartmentScope scope) {
        if (scope.admin()) {
            auditService.logAdminBypass("Student", studentId, Set.of());
            return;
        }
        if (scope.student() && studentId != null && studentId.equals(scope.studentId())) {
            return;
        }
        if (!scope.manager()
                || scope.departmentIds().isEmpty()
                || !studentRepository.existsActiveByIdAndDepartmentIds(studentId, scope.departmentIds())) {
            denyAccess("Student", studentId, scope, "student access denied");
        }
    }

    @Transactional(readOnly = true)
    public void requireStudentClassAccess(Long classId, DepartmentScope scope) {
        if (scope.admin()) {
            auditService.logAdminBypass("StudentClass", classId, Set.of());
            return;
        }
        if (!scope.manager()
                || scope.departmentIds().isEmpty()
                || !studentClassRepository.existsActiveByIdAndDepartmentIds(classId, scope.departmentIds())) {
            denyAccess("StudentClass", classId, scope, "class access denied");
        }
    }

    @Transactional(readOnly = true)
    public void requireEventArticleAccess(Long articleId, DepartmentScope scope) {
        if (scope.admin()) {
            auditService.logAdminBypass("EventArticle", articleId, Set.of());
            return;
        }
        if (!scope.manager() || !canAccessEventArticle(articleId, scope)) {
            denyAccess("EventArticle", articleId, scope, "event article access denied");
        }
    }

    @Transactional(readOnly = true)
    public boolean canAccessEventArticle(Long articleId, DepartmentScope scope) {
        if (scope.admin()) {
            return true;
        }
        if (!scope.manager() || scope.departmentIds().isEmpty()) {
            return false;
        }
        return eventArticleRepository.existsByIdAndOwnerDepartmentIds(articleId, scope.departmentIds());
    }

    @Transactional(readOnly = true)
    public void requireSeriesAccess(Long seriesId, DepartmentScope scope) {
        if (scope.admin()) {
            auditService.logAdminBypass("ActivitySeries", seriesId, Set.of());
            return;
        }
        if (!scope.manager() || !canAccessSeries(seriesId, scope)) {
            denyAccess("ActivitySeries", seriesId, scope, "series access denied");
        }
    }

    @Transactional(readOnly = true)
    public boolean canAccessSeries(Long seriesId, DepartmentScope scope) {
        if (scope.admin()) {
            return true;
        }
        if (!scope.manager() || scope.departmentIds().isEmpty()) {
            return false;
        }
        return activitySeriesRepository.existsActiveByIdAndDepartmentScope(seriesId, scope.departmentIds());
    }

    @Transactional(readOnly = true)
    public void requireMiniGameAccess(Long miniGameId, DepartmentScope scope) {
        if (scope.admin()) {
            auditService.logAdminBypass("MiniGame", miniGameId, Set.of());
            return;
        }
        if (!scope.manager() || !canAccessMiniGame(miniGameId, scope)) {
            denyAccess("MiniGame", miniGameId, scope, "minigame access denied");
        }
    }

    @Transactional(readOnly = true)
    public boolean canAccessMiniGame(Long miniGameId, DepartmentScope scope) {
        if (scope.admin()) {
            return true;
        }
        if (!scope.manager() || scope.departmentIds().isEmpty()) {
            return false;
        }
        return miniGameRepository.existsByIdAndActivityOrganizerDepartmentIds(miniGameId, scope.departmentIds());
    }

    public Set<Long> managerDepartmentFilter(DepartmentScope scope, Long requestedDepartmentId) {
        if (scope.admin()) {
            auditService.logAdminBypass("DepartmentFilter", requestedDepartmentId, requestedDepartmentId == null ? Set.of() : Set.of(requestedDepartmentId));
            return requestedDepartmentId == null ? Set.of() : Set.of(requestedDepartmentId);
        }
        if (!scope.manager() || scope.departmentIds().isEmpty()) {
            denyAccess("DepartmentFilter", requestedDepartmentId, scope, "department filter denied");
            return Set.of();
        }
        if (requestedDepartmentId == null) {
            return scope.departmentIds();
        }
        if (!scope.departmentIds().contains(requestedDepartmentId)) {
            denyAccess("DepartmentFilter", requestedDepartmentId, scope, "department filter out of scope");
            return properties.shouldDenyAccess() ? Set.of() : scope.departmentIds();
        }
        return Set.of(requestedDepartmentId);
    }

    private void denyAccess(String entityType, Long entityId, DepartmentScope scope, String reason) {
        auditService.logScopeViolation(entityType, entityId, scope, reason);
        if (properties.shouldDenyAccess()) {
            throw new ForbiddenException(ACCESS_DENIED);
        }
    }
}
