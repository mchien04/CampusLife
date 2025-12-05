package vn.campuslife.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.entity.User;
import vn.campuslife.model.Response;
import vn.campuslife.model.SendEmailRequest;
import vn.campuslife.model.SendNotificationOnlyRequest;
import vn.campuslife.repository.EmailAttachmentRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.service.EmailService;

import java.io.File;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final EmailAttachmentRepository emailAttachmentRepository;

    /**
     * Test endpoint để kiểm tra authentication
     */
    @GetMapping("/test-auth")
    public ResponseEntity<Response> testAuth(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(Response.error("Authentication is null"));
        }
        return ResponseEntity.ok(new Response(true, "Authentication successful",
                Map.of(
                        "username", authentication.getName(),
                        "authorities", authentication.getAuthorities().stream()
                                .map(a -> a.getAuthority())
                                .collect(java.util.stream.Collectors.toList()))));
    }

    /**
     * Gửi email với nhiều tùy chọn người nhận (Multipart - có thể có attachments)
     */
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response> sendEmail(
            @RequestPart(value = "request", required = true) SendEmailRequest request,
            @RequestPart(value = "attachments", required = false) MultipartFile[] attachments,
            Authentication authentication) {
        try {
            logger.info("Email send request received (multipart). Authentication: {}, Authorities: {}",
                    authentication != null ? authentication.getName() : "null",
                    authentication != null ? authentication.getAuthorities() : "null");

            Long senderId = getUserIdFromAuth(authentication);
            if (senderId == null) {
                logger.warn("User not found in authentication");
                return ResponseEntity.badRequest()
                        .body(Response.error("User not found"));
            }

            Response response = emailService.sendEmail(request, senderId, attachments);
            return response.isStatus()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error sending email: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Response.error("Server error occurred: " + e.getMessage()));
        }
    }

    /**
     * Gửi email với nhiều tùy chọn người nhận (JSON - không có attachments)
     * Alternative endpoint cho trường hợp frontend gửi JSON thay vì multipart
     */
    @PostMapping(value = "/send-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> sendEmailJson(
            @RequestBody SendEmailRequest request,
            Authentication authentication) {
        try {
            logger.info("Email send request received (JSON). Authentication: {}, Authorities: {}",
                    authentication != null ? authentication.getName() : "null",
                    authentication != null ? authentication.getAuthorities() : "null");

            Long senderId = getUserIdFromAuth(authentication);
            if (senderId == null) {
                logger.warn("User not found in authentication");
                return ResponseEntity.badRequest()
                        .body(Response.error("User not found"));
            }

            Response response = emailService.sendEmail(request, senderId, null);
            return response.isStatus()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error sending email: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Response.error("Server error occurred: " + e.getMessage()));
        }
    }

    /**
     * Chỉ tạo notification (không gửi email)
     */
    @PostMapping("/notifications/send")
    public ResponseEntity<Response> sendNotificationOnly(
            @RequestBody SendNotificationOnlyRequest request) {
        try {
            Response response = emailService.sendNotificationOnly(request);
            return response.isStatus()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error sending notification: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Response.error("Server error occurred: " + e.getMessage()));
        }
    }

    /**
     * Lấy lịch sử email đã gửi
     */
    @GetMapping("/history")
    public ResponseEntity<Response> getEmailHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long senderId = getUserIdFromAuth(authentication);
            if (senderId == null) {
                return ResponseEntity.badRequest()
                        .body(Response.error("User not found"));
            }

            Pageable pageable = PageRequest.of(page, size);
            Response response = emailService.getEmailHistory(senderId, pageable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting email history: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Response.error("Server error occurred: " + e.getMessage()));
        }
    }

    /**
     * Xem chi tiết email đã gửi
     */
    @GetMapping("/history/{emailId}")
    public ResponseEntity<Response> getEmailHistoryById(@PathVariable Long emailId) {
        try {
            Response response = emailService.getEmailHistoryById(emailId);
            return response.isStatus()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error getting email history: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Response.error("Server error occurred: " + e.getMessage()));
        }
    }

    /**
     * Gửi lại email
     */
    @PostMapping("/history/{emailId}/resend")
    public ResponseEntity<Response> resendEmail(@PathVariable Long emailId) {
        try {
            Response response = emailService.resendEmail(emailId);
            return response.isStatus()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error resending email: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Response.error("Server error occurred: " + e.getMessage()));
        }
    }

    /**
     * Download file đính kèm
     */
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        try {
            Optional<vn.campuslife.entity.EmailAttachment> attachmentOpt = emailAttachmentRepository
                    .findById(attachmentId);
            if (attachmentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            vn.campuslife.entity.EmailAttachment attachment = attachmentOpt.get();
            File file = new File(attachment.getFilePath());
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(attachment.getContentType()))
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error downloading attachment: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Helper method to get user ID from authentication
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        try {
            if (authentication == null) {
                return null;
            }
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            return userOpt.map(User::getId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
