package vn.campuslife.controller.activity.task;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.task.CreateActivityTaskRequest;
import vn.campuslife.model.activity.task.TaskAssignmentRequest;
import vn.campuslife.security.department.DepartmentRequestScope;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.security.department.DepartmentScopeRouting;
import vn.campuslife.service.ActivityTaskService;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class ActivityTaskController {

    private final ActivityTaskService activityTaskService;
    private final DepartmentScopeRouting departmentScopeRouting;

    /**
     * Tạo nhiệm vụ mới
     */
    @PostMapping
    public ResponseEntity<Response> createTask(@RequestBody @Valid CreateActivityTaskRequest request,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? activityTaskService.createTask(request, scope)
                : activityTaskService.createTask(request);
        return ResponseEntity.status(response.isStatus() ? 201 : 400).body(response);
    }

    /**
     * Lấy danh sách nhiệm vụ theo hoạt động
     */
    @GetMapping("/activity/{activityId}")
    public ResponseEntity<Response> getTasksByActivity(@PathVariable Long activityId, HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? activityTaskService.getTasksByActivity(activityId, scope)
                : activityTaskService.getTasksByActivity(activityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy chi tiết nhiệm vụ
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<Response> getTaskById(@PathVariable Long taskId, HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? activityTaskService.getTaskById(taskId, scope)
                : activityTaskService.getTaskById(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật nhiệm vụ
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<Response> updateTask(@PathVariable Long taskId,
            @RequestBody @Valid CreateActivityTaskRequest request,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? activityTaskService.updateTask(taskId, request, scope)
                : activityTaskService.updateTask(taskId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa nhiệm vụ
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Response> deleteTask(@PathVariable Long taskId, HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? activityTaskService.deleteTask(taskId, scope)
                : activityTaskService.deleteTask(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phân công nhiệm vụ cho sinh viên
     */
    @PostMapping("/assign")
    public ResponseEntity<Response> assignTask(@RequestBody @Valid TaskAssignmentRequest request,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? activityTaskService.assignTask(request, scope)
                : activityTaskService.assignTask(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách phân công theo nhiệm vụ
     */
    @GetMapping("/{taskId}/assignments")
    public ResponseEntity<Response> getTaskAssignments(@PathVariable Long taskId, HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? activityTaskService.getTaskAssignments(taskId, scope)
                : activityTaskService.getTaskAssignments(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Tự động phân công nhiệm vụ bắt buộc
     */
    @PostMapping("/auto-assign/{activityId}")
    public ResponseEntity<Response> autoAssignMandatoryTasks(@PathVariable Long activityId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? activityTaskService.autoAssignMandatoryTasks(activityId, scope)
                : activityTaskService.autoAssignMandatoryTasks(activityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách sinh viên đăng ký cho activity để phân công nhiệm vụ
     */
    @GetMapping("/activity/{activityId}/registered-students")
    public ResponseEntity<Response> getRegisteredStudentsForActivity(@PathVariable Long activityId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? activityTaskService.getRegisteredStudentsForActivity(activityId, scope)
                : activityTaskService.getRegisteredStudentsForActivity(activityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phân công nhiệm vụ cho tất cả sinh viên đăng ký activity
     */
    @PostMapping("/assign-to-registered/{activityId}")
    public ResponseEntity<Response> assignTaskToRegisteredStudents(@PathVariable Long activityId,
            @RequestParam Long taskId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = departmentScopeRouting.useManagerScopedPath(scope)
                ? activityTaskService.assignTaskToRegisteredStudents(activityId, taskId, scope)
                : activityTaskService.assignTaskToRegisteredStudents(activityId, taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Kiểm tra và cập nhật OVERDUE cho các task assignment quá hạn
     * Endpoint để test thủ công hoặc trigger ngay lập tức
     */
    @PostMapping("/check-overdue")
    public ResponseEntity<Response> checkOverdueAssignments() {
        Response response = activityTaskService.checkAndUpdateOverdueAssignments();
        return ResponseEntity.ok(response);
    }

    private DepartmentScope currentScope(HttpServletRequest request) {
        return DepartmentRequestScope.get(request).orElse(null);
    }
}

