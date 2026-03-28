# Phase 4 - Chuẩn bị Sự kiện (Preparation) - Allocate theo nguồn ví & xin bổ sung ngân sách

## 1. Allocate theo nguồn ví (BudgetCategory)

### 1. Mô tả nghiệp vụ
- Allocate cho task phải chọn nguồn ví (một `BudgetCategory`).
- Khi allocate, số dư khả dụng của ví giảm (tính theo: `availableToAllocateAmount = allocatedAmount - allocatedToTasksAmount`).
- Tổng allocate từ một ví không được vượt `BudgetCategory.allocatedAmount`.
- Tổng allocate của task = tổng các allocation theo ví (`task.allocatedAmount`).

### 2. API Endpoint
- **Method:** PUT
- **Path:** /api/preparation/tasks/{taskId}/allocation
- **Authentication:** Required (ADMIN/MANAGER)

### 3. Request
- **Path Parameters:**
  - taskId: long - ID task
- **Query Parameters:** none
- **Request Body:**
```json
{
  "categoryId": "long - ID ví (BudgetCategory) của activity",
  "allocatedAmount": "string - số tiền >= 0 (BigDecimal dạng chuỗi)"
}
```

### 4. Response
- **Success (200):**
```json
{
  "status": true,
  "message": "OK",
  "body": {
    "id": 10,
    "activityId": 34,
    "ownerId": 100,
    "ownerName": "Nguyen Van A",
    "title": "Mua hoa",
    "description": null,
    "deadline": "2026-03-30T12:00:00",
    "allocatedAmount": 1500000,
    "isFinancial": true,
    "status": "PENDING"
  }
}
```
- **Error Responses:**
  - 400: task không tài chính / category không thuộc activity / allocated < committed
  - 409: ví không đủ số dư để allocate
  - 404: không tìm thấy task

## 2. Chặn Expense vượt allocate + gợi ý nguồn bổ sung

### 1. Mô tả nghiệp vụ
Khi member tạo Expense, hệ thống kiểm tra “committed” (PENDING_LEADER/PENDING_ADMIN/APPROVED) của task:
- Nếu `committed + amount` vượt `task.allocatedAmount` thì không tạo Expense, trả về thông tin cần bổ sung và danh sách ví còn dư để đề xuất.

### 2. API Endpoint
- **Method:** POST
- **Path:** /api/preparation/tasks/{taskId}/expenses
- **Authentication:** Required (Member của task)

### 3. Request
- **Path Parameters:** taskId (long)
- **Query Parameters:** none
- **Request Body:**
```json
{
  "categoryId": "long - ID hạng mục chi",
  "amount": "string - số tiền > 0",
  "description": "string - optional",
  "evidenceUrl": "string - optional"
}
```

### 4. Response
- **Success (200):** Trả `ExpenseDto`
- **Error Responses:**
  - 409: vượt allocate, trả body `OverBudgetInfoDto`
```json
{
  "status": false,
  "message": "Expense exceeds task allocated amount",
  "body": {
    "taskId": 10,
    "requiredAdditionalAmount": 200000,
    "currentAllocatedAmount": 1000000,
    "committedAmount": 900000,
    "suggestedSources": [
      { "categoryId": 13, "categoryName": "Khác", "availableToAllocateAmount": 1500000 }
    ]
  }
}
```

## 3. Xin bổ sung allocate (AllocationAdjustmentRequest) + admin duyệt

### 1. Mô tả nghiệp vụ
- Member tạo yêu cầu xin bổ sung ngân sách allocate cho task.
- Admin/Manager duyệt hoặc từ chối.
- Khi duyệt: phải chọn ví nguồn (categoryId), hệ thống tăng allocation từ ví đó cho task và cập nhật `task.allocatedAmount`.

### 2. API Endpoint
- **Method:** POST
- **Path:** /api/preparation/tasks/{taskId}/allocation-adjustments
- **Authentication:** Required (Member của task)

### 3. Request
- **Path Parameters:** taskId (long)
- **Query Parameters:** none
- **Request Body:**
```json
{
  "amount": "string - số tiền > 0",
  "preferredCategoryId": "long - optional (gợi ý nguồn)"
}
```

### 4. Response
- **Success (200):** Trả `AllocationAdjustmentRequestDto`

---

### 2. API Endpoint
- **Method:** GET
- **Path:** /api/preparation/activities/{activityId}/allocation-adjustments
- **Authentication:** Required (ADMIN/MANAGER)

### 3. Request
- **Path Parameters:** activityId (long)
- **Query Parameters:**
  - status: AllocationAdjustmentStatus - optional (PENDING/APPROVED/REJECTED)
- **Request Body:** none

### 4. Response
- **Success (200):** Trả list `AllocationAdjustmentRequestDto`

---

### 2. API Endpoint
- **Method:** PUT
- **Path:** /api/preparation/allocation-adjustments/{requestId}/admin-decision
- **Authentication:** Required (ADMIN/MANAGER)

### 3. Request
- **Path Parameters:** requestId (long)
- **Query Parameters:** none
- **Request Body:**
```json
{
  "approved": "boolean - true duyệt, false từ chối",
  "categoryId": "long - bắt buộc khi approved=true"
}
```

### 4. Response
- **Success (200):** Trả `AllocationAdjustmentRequestDto`
- **Error Responses:**
  - 400: request không pending / thiếu categoryId khi approve
  - 409: ví không đủ số dư để allocate
  - 404: request/task/category không tồn tại

