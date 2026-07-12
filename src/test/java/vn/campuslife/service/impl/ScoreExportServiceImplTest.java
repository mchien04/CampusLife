package vn.campuslife.service.impl;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.campuslife.entity.Department;
import vn.campuslife.entity.Semester;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.StudentClass;
import vn.campuslife.entity.StudentScore;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.repository.SemesterRepository;
import vn.campuslife.repository.StudentScoreRepository;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.service.ScoreExportService;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreExportServiceImplTest {

    @Mock
    private SemesterRepository semesterRepository;
    @Mock
    private StudentScoreRepository studentScoreRepository;
    @Mock
    private DepartmentAuthorizationService departmentAuthorizationService;

    @InjectMocks
    private ScoreExportServiceImpl scoreExportService;

    private Semester semester;
    private Student student;

    @BeforeEach
    void setUp() {
        semester = new Semester();
        semester.setId(1L);
        semester.setName("HK1 2025-2026");

        Department dept = new Department();
        dept.setId(10L);
        dept.setName("CNTT");

        StudentClass clazz = new StudentClass();
        clazz.setId(20L);
        clazz.setClassName("D21CQCN01");

        student = new Student();
        student.setId(100L);
        student.setStudentCode("SV001");
        student.setFullName("Nguyen Van A");
        student.setDeleted(false);
        student.setDepartment(dept);
        student.setStudentClass(clazz);
    }

    @Test
    void exportSemesterScores_buildsWorkbookWithTotals() throws Exception {
        StudentScore rl = score(ScoreType.REN_LUYEN, "5");
        StudentScore ctxh = score(ScoreType.CONG_TAC_XA_HOI, "2");

        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(studentScoreRepository.findBySemesterIdOrderByScoreDesc(1L)).thenReturn(List.of(rl, ctxh));

        ScoreExportService.ExportFile file = scoreExportService.exportSemesterScores(1L, null, null, null);

        assertThat(file.contentType()).contains("spreadsheetml");
        assertThat(file.filename()).startsWith("bang_diem_hk1_");
        assertThat(file.bytes()).isNotEmpty();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(file.bytes()))) {
            Sheet sheet = wb.getSheet("BangDiem");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(7).getCell(1).getStringCellValue()).isEqualTo("SV001");
            assertThat(sheet.getRow(7).getCell(5).getNumericCellValue()).isEqualTo(5.0);
            assertThat(sheet.getRow(7).getCell(6).getNumericCellValue()).isEqualTo(2.0);
            assertThat(sheet.getRow(7).getCell(8).getNumericCellValue()).isEqualTo(7.0);
        }
    }

    @Test
    void exportSemesterScores_filtersByClass() throws Exception {
        Student other = new Student();
        other.setId(101L);
        other.setStudentCode("SV002");
        other.setFullName("Tran B");
        other.setDeleted(false);
        StudentClass otherClass = new StudentClass();
        otherClass.setId(99L);
        otherClass.setClassName("OTHER");
        other.setStudentClass(otherClass);
        Department dept = new Department();
        dept.setId(10L);
        dept.setName("CNTT");
        other.setDepartment(dept);

        StudentScore inClass = score(ScoreType.REN_LUYEN, "5");
        StudentScore outClass = new StudentScore();
        outClass.setStudent(other);
        outClass.setSemester(semester);
        outClass.setScoreType(ScoreType.REN_LUYEN);
        outClass.setScore(new BigDecimal("9"));

        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(studentScoreRepository.findBySemesterIdOrderByScoreDesc(1L))
                .thenReturn(List.of(inClass, outClass));

        ScoreExportService.ExportFile file =
                scoreExportService.exportSemesterScores(1L, null, 20L, ScoreType.REN_LUYEN);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(file.bytes()))) {
            Sheet sheet = wb.getSheet("BangDiem");
            assertThat(sheet.getRow(7).getCell(1).getStringCellValue()).isEqualTo("SV001");
            assertThat(sheet.getRow(8)).isNull();
        }
    }

    private StudentScore score(ScoreType type, String points) {
        StudentScore ss = new StudentScore();
        ss.setStudent(student);
        ss.setSemester(semester);
        ss.setScoreType(type);
        ss.setScore(new BigDecimal(points));
        return ss;
    }
}
