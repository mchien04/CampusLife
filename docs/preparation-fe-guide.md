# Hướng dẫn Frontend (TypeScript) - Module Chuẩn bị Sự kiện (Preparation) - Tài chính v2

## 1. Tổng quan thay đổi (v2)
Module tài chính Preparation đã nâng cấp theo mô hình:
- `ActivityBudget` (1-1 với Activity) và nhiều `BudgetCategory` (Marketing, Hậu cần...)
- `PreparationTask` có `ownerId` (Leader), `allocatedAmount`, `isFinancial`
- `FundAdvance` 2 bước `REQUESTED → HOLDING/REJECTED`, gắn ví nguồn (`categoryId`) và trừ dần khi chi phí được duyệt cấp cuối
- `Expense` duyệt 2 cấp với `status`: `PENDING_LEADER → PENDING_ADMIN → APPROVED` hoặc `REJECTED`
- `AuditLog` ghi lại các thay đổi tài chính

Lưu ý: API/DTO tài chính v1 (Budget/Expense.approved) đã được thay thế bởi v2.

## 2. UI/UX theo role

### 2.1. MEMBER (thuộc Task)
Màn hình trong trang Activity → tab “Tài chính” (hoặc trang con theo Task):
- Chọn Task (task `isFinancial=true`)
- Chọn Category trong ActivityBudget
- Upload ảnh hóa đơn (MultipartFile) → lấy `evidenceUrl`
- Tạo Expense (`PENDING_LEADER`)
- Xem trạng thái Expense và lịch sử của chính mình (có thể filter theo Activity)

### 2.2. LEADER (owner của Task)
Trong trang Task:
- Danh sách Expense đang `PENDING_LEADER`
- Xem minh chứng (evidenceUrl) và duyệt:
  - Approve: chuyển `PENDING_ADMIN`
  - Reject: chuyển `REJECTED`
- Tạo yêu cầu ứng (FundAdvance REQUESTED) theo ví nguồn
- Nhận task / yêu cầu hoàn thành task (workflow)

### 2.3. ADMIN/MANAGER
Trang quản trị Preparation Finance:
- Khởi tạo ActivityBudget + Categories (và cập nhật allocated theo từng category)
- Cấp phát `allocatedAmount` cho Task theo ví (TaskAllocation)
- Duyệt FundAdvance (REQUESTED → HOLDING/REJECTED) và hoàn ứng (HOLDING → SETTLED)
- Duyệt cấp cuối (`PENDING_ADMIN → APPROVED/REJECTED`)
- Xem Financial Report (chi theo category + task vượt budget)

## 3. Luồng nghiệp vụ (để FE implement đúng)

### 3.1. Duyệt chi phí 2 cấp
1) MEMBER tạo Expense → `PENDING_LEADER` và gửi notification cho LEADER
2) LEADER duyệt:
   - Approved → `PENDING_ADMIN` và gửi notification cho ADMIN/MANAGER
   - Rejected → `REJECTED` và notify MEMBER
3) ADMIN/MANAGER duyệt cấp cuối:
   - Có thể duyệt trực tiếp từ `PENDING_LEADER` (bỏ qua leader) do quyền cao hơn
   - Approved → `APPROVED` và thực hiện atomically trong transaction:
     - Trừ `FundAdvance.remainingAmount` (FIFO theo createdAt) của member theo task và theo ví (`categoryId`)
     - Cộng `BudgetCategory.usedAmount`
     - Ghi `AuditLog`
     - Notify MEMBER + cảnh báo “ví sắp cạn” theo `cashAvailableAmount` nếu <= 10%
   - Rejected → `REJECTED` và notify MEMBER

### 3.2. Ràng buộc ngân sách (quan trọng)
- `sum(categories.allocatedAmount) <= activityBudget.totalAmount`
- `category.allocatedAmount >= category.usedAmount`
- `sum(task.allocatedAmount theo activity) <= activityBudget.totalAmount`
- Khi duyệt cấp cuối:
  - Không vượt `task.allocatedAmount`
  - Không vượt `category.allocatedAmount - category.usedAmount`
  - Không vượt `category.cashAvailableAmount` (để ứng)
  - Không vượt tổng FundAdvance còn lại của member theo task và theo ví

## 4. API contract & types TypeScript

### 4.1. Wrapper response
```ts
export type ApiResponse<T> = {
  status: boolean;
  message: string;
  body: T;
};
```

### 4.2. Enum types
```ts
export type PreparationTaskStatus = 'PENDING' | 'ACCEPTED' | 'COMPLETION_REQUESTED' | 'COMPLETED';
export type ExpenseStatus = 'PENDING_LEADER' | 'PENDING_ADMIN' | 'APPROVED' | 'REJECTED';
export type FundAdvanceStatus = 'REQUESTED' | 'HOLDING' | 'SETTLED' | 'REJECTED';
export type PreparationTaskMemberRole = 'LEADER' | 'MEMBER';
export type AllocationAdjustmentStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type WorkloadWarningType = 'OVERLOADED' | 'UNASSIGNED';
```

