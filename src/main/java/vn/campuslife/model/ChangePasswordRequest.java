package vn.campuslife.model;

import lombok.Data;

/**
 * Request DTO cho chức năng đổi mật khẩu
 * User phải nhập mật khẩu cũ, mật khẩu mới và xác nhận mật khẩu mới
 */
@Data
public class ChangePasswordRequest {
    private String oldPassword;      // Mật khẩu cũ
    private String newPassword;      // Mật khẩu mới
    private String confirmPassword;  // Xác nhận mật khẩu mới
}

