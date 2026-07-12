package vn.campuslife.model.activity.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmissionSummary {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentCode;
    private String content;
    private List<String> fileUrls;
    private Boolean isCompleted;
    private String feedback;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime gradedAt;
}
