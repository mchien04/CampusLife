package vn.campuslife.service;

import vn.campuslife.entity.ActivityTask;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.TaskAssignment;

public interface ReminderScheduleService {

    void createEventRemindersForApprovedRegistration(ActivityRegistration registration);

    void cancelPendingEventRemindersForRegistration(ActivityRegistration registration);

    void syncEventRemindersForActivity(vn.campuslife.entity.Activity activity);

    void createTaskRemindersForAssignment(TaskAssignment assignment);

    void cancelPendingTaskRemindersForAssignment(TaskAssignment assignment);

    void syncTaskRemindersForTask(ActivityTask task);
}
