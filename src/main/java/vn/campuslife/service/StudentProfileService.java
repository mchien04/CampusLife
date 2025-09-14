package vn.campuslife.service;

import vn.campuslife.model.Response;
import vn.campuslife.model.StudentProfileUpdateRequest;

public interface StudentProfileService {

    /**
     * Tự động tạo Student record khi user register với role STUDENT
     */
    Response createStudentProfile(Long userId);

    /**
     * Cập nhật thông tin profile của student
     */
    Response updateStudentProfile(Long studentId, StudentProfileUpdateRequest request);

    /**
     * Lấy thông tin profile của student
     */
    Response getStudentProfile(Long studentId);

    /**
     * Lấy thông tin profile theo username
     */
    Response getStudentProfileByUsername(String username);
}
