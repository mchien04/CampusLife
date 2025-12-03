package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.model.Response;
import vn.campuslife.model.student.BulkCreateStudentsRequest;
import vn.campuslife.model.student.BulkSendCredentialsRequest;
import vn.campuslife.model.student.UpdateStudentAccountRequest;
import vn.campuslife.service.StudentAccountManagementService;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class StudentAccountManagementController {
    
    private final StudentAccountManagementService studentAccountManagementService;
    
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
     * Tạo tài khoản hàng loạt từ danh sách
     * POST /api/admin/students/bulk-create
     */
    @PostMapping("/bulk-create")
    public ResponseEntity<Response> bulkCreateStudents(@RequestBody BulkCreateStudentsRequest request) {
        Response response = studentAccountManagementService.bulkCreateStudents(request);
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

