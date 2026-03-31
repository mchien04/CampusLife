# Phase 3 - Chuẩn bị Sự kiện (Preparation) - Task member/leader + workflow + workload

## 1. Role member trong Task

### 1. Mô tả nghiệp vụ
- Một task có nhiều người tham gia (member).
- Trong những người tham gia có thể gán 1 hoặc nhiều leader.
- Task tài chính bắt buộc phải có ít nhất 1 leader.

### 2. Migration
- File: `docs/migrations/2026_03_29_phase3_task_member_role.sql`

## 2. Xem danh sách member theo Task

### 1. Mô tả nghiệp vụ
Trả về danh sách member của task kèm role `LEADER/MEMBER`.

### 2. API Endpoint
- **Method:** GET
- **Path:** /api/preparation/tasks/{taskId}/members
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
    { "studentId": 100, "studentName": "Nguyen Van A", "role": "LEADER" },
    { "studentId": 101, "studentName": "Nguyen Van B", "role": "MEMBER" }
  ]
}
```
- **Error Responses:**
  - 403: không đủ quyền
  - 404: task không tồn tại

## 3. Xóa member khỏi Task

### 1. Mô tả nghiệp vụ
Xóa một member khỏi task. Nếu member là leader của task tài chính thì phải đảm bảo sau khi xóa vẫn còn ít nhất 1 leader.

### 2. API Endpoint
- **Method:** DELETE
- **Path:** /api/preparation/tasks/{taskId}/members/{studentId}
- **Authentication:** Required (ADMIN/MANAGER hoặc Leader của task)

### 3. Request
- **Path Parameters:**
  - taskId: long - ID task
  - studentId: long - ID sinh viên
- **Query Parameters:** none
- **Request Body:** none

### 4. Response
- **Success (200):**
```json
{ "status": true, "message": "OK", "body": null }
```
- **Error Responses:**
  - 400: task tài chính phải có ít nhất 1 leader
  - 404: task/member mapping không tồn tại

## 4. Gán / Thu hồi leader

### 1. Mô tả nghiệp vụ
- Gán leader: chuyển role member thành `LEADER` (nếu chưa là member thì tự tạo mapping).
- Thu hồi leader: chuyển role leader về `MEMBER`. Nếu task tài chính và chỉ còn 1 leader thì không cho thu hồi.

### 2. API Endpoint
- **Method:** POST
- **Path:** /api/preparation/tasks/{taskId}/leaders/{studentId}
- **Authentication:** Required (ADMIN/MANAGER hoặc Leader của task)

### 3. Request
- **Path Parameters:** taskId (long), studentId (long)
- **Query Parameters:** none
- **Request Body:** none

### 4. Response
- **Success (200):**
```json
{ "status": true, "message": "OK", "body": null }
```
- **Error Responses:**
  - 400: student không thuộc organizer của activity
  - 404: task hoặc student không tồn tại

---

### 2. API Endpoint
- **Method:** DELETE
- **Path:** /api/preparation/tasks/{taskId}/leaders/{studentId}
- **Authentication:** Required (ADMIN/MANAGER hoặc Leader của task)

### 3. Request
- **Path Parameters:** taskId (long), studentId (long)
- **Query Parameters:** none
- **Request Body:** none

### 4. Response
- **Success (200):**
```json
{ "status": true, "message": "OK", "body": null }
```
- **Error Responses:**
  - 400: task tài chính phải có ít nhất 1 leader
  - 404: task/member mapping không tồn tại

## 5. Workflow trạng thái Task

### 1. Mô tả nghiệp vụ
Luồng chuẩn:
- `PENDING` → `ACCEPTED` (leader nhận task)
- `ACCEPTED` → `COMPLETION_REQUESTED` (leader gửi yêu cầu hoàn thành)
- `COMPLETION_REQUESTED` → `COMPLETED` (admin duyệt) hoặc quay lại `ACCEPTED` (admin từ chối)

### 2. API Endpoint
- **Method:** PUT
- **Path:** /api/preparation/tasks/{taskId}/accept
- **Authentication:** Required (Leader của task)

### 3. Request
- **Path Parameters:** taskId (long)
- **Query Parameters:** none
- **Request Body:** none

### 4. Response
- **Success (200):** Trả `PreparationTaskDto`
- **Error Responses:**
  - 400: task không ở trạng thái PENDING
  - 403: không đủ quyền leader
  - 404: task không tồn tại

### 5. Migration (DB enum)
- File: `docs/migrations/2026_03_31_phase3_preparation_task_status_add_completion_requested.sql`

---

### 2. API Endpoint
- **Method:** PUT
- **Path:** /api/preparation/tasks/{taskId}/request-complete
- **Authentication:** Required (Leader của task)

### 3. Request
- **Path Parameters:** taskId (long)
- **Query Parameters:** none
- **Request Body:** none

### 4. Response
- **Success (200):** Trả `PreparationTaskDto`
- **Error Responses:**
  - 400: task phải ACCEPTED trước khi request complete
  - 403/404: tương tự

---

### 2. API Endpoint
- **Method:** PUT
- **Path:** /api/preparation/tasks/{taskId}/complete-decision
- **Authentication:** Required (ADMIN/MANAGER)

### 3. Request
- **Path Parameters:** taskId (long)
- **Query Parameters:** none
- **Request Body:**
```json
{ "approved": "boolean - true duyệt, false từ chối" }
```

### 4. Response
- **Success (200):** Trả `PreparationTaskDto`
- **Error Responses:**
  - 400: task không ở trạng thái COMPLETION_REQUESTED
  - 404: task không tồn tại

## 6. Cảnh báo workload theo Activity

### 1. Mô tả nghiệp vụ
Trả về danh sách cảnh báo:
- `OVERLOADED`: sinh viên tham gia > 3 task trong activity.
- `UNASSIGNED`: sinh viên thuộc BTC nhưng chưa tham gia task nào trong activity.

### 2. API Endpoint
- **Method:** GET
- **Path:** /api/preparation/activities/{activityId}/workload-warnings
- **Authentication:** Required (ADMIN/MANAGER hoặc Organizer)

### 3. Request
- **Path Parameters:** activityId (long)
- **Query Parameters:** none
- **Request Body:** none

### 4. Response
- **Success (200):**
```json
{
  "status": true,
  "message": "OK",
  "body": [
    { "studentId": 100, "studentName": "Nguyen Van A", "taskCount": 4, "type": "OVERLOADED" },
    { "studentId": 200, "studentName": "Nguyen Van C", "taskCount": 0, "type": "UNASSIGNED" }
  ]
}
```
- **Error Responses:**
  - 400: preparation chưa bật
  - 403: không đủ quyền
  - 404: activity không tồn tại
