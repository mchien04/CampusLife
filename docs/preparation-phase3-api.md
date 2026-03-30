# Phase 3 - Chuẩn bị Sự kiện (Preparation) - Remove budgetLimit

## 0. Migration

### 1. Mô tả nghiệp vụ
Theo tài liệu nghiệp vụ, `budgetLimit` trong task tài chính bị dư thừa. Hệ thống dùng `allocatedAmount` làm “trần chi” và kiểm soát vượt ngân sách dựa vào:
- `task.allocatedAmount`
- `category.remainingAmount`
- `fundAdvance.remainingAmount` tổng

### 2. Migration script
- File: `docs/migrations/2026_03_29_phase3_remove_budget_limit.sql`

## 1. Tạo Task (không còn budgetLimit)

### 1. Mô tả nghiệp vụ
Tạo task chuẩn bị (tài chính hoặc không tài chính). `allocatedAmount` sẽ được quản lý qua API allocate riêng, không khai báo `budgetLimit` khi tạo task.

### 2. API Endpoint
- **Method:** POST
- **Path:** /api/preparation/activities/{activityId}/tasks
- **Authentication:** Required (ADMIN/MANAGER)

### 3. Request
- **Path Parameters:**
  - activityId: long - ID activity
- **Query Parameters:** none
- **Request Body:**
```json
{
  "ownerId": "long - ID leader/assignee",
  "title": "string - tiêu đề task",
  "description": "string - mô tả (optional)",
  "deadline": "string - ISO datetime (optional)",
  "isFinancial": "boolean - task tài chính hay không"
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
    "allocatedAmount": 0,
    "isFinancial": true,
    "status": "PENDING"
  }
}
```
- **Error Responses:**
  - 400: activity chưa bật preparation hoặc owner không thuộc organizer của activity
  - 403: không đủ quyền
  - 404: activity hoặc student không tồn tại

## 2. Allocate cho Task (bỏ ràng buộc budgetLimit)

### 1. Mô tả nghiệp vụ
Admin/Manager cấp `allocatedAmount` cho task. Phê duyệt expense cấp cuối chỉ kiểm tra không vượt `allocatedAmount`, không kiểm tra `budgetLimit`.

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
  "allocatedAmount": "string - số tiền >= 0 (BigDecimal dạng chuỗi)"
}
```

### 4. Response
- **Success (200/201):** Trả `PreparationTaskDto` như API tạo task.
- **Error Responses:**
  - 400: allocatedAmount < approvedSpent hoặc vượt tổng ngân sách activity
  - 404: task không tồn tại

## 3. Báo cáo tài chính (TaskOverBudgetDto không còn budgetLimit)

### 1. Mô tả nghiệp vụ
Danh sách task “vượt budget” được xác định khi `approvedSpent > allocatedAmount`.

### 2. API Endpoint
- **Method:** GET
- **Path:** /api/preparation/activities/{activityId}/financial-report
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
    "categories": [],
    "overBudgetTasks": [
      {
        "taskId": 10,
        "title": "Mua hoa",
        "allocatedAmount": 1000000,
        "approvedSpent": 1200000
      }
    ]
  }
}
```

