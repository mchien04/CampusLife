package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.PreparationTaskMemberRole;
import vn.campuslife.enumeration.PreparationTaskStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyPreparationTaskDto {
    private Long id;
    private Long activityId;
    private Long ownerId;
    private String ownerName;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private BigDecimal allocatedAmount;
    private boolean isFinancial;
    private PreparationTaskStatus status;
    private PreparationTaskMemberRole myRole;
}

