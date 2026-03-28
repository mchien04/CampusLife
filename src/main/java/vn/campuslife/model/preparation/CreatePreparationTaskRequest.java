package vn.campuslife.model.preparation;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePreparationTaskRequest {
    private Long activityId;

    @NotNull(message = "Assignee ID is required")
    @JsonAlias("assigneeId")
    private Long ownerId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private LocalDateTime deadline;

    private Boolean isFinancial;
}
