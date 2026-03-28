package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityBudgetDto {
    private Long id;
    private Long activityId;
    private BigDecimal totalAmount;
    private List<BudgetCategoryDto> categories;
}

