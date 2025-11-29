package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.Role;

@Data
public class UpdateUserRequest {
    private String username;
    private String email;
    private String password; // Có thể null nếu không muốn đổi mật khẩu
    private Role role; // ADMIN hoặc MANAGER
    private Boolean isActivated; // Có thể null
}

