package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.PreparationTaskStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreparationTaskDto {
    private Long id;
    private Long activityId;
    private Long assigneeId;
    private String assigneeName;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private PreparationTaskStatus status;
}

