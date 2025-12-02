package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.model.ActivityParticipationResponse;
import vn.campuslife.service.ActivityParticipationService;

@RestController
@RequestMapping("/api/participations")
@RequiredArgsConstructor
public class ActivityParticipationController {
    private final ActivityParticipationService participationService;

    @GetMapping("/student/{studentId}/activity/{activityId}")
    public ResponseEntity<?> getParticipation(
            @PathVariable Long studentId,
            @PathVariable Long activityId
    ) {
        ActivityParticipation p =
                participationService.getParticipation(studentId, activityId);

        if (p == null) {
            return ResponseEntity.ok("No participation found");
        }

        var r = p.getRegistration();
        var a = r.getActivity();
        var s = r.getStudent();

        ActivityParticipationResponse dto = new ActivityParticipationResponse(
                p.getId(),                 // id
                a.getId(),                 // activityId
                a.getName(),               // activityName
                s.getId(),                 // studentId
                s.getFullName(),               // studentName
                s.getStudentCode(),        // studentCode
                p.getParticipationType(),  // participationType (Enum)
                p.getPointsEarned(),       // pointsEarned
                p.getDate(),               // date
                p.getIsCompleted(),        // isCompleted
                p.getCheckInTime(),        // checkInTime
                p.getCheckOutTime()      // checkOutTime

        );

        return ResponseEntity.ok(dto);
    }
}
