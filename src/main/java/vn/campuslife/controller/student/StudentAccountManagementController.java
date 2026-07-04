package vn.campuslife.controller.student;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.model.Response;
import vn.campuslife.model.student.BulkCreateStudentsRequest;
import vn.campuslife.model.student.BulkSendCredentialsRequest;
import vn.campuslife.model.student.CreateMultipleStudentsRequest;
import vn.campuslife.model.student.CreateStudentRequest;
import vn.campuslife.model.student.UpdateStudentAccountRequest;
import vn.campuslife.service.StudentAccountManagementService;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class StudentAccountManagementController {
    
    private final StudentAccountManagementService studentAccountManagementService;
    
    /**
     * Kiểm tra mã số sinh viên và email đã được sử dụng chưa
     * GET /api/admin/students/validate?studentCode=...&email=...
     */
    @GetMapping("/validate")
    public ResponseEntity<Response> validateStudentAccount(
            @RequestParam(required = false) String studentCode,
            @RequestParam(required = false) String email) {
        Response response = studentAccountManagementService.validateStudentAccount(studentCode, email);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload và parse file Excel
     * POST /api/admin/students/upload-excel
     */
    @PostMapping("/upload-excel")
    public ResponseEntity<Response> uploadExcel(@RequestParam("file") MultipartFile file) {
        Response response = studentAccountManagementService.uploadAndParseExcel(file);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }
    
    /**
     * Tạo tài khoản hàng loạt từ danh sách (Import Excel)
     * POST /api/admin/students/bulk-create
     */
    @PostMapping("/bulk-create")
    public ResponseEntity<Response> bulkCreateStudents(@RequestBody BulkCreateStudentsRequest request) {
        Response response = studentAccountManagementService.bulkCreateStudents(request);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }
    
    /**
     * Tạo tài khoản sinh viên đơn lẻ
     * POST /api/admin/students/create
     */
    @PostMapping("/create")
    public ResponseEntity<Response> createStudent(@RequestBody CreateStudentRequest request) {
        Response response = studentAccountManagementService.createStudent(request);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }
    
    /**
     * Tạo tài khoản sinh viên từ danh sách json array (không qua Excel)
     * POST /api/admin/students/create-multiple
     */
    @PostMapping("/create-multiple")
    public ResponseEntity<Response> createMultipleStudents(@RequestBody CreateMultipleStudentsRequest request) {
        Response response = studentAccountManagementService.createMultipleStudents(request);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }
    
    /**
     * Lấy danh sách tài khoản chờ review
     * GET /api/admin/students/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<Response> getPendingAccounts() {
        Response response = studentAccountManagementService.getPendingAccounts();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Chỉnh sửa thông tin tài khoản
     * PUT /api/admin/students/{studentId}/account
     */
    @PutMapping("/{studentId}/account")
    public ResponseEntity<Response> updateStudentAccount(
            @PathVariable Long studentId,
            @RequestBody UpdateStudentAccountRequest request) {
        Response response = studentAccountManagementService.updateStudentAccount(studentId, request);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }
    
    /**
     * Xóa tài khoản (soft delete)
     * DELETE /api/admin/students/{studentId}/account
     */
    @DeleteMapping("/{studentId}/account")
    public ResponseEntity<Response> deleteStudentAccount(@PathVariable Long studentId) {
        Response response = studentAccountManagementService.deleteStudentAccount(studentId);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }
    
    /**
     * Gửi email credentials cho 1 sinh viên
     * POST /api/admin/students/{studentId}/send-credentials
     */
    @PostMapping("/{studentId}/send-credentials")
    public ResponseEntity<Response> sendCredentials(@PathVariable Long studentId) {
        Response response = studentAccountManagementService.sendCredentials(studentId);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }
    
    /**
     * Gửi email credentials hàng loạt
     * POST /api/admin/students/bulk-send-credentials
     */
    @PostMapping("/bulk-send-credentials")
    public ResponseEntity<Response> bulkSendCredentials(@RequestBody BulkSendCredentialsRequest request) {
        Response response = studentAccountManagementService.bulkSendCredentials(request);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }
}


