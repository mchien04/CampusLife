package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.Role;

import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private Boolean isActivated;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
}

