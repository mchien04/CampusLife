package vn.campuslife.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.entity.Student;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
    private Long id;
    private String studentCode;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dob;
    private String avatarUrl;
    private String departmentName;
    private String className;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;



    public static StudentResponse fromEntity(Student student) {
        StudentResponse response = new StudentResponse();
        response.setId(student.getId());
        response.setStudentCode(student.getStudentCode());
        response.setFullName(student.getFullName());
        response.setEmail(student.getUser().getEmail());
        response.setPhone(student.getPhone());
        response.setDob(student.getDob());
        response.setAvatarUrl(student.getAvatarUrl());

        // Department info
        if (student.getDepartment() != null) {
            response.setDepartmentName(student.getDepartment().getName());
        }

        // Class info
        if (student.getStudentClass() != null) {
            response.setClassName(student.getStudentClass().getClassName());
        }

        // Address info
        if (student.getAddress() != null) {
            response.setAddress(student.getAddress().getStreet() + ", " +
                    student.getAddress().getWardName() + ", " +
                    student.getAddress().getProvinceName());
        }

        response.setCreatedAt(student.getCreatedAt());
        response.setUpdatedAt(student.getUpdatedAt());

        return response;
    }
}