package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.NotificationType;
import vn.campuslife.enumeration.RecipientType;

import java.util.List;

@Data
public class SendNotificationOnlyRequest {
    private RecipientType recipientType; // required
    
    // Optional filters based on recipientType
    private List<Long> recipientIds; // For INDIVIDUAL or CUSTOM_LIST
    private Long activityId; // For ACTIVITY_REGISTRATIONS
    private Long seriesId; // For SERIES_REGISTRATIONS
    private Long classId; // For BY_CLASS
    private Long departmentId; // For BY_DEPARTMENT
    
    private String title; // required
    private String content; // required
    private NotificationType type; // required
    private String actionUrl; // optional
}

