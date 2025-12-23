package vn.campuslife.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.*;
import vn.campuslife.model.*;
import vn.campuslife.repository.*;
import vn.campuslife.service.EmailService;
import vn.campuslife.service.NotificationService;
import vn.campuslife.util.EmailUtil;
import vn.campuslife.util.NotificationUrlUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final EmailUtil emailUtil;
    private final EmailHistoryRepository emailHistoryRepository;
    private final EmailAttachmentRepository emailAttachmentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ActivityRepository activityRepository;
    private final ActivitySeriesRepository activitySeriesRepository;
    private final StudentClassRepository studentClassRepository;
    private final DepartmentRepository departmentRepository;
    private final NotificationService notificationService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.public-url:http://localhost:8080}")
    private String publicUrl;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public Response sendEmail(SendEmailRequest request, Long senderId, MultipartFile[] attachments) {
        try {
            // Validate sender
            Optional<User> senderOpt = userRepository.findById(senderId);
            if (senderOpt.isEmpty()) {
                return Response.error("Sender not found");
            }
            User sender = senderOpt.get();

            // Validate request
            if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
                return Response.error("Subject is required");
            }
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return Response.error("Content is required");
            }
            if (request.getRecipientType() == null) {
                return Response.error("Recipient type is required");
            }

            // Get recipients based on recipientType
            List<User> recipients = getRecipients(request);
            if (recipients.isEmpty()) {
                return Response.error("No recipients found");
            }

            // Save attachments if any
            List<EmailAttachment> savedAttachments = new ArrayList<>();
            List<File> attachmentFiles = new ArrayList<>();
            if (attachments != null && attachments.length > 0) {
                savedAttachments = saveAttachments(attachments, null); // emailHistoryId will be set later
                attachmentFiles = convertToFiles(savedAttachments);
            }

            // Build recipient filter JSON
            String recipientFilter = buildRecipientFilter(request);

            // Send emails to each recipient
            int successCount = 0;
            int failedCount = 0;
            List<EmailHistory> emailHistories = new ArrayList<>();
            EmailStatus overallStatus = EmailStatus.SUCCESS;

            for (User recipient : recipients) {
                try {
                    // Build template variables
                    Map<String, String> templateVars = buildTemplateVariables(recipient, request);

                    // Process template
                    String processedContent = emailUtil.processTemplate(request.getContent(), templateVars);
                    String processedSubject = emailUtil.processTemplate(request.getSubject(), templateVars);

                    // Send email
                    boolean sent = emailUtil.sendCustomEmail(
                            recipient.getEmail(),
                            processedSubject,
                            processedContent,
                            request.getIsHtml() != null && request.getIsHtml(),
                            attachmentFiles);

                    // Create email history
                    EmailHistory emailHistory = new EmailHistory();
                    emailHistory.setSender(sender);
                    emailHistory.setRecipient(recipient);
                    emailHistory.setRecipientEmail(recipient.getEmail());
                    emailHistory.setSubject(processedSubject);
                    emailHistory.setContent(processedContent);
                    emailHistory.setHtml(request.getIsHtml() != null && request.getIsHtml());
                    emailHistory.setRecipientType(request.getRecipientType());
                    emailHistory.setRecipientFilter(recipientFilter);
                    emailHistory.setAttachmentCount(savedAttachments.size());
                    emailHistory.setSentAt(LocalDateTime.now());
                    emailHistory.setStatus(sent ? EmailStatus.SUCCESS : EmailStatus.FAILED);
                    if (!sent) {
                        emailHistory.setErrorMessage("Failed to send email");
                        overallStatus = EmailStatus.PARTIAL;
                    }

                    // Create notification if requested
                    if (request.getCreateNotification() != null && request.getCreateNotification()) {
                        try {
                            String notificationTitle = request.getNotificationTitle() != null
                                    ? emailUtil.processTemplate(request.getNotificationTitle(), templateVars)
                                    : processedSubject;
                            String notificationContent = processedContent;
                            NotificationType notificationType = request.getNotificationType() != null
                                    ? request.getNotificationType()
                                    : NotificationType.SYSTEM_ANNOUNCEMENT;

                            // Auto-generate actionUrl and metadata if not provided
                            String actionUrl = request.getNotificationActionUrl();
                            Map<String, Object> metadata = new HashMap<>();
                            
                            // Auto-generate URL for activity
                            if (request.getActivityId() != null) {
                                if (actionUrl == null || actionUrl.trim().isEmpty()) {
                                    actionUrl = NotificationUrlUtils.generateActivityUrl(request.getActivityId(), frontendUrl);
                                }
                                metadata.put("activityId", request.getActivityId());
                            }
                            
                            // Auto-generate URL for series
                            if (request.getSeriesId() != null) {
                                if (actionUrl == null || actionUrl.trim().isEmpty()) {
                                    actionUrl = NotificationUrlUtils.generateSeriesUrl(request.getSeriesId(), frontendUrl);
                                }
                                metadata.put("seriesId", request.getSeriesId());
                            }

                            notificationService.sendNotification(
                                    recipient.getId(),
                                    notificationTitle,
                                    notificationContent,
                                    notificationType,
                                    actionUrl,
                                    metadata.isEmpty() ? null : metadata);
                            emailHistory.setNotificationCreated(true);
                        } catch (Exception e) {
                            logger.error("Failed to create notification for user {}: {}", recipient.getId(),
                                    e.getMessage());
                        }
                    }

                    emailHistories.add(emailHistory);
                    if (sent) {
                        successCount++;
                    } else {
                        failedCount++;
                    }
                } catch (Exception e) {
                    logger.error("Failed to send email to {}: {}", recipient.getEmail(), e.getMessage(), e);
                    failedCount++;
                    overallStatus = EmailStatus.PARTIAL;

                    // Create failed email history
                    EmailHistory emailHistory = new EmailHistory();
                    emailHistory.setSender(sender);
                    emailHistory.setRecipient(recipient);
                    emailHistory.setRecipientEmail(recipient.getEmail());
                    emailHistory.setSubject(request.getSubject());
                    emailHistory.setContent(request.getContent());
                    emailHistory.setHtml(request.getIsHtml() != null && request.getIsHtml());
                    emailHistory.setRecipientType(request.getRecipientType());
                    emailHistory.setRecipientFilter(recipientFilter);
                    emailHistory.setAttachmentCount(0);
                    emailHistory.setSentAt(LocalDateTime.now());
                    emailHistory.setStatus(EmailStatus.FAILED);
                    emailHistory.setErrorMessage(e.getMessage());
                    emailHistories.add(emailHistory);
                }
            }

            // Save all email histories
            emailHistories = emailHistoryRepository.saveAll(emailHistories);

            // Update attachments with emailHistoryId
            if (!savedAttachments.isEmpty() && !emailHistories.isEmpty()) {
                for (int i = 0; i < savedAttachments.size(); i++) {
                    EmailAttachment attachment = savedAttachments.get(i);
                    // Attach to first email history (all emails in same batch share attachments)
                    attachment.setEmailHistory(emailHistories.get(0));
                }
                emailAttachmentRepository.saveAll(savedAttachments);
            }

            // Build response
            Map<String, Object> result = new HashMap<>();
            result.put("totalRecipients", recipients.size());
            result.put("successCount", successCount);
            result.put("failedCount", failedCount);
            result.put("status", overallStatus);
            result.put("emailHistories", emailHistories.stream()
                    .map(this::toEmailHistoryResponse)
                    .collect(Collectors.toList()));

            String message = String.format("Email sent to %d recipients (%d success, %d failed)",
                    recipients.size(), successCount, failedCount);
            return Response.success(message, result);
        } catch (Exception e) {
            logger.error("Failed to send email: {}", e.getMessage(), e);
            return Response.error("Failed to send email: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response sendNotificationOnly(SendNotificationOnlyRequest request) {
        try {
            // Validate request
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                return Response.error("Title is required");
            }
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return Response.error("Content is required");
            }
            if (request.getType() == null) {
                return Response.error("Notification type is required");
            }
            if (request.getRecipientType() == null) {
                return Response.error("Recipient type is required");
            }

            // Get recipients
            List<User> recipients = getRecipientsForNotification(request);
            if (recipients.isEmpty()) {
                return Response.error("No recipients found");
            }

            // Send notifications
            int successCount = 0;
            int failedCount = 0;

            // Auto-generate actionUrl and metadata if not provided
            String actionUrl = request.getActionUrl();
            Map<String, Object> metadata = new HashMap<>();
            
            // Auto-generate URL for activity
            if (request.getActivityId() != null) {
                if (actionUrl == null || actionUrl.trim().isEmpty()) {
                    actionUrl = NotificationUrlUtils.generateActivityUrl(request.getActivityId(), frontendUrl);
                }
                metadata.put("activityId", request.getActivityId());
            }
            
            // Auto-generate URL for series
            if (request.getSeriesId() != null) {
                if (actionUrl == null || actionUrl.trim().isEmpty()) {
                    actionUrl = NotificationUrlUtils.generateSeriesUrl(request.getSeriesId(), frontendUrl);
                }
                metadata.put("seriesId", request.getSeriesId());
            }

            for (User recipient : recipients) {
                try {
                    // Build template variables
                    Map<String, String> templateVars = buildTemplateVariablesForNotification(recipient, request);

                    // Process template
                    String processedContent = emailUtil.processTemplate(request.getContent(), templateVars);
                    String processedTitle = emailUtil.processTemplate(request.getTitle(), templateVars);

                    notificationService.sendNotification(
                            recipient.getId(),
                            processedTitle,
                            processedContent,
                            request.getType(),
                            actionUrl,
                            metadata.isEmpty() ? null : metadata);
                    successCount++;
                } catch (Exception e) {
                    logger.error("Failed to send notification to user {}: {}", recipient.getId(), e.getMessage());
                    failedCount++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("totalRecipients", recipients.size());
            result.put("successCount", successCount);
            result.put("failedCount", failedCount);

            String message = String.format("Notification sent to %d recipients (%d success, %d failed)",
                    recipients.size(), successCount, failedCount);
            return Response.success(message, result);
        } catch (Exception e) {
            logger.error("Failed to send notification: {}", e.getMessage(), e);
            return Response.error("Failed to send notification: " + e.getMessage());
        }
    }

    @Override
    public Response getEmailHistory(Long senderId, Pageable pageable) {
        try {
            Page<EmailHistory> emailHistories = emailHistoryRepository.findBySenderIdOrderBySentAtDesc(senderId,
                    pageable);
            List<EmailHistoryResponse> responses = emailHistories.getContent().stream()
                    .map(this::toEmailHistoryResponse)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("content", responses);
            result.put("totalElements", emailHistories.getTotalElements());
            result.put("totalPages", emailHistories.getTotalPages());
            result.put("size", emailHistories.getSize());
            result.put("number", emailHistories.getNumber());

            return Response.success("Email history retrieved successfully", result);
        } catch (Exception e) {
            logger.error("Failed to get email history: {}", e.getMessage(), e);
            return Response.error("Failed to get email history: " + e.getMessage());
        }
    }

    @Override
    public Response getEmailHistoryById(Long emailId) {
        try {
            Optional<EmailHistory> emailHistoryOpt = emailHistoryRepository.findById(emailId);
            if (emailHistoryOpt.isEmpty()) {
                return Response.error("Email history not found");
            }

            EmailHistoryResponse response = toEmailHistoryResponse(emailHistoryOpt.get());
            return Response.success("Email history retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Failed to get email history: {}", e.getMessage(), e);
            return Response.error("Failed to get email history: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response resendEmail(Long emailId) {
        try {
            Optional<EmailHistory> emailHistoryOpt = emailHistoryRepository.findById(emailId);
            if (emailHistoryOpt.isEmpty()) {
                return Response.error("Email history not found");
            }

            EmailHistory emailHistory = emailHistoryOpt.get();
            User recipient = emailHistory.getRecipient();
            if (recipient == null) {
                return Response.error("Recipient not found");
            }

            // Get attachments
            List<EmailAttachment> attachments = emailAttachmentRepository.findByEmailHistoryId(emailId);
            List<File> attachmentFiles = convertToFiles(attachments);

            // Resend email
            boolean sent = emailUtil.sendCustomEmail(
                    emailHistory.getRecipientEmail(),
                    emailHistory.getSubject(),
                    emailHistory.getContent(),
                    emailHistory.isHtml(),
                    attachmentFiles);

            // Update email history
            emailHistory.setSentAt(LocalDateTime.now());
            emailHistory.setStatus(sent ? EmailStatus.SUCCESS : EmailStatus.FAILED);
            if (!sent) {
                emailHistory.setErrorMessage("Failed to resend email");
            }
            emailHistoryRepository.save(emailHistory);

            return Response.success(sent ? "Email resent successfully" : "Failed to resend email",
                    toEmailHistoryResponse(emailHistory));
        } catch (Exception e) {
            logger.error("Failed to resend email: {}", e.getMessage(), e);
            return Response.error("Failed to resend email: " + e.getMessage());
        }
    }

    // Helper methods

    private List<User> getRecipients(SendEmailRequest request) {
        List<User> recipients = new ArrayList<>();

        switch (request.getRecipientType()) {
            case BULK:
                // BULK: Gửi theo danh sách user IDs (có thể 1 hoặc nhiều)
                if (request.getRecipientIds() != null && !request.getRecipientIds().isEmpty()) {
                    recipients = userRepository.findAllById(request.getRecipientIds());
                }
                break;

            case ACTIVITY_REGISTRATIONS:
                if (request.getActivityId() != null) {
                    List<ActivityRegistration> registrations = activityRegistrationRepository
                            .findByActivityIdAndActivityIsDeletedFalse(request.getActivityId());
                    recipients = registrations.stream()
                            .map(ar -> ar.getStudent().getUser())
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());
                }
                break;

            case SERIES_REGISTRATIONS:
                if (request.getSeriesId() != null) {
                    List<ActivityRegistration> registrations = activityRegistrationRepository
                            .findBySeriesId(request.getSeriesId());
                    recipients = registrations.stream()
                            .map(ar -> ar.getStudent().getUser())
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());
                }
                break;

            case ALL_STUDENTS:
                List<Student> allStudents = studentRepository.findAll().stream()
                        .filter(s -> !s.isDeleted())
                        .collect(Collectors.toList());
                recipients = allStudents.stream()
                        .map(Student::getUser)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                break;

            case BY_CLASS:
                if (request.getClassId() != null) {
                    List<Student> students = studentRepository.findByClassId(request.getClassId());
                    recipients = students.stream()
                            .map(Student::getUser)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }
                break;

            case BY_DEPARTMENT:
                if (request.getDepartmentId() != null) {
                    Page<Student> students = studentRepository.findByStudentClassDepartmentIdAndIsDeletedFalse(
                            request.getDepartmentId(), org.springframework.data.domain.Pageable.unpaged());
                    recipients = students.getContent().stream()
                            .map(Student::getUser)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }
                break;
        }

        return recipients;
    }

    private List<User> getRecipientsForNotification(SendNotificationOnlyRequest request) {
        // Similar logic to getRecipients but for notification-only requests
        List<User> recipients = new ArrayList<>();

        switch (request.getRecipientType()) {
            case BULK:
                // BULK: Gửi theo danh sách user IDs (có thể 1 hoặc nhiều)
                if (request.getRecipientIds() != null && !request.getRecipientIds().isEmpty()) {
                    recipients = userRepository.findAllById(request.getRecipientIds());
                }
                break;

            case ACTIVITY_REGISTRATIONS:
                if (request.getActivityId() != null) {
                    List<ActivityRegistration> registrations = activityRegistrationRepository
                            .findByActivityIdAndActivityIsDeletedFalse(request.getActivityId());
                    recipients = registrations.stream()
                            .map(ar -> ar.getStudent().getUser())
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());
                }
                break;

            case SERIES_REGISTRATIONS:
                if (request.getSeriesId() != null) {
                    List<ActivityRegistration> registrations = activityRegistrationRepository
                            .findBySeriesId(request.getSeriesId());
                    recipients = registrations.stream()
                            .map(ar -> ar.getStudent().getUser())
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());
                }
                break;

            case ALL_STUDENTS:
                List<Student> allStudents = studentRepository.findAll().stream()
                        .filter(s -> !s.isDeleted())
                        .collect(Collectors.toList());
                recipients = allStudents.stream()
                        .map(Student::getUser)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                break;

            case BY_CLASS:
                if (request.getClassId() != null) {
                    List<Student> students = studentRepository.findByClassId(request.getClassId());
                    recipients = students.stream()
                            .map(Student::getUser)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }
                break;

            case BY_DEPARTMENT:
                if (request.getDepartmentId() != null) {
                    Page<Student> students = studentRepository.findByStudentClassDepartmentIdAndIsDeletedFalse(
                            request.getDepartmentId(), org.springframework.data.domain.Pageable.unpaged());
                    recipients = students.getContent().stream()
                            .map(Student::getUser)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }
                break;
        }

        return recipients;
    }

    private Map<String, String> buildTemplateVariables(User recipient, SendEmailRequest request) {
        Map<String, String> vars = new HashMap<>();

        // Add email for all recipients
        vars.put("email", recipient.getEmail() != null ? recipient.getEmail() : "");

        // Get student info if recipient is a student
        Optional<Student> studentOpt = studentRepository.findByUserIdAndIsDeletedFalse(recipient.getId());
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            vars.put("studentName", student.getFullName() != null ? student.getFullName() : "");
            vars.put("studentCode", student.getStudentCode() != null ? student.getStudentCode() : "");

            if (student.getStudentClass() != null) {
                vars.put("className",
                        student.getStudentClass().getClassName() != null ? student.getStudentClass().getClassName()
                                : "");
            }
            if (student.getDepartment() != null) {
                vars.put("departmentName",
                        student.getDepartment().getName() != null ? student.getDepartment().getName() : "");
            }
        }

        // Add activity info if applicable
        if (request.getActivityId() != null) {
            Optional<Activity> activityOpt = activityRepository.findById(request.getActivityId());
            if (activityOpt.isPresent()) {
                Activity activity = activityOpt.get();
                vars.put("activityName", activity.getName() != null ? activity.getName() : "");
                if (activity.getStartDate() != null) {
                    vars.put("activityDate", activity.getStartDate().toString());
                }
            }
        }

        // Add series info if applicable
        if (request.getSeriesId() != null) {
            Optional<ActivitySeries> seriesOpt = activitySeriesRepository.findById(request.getSeriesId());
            if (seriesOpt.isPresent()) {
                ActivitySeries series = seriesOpt.get();
                vars.put("seriesName", series.getName() != null ? series.getName() : "");
            }
        }

        // Add custom template variables
        if (request.getTemplateVariables() != null) {
            vars.putAll(request.getTemplateVariables());
        }

        return vars;
    }

    private Map<String, String> buildTemplateVariablesForNotification(User recipient,
            SendNotificationOnlyRequest request) {
        Map<String, String> vars = new HashMap<>();

        // Add email for all recipients
        vars.put("email", recipient.getEmail() != null ? recipient.getEmail() : "");

        // Get student info if recipient is a student
        Optional<Student> studentOpt = studentRepository.findByUserIdAndIsDeletedFalse(recipient.getId());
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            vars.put("studentName", student.getFullName() != null ? student.getFullName() : "");
            vars.put("studentCode", student.getStudentCode() != null ? student.getStudentCode() : "");

            if (student.getStudentClass() != null) {
                vars.put("className",
                        student.getStudentClass().getClassName() != null ? student.getStudentClass().getClassName()
                                : "");
            }
            if (student.getDepartment() != null) {
                vars.put("departmentName",
                        student.getDepartment().getName() != null ? student.getDepartment().getName() : "");
            }
        }

        // Add activity info if applicable
        if (request.getActivityId() != null) {
            Optional<Activity> activityOpt = activityRepository.findById(request.getActivityId());
            if (activityOpt.isPresent()) {
                Activity activity = activityOpt.get();
                vars.put("activityName", activity.getName() != null ? activity.getName() : "");
                if (activity.getStartDate() != null) {
                    vars.put("activityDate", activity.getStartDate().toString());
                }
            }
        }

        // Add series info if applicable
        if (request.getSeriesId() != null) {
            Optional<ActivitySeries> seriesOpt = activitySeriesRepository.findById(request.getSeriesId());
            if (seriesOpt.isPresent()) {
                ActivitySeries series = seriesOpt.get();
                vars.put("seriesName", series.getName() != null ? series.getName() : "");
            }
        }

        // Add custom template variables
        if (request.getTemplateVariables() != null) {
            vars.putAll(request.getTemplateVariables());
        }

        return vars;
    }

    private String buildRecipientFilter(SendEmailRequest request) {
        try {
            Map<String, Object> filter = new HashMap<>();
            if (request.getActivityId() != null) {
                filter.put("activityId", request.getActivityId());
            }
            if (request.getSeriesId() != null) {
                filter.put("seriesId", request.getSeriesId());
            }
            if (request.getClassId() != null) {
                filter.put("classId", request.getClassId());
            }
            if (request.getDepartmentId() != null) {
                filter.put("departmentId", request.getDepartmentId());
            }
            if (request.getRecipientIds() != null && !request.getRecipientIds().isEmpty()) {
                filter.put("recipientIds", request.getRecipientIds());
            }
            return objectMapper.writeValueAsString(filter);
        } catch (Exception e) {
            logger.error("Failed to build recipient filter: {}", e.getMessage());
            return "{}";
        }
    }

    private List<EmailAttachment> saveAttachments(MultipartFile[] files, Long emailHistoryId) {
        List<EmailAttachment> attachments = new ArrayList<>();
        if (files == null || files.length == 0) {
            return attachments;
        }

        try {
            Path attachmentDir = Paths.get(uploadDir, "email-attachments");
            if (emailHistoryId != null) {
                attachmentDir = attachmentDir.resolve(emailHistoryId.toString());
            }
            Files.createDirectories(attachmentDir);

            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                // Validate file size (max 10MB)
                if (file.getSize() > 10 * 1024 * 1024) {
                    logger.warn("File {} exceeds 10MB limit, skipping", file.getOriginalFilename());
                    continue;
                }

                // Generate unique filename
                String originalFilename = file.getOriginalFilename();
                String fileExtension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String fileName = UUID.randomUUID().toString() + fileExtension;

                // Save file
                Path filePath = attachmentDir.resolve(fileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // Create attachment record
                EmailAttachment attachment = new EmailAttachment();
                attachment.setFileName(originalFilename != null ? originalFilename : fileName);
                attachment.setFilePath(filePath.toString());
                attachment.setFileSize(file.getSize());
                attachment.setContentType(
                        file.getContentType() != null ? file.getContentType() : "application/octet-stream");
                attachments.add(attachment);
            }
        } catch (IOException e) {
            logger.error("Failed to save attachments: {}", e.getMessage(), e);
        }

        return attachments;
    }

    private List<File> convertToFiles(List<EmailAttachment> attachments) {
        return attachments.stream()
                .map(att -> new File(att.getFilePath()))
                .filter(File::exists)
                .collect(Collectors.toList());
    }

    private EmailHistoryResponse toEmailHistoryResponse(EmailHistory emailHistory) {
        EmailHistoryResponse response = new EmailHistoryResponse();
        response.setId(emailHistory.getId());
        response.setSenderId(emailHistory.getSender().getId());
        response.setSenderName(emailHistory.getSender().getUsername());
        if (emailHistory.getRecipient() != null) {
            response.setRecipientId(emailHistory.getRecipient().getId());
        }
        response.setRecipientEmail(emailHistory.getRecipientEmail());
        response.setSubject(emailHistory.getSubject());
        response.setContent(emailHistory.getContent());
        response.setIsHtml(emailHistory.isHtml());
        response.setRecipientType(emailHistory.getRecipientType());
        response.setRecipientCount(1); // For individual emails
        response.setSentAt(emailHistory.getSentAt());
        response.setStatus(emailHistory.getStatus());
        response.setErrorMessage(emailHistory.getErrorMessage());
        response.setNotificationCreated(emailHistory.isNotificationCreated());

        // Get attachments
        List<EmailAttachment> attachments = emailAttachmentRepository.findByEmailHistoryId(emailHistory.getId());
        List<EmailAttachmentResponse> attachmentResponses = attachments.stream()
                .map(this::toEmailAttachmentResponse)
                .collect(Collectors.toList());
        response.setAttachments(attachmentResponses);

        return response;
    }

    private EmailAttachmentResponse toEmailAttachmentResponse(EmailAttachment attachment) {
        EmailAttachmentResponse response = new EmailAttachmentResponse();
        response.setId(attachment.getId());
        response.setFileName(attachment.getFileName());
        response.setFileUrl(publicUrl + "/api/emails/attachments/" + attachment.getId() + "/download");
        response.setFileSize(attachment.getFileSize());
        response.setContentType(attachment.getContentType());
        return response;
    }
}
