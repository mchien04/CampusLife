package vn.campuslife.service;

import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.model.Response;
import vn.campuslife.model.student.BulkCreateStudentsRequest;
import vn.campuslife.model.student.BulkSendCredentialsRequest;
import vn.campuslife.model.student.UpdateStudentAccountRequest;

/**
 * Service để quản lý tài khoản sinh viên cho admin
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
     * Lấy danh sách tài khoản chờ review (tất cả tài khoản đã tạo)
     */
    Response getPendingAccounts();
    
    /**
     * Chỉnh sửa thông tin tài khoản
     */
    Response updateStudentAccount(Long studentId, UpdateStudentAccountRequest request);
    
    /**
     * Xóa tài khoản (soft delete)
     */
    Response deleteStudentAccount(Long studentId);
    
    /**
     * Gửi email credentials cho 1 sinh viên
     */
    Response sendCredentials(Long studentId);
    
    /**
     * Gửi email credentials hàng loạt
     */
    Response bulkSendCredentials(BulkSendCredentialsRequest request);
}

