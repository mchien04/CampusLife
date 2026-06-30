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
import vn.campuslife.enumeration.TaskStatus;
import vn.campuslife.model.Response;
import vn.campuslife.repository.*;
import vn.campuslife.service.ActivitySeriesService;
import vn.campuslife.service.ReminderScheduleService;
import vn.campuslife.service.ScoreRuleEngine;
import vn.campuslife.service.SemesterHelperService;
import vn.campuslife.service.UploadStorageService;
import vn.campuslife.config.UploadProperties;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskSubmissionServiceImplTest {

    @Mock
    private TaskSubmissionRepository taskSubmissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

    @Mock
    private ActivityRegistrationRepository activityRegistrationRepository;

    @Mock
    private ActivityParticipationRepository activityParticipationRepository;

    @Mock
    private ScoreRuleEngine scoreRuleEngine;

    @Mock
    private ActivitySeriesService activitySeriesService;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ActivityTaskRepository activityTaskRepository;

    @Mock
    private UploadProperties uploadProperties;

    @Mock
    private UploadStorageService uploadStorageService;

    @Mock
    private SemesterHelperService semesterHelperService;

    @Mock
    private ReminderScheduleService reminderScheduleService;

    @InjectMocks
    private TaskSubmissionServiceImpl taskSubmissionService;

    private User grader;
    private Student student;
    private Activity activity;
    private ActivityTask task;
    private TaskSubmission submission;

    @BeforeEach
    void setUp() {
        grader = new User();
        grader.setId(1L);

        student = new Student();
        student.setId(10L);

        activity = new Activity();
        activity.setId(100L);
        activity.setName("Test Activity");

        task = new ActivityTask();
        task.setId(150L);
        task.setActivity(activity);

        submission = new TaskSubmission();
        submission.setId(600L);
        submission.setTask(task);
        submission.setStudent(student);
    }

    @Test
    void gradeSubmission_StandaloneActivity_GradedCompleted_Successful() {
        activity.setRequiresSubmission(true);

        when(taskSubmissionRepository.findById(600L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(1L)).thenReturn(Optional.of(grader));

        ActivityRegistration registration = new ActivityRegistration();
        registration.setStatus(RegistrationStatus.ATTENDED);
        when(activityRegistrationRepository.findByActivityIdAndStudentId(100L, 10L))
                .thenReturn(Optional.of(registration));

        ActivityParticipation participation = new ActivityParticipation();
        when(activityParticipationRepository.findByRegistration(registration))
                .thenReturn(Optional.of(participation));

        TaskAssignment assignment = new TaskAssignment();
        when(taskAssignmentRepository.findByTaskIdAndStudentId(150L, 10L))
                .thenReturn(Optional.of(assignment));

        Response response = taskSubmissionService.gradeSubmission(600L, 1L, true, "Good job");

        assertTrue(response.isStatus());
        assertEquals("Submission graded successfully", response.getMessage());

        assertEquals(SubmissionStatus.GRADED, submission.getStatus());
        assertEquals("Good job", submission.getFeedback());
        assertEquals(true, submission.getIsCompleted());

        verify(taskSubmissionRepository).save(submission);
        verify(taskAssignmentRepository).save(assignment);
        assertEquals(TaskStatus.COMPLETED, assignment.getStatus());
        verify(activityParticipationRepository).save(participation);
        assertEquals(true, participation.getIsCompleted());

        verify(scoreRuleEngine).applySubmissionGraded(submission, grader);
        verifyNoInteractions(activitySeriesService);
    }

    @Test
    void gradeSubmission_StandaloneActivity_NotYetAttended_DoesNotCompleteOrScore() {
        activity.setRequiresSubmission(true);

        when(taskSubmissionRepository.findById(600L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(1L)).thenReturn(Optional.of(grader));

        ActivityRegistration registration = new ActivityRegistration();
        registration.setStatus(RegistrationStatus.APPROVED);
        when(activityRegistrationRepository.findByActivityIdAndStudentId(100L, 10L))
                .thenReturn(Optional.of(registration));

        TaskAssignment assignment = new TaskAssignment();
        when(taskAssignmentRepository.findByTaskIdAndStudentId(150L, 10L))
                .thenReturn(Optional.of(assignment));

        Response response = taskSubmissionService.gradeSubmission(600L, 1L, false, "Late");

        assertTrue(response.isStatus());
        verify(activityParticipationRepository, never()).save(any());
        verify(scoreRuleEngine, never()).applySubmissionGraded(any(), any());
        verifyNoInteractions(activitySeriesService);
    }

    @Test
    void gradeSubmission_ActivityInSeries_GradedCompleted_TriggersSeriesProgress() {
        activity.setRequiresSubmission(true);
        activity.setSeriesId(800L); // In series

        when(taskSubmissionRepository.findById(600L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(1L)).thenReturn(Optional.of(grader));

        ActivityRegistration registration = new ActivityRegistration();
        registration.setStatus(RegistrationStatus.ATTENDED);
        when(activityRegistrationRepository.findByActivityIdAndStudentId(100L, 10L))
                .thenReturn(Optional.of(registration));

        ActivityParticipation participation = new ActivityParticipation();
        when(activityParticipationRepository.findByRegistration(registration))
                .thenReturn(Optional.of(participation));

        Response response = taskSubmissionService.gradeSubmission(600L, 1L, true, "Passed milestone");

        assertTrue(response.isStatus());
        verify(activitySeriesService).updateStudentProgress(10L, 100L);
        verify(scoreRuleEngine, never()).applySubmissionGraded(any(), any());
    }

    @Test
    void gradeSubmission_SubmissionNotFound_ReturnsError() {
        when(taskSubmissionRepository.findById(600L)).thenReturn(Optional.empty());

        Response response = taskSubmissionService.gradeSubmission(600L, 1L, true, "Feedback");

        assertFalse(response.isStatus());
        assertEquals("Submission not found", response.getMessage());
    }

    @Test
    void gradeSubmission_GraderNotFound_ReturnsError() {
        when(taskSubmissionRepository.findById(600L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Response response = taskSubmissionService.gradeSubmission(600L, 1L, true, "Feedback");

        assertFalse(response.isStatus());
        assertEquals("Grader not found", response.getMessage());
    }

    @Test
    void submitTask_Success() {
        activity.setRequiresSubmission(true);
        activity.setDraft(false);
        activity.setDeleted(false);
        task.setDeadline(LocalDateTime.now().plusDays(1));

        when(activityTaskRepository.findById(150L)).thenReturn(Optional.of(task));
        when(studentRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(student));
        when(taskSubmissionRepository.findByTaskIdAndStudentIdAndIsDeletedFalse(150L, 10L)).thenReturn(Optional.empty());

        ActivityRegistration registration = new ActivityRegistration();
        registration.setStatus(RegistrationStatus.ATTENDED);
        when(activityRegistrationRepository.findByActivityIdAndStudentId(100L, 10L)).thenReturn(Optional.of(registration));
        when(uploadProperties.getPaths()).thenReturn(new UploadProperties.Paths());

        Response response = taskSubmissionService.submitTask(150L, 10L, "My Content", null, null);

        assertTrue(response.isStatus());
        assertEquals("Task submitted successfully", response.getMessage());
        verify(taskSubmissionRepository).save(any(TaskSubmission.class));
    }

    @Test
    void submitTask_PastDeadline_ReturnsError() {
        activity.setRequiresSubmission(true);
        activity.setDraft(false);
        activity.setDeleted(false);
        task.setDeadline(LocalDateTime.now().minusDays(1)); // past deadline

        when(activityTaskRepository.findById(150L)).thenReturn(Optional.of(task));

        Response response = taskSubmissionService.submitTask(150L, 10L, "My Content", null, null);

        assertFalse(response.isStatus());
        assertEquals("Submission deadline has passed", response.getMessage());
        verify(taskSubmissionRepository, never()).save(any(TaskSubmission.class));
    }

    @Test
    void submitTask_NotRegistered_ReturnsError() {
        activity.setRequiresSubmission(true);
        activity.setDraft(false);
        activity.setDeleted(false);
        task.setDeadline(LocalDateTime.now().plusDays(1));

        when(activityTaskRepository.findById(150L)).thenReturn(Optional.of(task));
        when(activityRegistrationRepository.findByActivityIdAndStudentId(100L, 10L)).thenReturn(Optional.empty());

        Response response = taskSubmissionService.submitTask(150L, 10L, "My Content", null, null);

        assertFalse(response.isStatus());
        assertEquals("Student is not registered for this activity", response.getMessage());
        verify(taskSubmissionRepository, never()).save(any(TaskSubmission.class));
    }

    @Test
    void submitTask_ApprovedRegistrationNotAttended_Success() {
        activity.setRequiresSubmission(true);
        activity.setDraft(false);
        activity.setDeleted(false);
        task.setDeadline(LocalDateTime.now().plusDays(1));

        when(activityTaskRepository.findById(150L)).thenReturn(Optional.of(task));
        when(studentRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(student));
        when(taskSubmissionRepository.findByTaskIdAndStudentIdAndIsDeletedFalse(150L, 10L)).thenReturn(Optional.empty());

        ActivityRegistration registration = new ActivityRegistration();
        registration.setStatus(RegistrationStatus.APPROVED); // not attended yet!
        when(activityRegistrationRepository.findByActivityIdAndStudentId(100L, 10L)).thenReturn(Optional.of(registration));
        when(uploadProperties.getPaths()).thenReturn(new UploadProperties.Paths());

        Response response = taskSubmissionService.submitTask(150L, 10L, "My Content", null, null);

        assertTrue(response.isStatus());
        assertEquals("Task submitted successfully", response.getMessage());
        verify(taskSubmissionRepository).save(any(TaskSubmission.class));
    }

    @Test
    void submitTask_ActivityDraft_ReturnsError() {
        activity.setRequiresSubmission(true);
        activity.setDraft(true); // draft!
        activity.setDeleted(false);

        when(activityTaskRepository.findById(150L)).thenReturn(Optional.of(task));

        Response response = taskSubmissionService.submitTask(150L, 10L, "My Content", null, null);

        assertFalse(response.isStatus());
        assertEquals("Activity is not published yet", response.getMessage());
        verify(taskSubmissionRepository, never()).save(any(TaskSubmission.class));
    }

    @Test
    void gradeSubmission_StandaloneActivity_GradedCompleted_BeforeAttendance_DoesNotScore() {
        activity.setRequiresSubmission(true);

        when(taskSubmissionRepository.findById(600L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(1L)).thenReturn(Optional.of(grader));

        ActivityRegistration registration = new ActivityRegistration();
        registration.setStatus(RegistrationStatus.APPROVED); // not attended yet
        when(activityRegistrationRepository.findByActivityIdAndStudentId(100L, 10L))
                .thenReturn(Optional.of(registration));

        TaskAssignment assignment = new TaskAssignment();
        when(taskAssignmentRepository.findByTaskIdAndStudentId(150L, 10L))
                .thenReturn(Optional.of(assignment));

        Response response = taskSubmissionService.gradeSubmission(600L, 1L, true, "Passed task");

        assertTrue(response.isStatus());
        assertEquals(SubmissionStatus.GRADED, submission.getStatus());
        assertTrue(submission.getIsCompleted());

        // Verify participation is not saved as completed and score rule engine is not called
        verify(activityParticipationRepository, never()).save(any());
        verify(scoreRuleEngine, never()).applySubmissionGraded(any(), any());
    }
}
