package vn.campuslife.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.campuslife.model.Response;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderService reminderService;

    /**
     * Tự động gửi thông báo nhắc nhở cho các sự kiện sắp diễn ra
     * Chạy mỗi giờ
     */
    @Scheduled(cron = "0 0 * * * ?") // Chạy mỗi giờ
    public void sendReminderNotifications() {
        try {
            logger.info("Starting scheduled task: Send reminder notifications");
            Response response = reminderService.sendReminderNotifications();
            if (response.isStatus()) {
                logger.info("Scheduled task completed successfully: {}", response.getMessage());
            } else {
                logger.warn("Scheduled task completed with errors: {}", response.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error in scheduled task sendReminderNotifications: {}", e.getMessage(), e);
        }
    }
}

