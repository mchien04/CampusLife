# Sequence Diagram - Preparation Export Module (Xuất báo cáo chuẩn bị)

Hệ thống: **CampusLife** (Spring Boot + React)

---

## Tóm tắt các Sequence Diagram

| STT | Chức năng | Mã yêu cầu | Ghi chú |
|-----|-----------|-----------|---------|
| 1 | Xuất báo cáo tài chính | J.41 | Financial report (XLSX/PDF) với Budget, CashFlow, Transactions, Debts |
| 2 | Xuất báo cáo hoạt động & Audit | J.42 | Operational + Audit report, đồng thời ghi AuditLog |

---

## 1. Xuất báo cáo tài chính (J.41)

> **Endpoint:** `GET /api/preparation/activities/{activityId}/exports/financial?format=xlsx|pdf`  
> **Đối tượng:** Admin / Manager / Organizer / Supervisor  
> **PreAuthorize:** `hasAnyRole('ADMIN','MANAGER')` hoặc `@preparationSecurity.isActivityPrepSupervisor(...)` hoặc `@preparationSecurity.isOrganizer(...)`

```mermaid
sequenceDiagram
    autonumber
    participant Admin as Admin/Manager
    participant Client as Client (React)
    participant EC as Controller (PreparationExportController)
    participant ES as Service (PreparationExportService)
    participant Repo as Repository (JPA Repositories)
    participant DB as Database
    participant FG as FileGenerator (Excel/PDF)

    Note over Admin, FG: === LUỒNG XUẤT BÁO CÁO TÀI CHÍNH (J.41) ===

    Admin->>Client: Chọn format (xlsx / pdf) và bấm "Xuất báo cáo tài chính"
    Client->>EC: GET /api/preparation/activities/{id}/exports/financial?format={format}
    EC->>ES: exportFinancial(activityId, format)

    ES->>Repo: activityRepository.findByIdAndIsDeletedFalse(activityId)
    Repo->>DB: SELECT * FROM activities WHERE id = ? AND is_deleted = false
    DB-->>Repo: Activity record
    Repo-->>ES: Optional<Activity>

    alt Activity không tồn tại
        ES-->>EC: throw ResourceNotFoundException
        EC-->>Client: 404 Not Found<br/>{"error": "Activity not found"}
        Client-->>Admin: Hiển thị lỗi: Không tìm thấy hoạt động
    else Activity tồn tại
        ES->>ES: requirePreparationEnabled(activity)<br/>Kiểm tra activity.isHasPreparation() == true
        alt Preparation chưa bật
            ES-->>EC: throw FeatureNotEnabledException
            EC-->>Client: 400 Bad Request<br/>{"error": "Preparation feature is not enabled"}
            Client-->>Admin: Hiển thị lỗi: Chưa bật tính năng chuẩn bị
        else Preparation đã bật
            alt format == pdf
                ES->>ES: buildFinancialPdf(activityId)
            else format == xlsx (hoặc mặc định)
                ES->>ES: buildFinancialWorkbook(activityId)
            end

            Note over ES, DB: LẤY DỮ LIỆU TỔNG QUAN TÀI CHÍNH
            ES->>ES: financeService.getFinanceOverviewReport(activityId)
            ES->>Repo: (qua PreparationFinanceService) Truy vấn budgets, wallets, allocations
            Repo->>DB: SELECT các bảng preparation_budgets, budget_categories, allocations...
            DB-->>Repo: FinanceOverview data
            Repo-->>ES: FinanceOverviewReportDto

            ES->>ES: financeService.getCashFlowReport(activityId)
            ES->>Repo: (qua PreparationFinanceService) Truy vấn cash flow, invoice status
            Repo->>DB: SELECT expenses, fund_advances GROUP BY status
            DB-->>Repo: CashFlow data
            Repo-->>ES: CashFlowReportDto

            ES->>ES: financeService.listFundAdvanceDebts(activityId, null)
            ES->>Repo: (qua PreparationFinanceService) Truy vấn nợ tạm ứng
            Repo->>DB: SELECT fund_advances, students WHERE holding_amount > 0
            DB-->>Repo: List<FundAdvanceDebtDto>
            Repo-->>ES: List<FundAdvanceDebtDto>

            Note over ES, DB: LẤY AUDIT LOG CHO GIAO DỊCH TIỀN MẶT
            ES->>Repo: preparationTaskRepository.findByActivityIdOrderByDeadlineAscIdAsc(activityId)
            Repo->>DB: SELECT * FROM preparation_tasks WHERE activity_id = ?
            DB-->>Repo: List<PreparationTask>
            Repo-->>ES: List<taskIds>

            ES->>Repo: expenseRepository.findByTaskActivityIdOrderByCreatedAtDesc(activityId)
            Repo->>DB: SELECT * FROM expenses WHERE activity_id = ?
            DB-->>Repo: List<Expense>
            Repo-->>ES: List<expenseIds>

            ES->>Repo: fundAdvanceRepository.findByTaskActivityIdOrderByCreatedAtDesc(activityId)
            Repo->>DB: SELECT * FROM fund_advances WHERE activity_id = ?
            DB-->>Repo: List<FundAdvance>
            Repo-->>ES: List<fundAdvanceIds>

            ES->>Repo: allocationAdjustmentRequestRepository.findByTaskActivityIdOrderByCreatedAtDesc(activityId)
            Repo->>DB: SELECT * FROM allocation_adjustment_requests WHERE activity_id = ?
            DB-->>Repo: List<AllocationAdjustmentRequest>
            Repo-->>ES: List<allocationAdjIds>

            loop Với mỗi entityType (PreparationTask, Expense, FundAdvance, AllocationAdjustmentRequest)
                ES->>Repo: auditLogRepository.findByEntityTypeAndEntityIdInOrderByCreatedAtDesc(type, ids)
                Repo->>DB: SELECT * FROM audit_logs WHERE entity_type = ? AND entity_id IN (...)
                DB-->>Repo: List<AuditLog>
                Repo-->>ES: List<AuditLog>
            end
            ES->>ES: collectPreparationAuditLogs()<br/>Merge & sort by createdAt DESC

            ES->>Repo: fundAdvanceRepository.findAllById(faIds)
            Repo->>DB: SELECT * FROM fund_advances WHERE id IN (...)
            DB-->>Repo: Map<Long, FundAdvance>
            Repo-->>ES: Map<Long, FundAdvance>

            ES->>Repo: expenseRepository.findAllById(exIds)
            Repo->>DB: SELECT * FROM expenses WHERE id IN (...)
            DB-->>Repo: Map<Long, Expense>
            Repo-->>ES: Map<Long, Expense>

            ES->>Repo: studentRepository.findAllById(studentIds)
            Repo->>DB: SELECT * FROM students WHERE id IN (...)
            DB-->>Repo: List<Student>
            Repo-->>ES: Map<Long, Student>

            Note over ES, FG: TẠO FILE BÁO CÁO TÀI CHÍNH
            alt format == xlsx
                ES->>FG: Tạo XSSFWorkbook với các sheet:<br/>BudgetVsActual, CashFlow, CashTransactions, FundAdvanceDebts
                FG-->>ES: byte[] (workbook bytes)
            else format == pdf
                ES->>FG: Tạo Document (OpenPDF / iText) với các section:<br/>Budget vs Actual, Cash Flow Summary, Invoice Status, Cash Transactions, Fund Advance Debts
                FG-->>ES: byte[] (pdf bytes)
            end

            ES->>ES: Tạo ExportFile(filename, contentType, bytes)<br/>filename = preparation_financial_activity_{id}_{timestamp}.{ext}
            ES-->>EC: ExportFile

            EC->>EC: fileResponse(file)<br/>Set Content-Disposition: attachment; filename="..."<br/>Set Content-Type: application/pdf hoặc application/vnd.openxmlformats...
            EC-->>Client: 200 OK<br/>Body: byte[] (file download)
            Client->>Client: Tự động tải file xuống máy / Hiển thị preview
            Client-->>Admin: Thông báo "Xuất báo cáo tài chính thành công"
        end
    end
```

