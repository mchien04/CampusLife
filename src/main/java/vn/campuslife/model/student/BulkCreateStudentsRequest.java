package vn.campuslife.model.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request để tạo tài khoản từ danh sách
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateStudentsRequest {
    private List<ExcelStudentRow> students; // Danh sách sinh viên
}

