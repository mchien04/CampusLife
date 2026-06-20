package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.EmailHistory;
import vn.campuslife.entity.ReminderSchedule;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.EmailStatus;
import vn.campuslife.enumeration.RecipientType;
import vn.campuslife.enumeration.ReminderCode;
import vn.campuslife.enumeration.ReminderStatus;
import vn.campuslife.enumeration.ReminderTargetType;
import vn.campuslife.enumeration.Role;
import vn.campuslife.repository.EmailHistoryRepository;
import vn.campuslife.repository.ReminderScheduleRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.TaskSubmissionRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.util.EmailUtil;

import java.time.LocalDateTime;
import java.util.Collections;
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
                Collections.emptyList()
        );

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
}