---

## 2. Xuất báo cáo hoạt động & Audit (J.42)

> **Endpoint Operational:** `GET /api/preparation/activities/{activityId}/exports/operational?format=xlsx|pdf`  
> **Endpoint Audit:** `GET /api/preparation/activities/{activityId}/exports/audit?format=xlsx|pdf`  
> **Đối tượng:** Admin / Manager / Organizer / Supervisor  
> **PreAuthorize:** tương tự J.41

```mermaid
sequenceDiagram
    autonumber
    participant Admin as Admin/Manager
    participant Client as Client (React)
    participant EC as Controller (PreparationExportController)
    participant ES as Service (PreparationExportService)
    participant Repo as Repository (JPA Repositories)
    participant DB as Database
    participant FG as FileGenerator (Excel/PDF)

    Note over Admin, FG: === LUỒNG XUẤT BÁO CÁO HOẠT ĐỘNG (Operational - J.42) ===

    Admin->>Client: Chọn format (xlsx / pdf) và bấm "Xuất báo cáo hoạt động"
    Client->>EC: GET /api/preparation/activities/{id}/exports/operational?format={format}
    EC->>ES: exportOperational(activityId, format)

    ES->>Repo: activityRepository.findByIdAndIsDeletedFalse(activityId)
    Repo->>DB: SELECT * FROM activities WHERE id = ? AND is_deleted = false
    DB-->>Repo: Activity record
    Repo-->>ES: Optional<Activity>

    alt Activity không tồn tại hoặc chưa bật Preparation
        ES-->>EC: throw ResourceNotFoundException / FeatureNotEnabledException
        EC-->>Client: 404 / 400 Bad Request
        Client-->>Admin: Hiển thị lỗi tương ứng
    else Activity hợp lệ
        alt format == pdf
            ES->>ES: buildOperationalPdf(activityId)
        else format == xlsx
            ES->>ES: buildOperationalWorkbook(activityId)
        end

        Note over ES, DB: LẤY DỮ LIỆU HOẠT ĐỘNG & NHÂN SỰ
        ES->>Repo: preparationTaskRepository.findByActivityIdOrderByDeadlineAscIdAsc(activityId)
        Repo->>DB: SELECT * FROM preparation_tasks WHERE activity_id = ? ORDER BY deadline ASC, id ASC
        DB-->>Repo: List<PreparationTask>
        Repo-->>ES: List<PreparationTask>

        ES->>Repo: activityOrganizerRepository.findByActivityId(activityId)
        Repo->>DB: SELECT * FROM activity_organizers WHERE activity_id = ?<br/>JOIN students
        DB-->>Repo: List<ActivityOrganizer>
        Repo-->>ES: List<Student> (organizers)

        ES->>Repo: preparationTaskMemberRepository.findByActivityIdWithTaskAndStudent(activityId)
        Repo->>DB: SELECT ptm.*, pt.*, s.* FROM preparation_task_members ...<br/>WHERE pt.activity_id = ?
        DB-->>Repo: List<PreparationTaskMember>
        Repo-->>ES: List<PreparationTaskMember> (assignments + role)

        ES->>Repo: preparationTaskMemberRepository.countTasksByStudentInActivity(activityId)
        Repo->>DB: SELECT student_id, COUNT(DISTINCT task_id) FROM preparation_task_members ...<br/>GROUP BY student_id
        DB-->>Repo: List<StudentTaskCountView>
        Repo-->>ES: Map<studentId, taskCount> (progress)

        ES->>Repo: activityRegistrationRepository.findByActivityId(activityId)
        Repo->>DB: SELECT * FROM activity_registrations WHERE activity_id = ?<br/>JOIN students
        DB-->>Repo: List<ActivityRegistration>
        Repo-->>ES: List<Registration> (danh sách đăng ký)

        ES->>Repo: activityParticipationRepository.findByActivityId(activityId)
        Repo->>DB: SELECT * FROM activity_participations WHERE activity_id = ?<br/>(check-in / check-out records)
        DB-->>Repo: List<ActivityParticipation>
        Repo-->>ES: List<Participation> (check-in / check-out)

        ES->>ES: financeService.listExpensesByActivity(activityId, null)
        ES->>Repo: (qua PreparationFinanceService) Truy vấn expenses
        Repo->>DB: SELECT * FROM expenses WHERE activity_id = ?
        DB-->>Repo: List<ExpenseDto>
        Repo-->>ES: List<ExpenseDto> (expense evidence)

        ES->>Repo: studentRepository.findAllById(organizerIds)
        Repo->>DB: SELECT * FROM students WHERE id IN (...)
        DB-->>Repo: List<Student>
        Repo-->>ES: Map<Long, Student>

        Note over ES, FG: TẠO FILE BÁO CÁO HOẠT ĐỘNG
        alt format == xlsx
            ES->>FG: Tạo XSSFWorkbook với các sheet:<br/>Tasks, Workload, ExpenseEvidence
            FG-->>ES: byte[] (workbook bytes)
        else format == pdf
            ES->>FG: Tạo Document (OpenPDF / iText) với các section:<br/>Task List, Workload Report, Expense Evidence
            FG-->>ES: byte[] (pdf bytes)
        end

        ES->>ES: Tạo ExportFile(filename, contentType, bytes)<br/>filename = preparation_operational_activity_{id}_{timestamp}.{ext}
        ES-->>EC: ExportFile

        par GHI AUDIT LOG (ĐỒNG THỜI)
            ES->>Repo: auditLogRepository.save(AuditLog)<br/>who={actor}, when={now}, what="OPERATIONAL_EXPORT",<br/>entityId={activityId}, detail={format}
            Repo->>DB: INSERT INTO audit_logs (actor_id, action, entity_type, entity_id, detail, created_at) VALUES (...)
            DB-->>Repo: AuditLog record
            Repo-->>ES: AuditLog
        and TRẢ VỀ FILE
            EC->>EC: fileResponse(file)<br/>Set Content-Disposition: attachment; filename="..."
            EC-->>Client: 200 OK<br/>Body: byte[] (file download)
        end

        Client->>Client: Tự động tải file / Preview
        Client-->>Admin: Thông báo "Xuất báo cáo hoạt động thành công"
    end

    Note over Admin, FG: === LUỒNG XUẤT BÁO CÁO AUDIT (Audit Report - J.42) ===

    Admin->>Client: Chọn format và bấm "Xuất báo cáo audit"
    Client->>EC: GET /api/preparation/activities/{id}/exports/audit?format={format}
    EC->>ES: exportAudit(activityId, format)

    ES->>Repo: activityRepository.findByIdAndIsDeletedFalse(activityId)
    Repo->>DB: SELECT * FROM activities WHERE id = ? AND is_deleted = false
    DB-->>Repo: Activity record
    Repo-->>ES: Optional<Activity>

    alt Activity không tồn tại hoặc chưa bật Preparation
        ES-->>EC: throw ResourceNotFoundException / FeatureNotEnabledException
        EC-->>Client: 404 / 400 Bad Request
        Client-->>Admin: Hiển thị lỗi tương ứng
    else Activity hợp lệ
        alt format == pdf
            ES->>ES: buildAuditPdf(activityId)
        else format == xlsx
            ES->>ES: buildAuditWorkbook(activityId)
        end

        Note over ES, DB: LẤY DỮ LIỆU AUDIT LOG
        ES->>Repo: collectPreparationAuditLogs(activityId)<br/>Gọi: preparationTaskRepository, expenseRepository,<br/>fundAdvanceRepository, allocationAdjustmentRequestRepository
        Repo->>DB: SELECT * FROM preparation_tasks / expenses / fund_advances / allocation_adjustments WHERE activity_id = ?
        DB-->>Repo: List entity IDs
        Repo-->>ES: List<taskIds>, List<expenseIds>, List<fundAdvanceIds>, List<allocationAdjIds>

        loop Với mỗi entityType (PreparationTask, Expense, FundAdvance, AllocationAdjustmentRequest)
            ES->>Repo: auditLogRepository.findByEntityTypeAndEntityIdInOrderByCreatedAtDesc(type, ids)
            Repo->>DB: SELECT * FROM audit_logs WHERE entity_type = ? AND entity_id IN (...)<br/>ORDER BY created_at DESC
            DB-->>Repo: List<AuditLog>
            Repo-->>ES: List<AuditLog>
        end
        ES->>ES: Merge & sort by createdAt DESC

        ES->>ES: financeService.getFinanceOverviewReport(activityId)<br/>(Lấy reserveCategoryId = wallet "Khác")
        ES->>Repo: (qua PreparationFinanceService) Truy vấn budgets, wallets
        Repo->>DB: SELECT * FROM budget_categories WHERE name = 'Khác' AND activity_id = ?
        DB-->>Repo: BudgetCategory (reserve)
        Repo-->>ES: reserveCategoryId

        ES->>Repo: allocationAdjustmentRequestRepository.findAllById(adjIds)
        Repo->>DB: SELECT * FROM allocation_adjustment_requests WHERE id IN (...)
        DB-->>Repo: List<AllocationAdjustmentRequest>
        Repo-->>ES: Map<Long, AllocationAdjustmentRequest>

        Note over ES, FG: TẠO FILE BÁO CÁO AUDIT
        alt format == xlsx
            ES->>FG: Tạo XSSFWorkbook với các sheet:<br/>AuditLogs, ReserveTransfers (nếu có reserveCategoryId)
            FG-->>ES: byte[] (workbook bytes)
        else format == pdf
            ES->>FG: Tạo Document (OpenPDF / iText) với các section:<br/>Audit Logs, Reserve Transfers (Wallet 'Khác')
            FG-->>ES: byte[] (pdf bytes)
        end

        ES->>ES: Tạo ExportFile(filename, contentType, bytes)<br/>filename = preparation_audit_activity_{id}_{timestamp}.{ext}
        ES-->>EC: ExportFile

        par GHI AUDIT LOG (ĐỒNG THỜI)
            ES->>Repo: auditLogRepository.save(AuditLog)<br/>who={actor}, when={now}, what="AUDIT_EXPORT",<br/>entityId={activityId}, detail={format}
            Repo->>DB: INSERT INTO audit_logs (...) VALUES (...)
            DB-->>Repo: AuditLog record
            Repo-->>ES: AuditLog
        and TRẢ VỀ FILE
            EC->>EC: fileResponse(file)
            EC-->>Client: 200 OK<br/>Body: byte[] (file download)
        end

        Client->>Client: Tự động tải file / Preview
        Client-->>Admin: Thông báo "Xuất báo cáo audit thành công"
    end
```

