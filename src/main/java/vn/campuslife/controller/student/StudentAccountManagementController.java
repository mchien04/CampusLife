package vn.campuslife.controller.student;

import jakarta.servlet.http.HttpServletRequest;
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
import vn.campuslife.security.department.DepartmentRequestScope;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.service.StudentAccountManagementService;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class StudentAccountManagementController {

    private final StudentAccountManagementService studentAccountManagementService;

    @GetMapping("/validate")
    public ResponseEntity<Response> validateStudentAccount(
            @RequestParam(required = false) String studentCode,
            @RequestParam(required = false) String email) {
        Response response = studentAccountManagementService.validateStudentAccount(studentCode, email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload-excel")
    public ResponseEntity<Response> uploadExcel(@RequestParam("file") MultipartFile file) {
        Response response = studentAccountManagementService.uploadAndParseExcel(file);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }

    @PostMapping("/bulk-create")
    public ResponseEntity<Response> bulkCreateStudents(@RequestBody BulkCreateStudentsRequest request) {
        Response response = studentAccountManagementService.bulkCreateStudents(request);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }

    @PostMapping("/create")
    public ResponseEntity<Response> createStudent(
            @RequestBody CreateStudentRequest request,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = hasManagerScope(scope)
                ? studentAccountManagementService.createStudent(request, scope)
                : studentAccountManagementService.createStudent(request);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }

    @PostMapping("/create-multiple")
    public ResponseEntity<Response> createMultipleStudents(@RequestBody CreateMultipleStudentsRequest request) {
        Response response = studentAccountManagementService.createMultipleStudents(request);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<Response> getPendingAccounts(HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = hasManagerScope(scope)
                ? studentAccountManagementService.getPendingAccounts(scope)
                : studentAccountManagementService.getPendingAccounts();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{studentId}/account")
    public ResponseEntity<Response> updateStudentAccount(
            @PathVariable Long studentId,
            @RequestBody UpdateStudentAccountRequest request,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = hasManagerScope(scope)
                ? studentAccountManagementService.updateStudentAccount(studentId, request, scope)
                : studentAccountManagementService.updateStudentAccount(studentId, request);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }

    @DeleteMapping("/{studentId}/account")
    public ResponseEntity<Response> deleteStudentAccount(@PathVariable Long studentId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = hasManagerScope(scope)
                ? studentAccountManagementService.deleteStudentAccount(studentId, scope)
                : studentAccountManagementService.deleteStudentAccount(studentId);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }

    @PostMapping("/{studentId}/send-credentials")
    public ResponseEntity<Response> sendCredentials(@PathVariable Long studentId,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = hasManagerScope(scope)
                ? studentAccountManagementService.sendCredentials(studentId, scope)
                : studentAccountManagementService.sendCredentials(studentId);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }

    @PostMapping("/bulk-send-credentials")
    public ResponseEntity<Response> bulkSendCredentials(@RequestBody BulkSendCredentialsRequest request,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        Response response = hasManagerScope(scope)
                ? studentAccountManagementService.bulkSendCredentials(request, scope)
                : studentAccountManagementService.bulkSendCredentials(request);
        return ResponseEntity.status(response.isStatus() ? 200 : 400).body(response);
    }

    private DepartmentScope currentScope(HttpServletRequest request) {
        return DepartmentRequestScope.get(request).orElse(null);
    }

    private boolean hasManagerScope(DepartmentScope scope) {
        return scope != null && scope.manager() && !scope.departmentIds().isEmpty();
    }
}
