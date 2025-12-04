package vn.campuslife.service;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.model.Response;
import vn.campuslife.model.SendEmailRequest;
import vn.campuslife.model.SendNotificationOnlyRequest;

public interface EmailService {
    /**
     * Gửi email với nhiều tùy chọn người nhận
     */
    Response sendEmail(SendEmailRequest request, Long senderId, MultipartFile[] attachments);

    /**
     * Chỉ tạo notification (không gửi email)
     */
    Response sendNotificationOnly(SendNotificationOnlyRequest request);

    /**
     * Lấy lịch sử email đã gửi
     */
    Response getEmailHistory(Long senderId, Pageable pageable);

    /**
     * Lấy chi tiết email đã gửi
     */
    Response getEmailHistoryById(Long emailId);

    /**
     * Gửi lại email
     */
    Response resendEmail(Long emailId);
}

