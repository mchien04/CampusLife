package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityReminderRespone {
    private Long reminderId;
    private Long registrationId;
    private Long eventId;
    private String eventName;
    private String location;
    private String startDate;

    private boolean remind1Day;
    private boolean remind1Hour;
}
