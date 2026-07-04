package vn.campuslife.model.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request để tạo tài khoản sinh viên từ danh sách (không qua Excel)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMultipleStudentsRequest {
    private List<CreateStudentRequest> students; // Danh sách sinh viên cần tạo
}
