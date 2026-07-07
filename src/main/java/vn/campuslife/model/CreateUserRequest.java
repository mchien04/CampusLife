package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.Role;

import java.util.List;

@Data
public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
    private Role role; // ADMIN hoặc MANAGER
    private Boolean isActivated; // Có thể null, mặc định false
    /** Bắt buộc khi role = MANAGER: danh sách department_id manager được quản lý */
    private List<Long> departmentIds;
}

