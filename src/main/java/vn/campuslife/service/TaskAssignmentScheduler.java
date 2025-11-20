package vn.campuslife.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.campuslife.model.Response;
import vn.campuslife.service.ActivityTaskService;

@Component
@RequiredArgsConstructor
public class TaskAssignmentScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TaskAssignmentScheduler.class);

    private final ActivityTaskService activityTaskService;

    /**
     * Tự động kiểm tra và cập nhật OVERDUE cho các task assignment quá hạn
     * Chạy mỗi ngày lúc 00:00 (nửa đêm)
     */
    @Scheduled(cron = "0 0 0 * * ?") // Chạy mỗi ngày lúc 00:00
    public void checkOverdueAssignments() {
        try {
            logger.info("Starting scheduled task: Check and update overdue assignments");
            Response response = activityTaskService.checkAndUpdateOverdueAssignments();
            if (response.isStatus()) {
                logger.info("Scheduled task completed successfully: {}", response.getMessage());
            } else {
                logger.warn("Scheduled task completed with errors: {}", response.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error in scheduled task checkOverdueAssignments: {}", e.getMessage(), e);
        }
    }

    /**
     * Tự động kiểm tra và cập nhật OVERDUE cho các task assignment quá hạn
     * Chạy mỗi giờ (để test hoặc cần check thường xuyên hơn)
     * Uncomment nếu muốn chạy mỗi giờ thay vì mỗi ngày
     */
    // @Scheduled(cron = "0 0 * * * ?") // Chạy mỗi giờ
    // public void checkOverdueAssignmentsHourly() {
    //     try {
    //         logger.info("Starting hourly scheduled task: Check and update overdue assignments");
    //         Response response = activityTaskService.checkAndUpdateOverdueAssignments();
    //         if (response.isStatus()) {
    //             logger.info("Hourly scheduled task completed successfully: {}", response.getMessage());
    //         } else {
    //             logger.warn("Hourly scheduled task completed with errors: {}", response.getMessage());
    //         }
    //     } catch (Exception e) {
    //         logger.error("Error in hourly scheduled task checkOverdueAssignments: {}", e.getMessage(), e);
    //     }
    // }
}

