package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.EmailHistory;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.entity.ActivitySeries;
import vn.campuslife.entity.ReminderSchedule;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.TaskAssignment;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.EmailStatus;
import vn.campuslife.enumeration.RecipientType;
import vn.campuslife.enumeration.ReminderCode;
import vn.campuslife.enumeration.ReminderStatus;
import vn.campuslife.enumeration.ReminderTargetType;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.Role;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.TaskStatus;
import vn.campuslife.repository.ActivityParticipationRepository;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.ActivitySeriesRepository;
import vn.campuslife.repository.EmailHistoryRepository;
import vn.campuslife.repository.ReminderScheduleRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.StudentSeriesProgressRepository;
import vn.campuslife.repository.TaskAssignmentRepository;
import vn.campuslife.repository.TaskSubmissionRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.service.ScoreRuleEngine;
import vn.campuslife.util.EmailUtil;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReminderDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(ReminderDispatchService.class);

    @Value("${app.reminder.task.overdue-repeat-days:1}")
    private long taskOverdueRepeatDays;

    @Value("${app.reminder.task.overdue-repeat-minutes:0}")
    private long taskOverdueRepeatMinutes;

    private final ReminderScheduleRepository reminderScheduleRepository;
    private final EmailHistoryRepository emailHistoryRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ActivityParticipationRepository activityParticipationRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final ActivitySeriesRepository activitySeriesRepository;
    private final StudentSeriesProgressRepository studentSeriesProgressRepository;
    private final ScoreRuleEngine scoreRuleEngine;
    private final EmailUtil emailUtil;
    private final ReminderRuntimeSchedulerService reminderRuntimeSchedulerService;

    @Transactional
    public void dispatchReminder(Long reminderId) {
        Optional<ReminderSchedule> reminderOpt = reminderScheduleRepository.findById(reminderId);
        if (reminderOpt.isEmpty()) {
            return;
        }

        ReminderSchedule reminder = reminderOpt.get();
        if (reminder.getStatus() != ReminderStatus.PENDING) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (reminder.getRemindAt() != null && reminder.getRemindAt().isAfter(now)) {
            return;
        }

        if (isTaskAlreadySubmitted(reminder)) {
            reminder.setStatus(ReminderStatus.CANCELLED);
            reminder.setErrorMessage(null);
            reminderScheduleRepository.save(reminder);
            logger.info("Cancelled reminder {} because the task was already submitted", reminderId);
            return;
        }

        if (isNoShowReminderCancelled(reminder)) {
            reminder.setStatus(ReminderStatus.CANCELLED);
            reminder.setErrorMessage(null);
            reminderScheduleRepository.save(reminder);
            logger.info("Cancelled reminder {} because the student already attended or no registration exists",
                    reminderId);
            return;
        }

        if (isTaskOverdueReminderCancelled(reminder)) {
            reminder.setStatus(ReminderStatus.CANCELLED);
            reminder.setErrorMessage(null);
            reminderScheduleRepository.save(reminder);
            logger.info("Cancelled reminder {} because task overdue does not apply for this activity", reminderId);
            return;
        }

        if (isSeriesMinimumReminderCancelled(reminder)) {
            reminder.setStatus(ReminderStatus.CANCELLED);
            reminder.setErrorMessage(null);
            reminderScheduleRepository.save(reminder);
            logger.info("Cancelled reminder {} because the series minimum requirement no longer applies", reminderId);
            return;
        }

        if (isNoShowPenaltyReminder(reminder)) {
            applyNoShowPenalty(reminder);
        }
        if (isTaskOverdueReminder(reminder)) {
            applyTaskOverdue(reminder);
        }
        if (isSeriesMinimumRequirementReminder(reminder)) {
            applySeriesMinimumRequirement(reminder);
        }

        User sender = resolveSystemSender();
        if (sender == null) {
            reminder.setStatus(ReminderStatus.FAILED);
            reminder.setErrorMessage("No active sender user available for reminder email");
            reminderScheduleRepository.save(reminder);
            logger.error("Failed to send reminder {} because no sender user is available", reminderId);
            return;
        }

        boolean sent = emailUtil.sendCustomEmail(
                reminder.getRecipientEmail(),
                reminder.getSubject(),
                reminder.getContent(),
                false,
                Collections.emptyList());

        EmailHistory emailHistory = new EmailHistory();
        emailHistory.setSender(sender);
        emailHistory.setRecipient(reminder.getUser());
        emailHistory.setRecipientEmail(reminder.getRecipientEmail());
        emailHistory.setSubject(reminder.getSubject());
        emailHistory.setContent(reminder.getContent());
        emailHistory.setHtml(false);
        emailHistory.setRecipientType(RecipientType.BULK);
        emailHistory.setRecipientFilter(buildRecipientFilter(reminder));
        emailHistory.setAttachmentCount(0);
        emailHistory.setSentAt(now);
        emailHistory.setStatus(sent ? EmailStatus.SUCCESS : EmailStatus.FAILED);
        if (!sent) {
            emailHistory.setErrorMessage("Failed to send reminder email");
        }
        emailHistoryRepository.save(emailHistory);

        if (shouldRepeatOverdueReminder(reminder) && !isTaskAlreadySubmitted(reminder)) {
            LocalDateTime nextRemindAt = (reminder.getRemindAt() != null && reminder.getRemindAt().isAfter(now)
                    ? reminder.getRemindAt()
                    : now);
            nextRemindAt = taskOverdueRepeatMinutes > 0
                    ? nextRemindAt.plusMinutes(taskOverdueRepeatMinutes)
                    : nextRemindAt.plusDays(taskOverdueRepeatDays);
            reminder.setStatus(ReminderStatus.PENDING);
            reminder.setRemindAt(nextRemindAt);
            reminder.setSentAt(null);
            reminder.setErrorMessage(sent ? null : "Last overdue reminder send failed; will retry on next cycle");
            logger.info("Rescheduled overdue reminder {} for {}", reminderId, nextRemindAt);
        } else {
            reminder.setStatus(sent ? ReminderStatus.SENT : ReminderStatus.FAILED);
            reminder.setSentAt(sent ? now : null);
            reminder.setErrorMessage(sent ? null : "Failed to send reminder email");
        }
        reminder = reminderScheduleRepository.save(reminder);
        if (reminder.getStatus() == ReminderStatus.PENDING) {
            reminderRuntimeSchedulerService.scheduleReminder(reminder);
        }

        if (sent) {
            logger.info("Sent reminder email {} to {}", reminderId, reminder.getRecipientEmail());
        } else {
            logger.warn("Failed to send reminder email {} to {}", reminderId, reminder.getRecipientEmail());
        }
    }

    private User resolveSystemSender() {
        List<User> privilegedUsers = userRepository.findAllByRoleInAndIsDeletedFalse(List.of(Role.ADMIN, Role.MANAGER));
        if (!privilegedUsers.isEmpty()) {
            return privilegedUsers.get(0);
        }

        return userRepository.findAll().stream()
                .filter(user -> !user.isDeleted())
                .findFirst()
                .orElse(null);
    }

    private String buildRecipientFilter(ReminderSchedule reminder) {
        return "{\"targetType\":\"" + reminder.getTargetType().name()
                + "\",\"targetId\":" + reminder.getTargetId()
                + ",\"reminderCode\":\"" + reminder.getReminderCode().name() + "\"}";
    }

    private boolean shouldRepeatOverdueReminder(ReminderSchedule reminder) {
        return reminder.getTargetType() == ReminderTargetType.TASK
                && reminder.getReminderCode() == ReminderCode.TASK_OVERDUE;
    }

    private boolean isTaskOverdueReminder(ReminderSchedule reminder) {
        return reminder.getTargetType() == ReminderTargetType.TASK
                && reminder.getReminderCode() == ReminderCode.TASK_OVERDUE;
    }

    private boolean isNoShowPenaltyReminder(ReminderSchedule reminder) {
        return reminder.getTargetType() == ReminderTargetType.EVENT
                && reminder.getReminderCode() == ReminderCode.EVENT_NO_SHOW_PENALTY;
    }

    private boolean isSeriesMinimumRequirementReminder(ReminderSchedule reminder) {
        return reminder.getTargetType() == ReminderTargetType.SERIES
                && reminder.getReminderCode() == ReminderCode.SERIES_MINIMUM_REQUIREMENT;
    }

    private boolean isTaskAlreadySubmitted(ReminderSchedule reminder) {
        if (reminder.getTargetType() != ReminderTargetType.TASK || reminder.getUser() == null
                || reminder.getUser().getId() == null) {
            return false;
        }

        return studentRepository.findByUserIdAndIsDeletedFalse(reminder.getUser().getId())
                .flatMap(student -> taskSubmissionRepository.findByTaskIdAndStudentIdAndIsDeletedFalse(
                        reminder.getTargetId(),
                        student.getId()))
                .isPresent();
    }

    private boolean isNoShowReminderCancelled(ReminderSchedule reminder) {
        if (!isNoShowPenaltyReminder(reminder) || reminder.getUser() == null || reminder.getUser().getId() == null) {
            return false;
        }

        Optional<Student> studentOpt = studentRepository.findByUserIdAndIsDeletedFalse(reminder.getUser().getId());
        if (studentOpt.isEmpty()) {
            return true;
        }

        Optional<ActivityRegistration> registrationOpt = activityRegistrationRepository.findByActivityIdAndStudentId(
                reminder.getTargetId(),
                studentOpt.get().getId());
        if (registrationOpt.isEmpty()) {
            return true;
        }

        ActivityRegistration registration = registrationOpt.get();
        if (registration.getStatus() != null
                && registration.getStatus() != vn.campuslife.enumeration.RegistrationStatus.APPROVED) {
            return true;
        }

        Optional<ActivityParticipation> participationOpt = activityParticipationRepository
                .findByRegistration(registration);
        if (participationOpt.isEmpty()) {
            return false;
        }

        ActivityParticipation participation = participationOpt.get();
        return EnumSet.of(
                ParticipationType.ATTENDED,
                ParticipationType.COMPLETED)
                .contains(participation.getParticipationType())
                || registration.getStatus() == RegistrationStatus.ATTENDED
                || participation.getCheckOutTime() != null
                || Boolean.TRUE.equals(participation.getIsCompleted());
    }

    private boolean isTaskOverdueReminderCancelled(ReminderSchedule reminder) {
        if (!isTaskOverdueReminder(reminder) || reminder.getUser() == null || reminder.getUser().getId() == null) {
            return false;
        }

        Optional<Student> studentOpt = studentRepository.findByUserIdAndIsDeletedFalse(reminder.getUser().getId());
        if (studentOpt.isEmpty()) {
            return true;
        }

        Optional<TaskAssignment> assignmentOpt = taskAssignmentRepository.findByTaskIdAndStudentId(
                reminder.getTargetId(),
                studentOpt.get().getId());
        if (assignmentOpt.isEmpty() || assignmentOpt.get().getTask() == null
                || assignmentOpt.get().getTask().getActivity() == null) {
            return true;
        }

        Activity activity = assignmentOpt.get().getTask().getActivity();
        return !activity.isRequiresSubmission() || activity.getSeriesId() != null;
    }

    private boolean isSeriesMinimumReminderCancelled(ReminderSchedule reminder) {
        if (!isSeriesMinimumRequirementReminder(reminder) || reminder.getUser() == null
                || reminder.getUser().getId() == null) {
            return false;
        }

        Optional<Student> studentOpt = studentRepository.findByUserIdAndIsDeletedFalse(reminder.getUser().getId());
        Optional<ActivitySeries> seriesOpt = activitySeriesRepository.findById(reminder.getTargetId());
        if (studentOpt.isEmpty() || seriesOpt.isEmpty() || seriesOpt.get().isDeleted()) {
            return true;
        }

        ActivitySeries series = seriesOpt.get();
        if (!series.isMinimumRequirementEnabled()
                || series.getMinimumRequiredEvents() == null
                || series.getMinimumRequiredEvents() <= 0
                || series.getMinimumPenaltyPoints() == null
                || series.getMinimumPenaltyPoints() <= 0) {
            return true;
        }

        return activityRegistrationRepository.findBySeriesIdAndStudentId(series.getId(), studentOpt.get().getId())
                .stream()
                .noneMatch(reg -> reg.getStatus() == RegistrationStatus.APPROVED
                        || reg.getStatus() == RegistrationStatus.ATTENDED);
    }

    private void applyNoShowPenalty(ReminderSchedule reminder) {
        Optional<Student> studentOpt = studentRepository.findByUserIdAndIsDeletedFalse(reminder.getUser().getId());
        if (studentOpt.isEmpty()) {
            return;
        }

        Optional<ActivityRegistration> registrationOpt = activityRegistrationRepository.findByActivityIdAndStudentId(
                reminder.getTargetId(),
                studentOpt.get().getId());
        if (registrationOpt.isEmpty()) {
            return;
        }

        scoreRuleEngine.applyNoShowPenalty(registrationOpt.get(), resolveSystemSender());
    }

    private void applyTaskOverdue(ReminderSchedule reminder) {
        Optional<Student> studentOpt = studentRepository.findByUserIdAndIsDeletedFalse(reminder.getUser().getId());
        if (studentOpt.isEmpty()) {
            return;
        }

        Optional<TaskAssignment> assignmentOpt = taskAssignmentRepository.findByTaskIdAndStudentId(
                reminder.getTargetId(),
                studentOpt.get().getId());
        if (assignmentOpt.isEmpty()) {
            return;
        }

        TaskAssignment assignment = assignmentOpt.get();
        if (assignment.getTask() == null || assignment.getTask().getActivity() == null
                || !assignment.getTask().getActivity().isRequiresSubmission()
                || assignment.getTask().getActivity().getSeriesId() != null) {
            return;
        }
        if (assignment.getStatus() != TaskStatus.COMPLETED && assignment.getStatus() != TaskStatus.OVERDUE) {
            assignment.setStatus(TaskStatus.OVERDUE);
            taskAssignmentRepository.save(assignment);
        }
        scoreRuleEngine.applyTaskOverdue(assignment, resolveSystemSender());
    }

    private void applySeriesMinimumRequirement(ReminderSchedule reminder) {
        Optional<Student> studentOpt = studentRepository.findByUserIdAndIsDeletedFalse(reminder.getUser().getId());
        Optional<ActivitySeries> seriesOpt = activitySeriesRepository.findById(reminder.getTargetId());
        if (studentOpt.isEmpty() || seriesOpt.isEmpty()) {
            return;
        }

        int completedCount = studentSeriesProgressRepository
                .findByStudentIdAndSeriesId(studentOpt.get().getId(), seriesOpt.get().getId())
                .map(progress -> progress.getCompletedCount() != null ? progress.getCompletedCount() : 0)
                .orElse(0);
        scoreRuleEngine.applySeriesMinimumRequirement(seriesOpt.get(), studentOpt.get(), completedCount,
                resolveSystemSender());
    }
}
