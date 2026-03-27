# Báo cáo nâng cấp - Module Chuẩn bị Sự kiện (Preparation) - Tài chính v2

## 1) Mục tiêu nâng cấp
- Chuẩn hoá quản lý tài chính theo Activity bằng ngân sách theo hạng mục (Category).
- Tách rõ quyền và luồng duyệt chi phí 2 cấp: Leader → Admin/Manager.
- Ghi nhận AuditLog cho mọi thay đổi tài chính.
- Tự động cập nhật số dư tạm ứng và ngân sách hạng mục khi duyệt cấp cuối.

## 2) Thay đổi chính (so với v1)
- Ngân sách:
  - v1: `Budget` (1-1 Activity)
  - v2: `ActivityBudget` (1-1 Activity) + nhiều `BudgetCategory` (Marketing, Hậu cần...)
- Chi phí:
  - v1: `Expense.approved: Boolean | null` (WAITING_APPROVAL/APPROVED/REJECTED)
  - v2: `Expense.status: ExpenseStatus` (PENDING_LEADER/PENDING_ADMIN/APPROVED/REJECTED)
- Tạm ứng:
  - v2 bổ sung `FundAdvance` để theo dõi tạm ứng theo (Task, Student), trừ dần khi chi phí APPROVED cấp cuối.
- Task tài chính:
  - v2 nâng cấp `PreparationTask` có `owner (Leader)`, `budgetLimit`, `allocatedAmount`, `isFinancial`.
- Audit:
  - v2 bổ sung `AuditLog` lưu ai thay đổi, thay đổi gì, lúc nào.

## 3) Files/Thành phần mới
### 3.1. Entity/Enum
- `ActivityBudget`, `BudgetCategory`
- `PreparationTaskMember`
- `FundAdvance`, `FundAdvanceStatus`
- `Expense` (map sang bảng `preparation_expenses`), `ExpenseStatus`
- `AuditLog`

### 3.2. Repository
- `ActivityBudgetRepository`
- `BudgetCategoryRepository`
- `PreparationTaskMemberRepository`
- `FundAdvanceRepository`
- `AuditLogRepository`
- `ExpenseRepository` cập nhật query theo Task/Category/Status

### 3.3. Service
- `PreparationFinanceService`, `PreparationFinanceServiceImpl`
  - Transactional duyệt 2 cấp
  - Tự động trừ FundAdvance + cập nhật usedAmount
  - Notification khi có expense chờ duyệt / ngân sách sắp cạn
  - Ghi AuditLog

### 3.4. Controller/API
- `PreparationFinanceController` cung cấp bộ API tài chính v2 (budget, allocation, fund-advances, expense, approvals, report).

## 4) Nghiệp vụ/Rule quan trọng (để FE/BE đồng bộ)
- `sum(categories.allocatedAmount) <= activityBudget.totalAmount`
- `category.allocatedAmount >= category.usedAmount`
- `sum(task.allocatedAmount theo activity) <= activityBudget.totalAmount`
- Khi APPROVED cấp cuối:
  - Không vượt `task.allocatedAmount`
  - Không vượt `task.budgetLimit` (nếu có)
  - Không vượt `category.remaining = allocated - used`
  - Không vượt tổng `FundAdvance.remainingAmount` của member theo task
  - Trừ FundAdvance theo thứ tự tạo (FIFO) và cập nhật `FundAdvance.status` sang SETTLED nếu còn lại = 0

## 5) API v2 (tóm tắt)
- Budget:
  - `PUT /api/preparation/activities/{activityId}/budget` (ADMIN/MANAGER)
- Task allocation & members:
  - `PUT /api/preparation/tasks/{taskId}/allocation` (ADMIN/MANAGER)
  - `POST /api/preparation/tasks/{taskId}/members/{studentId}` (ADMIN/MANAGER hoặc LEADER)
- Fund advance:
  - `POST /api/preparation/tasks/{taskId}/fund-advances` (ADMIN/MANAGER)
- Expense:
  - `POST /api/preparation/tasks/{taskId}/expenses/evidence` (MEMBER)
  - `POST /api/preparation/tasks/{taskId}/expenses` (MEMBER) → `PENDING_LEADER`
  - `PUT /api/preparation/expenses/{expenseId}/leader-decision` (LEADER)
  - `PUT /api/preparation/expenses/{expenseId}/admin-decision` (ADMIN/MANAGER)
  - `GET /api/preparation/activities/{activityId}/expenses?status=...` (ADMIN/MANAGER hoặc Organizer)
- Report:
  - `GET /api/preparation/activities/{activityId}/financial-report` (ADMIN/MANAGER hoặc Organizer)

## 6) Hướng dẫn FE TypeScript
- Xem file `docs/preparation-fe-guide.md` để lấy:
  - TS types/enum cho toàn bộ DTO/Request của v2
  - Luồng nghiệp vụ theo role
  - Ví dụ gọi API (fetch + upload evidence)

