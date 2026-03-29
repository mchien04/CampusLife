# Event Preparation Bu — Báo cáo cho Frontend (tổng hợp Phase 1→6)

Tài liệu này tổng hợp lại toàn bộ nghiệp vụ và API của module **Preparation (Finance v2)** theo các phase. Mục tiêu là giúp FE triển khai UI theo đúng luồng, biết rõ mỗi thao tác gọi API nào, dùng DTO nào và hiển thị gì.

## 0. Nguyên tắc dữ liệu (rất quan trọng)

### 0.1. Hai lớp “ngân sách” và “tiền mặt”
- **Ngân sách ví (BudgetCategory)** quản lý 2 lớp số:
  - `allocatedAmount`: hạn mức ngân sách của ví.
  - `usedAmount`: đã chi thật (chỉ tăng khi Expense APPROVED cấp cuối).
  - `allocatedToTasksAmount`: tổng đã allocate từ ví sang các task (tính từ TaskAllocation).
  - `availableToAllocateAmount = allocatedAmount - allocatedToTasksAmount`: còn có thể cấp phát cho task.
  - `remainingAmount = allocatedAmount - usedAmount`: còn lại để chi thật.
  - `cashOutsideAmount`: tiền đang nằm “ngoài ví” do đang tạm ứng (sum FundAdvance HOLDING theo ví).
  - `cashAvailableAmount = remainingAmount - cashOutsideAmount`: tiền “còn trong ví” để tiếp tục tạm ứng.
- **Allocate (TaskAllocation)** là “giữ chỗ ngân sách” của ví cho task (spending cap), không phải chi thật.
- **FundAdvance** là “tiền mặt đang nằm ở ai”, được trừ dần khi Expense APPROVED.

### 0.2. Residual wallet “Khác”
- Nếu admin cấu hình ngân sách có hạng mục, hệ thống tự tạo/cập nhật ví “Khác” = `totalAmount - sum(hạng mục chính)`.
- Nếu admin không nhập hạng mục, hệ thống tạo ví mặc định “Tổng” để luôn có `categoryId` cho Expense/Advance.

## 1. Wrapper response & types (FE dùng chung)

### 1.1. Wrapper
API đều trả dạng:
```ts
export type ApiResponse<T> = { status: boolean; message: string; body: T };
```

### 1.2. DTO/Enum quan trọng
FE dùng theo [preparation-fe-guide.md](file:///d:/2025-2026%20HKI/TLCN/campuslife/docs/preparation-fe-guide.md), nổi bật:
- `ActivityBudgetDto`, `BudgetCategoryDto`
- `PreparationTaskDto`, `PreparationTaskMemberDto`, `PreparationTaskStatus`
- `ExpenseDto`, `ExpenseStatus`, `OverBudgetInfoDto`
- `FundAdvanceDto`, `FundAdvanceStatus`, `FundAdvanceDebtDto`
- `AllocationAdjustmentRequestDto`, `AllocationAdjustmentStatus`
- `FundAdvanceSourceSuggestionDto` (gợi ý nguồn ví để ứng)
- Report: `FinanceOverviewReportDto`, `CashFlowReportDto`, `TaskSpendStatusDto`, `InvoiceStatusSummaryDto`

## 2. UI theo vai trò (các màn chính)

### 2.1. Common (Organizer/Leader/Member)
**Màn hình Activity → Preparation Dashboard**
- Hiển thị:
  - `hasPreparation` + danh sách task.
  - Thông báo “Preparation chưa bật” nếu cần.
- API:
  - `GET /api/preparation/activities/{activityId}/dashboard`
- DTO:
  - `PreparationDashboardDto`

**Màn hình “My Preparation Activities” (Student)**
- Mục đích: lấy danh sách activityId mà user đang là organizer (để FE filter/quick access).
- API:
  - `GET /api/preparation/my/activity-ids`
- DTO:
  - `number[]` (list activityId)

### 2.2. Admin/Manager: “Finance Admin Panel”
Các tab khuyến nghị:
1) **Budget setup** (ActivityBudget + ví)
2) **Task allocation** (cấp phát theo ví)
3) **Pending invoices** (PENDING_ADMIN)
4) **Allocation adjustment requests** (xin bổ sung allocate)
5) **Fund advances** (duyệt REQUESTED, hoàn ứng, nợ tạm ứng)
6) **Reports** (Budget vs Actual, Cash Flow)