### 4.3. DTO types (API trả về)
```ts
export type UploadResultDto = { url: string };

export type PreparationTaskDto = {
  id: number;
  activityId: number;
  ownerId: number;
  ownerName: string | null;
  title: string;
  description: string | null;
  deadline: string | null;
  allocatedAmount: string;
  isFinancial: boolean;
  status: PreparationTaskStatus;
};

export type PreparationTaskMemberDto = {
  studentId: number;
  studentName: string | null;
  role: PreparationTaskMemberRole;
};

export type MyPreparationTaskDto = {
  id: number;
  activityId: number;
  ownerId: number;
  ownerName: string | null;
  title: string;
  description: string | null;
  deadline: string | null;
  allocatedAmount: string;
  isFinancial: boolean;
  status: PreparationTaskStatus;
  myRole: PreparationTaskMemberRole;
};

export type OrganizerDto = {
  studentId: number;
  fullName: string | null;
};

export type PreparationDashboardDto = {
  activityId: number;
  hasPreparation: boolean;
  tasks: PreparationTaskDto[];
  budget: null;
  financeMessage: string | null;
};

export type BudgetCategoryDto = {
  id: number;
  name: string;
  allocatedAmount: string;
  allocatedToTasksAmount: string;
  availableToAllocateAmount: string;
  cashOutsideAmount: string;
  cashAvailableAmount: string;
  usedAmount: string;
  remainingAmount: string;
  usedPercent: number;
};

export type ActivityBudgetDto = {
  id: number;
  activityId: number;
  totalAmount: string;
  categories: BudgetCategoryDto[];
};

export type ExpenseDto = {
  id: number;
  activityId: number | null;
  taskId: number | null;
  categoryId: number | null;
  categoryName: string | null;
  amount: string;
  description: string | null;
  evidenceUrl: string | null;
  status: ExpenseStatus;
  createdById: number | null;
  createdByName: string | null;
  createdAt: string;
};

export type FundAdvanceDto = {
  id: number;
  taskId: number;
  categoryId: number | null;
  categoryName: string | null;
  studentId: number;
  studentName: string | null;
  requestedById: number | null;
  requestedByName: string | null;
  amount: string;
  remainingAmount: string;
  status: FundAdvanceStatus;
  createdAt: string;
  decidedAt: string | null;
};

export type TaskOverBudgetDto = {
  taskId: number;
  title: string;
  allocatedAmount: string;
  approvedSpent: string;
};

export type AllocationSourceSuggestionDto = {
  categoryId: number;
  categoryName: string;
  availableToAllocateAmount: string;
};

export type AllocationAdjustmentSourcePlanDto = {
  categoryId: number;
  categoryName: string | null;
  amount: string;
};

export type FundAdvanceSourceSuggestionDto = {
  categoryId: number;
  categoryName: string | null;
  allocationRemainingAmount: string;
  cashAvailableAmount: string;
  maxAdvanceAmount: string;
};

export type ExpenseCategorySuggestionDto = {
  categoryId: number;
  categoryName: string | null;
  allocationRemainingAmount: string;
  walletRemainingAmount: string;
  maxExpenseAmount: string;
};

export type TaskAllocationSourceDto = {
  categoryId: number;
  categoryName: string | null;
  allocatedAmount: string;
  holdingAdvanceAmount: string;
  approvedSpentAmount: string;
  allocationRemainingAmount: string;
};

export type FundAdvanceDebtDto = {
  studentId: number;
  studentName: string | null;
  holdingAmount: string;
};

export type InvoiceStatusSummaryDto = {
  status: ExpenseStatus;
  count: number;
  totalAmount: string;
};

export type TaskSpendStatusDto = {
  taskId: number;
  title: string | null;
  allocatedAmount: string;
  committedAmount: string;
  approvedSpent: string;
  usedPercent: number;
};

export type FinanceOverviewReportDto = {
  activityId: number;
  totalBudget: string;
  totalAllocatedToTasks: string;
  totalApprovedSpent: string;
  varianceAllocatedVsApproved: string;
  wallets: BudgetCategoryDto[];
  tasks: TaskSpendStatusDto[];
};

export type CashFlowReportDto = {
  activityId: number;
  totalBudget: string;
  approvedSpent: string;
  cashOutsideWallet: string;
  cashInsideWallet: string;
  advanceDebts: FundAdvanceDebtDto[];
  invoiceStatusSummary: InvoiceStatusSummaryDto[];
};

export type WorkloadWarningDto = {
  studentId: number;
  studentName: string | null;
  taskCount: number;
  type: WorkloadWarningType;
};

export type OverBudgetInfoDto = {
  taskId: number;
  requiredAdditionalAmount: string;
  currentAllocatedAmount: string;
  committedAmount: string;
  suggestedSources: AllocationSourceSuggestionDto[];
};

export type AllocationAdjustmentRequestDto = {
  id: number;
  activityId: number;
  taskId: number;
  amount: string;
  description: string;
  status: AllocationAdjustmentStatus;
  requestedById: number | null;
  requestedByName: string | null;
  createdAt: string;
  decidedAt: string | null;
  decidedById: number | null;
};

export type FinancialReportDto = {
  activityId: number;
  totalBudget: string;
  categories: BudgetCategoryDto[];
  overBudgetTasks: TaskOverBudgetDto[];
};
```

