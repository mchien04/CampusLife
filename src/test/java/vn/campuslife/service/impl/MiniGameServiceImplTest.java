package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.AttemptStatus;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.quiz.StartAttemptResponse;
import vn.campuslife.model.activity.quiz.SubmitAttemptResponse;
import vn.campuslife.repository.*;
import vn.campuslife.service.ActivitySeriesService;
import vn.campuslife.service.SemesterHelperService;
import vn.campuslife.service.ScoreRuleEngine;
import vn.campuslife.config.UploadProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MiniGameServiceImplTest {

    @Mock
    private MiniGameAttemptRepository attemptRepository;

    @Mock
    private MiniGameRepository miniGameRepository;

    @Mock
    private MiniGameQuizRepository quizRepository;

    @Mock
    private MiniGameQuizQuestionRepository questionRepository;

    @Mock
    private MiniGameQuizOptionRepository optionRepository;

    @Mock
    private MiniGameAnswerRepository answerRepository;

    @Mock
    private ActivityRegistrationRepository registrationRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ActivityParticipationRepository participationRepository;

    @Mock
    private ActivitySeriesService activitySeriesService;

    @Mock
    private ScoreRuleEngine scoreRuleEngine;

    @Mock
    private ScoreEntryRepository scoreEntryRepository;

    @Mock
    private SemesterHelperService semesterHelperService;

    @Mock
    private UploadProperties uploadProperties;

    @InjectMocks
    private MiniGameServiceImpl miniGameService;

    private Student student;
    private User studentUser;
    private Activity activity;
    private MiniGame miniGame;
    private MiniGameAttempt attempt;

    @BeforeEach
    void setUp() {
        studentUser = new User();
        studentUser.setId(33L);

        student = new Student();
        student.setId(10L);
        student.setUser(studentUser);

        activity = new Activity();
        activity.setId(100L);
        activity.setName("MiniGame Activity");

        miniGame = new MiniGame();
        miniGame.setId(150L);
        miniGame.setActivity(activity);
        miniGame.setRequiredCorrectAnswers(2);

        attempt = new MiniGameAttempt();
        attempt.setId(700L);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setMiniGame(miniGame);
        attempt.setStudent(student);
    }

    @Test
    void submitAttempt_CorrectAnswers_PassedAndCalculatesScore() {
        when(attemptRepository.findById(700L)).thenReturn(Optional.of(attempt));

        MiniGameQuizQuestion q1 = new MiniGameQuizQuestion();
        q1.setId(1L);
        MiniGameQuizQuestion q2 = new MiniGameQuizQuestion();
        q2.setId(2L);

        MiniGameQuizOption opt1 = new MiniGameQuizOption();
        opt1.setId(11L);
        opt1.setCorrect(true);

        MiniGameQuizOption opt2 = new MiniGameQuizOption();
        opt2.setId(12L);
        opt2.setCorrect(true);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(q1));
        when(questionRepository.findById(2L)).thenReturn(Optional.of(q2));
        when(optionRepository.findById(11L)).thenReturn(Optional.of(opt1));
        when(optionRepository.findById(12L)).thenReturn(Optional.of(opt2));

        ActivityRegistration registration = new ActivityRegistration();
        registration.setId(50L);
        registration.setActivity(activity);
        registration.setStudent(student);
        when(registrationRepository.findByActivityIdAndStudentId(100L, 10L))
                .thenReturn(Optional.of(registration));

        when(participationRepository.findByRegistration(registration))
                .thenReturn(Optional.empty());

        Map<Long, Long> answers = new HashMap<>();
        answers.put(1L, 11L);
        answers.put(2L, 12L);

        Response response = miniGameService.submitAttempt(700L, 10L, answers);

        assertTrue(response.isStatus());
        assertEquals(AttemptStatus.PASSED, attempt.getStatus());
        assertEquals(2, attempt.getCorrectCount());

        verify(answerRepository, times(2)).save(any(MiniGameAnswer.class));
        verify(attemptRepository).save(attempt);

        // Standalone quiz triggers applyMiniGamePassed
        verify(scoreRuleEngine).applyMiniGamePassed(attempt, studentUser);
        verifyNoInteractions(activitySeriesService);
    }

    @Test
    void startAttempt_InProgressExistsAtMaxAttempts_ResumesInsteadOfBlocking() {
        miniGame.setMaxAttempts(1);
        attempt.setStartedAt(java.time.LocalDateTime.now());

        when(miniGameRepository.findById(150L)).thenReturn(Optional.of(miniGame));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(attemptRepository.findInProgressAttempt(10L, 150L, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));

        Response response = miniGameService.startAttempt(150L, 10L);

        assertTrue(response.isStatus());
        assertInstanceOf(StartAttemptResponse.class, response.getBody());
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void submitAttempt_FinalFailedAttempt_Standalone_AppliesExhaustedPenalty() {
        miniGame.setMaxAttempts(2);
        when(attemptRepository.findById(700L)).thenReturn(Optional.of(attempt));

        MiniGameQuizQuestion q1 = new MiniGameQuizQuestion();
        q1.setId(1L);
        MiniGameQuizQuestion q2 = new MiniGameQuizQuestion();
        q2.setId(2L);

        MiniGameQuizOption opt1 = new MiniGameQuizOption();
        opt1.setId(11L);
        opt1.setCorrect(false);

        MiniGameQuizOption opt2 = new MiniGameQuizOption();
        opt2.setId(12L);
        opt2.setCorrect(false);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(q1));
        when(questionRepository.findById(2L)).thenReturn(Optional.of(q2));
        when(optionRepository.findById(11L)).thenReturn(Optional.of(opt1));
        when(optionRepository.findById(12L)).thenReturn(Optional.of(opt2));
        when(attemptRepository.existsByStudentIdAndMiniGameIdAndStatus(10L, 150L, AttemptStatus.PASSED))
                .thenReturn(false);
        when(attemptRepository.findByStudentIdAndMiniGameId(10L, 150L))
                .thenReturn(List.of(new MiniGameAttempt(), attempt));

        Map<Long, Long> answers = new HashMap<>();
        answers.put(1L, 11L);
        answers.put(2L, 12L);

        Response response = miniGameService.submitAttempt(700L, 10L, answers);

        assertTrue(response.isStatus());
        assertEquals(AttemptStatus.FAILED, attempt.getStatus());
        verify(scoreRuleEngine).applyMiniGameExhaustedAttempts(attempt, studentUser);
        verify(scoreRuleEngine, never()).applyMiniGamePassed(any(), any());
    }

    @Test
    void submitAttempt_FinalFailedAttempt_AfterPriorPass_DoesNotApplyExhaustedPenalty() {
        miniGame.setMaxAttempts(2);
        when(attemptRepository.findById(700L)).thenReturn(Optional.of(attempt));

        MiniGameQuizQuestion q1 = new MiniGameQuizQuestion();
        q1.setId(1L);
        MiniGameQuizQuestion q2 = new MiniGameQuizQuestion();
        q2.setId(2L);

        MiniGameQuizOption opt1 = new MiniGameQuizOption();
        opt1.setId(11L);
        opt1.setCorrect(false);

        MiniGameQuizOption opt2 = new MiniGameQuizOption();
        opt2.setId(12L);
        opt2.setCorrect(false);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(q1));
        when(questionRepository.findById(2L)).thenReturn(Optional.of(q2));
        when(optionRepository.findById(11L)).thenReturn(Optional.of(opt1));
        when(optionRepository.findById(12L)).thenReturn(Optional.of(opt2));
        when(attemptRepository.existsByStudentIdAndMiniGameIdAndStatus(10L, 150L, AttemptStatus.PASSED))
                .thenReturn(true);

        Map<Long, Long> answers = new HashMap<>();
        answers.put(1L, 11L);
        answers.put(2L, 12L);

        Response response = miniGameService.submitAttempt(700L, 10L, answers);

        assertTrue(response.isStatus());
        assertEquals(AttemptStatus.FAILED, attempt.getStatus());
        verify(scoreRuleEngine, never()).applyMiniGameExhaustedAttempts(any(), any());
        verify(scoreRuleEngine, never()).applyMiniGamePassed(any(), any());
    }

    @Test
    void submitAttempt_FinalFailedAttempt_InSeries_DoesNotApplyExhaustedPenalty() {
        miniGame.setMaxAttempts(1);
        activity.setSeriesId(888L);
        when(attemptRepository.findById(700L)).thenReturn(Optional.of(attempt));

        MiniGameQuizQuestion q1 = new MiniGameQuizQuestion();
        q1.setId(1L);
        MiniGameQuizOption opt1 = new MiniGameQuizOption();
        opt1.setId(11L);
        opt1.setCorrect(false);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(q1));
        when(optionRepository.findById(11L)).thenReturn(Optional.of(opt1));

        Map<Long, Long> answers = new HashMap<>();
        answers.put(1L, 11L);

        Response response = miniGameService.submitAttempt(700L, 10L, answers);

        assertTrue(response.isStatus());
        assertEquals(AttemptStatus.FAILED, attempt.getStatus());
        verify(scoreRuleEngine, never()).applyMiniGameExhaustedAttempts(any(), any());
        verifyNoInteractions(activitySeriesService);
    }

    @Test
    void calculateScoreAndCreateParticipation_Standalone_Successful() {
        attempt.setStatus(AttemptStatus.PASSED);
        when(attemptRepository.findById(700L)).thenReturn(Optional.of(attempt));

        ActivityRegistration registration = new ActivityRegistration();
        when(registrationRepository.findByActivityIdAndStudentId(100L, 10L))
                .thenReturn(Optional.of(registration));
        when(participationRepository.findByRegistration(registration)).thenReturn(Optional.empty());

        Response response = miniGameService.calculateScoreAndCreateParticipation(700L);

        assertTrue(response.isStatus());
        verify(participationRepository).save(any(ActivityParticipation.class));
        assertEquals(RegistrationStatus.ATTENDED, registration.getStatus());
        verify(scoreRuleEngine).applyMiniGamePassed(attempt, studentUser);
        verifyNoInteractions(activitySeriesService);
    }

    @Test
    void calculateScoreAndCreateParticipation_SeriesActivity_TriggersSeriesProgress() {
        attempt.setStatus(AttemptStatus.PASSED);
        activity.setSeriesId(888L); // Belongs to series
        when(attemptRepository.findById(700L)).thenReturn(Optional.of(attempt));

        ActivityRegistration registration = new ActivityRegistration();
        when(registrationRepository.findByActivityIdAndStudentId(100L, 10L))
                .thenReturn(Optional.of(registration));
        when(participationRepository.findByRegistration(registration)).thenReturn(Optional.empty());

        Response response = miniGameService.calculateScoreAndCreateParticipation(700L);

        assertTrue(response.isStatus());
        verify(activitySeriesService).updateStudentProgress(10L, 100L);
        verifyNoInteractions(scoreRuleEngine);
    }

    @Test
    void calculateScoreAndCreateParticipation_IdempotentCheck_ReturnsExisting() {
        attempt.setStatus(AttemptStatus.PASSED);
        when(attemptRepository.findById(700L)).thenReturn(Optional.of(attempt));

        ActivityRegistration registration = new ActivityRegistration();
        ActivityParticipation existingParticipation = new ActivityParticipation();
        existingParticipation.setParticipationType(ParticipationType.COMPLETED);
        existingParticipation.setIsCompleted(true); // Already completed

        when(registrationRepository.findByActivityIdAndStudentId(100L, 10L))
                .thenReturn(Optional.of(registration));
        when(participationRepository.findByRegistration(registration))
                .thenReturn(Optional.of(existingParticipation));

        Response response = miniGameService.calculateScoreAndCreateParticipation(700L);

        assertTrue(response.isStatus());
        assertEquals("Participation already exists. Points already awarded.", response.getMessage());
        verify(participationRepository, never()).save(any());
        verify(scoreRuleEngine, never()).applyMiniGamePassed(any(), any());
    }
}
