package vn.campuslife.service;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.model.Response;
import vn.campuslife.model.SendEmailRequest;
import vn.campuslife.model.SendNotificationOnlyRequest;
import vn.campuslife.security.department.DepartmentScope;

public interface EmailService {
    Response sendEmail(SendEmailRequest request, Long senderId, MultipartFile[] attachments);

    Response sendEmail(SendEmailRequest request, Long senderId, MultipartFile[] attachments, DepartmentScope scope);

    Response sendNotificationOnly(SendNotificationOnlyRequest request);

    Response sendNotificationOnly(SendNotificationOnlyRequest request, DepartmentScope scope);

    Response getEmailHistory(Long senderId, Pageable pageable);

    Response getEmailHistory(Long senderId, Pageable pageable, DepartmentScope scope);

    /**
     * Lấy chi tiết email đã gửi
     */
    Response getEmailHistoryById(Long emailId);

    /**
     * Gửi lại email
     */
    Response resendEmail(Long emailId);
}