### 2.3. Leader: “Task Leader Panel”
Các tab khuyến nghị trong Task:
1) **Members** (xem/gán leader/member nếu có quyền)
2) **Expenses pending leader** (PENDING_LEADER)
3) **Fund advance request** (tạo yêu cầu ứng)
4) **Task status workflow** (ACCEPT/REQUEST_COMPLETE)

### 2.4. Member: “Task Member”
Trong Task:
1) Upload evidence
2) Create expense
3) Theo dõi trạng thái expense của mình

## 3. Chức năng & API theo Phase

### Phase 0 — Bật/Tắt Preparation + Quản lý Organizer

#### 3.0. Bật/tắt Preparation cho Activity
- Mục đích: bật cờ `hasPreparation` để mở toàn bộ module.
- API:
  - `PUT /api/preparation/activities/{activityId}/toggle?enabled=true|false`
- UI:
  - Admin/Manager toggle switch trên màn quản trị activity.

#### 3.0. Quản lý Organizer của Activity
- Mục đích: xác định BTC/Organizer để phân quyền cho toàn bộ nghiệp vụ.
- API:
  - `GET /api/preparation/activities/{activityId}/organizers`
  - `POST /api/preparation/activities/{activityId}/organizers/{studentId}`
  - `DELETE /api/preparation/activities/{activityId}/organizers/{studentId}`
- DTO:
  - `OrganizerDto[]` (list organizers)
- UI:
  - Admin tab “Organizers”: list + add/remove.

### Phase 1 — Siết quyền truy cập & các nghiệp vụ finance cơ bản

#### 3.1. Thêm member vào task
- Mục đích: đảm bảo sinh viên nhận ứng/chi thuộc organizer và thuộc task.
- API:
  - `POST /api/preparation/tasks/{taskId}/members/{studentId}`
- UI:
  - Admin/Leader chọn sinh viên (organizer) → add.

#### 3.2. Tạo Expense (member)
- Mục đích: member tạo chứng từ chi.
- API:
  - Upload file: `POST /api/preparation/tasks/{taskId}/expenses/evidence` (multipart) → trả `UploadResultDto`
  - Create: `POST /api/preparation/tasks/{taskId}/expenses`
  - List theo activity: `GET /api/preparation/activities/{activityId}/expenses?status=...` (status optional)
- DTO:
  - Request: `CreateExpenseRequest`
  - Response: `ExpenseDto`
- UI:
  - Form: category dropdown (từ ActivityBudget), amount, description, evidenceUrl.
  - List của user: filter theo status.

#### 3.3. Duyệt expense 2 cấp
- Leader:
  - `PUT /api/preparation/expenses/{expenseId}/leader-decision` (body `{approved}`)
- Admin:
  - `PUT /api/preparation/expenses/{expenseId}/admin-decision` (body `{approved}`)
- UI:
  - Leader tab: “Chờ duyệt” → approve/reject.
  - Admin tab: “Chờ kế toán/Admin” → approve/reject.

Ghi chú: tài liệu Phase 1 ban đầu có “FundAdvance do ADMIN tạo ngay HOLDING”. Hiện hệ thống đã nâng cấp theo Phase 5 (2 bước request/approve), xem Phase 5 bên dưới.

### Phase 2 — Ví ngân sách & “Khác” (Residual)

