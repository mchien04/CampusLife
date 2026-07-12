package vn.campuslife.model.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.EventTimeStatus;
import vn.campuslife.enumeration.RegistrationStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalCalendarEventItem {
    private Long registrationId;
    private Long activityId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private RegistrationStatus status;
    private EventTimeStatus eventTimeStatus;
    private ActivityType activityType;
    private String bannerUrl;
    private String shareLink;
    private String ticketCode;
    private Long seriesId;
    @JsonProperty("important")
    private boolean important;
}
