package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityReminder;
import vn.campuslife.entity.ActivitySeries;
import vn.campuslife.model.ActivityReminderRespone;
import vn.campuslife.model.CreateActivitySeriesRequest;
import vn.campuslife.model.Response;
import vn.campuslife.service.ActivityReminderService;
import vn.campuslife.service.ActivitySeriesService;

import java.util.List;

@RestController
@RequestMapping("/api/reminder")
@RequiredArgsConstructor
public class ActivityReminderController {
    private final ActivityReminderService reminderService;
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getRemindersByStudent(@PathVariable Long studentId) {
        List<ActivityReminderRespone> reminders = reminderService.getRemindersByStudent(studentId);
        return ResponseEntity.ok(reminders);
    }

}
