package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.*;
import vn.campuslife.model.score.ScoreEntryCommand;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.SemesterRepository;
import vn.campuslife.repository.StudentSeriesProgressRepository;
import vn.campuslife.service.ActivityScoreRuleService;
import vn.campuslife.service.ScoreEntryService;
import vn.campuslife.service.ScoreSemesterResolver;
import vn.campuslife.service.SemesterHelperService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScoreRuleEngineImplTest {

    @Mock
    private ActivityScoreRuleService ruleService;

    @Mock
    private ScoreEntryService scoreEntryService;

    @Mock
    private ScoreSemesterResolver semesterResolver;

    @Mock
    private StudentSeriesProgressRepository progressRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private SemesterHelperService semesterHelperService;

    @Mock
    private SemesterRepository semesterRepository;

    @InjectMocks
    private ScoreRuleEngineImpl scoreRuleEngine;

    private User actor;
    private Student student;
    private Department studentDept;
    private Activity activity;
    private Semester semester;
    private ActivityRegistration registration;

    @BeforeEach
    void setUp() {
        actor = new User();
        actor.setId(1L);

        studentDept = new Department();
        studentDept.setId(5L);
        studentDept.setName("IT");

        student = new Student();
        student.setId(10L);
        student.setDepartment(studentDept);

        activity = new Activity();
        activity.setId(100L);
        activity.setName("Test Activity");

        semester = new Semester();
        semester.setId(200L);

        registration = new ActivityRegistration();
        registration.setStudent(student);
        registration.setActivity(activity);
    }

    @Test
    void applyActivityCompleted_StandaloneActivity_SuccessfulScoring() {
        ActivityParticipation participation = new ActivityParticipation();
        participation.setId(500L);
        participation.setIsCompleted(true);
        participation.setRegistration(registration);
        participation.setDate(LocalDateTime.now());

        ActivityScoreRule rule = new ActivityScoreRule();
        rule.setId(150L);
        rule.setPoints(BigDecimal.valueOf(10));
        rule.setScoreType(ScoreType.REN_LUYEN);
        rule.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);

        when(ruleService.getEnabledRules(activity.getId(), ScoreRuleTrigger.PARTICIPATION_COMPLETED))
                .thenReturn(Collections.singletonList(rule));
        when(semesterResolver.resolveSemester(eq(activity), eq(rule), any())).thenReturn(semester);

        scoreRuleEngine.applyActivityCompleted(participation, actor);

        ArgumentCaptor<ScoreEntryCommand> commandCaptor = ArgumentCaptor.forClass(ScoreEntryCommand.class);
        verify(scoreEntryService).upsertEntry(commandCaptor.capture());
        ScoreEntryCommand command = commandCaptor.getValue();

        assertEquals(student.getId(), command.getStudentId());
        assertEquals(activity.getId(), command.getActivityId());
        assertEquals(rule.getId(), command.getRuleId());
        assertEquals(semester.getId(), command.getSemesterId());
        assertEquals(ScoreType.REN_LUYEN, command.getScoreType());
        assertEquals(ScoreEntrySourceType.ACTIVITY_PARTICIPATION, command.getSourceType());
        assertEquals(participation.getId(), command.getSourceId());
        assertEquals(BigDecimal.valueOf(10), command.getPoints());
        assertEquals(actor, command.getActor());
    }

    @Test
    void applyActivityCompleted_ActivityInSeries_SkipsScoring() {
        activity.setSeriesId(999L); // Belongs to a series

        ActivityParticipation participation = new ActivityParticipation();
        participation.setIsCompleted(true);
        participation.setRegistration(registration);

        scoreRuleEngine.applyActivityCompleted(participation, actor);

        verifyNoInteractions(ruleService, scoreEntryService);
    }

    @Test
    void applyActivityCompleted_ParticipationNotCompleted_SkipsScoring() {
        ActivityParticipation participation = new ActivityParticipation();
        participation.setIsCompleted(false);
        participation.setRegistration(registration);

        when(ruleService.getEnabledRules(activity.getId(), ScoreRuleTrigger.PARTICIPATION_COMPLETED))
                .thenReturn(Collections.emptyList());

        scoreRuleEngine.applyActivityCompleted(participation, actor);

        verifyNoInteractions(scoreEntryService);
    }

    @Test
    void applyActivityCompleted_NotEligibleAudience_SkipsScoring() {
        ActivityParticipation participation = new ActivityParticipation();
        participation.setIsCompleted(true);
        participation.setRegistration(registration);

        Department otherDept = new Department();
        otherDept.setId(6L);
        otherDept.setName("Biz");

        ActivityScoreRule rule = new ActivityScoreRule();
        rule.setId(150L);
        rule.setAudience(ScoreRuleAudience.DEPARTMENT_ONLY);
        rule.setTargetDepartments(Collections.singleton(otherDept)); // student is "IT"

        when(ruleService.getEnabledRules(activity.getId(), ScoreRuleTrigger.PARTICIPATION_COMPLETED))
                .thenReturn(Collections.singletonList(rule));

        scoreRuleEngine.applyActivityCompleted(participation, actor);

        verifyNoInteractions(scoreEntryService);
    }

    @Test
    void applySubmissionGraded_Standalone_Completed_SuccessfulScoring() {
        ActivityTask task = new ActivityTask();
        task.setActivity(activity);

        TaskSubmission submission = new TaskSubmission();
        submission.setId(600L);
        submission.setTask(task);
        submission.setStudent(student);
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setIsCompleted(true);
        submission.setSubmittedAt(LocalDateTime.now());

        ActivityScoreRule rule = new ActivityScoreRule();
        rule.setId(151L);
        rule.setPoints(BigDecimal.valueOf(5));
        rule.setFailPoints(BigDecimal.valueOf(0));
        rule.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);

        when(ruleService.getEnabledRules(activity.getId(), ScoreRuleTrigger.SUBMISSION_GRADED))
                .thenReturn(Collections.singletonList(rule));
        when(semesterResolver.resolveSemester(eq(activity), eq(rule), any())).thenReturn(semester);

        scoreRuleEngine.applySubmissionGraded(submission, actor);

        ArgumentCaptor<ScoreEntryCommand> commandCaptor = ArgumentCaptor.forClass(ScoreEntryCommand.class);
        verify(scoreEntryService).upsertEntry(commandCaptor.capture());
        ScoreEntryCommand command = commandCaptor.getValue();

        assertEquals(BigDecimal.valueOf(5), command.getPoints());
        assertEquals(ScoreEntrySourceType.TASK_SUBMISSION, command.getSourceType());
    }

    @Test
    void applySubmissionGraded_Standalone_Failed_AwardsFailPoints() {
        ActivityTask task = new ActivityTask();
        task.setActivity(activity);

        TaskSubmission submission = new TaskSubmission();
        submission.setId(600L);
        submission.setTask(task);
        submission.setStudent(student);
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setIsCompleted(false); // FAILED
        submission.setSubmittedAt(LocalDateTime.now());

        ActivityScoreRule rule = new ActivityScoreRule();
        rule.setId(151L);
        rule.setPoints(BigDecimal.valueOf(5));
        rule.setFailPoints(BigDecimal.valueOf(1)); // 1 point for fail/incomplete
        rule.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);

        when(ruleService.getEnabledRules(activity.getId(), ScoreRuleTrigger.SUBMISSION_GRADED))
                .thenReturn(Collections.singletonList(rule));
        when(semesterResolver.resolveSemester(eq(activity), eq(rule), any())).thenReturn(semester);

        scoreRuleEngine.applySubmissionGraded(submission, actor);

        ArgumentCaptor<ScoreEntryCommand> commandCaptor = ArgumentCaptor.forClass(ScoreEntryCommand.class);
        verify(scoreEntryService).upsertEntry(commandCaptor.capture());
        ScoreEntryCommand command = commandCaptor.getValue();

        assertEquals(BigDecimal.valueOf(1), command.getPoints());
    }

    @Test
    void applySubmissionGraded_InSeries_SkipsScoring() {
        activity.setSeriesId(999L);
        ActivityTask task = new ActivityTask();
        task.setActivity(activity);

        TaskSubmission submission = new TaskSubmission();
        submission.setTask(task);

        scoreRuleEngine.applySubmissionGraded(submission, actor);

        verifyNoInteractions(ruleService, scoreEntryService);
    }

    @Test
    void applyMiniGamePassed_StandalonePassed_SuccessfulScoring() {
        MiniGame miniGame = new MiniGame();
        miniGame.setActivity(activity);

        MiniGameAttempt attempt = new MiniGameAttempt();
        attempt.setId(700L);
        attempt.setStatus(AttemptStatus.PASSED);
        attempt.setMiniGame(miniGame);
        attempt.setStudent(student);
        attempt.setSubmittedAt(LocalDateTime.now());

        ActivityScoreRule rule = new ActivityScoreRule();
        rule.setId(152L);
        rule.setPoints(BigDecimal.valueOf(8));
        rule.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);

        when(ruleService.getEnabledRules(activity.getId(), ScoreRuleTrigger.MINIGAME_PASSED))
                .thenReturn(Collections.singletonList(rule));
        when(semesterResolver.resolveSemester(eq(activity), eq(rule), any())).thenReturn(semester);

        scoreRuleEngine.applyMiniGamePassed(attempt, actor);

        ArgumentCaptor<ScoreEntryCommand> commandCaptor = ArgumentCaptor.forClass(ScoreEntryCommand.class);
        verify(scoreEntryService).upsertEntry(commandCaptor.capture());
        ScoreEntryCommand command = commandCaptor.getValue();

        assertEquals(BigDecimal.valueOf(8), command.getPoints());
        assertEquals(ScoreEntrySourceType.MINIGAME_ATTEMPT, command.getSourceType());
    }

    @Test
    void applyMiniGamePassed_FailedAttempt_SkipsScoring() {
        MiniGameAttempt attempt = new MiniGameAttempt();
        attempt.setStatus(AttemptStatus.FAILED);

        scoreRuleEngine.applyMiniGamePassed(attempt, actor);

        verifyNoInteractions(ruleService, scoreEntryService);
    }

    @Test
    void applyMiniGameExhaustedAttempts_FinalFailedAttempt_UsesFailPoints() {
        MiniGame miniGame = new MiniGame();
        miniGame.setActivity(activity);

        MiniGameAttempt attempt = new MiniGameAttempt();
        attempt.setId(701L);
        attempt.setStatus(AttemptStatus.FAILED);
        attempt.setMiniGame(miniGame);
        attempt.setStudent(student);
        attempt.setSubmittedAt(LocalDateTime.now());

        ActivityScoreRule rule = new ActivityScoreRule();
        rule.setId(153L);
        rule.setScoreType(ScoreType.REN_LUYEN);
        rule.setPoints(BigDecimal.ZERO);
        rule.setFailPoints(BigDecimal.valueOf(-2));
        rule.setAudience(ScoreRuleAudience.ALL_PARTICIPANTS);

        when(ruleService.getEnabledRules(activity.getId(), ScoreRuleTrigger.MINIGAME_EXHAUSTED_ATTEMPTS))
                .thenReturn(Collections.singletonList(rule));
        when(semesterResolver.resolveSemester(eq(activity), eq(rule), any())).thenReturn(semester);

        scoreRuleEngine.applyMiniGameExhaustedAttempts(attempt, actor);

        ArgumentCaptor<ScoreEntryCommand> commandCaptor = ArgumentCaptor.forClass(ScoreEntryCommand.class);
        verify(scoreEntryService).upsertEntry(commandCaptor.capture());
        ScoreEntryCommand command = commandCaptor.getValue();

        assertEquals(BigDecimal.valueOf(-2), command.getPoints());
        assertEquals(ScoreEntrySourceType.MINIGAME_ATTEMPT, command.getSourceType());
        assertEquals(attempt.getId(), command.getSourceId());
    }

    @Test
    void applyMiniGameExhaustedAttempts_InSeries_SkipsScoring() {
        activity.setSeriesId(999L);
        MiniGame miniGame = new MiniGame();
        miniGame.setActivity(activity);

        MiniGameAttempt attempt = new MiniGameAttempt();
        attempt.setStatus(AttemptStatus.FAILED);
        attempt.setMiniGame(miniGame);

        scoreRuleEngine.applyMiniGameExhaustedAttempts(attempt, actor);

        verifyNoInteractions(ruleService, scoreEntryService);
    }

    @Test
    void applySeriesMilestone_ParsesMilestonesAndUpsertsCorrectly() {
        ActivitySeries series = new ActivitySeries();
        series.setId(800L);
        series.setName("Java Series");
        series.setMilestonePoints("{\"3\":5,\"5\":10}"); // 3 activities -> 5 points, 5 activities -> 10 points
        series.setScoreType(ScoreType.CHUYEN_DE);

        StudentSeriesProgress progress = new StudentSeriesProgress();
        progress.setId(900L);
        progress.setSeries(series);
        progress.setStudent(student);
        progress.setCompletedCount(4); // Achieved milestone 3 but not 5 -> should get 5 points
        progress.setPointsEarned(BigDecimal.ZERO);

        when(activityRepository.findBySeriesIdAndIsDeletedFalse(series.getId()))
                .thenReturn(Collections.singletonList(activity));
        when(semesterHelperService.getSemesterForActivity(any())).thenReturn(semester);

        scoreRuleEngine.applySeriesMilestone(progress, actor);

        ArgumentCaptor<ScoreEntryCommand> commandCaptor = ArgumentCaptor.forClass(ScoreEntryCommand.class);
        verify(scoreEntryService).upsertEntry(commandCaptor.capture());
        ScoreEntryCommand command = commandCaptor.getValue();

        assertEquals(BigDecimal.valueOf(5), command.getPoints());
        assertEquals(ScoreType.CHUYEN_DE, command.getScoreType());
        assertEquals(ScoreEntrySourceType.SERIES_PROGRESS, command.getSourceType());
        assertEquals(progress.getId(), command.getSourceId());

        verify(progressRepository).save(progress);
        assertEquals(BigDecimal.valueOf(5), progress.getPointsEarned());
    }

    @Test
    void applySeriesMilestone_LowerPointsRequested_NoOp() {
        ActivitySeries series = new ActivitySeries();
        series.setId(800L);
        series.setMilestonePoints("{\"3\":5,\"5\":10}");

        StudentSeriesProgress progress = new StudentSeriesProgress();
        progress.setSeries(series);
        progress.setStudent(student);
        progress.setCompletedCount(2); // Achieved no milestone (0 points)
        progress.setPointsEarned(BigDecimal.valueOf(5)); // But already has 5 points

        scoreRuleEngine.applySeriesMilestone(progress, actor);

        verifyNoInteractions(activityRepository, scoreEntryService, progressRepository);
    }

    @Test
    void applySeriesMinimumRequirement_NotMet_UpsertsNegativePenalty() {
        ActivitySeries series = new ActivitySeries();
        series.setId(800L);
        series.setName("Java Series");
        series.setScoreType(ScoreType.CHUYEN_DE);
        series.setMinimumRequirementEnabled(true);
        series.setMinimumRequiredEvents(3);
        series.setMinimumPenaltyPoints(2);

        activity.setStartDate(LocalDateTime.now().minusDays(3));

        when(activityRepository.findBySeriesIdAndIsDeletedFalse(series.getId()))
                .thenReturn(Collections.singletonList(activity));
        when(semesterHelperService.getSemesterForActivity(activity)).thenReturn(semester);

        scoreRuleEngine.applySeriesMinimumRequirement(series, student, 2, actor);

        ArgumentCaptor<ScoreEntryCommand> commandCaptor = ArgumentCaptor.forClass(ScoreEntryCommand.class);
        verify(scoreEntryService).upsertEntry(commandCaptor.capture());
        ScoreEntryCommand command = commandCaptor.getValue();

        assertEquals(student.getId(), command.getStudentId());
        assertEquals(semester.getId(), command.getSemesterId());
        assertEquals(ScoreType.CHUYEN_DE, command.getScoreType());
        assertEquals(ScoreEntrySourceType.SERIES_MINIMUM_REQUIREMENT, command.getSourceType());
        assertEquals(series.getId(), command.getSourceId());
        assertEquals(BigDecimal.valueOf(-2), command.getPoints());
        assertTrue(command.getReason().contains("2/3"));
    }

    @Test
    void applySeriesMinimumRequirement_MetRequirement_UpsertsZeroPoints() {
        ActivitySeries series = new ActivitySeries();
        series.setId(801L);
        series.setName("Career Series");
        series.setScoreType(ScoreType.REN_LUYEN);
        series.setMinimumRequirementEnabled(true);
        series.setMinimumRequiredEvents(2);
        series.setMinimumPenaltyPoints(3);
        series.setMainActivity(activity);
        activity.setStartDate(LocalDateTime.now().minusDays(1));

        when(activityRepository.findBySeriesIdAndIsDeletedFalse(series.getId())).thenReturn(Collections.emptyList());
        when(semesterHelperService.getSemesterForActivity(activity)).thenReturn(semester);

        scoreRuleEngine.applySeriesMinimumRequirement(series, student, 2, actor);

        ArgumentCaptor<ScoreEntryCommand> commandCaptor = ArgumentCaptor.forClass(ScoreEntryCommand.class);
        verify(scoreEntryService).upsertEntry(commandCaptor.capture());
        assertEquals(BigDecimal.ZERO, commandCaptor.getValue().getPoints());
        assertTrue(commandCaptor.getValue().getReason().contains("met"));
    }
}
