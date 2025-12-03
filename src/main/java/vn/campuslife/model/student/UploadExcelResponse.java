package vn.campuslife.model.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response sau khi upload Excel
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadExcelResponse {
    private Integer totalRows;                          // Tổng số dòng
    private List<ExcelStudentRow> validRows;           // Các dòng hợp lệ
    private List<ExcelStudentRow> invalidRows;          // Các dòng không hợp lệ
    private Map<Integer, String> errors;               // Lỗi theo dòng số (key = row number, value = error message)
}

