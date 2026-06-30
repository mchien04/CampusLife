package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.ActivityTask;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.TaskAssignment;
import vn.campuslife.entity.TaskSubmission;
import vn.campuslife.entity.User;
import vn.campuslife.enumeration.SubmissionStatus;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.TaskStatus;
import vn.campuslife.model.score.AppliedScoreAward;
import vn.campuslife.model.Response;
import vn.campuslife.repository.*;
import vn.campuslife.service.ReminderScheduleService;
import vn.campuslife.model.activity.task.TaskSubmissionResponse;
import vn.campuslife.repository.ActivityParticipationRepository;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.ActivityTaskRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.TaskAssignmentRepository;
import vn.campuslife.repository.TaskSubmissionRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.service.TaskSubmissionService;
import vn.campuslife.service.SemesterHelperService;
import vn.campuslife.service.ScoreRuleEngine;
import vn.campuslife.service.UploadStorageService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskSubmissionServiceImpl implements TaskSubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(TaskSubmissionServiceImpl.class);

    private final UploadProperties uploadProperties;
    private final UploadStorageService uploadStorageService;

    private final TaskSubmissionRepository taskSubmissionRepository;
    private final ActivityTaskRepository activityTaskRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ActivityParticipationRepository activityParticipationRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final SemesterHelperService semesterHelperService;
    private final ReminderScheduleService reminderScheduleService;
    private final ScoreRuleEngine scoreRuleEngine;
    private final vn.campuslife.service.ActivitySeriesService activitySeriesService;

    @Override
    @Transactional
    public Response submitTask(Long taskId, Long studentId, String content, List<MultipartFile> files,
            List<MultipartFile> images) {
        try {
            // Validate task exists
            Optional<ActivityTask> taskOpt = activityTaskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                return new Response(false, "Task not found", null);
            }
            ActivityTask task = taskOpt.get();
            Activity activity = task.getActivity();

            if (activity == null || activity.isDeleted()) {
                return new Response(false, "Activity not found or deleted", null);
            }
            if (activity.isDraft()) {
                return new Response(false, "Activity is not published yet", null);
            }

            LocalDateTime now = LocalDateTime.now();
            if (task.getDeadline() != null && now.isAfter(task.getDeadline())) {
                return new Response(false, "Submission deadline has passed", null);
            }

            Optional<ActivityRegistration> regOpt = activityRegistrationRepository
                    .findByActivityIdAndStudentId(activity.getId(), studentId);
            if (regOpt.isEmpty()) {
                return new Response(false, "Student is not registered for this activity", null);
            }
            ActivityRegistration registration = regOpt.get();
            if (registration.getStatus() != RegistrationStatus.APPROVED && registration.getStatus() != RegistrationStatus.ATTENDED) {
                return new Response(false, "Registration is not approved or attended", null);
            }

            // Validate student exists
            Optional<Student> studentOpt = studentRepository.findByIdAndIsDeletedFalse(studentId);
            if (studentOpt.isEmpty()) {
                return new Response(false, "Student not found", null);
            }

            // Check if submission already exists
            Optional<TaskSubmission> existingSubmission = taskSubmissionRepository
                    .findByTaskIdAndStudentIdAndIsDeletedFalse(taskId, studentId);
            if (existingSubmission.isPresent()) {
                return new Response(false, "Submission already exists for this task", null);
            }

            // Create submission
            TaskSubmission submission = new TaskSubmission();
            submission.setTask(taskOpt.get());
            submission.setStudent(studentOpt.get());
            submission.setContent(content);
            submission.setStatus(SubmissionStatus.SUBMITTED);

            List<String> attachmentUrls = storeSubmissionAttachments(files, images);
            if (!attachmentUrls.isEmpty()) {
                submission.setFileUrls(String.join(",", attachmentUrls));
            }

            taskSubmissionRepository.save(submission);

            // Auto-grade check: If auto-graded immediately during submit
            if (submission.getStatus() == SubmissionStatus.GRADED) {
                try {
                    finalizeSubmissionResultIfEligible(submission, studentOpt.get().getUser());
                } catch (Exception ex) {
                    logger.warn("Auto-finalize submission result failed: {}", ex.getMessage());
                }
            }

            // Cập nhật TaskAssignment status sang ASSIGNED khi sinh viên nộp bài
            try {
                Optional<TaskAssignment> assignmentOpt = taskAssignmentRepository
                        .findByTaskIdAndStudentId(taskId, studentId);
                if (assignmentOpt.isPresent()) {
                    TaskAssignment assignment = assignmentOpt.get();
                    assignment.setStatus(TaskStatus.ASSIGNED);
                    taskAssignmentRepository.save(assignment);
                    reminderScheduleService.cancelPendingTaskRemindersForAssignment(assignment);
                    logger.info("Updated TaskAssignment status to ASSIGNED for task {} and student {}",
                            taskId, studentId);
                }
            } catch (Exception e) {
                logger.warn("Failed to update TaskAssignment status after submission: {}", e.getMessage());
                // Không fail submission nếu update assignment status lỗi
            }

            return new Response(true, "Task submitted successfully", toDto(submission));
        } catch (IOException e) {
            logger.error("Failed to upload files: {}", e.getMessage(), e);
            return new Response(false, "Failed to upload files: " + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Failed to submit task: {}", e.getMessage(), e);
            return new Response(false, "Failed to submit task: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response updateSubmission(Long submissionId, Long studentId, String content, List<MultipartFile> files,
            List<MultipartFile> images) {
        try {
            Optional<TaskSubmission> submissionOpt = taskSubmissionRepository.findById(submissionId);
            if (submissionOpt.isEmpty()) {
                return new Response(false, "Submission not found", null);
            }

            TaskSubmission submission = submissionOpt.get();
            if (!submission.getStudent().getId().equals(studentId)) {
                return new Response(false, "Unauthorized to update this submission", null);
            }

            ActivityTask task = submission.getTask();
            if (task == null) {
                return new Response(false, "Task not found", null);
            }
            Activity activity = task.getActivity();
            if (activity == null || activity.isDeleted()) {
                return new Response(false, "Activity not found or deleted", null);
            }
            if (activity.isDraft()) {
                return new Response(false, "Activity is not published yet", null);
            }

            LocalDateTime now = LocalDateTime.now();
            if (task.getDeadline() != null && now.isAfter(task.getDeadline())) {
                return new Response(false, "Submission deadline has passed", null);
            }

            if (submission.getStatus() == vn.campuslife.enumeration.SubmissionStatus.GRADED) {
                return new Response(false, "Cannot update a graded submission", null);
            }

            submission.setContent(content);

            List<String> attachmentUrls = storeSubmissionAttachments(files, images);
            if (!attachmentUrls.isEmpty()) {
                submission.setFileUrls(String.join(",", attachmentUrls));
            }

            taskSubmissionRepository.save(submission);
            return new Response(true, "Submission updated successfully", toDto(submission));
        } catch (IOException e) {
            logger.error("Failed to upload files: {}", e.getMessage(), e);
            return new Response(false, "Failed to upload files: " + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Failed to update submission: {}", e.getMessage(), e);
            return new Response(false, "Failed to update submission: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getStudentSubmissions(Long taskId, Long studentId) {
        try {
            Optional<TaskSubmission> submissionOpt = taskSubmissionRepository
                    .findByTaskIdAndStudentIdAndIsDeletedFalse(taskId, studentId);
            if (submissionOpt.isEmpty()) {
                return new Response(false, "No submission found for this task", null);
            }
            return new Response(true, "Student submission retrieved successfully", toDto(submissionOpt.get()));
        } catch (Exception e) {
            logger.error("Failed to get student submissions: {}", e.getMessage(), e);
            return new Response(false, "Failed to get submissions: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getTaskSubmissions(Long taskId) {
        try {
            List<TaskSubmission> submissions = taskSubmissionRepository
                    .findByTaskIdAndIsDeletedFalseOrderBySubmittedAtDesc(taskId);
            List<TaskSubmissionResponse> dtos = submissions.stream().map(this::toDto).toList();
            return new Response(true, "Task submissions retrieved successfully", dtos);
        } catch (Exception e) {
            logger.error("Failed to get task submissions: {}", e.getMessage(), e);
            return new Response(false, "Failed to get task submissions: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response gradeSubmission(Long submissionId, Long graderId, boolean isCompleted, String feedback) {
        try {
            Optional<TaskSubmission> submissionOpt = taskSubmissionRepository.findById(submissionId);
            if (submissionOpt.isEmpty()) {
                return new Response(false, "Submission not found", null);
            }

            Optional<User> graderOpt = userRepository.findById(graderId);
            if (graderOpt.isEmpty()) {
                return new Response(false, "Grader not found", null);
            }

            TaskSubmission submission = submissionOpt.get();
            ActivityTask task = submission.getTask();
            Activity activity = task.getActivity();

            // Set score to 0 for backward compatibility
            submission.setIsCompleted(isCompleted);
            submission.setScore(0.0);
            submission.setFeedback(feedback);
            submission.setGrader(graderOpt.get());
            submission.setStatus(SubmissionStatus.GRADED);
            submission.setGradedAt(LocalDateTime.now());

            taskSubmissionRepository.save(submission);

            // Cập nhật TaskAssignment status sang COMPLETED khi chấm điểm
            try {
                Optional<TaskAssignment> assignmentOpt = taskAssignmentRepository
                        .findByTaskIdAndStudentId(task.getId(), submission.getStudent().getId());
                if (assignmentOpt.isPresent()) {
                    TaskAssignment assignment = assignmentOpt.get();
                    assignment.setStatus(TaskStatus.COMPLETED);
                    taskAssignmentRepository.save(assignment);
                    logger.info("Updated TaskAssignment status to COMPLETED for task {} and student {}",
                            task.getId(), submission.getStudent().getId());
                }
            } catch (Exception e) {
                logger.warn("Failed to update TaskAssignment status after grading: {}", e.getMessage());
            }

            try {
                finalizeSubmissionResultIfEligible(submission, graderOpt.get());
            } catch (Exception ex) {
                logger.warn("Auto-update participation/score after grading failed: {}", ex.getMessage());
            }

            return new Response(true, "Submission graded successfully", toDto(submission));
        } catch (Exception e) {
            logger.error("Failed to grade submission: {}", e.getMessage(), e);
            return new Response(false, "Failed to grade submission: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getSubmissionDetails(Long submissionId) {
        try {
            Optional<TaskSubmission> submissionOpt = taskSubmissionRepository.findById(submissionId);
            if (submissionOpt.isEmpty()) {
                return new Response(false, "Submission not found", null);
            }

            return new Response(true, "Submission details retrieved successfully", toDto(submissionOpt.get()));
        } catch (Exception e) {
            logger.error("Failed to get submission details: {}", e.getMessage(), e);
            return new Response(false, "Failed to get submission details: " + e.getMessage(), null);
        }
    }

    @Override
    @Transactional
    public Response deleteSubmission(Long submissionId, Long studentId) {
        try {
            Optional<TaskSubmission> submissionOpt = taskSubmissionRepository.findById(submissionId);
            if (submissionOpt.isEmpty()) {
                return new Response(false, "Submission not found", null);
            }

            TaskSubmission submission = submissionOpt.get();
            if (!submission.getStudent().getId().equals(studentId)) {
                return new Response(false, "Unauthorized to delete this submission", null);
            }

            submission.setDeleted(true);
            taskSubmissionRepository.save(submission);

            taskAssignmentRepository.findByTaskIdAndStudentId(
                    submission.getTask().getId(),
                    submission.getStudent().getId())
                    .ifPresent(reminderScheduleService::createTaskRemindersForAssignment);

            return new Response(true, "Submission deleted successfully", null);
        } catch (Exception e) {
            logger.error("Failed to delete submission: {}", e.getMessage(), e);
            return new Response(false, "Failed to delete submission: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getSubmissionFiles(Long submissionId) {
        try {
            Optional<TaskSubmission> submissionOpt = taskSubmissionRepository.findById(submissionId);
            if (submissionOpt.isEmpty()) {
                return new Response(false, "Submission not found", null);
            }

            TaskSubmission submission = submissionOpt.get();
            return new Response(true, "Submission files retrieved successfully",
                    buildAttachmentItems(submission.getFileUrls()));
        } catch (Exception e) {
            logger.error("Failed to get submission files: {}", e.getMessage(), e);
            return new Response(false, "Failed to get submission files: " + e.getMessage(), null);
        }
    }

    private TaskSubmissionResponse toDto(TaskSubmission submission) {
        TaskSubmissionResponse dto = new TaskSubmissionResponse();
        dto.setId(submission.getId());

        if (submission.getTask() != null) {
            dto.setTaskId(submission.getTask().getId());
            dto.setTaskTitle(submission.getTask().getName());
        }

        if (submission.getStudent() != null) {
            dto.setStudentId(submission.getStudent().getId());
            dto.setStudentCode(submission.getStudent().getStudentCode());
            dto.setStudentName(submission.getStudent().getFullName());
        }

        dto.setContent(submission.getContent());
        if (submission.getFileUrls() != null && !submission.getFileUrls().isEmpty()) {
            // Convert relative paths to full URLs
            List<String> fileUrls = Arrays.asList(submission.getFileUrls().split(","));
            List<String> fullUrls = fileUrls.stream()
                    .map(url -> uploadStorageService.toPublicUrl(url.trim()))
                    .collect(java.util.stream.Collectors.toList());
            dto.setFileUrls(fullUrls);
        }
        dto.setAttachments(buildAttachmentItems(submission.getFileUrls()));

        dto.setScore(submission.getScore());
        dto.setIsCompleted(submission.getIsCompleted());
        dto.setFeedback(submission.getFeedback());
        if (submission.getGrader() != null) {
            dto.setGraderId(submission.getGrader().getId());
            dto.setGraderUsername(submission.getGrader().getUsername());
        }

        dto.setStatus(submission.getStatus());
        dto.setSubmittedAt(submission.getSubmittedAt());
        dto.setUpdatedAt(submission.getUpdatedAt());
        dto.setGradedAt(submission.getGradedAt());
        return dto;
    }

    private List<String> storeSubmissionAttachments(List<MultipartFile> files, List<MultipartFile> images)
            throws IOException {
        List<String> attachmentUrls = new ArrayList<>();
        String submissionDirectory = uploadProperties.getPaths().getSubmissions();
        attachmentUrls.addAll(storeFiles(files, submissionDirectory, false));
        attachmentUrls.addAll(storeFiles(images, submissionDirectory, true));
        return attachmentUrls;
    }

    private List<String> storeFiles(List<MultipartFile> files, String relativeDirectory, boolean imageOnly)
            throws IOException {
        List<String> storedPaths = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return storedPaths;
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            storedPaths.add(uploadStorageService.store(file, relativeDirectory, imageOnly));
        }
        return storedPaths;
    }

    private List<TaskSubmissionResponse.AttachmentItem> buildAttachmentItems(String storedUrls) {
        List<TaskSubmissionResponse.AttachmentItem> attachments = new ArrayList<>();
        if (storedUrls == null || storedUrls.isBlank()) {
            return attachments;
        }

        List<String> relativeUrls = Arrays.asList(storedUrls.split(","));
        for (String relativeUrl : relativeUrls) {
            String trimmedUrl = relativeUrl == null ? "" : relativeUrl.trim();
            if (trimmedUrl.isEmpty()) {
                continue;
            }

            TaskSubmissionResponse.AttachmentItem attachmentItem = new TaskSubmissionResponse.AttachmentItem();
            attachmentItem.setUrl(uploadStorageService.toPublicUrl(trimmedUrl));
            attachmentItem.setType(detectAttachmentType(trimmedUrl));
            attachments.add(attachmentItem);
        }
        return attachments;
    }

    private String detectAttachmentType(String path) {
        String normalized = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".jpg")
                || normalized.endsWith(".jpeg")
                || normalized.endsWith(".png")
                || normalized.endsWith(".gif")
                || normalized.endsWith(".webp")
                || normalized.endsWith(".bmp")
                || normalized.endsWith(".svg")) {
            return "image";
        }
        return "file";
    }

    private void finalizeSubmissionResultIfEligible(TaskSubmission submission, User actor) {
        ActivityTask task = submission.getTask();
        Activity activity = task != null ? task.getActivity() : null;
        if (activity == null || !activity.isRequiresSubmission()) {
            return;
        }

        Student student = submission.getStudent();
        Optional<ActivityRegistration> regOpt = activityRegistrationRepository
                .findByActivityIdAndStudentId(activity.getId(), student.getId());
        if (regOpt.isEmpty()) {
            return;
        }

        ActivityRegistration registration = regOpt.get();
        if (registration.getStatus() != RegistrationStatus.ATTENDED) {
            logger.info("Submission {} graded before attendance is confirmed for registration {}",
                    submission.getId(), registration.getId());
            return;
        }

        if (activity.getSeriesId() != null) {
            activityParticipationRepository.findByRegistration(registration)
                    .ifPresent(participation -> {
                        participation.setIsCompleted(submission.getIsCompleted());
                        participation.setPointsEarned(BigDecimal.ZERO);
                        participation.setParticipationType(ParticipationType.COMPLETED);
                        activityParticipationRepository.save(participation);
                    });

            if (Boolean.TRUE.equals(submission.getIsCompleted())) {
                try {
                    activitySeriesService.updateStudentProgress(student.getId(), activity.getId());
                    logger.info("Updated series progress for submission activity {} in series {}",
                            activity.getName(), activity.getSeriesId());
                } catch (Exception e) {
                    logger.warn("Failed to update series progress: {}", e.getMessage());
                }
            }
            return;
        }

        List<AppliedScoreAward> awards = scoreRuleEngine.applySubmissionGraded(submission, actor);
        BigDecimal totalPoints = awards != null ? awards.stream()
                .map(AppliedScoreAward::getPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        activityParticipationRepository.findByRegistration(registration)
                .ifPresent(participation -> {
                    participation.setIsCompleted(submission.getIsCompleted());
                    participation.setPointsEarned(totalPoints);
                    participation.setParticipationType(ParticipationType.COMPLETED);
                    activityParticipationRepository.save(participation);
                });
    }

}
