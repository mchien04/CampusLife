package vn.campuslife.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Notification;
import vn.campuslife.entity.Department;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.NotificationStatus;
import vn.campuslife.enumeration.NotificationType;
import vn.campuslife.model.DeviceToken;
import vn.campuslife.model.NotificationDetailResponse;
import vn.campuslife.model.Response;
import vn.campuslife.repository.*;
import vn.campuslife.service.FcmService;
import vn.campuslife.service.NotificationService;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentClassRepository studentClassRepository;
    private final DepartmentRepository departmentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FcmService fcmService;
    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    @Transactional
    public Response sendNotification(Long userId, String title, String content, NotificationType type, String actionUrl,
            Map<String, Object> metadata) {
        return sendNotification(userId, title, content, type, actionUrl, metadata, null, null);
    }

    @Override
    @Transactional
    public Response sendNotification(Long userId, String title, String content, NotificationType type, String actionUrl,
            Map<String, Object> metadata, Long senderDepartmentId, Set<Long> targetDepartmentIds) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return new Response(false, "User not found", null);
            }

            Notification notification = new Notification();
            notification.setUser(userOpt.get());
            notification.setTitle(title);
            notification.setContent(content);
            notification.setType(type);
            notification.setActionUrl(actionUrl);
            notification.setStatus(NotificationStatus.UNREAD);
            applyDepartmentMetadata(notification, senderDepartmentId, targetDepartmentIds);

            if (metadata != null && !metadata.isEmpty()) {
                try {
                    notification.setMetadata(objectMapper.writeValueAsString(metadata));
                } catch (Exception e) {
                    notification.setMetadata(metadata.toString());
                }
            }
            notificationRepository.save(notification);

            Map<String, String> data = new HashMap<>();
            data.put("notificationId", notification.getId().toString());
            data.put("type", notification.getType().name());
            if (actionUrl != null) {
                data.put("actionUrl", actionUrl);
            }

            List<DeviceToken> tokens = deviceTokenRepository.findAllByUserId(userId);

            for (DeviceToken dt : tokens) {
                fcmService.send(
                        dt.getToken(),
                        notification.getTitle(),
                        notification.getContent(),
                        data
                );
            }
            return new Response(true, "Notification sent successfully", notification);
        } catch (Exception e) {
            return new Response(false, "Failed to send notification: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response sendBulkNotification(
            List<Long> userIds,
            String title,
            String content,
            NotificationType type,
            String actionUrl,
            Map<String, Object> metadata
    ) {
        try {
            List<Notification> savedNotifications = new ArrayList<>();

            for (Long userId : userIds) {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isEmpty()) continue;

                // 1️⃣ TẠO & SAVE TRƯỚC
                Notification notification = new Notification();
                notification.setUser(userOpt.get());
                notification.setTitle(title);
                notification.setContent(content);
                notification.setType(type);
                notification.setActionUrl(actionUrl);
                notification.setStatus(NotificationStatus.UNREAD);

                if (metadata != null && !metadata.isEmpty()) {
                    notification.setMetadata(objectMapper.writeValueAsString(metadata));
                }

                notification = notificationRepository.save(notification);
                savedNotifications.add(notification);

                Map<String, String> data = new HashMap<>();
                data.put("notificationId", notification.getId().toString());
                data.put("type", notification.getType().name());
                if (actionUrl != null) {
                    data.put("actionUrl", actionUrl);
                }
                List<DeviceToken> tokens =
                        deviceTokenRepository.findAllByUserId(userId);

                for (DeviceToken dt : tokens) {
                    fcmService.send(
                            dt.getToken(),
                            notification.getTitle(),
                            notification.getContent(),
                            data
                    );
                }
            }

            return new Response(true, "Bulk notification sent successfully", savedNotifications);

        } catch (Exception e) {
            return new Response(false, "Failed to send bulk notification: " + e.getMessage(), null);
        }
    }

    /**
     * Async version of bulk notification - saves notifications synchronously,
     * then sends FCM pushes asynchronously in parallel.
     */
    @Async("notificationExecutor")
    @Transactional
    public void sendBulkNotificationAsync(
            List<Long> userIds,
            String title,
            String content,
            NotificationType type,
            String actionUrl,
            Map<String, Object> metadata
    ) {
        logger.info("Starting async bulk notification to {} users", userIds.size());
        int successCount = 0;
        int failCount = 0;

        for (Long userId : userIds) {
            try {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isEmpty()) continue;

                Notification notification = new Notification();
                notification.setUser(userOpt.get());
                notification.setTitle(title);
                notification.setContent(content);
                notification.setType(type);
                notification.setActionUrl(actionUrl);
                notification.setStatus(NotificationStatus.UNREAD);

                if (metadata != null && !metadata.isEmpty()) {
                    notification.setMetadata(objectMapper.writeValueAsString(metadata));
                }

                notification = notificationRepository.save(notification);
                successCount++;

                Map<String, String> data = new HashMap<>();
                data.put("notificationId", notification.getId().toString());
                data.put("type", notification.getType().name());
                if (actionUrl != null) {
                    data.put("actionUrl", actionUrl);
                }

                List<DeviceToken> tokens = deviceTokenRepository.findAllByUserId(userId);
                for (DeviceToken dt : tokens) {
                    try {
                        fcmService.send(dt.getToken(), notification.getTitle(), notification.getContent(), data);
                    } catch (Exception fcmEx) {
                        logger.warn("FCM send failed for user {}: {}", userId, fcmEx.getMessage());
                        failCount++;
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to send async notification to user {}: {}", userId, e.getMessage());
                failCount++;
            }
        }

        logger.info("Async bulk notification completed: {} success, {} failures", successCount, failCount);
    }


    @Override
    @Transactional
    public Response sendNotificationToDepartment(Long departmentId, String title, String content, NotificationType type,
            String actionUrl, Map<String, Object> metadata) {
        try {
            List<Long> userIds = studentRepository.findUserIdsByDepartmentId(departmentId);

            return sendBulkNotification(userIds, title, content, type, actionUrl, metadata);
        } catch (Exception e) {
            return new Response(false, "Failed to send notification to department: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response sendNotificationToClass(Long classId, String title, String content, NotificationType type,
            String actionUrl, Map<String, Object> metadata) {
        try {
            List<Long> userIds = studentRepository.findUserIdsByClassId(classId);
            return sendBulkNotification(userIds, title, content, type, actionUrl, metadata);
        } catch (Exception e) {
            return new Response(false, "Failed to send notification to class: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getUserNotifications(Long userId, Pageable pageable) {
        try {
            Page<Notification> notifications = notificationRepository
                    .findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);
            return new Response(true, "Notifications retrieved successfully", notifications);
        } catch (Exception e) {
            return new Response(false, "Failed to get notifications: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getUnreadNotifications(Long userId) {
        try {
            List<Notification> notifications = notificationRepository
                    .findByUserIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD);
            return new Response(true, "Unread notifications retrieved successfully", notifications);
        } catch (Exception e) {
            return new Response(false, "Failed to get unread notifications: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response markAsRead(Long notificationId, Long userId) {
        try {
            Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
            if (notificationOpt.isEmpty()) {
                return new Response(false, "Notification not found", null);
            }

            Notification notification = notificationOpt.get();
            if (!notification.getUser().getId().equals(userId)) {
                return new Response(false, "Unauthorized to access this notification", null);
            }

            notification.setStatus(NotificationStatus.READ);
            notificationRepository.save(notification);
            return new Response(true, "Notification marked as read", notification);
        } catch (Exception e) {
            return new Response(false, "Failed to mark notification as read: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response markAllAsRead(Long userId) {
        try {
            List<Notification> unreadNotifications = notificationRepository
                    .findByUserIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD);
            for (Notification notification : unreadNotifications) {
                notification.setStatus(NotificationStatus.READ);
            }
            notificationRepository.saveAll(unreadNotifications);
            return new Response(true, "All notifications marked as read", null);
        } catch (Exception e) {
            return new Response(false, "Failed to mark all notifications as read: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getUnreadCount(Long userId) {
        try {
            Long count = notificationRepository.countUnreadByUserId(userId);
            return new Response(true, "Unread count retrieved successfully", count);
        } catch (Exception e) {
            return new Response(false, "Failed to get unread count: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response deleteNotification(Long notificationId, Long userId) {
        try {
            Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
            if (notificationOpt.isEmpty()) {
                return new Response(false, "Notification not found", null);
            }

            Notification notification = notificationOpt.get();
            if (!notification.getUser().getId().equals(userId)) {
                return new Response(false, "Unauthorized to delete this notification", null);
            }

            notification.setDeleted(true);
            notificationRepository.save(notification);
            return new Response(true, "Notification deleted successfully", null);
        } catch (Exception e) {
            return new Response(false, "Failed to delete notification: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response archiveNotification(Long notificationId, Long userId) {
        try {
            Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
            if (notificationOpt.isEmpty()) {
                return new Response(false, "Notification not found", null);
            }

            Notification notification = notificationOpt.get();
            if (!notification.getUser().getId().equals(userId)) {
                return new Response(false, "Unauthorized to archive this notification", null);
            }

            notification.setStatus(NotificationStatus.ARCHIVED);
            notificationRepository.save(notification);
            return new Response(true, "Notification archived successfully", notification);
        } catch (Exception e) {
            return new Response(false, "Failed to archive notification: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getNotificationDetail(Long notificationId, Long userId) {
        try {
            Optional<Notification> notificationOpt = notificationRepository.findByIdAndUserId(notificationId, userId);
            if (notificationOpt.isEmpty()) {
                return new Response(false, "Notification not found or unauthorized", null);
            }

            Notification notification = notificationOpt.get();
            NotificationDetailResponse response = toDetailResponse(notification);
            return new Response(true, "Notification detail retrieved successfully", response);
        } catch (Exception e) {
            return new Response(false, "Failed to get notification detail: " + e.getMessage(), null);
        }
    }

    private NotificationDetailResponse toDetailResponse(Notification notification) {
        NotificationDetailResponse response = new NotificationDetailResponse();
        response.setId(notification.getId());
        response.setTitle(notification.getTitle());
        response.setContent(notification.getContent());
        response.setType(notification.getType());
        response.setStatus(notification.getStatus());
        response.setActionUrl(notification.getActionUrl());
        response.setCreatedAt(notification.getCreatedAt());
        response.setUpdatedAt(notification.getUpdatedAt());
        
        // Parse metadata from JSON string
        Map<String, Object> metadata = null;
        if (notification.getMetadata() != null && !notification.getMetadata().trim().isEmpty()) {
            try {
                metadata = objectMapper.readValue(notification.getMetadata(), 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                // If parsing fails, metadata remains null
                metadata = null;
            }
        }
        response.setMetadata(metadata);
        
        // Extract activityId and seriesId from metadata
        if (metadata != null) {
            if (metadata.containsKey("activityId")) {
                Object activityIdObj = metadata.get("activityId");
                if (activityIdObj instanceof Number) {
                    response.setActivityId(((Number) activityIdObj).longValue());
                }
            }
            if (metadata.containsKey("seriesId")) {
                Object seriesIdObj = metadata.get("seriesId");
                if (seriesIdObj instanceof Number) {
                    response.setSeriesId(((Number) seriesIdObj).longValue());
                }
            }
        }
        
        // Set readAt to updatedAt when status is READ
        if (notification.getStatus() == NotificationStatus.READ) {
            response.setReadAt(notification.getUpdatedAt());
        }
        
        return response;
    }

    private void applyDepartmentMetadata(Notification notification, Long senderDepartmentId,
                                         Set<Long> targetDepartmentIds) {
        if (senderDepartmentId != null) {
            departmentRepository.findById(senderDepartmentId).ifPresent(notification::setSenderDepartment);
        }
        if (targetDepartmentIds != null && !targetDepartmentIds.isEmpty()) {
            Set<Department> targets = new LinkedHashSet<>();
            for (Long departmentId : targetDepartmentIds) {
                departmentRepository.findById(departmentId).ifPresent(targets::add);
            }
            notification.setTargetDepartments(targets);
        }
    }
}
