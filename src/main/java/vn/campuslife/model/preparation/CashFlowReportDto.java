package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowReportDto {
    private Long activityId;
    private BigDecimal totalBudget;
    private BigDecimal approvedSpent;
    private BigDecimal cashOutsideWallet;
    private BigDecimal cashInsideWallet;
    private List<FundAdvanceDebtDto> advanceDebts;
    private List<InvoiceStatusSummaryDto> invoiceStatusSummary;
}