### 4.4. Request types (FE gửi lên)
```ts
export type ApproveExpenseRequest = { approved: boolean };

export type ApproveFundAdvanceRequest = { approved: boolean };

export type UpsertBudgetCategoryRequest = {
  name: string;
  allocatedAmount: string;
};

export type UpsertActivityBudgetRequest = {
  totalAmount: string;
  categories: UpsertBudgetCategoryRequest[];
};

export type AllocateTaskAmountRequest = {
  categoryId: number;
  allocatedAmount: string;
};

export type CreateFundAdvanceRequest = {
  studentId: number;
  categoryId: number;
  amount: string;
};

export type CreateExpenseRequest = {
  categoryId: number;
  amount: string;
  description?: string | null;
  evidenceUrl?: string | null;
};

export type CreateAllocationAdjustmentRequest = {
  amount: string;
  description: string;
};

export type AdminDecisionAllocationAdjustmentRequest = {
  approved: boolean;
  categoryId?: number | null;
  sources?: { categoryId: number; amount: string }[];
};

export type ApproveTaskCompletionRequest = { approved: boolean };

export type TogglePreparationRequest = { enabled: boolean };
```

## 5. Endpoint usage theo role

### 5.1. Common
- `PUT /api/preparation/activities/{activityId}/toggle?enabled=true|false`
- `GET /api/preparation/activities/{activityId}/dashboard` (tasks + hasPreparation)
- `GET /api/preparation/my/activity-ids`
- `GET /api/preparation/activities/{activityId}/organizers`
- `GET /api/preparation/activities/{activityId}/financial-report`
- `GET /api/preparation/activities/{activityId}/expenses?status=PENDING_LEADER|PENDING_ADMIN|APPROVED|REJECTED` (status optional)
- `GET /api/preparation/activities/{activityId}/budget`
- `GET /api/preparation/activities/{activityId}/workload-warnings`
- `GET /api/preparation/activities/{activityId}/reports/finance-overview`
- `GET /api/preparation/activities/{activityId}/reports/cash-flow`

### 5.2. MEMBER
- Upload hóa đơn:
  - `POST /api/preparation/tasks/{taskId}/expenses/evidence` (multipart)
- Tạo expense:
  - `POST /api/preparation/tasks/{taskId}/expenses`
- Gợi ý ví để tạo expense:
  - `GET /api/preparation/tasks/{taskId}/expense-category-suggestions?amount=...`
- Xin bổ sung allocate:
  - `POST /api/preparation/tasks/{taskId}/allocation-adjustments`
- Danh sách task của tôi + role theo activity:
  - `GET /api/preparation/my/activities/tasks?activityId=...`

### 5.3. LEADER
- Thêm member vào task:
  - `POST /api/preparation/tasks/{taskId}/members/{studentId}`
- Xem member theo task:
  - `GET /api/preparation/tasks/{taskId}/members`
- Gán/thu hồi leader:
  - `POST /api/preparation/tasks/{taskId}/leaders/{studentId}`
  - `DELETE /api/preparation/tasks/{taskId}/leaders/{studentId}`
- Duyệt cấp 1:
  - `PUT /api/preparation/expenses/{expenseId}/leader-decision`
- Request ứng:
  - `POST /api/preparation/tasks/{taskId}/fund-advances`
- Gợi ý nguồn ví để ứng:
  - `GET /api/preparation/tasks/{taskId}/fund-advance-source-suggestions?amount=...`
- Danh sách tạm ứng của tôi (người nhận ứng) theo activity:
  - `GET /api/preparation/my/fund-advances?activityId=...&taskId=...`
- Danh sách tạm ứng theo task (xem lịch sử của cả task):
  - `GET /api/preparation/tasks/{taskId}/fund-advances`
- Xem allocation sources theo task:
  - `GET /api/preparation/tasks/{taskId}/allocation-sources`
