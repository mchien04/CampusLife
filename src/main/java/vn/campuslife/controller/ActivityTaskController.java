package vn.campuslife.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.*;
import vn.campuslife.service.ActivityTaskService;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class ActivityTaskController {

    private final ActivityTaskService activityTaskService;

    /**
     * Tạo nhiệm vụ mới
     */
    @PostMapping
    public ResponseEntity<Response> createTask(@RequestBody @Valid CreateActivityTaskRequest request) {
        Response response = activityTaskService.createTask(request);
        return ResponseEntity.status(response.isStatus() ? 201 : 400).body(response);
    }

    /**
     * Lấy danh sách nhiệm vụ theo hoạt động
     */
    @GetMapping("/activity/{activityId}")
    public ResponseEntity<Response> getTasksByActivity(@PathVariable Long activityId) {
        Response response = activityTaskService.getTasksByActivity(activityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy chi tiết nhiệm vụ
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<Response> getTaskById(@PathVariable Long taskId) {
        Response response = activityTaskService.getTaskById(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật nhiệm vụ
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<Response> updateTask(@PathVariable Long taskId,
            @RequestBody @Valid CreateActivityTaskRequest request) {
        Response response = activityTaskService.updateTask(taskId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa nhiệm vụ
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Response> deleteTask(@PathVariable Long taskId) {
        Response response = activityTaskService.deleteTask(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phân công nhiệm vụ cho sinh viên
     */
    @PostMapping("/assign")
    public ResponseEntity<Response> assignTask(@RequestBody @Valid TaskAssignmentRequest request) {
        Response response = activityTaskService.assignTask(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách phân công theo nhiệm vụ
     */
    @GetMapping("/{taskId}/assignments")
    public ResponseEntity<Response> getTaskAssignments(@PathVariable Long taskId) {
        Response response = activityTaskService.getTaskAssignments(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Tự động phân công nhiệm vụ bắt buộc
     */
    @PostMapping("/auto-assign/{activityId}")
    public ResponseEntity<Response> autoAssignMandatoryTasks(@PathVariable Long activityId) {
        Response response = activityTaskService.autoAssignMandatoryTasks(activityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách sinh viên đăng ký cho activity để phân công nhiệm vụ
     */
    @GetMapping("/activity/{activityId}/registered-students")
    public ResponseEntity<Response> getRegisteredStudentsForActivity(@PathVariable Long activityId) {
        Response response = activityTaskService.getRegisteredStudentsForActivity(activityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phân công nhiệm vụ cho tất cả sinh viên đăng ký activity
     */
    @PostMapping("/assign-to-registered/{activityId}")
    public ResponseEntity<Response> assignTaskToRegisteredStudents(@PathVariable Long activityId,
            @RequestParam Long taskId) {
        Response response = activityTaskService.assignTaskToRegisteredStudents(activityId, taskId);
        return ResponseEntity.ok(response);
    }
}
