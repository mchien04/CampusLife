package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.service.ActivityTaskService;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class TaskAssignmentController {

    private final ActivityTaskService activityTaskService;

    /**
     * Lấy danh sách nhiệm vụ của sinh viên
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<Response> getStudentTasks(@PathVariable Long studentId) {
        Response response = activityTaskService.getStudentTasks(studentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật trạng thái nhiệm vụ
     */
    @PutMapping("/{assignmentId}/status")
    public ResponseEntity<Response> updateTaskStatus(@PathVariable Long assignmentId, 
                                                   @RequestParam String status) {
        Response response = activityTaskService.updateTaskStatus(assignmentId, status);
        return ResponseEntity.ok(response);
    }

    /**
     * Hủy phân công nhiệm vụ
     */
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Response> removeTaskAssignment(@PathVariable Long assignmentId) {
        Response response = activityTaskService.removeTaskAssignment(assignmentId);
        return ResponseEntity.ok(response);
    }
}