#### 3.4. Upsert ngân sách theo ví
- Mục đích: Admin tạo/cập nhật ngân sách tổng và các ví.
- API:
  - `PUT /api/preparation/activities/{activityId}/budget`
- DTO:
  - Request: `UpsertActivityBudgetRequest` (gồm `totalAmount` + `categories[]`)
  - Response: `ActivityBudgetDto`
- UI:
  - Form totalAmount + table categoryName/allocatedAmount.
  - FE hiển thị thêm dòng “Khác” (từ API trả về).

#### 3.5. Xem chi tiết ngân sách theo Activity
- Mục đích: FE load danh sách ví để:
  - hiển thị trạng thái ví (used/remaining)
  - chọn category khi tạo expense / request ứng
- API:
  - `GET /api/preparation/activities/{activityId}/budget`
- DTO:
  - `ActivityBudgetDto` với `BudgetCategoryDto` (có `availableToAllocateAmount`, `cashAvailableAmount`).
- UI:
  - Wallet table: allocated, allocatedToTasks, availableToAllocate, used, remaining, cashOutside, cashAvailable.
  - Highlight ví “Khác”.

### Phase 3 — Task member/leader + workflow + workload

#### 3.5. Tạo task chuẩn bị (admin)
- Mục đích: tạo task cho activity và gán leader/assignee ban đầu.
- API:
  - `POST /api/preparation/activities/{activityId}/tasks`
- DTO:
  - Request: `CreatePreparationTaskRequest` (ownerId/title/description/deadline/isFinancial)
  - Response: `PreparationTaskDto`
- UI:
  - Admin tạo task form + chọn assignee.

#### 3.5. Update status task (assignee)
- Mục đích: cập nhật status task theo quyền “assignee” (cơ chế chung).
- API:
  - `PUT /api/preparation/tasks/{taskId}/status` (body `{status}`)
- DTO:
  - Request: `UpdatePreparationTaskStatusRequest`
  - Response: `PreparationTaskDto`
- UI:
  - Nút đổi trạng thái (nếu FE dùng luồng status chung; ngoài ra còn workflow accept/request-complete ở mục 3.8).

#### 3.6. Xem members theo task
- API:
  - `GET /api/preparation/tasks/{taskId}/members`
- DTO:
  - `PreparationTaskMemberDto[]`
- UI:
  - Member list with role pill: LEADER/MEMBER.

#### 3.7. Xóa member / gán leader / thu hồi leader
- API:
  - `DELETE /api/preparation/tasks/{taskId}/members/{studentId}`
  - `POST /api/preparation/tasks/{taskId}/leaders/{studentId}`
  - `DELETE /api/preparation/tasks/{taskId}/leaders/{studentId}`
- UI:
  - Action menu theo từng member.
  - Với task tài chính: luôn phải còn ≥ 1 leader.

#### 3.8. Workflow trạng thái task
- API:
  - `PUT /api/preparation/tasks/{taskId}/accept` (member/leader)
  - `PUT /api/preparation/tasks/{taskId}/request-complete`
  - `PUT /api/preparation/tasks/{taskId}/complete-decision` (admin)
- UI:
  - Task header hiển thị status + action theo role.

#### 3.9. Cảnh báo workload theo Activity
- API:
  - `GET /api/preparation/activities/{activityId}/workload-warnings`
- DTO:
  - `WorkloadWarningDto[]`
- UI:
  - Widget cảnh báo: OVERLOADED / UNASSIGNED.

### Phase 4 — Allocate theo nguồn ví & xin bổ sung ngân sách

#### 3.10. Allocate cho task theo ví
- Mục đích: Admin “giữ chỗ ngân sách” từ ví cho task.
- API:
  - `PUT /api/preparation/tasks/{taskId}/allocation`
- DTO:
  - Request: `AllocateTaskAmountRequest` (`categoryId`, `allocatedAmount`)
  - Response: `PreparationTaskDto` (allocatedAmount là tổng allocation của task)