---

## Tóm tắt thành phần và chức năng

### Participants (Actors & Hệ thống)

| Participant | Vai trò | Mô tả |
|-------------|---------|-------|
| **Admin/Manager** | Actor | Người quản trị hoặc người phụ trách hoạt động, có quyền xuất báo cáo tài chính, hoạt động và audit. Có thể là Admin, Manager, Supervisor hoặc Organizer. |
| **Client** | React Frontend | Ứng dụng React giao tiếp với backend qua REST API. Xử lý form chọn format (xlsx/pdf), trigger download, hiển thị trạng thái loading/thành công/lỗi. |
| **Controller** | Spring REST | `PreparationExportController` tiếp nhận HTTP GET request, phân loại endpoint (`/financial`, `/operational`, `/audit`), gọi Service, trả về `ResponseEntity<byte[]>` với header `Content-Disposition: attachment`. |
| **Service** | Business Logic | `PreparationExportServiceImpl` chứa toàn bộ logic: validate activity (`findByIdAndIsDeletedFalse`), kiểm tra `isHasPreparation`, tổng hợp dữ liệu tài chính/hoạt động/audit, tạo file Excel/PDF. |
| **Repository** | Data Access | Các JPA Repository (`ActivityRepository`, `PreparationTaskRepository`, `PreparationTaskMemberRepository`, `ExpenseRepository`, `FundAdvanceRepository`, `AllocationAdjustmentRequestRepository`, `AuditLogRepository`, `ActivityOrganizerRepository`, `StudentRepository`, `ActivityRegistrationRepository`, `ActivityParticipationRepository`) giao tiếp với Database. |
| **Database** | PostgreSQL / MySQL | Cơ sở dữ liệu quan hệ lưu trữ: activities, preparation_tasks, preparation_task_members, expenses, fund_advances, budget_categories, allocation_adjustment_requests, audit_logs, students, activity_organizers, activity_registrations, activity_participations. |
| **FileGenerator** | Excel / PDF Engine | Apache POI (`XSSFWorkbook`) cho XLSX; OpenPDF / iText (`Document`, `PdfPTable`, `PdfPageEventHelper`) cho PDF. Tạo file byte array để trả về client dạng download. |

