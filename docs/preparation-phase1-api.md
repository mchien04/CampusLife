# Phase 1 - Chuẩn bị Sự kiện (Preparation) - Siết nghiệp vụ & quyền truy cập

## 1. Thêm thành viên vào Task

### 1. Mô tả nghiệp vụ
Thêm một sinh viên vào danh sách member của task chuẩn bị. Sinh viên được thêm phải thuộc ban tổ chức (Organizer) của activity chứa task.

### 2. API Endpoint
- **Method:** POST
- **Path:** /api/preparation/tasks/{taskId}/members/{studentId}
- **Authentication:** Required (ADMIN/MANAGER hoặc Leader của task)

### 3. Request
- **Path Parameters:**
  - taskId: long - ID task
  - studentId: long - ID sinh viên cần thêm
- **Query Parameters:** none
- **Request Body:** none

### 4. Response
- **Success (200):**
```json
{
  "status": true,
  "message": "OK",
  "body": null
}
```
- **Error Responses:**
  - 400: task không có activity, student không thuộc organizer của activity
  - 403: người thực hiện không phải organizer (khi không phải ADMIN/MANAGER)
  - 404: không tìm thấy task hoặc student

## 2. Tạo phiếu tạm ứng (FundAdvance)

### 1. Mô tả nghiệp vụ
Tạo một khoản tạm ứng cho một sinh viên thuộc task tài chính. Sinh viên nhận tạm ứng phải là organizer của activity và là member/leader của task.

### 2. API Endpoint
- **Method:** POST
- **Path:** /api/preparation/tasks/{taskId}/fund-advances
- **Authentication:** Required (ADMIN/MANAGER)

### 3. Request
- **Path Parameters:**
  - taskId: long - ID task
- **Query Parameters:** none
- **Request Body:**
```json
{
  "studentId": "long - ID sinh viên nhận tạm ứng",
  "amount": "string - số tiền > 0 (BigDecimal dạng chuỗi)"
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
    "taskId": 10,
    "studentId": 100,
    "studentName": "Nguyen Van A",
    "amount": 500000,
    "remainingAmount": 500000,
    "status": "HOLDING",
    "createdAt": "2026-03-29T10:00:00"
  }
}
```
- **Error Responses:**
  - 400: task không tài chính, student không thuộc organizer của activity, student không thuộc task
  - 404: không tìm thấy task hoặc student

## 3. Xem danh sách tạm ứng theo Task

### 1. Mô tả nghiệp vụ
Lấy danh sách FundAdvance của một task để theo dõi dòng tiền đang tạm ứng.

### 2. API Endpoint
- **Method:** GET
- **Path:** /api/preparation/tasks/{taskId}/fund-advances
- **Authentication:** Required (ADMIN/MANAGER hoặc Leader của task)

### 3. Request
- **Path Parameters:**
  - taskId: long - ID task
- **Query Parameters:** none
- **Request Body:** none

### 4. Response
- **Success (200):**
```json
{
  "status": true,
  "message": "OK",
  "body": [
    {
      "id": 1,
      "taskId": 10,
      "studentId": 100,
      "studentName": "Nguyen Van A",
      "amount": 500000,
      "remainingAmount": 200000,
      "status": "HOLDING",
      "createdAt": "2026-03-29T10:00:00"
    }
  ]
}
```
- **Error Responses:**
  - 403: không đủ quyền
  - 404: không tìm thấy task

## 4. Tạo khoản chi (Expense) của Task tài chính

### 1. Mô tả nghiệp vụ
Member của task tạo khoản chi kèm chứng từ (evidenceUrl). Người tạo phải thuộc ban tổ chức (Organizer) của activity và khoản chi sẽ vào trạng thái chờ leader duyệt.

### 2. API Endpoint
- **Method:** POST
- **Path:** /api/preparation/tasks/{taskId}/expenses
- **Authentication:** Required (Member của task)

### 3. Request
- **Path Parameters:**
  - taskId: long - ID task
- **Query Parameters:** none
- **Request Body:**
```json
{
  "categoryId": "long - ID hạng mục ngân sách của activity",
  "amount": "string - số tiền > 0 (BigDecimal dạng chuỗi)",
  "description": "string - mô tả (optional)",
  "evidenceUrl": "string - URL chứng từ (optional)"
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
    "taskId": 10,
    "categoryId": 5,
    "categoryName": "Hoa",
    "amount": 150000,
    "description": "Mua hoa",
    "evidenceUrl": "https://...",
    "status": "PENDING_LEADER",
    "createdById": 100,
    "createdByName": "Nguyen Van A",
    "createdAt": "2026-03-29T10:00:00"
  }
}
```
- **Error Responses:**
  - 400: task không tài chính, task không có activity, category không thuộc activity
  - 403: người tạo không thuộc organizer của activity
  - 404: không tìm thấy student hoặc task

