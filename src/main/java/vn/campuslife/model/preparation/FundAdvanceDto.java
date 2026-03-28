package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.FundAdvanceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundAdvanceDto {
    private Long id;
    private Long taskId;
    private Long studentId;
    private String studentName;
    private Long requestedById;
    private String requestedByName;
    private BigDecimal amount;
    private BigDecimal remainingAmount;
    private FundAdvanceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;
}
