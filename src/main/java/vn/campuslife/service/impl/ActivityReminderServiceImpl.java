package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.ActivityReminder;
import vn.campuslife.model.ActivityRegistrationResponse;
import vn.campuslife.model.ActivityReminderRespone;
import vn.campuslife.repository.ActivityReminderRepository;
import vn.campuslife.service.ActivityReminderService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityReminderServiceImpl implements ActivityReminderService {
    private final ActivityReminderRepository reminderRepository;

    @Override
    public List<ActivityReminderRespone> getRemindersByStudent(Long studentId) {

        List<ActivityReminder> reminders = reminderRepository.findByRegistrationStudentId(studentId);

        return reminders.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ActivityReminderRespone toResponse(ActivityReminder reminder) {
        ActivityReminderRespone dto = new ActivityReminderRespone();
        dto.setReminderId(reminder.getId());
        dto.setRemind1Day(reminder.isRemind1Day());
        dto.setRemind1Hour(reminder.isRemind1Hour());

        ActivityRegistration res = reminder.getRegistration();
        dto.setRegistrationId(res.getId());

        // Lấy activity
        Activity activity = reminder.getRegistration().getActivity();
        dto.setEventId(activity.getId());
        dto.setEventName(activity.getName());
        dto.setLocation(activity.getLocation());
        dto.setStartDate(activity.getStartDate().toString());



        return dto;
    }


}
