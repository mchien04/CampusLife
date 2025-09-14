package vn.campuslife.service;

public interface StudentService {

    /**
     * Lấy Student ID theo username
     */
    Long getStudentIdByUsername(String username);

    /**
     * Lấy Student ID theo User ID
     */
    Long getStudentIdByUserId(Long userId);
}
