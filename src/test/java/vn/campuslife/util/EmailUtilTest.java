package vn.campuslife.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailUtilTest {

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EmailUtil emailUtil;

    @BeforeEach
    public void setUp() {
        emailUtil = new EmailUtil(restTemplate, objectMapper);
        ReflectionTestUtils.setField(emailUtil, "apiKey", "re_test_ApiKey123");
        ReflectionTestUtils.setField(emailUtil, "fromEmail", "noreply@campuslife.app");
        ReflectionTestUtils.setField(emailUtil, "frontendUrl", "http://localhost:3000");
    }

    @Test
    public void testSendActivationEmail_Success() throws Exception {
        // Arrange
        String to = "student@example.com";
        String token = "activation-token-123";
        
        ResponseEntity<String> responseEntity = ResponseEntity.ok("{\"id\": \"email-id\"}");
        when(restTemplate.postForEntity(eq("https://api.resend.com/emails"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        boolean result = emailUtil.sendActivationEmail(to, token);

        // Assert
        assertTrue(result);
        
        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://api.resend.com/emails"), captor.capture(), eq(String.class));
        
        HttpEntity<String> entity = captor.getValue();
        assertNotNull(entity);
        
        // Check Headers
        HttpHeaders headers = entity.getHeaders();
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        assertEquals("Bearer re_test_ApiKey123", headers.getFirst(HttpHeaders.AUTHORIZATION));

        // Check Body
        String body = entity.getBody();
        assertNotNull(body);
        
        Map<String, Object> payload = objectMapper.readValue(body, Map.class);
        assertEquals("noreply@campuslife.app", payload.get("from"));
        assertEquals(List.of(to), payload.get("to"));
        assertEquals("Activate Your CampusLife Account", payload.get("subject"));
        assertTrue(((String) payload.get("html")).contains("http://localhost:3000/verify?token=activation-token-123"));
    }

    @Test
    public void testSendPasswordResetEmail_Success() throws Exception {
        // Arrange
        String to = "user@example.com";
        String token = "reset-token-xyz";

        ResponseEntity<String> responseEntity = ResponseEntity.ok("{\"id\": \"email-id\"}");
        when(restTemplate.postForEntity(eq("https://api.resend.com/emails"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        boolean result = emailUtil.sendPasswordResetEmail(to, token);

        // Assert
        assertTrue(result);

        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://api.resend.com/emails"), captor.capture(), eq(String.class));

        HttpEntity<String> entity = captor.getValue();
        String body = entity.getBody();
        Map<String, Object> payload = objectMapper.readValue(body, Map.class);
        assertEquals("Reset Your CampusLife Password", payload.get("subject"));
        assertTrue(((String) payload.get("html")).contains("http://localhost:3000/reset-password?token=reset-token-xyz"));
    }

    @Test
    public void testSendStudentCredentialsEmail_Success() throws Exception {
        // Arrange
        String to = "student@example.com";
        String username = "student_user";
        String password = "password123";

        ResponseEntity<String> responseEntity = ResponseEntity.ok("{\"id\": \"email-id\"}");
        when(restTemplate.postForEntity(eq("https://api.resend.com/emails"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        boolean result = emailUtil.sendStudentCredentialsEmail(to, username, password);

        // Assert
        assertTrue(result);

        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://api.resend.com/emails"), captor.capture(), eq(String.class));

        HttpEntity<String> entity = captor.getValue();
        String body = entity.getBody();
        Map<String, Object> payload = objectMapper.readValue(body, Map.class);
        assertEquals("Thông tin tài khoản CampusLife", payload.get("subject"));
        String html = (String) payload.get("html");
        assertTrue(html.contains("student_user"));
        assertTrue(html.contains("password123"));
        assertTrue(html.contains("http://localhost:3000/login"));
    }

    @Test
    public void testSendCustomEmail_WithAttachments_Success(@TempDir Path tempDir) throws Exception {
        // Arrange
        String to = "recipient@example.com";
        String subject = "Test Custom Email";
        String content = "Hello! Find attached files.";
        
        Path tempFile = tempDir.resolve("test-attachment.txt");
        Files.writeString(tempFile, "Hello World Attachment Data");
        File attachmentFile = tempFile.toFile();

        ResponseEntity<String> responseEntity = ResponseEntity.ok("{\"id\": \"email-id\"}");
        when(restTemplate.postForEntity(eq("https://api.resend.com/emails"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        boolean result = emailUtil.sendCustomEmail(to, subject, content, true, List.of(attachmentFile));

        // Assert
        assertTrue(result);

        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://api.resend.com/emails"), captor.capture(), eq(String.class));

        HttpEntity<String> entity = captor.getValue();
        String body = entity.getBody();
        Map<String, Object> payload = objectMapper.readValue(body, Map.class);
        assertEquals(subject, payload.get("subject"));
        assertEquals(content, payload.get("html"));
        
        List<Map<String, Object>> attachmentsPayload = (List<Map<String, Object>>) payload.get("attachments");
        assertNotNull(attachmentsPayload);
        assertEquals(1, attachmentsPayload.size());
        assertEquals("test-attachment.txt", attachmentsPayload.get(0).get("filename"));
        // base64 of "Hello World Attachment Data"
        assertEquals("SGVsbG8gV29ybGQgQXR0YWNobWVudCBEYXRh", attachmentsPayload.get(0).get("content"));
    }

    @Test
    public void testSendViaResend_ApiKeyMissing() {
        // Arrange
        ReflectionTestUtils.setField(emailUtil, "apiKey", "");

        // Act
        boolean result = emailUtil.sendCustomEmail("test@example.com", "Subject", "Content", false, null);

        // Assert
        assertFalse(result);
        verifyNoInteractions(restTemplate);
    }

    @Test
    public void testSendViaResend_ApiFailureResponse() {
        // Arrange
        ResponseEntity<String> errorResponse = ResponseEntity.badRequest().body("{\"message\": \"Invalid domain\"}");
        when(restTemplate.postForEntity(eq("https://api.resend.com/emails"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(errorResponse);

        // Act
        boolean result = emailUtil.sendCustomEmail("test@example.com", "Subject", "Content", false, null);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testProcessTemplate_Success() {
        // Arrange
        String template = "Hello {{studentName}}, welcome to {{activityName}}!";
        Map<String, String> variables = Map.of(
                "studentName", "John Doe",
                "activityName", "Spring Boot Coding"
        );

        // Act
        String result = emailUtil.processTemplate(template, variables);

        // Assert
        assertEquals("Hello John Doe, welcome to Spring Boot Coding!", result);
    }

    @Test
    public void testProcessTemplate_NullVariables() {
        // Arrange
        String template = "Hello {{studentName}}";

        // Act
        String result = emailUtil.processTemplate(template, null);

        // Assert
        assertEquals(template, result);
    }
}
