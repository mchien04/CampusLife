package vn.campuslife.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.service.StudentClassService;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class StudentClassController {

    private final StudentClassService studentClassService;

    /**
     * Tạo lớp học mới
     */
    @PostMapping
    public ResponseEntity<Response> createClass(@RequestParam String className,
            @RequestParam(required = false) String description,
            @RequestParam Long departmentId) {
        try {
            Response response = studentClassService.createClass(className, description, departmentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to create class: " + e.getMessage(), null));
        }
    }

    /**
     * Cập nhật thông tin lớp học
     */
    @PutMapping("/{classId}")
    public ResponseEntity<Response> updateClass(@PathVariable Long classId,
            @RequestParam String className,
            @RequestParam(required = false) String description) {
        try {
            Response response = studentClassService.updateClass(classId, className, description);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to update class: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy danh sách tất cả lớp học
     */
    @GetMapping
    public ResponseEntity<Response> getAllClasses() {
        try {
            Response response = studentClassService.getAllClasses();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get classes: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy danh sách lớp học theo department
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Response> getClassesByDepartment(@PathVariable Long departmentId) {
        try {
            Response response = studentClassService.getClassesByDepartment(departmentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get classes: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy thông tin lớp học theo ID
     */
    @GetMapping("/{classId}")
    public ResponseEntity<Response> getClassById(@PathVariable Long classId) {
        try {
            Response response = studentClassService.getClassById(classId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get class: " + e.getMessage(), null));
        }
    }

    /**
     * Xóa lớp học
     */
    @DeleteMapping("/{classId}")
    public ResponseEntity<Response> deleteClass(@PathVariable Long classId) {
        try {
            Response response = studentClassService.deleteClass(classId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to delete class: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy danh sách sinh viên trong lớp
     */
    @GetMapping("/{classId}/students")
    public ResponseEntity<Response> getStudentsInClass(@PathVariable Long classId) {
        try {
            Response response = studentClassService.getStudentsInClass(classId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get students: " + e.getMessage(), null));
        }
    }

    /**
     * Thêm sinh viên vào lớp
     */
    @PostMapping("/{classId}/students/{studentId}")
    public ResponseEntity<Response> addStudentToClass(@PathVariable Long classId,
            @PathVariable Long studentId) {
        try {
            Response response = studentClassService.addStudentToClass(classId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to add student to class: " + e.getMessage(), null));
        }
    }

    /**
     * Xóa sinh viên khỏi lớp
     */
    @DeleteMapping("/{classId}/students/{studentId}")
    public ResponseEntity<Response> removeStudentFromClass(@PathVariable Long classId,
            @PathVariable Long studentId) {
        try {
            Response response = studentClassService.removeStudentFromClass(classId, studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to remove student from class: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy thông tin lớp học theo tên
     */
    @GetMapping("/name/{className}")
    public ResponseEntity<Response> getClassByName(@PathVariable String className) {
        try {
            Response response = studentClassService.getClassByName(className);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get class: " + e.getMessage(), null));
        }
    }
}
