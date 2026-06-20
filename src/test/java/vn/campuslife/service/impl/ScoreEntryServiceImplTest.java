package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.ScoreEntrySourceType;
import vn.campuslife.enumeration.ScoreEntryStatus;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.score.ScoreEntryCommand;
import vn.campuslife.repository.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScoreEntryServiceImplTest {

    @Mock
    private ScoreEntryRepository scoreEntryRepository;

    @Mock
    private StudentScoreRepository studentScoreRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityScoreRuleRepository ruleRepository;

    @InjectMocks
    private ScoreEntryServiceImpl scoreEntryService;

    private ScoreEntryCommand command;
    private Student student;
    private Semester semester;
    private Activity activity;
    private ActivityScoreRule rule;
    private User actor;

    @BeforeEach
    void setUp() {
        actor = new User();
        actor.setId(1L);

        student = new Student();
        student.setId(10L);

        semester = new Semester();
        semester.setId(200L);

        activity = new Activity();
        activity.setId(100L);

        rule = new ActivityScoreRule();
        rule.setId(150L);

        command = ScoreEntryCommand.builder()
                .studentId(10L)
                .semesterId(200L)
                .activityId(100L)
                .ruleId(150L)
                .scoreType(ScoreType.REN_LUYEN)
                .sourceType(ScoreEntrySourceType.ACTIVITY_PARTICIPATION)
                .sourceId(500L)
                .points(BigDecimal.valueOf(10))
                .reason("Completed Test Activity")
                .actor(actor)
                .build();
    }

    @Test
    void upsertEntry_NoExistingEntry_CreatesNewEntry() {
        when(scoreEntryRepository.findByStudentIdAndSourceTypeAndSourceIdAndRuleIdAndStatus(
                10L, ScoreEntrySourceType.ACTIVITY_PARTICIPATION, 500L, 150L, ScoreEntryStatus.ACTIVE))
                .thenReturn(Optional.empty());

        when(studentRepository.getReferenceById(10L)).thenReturn(student);
        when(semesterRepository.getReferenceById(200L)).thenReturn(semester);
        when(activityRepository.getReferenceById(100L)).thenReturn(activity);
        when(ruleRepository.getReferenceById(150L)).thenReturn(rule);

        when(scoreEntryRepository.sumPointsByStudentAndSemesterAndScoreTypeAndStatus(
                10L, 200L, ScoreType.REN_LUYEN, ScoreEntryStatus.ACTIVE))
                .thenReturn(BigDecimal.valueOf(10));

        when(studentScoreRepository.findByStudentIdAndSemesterIdAndScoreType(10L, 200L, ScoreType.REN_LUYEN))
                .thenReturn(Optional.empty());

        ScoreEntry result = scoreEntryService.upsertEntry(command);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(10), result.getPoints());
        assertEquals(ScoreEntryStatus.ACTIVE, result.getStatus());
        assertEquals("Completed Test Activity", result.getReason());

        verify(scoreEntryRepository).save(result);
        verify(studentScoreRepository).save(any(StudentScore.class));
    }

    @Test
    void upsertEntry_ExistingEntryWithSamePoints_ReturnsExistingWithoutSaving() {
        ScoreEntry existing = new ScoreEntry();
        existing.setId(88L);
        existing.setPoints(BigDecimal.valueOf(10));

        when(scoreEntryRepository.findByStudentIdAndSourceTypeAndSourceIdAndRuleIdAndStatus(
                10L, ScoreEntrySourceType.ACTIVITY_PARTICIPATION, 500L, 150L, ScoreEntryStatus.ACTIVE))
                .thenReturn(Optional.of(existing));

        ScoreEntry result = scoreEntryService.upsertEntry(command);

        assertEquals(existing, result);
        verify(scoreEntryRepository, never()).save(any());
        verifyNoInteractions(studentScoreRepository);
    }

    @Test
    void upsertEntry_ExistingEntryWithDifferentPoints_UpdatesPointsAndRefreshesScore() {
        ScoreEntry existing = new ScoreEntry();
        existing.setId(88L);
        existing.setPoints(BigDecimal.valueOf(5)); // different

        when(scoreEntryRepository.findByStudentIdAndSourceTypeAndSourceIdAndRuleIdAndStatus(
                10L, ScoreEntrySourceType.ACTIVITY_PARTICIPATION, 500L, 150L, ScoreEntryStatus.ACTIVE))
                .thenReturn(Optional.of(existing));

        when(scoreEntryRepository.sumPointsByStudentAndSemesterAndScoreTypeAndStatus(
                10L, 200L, ScoreType.REN_LUYEN, ScoreEntryStatus.ACTIVE))
                .thenReturn(BigDecimal.valueOf(10));

        StudentScore studentScore = new StudentScore();
        when(studentScoreRepository.findByStudentIdAndSemesterIdAndScoreType(10L, 200L, ScoreType.REN_LUYEN))
                .thenReturn(Optional.of(studentScore));

        ScoreEntry result = scoreEntryService.upsertEntry(command);

        assertEquals(existing, result);
        assertEquals(BigDecimal.valueOf(10), existing.getPoints());
        verify(scoreEntryRepository).save(existing);
        verify(studentScoreRepository).save(studentScore);
    }

    @Test
    void reverseEntries_ActiveEntriesFound_ReversesThemAndRefreshesScore() {
        ScoreEntry entry = new ScoreEntry();
        entry.setStudent(student);
        entry.setSemester(semester);
        entry.setScoreType(ScoreType.REN_LUYEN);
        entry.setStatus(ScoreEntryStatus.ACTIVE);

        when(scoreEntryRepository.findBySourceTypeAndSourceIdAndStatus(
                ScoreEntrySourceType.ACTIVITY_PARTICIPATION, 500L, ScoreEntryStatus.ACTIVE))
                .thenReturn(Collections.singletonList(entry));

        when(scoreEntryRepository.sumPointsByStudentAndSemesterAndScoreTypeAndStatus(
                10L, 200L, ScoreType.REN_LUYEN, ScoreEntryStatus.ACTIVE))
                .thenReturn(BigDecimal.ZERO);

        StudentScore studentScore = new StudentScore();
        when(studentScoreRepository.findByStudentIdAndSemesterIdAndScoreType(10L, 200L, ScoreType.REN_LUYEN))
                .thenReturn(Optional.of(studentScore));

        scoreEntryService.reverseEntries(ScoreEntrySourceType.ACTIVITY_PARTICIPATION, 500L, "Reversed due to cancel", actor);

        assertEquals(ScoreEntryStatus.REVERSED, entry.getStatus());
        assertEquals("Reversed due to cancel", entry.getReason());
        assertEquals(actor, entry.getCreatedBy());

        verify(scoreEntryRepository).save(entry);
        verify(studentScoreRepository).save(studentScore);
        assertEquals(BigDecimal.ZERO, studentScore.getScore());
    }
}
