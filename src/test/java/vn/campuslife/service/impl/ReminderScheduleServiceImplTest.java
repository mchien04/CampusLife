package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.ActivitySeries;
import vn.campuslife.entity.ReminderSchedule;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.ReminderCode;
import vn.campuslife.enumeration.ReminderStatus;
import vn.campuslife.enumeration.ReminderTargetType;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.ReminderScheduleRepository;
import vn.campuslife.repository.TaskAssignmentRepository;
import vn.campuslife.repository.TaskSubmissionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderScheduleServiceImplTest {

    @Mock
    private ReminderScheduleRepository reminderScheduleRepository;

    @Mock
    private ReminderRuntimeSchedulerService reminderRuntimeSchedulerService;

    @Mock
    private ActivityRegistrationRepository activityRegistrationRepository;

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

    @Mock
    private TaskSubmissionRepository taskSubmissionRepository;

    @InjectMocks
    private ReminderScheduleServiceImpl reminderScheduleService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reminderScheduleService, "taskOverdueInitialHours", 1L);
    }

    @Test
    void syncSeriesMinimumRequirementReminder_ValidConfig_SchedulesQuartzReminder() {
        User user = new User();
        user.setId(20L);
        user.setEmail("student@campuslife.vn");

        Student student = new Student();
        student.setId(10L);
        student.setUser(user);

        ActivitySeries series = new ActivitySeries();
        series.setId(99L);
        series.setName("Enterprise Series");
        series.setMinimumRequirementEnabled(true);
        series.setMinimumRequiredEvents(3);
        series.setMinimumPenaltyPoints(2);

        Activity firstActivity = new Activity();
        firstActivity.setEndDate(LocalDateTime.of(2026, 6, 20, 10, 0));

        Activity lastActivity = new Activity();
        lastActivity.setEndDate(LocalDateTime.of(2026, 6, 25, 15, 0));

        ActivityRegistration approvedRegistration = new ActivityRegistration();
        approvedRegistration.setStatus(RegistrationStatus.APPROVED);
        approvedRegistration.setActivity(firstActivity);
        approvedRegistration.setStudent(student);

        ActivityRegistration attendedRegistration = new ActivityRegistration();
        attendedRegistration.setStatus(RegistrationStatus.ATTENDED);
        attendedRegistration.setActivity(lastActivity);
        attendedRegistration.setStudent(student);

        when(activityRegistrationRepository.findBySeriesIdAndStudentId(99L, 10L))
                .thenReturn(List.of(approvedRegistration));
        when(activityRegistrationRepository.findBySeriesId(99L))
                .thenReturn(List.of(approvedRegistration, attendedRegistration));
        when(reminderScheduleRepository.findByUserIdAndTargetTypeAndTargetIdAndReminderCode(
                20L, ReminderTargetType.SERIES, 99L, ReminderCode.SERIES_MINIMUM_REQUIREMENT))
                .thenReturn(Optional.empty());
        when(reminderScheduleRepository.save(any(ReminderSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reminderScheduleService.syncSeriesMinimumRequirementReminder(series, student);

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderScheduleRepository).save(captor.capture());
        ReminderSchedule reminder = captor.getValue();

        assertEquals(ReminderTargetType.SERIES, reminder.getTargetType());
        assertEquals(ReminderCode.SERIES_MINIMUM_REQUIREMENT, reminder.getReminderCode());
        assertEquals(99L, reminder.getTargetId());
        assertEquals("student@campuslife.vn", reminder.getRecipientEmail());
        assertEquals(LocalDateTime.of(2026, 6, 25, 15, 1), reminder.getRemindAt());
        assertNotNull(reminder.getSubject());

        verify(reminderRuntimeSchedulerService).scheduleReminder(reminder);
    }

    @Test
    void syncSeriesMinimumRequirementReminders_InvalidConfig_CancelsPendingReminders() {
        ActivitySeries series = new ActivitySeries();
        series.setId(99L);
        series.setMinimumRequirementEnabled(false);

        ReminderSchedule firstReminder = new ReminderSchedule();
        firstReminder.setId(1L);
        firstReminder.setStatus(ReminderStatus.PENDING);

        ReminderSchedule secondReminder = new ReminderSchedule();
        secondReminder.setId(2L);
        secondReminder.setStatus(ReminderStatus.FAILED);

        when(activityRegistrationRepository.findBySeriesId(99L)).thenReturn(List.of());
        when(reminderScheduleRepository.findByTargetTypeAndTargetIdAndStatusIn(
                eq(ReminderTargetType.SERIES), eq(99L), anyCollection()))
                .thenReturn(List.of(firstReminder, secondReminder));

        reminderScheduleService.syncSeriesMinimumRequirementReminders(series);

        assertEquals(ReminderStatus.CANCELLED, firstReminder.getStatus());
        assertEquals(ReminderStatus.CANCELLED, secondReminder.getStatus());
        verify(reminderRuntimeSchedulerService).cancelReminder(1L);
        verify(reminderRuntimeSchedulerService).cancelReminder(2L);
        verify(reminderScheduleRepository).saveAll(List.of(firstReminder, secondReminder));
    }
}
