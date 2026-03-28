package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ExpenseStatus;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceStatusSummaryDto {
    private ExpenseStatus status;
    private Long count;
    private BigDecimal totalAmount;
}

