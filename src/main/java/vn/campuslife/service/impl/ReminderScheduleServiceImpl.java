package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.ActivityTask;
import vn.campuslife.entity.ReminderSchedule;
import vn.campuslife.entity.TaskAssignment;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.ReminderCode;
import vn.campuslife.enumeration.ReminderStatus;
import vn.campuslife.enumeration.ReminderTargetType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.ReminderScheduleRepository;
import vn.campuslife.repository.TaskAssignmentRepository;
import vn.campuslife.repository.TaskSubmissionRepository;
import vn.campuslife.service.ReminderScheduleService;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReminderScheduleServiceImpl implements ReminderScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(ReminderScheduleServiceImpl.class);

    @Value("${app.reminder.event.before-days:1}")
    private long eventReminderBeforeDays;

    @Value("${app.reminder.event.before-hours:1}")
    private long eventReminderBeforeHours;

    @Value("${app.reminder.event.before-minutes:0}")
    private long eventReminderBeforeMinutes;

    @Value("${app.reminder.task.before-days:1}")
    private long taskReminderBeforeDays;

    @Value("${app.reminder.task.before-hours:3}")
    private long taskReminderBeforeHours;

    @Value("${app.reminder.task.before-minutes:0}")
    private long taskReminderBeforeMinutes;

    @Value("${app.reminder.task.overdue-initial-hours:1}")
    private long taskOverdueInitialHours;

    @Value("${app.reminder.task.overdue-initial-minutes:0}")
    private long taskOverdueInitialMinutes;

    private final ReminderScheduleRepository reminderScheduleRepository;
    private final ReminderRuntimeSchedulerService reminderRuntimeSchedulerService;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;

    @Override
    @Transactional
    public void createEventRemindersForApprovedRegistration(ActivityRegistration registration) {
        if (registration == null || registration.getStatus() != RegistrationStatus.APPROVED) {
            return;
        }

        Activity activity = registration.getActivity();
        if (activity == null || activity.getId() == null || activity.getStartDate() == null) {
            return;
        }

        User user = registration.getStudent() != null ? registration.getStudent().getUser() : null;
        if (user == null || user.getId() == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        LocalDateTime eventStart = activity.getStartDate();
        createOrUpdateEventReminder(user, activity, ReminderCode.BEFORE_1_DAY, calculateEventBeforeDayReminderAt(eventStart));
        createOrUpdateEventReminder(user, activity, ReminderCode.BEFORE_1_HOUR, calculateEventBeforeHourReminderAt(eventStart));
    }

    @Override
    @Transactional
    public void cancelPendingEventRemindersForRegistration(ActivityRegistration registration) {
        if (registration == null || registration.getActivity() == null || registration.getStudent() == null
                || registration.getStudent().getUser() == null) {
            return;
        }

        Long userId = registration.getStudent().getUser().getId();
        Long activityId = registration.getActivity().getId();
        if (userId == null || activityId == null) {
            return;
        }

        List<ReminderSchedule> reminders = reminderScheduleRepository.findByUserIdAndTargetTypeAndTargetIdAndStatusIn(
                userId,
                ReminderTargetType.EVENT,
                activityId,
                EnumSet.of(ReminderStatus.PENDING, ReminderStatus.FAILED)
        );

        if (reminders.isEmpty()) {
            return;
        }

        for (ReminderSchedule reminder : reminders) {
            reminder.setStatus(ReminderStatus.CANCELLED);
            reminderRuntimeSchedulerService.cancelReminder(reminder.getId());
        }
        reminderScheduleRepository.saveAll(reminders);
        logger.info("Cancelled {} pending event reminders for registration {}", reminders.size(), registration.getId());
    }

    @Override
    @Transactional
    public void syncEventRemindersForActivity(Activity activity) {
        if (activity == null || activity.getId() == null) {
            return;
        }

        List<ActivityRegistration> registrations = activityRegistrationRepository
                .findByActivityIdAndStatus(activity.getId(), RegistrationStatus.APPROVED);

        for (ActivityRegistration registration : registrations) {
            createEventRemindersForApprovedRegistration(registration);
        }
    }

    @Override
    @Transactional
    public void createTaskRemindersForAssignment(TaskAssignment assignment) {
        if (assignment == null || assignment.getTask() == null || assignment.getStudent() == null
                || assignment.getStudent().getUser() == null) {
            return;
        }

        ActivityTask task = assignment.getTask();
        User user = assignment.getStudent().getUser();
        if (task.getId() == null || task.getDeadline() == null || user.getId() == null
                || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        boolean hasSubmission = taskSubmissionRepository.findByTaskIdAndStudentIdAndIsDeletedFalse(
                task.getId(),
                assignment.getStudent().getId()
        ).isPresent();
        if (hasSubmission) {
            cancelPendingTaskRemindersForAssignment(assignment);
            return;
        }

        LocalDateTime deadline = task.getDeadline();
        createOrUpdateTaskReminder(user, task, ReminderCode.TASK_BEFORE_1_DAY, calculateTaskBeforeDayReminderAt(deadline));
        createOrUpdateTaskReminder(user, task, ReminderCode.TASK_BEFORE_3_HOURS, calculateTaskBeforeHourReminderAt(deadline));
        createOrUpdateTaskReminder(user, task, ReminderCode.TASK_OVERDUE, calculateTaskOverdueReminderAt(deadline));
    }

    @Override
    @Transactional
    public void cancelPendingTaskRemindersForAssignment(TaskAssignment assignment) {
        if (assignment == null || assignment.getTask() == null || assignment.getStudent() == null
                || assignment.getStudent().getUser() == null) {
            return;
        }

        Long userId = assignment.getStudent().getUser().getId();
        Long taskId = assignment.getTask().getId();
        if (userId == null || taskId == null) {
            return;
        }

        List<ReminderSchedule> reminders = reminderScheduleRepository.findByUserIdAndTargetTypeAndTargetIdAndStatusIn(
                userId,
                ReminderTargetType.TASK,
                taskId,
                EnumSet.of(ReminderStatus.PENDING, ReminderStatus.FAILED)
        );

        if (reminders.isEmpty()) {
            return;
        }

        for (ReminderSchedule reminder : reminders) {
            reminder.setStatus(ReminderStatus.CANCELLED);
            reminderRuntimeSchedulerService.cancelReminder(reminder.getId());
        }
        reminderScheduleRepository.saveAll(reminders);
        logger.info("Cancelled {} pending task reminders for assignment {}", reminders.size(), assignment.getId());
    }

    @Override
    @Transactional
    public void syncTaskRemindersForTask(ActivityTask task) {
        if (task == null || task.getId() == null) {
            return;
        }

        List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(task.getId());
        for (TaskAssignment assignment : assignments) {
            boolean hasSubmission = taskSubmissionRepository.findByTaskIdAndStudentIdAndIsDeletedFalse(
                    task.getId(),
                    assignment.getStudent().getId()
            ).isPresent();
            if (hasSubmission) {
                cancelPendingTaskRemindersForAssignment(assignment);
            } else {
                createTaskRemindersForAssignment(assignment);
            }
        }
    }

    private void createOrUpdateEventReminder(User user, Activity activity, ReminderCode reminderCode,
            LocalDateTime remindAt) {
        if (remindAt == null || !remindAt.isAfter(LocalDateTime.now())) {
            return;
        }

        Optional<ReminderSchedule> reminderOpt = reminderScheduleRepository
                .findByUserIdAndTargetTypeAndTargetIdAndReminderCode(
                        user.getId(),
                        ReminderTargetType.EVENT,
                        activity.getId(),
                        reminderCode);

        if (reminderOpt.isPresent() && reminderOpt.get().getStatus() == ReminderStatus.SENT) {
            return;
        }

        ReminderSchedule reminder = reminderOpt.orElseGet(ReminderSchedule::new);

        reminder.setUser(user);
        reminder.setTargetType(ReminderTargetType.EVENT);
        reminder.setTargetId(activity.getId());
        reminder.setReminderCode(reminderCode);
        reminder.setRemindAt(remindAt);
        reminder.setStatus(ReminderStatus.PENDING);
        reminder.setRecipientEmail(user.getEmail());
        reminder.setSubject(buildEventReminderSubject(reminderCode, activity));
        reminder.setContent(buildEventReminderContent(reminderCode, activity));
        reminder.setErrorMessage(null);
        reminder.setSentAt(null);

        reminder = reminderScheduleRepository.save(reminder);
        reminderRuntimeSchedulerService.scheduleReminder(reminder);
        logger.info("Prepared {} reminder for user {} and activity {}", reminderCode, user.getId(), activity.getId());
    }

    private void createOrUpdateTaskReminder(User user, ActivityTask task, ReminderCode reminderCode,
            LocalDateTime remindAt) {
        if (remindAt == null) {
            return;
        }

        boolean allowImmediateDispatch = reminderCode == ReminderCode.TASK_OVERDUE;
        if (!allowImmediateDispatch && !remindAt.isAfter(LocalDateTime.now())) {
            return;
        }

        Optional<ReminderSchedule> reminderOpt = reminderScheduleRepository
                .findByUserIdAndTargetTypeAndTargetIdAndReminderCode(
                        user.getId(),
                        ReminderTargetType.TASK,
                        task.getId(),
                        reminderCode);

        if (reminderOpt.isPresent() && reminderOpt.get().getStatus() == ReminderStatus.SENT) {
            return;
        }

        ReminderSchedule reminder = reminderOpt.orElseGet(ReminderSchedule::new);
        reminder.setUser(user);
        reminder.setTargetType(ReminderTargetType.TASK);
        reminder.setTargetId(task.getId());
        reminder.setReminderCode(reminderCode);
        reminder.setRemindAt(remindAt);
        reminder.setStatus(ReminderStatus.PENDING);
        reminder.setRecipientEmail(user.getEmail());
        reminder.setSubject(buildTaskReminderSubject(reminderCode, task));
        reminder.setContent(buildTaskReminderContent(reminderCode, task));
        reminder.setErrorMessage(null);
        reminder.setSentAt(null);

        reminder = reminderScheduleRepository.save(reminder);
        reminderRuntimeSchedulerService.scheduleReminder(reminder);
        logger.info("Prepared {} reminder for user {} and task {}", reminderCode, user.getId(), task.getId());
    }

    private String buildEventReminderSubject(ReminderCode reminderCode, Activity activity) {
        if (reminderCode == ReminderCode.BEFORE_1_DAY) {
            return "Nhac nho su kien truoc 1 ngay: " + activity.getName();
        }
        return "Nhac nho su kien truoc 1 gio: " + activity.getName();
    }

    private String buildEventReminderContent(ReminderCode reminderCode, Activity activity) {
        String timeText = reminderCode == ReminderCode.BEFORE_1_DAY ? "1 ngay" : "1 gio";
        String startText = activity.getStartDate() != null ? activity.getStartDate().toString() : "chua xac dinh";
        return "Su kien \"" + activity.getName() + "\" se dien ra sau " + timeText
                + ". Thoi gian bat dau: " + startText + ".";
    }

    private String buildTaskReminderSubject(ReminderCode reminderCode, ActivityTask task) {
        if (reminderCode == ReminderCode.TASK_BEFORE_1_DAY) {
            return "Nhac nho han nop bai truoc 1 ngay: " + task.getName();
        }
        if (reminderCode == ReminderCode.TASK_BEFORE_3_HOURS) {
            return "Nhac nho han nop bai truoc 3 gio: " + task.getName();
        }
        return "Thong bao bai da qua han: " + task.getName();
    }

    private String buildTaskReminderContent(ReminderCode reminderCode, ActivityTask task) {
        String deadlineText = task.getDeadline() != null ? task.getDeadline().toString() : "chua xac dinh";
        String activityName = task.getActivity() != null ? task.getActivity().getName() : "su kien";
        if (reminderCode == ReminderCode.TASK_OVERDUE) {
            return "Bai thu hoach/task \"" + task.getName() + "\" cua su kien \"" + activityName
                    + "\" da qua han nop. Han nop truoc do la: " + deadlineText
                    + ". Vui long kiem tra va hoan thanh som nhat co the.";
        }

        String timeText = reminderCode == ReminderCode.TASK_BEFORE_1_DAY ? "1 ngay" : "3 gio";
        return "Bai thu hoach/task \"" + task.getName() + "\" cua su kien \"" + activityName
                + "\" se den han sau " + timeText + ". Han nop: " + deadlineText + ".";
    }

    private LocalDateTime calculateEventBeforeDayReminderAt(LocalDateTime eventStart) {
        if (eventReminderBeforeMinutes > 0) {
            return eventStart.minusMinutes(eventReminderBeforeMinutes);
        }
        return eventStart.minusDays(eventReminderBeforeDays);
    }

    private LocalDateTime calculateEventBeforeHourReminderAt(LocalDateTime eventStart) {
        if (eventReminderBeforeMinutes > 0) {
            return eventStart.minusMinutes(eventReminderBeforeMinutes);
        }
        return eventStart.minusHours(eventReminderBeforeHours);
    }

    private LocalDateTime calculateTaskBeforeDayReminderAt(LocalDateTime deadline) {
        if (taskReminderBeforeMinutes > 0) {
            return deadline.minusMinutes(taskReminderBeforeMinutes);
        }
        return deadline.minusDays(taskReminderBeforeDays);
    }

    private LocalDateTime calculateTaskBeforeHourReminderAt(LocalDateTime deadline) {
        if (taskReminderBeforeMinutes > 0) {
            return deadline.minusMinutes(taskReminderBeforeMinutes);
        }
        return deadline.minusHours(taskReminderBeforeHours);
    }

    private LocalDateTime calculateTaskOverdueReminderAt(LocalDateTime deadline) {
        if (taskOverdueInitialMinutes > 0) {
            return deadline.plusMinutes(taskOverdueInitialMinutes);
        }
        return deadline.plusHours(taskOverdueInitialHours);
    }
}
