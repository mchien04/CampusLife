package vn.campuslife.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import vn.campuslife.enumeration.Gender;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileUpdateRequest {

    @NotBlank(message = "Student code is required")
    private String studentCode;

    @NotBlank(message = "Full name is required")
    private String fullName;

    // className is now handled through StudentClass entity
    // private String className;

    private Long departmentId;

    private Long classId;

    private String phone;

    // Address is now handled separately through Address entity
    // private String address;

    private LocalDate dob;

    private String avatarUrl;

    private Gender gender;

    private String email;
}
