package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.NotificationStatus;
import vn.campuslife.enumeration.NotificationType;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDetailResponse {
    private Long id;
    private String title;
    private String content;
    private NotificationType type;
    private NotificationStatus status;
    private String actionUrl;
    private Map<String, Object> metadata; // Parsed from JSON string
    private Long activityId; // Optional, extracted from metadata
    private Long seriesId; // Optional, extracted from metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime readAt; // updatedAt when status = READ
}

