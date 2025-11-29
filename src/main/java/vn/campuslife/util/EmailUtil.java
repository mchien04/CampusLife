package vn.campuslife.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Component
public class EmailUtil {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailUtil(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendActivationEmail(String to, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Activate Your CampusLife Account");
            String activationLink = frontendUrl + "/verify?token=" + token;
            String content = "<h3>Welcome to CampusLife!</h3>" +
                    "<p>Please click the link below to activate your account:</p>" +
                    "<a href=\"" + activationLink + "\">Activate Account</a>" +
                    "<p>This link will expire in 24 hours.</p>";
            helper.setText(content, true);

            System.out.println("Attempting to send activation email to: " + to);
            mailSender.send(message);
            System.out.println("Email sent successfully to: " + to);
            return true;
        } catch (Exception e) {
            System.err.println("Email sending failed to " + to + ": " + e.getMessage());
            if (e.getMessage().contains("Daily user sending limit exceeded")) {
                System.err.println(
                        "Gmail daily sending limit exceeded. Please wait 24 hours or use a different email service.");
            }
            return false;
        }
    }

    public boolean sendPasswordResetEmail(String to, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Reset Your CampusLife Password");
            String resetLink = frontendUrl + "/reset-password?token=" + token;
            String content = "<h3>Password Reset Request</h3>" +
                    "<p>You have requested to reset your password. Please click the link below to reset your password:</p>" +
                    "<a href=\"" + resetLink + "\" style=\"display:inline-block;padding:10px 20px;background-color:#007bff;color:#ffffff;text-decoration:none;border-radius:5px;\">Reset Password</a>" +
                    "<p>If you did not request this, please ignore this email.</p>" +
                    "<p>This link will expire in 1 hour.</p>" +
                    "<p>For security reasons, please do not share this link with anyone.</p>";
            helper.setText(content, true);

            System.out.println("Attempting to send password reset email to: " + to);
            mailSender.send(message);
            System.out.println("Password reset email sent successfully to: " + to);
            return true;
        } catch (Exception e) {
            System.err.println("Password reset email sending failed to " + to + ": " + e.getMessage());
            if (e.getMessage().contains("Daily user sending limit exceeded")) {
                System.err.println(
                        "Gmail daily sending limit exceeded. Please wait 24 hours or use a different email service.");
            }
            return false;
        }
    }
}
