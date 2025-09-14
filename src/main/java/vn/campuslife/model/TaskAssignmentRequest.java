package vn.campuslife.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import vn.campuslife.enumeration.TaskStatus;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskAssignmentRequest {

    @NotNull(message = "Task ID is required")
    private Long taskId;

    @NotEmpty(message = "Student IDs are required")
    private List<Long> studentIds;

    private TaskStatus status = TaskStatus.PENDING;
}
