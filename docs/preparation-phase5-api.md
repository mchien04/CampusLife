# Phase 5 - Chuẩn bị Sự kiện (Preparation) - FundAdvance 2 bước + “dứt điểm kỳ trước”

## 1. Trạng thái FundAdvance

### 1. Mô tả nghiệp vụ
FundAdvance được tách rõ 2 bước:
- Leader tạo **yêu cầu ứng**: `REQUESTED`
- Admin/Manager **duyệt**:
  - Approve → `HOLDING` (tiền đang nằm ở người nhận ứng)
  - Reject → `REJECTED`
- Khi chi phí được duyệt cấp cuối, hệ thống trừ dần `remainingAmount` của các khoản `HOLDING` (FIFO). Khi `remainingAmount = 0` thì chuyển `SETTLED`.

### 2. Migration
- File: `docs/migrations/2026_03_29_phase5_fund_advances_request_approve.sql`

## 2. Leader tạo yêu cầu ứng (request)

### 1. Mô tả nghiệp vụ
Leader của task tạo yêu cầu ứng cho một member trong task.
Ràng buộc “dứt điểm kỳ trước”: **không cho tạo yêu cầu ứng mới** nếu sinh viên đó còn khoản ứng `HOLDING` (remainingAmount > 0) trong cùng activity.

### 2. API Endpoint
- **Method:** POST
- **Path:** /api/preparation/tasks/{taskId}/fund-advances
- **Authentication:** Required (Leader của task)

### 3. Request
- **Path Parameters:**
  - taskId: long - ID task
- **Query Parameters:** none
- **Request Body:**
```json
{
  "studentId": "long - ID sinh viên nhận ứng",
  "amount": "string - số tiền > 0"
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
    "requestedById": 200,
    "requestedByName": "Leader B",
    "amount": 500000,
    "remainingAmount": 0,
    "status": "REQUESTED",
    "createdAt": "2026-03-29T10:00:00",
    "decidedAt": null
  }
}
```
- **Error Responses:**
  - 400: task không tài chính / student không thuộc organizer hoặc không thuộc task / còn nợ HOLDING trong activity
  - 403: không đủ quyền leader
  - 404: task hoặc student không tồn tại

## 3. Admin duyệt yêu cầu ứng

### 1. Mô tả nghiệp vụ
Admin/Manager duyệt yêu cầu ứng:
- Approve: chuyển `REQUESTED → HOLDING`, set `remainingAmount = amount`.
- Reject: chuyển `REQUESTED → REJECTED`, `remainingAmount = 0`.

### 2. API Endpoint
- **Method:** PUT
- **Path:** /api/preparation/fund-advances/{fundAdvanceId}/admin-decision
- **Authentication:** Required (ADMIN/MANAGER)

### 3. Request
- **Path Parameters:**
  - fundAdvanceId: long - ID fund advance
- **Query Parameters:** none
- **Request Body:**
```json
{
  "approved": "boolean - true duyệt, false từ chối"
}
```

### 4. Response
- **Success (200):** Trả `FundAdvanceDto`
- **Error Responses:**
  - 400: fund advance không ở trạng thái REQUESTED / còn nợ HOLDING trong activity
  - 404: fund advance không tồn tại

## 4. Report “tiền ngoài ví” (nợ tạm ứng) theo activity/student

### 1. Mô tả nghiệp vụ
Trả về danh sách sinh viên đang giữ tiền ứng chưa quyết toán trong activity (tổng `remainingAmount` của các khoản `HOLDING`).

### 2. API Endpoint
- **Method:** GET
- **Path:** /api/preparation/activities/{activityId}/fund-advance-debts
- **Authentication:** Required (ADMIN/MANAGER)

### 3. Request
- **Path Parameters:**
  - activityId: long - ID activity
- **Query Parameters:**
  - studentId: long - optional, lọc theo sinh viên
- **Request Body:** none

### 4. Response
- **Success (200):**
```json
{
  "status": true,
  "message": "OK",
  "body": [
    { "studentId": 100, "studentName": "Nguyen Van A", "holdingAmount": 300000 }
  ]
}
```
- **Error Responses:**
  - 400: preparation chưa bật
  - 403: không đủ quyền
  - 404: activity/student không tồn tại

