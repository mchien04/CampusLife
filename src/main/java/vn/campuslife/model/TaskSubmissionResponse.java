package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskSubmissionResponse {
    private Long id;

    // Task
    private Long taskId;
    private String taskTitle;

    // Student
    private Long studentId;
    private String studentCode;
    private String studentName;

    // Content & files
    private String content;
    private List<String> fileUrls;

    // Grading
    private Double score;
    private String feedback;
    private Long graderId;
    private String graderUsername;

    // Status & timestamps
    private SubmissionStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime gradedAt;
}
