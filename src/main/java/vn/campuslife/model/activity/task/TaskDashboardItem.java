package vn.campuslife.model.activity.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDashboardItem {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime deadline;
    private Long activityId;
    private String activityName;
    private LocalDateTime createdAt;

    // Submission stats
    private Long submissionCount;
    private Long gradedCount;
    private Long pendingGradeCount;

    // Submissions
    private List<TaskSubmissionSummary> submissions;
}
