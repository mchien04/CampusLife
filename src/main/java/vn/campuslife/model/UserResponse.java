package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.Role;

import java.time.LocalDateTime;
import java.util.List;

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
    /** Khoa được phân công (chỉ có ý nghĩa với role MANAGER) */
    private List<Long> departmentIds;
}

