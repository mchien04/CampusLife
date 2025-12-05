package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.NotificationType;
import vn.campuslife.enumeration.RecipientType;

import java.util.List;
import java.util.Map;

@Data
public class SendNotificationOnlyRequest {
    private RecipientType recipientType; // required
    
    // Optional filters based on recipientType
    private List<Long> recipientIds; // Required for BULK (có thể 1 hoặc nhiều)
    private Long activityId; // Required for ACTIVITY_REGISTRATIONS
    private Long seriesId; // Required for SERIES_REGISTRATIONS
    private Long classId; // Required for BY_CLASS
    private Long departmentId; // Required for BY_DEPARTMENT
    
    private String title; // required
    private String content; // required - Text or HTML (có thể dùng template variables)
    private NotificationType type; // required
    private String actionUrl; // optional
    
    // Template variables (optional) - để thay thế {{variableName}} trong content
    private Map<String, String> templateVariables; // optional
}

