package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityTask;
import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.ActivitySeries;
import vn.campuslife.entity.MiniGame;
import vn.campuslife.entity.ReminderSchedule;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.StudentSeriesProgress;
import vn.campuslife.entity.TaskAssignment;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.ReminderCode;
import vn.campuslife.enumeration.ReminderStatus;
import vn.campuslife.enumeration.ReminderTargetType;
import vn.campuslife.enumeration.Role;
import vn.campuslife.enumeration.TaskStatus;
import vn.campuslife.repository.ActivityParticipationRepository;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.ActivitySeriesRepository;
import vn.campuslife.repository.EmailHistoryRepository;
import vn.campuslife.repository.MiniGameAttemptRepository;
import vn.campuslife.repository.MiniGameRepository;
import vn.campuslife.repository.ReminderScheduleRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.StudentSeriesProgressRepository;
import vn.campuslife.repository.TaskAssignmentRepository;
import vn.campuslife.repository.TaskSubmissionRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.service.ScoreRuleEngine;
import vn.campuslife.util.EmailUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderDispatchServiceTest {

    @Mock
    private ReminderScheduleRepository reminderScheduleRepository;

    @Mock
    private EmailHistoryRepository emailHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TaskSubmissionRepository taskSubmissionRepository;

    @Mock
    private ActivityRegistrationRepository activityRegistrationRepository;

    @Mock
    private ActivityParticipationRepository activityParticipationRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private MiniGameRepository miniGameRepository;

    @Mock
    private MiniGameAttemptRepository miniGameAttemptRepository;

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

    @Mock
    private ActivitySeriesRepository activitySeriesRepository;

    @Mock
    private StudentSeriesProgressRepository studentSeriesProgressRepository;

    @Mock
    private ScoreRuleEngine scoreRuleEngine;

    @Mock
    private EmailUtil emailUtil;

    @Mock
    private ReminderRuntimeSchedulerService reminderRuntimeSchedulerService;

    @InjectMocks
    private ReminderDispatchService reminderDispatchService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reminderDispatchService, "taskOverdueRepeatDays", 1L);
        ReflectionTestUtils.setField(reminderDispatchService, "taskOverdueRepeatMinutes", 0L);
    }

    @Test
    void dispatchReminder_TaskOverdue_UpdatesAssignmentAppliesPenaltyAndReschedules() {
        User studentUser = new User();
        studentUser.setId(10L);
        studentUser.setEmail("student@campuslife.vn");

        User sender = new User();
        sender.setId(99L);
        sender.setRole(Role.MANAGER);

        Student student = new Student();
        student.setId(7L);
        student.setUser(studentUser);

        ReminderSchedule reminder = new ReminderSchedule();
        reminder.setId(1L);
        reminder.setStatus(ReminderStatus.PENDING);
        reminder.setTargetType(ReminderTargetType.TASK);
        reminder.setReminderCode(ReminderCode.TASK_OVERDUE);
        reminder.setTargetId(123L);
        reminder.setUser(studentUser);
        reminder.setRecipientEmail(studentUser.getEmail());
        reminder.setSubject("Overdue");
        reminder.setContent("Task overdue");
        reminder.setRemindAt(LocalDateTime.now().minusMinutes(5));

        TaskAssignment assignment = new TaskAssignment();
        assignment.setStatus(TaskStatus.ASSIGNED);
        Activity activity = new Activity();
        activity.setRequiresSubmission(true);
        ActivityTask task = new ActivityTask();
        task.setActivity(activity);
        assignment.setTask(task);

        when(reminderScheduleRepository.findById(1L)).thenReturn(Optional.of(reminder));
        when(reminderScheduleRepository.save(any(ReminderSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.findByUserIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(student));
        when(taskSubmissionRepository.findByTaskIdAndStudentIdAndIsDeletedFalse(123L, 7L)).thenReturn(Optional.empty());
        when(taskAssignmentRepository.findByTaskIdAndStudentId(123L, 7L)).thenReturn(Optional.of(assignment));
        when(userRepository.findAllByRoleInAndIsDeletedFalse(List.of(Role.ADMIN, Role.MANAGER)))
                .thenReturn(List.of(sender));
        when(emailUtil.sendCustomEmail(any(), any(), any(), any(Boolean.class), any()))
                .thenReturn(true);

        reminderDispatchService.dispatchReminder(1L);

        assertEquals(TaskStatus.OVERDUE, assignment.getStatus());
        assertEquals(ReminderStatus.PENDING, reminder.getStatus());
        verify(taskAssignmentRepository).save(assignment);
        verify(scoreRuleEngine).applyTaskOverdue(assignment, sender);
        verify(reminderRuntimeSchedulerService).scheduleReminder(reminder);
    }

    @Test
    void dispatchReminder_TaskOverdue_OptionalTask_CancelsReminderWithoutPenalty() {
        User studentUser = new User();
        studentUser.setId(10L);
        studentUser.setEmail("student@campuslife.vn");

        Student student = new Student();
        student.setId(7L);
        student.setUser(studentUser);

        ReminderSchedule reminder = new ReminderSchedule();
        reminder.setId(10L);
        reminder.setStatus(ReminderStatus.PENDING);
        reminder.setTargetType(ReminderTargetType.TASK);
        reminder.setReminderCode(ReminderCode.TASK_OVERDUE);
        reminder.setTargetId(123L);
        reminder.setUser(studentUser);
        reminder.setRecipientEmail(studentUser.getEmail());
        reminder.setRemindAt(LocalDateTime.now().minusMinutes(5));

        Activity activity = new Activity();
        activity.setRequiresSubmission(false);
        ActivityTask task = new ActivityTask();
        task.setActivity(activity);

        TaskAssignment assignment = new TaskAssignment();
        assignment.setTask(task);

        when(reminderScheduleRepository.findById(10L)).thenReturn(Optional.of(reminder));
        when(reminderScheduleRepository.save(any(ReminderSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.findByUserIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(student));
        when(taskSubmissionRepository.findByTaskIdAndStudentIdAndIsDeletedFalse(123L, 7L)).thenReturn(Optional.empty());
        when(taskAssignmentRepository.findByTaskIdAndStudentId(123L, 7L)).thenReturn(Optional.of(assignment));

        reminderDispatchService.dispatchReminder(10L);

        assertEquals(ReminderStatus.CANCELLED, reminder.getStatus());
        verify(scoreRuleEngine, never()).applyTaskOverdue(any(), any());
        verify(emailUtil, never()).sendCustomEmail(any(), any(), any(), any(Boolean.class), any());
    }

    @Test
    void dispatchReminder_NoShow_WithOnlyRegisteredParticipation_DoesNotCancelAndAppliesPenalty() {
        User studentUser = new User();
        studentUser.setId(10L);
        studentUser.setEmail("student@campuslife.vn");

        User sender = new User();
        sender.setId(99L);
        sender.setRole(Role.ADMIN);

        Student student = new Student();
        student.setId(7L);
        student.setUser(studentUser);

        ReminderSchedule reminder = new ReminderSchedule();
        reminder.setId(2L);
        reminder.setStatus(ReminderStatus.PENDING);
        reminder.setTargetType(ReminderTargetType.EVENT);
        reminder.setReminderCode(ReminderCode.EVENT_NO_SHOW_PENALTY);
        reminder.setTargetId(456L);
        reminder.setUser(studentUser);
        reminder.setRecipientEmail(studentUser.getEmail());
        reminder.setSubject("No-show");
        reminder.setContent("No-show penalty");
        reminder.setRemindAt(LocalDateTime.now().minusMinutes(2));

        ActivityRegistration registration = new ActivityRegistration();
        registration.setStatus(RegistrationStatus.APPROVED);

        ActivityParticipation participation = new ActivityParticipation();
        participation.setParticipationType(ParticipationType.REGISTERED);
        participation.setIsCompleted(false);

        when(reminderScheduleRepository.findById(2L)).thenReturn(Optional.of(reminder));
        when(reminderScheduleRepository.save(any(ReminderSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.findByUserIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(student));
        when(activityRegistrationRepository.findByActivityIdAndStudentId(456L, 7L))
                .thenReturn(Optional.of(registration));
        when(activityRepository.findById(456L)).thenReturn(Optional.empty());
        when(activityParticipationRepository.findByRegistration(registration)).thenReturn(Optional.of(participation));
        when(userRepository.findAllByRoleInAndIsDeletedFalse(List.of(Role.ADMIN, Role.MANAGER)))
                .thenReturn(List.of(sender));
        when(emailUtil.sendCustomEmail(any(), any(), any(), any(Boolean.class), any()))
                .thenReturn(true);

        reminderDispatchService.dispatchReminder(2L);

        assertEquals(ReminderStatus.SENT, reminder.getStatus());
        verify(scoreRuleEngine).applyNoShowPenalty(registration, sender);
        verify(reminderRuntimeSchedulerService, never()).scheduleReminder(reminder);
    }

    @Test
    void dispatchReminder_NoShow_MinigameWithOneFailedAttempt_CancelsWithoutPenalty() {
        User studentUser = new User();
        studentUser.setId(10L);
        studentUser.setEmail("student@campuslife.vn");

        Student student = new Student();
        student.setId(7L);
        student.setUser(studentUser);

        ReminderSchedule reminder = new ReminderSchedule();
        reminder.setId(2L);
        reminder.setStatus(ReminderStatus.PENDING);
        reminder.setTargetType(ReminderTargetType.EVENT);
        reminder.setReminderCode(ReminderCode.EVENT_NO_SHOW_PENALTY);
        reminder.setTargetId(456L);
        reminder.setUser(studentUser);
        reminder.setRecipientEmail(studentUser.getEmail());
        reminder.setSubject("No-show");
        reminder.setContent("No-show penalty");
        reminder.setRemindAt(LocalDateTime.now().minusMinutes(2));

        ActivityRegistration registration = new ActivityRegistration();
        registration.setStatus(RegistrationStatus.APPROVED);

        Activity activity = new Activity();
        activity.setId(456L);
        activity.setType(ActivityType.MINIGAME);

        MiniGame miniGame = new MiniGame();
        miniGame.setId(88L);
        miniGame.setActivity(activity);

        when(reminderScheduleRepository.findById(2L)).thenReturn(Optional.of(reminder));
        when(reminderScheduleRepository.save(any(ReminderSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.findByUserIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(student));
        when(activityRegistrationRepository.findByActivityIdAndStudentId(456L, 7L))
                .thenReturn(Optional.of(registration));
        when(activityRepository.findById(456L)).thenReturn(Optional.of(activity));
        when(miniGameRepository.findByActivityId(456L)).thenReturn(Optional.of(miniGame));
        when(miniGameAttemptRepository.existsByStudentIdAndMiniGameId(7L, 88L)).thenReturn(true);

        reminderDispatchService.dispatchReminder(2L);

        assertEquals(ReminderStatus.CANCELLED, reminder.getStatus());
        verify(scoreRuleEngine, never()).applyNoShowPenalty(any(), any());
        verify(emailUtil, never()).sendCustomEmail(any(), any(), any(), any(Boolean.class), any());
    }

    @Test
    void dispatchReminder_SeriesMinimumRequirement_AppliesSeriesPenaltyFlow() {
        User studentUser = new User();
        studentUser.setId(10L);
        studentUser.setEmail("student@campuslife.vn");

        User sender = new User();
        sender.setId(99L);
        sender.setRole(Role.ADMIN);

        Student student = new Student();
        student.setId(7L);
        student.setUser(studentUser);

        ActivitySeries series = new ActivitySeries();
        series.setId(789L);
        series.setName("Career Series");
        series.setMinimumRequirementEnabled(true);
        series.setMinimumRequiredEvents(3);
        series.setMinimumPenaltyPoints(2);

        ReminderSchedule reminder = new ReminderSchedule();
        reminder.setId(3L);
        reminder.setStatus(ReminderStatus.PENDING);
        reminder.setTargetType(ReminderTargetType.SERIES);
        reminder.setReminderCode(ReminderCode.SERIES_MINIMUM_REQUIREMENT);
        reminder.setTargetId(789L);
        reminder.setUser(studentUser);
        reminder.setRecipientEmail(studentUser.getEmail());
        reminder.setSubject("Series minimum");
        reminder.setContent("Series minimum penalty");
        reminder.setRemindAt(LocalDateTime.now().minusMinutes(1));

        ActivityRegistration approvedRegistration = new ActivityRegistration();
        approvedRegistration.setStatus(RegistrationStatus.APPROVED);

        StudentSeriesProgress progress = new StudentSeriesProgress();
        progress.setCompletedCount(1);

        when(reminderScheduleRepository.findById(3L)).thenReturn(Optional.of(reminder));
        when(reminderScheduleRepository.save(any(ReminderSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.findByUserIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(student));
        when(activitySeriesRepository.findById(789L)).thenReturn(Optional.of(series));
        when(activityRegistrationRepository.findBySeriesIdAndStudentId(789L, 7L))
                .thenReturn(List.of(approvedRegistration));
        when(studentSeriesProgressRepository.findByStudentIdAndSeriesId(7L, 789L)).thenReturn(Optional.of(progress));
        when(userRepository.findAllByRoleInAndIsDeletedFalse(List.of(Role.ADMIN, Role.MANAGER)))
                .thenReturn(List.of(sender));
        when(emailUtil.sendCustomEmail(any(), any(), any(), any(Boolean.class), any()))
                .thenReturn(true);

        reminderDispatchService.dispatchReminder(3L);

        assertEquals(ReminderStatus.SENT, reminder.getStatus());
        verify(scoreRuleEngine).applySeriesMinimumRequirement(series, student, 1, sender);
    }
}
