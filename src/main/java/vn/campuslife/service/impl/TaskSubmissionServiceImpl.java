package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.SubmissionStatus;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.TaskStatus;
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
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ActivityParticipationRepository activityParticipationRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

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

            // Cập nhật TaskAssignment status sang ASSIGNED khi sinh viên nộp bài
            try {
                Optional<TaskAssignment> assignmentOpt = taskAssignmentRepository
                        .findByTaskIdAndStudentId(taskId, studentId);
                if (assignmentOpt.isPresent()) {
                    TaskAssignment assignment = assignmentOpt.get();
                    assignment.setStatus(TaskStatus.ASSIGNED);
                    taskAssignmentRepository.save(assignment);
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

            // Tính điểm từ isCompleted và activity points
            java.math.BigDecimal points;
            if (isCompleted) {
                // Đạt: điểm cộng từ maxPoints
                points = activity.getMaxPoints() != null ? activity.getMaxPoints() : java.math.BigDecimal.ZERO;
            } else {
                // Không đạt: điểm trừ từ penaltyPointsIncomplete
                java.math.BigDecimal penalty = activity.getPenaltyPointsIncomplete() != null
                        ? activity.getPenaltyPointsIncomplete()
                        : java.math.BigDecimal.ZERO;
                points = penalty.negate(); // Chuyển thành số âm
            }

            submission.setIsCompleted(isCompleted);
            submission.setScore(points.doubleValue()); // Lưu điểm số để backward compatibility
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
                // Không fail grading nếu update assignment status lỗi
            }

            // Tự động cập nhật ActivityParticipation và tổng hợp StudentScore nếu đủ điều
            // kiện
            try {
                Student student = submission.getStudent();

                if (activity != null && activity.isRequiresSubmission()) {
                    // Tìm registration của student cho activity này
                    Optional<ActivityRegistration> regOpt = activityRegistrationRepository
                            .findByActivityIdAndStudentId(activity.getId(), student.getId());
                    if (regOpt.isPresent()) {
                        ActivityRegistration registration = regOpt.get();

                        // Chỉ tự động khi đã ATTENDED (đã check-in/out)
                        if (registration.getStatus() == vn.campuslife.enumeration.RegistrationStatus.ATTENDED) {
                            // Lấy participation theo registration
                            Optional<ActivityParticipation> partOpt = activityParticipationRepository
                                    .findByRegistration(registration);
                            if (partOpt.isPresent()) {
                                ActivityParticipation participation = partOpt.get();

                                // Cập nhật participation với điểm đã tính từ isCompleted
                                participation.setIsCompleted(isCompleted);
                                participation.setPointsEarned(points);
                                participation.setParticipationType(ParticipationType.COMPLETED);
                                activityParticipationRepository.save(participation);

                                // Cộng dồn lại điểm StudentScore theo scoreType của activity
                                // Lấy tất cả participation COMPLETED cùng student và scoreType
                                java.util.List<ActivityParticipation> allParts = activityParticipationRepository
                                        .findByStudentIdAndScoreType(student.getId(), activity.getScoreType());

                                java.math.BigDecimal total = allParts.stream()
                                        .filter(p -> p.getParticipationType() == ParticipationType.COMPLETED)
                                        .map(p -> p.getPointsEarned() != null ? p.getPointsEarned()
                                                : java.math.BigDecimal.ZERO)
                                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                                // Cập nhật bản ghi StudentScore tương ứng ở học kỳ hiện tại (đang mở)
                                Optional<Semester> currentSemester = semesterRepository.findAll().stream()
                                        .filter(Semester::isOpen)
                                        .findFirst();
                                if (currentSemester.isPresent()) {
                                    Optional<StudentScore> scoreOpt = studentScoreRepository
                                            .findByStudentIdAndSemesterIdAndScoreType(
                                                    student.getId(),
                                                    currentSemester.get().getId(),
                                                    activity.getScoreType());
                                    if (scoreOpt.isPresent()) {
                                        StudentScore agg = scoreOpt.get();
                                        java.math.BigDecimal oldTotal = agg.getScore();
                                        agg.setScore(total);
                                        studentScoreRepository.save(agg);

                                        // Lưu lịch sử nếu thay đổi
                                        if (oldTotal == null || oldTotal.compareTo(total) != 0) {
                                            ScoreHistory hist = new ScoreHistory();
                                            hist.setScore(agg);
                                            hist.setOldScore(oldTotal);
                                            hist.setNewScore(total);
                                            hist.setChangedBy(graderOpt.get());
                                            hist.setChangeDate(LocalDateTime.now());
                                            hist.setReason("Auto update from graded submission and completion");
                                            hist.setActivityId(activity.getId());
                                            scoreHistoryRepository.save(hist);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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

            // Lấy học kỳ hiện tại
            Optional<Semester> currentSemester = semesterRepository.findAll().stream()
                    .filter(s -> s.isOpen())
                    .findFirst();
            if (currentSemester.isEmpty()) {
                logger.warn("No open semester found for score creation");
                return;
            }

            // Tìm bản ghi điểm tổng hợp theo scoreType của activity
            Optional<StudentScore> scoreOpt = studentScoreRepository
                    .findByStudentIdAndSemesterIdAndScoreType(
                            student.getId(),
                            currentSemester.get().getId(),
                            task.getActivity().getScoreType());

            if (scoreOpt.isEmpty()) {
                logger.warn("No aggregate score record found for student {} scoreType {} in semester {}",
                        student.getId(), task.getActivity().getScoreType(), currentSemester.get().getId());
                return;
            }

            StudentScore score = scoreOpt.get();

            // Cộng điểm
            BigDecimal oldScore = score.getScore();
            BigDecimal pointsToAdd = BigDecimal.valueOf(submission.getScore());
            BigDecimal newScore = oldScore.add(pointsToAdd);

            // Cập nhật
            score.setScore(newScore);
            studentScoreRepository.save(score);

            // Tạo ScoreHistory
            User grader = submission.getGrader();
            if (grader == null) {
                grader = userRepository.findById(1L).orElse(null);
            }

            Long activityId = task.getActivity().getId();
            ScoreHistory history = new ScoreHistory();
            history.setScore(score);
            history.setOldScore(oldScore);
            history.setNewScore(newScore);
            history.setChangedBy(grader);
            history.setChangeDate(LocalDateTime.now());
            history.setReason("Added points from task submission: " + task.getName());
            history.setActivityId(activityId);

            scoreHistoryRepository.save(history);

            logger.info("Added score {} (total: {}) for student {} from task submission {}",
                    pointsToAdd, newScore, student.getId(), submission.getId());

        } catch (Exception e) {
            logger.error("Failed to create score from submission: {}", e.getMessage(), e);
        }
    }
}