- Nhận task / yêu cầu hoàn thành:
  - `PUT /api/preparation/tasks/{taskId}/accept` (member/leader)
  - `PUT /api/preparation/tasks/{taskId}/request-complete`
  - `PUT /api/preparation/tasks/{taskId}/status` (update status chung)

### 5.4. ADMIN/MANAGER
- Upsert activity budget + categories:
  - `PUT /api/preparation/activities/{activityId}/budget`
- Quản lý organizer:
  - `POST /api/preparation/activities/{activityId}/organizers/{studentId}`
  - `DELETE /api/preparation/activities/{activityId}/organizers/{studentId}`
- Tạo task:
  - `POST /api/preparation/activities/{activityId}/tasks`
- Allocate amount cho task:
  - `PUT /api/preparation/tasks/{taskId}/allocation`
- Xem allocation sources theo task:
  - `GET /api/preparation/tasks/{taskId}/allocation-sources`
- Duyệt/từ chối fund advance:
  - `PUT /api/preparation/fund-advances/{fundAdvanceId}/admin-decision`
- Hoàn ứng:
  - `PUT /api/preparation/fund-advances/{fundAdvanceId}/return`
- Danh sách fund advance theo task:
  - `GET /api/preparation/tasks/{taskId}/fund-advances`
- Nợ tạm ứng theo activity/student:
  - `GET /api/preparation/activities/{activityId}/fund-advance-debts?studentId=...`
- Duyệt cấp cuối:
  - `PUT /api/preparation/expenses/{expenseId}/admin-decision`
- Danh sách/duyệt request bổ sung allocate:
  - `GET /api/preparation/activities/{activityId}/allocation-adjustments?status=...`
  - `GET /api/preparation/allocation-adjustments/{requestId}/source-suggestions`
  - `GET /api/preparation/allocation-adjustments/{requestId}/source-plan`
  - `PUT /api/preparation/allocation-adjustments/{requestId}/admin-decision`
- Duyệt hoàn thành task:
  - `PUT /api/preparation/tasks/{taskId}/complete-decision`

## 6. Gợi ý client code (fetch)

```ts
async function apiFetch<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    ...init,
    headers: {
      ...(init?.headers ?? {}),
      'Content-Type': init?.body instanceof FormData ? undefined : 'application/json',
    },
  });
  const data = (await res.json()) as ApiResponse<T>;
  if (!data.status) throw new Error(data.message || 'Request failed');
  return data.body;
}

export async function uploadExpenseEvidence(taskId: number, file: File): Promise<string> {
  const fd = new FormData();
  fd.append('file', file);
  const body = await apiFetch<UploadResultDto>(`/api/preparation/tasks/${taskId}/expenses/evidence`, {
    method: 'POST',
    body: fd,
  });
  return body.url;
}

export async function createExpense(taskId: number, payload: CreateExpenseRequest) {
  return apiFetch<ExpenseDto>(`/api/preparation/tasks/${taskId}/expenses`, {
    method: 'POST',
    body: JSON.stringify({
      categoryId: payload.categoryId,
      amount: payload.amount,
      description: payload.description ?? null,
      evidenceUrl: payload.evidenceUrl ?? null,
    }),
  });
}

export async function leaderDecision(expenseId: number, approved: boolean) {
  return apiFetch<ExpenseDto>(`/api/preparation/expenses/${expenseId}/leader-decision`, {
    method: 'PUT',
    body: JSON.stringify({ approved } satisfies ApproveExpenseRequest),
  });
}

export async function adminDecision(expenseId: number, approved: boolean) {
  return apiFetch<ExpenseDto>(`/api/preparation/expenses/${expenseId}/admin-decision`, {
    method: 'PUT',
    body: JSON.stringify({ approved } satisfies ApproveExpenseRequest),
  });
}
```

## 7. Nén ảnh hóa đơn (khuyến nghị)
```ts
export async function compressImage(file: File, opts?: { maxWidth?: number; quality?: number }) {
  const maxWidth = opts?.maxWidth ?? 1280;
  const quality = opts?.quality ?? 0.75;
  const imageBitmap = await createImageBitmap(file);
  const scale = Math.min(1, maxWidth / imageBitmap.width);
  const targetW = Math.round(imageBitmap.width * scale);
  const targetH = Math.round(imageBitmap.height * scale);
  const canvas = document.createElement('canvas');
  canvas.width = targetW;
  canvas.height = targetH;
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('Canvas not supported');
  ctx.drawImage(imageBitmap, 0, 0, targetW, targetH);
  const blob: Blob = await new Promise((resolve, reject) => {
    canvas.toBlob(b => (b ? resolve(b) : reject(new Error('Failed to compress image'))), 'image/jpeg', quality);
  });
  return new File([blob], file.name.replace(/\.\w+$/, '.jpg'), { type: 'image/jpeg' });
}
```
