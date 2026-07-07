package vn.campuslife.service;

import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.model.Response;
import vn.campuslife.model.student.BulkCreateStudentsRequest;
import vn.campuslife.model.student.BulkSendCredentialsRequest;
import vn.campuslife.model.student.CreateMultipleStudentsRequest;
import vn.campuslife.model.student.CreateStudentRequest;
import vn.campuslife.model.student.UpdateStudentAccountRequest;
import vn.campuslife.security.department.DepartmentScope;

/**
 * Service để quản lý tài khoản sinh viên cho admin/manager
 */
public interface StudentAccountManagementService {
    
    /**
     * Upload và parse file Excel
     */
    Response uploadAndParseExcel(MultipartFile file);
    
    /**
     * Tạo tài khoản hàng loạt từ danh sách
     */
    Response bulkCreateStudents(BulkCreateStudentsRequest request);
    
    /**
     * Tạo tài khoản sinh viên đơn lẻ
     */
    Response createStudent(CreateStudentRequest request);

    Response createStudent(CreateStudentRequest request, DepartmentScope scope);
    
    /**
     * Tạo tài khoản sinh viên từ danh sách (không qua Excel)
     */
    Response createMultipleStudents(CreateMultipleStudentsRequest request);
    
    /**
     * Lấy danh sách tài khoản chờ review (tất cả tài khoản đã tạo)
     */
    Response getPendingAccounts();

    Response getPendingAccounts(DepartmentScope scope);
    
    /**
     * Chỉnh sửa thông tin tài khoản
     */
    Response updateStudentAccount(Long studentId, UpdateStudentAccountRequest request);

    Response updateStudentAccount(Long studentId, UpdateStudentAccountRequest request, DepartmentScope scope);
    
    /**
     * Xóa tài khoản (soft delete)
     */
    Response deleteStudentAccount(Long studentId);

    Response deleteStudentAccount(Long studentId, DepartmentScope scope);
    
    /**
     * Gửi email credentials cho 1 sinh viên
     */
    Response sendCredentials(Long studentId);

    Response sendCredentials(Long studentId, DepartmentScope scope);
    
    /**
     * Gửi email credentials hàng loạt
     */
    Response bulkSendCredentials(BulkSendCredentialsRequest request);

    Response bulkSendCredentials(BulkSendCredentialsRequest request, DepartmentScope scope);

    /**
     * Kiểm tra mã số sinh viên và email đã được sử dụng chưa
     */
    Response validateStudentAccount(String studentCode, String email);
}

