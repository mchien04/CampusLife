package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.PreparationTaskMemberRole;
import vn.campuslife.enumeration.PreparationTaskStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private boolean isCheckinScanner;
    private PreparationTaskStatus status;
    private PreparationTaskMemberRole myRole;
    private List<String> completionProofUrls;
}

