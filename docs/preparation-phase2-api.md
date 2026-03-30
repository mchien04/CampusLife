# Phase 2 - Chuẩn bị Sự kiện (Preparation) - Ví ngân sách & hạng mục “Khác” (Residual)

## 1. Upsert ngân sách theo ví (tổng + hạng mục + “Khác”)

### 1. Mô tả nghiệp vụ
- Mỗi Activity có 1 ngân sách tổng (ví lớn).
- Nếu không nhập hạng mục, hệ thống tự tạo 1 ví mặc định “Tổng” với hạn mức = totalAmount (để tạo Expense vẫn có categoryId).
- Nếu có nhập hạng mục:
  - Người dùng chỉ nhập các hạng mục chính (ví dụ: “Bàn ghế”, “Hoa”…).
  - Hệ thống tự tính và tự tạo/cập nhật hạng mục “Khác” = totalAmount - sum(hạng mục chính).
  - Không cho cấu hình làm “Khác” nhỏ hơn số tiền đã chi (usedAmount) của “Khác” (nếu đã phát sinh chi).

### 2. API Endpoint
- **Method:** PUT
- **Path:** /api/preparation/activities/{activityId}/budget
- **Authentication:** Required (ADMIN/MANAGER)

### 3. Request
- **Path Parameters:**
  - activityId: long - ID activity
- **Query Parameters:** none
- **Request Body:**
```json
{
  "totalAmount": "string - tổng ngân sách >= 0 (BigDecimal dạng chuỗi)",
  "categories": [
    {
      "name": "string - tên hạng mục",
      "allocatedAmount": "string - ngân sách hạng mục >= 0 (BigDecimal dạng chuỗi)"
    }
  ]
}
```

### 4. Response
- **Success (200):**
```json
{
  "status": true,
  "message": "OK",
  "body": {
    "id": 1,
    "activityId": 34,
    "totalAmount": 5000000,
    "categories": [
      {
        "id": 11,
        "name": "Bàn ghế",
        "allocatedAmount": 2000000,
        "allocatedToTasksAmount": 0,
        "availableToAllocateAmount": 2000000,
        "cashOutsideAmount": 0,
        "cashAvailableAmount": 2000000,
        "usedAmount": 0,
        "remainingAmount": 2000000,
        "usedPercent": 0
      },
      {
        "id": 12,
        "name": "Hoa",
        "allocatedAmount": 1500000,
        "allocatedToTasksAmount": 0,
        "availableToAllocateAmount": 1500000,
        "cashOutsideAmount": 0,
        "cashAvailableAmount": 1500000,
        "usedAmount": 0,
        "remainingAmount": 1500000,
        "usedPercent": 0
      },
      {
        "id": 13,
        "name": "Khác",
        "allocatedAmount": 1500000,
        "allocatedToTasksAmount": 0,
        "availableToAllocateAmount": 1500000,
        "cashOutsideAmount": 0,
        "cashAvailableAmount": 1500000,
        "usedAmount": 0,
        "remainingAmount": 1500000,
        "usedPercent": 0
      }
    ]
  }
}
```
- **Error Responses:**
  - 400: tổng category vượt totalAmount; categoryName rỗng; không thể giảm allocated < used; “Khác” nhỏ hơn used
  - 403: không đủ quyền
  - 404: activity không tồn tại

## 2. Xem chi tiết ngân sách theo Activity

### 1. Mô tả nghiệp vụ
Trả về ActivityBudget + danh sách BudgetCategory để FE hiển thị danh sách ví và chọn hạng mục khi tạo Expense.

### 2. API Endpoint
- **Method:** GET
- **Path:** /api/preparation/activities/{activityId}/budget
- **Authentication:** Required (ADMIN/MANAGER hoặc Organizer của activity)

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
    "id": 1,
    "activityId": 34,
    "totalAmount": 5000000,
    "categories": [
      {
        "id": 13,
        "name": "Khác",
        "allocatedAmount": 1500000,
        "allocatedToTasksAmount": 300000,
        "availableToAllocateAmount": 1200000,
        "cashOutsideAmount": 200000,
        "cashAvailableAmount": 1100000,
        "usedAmount": 200000,
        "remainingAmount": 1300000,
        "usedPercent": 13.33
      }
    ]
  }
}
```
- **Error Responses:**
  - 400: preparation chưa bật
  - 403: không đủ quyền
  - 404: không tìm thấy ActivityBudget hoặc Activity
