package vn.campuslife.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Anchor;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfContentByte;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.AuditLog;
import vn.campuslife.entity.Expense;
import vn.campuslife.entity.FundAdvance;
import vn.campuslife.entity.PreparationTask;
import vn.campuslife.entity.PreparationTaskMember;
import vn.campuslife.entity.Student;
import vn.campuslife.enumeration.PreparationTaskMemberRole;
import vn.campuslife.exception.FeatureNotEnabledException;
import vn.campuslife.exception.ResourceNotFoundException;
import vn.campuslife.model.preparation.BudgetCategoryDto;
import vn.campuslife.model.preparation.CashFlowReportDto;
import vn.campuslife.model.preparation.ExpenseDto;
import vn.campuslife.model.preparation.FinanceOverviewReportDto;
import vn.campuslife.model.preparation.FundAdvanceDebtDto;
import vn.campuslife.model.preparation.InvoiceStatusSummaryDto;
import vn.campuslife.repository.ActivityOrganizerRepository;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.AllocationAdjustmentRequestRepository;
import vn.campuslife.repository.AuditLogRepository;
import vn.campuslife.repository.ExpenseRepository;
import vn.campuslife.repository.FundAdvanceRepository;
import vn.campuslife.repository.PreparationTaskMemberRepository;
import vn.campuslife.repository.PreparationTaskRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.service.PreparationExportService;
import vn.campuslife.service.PreparationFinanceService;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PreparationExportServiceImpl implements PreparationExportService {
    private final ActivityRepository activityRepository;
    private final ActivityOrganizerRepository activityOrganizerRepository;
    private final PreparationFinanceService financeService;
    private final PreparationTaskRepository preparationTaskRepository;
    private final PreparationTaskMemberRepository preparationTaskMemberRepository;
    private final StudentRepository studentRepository;
    private final ExpenseRepository expenseRepository;
    private final FundAdvanceRepository fundAdvanceRepository;
    private final AllocationAdjustmentRequestRepository allocationAdjustmentRequestRepository;
    private final AuditLogRepository auditLogRepository;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");
    private static final DateTimeFormatter TS_PRINT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static class FooterPageEvent extends PdfPageEventHelper {
        private final String footerLeft;

        private FooterPageEvent(String footerLeft) {
            this.footerLeft = footerLeft;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Font f = new Font(Font.HELVETICA, 9, Font.NORMAL);
            Phrase left = new Phrase(footerLeft, f);
            Phrase right = new Phrase("Page " + writer.getPageNumber(), f);

            float y = document.bottom() - 10;
            com.lowagie.text.pdf.ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, left, document.left(), y, 0);
            com.lowagie.text.pdf.ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, right, document.right(), y, 0);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ExportFile exportFinancial(Long activityId, String format) {
        Activity activity = getActiveActivity(activityId);
        requirePreparationEnabled(activity);

        if (isPdf(format)) {
            byte[] pdf = buildFinancialPdf(activityId);
            return new ExportFile(fileName("preparation_financial", activityId, "pdf"), "application/pdf", pdf);
        }
        byte[] xlsx = buildFinancialWorkbook(activityId);
        return new ExportFile(fileName("preparation_financial", activityId, "xlsx"),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportFile exportOperational(Long activityId, String format) {
        Activity activity = getActiveActivity(activityId);
        requirePreparationEnabled(activity);

        if (isPdf(format)) {
            byte[] pdf = buildOperationalPdf(activityId);
            return new ExportFile(fileName("preparation_operational", activityId, "pdf"), "application/pdf", pdf);
        }
        byte[] xlsx = buildOperationalWorkbook(activityId);
        return new ExportFile(fileName("preparation_operational", activityId, "xlsx"),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportFile exportAudit(Long activityId, String format) {
        Activity activity = getActiveActivity(activityId);
        requirePreparationEnabled(activity);

        if (isPdf(format)) {
            byte[] pdf = buildAuditPdf(activityId);
            return new ExportFile(fileName("preparation_audit", activityId, "pdf"), "application/pdf", pdf);
        }
        byte[] xlsx = buildAuditWorkbook(activityId);
        return new ExportFile(fileName("preparation_audit", activityId, "xlsx"),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx);
    }

    private byte[] buildFinancialWorkbook(Long activityId) {
        FinanceOverviewReportDto overview = financeService.getFinanceOverviewReport(activityId);
        CashFlowReportDto cashFlow = financeService.getCashFlowReport(activityId);
        List<FundAdvanceDebtDto> debts = financeService.listFundAdvanceDebts(activityId, null);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = headerStyle(wb);
            CellStyle money = moneyStyle(wb);
            CellStyle text = textStyle(wb);

            Sheet s1 = wb.createSheet("BudgetVsActual");
            int r = 0;
            Row h = s1.createRow(r++);
            writeRow(h, header, List.of("Wallet", "Allocated", "AllocatedToTasks", "AvailableToAllocate",
                    "CashOutside", "CashAvailable", "Used", "Remaining", "UsedPercent"));

            for (BudgetCategoryDto w : safeList(overview.getWallets())) {
                Row row = s1.createRow(r++);
                writeCell(row, 0, text, w.getName());
                writeCell(row, 1, money, w.getAllocatedAmount());
                writeCell(row, 2, money, w.getAllocatedToTasksAmount());
                writeCell(row, 3, money, w.getAvailableToAllocateAmount());
                writeCell(row, 4, money, w.getCashOutsideAmount());
                writeCell(row, 5, money, w.getCashAvailableAmount());
                writeCell(row, 6, money, w.getUsedAmount());
                writeCell(row, 7, money, w.getRemainingAmount());
                writeCell(row, 8, text, w.getUsedPercent() == null ? null : w.getUsedPercent() + "%");
            }
            autosize(s1, 9);

            Sheet s2 = wb.createSheet("CashFlow");
            int r2 = 0;
            Row meta = s2.createRow(r2++);
            writeRow(meta, header, List.of("Metric", "Value"));
            writeKeyValue(s2, r2++, text, money, "TotalBudget", cashFlow.getTotalBudget());
            writeKeyValue(s2, r2++, text, money, "ApprovedSpent", cashFlow.getApprovedSpent());
            writeKeyValue(s2, r2++, text, money, "CashOutsideWallet", cashFlow.getCashOutsideWallet());
            writeKeyValue(s2, r2++, text, money, "CashInsideWallet", cashFlow.getCashInsideWallet());
            r2++;

            Row invH = s2.createRow(r2++);
            writeRow(invH, header, List.of("InvoiceStatus", "Count", "TotalAmount"));
            for (InvoiceStatusSummaryDto inv : safeList(cashFlow.getInvoiceStatusSummary())) {
                Row row = s2.createRow(r2++);
                writeCell(row, 0, text, inv.getStatus() != null ? inv.getStatus().name() : null);
                writeCell(row, 1, text, inv.getCount());
                writeCell(row, 2, money, inv.getTotalAmount());
            }
            autosize(s2, 3);

            List<AuditLog> logs = collectPreparationAuditLogs(activityId);
            List<Long> faIds = logs.stream()
                    .filter(l -> "FundAdvance".equals(l.getEntityType()))
                    .map(AuditLog::getEntityId)
                    .filter(v -> v != null)
                    .distinct()
                    .toList();
            List<Long> exIds = logs.stream()
                    .filter(l -> "Expense".equals(l.getEntityType()))
                    .map(AuditLog::getEntityId)
                    .filter(v -> v != null)
                    .distinct()
                    .toList();
            Map<Long, FundAdvance> faById = fundAdvanceRepository.findAllById(faIds).stream()
                    .collect(Collectors.toMap(FundAdvance::getId, v -> v));
            Map<Long, Expense> exById = expenseRepository.findAllById(exIds).stream()
                    .collect(Collectors.toMap(Expense::getId, v -> v));

            Sheet sTx = wb.createSheet("CashTransactions");
            int rt = 0;
            Row th = sTx.createRow(rt++);
            writeRow(th, header, List.of("Time", "Type", "Direction", "Wallet", "Amount",
                    "TaskId", "StudentId", "StudentCode", "StudentName", "Actor", "EntityType", "EntityId"));
            for (AuditLog l : logs) {
                if (l == null || l.getAction() == null || l.getEntityType() == null || l.getEntityId() == null) {
                    continue;
                }
                String direction = null;
                BigDecimal amountVal = null;
                String wallet = null;
                Long taskId = null;
                Long studentId = null;
                String studentCode = null;
                String studentName = null;

                if ("FundAdvance".equals(l.getEntityType())
                        && ("ADMIN_DECISION_FUND_ADVANCE".equals(l.getAction())
                                || "RETURN_FUND_ADVANCE".equals(l.getAction()))) {
                    FundAdvance fa = faById.get(l.getEntityId());
                    if (fa == null) {
                        continue;
                    }
                    if ("ADMIN_DECISION_FUND_ADVANCE".equals(l.getAction())) {
                        if (l.getDetail() == null || !l.getDetail().contains("approved=true")) {
                            continue;
                        }
                        direction = "OUT";
                        amountVal = fa.getAmount();
                    } else {
                        direction = "IN";
                        amountVal = parseAmountFromDetail(l.getDetail());
                        if (amountVal == null) {
                            amountVal = fa.getAmount();
                        }
                    }
                    wallet = fa.getCategory() != null ? fa.getCategory().getName() : null;
                    taskId = fa.getTask() != null ? fa.getTask().getId() : null;
                    studentId = fa.getStudent() != null ? fa.getStudent().getId() : null;
                    studentCode = fa.getStudent() != null ? fa.getStudent().getStudentCode() : null;
                    studentName = fa.getStudent() != null ? fa.getStudent().getFullName() : null;
                } else if ("Expense".equals(l.getEntityType()) && "ADMIN_DECISION".equals(l.getAction())) {
                    if (l.getDetail() == null || !l.getDetail().contains("approved=true")) {
                        continue;
                    }
                    Expense ex = exById.get(l.getEntityId());
                    if (ex == null) {
                        continue;
                    }
                    direction = "SPEND";
                    amountVal = ex.getAmount();
                    wallet = ex.getCategory() != null ? ex.getCategory().getName() : null;
                    taskId = ex.getTask() != null ? ex.getTask().getId() : null;
                    studentId = ex.getCreatedBy() != null ? ex.getCreatedBy().getId() : null;
                    studentCode = ex.getCreatedBy() != null ? ex.getCreatedBy().getStudentCode() : null;
                    studentName = ex.getCreatedBy() != null ? ex.getCreatedBy().getFullName() : null;
                } else {
                    continue;
                }

                Row row = sTx.createRow(rt++);
                writeCell(row, 0, text, l.getCreatedAt());
                writeCell(row, 1, text, l.getAction());
                writeCell(row, 2, text, direction);
                writeCell(row, 3, text, wallet);
                writeCell(row, 4, money, amountVal);
                writeCell(row, 5, text, taskId);
                writeCell(row, 6, text, studentId);
                writeCell(row, 7, text, studentCode);
                writeCell(row, 8, text, studentName);
                writeCell(row, 9, text, l.getActor() != null ? l.getActor().getUsername() : null);
                writeCell(row, 10, text, l.getEntityType());
                writeCell(row, 11, text, l.getEntityId());
            }
            autosize(sTx, 12);

            Sheet s3 = wb.createSheet("FundAdvanceDebts");
            int r3 = 0;
            Row dh = s3.createRow(r3++);
            writeRow(dh, header, List.of("StudentId", "StudentCode", "StudentName", "HoldingAmount"));
            Map<Long, Student> studentById = studentRepository.findAllById(
                    safeList(debts).stream().map(FundAdvanceDebtDto::getStudentId).filter(v -> v != null).toList())
                    .stream().collect(Collectors.toMap(Student::getId, v -> v));
            for (FundAdvanceDebtDto d : safeList(debts)) {
                Student s = d.getStudentId() != null ? studentById.get(d.getStudentId()) : null;
                Row row = s3.createRow(r3++);
                writeCell(row, 0, text, d.getStudentId());
                writeCell(row, 1, text, s != null ? s.getStudentCode() : null);
                writeCell(row, 2, text, d.getStudentName());
                writeCell(row, 3, money, d.getHoldingAmount());
            }
            autosize(s3, 4);

            return toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export financial report", e);
        }
    }

    private byte[] buildOperationalWorkbook(Long activityId) {
        List<PreparationTask> tasks = preparationTaskRepository.findByActivityIdOrderByDeadlineAscIdAsc(activityId);
        List<ExpenseDto> expenses = financeService.listExpensesByActivity(activityId, null);

        List<PreparationTaskMember> members = preparationTaskMemberRepository
                .findByActivityIdWithTaskAndStudent(activityId);
        Map<Long, List<PreparationTaskMember>> membersByTaskId = members.stream()
                .filter(m -> m.getTask() != null && m.getTask().getId() != null)
                .collect(Collectors.groupingBy(m -> m.getTask().getId()));

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = headerStyle(wb);
            CellStyle money = moneyStyle(wb);
            CellStyle text = textStyle(wb);

            Sheet s1 = wb.createSheet("Tasks");
            int r = 0;
            Row h = s1.createRow(r++);
            writeRow(h, header, List.of("TaskId", "Title", "Owner", "Status", "Deadline", "IsFinancial",
                    "AllocatedAmount", "Leaders", "MembersCount"));
            for (PreparationTask t : safeList(tasks)) {
                List<PreparationTaskMember> ms = membersByTaskId.getOrDefault(t.getId(), List.of());
                String leaders = ms.stream()
                        .filter(m -> m.getRole() == PreparationTaskMemberRole.LEADER)
                        .map(m -> m.getStudent() != null ? m.getStudent().getFullName() : null)
                        .filter(v -> v != null && !v.isBlank())
                        .distinct()
                        .collect(Collectors.joining(", "));
                long memberCount = ms.stream().filter(m -> m.getStudent() != null).map(m -> m.getStudent().getId())
                        .distinct().count();
                Row row = s1.createRow(r++);
                writeCell(row, 0, text, t.getId());
                writeCell(row, 1, text, t.getTitle());
                writeCell(row, 2, text, t.getOwner() != null ? t.getOwner().getFullName() : null);
                writeCell(row, 3, text, t.getStatus() != null ? t.getStatus().name() : null);
                writeCell(row, 4, text, t.getDeadline());
                writeCell(row, 5, text, t.isFinancial());
                writeCell(row, 6, money, t.getAllocatedAmount());
                writeCell(row, 7, text, leaders);
                writeCell(row, 8, text, memberCount);
            }
            autosize(s1, 9);

            Sheet s2 = wb.createSheet("Workload");
            int r2 = 0;
            Row wh = s2.createRow(r2++);
            writeRow(wh, header, List.of("StudentId", "StudentCode", "StudentName", "TaskCount"));
            Map<Long, Long> countByStudentId = preparationTaskMemberRepository.countTasksByStudentInActivity(activityId)
                    .stream()
                    .collect(Collectors.toMap(
                            PreparationTaskMemberRepository.StudentTaskCountView::getStudentId,
                            PreparationTaskMemberRepository.StudentTaskCountView::getTaskCount));
            List<Student> organizers = activityOrganizerRepository.findByActivityId(activityId).stream()
                    .map(ao -> ao.getStudent())
                    .filter(s -> s != null && s.getId() != null)
                    .sorted(Comparator.comparing(Student::getStudentCode, Comparator.nullsLast(String::compareTo)))
                    .toList();
            for (Student s : organizers) {
                Row row = s2.createRow(r2++);
                writeCell(row, 0, text, s.getId());
                writeCell(row, 1, text, s.getStudentCode());
                writeCell(row, 2, text, s.getFullName());
                writeCell(row, 3, text, countByStudentId.getOrDefault(s.getId(), 0L));
            }
            autosize(s2, 4);

            Sheet s3 = wb.createSheet("ExpenseEvidence");
            int r3 = 0;
            Row eh = s3.createRow(r3++);
            writeRow(eh, header, List.of("ExpenseId", "TaskId", "Category", "Amount", "Status",
                    "CreatedBy", "CreatedAt", "EvidenceUrl", "Description"));
            Map<Long, Student> studentById = studentRepository.findAllById(
                    safeList(expenses).stream().map(ExpenseDto::getCreatedById).filter(v -> v != null).toList())
                    .stream().collect(Collectors.toMap(Student::getId, v -> v));
            for (ExpenseDto e : safeList(expenses)) {
                Student s = e.getCreatedById() != null ? studentById.get(e.getCreatedById()) : null;
                Row row = s3.createRow(r3++);
                writeCell(row, 0, text, e.getId());
                writeCell(row, 1, text, e.getTaskId());
                writeCell(row, 2, text, e.getCategoryName());
                writeCell(row, 3, money, e.getAmount());
                writeCell(row, 4, text, e.getStatus() != null ? e.getStatus().name() : null);
                writeCell(row, 5, text, s != null && s.getStudentCode() != null
                        ? s.getStudentCode() + " - " + e.getCreatedByName()
                        : e.getCreatedByName());
                writeCell(row, 6, text, e.getCreatedAt());
                writeCell(row, 7, text, e.getEvidenceUrl());
                writeCell(row, 8, text, e.getDescription());
            }
            autosize(s3, 9);

            return toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export operational report", e);
        }
    }

    private byte[] buildAuditWorkbook(Long activityId) {
        List<AuditLog> logs = collectPreparationAuditLogs(activityId);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = headerStyle(wb);
            CellStyle text = textStyle(wb);

            Sheet s1 = wb.createSheet("AuditLogs");
            int r = 0;
            Row h = s1.createRow(r++);
            writeRow(h, header, List.of("Time", "Actor", "Action", "EntityType", "EntityId", "Detail"));
            for (AuditLog l : safeList(logs)) {
                Row row = s1.createRow(r++);
                writeCell(row, 0, text, l.getCreatedAt());
                writeCell(row, 1, text, l.getActor() != null ? l.getActor().getUsername() : null);
                writeCell(row, 2, text, l.getAction());
                writeCell(row, 3, text, l.getEntityType());
                writeCell(row, 4, text, l.getEntityId());
                writeCell(row, 5, text, l.getDetail());
            }
            autosize(s1, 6);

            FinanceOverviewReportDto overview = financeService.getFinanceOverviewReport(activityId);
            Long reserveCategoryId = safeList(overview.getWallets()).stream()
                    .filter(w -> w.getName() != null && w.getName().equalsIgnoreCase("Khác"))
                    .map(BudgetCategoryDto::getId)
                    .findFirst()
                    .orElse(null);

            if (reserveCategoryId != null) {
                List<AuditLog> adjLogs = safeList(logs).stream()
                        .filter(l -> l != null
                                && "AllocationAdjustmentRequest".equals(l.getEntityType())
                                && "ADMIN_DECISION_ALLOCATION_ADJUSTMENT".equals(l.getAction())
                                && l.getDetail() != null
                                && l.getDetail().contains("approved=true"))
                        .toList();
                List<Long> adjIds = adjLogs.stream().map(AuditLog::getEntityId).distinct().toList();
                Map<Long, vn.campuslife.entity.AllocationAdjustmentRequest> reqById = allocationAdjustmentRequestRepository
                        .findAllById(adjIds).stream()
                        .collect(Collectors.toMap(vn.campuslife.entity.AllocationAdjustmentRequest::getId, v -> v));

                Sheet s2 = wb.createSheet("ReserveTransfers");
                int rr = 0;
                Row rh = s2.createRow(rr++);
                writeRow(rh, header, List.of("Time", "RequestId", "TaskId", "TaskTitle", "RequestedBy",
                        "RequestedAmount", "ReserveAmount", "Sources", "DecidedBy"));

                for (AuditLog l : adjLogs) {
                    vn.campuslife.entity.AllocationAdjustmentRequest req = reqById.get(l.getEntityId());
                    if (req == null || req.getTask() == null) {
                        continue;
                    }
                    String sources = extractSources(l.getDetail());
                    BigDecimal reserveAmount = extractReserveAmount(l.getDetail(), reserveCategoryId);
                    if (reserveAmount == null || reserveAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    Row row = s2.createRow(rr++);
                    writeCell(row, 0, text, l.getCreatedAt());
                    writeCell(row, 1, text, req.getId());
                    writeCell(row, 2, text, req.getTask().getId());
                    writeCell(row, 3, text, req.getTask().getTitle());
                    writeCell(row, 4, text, req.getRequestedBy() != null ? req.getRequestedBy().getFullName() : null);
                    writeCell(row, 5, text, req.getAmount());
                    writeCell(row, 6, text, reserveAmount);
                    writeCell(row, 7, text, sources);
                    writeCell(row, 8, text, req.getDecidedBy() != null ? req.getDecidedBy().getUsername() : null);
                }
                autosize(s2, 9);
            }

            return toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export audit report", e);
        }
    }

    private byte[] buildFinancialPdf(Long activityId) {
        FinanceOverviewReportDto overview = financeService.getFinanceOverviewReport(activityId);
        CashFlowReportDto cashFlow = financeService.getCashFlowReport(activityId);
        List<FundAdvanceDebtDto> debts = financeService.listFundAdvanceDebts(activityId, null);
        List<AuditLog> logs = collectPreparationAuditLogs(activityId);
        List<Long> studentIds = safeList(debts).stream()
                .map(FundAdvanceDebtDto::getStudentId)
                .filter(v -> v != null)
                .toList();
        Map<Long, Student> studentById = studentRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, v -> v));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 24, 24, 24, 36);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new FooterPageEvent("Financial Report • Activity " + activityId + " • "
                + TS_PRINT.format(LocalDateTime.now())));
        doc.open();

        Font title = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font normal = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font section = new Font(Font.HELVETICA, 12, Font.BOLD);
        doc.add(new Paragraph("Preparation - Financial Report (Activity " + activityId + ")", title));
        doc.add(new Paragraph("GeneratedAt: " + LocalDateTime.now(), normal));
        doc.add(new Paragraph(" ", normal));

        PdfPTable t1 = new PdfPTable(new float[] { 3, 2, 2, 2, 2, 2, 2, 2 });
        t1.setWidthPercentage(100);
        t1.setSpacingAfter(8);
        addPdfHeader(t1, List.of("Wallet", "Allocated", "AllocatedToTasks", "AvailableToAllocate",
                "CashOutside", "CashAvailable", "Used", "Remaining"));
        for (BudgetCategoryDto w : safeList(overview.getWallets())) {
            addPdfRow(t1, List.of(
                    w.getName(),
                    fmtMoney(w.getAllocatedAmount()),
                    fmtMoney(w.getAllocatedToTasksAmount()),
                    fmtMoney(w.getAvailableToAllocateAmount()),
                    fmtMoney(w.getCashOutsideAmount()),
                    fmtMoney(w.getCashAvailableAmount()),
                    fmtMoney(w.getUsedAmount()),
                    fmtMoney(w.getRemainingAmount())));
        }
        doc.add(new Paragraph("Budget vs Actual", section));
        doc.add(t1);
        doc.add(new Paragraph(" ", normal));

        doc.add(new Paragraph("Cash Flow Summary", section));
        PdfPTable t2 = new PdfPTable(new float[] { 3, 2 });
        t2.setWidthPercentage(60);
        t2.setSpacingAfter(8);
        addPdfHeader(t2, List.of("Metric", "Value"));
        addPdfRow(t2, List.of("TotalBudget", fmtMoney(cashFlow.getTotalBudget())));
        addPdfRow(t2, List.of("ApprovedSpent", fmtMoney(cashFlow.getApprovedSpent())));
        addPdfRow(t2, List.of("CashOutsideWallet", fmtMoney(cashFlow.getCashOutsideWallet())));
        addPdfRow(t2, List.of("CashInsideWallet", fmtMoney(cashFlow.getCashInsideWallet())));
        doc.add(t2);
        doc.add(new Paragraph(" ", normal));

        PdfPTable tInv = new PdfPTable(new float[] { 3, 2, 2 });
        tInv.setWidthPercentage(60);
        tInv.setSpacingAfter(8);
        addPdfHeader(tInv, List.of("InvoiceStatus", "Count", "TotalAmount"));
        for (InvoiceStatusSummaryDto inv : safeList(cashFlow.getInvoiceStatusSummary())) {
            addPdfRow(tInv, List.of(
                    inv.getStatus() != null ? inv.getStatus().name() : "-",
                    inv.getCount() != null ? String.valueOf(inv.getCount()) : "0",
                    fmtMoney(inv.getTotalAmount())));
        }
        doc.add(new Paragraph("Invoice Status Summary", section));
        doc.add(tInv);
        doc.add(new Paragraph(" ", normal));

        doc.add(new Paragraph("Cash Transactions", section));
        PdfPTable tTx = new PdfPTable(new float[] { 2, 2, 1, 2, 2, 1, 2, 2, 2 });
        tTx.setWidthPercentage(100);
        tTx.setSpacingAfter(8);
        addPdfHeader(tTx,
                List.of("Time", "Type", "Dir", "Wallet", "Amount", "TaskId", "StudentCode", "StudentName", "Actor"));
        List<AuditLog> sortedLogs = safeList(logs).stream()
                .sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                .toList();
        List<Long> faIds = sortedLogs.stream()
                .filter(l -> l != null && "FundAdvance".equals(l.getEntityType()))
                .map(AuditLog::getEntityId)
                .filter(v -> v != null)
                .distinct()
                .toList();
        List<Long> exIds = sortedLogs.stream()
                .filter(l -> l != null && "Expense".equals(l.getEntityType()))
                .map(AuditLog::getEntityId)
                .filter(v -> v != null)
                .distinct()
                .toList();
        Map<Long, FundAdvance> faById = fundAdvanceRepository.findAllById(faIds).stream()
                .collect(Collectors.toMap(FundAdvance::getId, v -> v));
        Map<Long, Expense> exById = expenseRepository.findAllById(exIds).stream()
                .collect(Collectors.toMap(Expense::getId, v -> v));

        for (AuditLog l : sortedLogs) {
            if (l == null || l.getAction() == null || l.getEntityType() == null || l.getEntityId() == null) {
                continue;
            }
            String direction;
            String wallet;
            String amount;
            String taskId;
            String studentCode;
            String studentName;
            String actor = l.getActor() != null ? l.getActor().getUsername() : "-";

            if ("FundAdvance".equals(l.getEntityType())
                    && ("ADMIN_DECISION_FUND_ADVANCE".equals(l.getAction())
                            || "RETURN_FUND_ADVANCE".equals(l.getAction()))) {
                FundAdvance fa = faById.get(l.getEntityId());
                if (fa == null) {
                    continue;
                }
                if ("ADMIN_DECISION_FUND_ADVANCE".equals(l.getAction())) {
                    if (l.getDetail() == null || !l.getDetail().contains("approved=true")) {
                        continue;
                    }
                    direction = "OUT";
                    amount = fmtMoney(fa.getAmount());
                } else {
                    direction = "IN";
                    BigDecimal a = parseAmountFromDetail(l.getDetail());
                    amount = fmtMoney(a != null ? a : fa.getAmount());
                }
                wallet = fa.getCategory() != null ? fa.getCategory().getName() : "-";
                taskId = fa.getTask() != null && fa.getTask().getId() != null ? String.valueOf(fa.getTask().getId())
                        : "-";
                studentCode = fa.getStudent() != null ? nullToDash(fa.getStudent().getStudentCode()) : "-";
                studentName = fa.getStudent() != null ? nullToDash(fa.getStudent().getFullName()) : "-";
            } else if ("Expense".equals(l.getEntityType()) && "ADMIN_DECISION".equals(l.getAction())) {
                if (l.getDetail() == null || !l.getDetail().contains("approved=true")) {
                    continue;
                }
                Expense ex = exById.get(l.getEntityId());
                if (ex == null) {
                    continue;
                }
                direction = "SPEND";
                amount = fmtMoney(ex.getAmount());
                wallet = ex.getCategory() != null ? ex.getCategory().getName() : "-";
                taskId = ex.getTask() != null && ex.getTask().getId() != null ? String.valueOf(ex.getTask().getId())
                        : "-";
                studentCode = ex.getCreatedBy() != null ? nullToDash(ex.getCreatedBy().getStudentCode()) : "-";
                studentName = ex.getCreatedBy() != null ? nullToDash(ex.getCreatedBy().getFullName()) : "-";
            } else {
                continue;
            }
            addPdfRow(tTx, List.of(
                    l.getCreatedAt() != null ? String.valueOf(l.getCreatedAt()) : "-",
                    l.getAction(),
                    direction,
                    wallet,
                    amount,
                    taskId,
                    studentCode,
                    studentName,
                    actor));
        }
        doc.add(tTx);
        doc.add(new Paragraph(" ", normal));

        doc.add(new Paragraph("Fund Advance Debts", section));
        PdfPTable t3 = new PdfPTable(new float[] { 2, 2, 4, 2 });
        t3.setWidthPercentage(90);
        t3.setSpacingAfter(8);
        addPdfHeader(t3, List.of("StudentId", "StudentCode", "StudentName", "HoldingAmount"));
        for (FundAdvanceDebtDto d : safeList(debts)) {
            Student s = d.getStudentId() != null ? studentById.get(d.getStudentId()) : null;
            addPdfRow(t3, List.of(String.valueOf(d.getStudentId()),
                    s != null ? nullToDash(s.getStudentCode()) : "-",
                    nullToDash(d.getStudentName()),
                    fmtMoney(d.getHoldingAmount())));
        }
        doc.add(t3);

        doc.close();
        return out.toByteArray();
    }

    private byte[] buildOperationalPdf(Long activityId) {
        List<PreparationTask> tasks = preparationTaskRepository.findByActivityIdOrderByDeadlineAscIdAsc(activityId);
        List<PreparationTaskMember> members = preparationTaskMemberRepository
                .findByActivityIdWithTaskAndStudent(activityId);
        Map<Long, List<PreparationTaskMember>> membersByTaskId = members.stream()
                .filter(m -> m.getTask() != null && m.getTask().getId() != null)
                .collect(Collectors.groupingBy(m -> m.getTask().getId()));
        List<ExpenseDto> expenses = financeService.listExpensesByActivity(activityId, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 24, 24, 24, 36);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new FooterPageEvent("Operational Report • Activity " + activityId + " • "
                + TS_PRINT.format(LocalDateTime.now())));
        doc.open();

        Font title = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font normal = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font section = new Font(Font.HELVETICA, 12, Font.BOLD);
        doc.add(new Paragraph("Preparation - Operational Report (Activity " + activityId + ")", title));
        doc.add(new Paragraph("GeneratedAt: " + LocalDateTime.now(), normal));
        doc.add(new Paragraph(" ", normal));

        PdfPTable t1 = new PdfPTable(new float[] { 1, 4, 2, 2, 2, 2, 3 });
        t1.setWidthPercentage(100);
        t1.setSpacingAfter(8);
        addPdfHeader(t1, List.of("Id", "Title", "Owner", "Status", "Deadline", "Allocated", "Leaders"));
        for (PreparationTask t : safeList(tasks)) {
            List<PreparationTaskMember> ms = membersByTaskId.getOrDefault(t.getId(), List.of());
            String leaders = ms.stream()
                    .filter(m -> m.getRole() == PreparationTaskMemberRole.LEADER)
                    .map(m -> m.getStudent() != null ? m.getStudent().getFullName() : null)
                    .filter(v -> v != null && !v.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));
            addPdfRow(t1, List.of(
                    String.valueOf(t.getId()),
                    nullToDash(t.getTitle()),
                    t.getOwner() != null ? t.getOwner().getFullName() : "-",
                    t.getStatus() != null ? t.getStatus().name() : "-",
                    t.getDeadline() != null ? String.valueOf(t.getDeadline()) : "-",
                    fmtMoney(t.getAllocatedAmount()),
                    leaders.isBlank() ? "-" : leaders));
        }
        doc.add(new Paragraph("Task List", section));
        doc.add(t1);
        doc.add(new Paragraph(" ", normal));

        Map<Long, Long> countByStudentId = preparationTaskMemberRepository.countTasksByStudentInActivity(activityId)
                .stream()
                .collect(Collectors.toMap(
                        PreparationTaskMemberRepository.StudentTaskCountView::getStudentId,
                        PreparationTaskMemberRepository.StudentTaskCountView::getTaskCount));
        List<Student> organizers = activityOrganizerRepository.findByActivityId(activityId).stream()
                .map(ao -> ao.getStudent())
                .filter(s -> s != null && s.getId() != null)
                .sorted(Comparator.comparing(Student::getStudentCode, Comparator.nullsLast(String::compareTo)))
                .toList();

        PdfPTable t2 = new PdfPTable(new float[] { 2, 2, 4, 2 });
        t2.setWidthPercentage(90);
        t2.setSpacingAfter(8);
        addPdfHeader(t2, List.of("StudentId", "StudentCode", "StudentName", "TaskCount"));
        for (Student s : organizers) {
            addPdfRow(t2, List.of(
                    String.valueOf(s.getId()),
                    nullToDash(s.getStudentCode()),
                    nullToDash(s.getFullName()),
                    String.valueOf(countByStudentId.getOrDefault(s.getId(), 0L))));
        }
        doc.add(new Paragraph("Workload Report", section));
        doc.add(t2);
        doc.add(new Paragraph(" ", normal));

        PdfPTable t3 = new PdfPTable(new float[] { 1, 1, 2, 2, 2, 2, 3 });
        t3.setWidthPercentage(100);
        t3.setSpacingAfter(8);
        addPdfHeader(t3, List.of("ExpenseId", "TaskId", "Wallet", "Amount", "Status", "CreatedAt", "EvidenceUrl"));
        for (ExpenseDto e : safeList(expenses)) {
            addPdfRowWithLink(t3, List.of(
                    e.getId() != null ? String.valueOf(e.getId()) : "-",
                    e.getTaskId() != null ? String.valueOf(e.getTaskId()) : "-",
                    nullToDash(e.getCategoryName()),
                    fmtMoney(e.getAmount()),
                    e.getStatus() != null ? e.getStatus().name() : "-",
                    e.getCreatedAt() != null ? String.valueOf(e.getCreatedAt()) : "-",
                    nullToDash(e.getEvidenceUrl())), 6);
        }
        doc.add(new Paragraph("Expense Evidence", section));
        doc.add(t3);
        doc.close();
        return out.toByteArray();
    }

    private byte[] buildAuditPdf(Long activityId) {
        List<AuditLog> logs = collectPreparationAuditLogs(activityId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 24, 24, 24, 36);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new FooterPageEvent("Audit Report • Activity " + activityId + " • "
                + TS_PRINT.format(LocalDateTime.now())));
        doc.open();
        Font title = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font normal = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font section = new Font(Font.HELVETICA, 12, Font.BOLD);
        doc.add(new Paragraph("Preparation - Audit Report (Activity " + activityId + ")", title));
        doc.add(new Paragraph("GeneratedAt: " + LocalDateTime.now(), normal));
        doc.add(new Paragraph(" ", normal));

        PdfPTable t1 = new PdfPTable(new float[] { 2, 2, 3, 2, 1, 5 });
        t1.setWidthPercentage(100);
        t1.setSpacingAfter(8);
        addPdfHeader(t1, List.of("Time", "Actor", "Action", "EntityType", "Id", "Detail"));
        for (AuditLog l : safeList(logs)) {
            addPdfRow(t1, List.of(
                    l.getCreatedAt() != null ? String.valueOf(l.getCreatedAt()) : "-",
                    l.getActor() != null ? l.getActor().getUsername() : "-",
                    nullToDash(l.getAction()),
                    nullToDash(l.getEntityType()),
                    l.getEntityId() != null ? String.valueOf(l.getEntityId()) : "-",
                    nullToDash(l.getDetail())));
        }
        doc.add(new Paragraph("Audit Logs", section));
        doc.add(t1);

        FinanceOverviewReportDto overview = financeService.getFinanceOverviewReport(activityId);
        Long reserveCategoryId = safeList(overview.getWallets()).stream()
                .filter(w -> w.getName() != null && w.getName().equalsIgnoreCase("Khác"))
                .map(BudgetCategoryDto::getId)
                .findFirst()
                .orElse(null);
        if (reserveCategoryId != null) {
            List<AuditLog> adjLogs = safeList(logs).stream()
                    .filter(l -> l != null
                            && "AllocationAdjustmentRequest".equals(l.getEntityType())
                            && "ADMIN_DECISION_ALLOCATION_ADJUSTMENT".equals(l.getAction())
                            && l.getDetail() != null
                            && l.getDetail().contains("approved=true"))
                    .toList();
            List<Long> adjIds = adjLogs.stream().map(AuditLog::getEntityId).distinct().toList();
            Map<Long, vn.campuslife.entity.AllocationAdjustmentRequest> reqById = allocationAdjustmentRequestRepository
                    .findAllById(adjIds).stream()
                    .collect(Collectors.toMap(vn.campuslife.entity.AllocationAdjustmentRequest::getId, v -> v));
            PdfPTable t2 = new PdfPTable(new float[] { 2, 1, 1, 3, 2, 2, 2, 4 });
            t2.setWidthPercentage(100);
            t2.setSpacingAfter(8);
            addPdfHeader(t2, List.of("Time", "ReqId", "TaskId", "TaskTitle", "RequestedAmount", "ReserveAmount",
                    "DecidedBy", "Sources"));
            for (AuditLog l : adjLogs) {
                vn.campuslife.entity.AllocationAdjustmentRequest req = reqById.get(l.getEntityId());
                if (req == null || req.getTask() == null) {
                    continue;
                }
                BigDecimal reserveAmount = extractReserveAmount(l.getDetail(), reserveCategoryId);
                if (reserveAmount == null || reserveAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                addPdfRow(t2, List.of(
                        l.getCreatedAt() != null ? String.valueOf(l.getCreatedAt()) : "-",
                        String.valueOf(req.getId()),
                        String.valueOf(req.getTask().getId()),
                        nullToDash(req.getTask().getTitle()),
                        fmtMoney(req.getAmount()),
                        fmtMoney(reserveAmount),
                        req.getDecidedBy() != null ? req.getDecidedBy().getUsername() : "-",
                        nullToDash(extractSources(l.getDetail()))));
            }
            doc.add(new Paragraph("Reserve Transfers (Wallet 'Khác')", section));
            doc.add(t2);
        }
        doc.close();
        return out.toByteArray();
    }

    private List<AuditLog> collectPreparationAuditLogs(Long activityId) {
        List<Long> taskIds = preparationTaskRepository.findByActivityIdOrderByDeadlineAscIdAsc(activityId).stream()
                .map(PreparationTask::getId)
                .filter(v -> v != null)
                .toList();
        if (taskIds.isEmpty()) {
            return List.of();
        }
        List<Long> expenseIds = expenseRepository.findByTaskActivityIdOrderByCreatedAtDesc(activityId).stream()
                .map(v -> v.getId())
                .filter(v -> v != null)
                .toList();
        List<Long> fundAdvanceIds = fundAdvanceRepository.findByTaskActivityIdOrderByCreatedAtDesc(activityId).stream()
                .map(v -> v.getId())
                .filter(v -> v != null)
                .toList();
        List<Long> allocationAdjIds = allocationAdjustmentRequestRepository
                .findByTaskActivityIdOrderByCreatedAtDesc(activityId)
                .stream()
                .map(v -> v.getId())
                .filter(v -> v != null)
                .toList();

        List<AuditLog> all = new ArrayList<>();
        all.addAll(fetchLogs("PreparationTask", taskIds));
        all.addAll(fetchLogs("Expense", expenseIds));
        all.addAll(fetchLogs("FundAdvance", fundAdvanceIds));
        all.addAll(fetchLogs("AllocationAdjustmentRequest", allocationAdjIds));
        return all.stream()
                .sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                        .reversed())
                .toList();
    }

    private List<AuditLog> fetchLogs(String entityType, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return auditLogRepository.findByEntityTypeAndEntityIdInOrderByCreatedAtDesc(entityType, ids);
    }

    private Activity getActiveActivity(Long activityId) {
        return activityRepository.findByIdAndIsDeletedFalse(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
    }

    private void requirePreparationEnabled(Activity activity) {
        if (activity == null || !activity.isHasPreparation()) {
            throw new FeatureNotEnabledException("Preparation feature is not enabled for this activity");
        }
    }

    private static boolean isPdf(String format) {
        return format != null && format.equalsIgnoreCase("pdf");
    }

    private static String fileName(String prefix, Long activityId, String ext) {
        return prefix + "_activity_" + activityId + "_" + TS.format(LocalDateTime.now()) + "." + ext;
    }

    private static <T> List<T> safeList(List<T> v) {
        return v == null ? List.of() : v;
    }

    private static byte[] toBytes(XSSFWorkbook wb) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private static void autosize(Sheet s, int cols) {
        for (int i = 0; i < cols; i++) {
            s.autoSizeColumn(i);
        }
    }

    private static void writeKeyValue(Sheet s, int rowIndex, CellStyle text, CellStyle money, String k, BigDecimal v) {
        Row row = s.createRow(rowIndex);
        writeCell(row, 0, text, k);
        writeCell(row, 1, money, v);
    }

    private static void writeRow(Row row, CellStyle style, List<Object> values) {
        for (int i = 0; i < values.size(); i++) {
            writeCell(row, i, style, values.get(i));
        }
    }

    private static void writeCell(Row row, int col, CellStyle style, Object value) {
        Cell cell = row.createCell(col);
        if (style != null) {
            cell.setCellStyle(style);
        }
        if (value == null) {
            return;
        }
        if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
            return;
        }
        if (value instanceof Boolean b) {
            cell.setCellValue(b);
            return;
        }
        if (value instanceof LocalDateTime t) {
            cell.setCellValue(String.valueOf(t));
            return;
        }
        cell.setCellValue(String.valueOf(value));
    }

    private static CellStyle headerStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        f.setBold(true);
        style.setFont(f);
        return style;
    }

    private static CellStyle textStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        return style;
    }

    private static CellStyle moneyStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }

    private static void addPdfHeader(PdfPTable table, List<String> headers) {
        Font font = new Font(Font.HELVETICA, 10, Font.BOLD);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
        table.setHeaderRows(1);
        table.setSplitLate(false);
    }

    private static void addPdfRow(PdfPTable table, List<String> values) {
        Font font = new Font(Font.HELVETICA, 9, Font.NORMAL);
        for (String v : values) {
            PdfPCell cell = new PdfPCell(new Phrase(nullToDash(v), font));
            cell.setVerticalAlignment(Element.ALIGN_TOP);
            table.addCell(cell);
        }
    }

    private static void addPdfRowWithLink(PdfPTable table, List<String> values, int linkIndex) {
        Font font = new Font(Font.HELVETICA, 9, Font.NORMAL);
        Font linkFont = new Font(Font.HELVETICA, 9, Font.UNDERLINE);
        for (int i = 0; i < values.size(); i++) {
            String v = values.get(i);
            PdfPCell cell;
            if (i == linkIndex && v != null && !v.isBlank() && !"-".equals(v)) {
                Anchor a = new Anchor(v, linkFont);
                a.setReference(v);
                cell = new PdfPCell(a);
            } else {
                cell = new PdfPCell(new Phrase(nullToDash(v), font));
            }
            cell.setVerticalAlignment(Element.ALIGN_TOP);
            table.addCell(cell);
        }
    }

    private static String fmtMoney(BigDecimal v) {
        if (v == null) {
            return "-";
        }
        return MONEY.format(v);
    }

    private static BigDecimal parseAmountFromDetail(String detail) {
        if (detail == null) {
            return null;
        }
        int idx = detail.indexOf("amount=");
        if (idx < 0) {
            return null;
        }
        String s = detail.substring(idx + "amount=".length());
        int end = s.indexOf(',');
        if (end >= 0) {
            s = s.substring(0, end);
        }
        s = s.trim();
        if (s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractSources(String detail) {
        if (detail == null) {
            return null;
        }
        int idx = detail.indexOf("sources=");
        if (idx < 0) {
            return null;
        }
        String s = detail.substring(idx + "sources=".length());
        int end = s.indexOf(",amount=");
        if (end >= 0) {
            s = s.substring(0, end);
        }
        return s.trim();
    }

    private static BigDecimal extractReserveAmount(String detail, Long reserveCategoryId) {
        if (detail == null || reserveCategoryId == null) {
            return null;
        }
        String sources = extractSources(detail);
        if (sources != null && !sources.isBlank()) {
            String[] parts = sources.split("\\|");
            for (String p : parts) {
                String[] kv = p.split(":");
                if (kv.length != 2) {
                    continue;
                }
                try {
                    Long id = Long.valueOf(kv[0].trim());
                    if (!reserveCategoryId.equals(id)) {
                        continue;
                    }
                    return new BigDecimal(kv[1].trim());
                } catch (Exception e) {
                    continue;
                }
            }
            return BigDecimal.ZERO;
        }
        int cidx = detail.indexOf("categoryId=");
        if (cidx >= 0) {
            String s = detail.substring(cidx + "categoryId=".length());
            int end = s.indexOf(',');
            if (end >= 0) {
                s = s.substring(0, end);
            }
            try {
                Long id = Long.valueOf(s.trim());
                if (!reserveCategoryId.equals(id)) {
                    return BigDecimal.ZERO;
                }
                BigDecimal amount = parseAmountFromDetail(detail);
                return amount != null ? amount : BigDecimal.ZERO;
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        }
        return null;
    }

    private static String nullToDash(String v) {
        return v == null || v.isBlank() ? "-" : v;
    }
}
