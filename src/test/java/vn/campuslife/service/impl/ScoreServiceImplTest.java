package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.campuslife.entity.Semester;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.StudentScore;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.Response;
import vn.campuslife.model.score.ScoreViewResponse;
import vn.campuslife.repository.*;
import vn.campuslife.service.ScoreEntryService;
import vn.campuslife.service.SemesterHelperService;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScoreServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private StudentScoreRepository studentScoreRepository;

    @Mock
    private ActivityParticipationRepository participationRepository;

    @Mock
    private StudentSeriesProgressRepository progressRepository;

    @Mock
    private ScoreEntryRepository scoreEntryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivitySeriesRepository seriesRepository;

    @Mock
    private SemesterHelperService semesterHelperService;

    @Mock
    private ScoreEntryService scoreEntryService;

    @InjectMocks
    private ScoreServiceImpl scoreService;

    private Student student;
    private Semester semester;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        student.setStudentCode("SV001");
        student.setFullName("Nguyen Van A");
        student.setDeleted(false);

        semester = new Semester();
        semester.setId(1L);
        semester.setName("HK1 2025-2026");
    }

    @Test
    void viewScores_ReturnsCumulativeTotal_ForCumulativeTypes() {
        // Given: CTXH=10, CHUYEN_DE=2 trong học kỳ hiện tại
        StudentScore ctxhScore = new StudentScore();
        ctxhScore.setStudent(student);
        ctxhScore.setSemester(semester);
        ctxhScore.setScoreType(ScoreType.CONG_TAC_XA_HOI);
        ctxhScore.setScore(BigDecimal.valueOf(10));

        StudentScore cdScore = new StudentScore();
        cdScore.setStudent(student);
        cdScore.setSemester(semester);
        cdScore.setScoreType(ScoreType.CHUYEN_DE);
        cdScore.setScore(BigDecimal.valueOf(2));

        when(studentScoreRepository.findByStudentAndSemester(1L, 1L))
                .thenReturn(List.of(ctxhScore, cdScore));

        // Tổng tích lũy cross-semester
        when(studentScoreRepository.sumScoreByStudentIdAndScoreType(1L, ScoreType.CONG_TAC_XA_HOI))
                .thenReturn(BigDecimal.valueOf(45));
        when(studentScoreRepository.sumScoreByStudentIdAndScoreType(1L, ScoreType.CHUYEN_DE))
                .thenReturn(BigDecimal.valueOf(8));

        // When
        Response response = scoreService.viewScores(1L, 1L);

        // Then
        assertTrue(response.isStatus());
        ScoreViewResponse view = (ScoreViewResponse) response.getBody();
        assertNotNull(view);
        assertEquals(2, view.getSummaries().size());

        ScoreViewResponse.ScoreTypeSummary ctxhSummary = view.getSummaries().stream()
                .filter(s -> s.getScoreType() == ScoreType.CONG_TAC_XA_HOI)
                .findFirst()
                .orElseThrow();
        assertEquals(BigDecimal.valueOf(10), ctxhSummary.getTotal());
        assertEquals(BigDecimal.valueOf(45), ctxhSummary.getCumulativeTotal());

        ScoreViewResponse.ScoreTypeSummary cdSummary = view.getSummaries().stream()
                .filter(s -> s.getScoreType() == ScoreType.CHUYEN_DE)
                .findFirst()
                .orElseThrow();
        assertEquals(BigDecimal.valueOf(2), cdSummary.getTotal());
        assertEquals(BigDecimal.valueOf(8), cdSummary.getCumulativeTotal());
    }

    @Test
    void viewScores_ReturnsNullCumulativeTotal_ForNonCumulativeType() {
        // Given: chỉ có REN_LUYEN
        StudentScore rlScore = new StudentScore();
        rlScore.setStudent(student);
        rlScore.setSemester(semester);
        rlScore.setScoreType(ScoreType.REN_LUYEN);
        rlScore.setScore(BigDecimal.valueOf(85));

        when(studentScoreRepository.findByStudentAndSemester(1L, 1L))
                .thenReturn(List.of(rlScore));

        // When
        Response response = scoreService.viewScores(1L, 1L);

        // Then
        assertTrue(response.isStatus());
        ScoreViewResponse view = (ScoreViewResponse) response.getBody();
        assertNotNull(view);
        assertEquals(1, view.getSummaries().size());

        ScoreViewResponse.ScoreTypeSummary rlSummary = view.getSummaries().get(0);
        assertEquals(ScoreType.REN_LUYEN, rlSummary.getScoreType());
        assertEquals(BigDecimal.valueOf(85), rlSummary.getTotal());
        assertNull(rlSummary.getCumulativeTotal());

        // Không gọi query tích lũy cho REN_LUYEN
        verify(studentScoreRepository, never()).sumScoreByStudentIdAndScoreType(anyLong(), eq(ScoreType.REN_LUYEN));
    }

    @Test
    void getTotalScore_ReturnsCumulativeTotalsMap() {
        // Given: 3 loại điểm trong học kỳ
        StudentScore rlScore = new StudentScore();
        rlScore.setScoreType(ScoreType.REN_LUYEN);
        rlScore.setScore(BigDecimal.valueOf(85));

        StudentScore ctxhScore = new StudentScore();
        ctxhScore.setScoreType(ScoreType.CONG_TAC_XA_HOI);
        ctxhScore.setScore(BigDecimal.valueOf(10));

        StudentScore cdScore = new StudentScore();
        cdScore.setScoreType(ScoreType.CHUYEN_DE);
        cdScore.setScore(BigDecimal.valueOf(2));

        when(studentScoreRepository.findByStudentAndSemester(1L, 1L))
                .thenReturn(List.of(rlScore, ctxhScore, cdScore));

        when(studentScoreRepository.sumScoreByStudentIdAndScoreType(1L, ScoreType.CONG_TAC_XA_HOI))
                .thenReturn(BigDecimal.valueOf(45));
        when(studentScoreRepository.sumScoreByStudentIdAndScoreType(1L, ScoreType.CHUYEN_DE))
                .thenReturn(BigDecimal.valueOf(8));

        // When
        Response response = scoreService.getTotalScore(1L, 1L);

        // Then
        assertTrue(response.isStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertNotNull(result);

        @SuppressWarnings("unchecked")
        Map<ScoreType, BigDecimal> totalsByType = (Map<ScoreType, BigDecimal>) result.get("totalsByType");
        assertEquals(BigDecimal.valueOf(85), totalsByType.get(ScoreType.REN_LUYEN));
        assertEquals(BigDecimal.valueOf(10), totalsByType.get(ScoreType.CONG_TAC_XA_HOI));
        assertEquals(BigDecimal.valueOf(2), totalsByType.get(ScoreType.CHUYEN_DE));

        @SuppressWarnings("unchecked")
        Map<ScoreType, BigDecimal> cumulativeTotals = (Map<ScoreType, BigDecimal>) result.get("cumulativeTotals");
        assertNotNull(cumulativeTotals);
        assertEquals(2, cumulativeTotals.size());
        assertEquals(BigDecimal.valueOf(45), cumulativeTotals.get(ScoreType.CONG_TAC_XA_HOI));
        assertEquals(BigDecimal.valueOf(8), cumulativeTotals.get(ScoreType.CHUYEN_DE));
        assertNull(cumulativeTotals.get(ScoreType.REN_LUYEN));

        assertEquals(BigDecimal.valueOf(97), result.get("grandTotal"));
    }
}
