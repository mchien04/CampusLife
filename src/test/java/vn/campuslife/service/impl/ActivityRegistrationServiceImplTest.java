package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.SubmissionStatus;
import vn.campuslife.model.Response;
import vn.campuslife.repository.*;
import vn.campuslife.service.ActivitySeriesService;
import vn.campuslife.service.NotificationService;
import vn.campuslife.service.ScoreRuleEngine;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActivityRegistrationServiceImplTest {

    @Mock
    private ActivityRegistrationRepository registrationRepository;

    @Mock
    private ActivityParticipationRepository participationRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ActivitySeriesService activitySeriesService;

    @Mock
    private ScoreRuleEngine scoreRuleEngine;

    @Mock
    private TaskSubmissionRepository taskSubmissionRepository;

    @InjectMocks
    private ActivityRegistrationServiceImpl activityRegistrationService;

    private Student student;
    private User studentUser;
    private Activity activity;
    private ActivityRegistration registration;
    private ActivityParticipation participation;

    @BeforeEach
    void setUp() {
        studentUser = new User();
        studentUser.setId(33L);

        student = new Student();
        student.setId(10L);
        student.setUser(studentUser);

        activity = new Activity();
        activity.setId(100L);
        activity.setName("Reg Activity");
        activity.setDraft(false);

        registration = new ActivityRegistration();
        registration.setId(50L);
        registration.setActivity(activity);
        registration.setStudent(student);
        registration.setStatus(RegistrationStatus.APPROVED);

        participation = new ActivityParticipation();
        participation.setId(600L);
        participation.setRegistration(registration);
        participation.setParticipationType(ParticipationType.ATTENDED);
    }

    @Test
    void gradeCompletion_NoSubmissionRequired_GradedCompleted_Successful() {
        activity.setRequiresSubmission(false);

        when(participationRepository.findById(600L)).thenReturn(Optional.of(participation));

        Response response = activityRegistrationService.gradeCompletion(600L, true, "Completed successfully");

        assertTrue(response.isStatus());
        assertEquals("Đã chấm điểm completion", response.getMessage());
        assertEquals(true, participation.getIsCompleted());
        assertEquals(ParticipationType.COMPLETED, participation.getParticipationType());

        verify(participationRepository).save(participation);
        verify(scoreRuleEngine).applyActivityCompleted(participation, studentUser);
        verifyNoInteractions(activitySeriesService);
    }

    @Test
    void gradeCompletion_SubmissionRequired_NoGradedSubmission_ReturnsError() {
        activity.setRequiresSubmission(true);

        when(participationRepository.findById(600L)).thenReturn(Optional.of(participation));
        when(taskSubmissionRepository.existsByActivityAndStudentAndStatus(100L, 10L, SubmissionStatus.GRADED))
                .thenReturn(false); // No graded submission

        Response response = activityRegistrationService.gradeCompletion(600L, true, "Completed");

        assertFalse(response.isStatus());
        assertEquals("Sinh viên chưa nộp bài hoặc chưa được chấm điểm", response.getMessage());
        verify(participationRepository, never()).save(any());
        verifyNoInteractions(scoreRuleEngine);
    }

    @Test
    void gradeCompletion_SubmissionRequired_HasGradedSubmission_Successful() {
        activity.setRequiresSubmission(true);

        when(participationRepository.findById(600L)).thenReturn(Optional.of(participation));
        when(taskSubmissionRepository.existsByActivityAndStudentAndStatus(100L, 10L, SubmissionStatus.GRADED))
                .thenReturn(true); // Has graded submission

        Response response = activityRegistrationService.gradeCompletion(600L, true, "Completed");

        assertTrue(response.isStatus());
        assertEquals("Đã chấm điểm completion (điểm đã được tính từ bài nộp)", response.getMessage());
        assertEquals(true, participation.getIsCompleted());

        verify(participationRepository).save(participation);
        verifyNoInteractions(scoreRuleEngine); // Scoring is already handled via the submission grading
    }

    @Test
    void gradeCompletion_InSeries_TriggersSeriesProgress() {
        activity.setRequiresSubmission(false);
        activity.setSeriesId(888L); // Part of series

        when(participationRepository.findById(600L)).thenReturn(Optional.of(participation));

        Response response = activityRegistrationService.gradeCompletion(600L, true, "Completed");

        assertTrue(response.isStatus());
        verify(activitySeriesService).updateStudentProgress(10L, 100L);
    }
}
