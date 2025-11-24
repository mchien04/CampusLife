package vn.campuslife.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityTaskResponse {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime deadline;
    private Long activityId;
    private String activityName;
    private LocalDateTime createdAt;
    private List<TaskAssignmentResponse> assignments;

    // Thống kê
    private Long totalAssignments;
    private Long completedAssignments;
    private Long pendingAssignments;
}
