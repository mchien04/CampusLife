package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.EmailStatus;
import vn.campuslife.enumeration.RecipientType;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EmailHistoryResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private Long recipientId; // nullable
    private String recipientEmail;
    private String subject;
    private String content;
    private Boolean isHtml;
    private RecipientType recipientType;
    private Integer recipientCount; // Số lượng người nhận
    private LocalDateTime sentAt;
    private EmailStatus status;
    private String errorMessage; // nullable
    private Boolean notificationCreated;
    private List<EmailAttachmentResponse> attachments;
}

