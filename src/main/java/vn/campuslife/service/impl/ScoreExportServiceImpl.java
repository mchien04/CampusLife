package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Semester;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.StudentScore;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.exception.ResourceNotFoundException;
import vn.campuslife.repository.SemesterRepository;
import vn.campuslife.repository.StudentScoreRepository;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.service.ScoreExportService;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScoreExportServiceImpl implements ScoreExportService {

    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final SemesterRepository semesterRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final DepartmentAuthorizationService departmentAuthorizationService;

    @Override
    @Transactional(readOnly = true)
    public ExportFile exportSemesterScores(Long semesterId, Long departmentId, Long classId, ScoreType scoreType) {
        return exportSemesterScores(semesterId, departmentId, classId, scoreType, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportFile exportSemesterScores(Long semesterId, Long departmentId, Long classId, ScoreType scoreType,
                                           DepartmentScope scope) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));

        Set<Long> managerDeptFilter = null;
        if (scope != null && scope.manager() && !scope.admin()) {
            managerDeptFilter = departmentAuthorizationService.managerDepartmentFilter(scope, departmentId);
            if (classId != null) {
                departmentAuthorizationService.requireStudentClassAccess(classId, scope);
            }
            if (managerDeptFilter.size() == 1) {
                departmentId = managerDeptFilter.iterator().next();
            } else {
                departmentId = null;
            }
        }

        List<StudentScore> scores = studentScoreRepository.findBySemesterIdOrderByScoreDesc(semesterId);
        Long effectiveDepartmentId = departmentId;
        Long effectiveClassId = classId;
        Set<Long> allowedDeptIds = managerDeptFilter;

        Map<Long, ScoreRow> byStudent = new LinkedHashMap<>();
        for (StudentScore ss : scores) {
            Student student = ss.getStudent();
            if (student == null || student.isDeleted()) {
                continue;
            }
            if (allowedDeptIds != null && allowedDeptIds.size() > 1) {
                if (student.getDepartment() == null || !allowedDeptIds.contains(student.getDepartment().getId())) {
                    continue;
                }
            }
            if (effectiveDepartmentId != null
                    && (student.getDepartment() == null
                    || !effectiveDepartmentId.equals(student.getDepartment().getId()))) {
                continue;
            }
            if (effectiveClassId != null
                    && (student.getStudentClass() == null
                    || !effectiveClassId.equals(student.getStudentClass().getId()))) {
                continue;
            }
            if (scoreType != null && ss.getScoreType() != scoreType) {
                continue;
            }

            ScoreRow row = byStudent.computeIfAbsent(student.getId(), id -> ScoreRow.from(student));
            BigDecimal points = ss.getScore() != null ? ss.getScore() : BigDecimal.ZERO;
            if (ss.getScoreType() != null) {
                row.scoresByType.merge(ss.getScoreType(), points, BigDecimal::add);
            }
        }

        List<ScoreRow> rows = new ArrayList<>(byStudent.values());
        rows.sort(Comparator
                .comparing(ScoreRow::total, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(r -> r.studentCode == null ? "" : r.studentCode));

        byte[] bytes = buildWorkbook(semester, rows, scoreType, effectiveDepartmentId, effectiveClassId);
        String filename = "bang_diem_hk" + semesterId + "_" + LocalDateTime.now().format(TS) + ".xlsx";
        return new ExportFile(filename, XLSX, bytes);
    }

    private byte[] buildWorkbook(Semester semester, List<ScoreRow> rows, ScoreType scoreType,
                                 Long departmentId, Long classId) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("BangDiem");
            CellStyle headerStyle = headerStyle(wb);

            int r = 0;
            Row meta1 = sheet.createRow(r++);
            meta1.createCell(0).setCellValue("Học kỳ");
            meta1.createCell(1).setCellValue(semester.getName() != null ? semester.getName() : "");
            Row meta2 = sheet.createRow(r++);
            meta2.createCell(0).setCellValue("Loại điểm");
            meta2.createCell(1).setCellValue(scoreType != null ? scoreTypeLabel(scoreType) : "Tất cả / Tổng điểm");
            Row meta3 = sheet.createRow(r++);
            meta3.createCell(0).setCellValue("Khoa ID");
            meta3.createCell(1).setCellValue(departmentId != null ? departmentId.toString() : "Tất cả");
            Row meta4 = sheet.createRow(r++);
            meta4.createCell(0).setCellValue("Lớp ID");
            meta4.createCell(1).setCellValue(classId != null ? classId.toString() : "Tất cả");
            Row meta5 = sheet.createRow(r++);
            meta5.createCell(0).setCellValue("Số sinh viên");
            meta5.createCell(1).setCellValue(rows.size());
            r++;

            List<String> headers = new ArrayList<>();
            headers.add("STT");
            headers.add("MSSV");
            headers.add("Họ tên");
            headers.add("Khoa");
            headers.add("Lớp");
            if (scoreType == null) {
                headers.add("Điểm rèn luyện");
                headers.add("Điểm CTXH");
                headers.add("Điểm chuyên đề");
                headers.add("Tổng điểm");
            } else {
                headers.add(scoreTypeLabel(scoreType));
            }

            Row headerRow = sheet.createRow(r++);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            int stt = 1;
            for (ScoreRow row : rows) {
                Row excelRow = sheet.createRow(r++);
                int c = 0;
                excelRow.createCell(c++).setCellValue(stt++);
                excelRow.createCell(c++).setCellValue(nullToEmpty(row.studentCode));
                excelRow.createCell(c++).setCellValue(nullToEmpty(row.studentName));
                excelRow.createCell(c++).setCellValue(nullToEmpty(row.departmentName));
                excelRow.createCell(c++).setCellValue(nullToEmpty(row.className));
                if (scoreType == null) {
                    setDecimal(excelRow.createCell(c++), row.scoreOf(ScoreType.REN_LUYEN));
                    setDecimal(excelRow.createCell(c++), row.scoreOf(ScoreType.CONG_TAC_XA_HOI));
                    setDecimal(excelRow.createCell(c++), row.scoreOf(ScoreType.CHUYEN_DE));
                    setDecimal(excelRow.createCell(c), row.total());
                } else {
                    setDecimal(excelRow.createCell(c), row.scoreOf(scoreType));
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export semester scores: " + e.getMessage(), e);
        }
    }

    private static CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static void setDecimal(Cell cell, BigDecimal value) {
        cell.setCellValue(value != null ? value.doubleValue() : 0d);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String scoreTypeLabel(ScoreType scoreType) {
        return switch (scoreType) {
            case REN_LUYEN -> "Điểm rèn luyện";
            case CONG_TAC_XA_HOI -> "Điểm công tác xã hội";
            case CHUYEN_DE -> "Điểm chuyên đề";
        };
    }

    private static final class ScoreRow {
        private final String studentCode;
        private final String studentName;
        private final String departmentName;
        private final String className;
        private final Map<ScoreType, BigDecimal> scoresByType = new HashMap<>();

        private ScoreRow(String studentCode, String studentName, String departmentName, String className) {
            this.studentCode = studentCode;
            this.studentName = studentName;
            this.departmentName = departmentName;
            this.className = className;
        }

        static ScoreRow from(Student student) {
            return new ScoreRow(
                    student.getStudentCode(),
                    student.getFullName(),
                    student.getDepartment() != null ? student.getDepartment().getName() : null,
                    student.getStudentClass() != null ? student.getStudentClass().getClassName() : null);
        }

        BigDecimal scoreOf(ScoreType type) {
            return scoresByType.getOrDefault(type, BigDecimal.ZERO);
        }

        BigDecimal total() {
            return scoresByType.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
