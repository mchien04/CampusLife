package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ExpenseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDto {
    private Long id;
    private Long activityId;
    private Long taskId;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private String description;
    private String evidenceUrl;
    private ExpenseStatus status;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
}
