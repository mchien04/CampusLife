package vn.campuslife.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileUpdateRequest {

    @NotBlank(message = "Student code is required")
    private String studentCode;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String className;

    private Long departmentId;

    private String phone;

    private String address;

    private LocalDate dob;

    private String avatarUrl;
}
