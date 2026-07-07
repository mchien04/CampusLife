package vn.campuslife.service;

import org.springframework.data.domain.Pageable;
import vn.campuslife.model.Response;
import vn.campuslife.security.department.DepartmentScope;

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

    Response getAllStudents(Pageable pageable, DepartmentScope scope);

    /**
     * Tìm kiếm sinh viên theo tên hoặc mã sinh viên
     */
    Response searchStudents(String keyword, Pageable pageable);

    Response searchStudents(String keyword, Pageable pageable, DepartmentScope scope);

    /**
     * Lấy sinh viên chưa có lớp
     */
    Response getStudentsWithoutClass(Pageable pageable);

    Response getStudentsWithoutClass(Pageable pageable, DepartmentScope scope);

    /**
     * Lấy sinh viên theo khoa
     */
    Response getStudentsByDepartment(Long departmentId, Pageable pageable);

    Response getStudentsByDepartment(Long departmentId, Pageable pageable, DepartmentScope scope);

    /**
     * Lấy thông tin sinh viên theo ID
     */
    Response getStudentById(Long studentId);

    Response getStudentById(Long studentId, DepartmentScope scope);

    /**
     * Lấy thông tin sinh viên theo username
     */
    Response getStudentByUsername(String username);

    Response getStudentByUsername(String username, DepartmentScope scope);
}