- UI:
  - Admin tab “Task allocation”:
    - chọn task
    - chọn ví nguồn
    - nhập allocatedAmount cho cặp (task, category)
  - Nên hiển thị: availableToAllocate của ví để admin biết còn dư bao nhiêu.

#### 3.11. Chặn Expense vượt allocate + gợi ý ví bổ sung
- Mục đích: member không thể tạo expense nếu committed vượt `task.allocatedAmount`.
- API:
  - `POST /api/preparation/tasks/{taskId}/expenses`
  - Nếu vượt: HTTP 409 trả `OverBudgetInfoDto` (kèm suggestedSources).
- UI:
  - Khi nhận 409:
    - hiển thị modal “Vượt allocate”
    - show `requiredAdditionalAmount`
    - list suggestedSources (category còn `availableToAllocateAmount`)
    - CTA: “Tạo yêu cầu bổ sung allocate”

#### 3.12. Xin bổ sung allocate (member) + admin duyệt
- API:
  - Member: `POST /api/preparation/tasks/{taskId}/allocation-adjustments`
  - Admin list: `GET /api/preparation/activities/{activityId}/allocation-adjustments?status=...`
  - Admin decision: `PUT /api/preparation/allocation-adjustments/{requestId}/admin-decision`
- DTO:
  - `AllocationAdjustmentRequestDto`
- UI:
  - Member: form request amount + preferredCategory (optional).
  - Admin: bảng pending, approve chọn categoryId.

### Phase 5 — FundAdvance 2 bước + “dứt điểm kỳ trước”

#### 3.13. Leader tạo yêu cầu ứng (REQUESTED)
- Mục đích: yêu cầu tạm ứng phải gắn **ví nguồn** để đảm bảo nhất quán với chi phí.
- API:
  - `POST /api/preparation/tasks/{taskId}/fund-advances`
- DTO:
  - Request: `CreateFundAdvanceRequest` (`studentId`, `categoryId`, `amount`)
  - Response: `FundAdvanceDto`
- UI:
  - Leader tab “Fund advance request”:
    - chọn member (đã là task member)
    - chọn ví nguồn
    - nhập amount
    - submit → tạo REQUESTED
  - Nếu user còn HOLDING chưa quyết toán trong activity → show error “dứt điểm kỳ trước”.

#### 3.14. Gợi ý nguồn ví để ứng theo allocation (bổ sung)
- Mục đích: FE chọn ví nhanh, đúng theo “task allocation còn dư” + “cashAvailable của ví”.
- API:
  - `GET /api/preparation/tasks/{taskId}/fund-advance-source-suggestions?amount=...`
- DTO:
  - `FundAdvanceSourceSuggestionDto[]`
- UI:
  - Leader chọn amount (optional) → gọi API suggestions:
    - hiển thị list ví, show:
      - allocationRemainingAmount
      - cashAvailableAmount
      - maxAdvanceAmount
    - chọn 1 ví để fill vào form request ứng.

#### 3.15. Admin duyệt ứng (REQUESTED → HOLDING/REJECTED)
- API:
  - `PUT /api/preparation/fund-advances/{fundAdvanceId}/admin-decision` (body `{approved}`)
- UI:
  - Admin tab “Fund advances”:
    - list REQUESTED
    - approve/reject
  - Khi approve: hệ thống kiểm `cashAvailableAmount` của ví nguồn đủ.

#### 3.16. Hoàn ứng (HOLDING → SETTLED)
- API:
  - `PUT /api/preparation/fund-advances/{fundAdvanceId}/return`
- UI:
  - Admin thao tác khi member nộp lại tiền thừa (đưa remainingAmount về 0).

#### 3.17. Report nợ tạm ứng theo activity/student
- API:
  - `GET /api/preparation/activities/{activityId}/fund-advance-debts?studentId=...`
- DTO:
  - `FundAdvanceDebtDto[]`
- UI:
  - Admin tab “Debts”: list student + holdingAmount.

