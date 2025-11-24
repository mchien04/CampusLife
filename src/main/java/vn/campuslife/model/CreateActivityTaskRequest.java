package vn.campuslife.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateActivityTaskRequest {

    @NotBlank(message = "Task name is required")
    private String name;

    private String description;

    private LocalDateTime deadline;

    @NotNull(message = "Activity ID is required")
    private Long activityId;
}
