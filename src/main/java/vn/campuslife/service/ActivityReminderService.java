package vn.campuslife.service;

import vn.campuslife.model.ActivityReminderRespone;
import java.util.List;

public interface ActivityReminderService {
    List<ActivityReminderRespone> getRemindersByStudent(Long studentId);
}