### Chức năng từng Sequence Diagram

| # | Tên | Endpoint chính | Đặc điểm kỹ thuật |
|---|-----|-----------------|-------------------|
| 1 | Xuất báo cáo tài chính | `GET /api/preparation/activities/{id}/exports/financial` | Tổng hợp 4 nguồn dữ liệu qua `PreparationFinanceService`: FinanceOverview (budget vs actual), CashFlow (approved spent, invoice status), FundAdvanceDebts, CashTransactions (từ AuditLog). Có 4 sheet/section chính. Transactional `readOnly = true`. |
| 2 | Xuất báo cáo hoạt động | `GET /api/preparation/activities/{id}/exports/operational` | Lấy task list (deadline, status, allocated), member assignments (leader/member), workload (task count per student), organizers, registrations, check-in/check-out, expense evidence. Có 3 sheet/section chính. Transactional `readOnly = true`. |
| 3 | Xuất báo cáo audit | `GET /api/preparation/activities/{id}/exports/audit` | Lấy audit logs cho các entity: PreparationTask, Expense, FundAdvance, AllocationAdjustmentRequest. Có thêm sheet ReserveTransfers cho wallet "Khác" (nếu có). Transactional `readOnly = true`. |
| 4 | AuditLog (cross-cutting) | Đồng thời với J.42 | Khi xuất Operational hoặc Audit, hệ thống ghi log `AUDIT_EXPORT` / `OPERATIONAL_EXPORT` vào bảng `audit_logs` ghi nhận: who (actor), when (timestamp), what (loại báo cáo), format (xlsx/pdf), activityId. |
