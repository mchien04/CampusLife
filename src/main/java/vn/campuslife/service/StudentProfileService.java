package vn.campuslife.service;

import org.springframework.web.multipart.MultipartFile;
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
     * Upload / replace student avatar (local uploads or R2 depending on provider).
     */
    Response uploadStudentAvatar(Long studentId, MultipartFile file);

    /**
     * Lấy thông tin profile của student
     */
    Response getStudentProfile(Long studentId);

    /**
     * Lấy thông tin profile theo username
     */
    Response getStudentProfileByUsername(String username);
}
