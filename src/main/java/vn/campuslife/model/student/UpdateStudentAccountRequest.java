package vn.campuslife.model.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request để chỉnh sửa thông tin tài khoản
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentAccountRequest {
    private String username;     // Optional
    private String email;        // Optional
    private String studentCode;  // Optional
    private String fullName;     // Optional
}

