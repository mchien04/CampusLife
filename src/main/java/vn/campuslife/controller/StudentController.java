package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.model.Response;
import vn.campuslife.service.StudentService;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    /**
     * Lấy danh sách tất cả sinh viên (có phân trang)
     */
    @GetMapping
    public ResponseEntity<Response> getAllStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Response response = studentService.getAllStudents(pageable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get students: " + e.getMessage(), null));
        }
    }

    /**
     * Tìm kiếm sinh viên theo tên hoặc mã sinh viên
     */
    @GetMapping("/search")
    public ResponseEntity<Response> searchStudents(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
            Response response = studentService.searchStudents(keyword, pageable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to search students: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy sinh viên chưa có lớp
     */
    @GetMapping("/without-class")
    public ResponseEntity<Response> getStudentsWithoutClass(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
            Response response = studentService.getStudentsWithoutClass(pageable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get students without class: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy sinh viên theo khoa
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Response> getStudentsByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
            Response response = studentService.getStudentsByDepartment(departmentId, pageable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get students by department: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy thông tin sinh viên theo ID
     */
    @GetMapping("/{studentId}")
    public ResponseEntity<Response> getStudentById(@PathVariable Long studentId) {
        try {
            Response response = studentService.getStudentById(studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get student: " + e.getMessage(), null));
        }
    }

    /**
     * Lấy thông tin sinh viên theo username
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<Response> getStudentByUsername(@PathVariable String username) {
        try {
            Response response = studentService.getStudentByUsername(username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, "Failed to get student: " + e.getMessage(), null));
        }
    }
}