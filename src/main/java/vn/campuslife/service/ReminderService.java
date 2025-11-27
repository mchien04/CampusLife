package vn.campuslife.service;

import vn.campuslife.model.Response;

public interface ReminderService {
    /**
     * Gửi thông báo nhắc nhở cho các sự kiện sắp diễn ra
     */
    Response sendReminderNotifications();
}