### Phase 6 — Reports & notification theo ngưỡng

#### 3.18. Finance overview report (Budget vs Actual)
- API:
  - `GET /api/preparation/activities/{activityId}/reports/finance-overview`
- DTO:
  - `FinanceOverviewReportDto`
- UI:
  - KPI cards: totalBudget, totalAllocatedToTasks, totalApprovedSpent, variance.
  - Wallet table (nhấn mạnh “Khác”).
  - Task usage table: allocated/committed/approved/%; đổi màu theo ngưỡng 80/90/100.

#### 3.19. Cash-flow report (cash in/out, debts, pending invoices)
- API:
  - `GET /api/preparation/activities/{activityId}/reports/cash-flow`
- DTO:
  - `CashFlowReportDto`
- UI:
  - Pie chart: cashInsideWallet vs cashOutsideWallet.
  - List debts (FundAdvanceDebtDto).
  - Invoice status summary (PENDING_LEADER/PENDING_ADMIN/APPROVED/REJECTED).

#### 3.20. Notification theo ngưỡng
- Trigger (server-side):
  - Khi admin duyệt Expense cấp cuối: cảnh báo task 80/90/100 + ví sắp cạn.
- UI (FE):
  - Notification bell đọc từ module notification hiện có (không nằm trong phạm vi tài liệu phase).
  - Trong các bảng report nên hiển thị cảnh báo bằng màu sắc (đồng bộ threshold).

#### 3.21. (Optional) Thống kê cá nhân (student stats)
- Mục đích: endpoint thống kê theo studentId (nằm trong PreparationController, không thuộc nhóm finance v2).
- API:
  - `GET /api/preparation/stats/{id}`
- DTO:
  - `TaskStatsRespone` (model chung, không nằm trong preparation-fe-guide)

## 4. Checklist UI theo luồng end-to-end (đề xuất)

### 4.1. Setup ban đầu (Admin)
1) Upsert budget (Phase 2) → xác nhận có “Khác” hoặc “Tổng”.
2) Tạo task tài chính (Phase 3 API create task trong docs phase3-api) + add members (Phase 1/3).
3) Allocate cho task theo ví (Phase 4) → đảm bảo task có allocation trước khi cho member chi/ứng.

### 4.2. Vận hành (Leader/Member/Admin)
1) Leader request ứng (Phase 5) — nên dùng suggestions (Phase 5.1) để chọn ví.
2) Admin approve ứng → HOLDING.
3) Member tạo expense (Phase 4) → không vượt allocate; leader/admin duyệt.
4) Admin approve expense → trừ FundAdvance của đúng ví và tăng usedAmount ví.
5) Nếu dư tiền: Admin hoàn ứng (Phase 5 return) → đưa HOLDING về 0.

## 5. Mapping nhanh: “chức năng → API”

- **Dashboard**: `GET /activities/{id}/dashboard`
- **Toggle preparation**: `PUT /activities/{id}/toggle?enabled=...`
- **Organizers**: `GET/POST/DELETE /activities/{id}/organizers`
- **Budget setup**: `PUT /activities/{id}/budget`, `GET /activities/{id}/budget`
- **My activity ids**: `GET /my/activity-ids`
- **Task members/leader**: `GET/POST/DELETE` members/leaders (Phase 3)
- **Task create/status**: `POST /activities/{id}/tasks`, `PUT /tasks/{id}/status`
- **Allocate**: `PUT /tasks/{taskId}/allocation`
- **Expense**: upload evidence, create, leader decision, admin decision, list by activity
- **OverBudget**: 409 trả `OverBudgetInfoDto`, tạo allocation adjustment request
- **Allocation adjustment**: create + list + admin decision
- **Fund advance**: request (leader) + suggest sources + admin decision + return + debts report
- **Reports**: finance-overview, cash-flow
- **Stats (optional)**: `GET /stats/{id}`

