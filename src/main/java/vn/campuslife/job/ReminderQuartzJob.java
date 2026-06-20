package vn.campuslife.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import vn.campuslife.service.impl.ReminderDispatchService;

@DisallowConcurrentExecution
public class ReminderQuartzJob implements Job {

    public static final String REMINDER_ID_KEY = "reminderId";

    @Autowired
    private ReminderDispatchService reminderDispatchService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        long reminderId = dataMap.getLongValue(REMINDER_ID_KEY);
        if (reminderId <= 0) {
            throw new JobExecutionException("Missing or invalid reminderId in Quartz job data");
        }

        reminderDispatchService.dispatchReminder(reminderId);
    }
}
