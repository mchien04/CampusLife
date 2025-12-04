package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.NotificationType;
import vn.campuslife.enumeration.RecipientType;

import java.util.List;
import java.util.Map;

@Data
public class SendEmailRequest {
    private RecipientType recipientType; // required
    
    // Optional filters based on recipientType
    private List<Long> recipientIds; // For INDIVIDUAL or CUSTOM_LIST
    private Long activityId; // For ACTIVITY_REGISTRATIONS
    private Long seriesId; // For SERIES_REGISTRATIONS
    private Long classId; // For BY_CLASS
    private Long departmentId; // For BY_DEPARTMENT
    
    private String subject; // required
    private String content; // required - Text or HTML
    private Boolean isHtml = false; // default: false
    
    // Template variables (e.g., {{studentName}}, {{activityName}})
    private Map<String, String> templateVariables;
    
    // Notification options
    private Boolean createNotification = false; // default: false
    private String notificationTitle; // Optional
    private NotificationType notificationType; // Optional
    private String notificationActionUrl; // Optional
}

