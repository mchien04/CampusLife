package vn.campuslife.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import vn.campuslife.enumeration.Gender;

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
    private Long classId;
    private String className;
    private Long departmentId;
    private String departmentName;
    private String phone;
    private String address; // Full address string for display
    private LocalDate dob;
    private String avatarUrl;
    private Gender gender;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isProfileComplete;
}
