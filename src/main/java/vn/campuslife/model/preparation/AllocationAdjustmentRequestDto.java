package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.AllocationAdjustmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllocationAdjustmentRequestDto {
    private Long id;
    private Long activityId;
    private Long taskId;
    private BigDecimal amount;
    private String description;
    private AllocationAdjustmentStatus status;
    private Long requestedById;
    private String requestedByName;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;
    private Long decidedById;
}
