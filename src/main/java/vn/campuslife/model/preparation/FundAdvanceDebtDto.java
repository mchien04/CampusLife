package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundAdvanceDebtDto {
    private Long studentId;
    private String studentName;
    private BigDecimal holdingAmount;
}

