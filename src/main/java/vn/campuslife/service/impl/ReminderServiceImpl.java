package vn.campuslife.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.Notification;
import vn.campuslife.enumeration.NotificationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.model.Response;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.NotificationRepository;
import vn.campuslife.service.ReminderService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    private static final Logger logger = LoggerFactory.getLogger(ReminderServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ActivityRegistrationRepository registrationRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public Response sendReminderNotifications() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneDayLater = now.plusDays(1);
            LocalDateTime oneHourLater = now.plusHours(1);

            // Query registrations for 1-day reminders
            List<ActivityRegistration> oneDayRegistrations = registrationRepository
                    .findRegistrationsFor1DayReminder(RegistrationStatus.APPROVED, now, oneDayLater);

            // Query registrations for 1-hour reminders
            List<ActivityRegistration> oneHourRegistrations = registrationRepository
                    .findRegistrationsFor1HourReminder(RegistrationStatus.APPROVED, now, oneHourLater);

            int oneDayCount = 0;
            int oneHourCount = 0;

            // Send 1-day reminders
            for (ActivityRegistration registration : oneDayRegistrations) {
                if (!hasReminderBeenSent(registration.getStudent().getUser().getId(),
                        registration.getActivity().getId(), "1_DAY")) {
                    sendReminderNotification(registration, NotificationType.REMINDER_1_DAY, "1 ngày");
                    oneDayCount++;
                }
            }

            // Send 1-hour reminders
            for (ActivityRegistration registration : oneHourRegistrations) {
                if (!hasReminderBeenSent(registration.getStudent().getUser().getId(),
                        registration.getActivity().getId(), "1_HOUR")) {
                    sendReminderNotification(registration, NotificationType.REMINDER_1_HOUR, "1 giờ");
                    oneHourCount++;
                }
            }

            logger.info("Reminder notifications sent: {} (1-day), {} (1-hour)", oneDayCount, oneHourCount);
            return Response.success(
                    String.format("Sent %d one-day reminders and %d one-hour reminders", oneDayCount, oneHourCount),
                    Map.of("oneDayCount", oneDayCount, "oneHourCount", oneHourCount));
        } catch (Exception e) {
            logger.error("Failed to send reminder notifications: {}", e.getMessage(), e);
            return Response.error("Failed to send reminder notifications: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra xem reminder đã được gửi chưa
     */
    private boolean hasReminderBeenSent(Long userId, Long activityId, String reminderType) {
        try {
            List<Notification> existingNotifications = notificationRepository
                    .findByUserIdAndTypeAndIsDeletedFalseOrderByCreatedAtDesc(userId,
                            reminderType.equals("1_DAY") ? NotificationType.REMINDER_1_DAY
                                    : NotificationType.REMINDER_1_HOUR);

            for (Notification notification : existingNotifications) {
                if (notification.getMetadata() != null) {
                    try {
                        Map<String, Object> metadata = objectMapper.readValue(notification.getMetadata(),
                                new TypeReference<Map<String, Object>>() {
                                });
                        if (metadata.get("activityId") != null
                                && metadata.get("activityId").toString().equals(activityId.toString())
                                && metadata.get("reminderType") != null
                                && metadata.get("reminderType").toString().equals(reminderType)) {
                            return true;
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to parse notification metadata: {}", e.getMessage());
                    }
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("Error checking reminder status: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Gửi thông báo nhắc nhở
     */
    private void sendReminderNotification(ActivityRegistration registration, NotificationType type, String timeText) {
        try {
            Activity activity = registration.getActivity();
            Long userId = registration.getStudent().getUser().getId();

            String title = "Nhắc nhở sự kiện";
            String content = String.format("Sự kiện \"%s\" sẽ diễn ra sau %s", activity.getName(), timeText);
            String actionUrl = "/activities/" + activity.getId();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("activityId", activity.getId());
            metadata.put("activityName", activity.getName());
            metadata.put("reminderType", type == NotificationType.REMINDER_1_DAY ? "1_DAY" : "1_HOUR");
            metadata.put("registrationId", registration.getId());

            Notification notification = new Notification();
            notification.setUser(registration.getStudent().getUser());
            notification.setTitle(title);
            notification.setContent(content);
            notification.setType(type);
            notification.setActionUrl(actionUrl);
            notification.setStatus(vn.campuslife.enumeration.NotificationStatus.UNREAD);

            try {
                notification.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (Exception e) {
                logger.warn("Failed to serialize metadata: {}", e.getMessage());
                notification.setMetadata(metadata.toString());
            }

            notificationRepository.save(notification);
            logger.debug("Sent reminder notification to user {} for activity {}", userId, activity.getId());
        } catch (Exception e) {
            logger.error("Failed to send reminder notification: {}", e.getMessage(), e);
        }
    }
}

