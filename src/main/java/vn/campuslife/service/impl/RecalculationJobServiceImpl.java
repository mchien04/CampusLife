package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.RecalculationJob;
import vn.campuslife.entity.Semester;
import vn.campuslife.entity.Student;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.Response;
import vn.campuslife.repository.RecalculationJobRepository;
import vn.campuslife.repository.SemesterRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.service.RecalculationJobService;
import vn.campuslife.service.ScoreEntryService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecalculationJobServiceImpl implements RecalculationJobService {

    private static final Logger logger = LoggerFactory.getLogger(RecalculationJobServiceImpl.class);
    private static final int BATCH_SIZE = 100;
    private static final int MAX_JOB_DURATION_MINUTES = 30;

    private final RecalculationJobRepository jobRepository;
    private final SemesterRepository semesterRepository;
    private final StudentRepository studentRepository;
    private final ScoreEntryService scoreEntryService;

    @Override
    @Transactional
    public Response startAsyncRecalculation(Long semesterId, Long createdBy) {
        try {
            // Resolve semester
            Semester semester;
            if (semesterId != null) {
                Optional<Semester> semesterOpt = semesterRepository.findById(semesterId);
                if (semesterOpt.isEmpty()) {
                    return Response.error("Semester not found");
                }
                semester = semesterOpt.get();
            } else {
                semester = semesterRepository.findAll().stream()
                        .filter(Semester::isOpen)
                        .findFirst()
                        .orElse(semesterRepository.findAll().stream().findFirst().orElse(null));
                if (semester == null) {
                    return Response.error("No semester found");
                }
            }

            // Concurrency lock: check for existing active jobs
            long activeCount = jobRepository.countBySemesterIdAndStatusIn(
                    semester.getId(), List.of("PENDING", "RUNNING"));
            if (activeCount > 0) {
                return new Response(false,
                        "A recalculation job is already running for this semester", null);
            }

            // Count active students
            List<Student> students = studentRepository.findByIsDeletedFalse();
            int totalStudents = students.size();

            // Create job record
            RecalculationJob job = new RecalculationJob();
            job.setSemesterId(semester.getId());
            job.setStatus("PENDING");
            job.setTotalStudents(totalStudents);
            job.setCreatedBy(createdBy);
            job.setCreatedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            job = jobRepository.save(job);

            // Launch async processing
            processRecalculationJob(job.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("jobId", job.getId());
            result.put("semesterId", semester.getId());
            result.put("semesterName", semester.getName());
            result.put("totalStudents", totalStudents);
            result.put("status", "PENDING");

            return Response.success("Recalculation job started", result);
        } catch (Exception e) {
            logger.error("Failed to start recalculation job: {}", e.getMessage(), e);
            return Response.error("Failed to start recalculation job: " + e.getMessage());
        }
    }

    @Async("recalculationExecutor")
    @Transactional
    public void processRecalculationJob(Long jobId) {
        RecalculationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            logger.error("Recalculation job {} not found", jobId);
            return;
        }

        job.setStatus("RUNNING");
        job.setStartedAt(LocalDateTime.now());
        jobRepository.save(job);

        try {
            List<Student> students = studentRepository.findByIsDeletedFalse();
            int totalStudents = students.size();
            int processedCount = 0;
            int errorCount = 0;
            StringBuilder errorDetails = new StringBuilder();
            LocalDateTime startTime = LocalDateTime.now();

            for (int i = 0; i < totalStudents; i += BATCH_SIZE) {
                // Timeout protection
                if (startTime.plusMinutes(MAX_JOB_DURATION_MINUTES).isBefore(LocalDateTime.now())) {
                    job.setStatus("TIMEOUT");
                    job.setProcessedStudents(processedCount);
                    job.setErrorCount(errorCount);
                    job.setErrorDetails(errorDetails.toString());
                    job.setCompletedAt(LocalDateTime.now());
                    jobRepository.save(job);
                    logger.warn("Recalculation job {} timed out after {} minutes", jobId, MAX_JOB_DURATION_MINUTES);
                    return;
                }

                int end = Math.min(i + BATCH_SIZE, totalStudents);
                List<Student> batch = students.subList(i, end);

                for (Student student : batch) {
                    try {
                        for (ScoreType type : ScoreType.values()) {
                            scoreEntryService.refreshStudentScore(student.getId(), job.getSemesterId(), type);
                        }
                        processedCount++;
                    } catch (Exception e) {
                        errorCount++;
                        errorDetails.append(String.format("Student %d: %s\n", student.getId(), e.getMessage()));
                        logger.error("Failed to recalculate for student {} in job {}: {}",
                                student.getId(), jobId, e.getMessage());
                    }
                }

                // Update progress after each batch
                job.setProcessedStudents(processedCount);
                job.setErrorCount(errorCount);
                jobRepository.save(job);

                logger.info("Recalculation job {} progress: {}/{} students processed",
                        jobId, processedCount, totalStudents);
            }

            // Mark completed
            job.setStatus(errorCount > 0 && processedCount == 0 ? "FAILED" : "COMPLETED");
            job.setErrorDetails(errorDetails.length() > 0 ? errorDetails.toString() : null);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);

            logger.info("Recalculation job {} completed: {} success, {} errors",
                    jobId, processedCount, errorCount);

        } catch (Exception e) {
            logger.error("Recalculation job {} failed: {}", jobId, e.getMessage(), e);
            job.setStatus("FAILED");
            job.setErrorDetails(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
        }
    }

    @Override
    public Response getJobStatus(Long jobId) {
        try {
            Optional<RecalculationJob> jobOpt = jobRepository.findById(jobId);
            if (jobOpt.isEmpty()) {
                return Response.error("Recalculation job not found");
            }

            RecalculationJob job = jobOpt.get();
            Map<String, Object> result = new HashMap<>();
            result.put("id", job.getId());
            result.put("semesterId", job.getSemesterId());
            result.put("status", job.getStatus());
            result.put("totalStudents", job.getTotalStudents());
            result.put("processedStudents", job.getProcessedStudents());
            result.put("errorCount", job.getErrorCount());
            result.put("startedAt", job.getStartedAt());
            result.put("completedAt", job.getCompletedAt());
            result.put("createdAt", job.getCreatedAt());

            // Calculate progress percentage
            if (job.getTotalStudents() > 0) {
                double progress = (double) job.getProcessedStudents() / job.getTotalStudents() * 100;
                result.put("progressPercent", Math.round(progress * 100.0) / 100.0);
            }

            return Response.success("Job status retrieved", result);
        } catch (Exception e) {
            return Response.error("Failed to get job status: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response retryFailedJob(Long jobId) {
        try {
            Optional<RecalculationJob> jobOpt = jobRepository.findById(jobId);
            if (jobOpt.isEmpty()) {
                return Response.error("Recalculation job not found");
            }

            RecalculationJob originalJob = jobOpt.get();
            if (!"FAILED".equals(originalJob.getStatus()) && !"TIMEOUT".equals(originalJob.getStatus())) {
                return Response.error("Only FAILED or TIMEOUT jobs can be retried");
            }

            // Create a new job for the same semester
            return startAsyncRecalculation(originalJob.getSemesterId(), originalJob.getCreatedBy());
        } catch (Exception e) {
            return Response.error("Failed to retry job: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void cleanupStaleJobs() {
        List<RecalculationJob> runningJobs = jobRepository.findByStatus("RUNNING");
        for (RecalculationJob job : runningJobs) {
            logger.warn("Marking stale RUNNING job {} as FAILED (app restart cleanup)", job.getId());
            job.setStatus("FAILED");
            job.setErrorDetails("Application restarted while job was running");
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
        }
    }
}
