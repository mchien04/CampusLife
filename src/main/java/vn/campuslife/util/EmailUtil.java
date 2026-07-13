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
        String content = "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); border: 1px solid #eaeaea;\">" +
                "<div style=\"text-align: center; margin-bottom: 30px;\">" +
                "<h2 style=\"color: #2563eb; margin: 0; font-size: 28px;\">CampusLife</h2>" +
                "<p style=\"color: #64748b; font-size: 16px; margin-top: 5px;\">Nền tảng Quản lý Sinh viên</p>" +
                "</div>" +
                "<h3 style=\"color: #1e293b; font-size: 20px;\">Chào mừng bạn đến với CampusLife! 🎉</h3>" +
                "<p style=\"color: #334155; line-height: 1.6; font-size: 16px;\">Xin chào,</p>" +
                "<p style=\"color: #334155; line-height: 1.6; font-size: 16px;\">Tài khoản của bạn đã được tạo thành công trên hệ thống CampusLife. Dưới đây là thông tin đăng nhập dành cho bạn:</p>" +
                "<div style=\"background-color: #f1f5f9; border-left: 4px solid #2563eb; padding: 20px; border-radius: 4px; margin: 25px 0;\">" +
                "<p style=\"margin: 0 0 10px 0; color: #0f172a; font-size: 16px;\"><span style=\"color: #64748b; display: inline-block; width: 120px;\">Tên đăng nhập:</span> <strong>" + username + "</strong></p>" +
                "<p style=\"margin: 0; color: #0f172a; font-size: 16px;\"><span style=\"color: #64748b; display: inline-block; width: 120px;\">Mật khẩu:</span> <strong>" + password + "</strong></p>" +
                "</div>" +
                "<div style=\"text-align: center; margin: 35px 0;\">" +
                "<a href=\"" + loginLink + "\" style=\"background-color: #2563eb; color: #ffffff; text-decoration: none; padding: 14px 28px; border-radius: 6px; font-weight: 600; font-size: 16px; display: inline-block; transition: background-color 0.3s;\">Đăng nhập ngay</a>" +
                "</div>" +
                "<div style=\"background-color: #fff7ed; border: 1px solid #fed7aa; padding: 15px; border-radius: 6px; margin-bottom: 25px;\">" +
                "<p style=\"color: #c2410c; margin: 0; font-size: 14px; display: flex; align-items: center;\">" +
                "<span style=\"font-size: 18px; margin-right: 8px;\">⚠️</span>" +
                "<strong>Quan trọng:</strong>&nbsp;Vui lòng đổi mật khẩu ngay sau khi đăng nhập lần đầu để đảm bảo an toàn cho tài khoản của bạn." +
                "</p>" +
                "</div>" +
                "<p style=\"color: #334155; line-height: 1.6; font-size: 16px;\">Nếu bạn gặp khó khăn trong quá trình đăng nhập, vui lòng liên hệ với bộ phận hỗ trợ hoặc quản trị viên để được giúp đỡ.</p>" +
                "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 30px 0;\" />" +
                "<p style=\"color: #64748b; font-size: 14px; margin: 0;\">Trân trọng,<br><strong style=\"color: #0f172a;\">CampusLife Team</strong></p>" +
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
