package vn.campuslife.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import vn.campuslife.enumeration.TaskStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskAssignmentResponse {

    private Long id;
    private Long taskId;
    private String taskName;
    private Long activityId; // ID của sự kiện chứa nhiệm vụ này
    private String activityName; // Tên sự kiện
    private Long studentId;
    private String studentName;
    private String studentCode;
    private TaskStatus status;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
