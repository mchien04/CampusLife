package vn.campuslife.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

@Component
public class EmailUtil {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from-email:noreply@campuslife.app}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailUtil(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private boolean sendViaResend(String to, String subject, String htmlContent, List<File> attachments) {
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Resend API key not configured. Skipping email to: " + to);
            return false;
        }
        try {
            String url = "https://api.resend.com/emails";

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("from", fromEmail);
            payload.put("to", List.of(to));
            payload.put("subject", subject);
            payload.put("html", htmlContent);

            if (attachments != null && !attachments.isEmpty()) {
                List<Map<String, Object>> attList = new ArrayList<>();
                for (File file : attachments) {
                    if (file != null && file.exists()) {
                        byte[] fileBytes = Files.readAllBytes(file.toPath());
                        String base64 = Base64.getEncoder().encodeToString(fileBytes);
                        Map<String, Object> att = new LinkedHashMap<>();
                        att.put("filename", file.getName());
                        att.put("content", base64);
                        attList.add(att);
                    }
                }
                if (!attList.isEmpty()) {
                    payload.put("attachments", attList);
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);

            System.out.println("Attempting to send email via Resend to: " + to);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Email sent successfully to: " + to);
                return true;
            } else {
                System.err.println("Resend API returned " + response.getStatusCode() + ": " + response.getBody());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Email sending failed to " + to + ": " + e.getMessage());
            return false;
        }
    }

    public boolean sendActivationEmail(String to, String token) {
        String activationLink = frontendUrl + "/verify?token=" + token;
        String content = "<h3>Welcome to CampusLife!</h3>" +
                "<p>Please click the link below to activate your account:</p>" +
                "<a href=\"" + activationLink + "\">Activate Account</a>" +
                "<p>This link will expire in 24 hours.</p>";
        return sendViaResend(to, "Activate Your CampusLife Account", content, null);
    }

    public boolean sendPasswordResetEmail(String to, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String content = "<h3>Password Reset Request</h3>" +
                "<p>You have requested to reset your password. Please click the link below to reset your password:</p>" +
                "<a href=\"" + resetLink + "\" style=\"display:inline-block;padding:10px 20px;background-color:#007bff;color:#ffffff;text-decoration:none;border-radius:5px;\">Reset Password</a>" +
                "<p>If you did not request this, please ignore this email.</p>" +
                "<p>This link will expire in 1 hour.</p>" +
                "<p>For security reasons, please do not share this link with anyone.</p>";
        return sendViaResend(to, "Reset Your CampusLife Password", content, null);
    }

    public boolean sendStudentCredentialsEmail(String to, String username, String password) {
        String loginLink = frontendUrl + "/login";
        String content = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
                "<h2 style=\"color: #007bff;\">Chào mừng đến với CampusLife!</h2>" +
                "<p>Xin chào,</p>" +
                "<p>Bạn đã được tạo tài khoản trên hệ thống CampusLife. Dưới đây là thông tin đăng nhập của bạn:</p>" +
                "<div style=\"background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;\">" +
                "<p style=\"margin: 5px 0;\"><strong>Tên đăng nhập:</strong> " + username + "</p>" +
                "<p style=\"margin: 5px 0;\"><strong>Mật khẩu:</strong> " + password + "</p>" +
                "</div>" +
                "<p>Vui lòng đăng nhập tại: <a href=\"" + loginLink + "\" style=\"color: #007bff;\">" + loginLink + "</a></p>" +
                "<p style=\"color: #dc3545; font-weight: bold;\">⚠️ Lưu ý: Vui lòng đổi mật khẩu sau khi đăng nhập lần đầu để bảo mật tài khoản.</p>" +
                "<p>Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ với quản trị viên.</p>" +
                "<p>Trân trọng,<br>CampusLife Team</p>" +
                "</div>";
        return sendViaResend(to, "Thông tin tài khoản CampusLife", content, null);
    }

    public boolean sendCustomEmail(String to, String subject, String content, boolean isHtml, List<File> attachments) {
        return sendViaResend(to, subject, content, attachments);
    }

    public String processTemplate(String template, Map<String, String> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }
}
