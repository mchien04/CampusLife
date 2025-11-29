package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.Role;

@Data
public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
    private Role role; // ADMIN hoặc MANAGER
    private Boolean isActivated; // Có thể null, mặc định false
}

