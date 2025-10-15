package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.SubmissionStatus;
import vn.campuslife.enumeration.ScoreSourceType;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.Response;
import vn.campuslife.model.TaskSubmissionResponse;
import vn.campuslife.repository.*;
import vn.campuslife.service.TaskSubmissionService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskSubmissionServiceImpl implements TaskSubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(TaskSubmissionServiceImpl.class);
    private static final String UPLOAD_DIR = "uploads/submissions";

    private final TaskSubmissionRepository taskSubmissionRepository;
    private final ActivityTaskRepository activityTaskRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final SemesterRepository semesterRepository;

    @Override
    @Transactional
    public Response submitTask(Long taskId, Long studentId, String content, List<MultipartFile> files) {
        try {
            // Validate task exists
            Optional<ActivityTask> taskOpt = activityTaskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                return new Response(false, "Task not found", null);
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

            // Handle file uploads
            if (files != null && !files.isEmpty()) {
                List<String> fileUrls = new ArrayList<>();
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                        Path filePath = Paths.get(UPLOAD_DIR, fileName);
                        Files.createDirectories(filePath.getParent());
                        Files.copy(file.getInputStream(), filePath);
                        fileUrls.add("/uploads/submissions/" + fileName);
                    }
                }
                submission.setFileUrls(String.join(",", fileUrls));
            }

            taskSubmissionRepository.save(submission);
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
    public Response updateSubmission(Long submissionId, Long studentId, String content, List<MultipartFile> files) {
        try {
            Optional<TaskSubmission> submissionOpt = taskSubmissionRepository.findById(submissionId);
            if (submissionOpt.isEmpty()) {
                return new Response(false, "Submission not found", null);
            }

            TaskSubmission submission = submissionOpt.get();
            if (!submission.getStudent().getId().equals(studentId)) {
                return new Response(false, "Unauthorized to update this submission", null);
            }

            submission.setContent(content);

            // Handle file uploads
            if (files != null && !files.isEmpty()) {
                List<String> fileUrls = new ArrayList<>();
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                        Path filePath = Paths.get(UPLOAD_DIR, fileName);
                        Files.createDirectories(filePath.getParent());
                        Files.copy(file.getInputStream(), filePath);
                        fileUrls.add("/uploads/submissions/" + fileName);
                    }
                }
                submission.setFileUrls(String.join(",", fileUrls));
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
    // @Transactional - Tạm thời bỏ để test
    public Response gradeSubmission(Long submissionId, Long graderId, Double score, String feedback) {
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
            submission.setScore(score);
            submission.setFeedback(feedback);
            submission.setGrader(graderOpt.get());
            submission.setStatus(SubmissionStatus.GRADED);
            submission.setGradedAt(LocalDateTime.now());

            taskSubmissionRepository.save(submission);

            // Tạm thời disable tự động tạo StudentScore để test
            // createScoreFromSubmission(submission);

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
            List<String> fileUrls = new ArrayList<>();
            if (submission.getFileUrls() != null && !submission.getFileUrls().isEmpty()) {
                fileUrls = Arrays.asList(submission.getFileUrls().split(","));
            }

            return new Response(true, "Submission files retrieved successfully", fileUrls);
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
            dto.setFileUrls(Arrays.asList(submission.getFileUrls().split(",")));
        }

        dto.setScore(submission.getScore());
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

    /**
     * Tự động tạo StudentScore từ chấm điểm submission
     */
    private void createScoreFromSubmission(TaskSubmission submission) {
        try {
            logger.info("Creating score from submission {} with score {}", submission.getId(), submission.getScore());

            if (submission.getScore() == null || submission.getScore() <= 0) {
                logger.info("No score to create for submission {}", submission.getId());
                return;
            }

            ActivityTask task = submission.getTask();
            Student student = submission.getStudent();

            logger.info("Task: {}, Student: {}, Grader: {}", task.getId(), student.getId(),
                    submission.getGrader() != null ? submission.getGrader().getId() : "null");

            // Lấy học kỳ hiện tại (có thể cần logic phức tạp hơn)
            Optional<Semester> currentSemester = semesterRepository.findAll().stream().findFirst();
            if (currentSemester.isEmpty()) {
                logger.warn("No semester found for score creation");
                return;
            }

            // Kiểm tra xem đã có điểm cho submission này chưa
            boolean exists = studentScoreRepository.existsByStudentIdAndSubmissionIdAndScoreSourceType(
                    student.getId(), submission.getId(), ScoreSourceType.ACTIVITY_SUBMISSION);

            if (exists) {
                logger.info("Score already exists for submission {} and student {}", submission.getId(),
                        student.getId());
                return;
            }

            // Đảm bảo grader không null
            if (submission.getGrader() == null) {
                logger.warn("Grader is null for submission {}, skipping score creation", submission.getId());
                return;
            }

            // Tạo StudentScore
            StudentScore score = new StudentScore();
            score.setStudent(student);
            score.setSemester(currentSemester.get());
            score.setCriterion(null); // Không có criterion cụ thể cho task
            score.setScore(BigDecimal.valueOf(submission.getScore()));
            score.setEnteredBy(submission.getGrader()); // Đã kiểm tra không null
            score.setEntryDate(LocalDateTime.now());
            score.setUpdatedDate(LocalDateTime.now());
            score.setScoreType(task.getActivity().getScoreType());
            score.setScoreSourceType(ScoreSourceType.ACTIVITY_SUBMISSION);
            score.setActivityId(task.getActivity().getId());
            score.setTaskId(task.getId());
            score.setSubmissionId(submission.getId());
            score.setSourceNote("Auto-generated from task submission: " + task.getName());

            studentScoreRepository.save(score);

            // Tạo ScoreHistory
            ScoreHistory history = new ScoreHistory();
            history.setScore(score);
            history.setOldScore(BigDecimal.ZERO);
            history.setNewScore(score.getScore());
            history.setChangedBy(submission.getGrader()); // Đã kiểm tra không null ở trên
            history.setChangeDate(LocalDateTime.now());
            history.setScoreSourceType(ScoreSourceType.ACTIVITY_SUBMISSION);
            history.setReason("Auto-generated from task submission");
            history.setActivityId(task.getActivity().getId());
            history.setTaskId(task.getId());
            history.setSubmissionId(submission.getId());

            scoreHistoryRepository.save(history);

            logger.info("Created score {} for student {} from task submission {}",
                    score.getScore(), student.getId(), submission.getId());

        } catch (Exception e) {
            logger.error("Failed to create score from submission: {}", e.getMessage(), e);
        }
    }
}
