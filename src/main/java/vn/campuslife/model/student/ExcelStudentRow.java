package vn.campuslife.model.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho một dòng trong Excel
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelStudentRow {
    private String studentCode; // Mã số sinh viên
    private String fullName;    // Họ tên
    private String email;       // Email
}

