package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllocationSourceSuggestionDto {
    private Long categoryId;
    private String categoryName;
    private BigDecimal availableToAllocateAmount;
}

