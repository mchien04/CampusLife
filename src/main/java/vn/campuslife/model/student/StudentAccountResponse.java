package vn.campuslife.model.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response cho tài khoản sinh viên (để review)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAccountResponse {
    private Long userId;
    private Long studentId;
    private String username;
    private String email;
    private String studentCode;
    private String fullName;
    private String password;        // Plain password (chỉ hiển thị khi chưa gửi email)
    private Boolean isActivated;
    private Boolean emailSent;       // Đã gửi email chưa
    private LocalDateTime createdAt;
}

