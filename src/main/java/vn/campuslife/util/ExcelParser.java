package vn.campuslife.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.model.student.ExcelStudentRow;

import java.io.InputStream;
import java.util.*;

/**
 * Utility để parse file Excel
 */
@Component
public class ExcelParser {
    
    /**
     * Parse file Excel và trả về danh sách ExcelStudentRow
     * Hỗ trợ cả file có header và không có header
     * 
     * @param file File Excel (.xlsx)
     * @return Danh sách ExcelStudentRow
     * @throws Exception Nếu có lỗi khi parse
     */
    public List<ExcelStudentRow> parseStudentExcel(MultipartFile file) throws Exception {
        List<ExcelStudentRow> rows = new ArrayList<>();
        
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên
            
            if (sheet.getPhysicalNumberOfRows() == 0) {
                return rows; // File rỗng
            }
            
            // Kiểm tra dòng đầu tiên có phải header không
            Row firstRow = sheet.getRow(0);
            boolean hasHeader = isHeaderRow(firstRow);
            
            int startRow = hasHeader ? 1 : 0; // Bỏ qua header nếu có
            
            // Tìm vị trí các cột (studentCode, fullName, email)
            int[] columnIndices = findColumnIndices(firstRow, hasHeader);
            int studentCodeCol = columnIndices[0];
            int fullNameCol = columnIndices[1];
            int emailCol = columnIndices[2];
            
            // Parse từng dòng
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                
                String studentCode = getCellValueAsString(row.getCell(studentCodeCol));
                String fullName = getCellValueAsString(row.getCell(fullNameCol));
                String email = getCellValueAsString(row.getCell(emailCol));
                
                // Bỏ qua dòng trống
                if (studentCode == null && fullName == null && email == null) {
                    continue;
                }
                
                ExcelStudentRow studentRow = new ExcelStudentRow();
                studentRow.setStudentCode(studentCode != null ? studentCode.trim() : null);
                studentRow.setFullName(fullName != null ? fullName.trim() : null);
                studentRow.setEmail(email != null ? email.trim() : null);
                
                rows.add(studentRow);
            }
        }
        
        return rows;
    }
    
    /**
     * Kiểm tra dòng đầu tiên có phải header không
     */
    private boolean isHeaderRow(Row row) {
        if (row == null) {
            return false;
        }
        
        // Kiểm tra các từ khóa header
        String[] headerKeywords = {"mã", "họ tên", "email", "mssv", "student code", "full name", "tên"};
        
        for (int i = 0; i < Math.min(3, row.getLastCellNum()); i++) {
            Cell cell = row.getCell(i);
            if (cell != null) {
                String cellValue = getCellValueAsString(cell).toLowerCase();
                for (String keyword : headerKeywords) {
                    if (cellValue.contains(keyword)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Tìm vị trí các cột (studentCode, fullName, email)
     * Trả về mảng [studentCodeIndex, fullNameIndex, emailIndex]
     */
    private int[] findColumnIndices(Row firstRow, boolean hasHeader) {
        int[] indices = new int[3];
        Arrays.fill(indices, -1);
        
        // Nếu có header, tìm theo tên cột
        if (hasHeader) {
            for (int i = 0; i < firstRow.getLastCellNum(); i++) {
                Cell cell = firstRow.getCell(i);
                if (cell == null) continue;
                
                String cellValue = getCellValueAsString(cell).toLowerCase();
                
                if (indices[0] == -1 && (cellValue.contains("mã") || cellValue.contains("mssv") || cellValue.contains("student code"))) {
                    indices[0] = i; // studentCode
                } else if (indices[1] == -1 && (cellValue.contains("họ tên") || cellValue.contains("full name") || cellValue.contains("tên"))) {
                    indices[1] = i; // fullName
                } else if (indices[2] == -1 && cellValue.contains("email")) {
                    indices[2] = i; // email
                }
            }
        }
        
        // Nếu không tìm thấy hoặc không có header, dùng thứ tự mặc định: A, B, C
        if (indices[0] == -1) indices[0] = 0; // Cột A
        if (indices[1] == -1) indices[1] = 1; // Cột B
        if (indices[2] == -1) indices[2] = 2; // Cột C
        
        return indices;
    }
    
    /**
     * Lấy giá trị cell dưới dạng String
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Format số nguyên không có phần thập phân
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
}

