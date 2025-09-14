package vn.campuslife.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileResponse {

    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String studentCode;
    private String fullName;
    private String className;
    private Long departmentId;
    private String departmentName;
    private String phone;
    private String address;
    private LocalDate dob;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isProfileComplete;
}
