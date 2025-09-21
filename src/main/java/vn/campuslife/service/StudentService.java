package vn.campuslife.service;

import org.springframework.data.domain.Pageable;
import vn.campuslife.model.Response;

public interface StudentService {

    /**
     * Lấy Student ID theo username
     */
    Long getStudentIdByUsername(String username);

    /**
     * Lấy Student ID theo User ID
     */
    Long getStudentIdByUserId(Long userId);

    /**
     * Lấy danh sách tất cả sinh viên (có phân trang)
     */
    Response getAllStudents(Pageable pageable);

    /**
     * Tìm kiếm sinh viên theo tên hoặc mã sinh viên
     */
    Response searchStudents(String keyword, Pageable pageable);

    /**
     * Lấy sinh viên chưa có lớp
     */
    Response getStudentsWithoutClass(Pageable pageable);

    /**
     * Lấy sinh viên theo khoa
     */
    Response getStudentsByDepartment(Long departmentId, Pageable pageable);

    /**
     * Lấy thông tin sinh viên theo ID
     */
    Response getStudentById(Long studentId);

    /**
     * Lấy thông tin sinh viên theo username
     */
    Response getStudentByUsername(String username);
}