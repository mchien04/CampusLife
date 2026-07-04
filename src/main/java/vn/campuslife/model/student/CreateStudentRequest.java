package vn.campuslife.model.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request để tạo tài khoản sinh viên đơn lẻ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudentRequest {
    private String studentCode; // Mã số sinh viên
    private String fullName;    // Họ tên
    private String email;       // Email
}
