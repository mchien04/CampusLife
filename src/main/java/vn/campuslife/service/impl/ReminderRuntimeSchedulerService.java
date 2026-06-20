package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import vn.campuslife.entity.ReminderSchedule;
import vn.campuslife.enumeration.ReminderStatus;
import vn.campuslife.job.ReminderQuartzJob;
import vn.campuslife.repository.ReminderScheduleRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderRuntimeSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(ReminderRuntimeSchedulerService.class);
    private static final String JOB_GROUP = "reminder-email-jobs";
    private static final String TRIGGER_GROUP = "reminder-email-triggers";

    private final Scheduler quartzScheduler;
    private final ReminderScheduleRepository reminderScheduleRepository;

    public void scheduleReminder(ReminderSchedule reminder) {
        if (reminder == null || reminder.getId() == null || reminder.getStatus() != ReminderStatus.PENDING
                || reminder.getRemindAt() == null) {
            return;
        }

        try {
            JobKey jobKey = jobKey(reminder.getId());
            deleteReminderJob(jobKey);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fireAt = reminder.getRemindAt().isAfter(now)
                    ? reminder.getRemindAt()
                    : now.plusSeconds(1);

            JobDetail jobDetail = JobBuilder.newJob(ReminderQuartzJob.class)
                    .withIdentity(jobKey)
                    .usingJobData(ReminderQuartzJob.REMINDER_ID_KEY, reminder.getId())
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey(reminder.getId()))
                    .forJob(jobDetail)
                    .startAt(toDate(fireAt))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withRepeatCount(0)
                            .withMisfireHandlingInstructionFireNow())
                    .build();

            quartzScheduler.scheduleJob(jobDetail, trigger);
            logger.info("Scheduled Quartz reminder {} for {}", reminder.getId(), fireAt);
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to schedule Quartz reminder " + reminder.getId(), e);
        }
    }

    public void cancelReminder(Long reminderId) {
        if (reminderId == null) {
            return;
        }

        try {
            deleteReminderJob(jobKey(reminderId));
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to cancel Quartz reminder " + reminderId, e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reconcilePendingReminders() {
        List<ReminderSchedule> pendingReminders = reminderScheduleRepository.findByStatus(ReminderStatus.PENDING);
        for (ReminderSchedule reminder : pendingReminders) {
            scheduleReminder(reminder);
        }
        logger.info("Reconciled {} pending reminders with Quartz", pendingReminders.size());
    }

    private void deleteReminderJob(JobKey jobKey) throws SchedulerException {
        if (quartzScheduler.checkExists(jobKey)) {
            quartzScheduler.deleteJob(jobKey);
            logger.info("Deleted existing Quartz reminder job {}", jobKey);
        }
    }

    private JobKey jobKey(Long reminderId) {
        return new JobKey("reminder-" + reminderId, JOB_GROUP);
    }

    private TriggerKey triggerKey(Long reminderId) {
        return new TriggerKey("reminder-trigger-" + reminderId, TRIGGER_GROUP);
    }

    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
