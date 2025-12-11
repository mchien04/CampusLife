package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.service.ActivityTaskService;
import vn.campuslife.service.StudentService;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class TaskAssignmentController {

    private final ActivityTaskService activityTaskService;
    private final StudentService studentService;

    /**
     * Lấy danh sách nhiệm vụ của chính mình (sinh viên)
     */
    @GetMapping("/my")
    public ResponseEntity<Response> getMyTasks(Authentication authentication) {
        try {
            Long studentId = getStudentIdFromAuth(authentication);
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(new Response(false, "Student not found", null));
            }
            Response response = activityTaskService.getStudentTasks(studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get tasks: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy danh sách nhiệm vụ của sinh viên (Admin/Manager có thể xem của bất kỳ sinh viên nào)
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

    /**
     * Helper method to get student ID from authentication
     */
    private Long getStudentIdFromAuth(Authentication authentication) {
        try {
            String username = authentication.getName();
            return studentService.getStudentIdByUsername(username);
        } catch (Exception e) {
            return null;
        }
    }
    @GetMapping("/activity/{activityId}/student/{studentId}")
    public ResponseEntity<Response> getAssignmentsByActivityAndStudent(
            @PathVariable Long activityId,
            @PathVariable Long studentId) {
        Response response = activityTaskService.getAssignmentsByActivityAndStudent(activityId, studentId);
        return ResponseEntity.ok(response);
    }
}
