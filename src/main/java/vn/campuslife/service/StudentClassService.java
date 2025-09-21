package vn.campuslife.service;

import org.springframework.data.domain.Pageable;
import vn.campuslife.model.Response;

import java.util.List;

public interface StudentClassService {

    // Tạo lớp học mới
    Response createClass(String className, String description, Long departmentId);

    // Cập nhật thông tin lớp học
    Response updateClass(Long classId, String className, String description);

    // Lấy danh sách tất cả lớp học
    Response getAllClasses();

    // Lấy danh sách lớp học theo department
    Response getClassesByDepartment(Long departmentId);

    // Lấy thông tin lớp học theo ID
    Response getClassById(Long classId);

    // Xóa lớp học (soft delete)
    Response deleteClass(Long classId);

    // Lấy danh sách sinh viên trong lớp
    Response getStudentsInClass(Long classId);

    // Lấy danh sách sinh viên trong lớp (có phân trang)
    Response getStudentsInClass(Long classId, Pageable pageable);


    // Thêm sinh viên vào lớp
    Response addStudentToClass(Long classId, Long studentId);

    // Xóa sinh viên khỏi lớp
    Response removeStudentFromClass(Long classId, Long studentId);

    // Lấy thông tin lớp học theo tên
    Response getClassByName(String className);
}
