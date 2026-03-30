# Phase 6 - Bộ báo cáo & Notification theo ngưỡng

## 1. Báo cáo Tổng quan Tài chính (Budget vs Actual)

### 1. Mô tả nghiệp vụ
Báo cáo tổng quan để Admin/Organizer nắm “sức khỏe” tài chính:
- Tổng ngân sách (Budget)
- Tổng đã cấp phát cho task (Allocate)
- Tổng chi thực tế đã duyệt (Actual Approved Spend)
- Variance = Allocate - Actual
- Trạng thái các ví (wallet/category), bao gồm “Khác” (Residual) nếu có
- Trạng thái sử dụng theo task (allocated/committed/approved + %)

### 2. API Endpoint
- **Method:** GET
- **Path:** /api/preparation/activities/{activityId}/reports/finance-overview
- **Authentication:** Required (ADMIN/MANAGER hoặc Organizer)

### 3. Request
- **Path Parameters:**
  - activityId: long - ID activity
- **Query Parameters:** none
- **Request Body:** none

### 4. Response
- **Success (200):**
```json
{
  "status": true,
  "message": "OK",
  "body": {
    "activityId": 34,
    "totalBudget": 5000000,
    "totalAllocatedToTasks": 3000000,
    "totalApprovedSpent": 1200000,
    "varianceAllocatedVsApproved": 1800000,
    "wallets": [],
    "tasks": []
  }
}
```
- **Error Responses:**
  - 400: preparation chưa bật hoặc chưa có ActivityBudget
  - 403: không đủ quyền
  - 404: activity không tồn tại

## 2. Báo cáo Dòng tiền & Tạm ứng (Cash Flow & Advance)

### 1. Mô tả nghiệp vụ
Nhóm báo cáo kiểm soát:
- Tiền ngoài ví: tổng `FundAdvance(HOLDING).remainingAmount` theo activity
- Tiền trong ví (ước tính): `totalBudget - approvedSpent - cashOutsideWallet`
- Danh sách nợ tạm ứng theo sinh viên
- Tổng hợp hóa đơn theo trạng thái (PENDING_LEADER/PENDING_ADMIN/APPROVED/REJECTED)

### 2. API Endpoint
- **Method:** GET
- **Path:** /api/preparation/activities/{activityId}/reports/cash-flow
- **Authentication:** Required (ADMIN/MANAGER hoặc Organizer)

### 3. Request
- **Path Parameters:**
  - activityId: long - ID activity
- **Query Parameters:** none
- **Request Body:** none

### 4. Response
- **Success (200):**
```json
{
  "status": true,
  "message": "OK",
  "body": {
    "activityId": 34,
    "totalBudget": 5000000,
    "approvedSpent": 1200000,
    "cashOutsideWallet": 300000,
    "cashInsideWallet": 3500000,
    "advanceDebts": [
      { "studentId": 100, "studentName": "Nguyen Van A", "holdingAmount": 300000 }
    ],
    "invoiceStatusSummary": [
      { "status": "PENDING_ADMIN", "count": 2, "totalAmount": 400000 }
    ]
  }
}
```
- **Error Responses:**
  - 400/403/404: tương tự báo cáo tổng quan

## 3. Notification theo ngưỡng

### 1. Mô tả nghiệp vụ
Khi Admin/Manager duyệt Expense cấp cuối (APPROVED), hệ thống tự trigger:
- Cảnh báo theo task khi `approvedSpent / allocatedAmount` đạt ≥ 80% / 90% / 100%
- Cảnh báo ví sắp cạn khi `BudgetCategory.remainingAmount <= 10% allocatedAmount`

### 2. Ghi chú kỹ thuật
- Dùng AuditLog để chống gửi lặp theo từng ngưỡng trên từng task/ví.

