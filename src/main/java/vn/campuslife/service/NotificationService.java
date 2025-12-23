package vn.campuslife.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.campuslife.entity.Notification;
import vn.campuslife.enumeration.NotificationStatus;
import vn.campuslife.enumeration.NotificationType;
import vn.campuslife.model.Response;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    // Gửi thông báo cho một user
    Response sendNotification(Long userId, String title, String content, NotificationType type, String actionUrl,
            Map<String, Object> metadata);

    // Gửi thông báo cho nhiều user
    Response sendBulkNotification(List<Long> userIds, String title, String content, NotificationType type,
            String actionUrl, Map<String, Object> metadata);

    // Gửi thông báo cho tất cả user trong department
    Response sendNotificationToDepartment(Long departmentId, String title, String content, NotificationType type,
            String actionUrl, Map<String, Object> metadata);

    // Gửi thông báo cho tất cả user trong class
    Response sendNotificationToClass(Long classId, String title, String content, NotificationType type,
            String actionUrl, Map<String, Object> metadata);

    // Lấy danh sách thông báo của user
    Response getUserNotifications(Long userId, Pageable pageable);

    // Lấy thông báo chưa đọc
    Response getUnreadNotifications(Long userId);

    // Đánh dấu đã đọc
    Response markAsRead(Long notificationId, Long userId);

    // Đánh dấu tất cả đã đọc
    Response markAllAsRead(Long userId);

    // Đếm số thông báo chưa đọc
    Response getUnreadCount(Long userId);

    // Xóa thông báo
    Response deleteNotification(Long notificationId, Long userId);

    // Lưu trữ thông báo
    Response archiveNotification(Long notificationId, Long userId);

    // Lấy chi tiết thông báo
    Response getNotificationDetail(Long notificationId, Long userId);
}
