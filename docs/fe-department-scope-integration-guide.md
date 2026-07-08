# FE Department Scope Integration Guide

> Tài liệu này chỉ liệt kê các thay đổi FE cần tích hợp: API, query params, request/response DTO và quy tắc route URL cho notification.

## 1. Response Wrapper Chung

Tất cả API liên quan vẫn trả wrapper:

```json
{
  "status": true,
  "message": "OK",
  "body": {}
}
```

FE đọc dữ liệu ở `body`, không phải `data`.

## 2. Quy tắc theo Role

| Role | FE cần làm |
|------|------------|
| `ADMIN` | Có thể xem toàn trường. Một số API hỗ trợ filter `departmentId`. |
| `MANAGER` | Không cần gửi danh sách khoa trong request. Backend tự lấy scope từ user login. Nếu gửi `departmentId`, ID đó phải thuộc khoa manager được phân công. |
| `STUDENT` | Không bị department scope khi xem public content. Sinh viên tự cập nhật khoa trong profile. |

Khi manager không có quyền khoa hoặc chọn ngoài scope, backend có thể trả `403 Access denied`.

## 3. User Management: Gán khoa cho Manager

Base path: `/api/admin/users`

Chỉ `ADMIN` dùng nhóm API này.

### Tạo Manager

`POST /api/admin/users`

```json
{
  "username": "manager01",
  "email": "manager01@example.com",
  "password": "123456",
  "role": "MANAGER",
  "isActivated": true,
  "departmentIds": [1, 2]
}
```

`departmentIds` bắt buộc khi `role = MANAGER`.

### Cập nhật Manager

`PUT /api/admin/users/{userId}`

```json
{
  "departmentIds": [1]
}
```

`departmentIds` là replace toàn bộ danh sách khoa manager quản lý.

### UserResponse

```json
{
  "id": 10,
  "username": "manager01",
  "email": "manager01@example.com",
  "role": "MANAGER",
  "isActivated": true,
  "departmentIds": [1, 2]
}
```

Không dùng endpoint riêng để gán khoa cho manager.

## 4. Student Account Management

Base path: `/api/admin/students`

`ADMIN` và `MANAGER` đều dùng được.

### Import Excel / Bulk Create

Các luồng này không cần khoa:

- `POST /api/admin/students/upload-excel`
- `POST /api/admin/students/bulk-create`
- `POST /api/admin/students/create-multiple`

Sinh viên sẽ tự chọn khoa khi cập nhật profile sau khi đăng nhập.

### Tạo thủ công một sinh viên

`POST /api/admin/students/create`

```json
{
  "studentCode": "SV001",
  "fullName": "Nguyen Van A",
  "email": "sv001@example.com",
  "departmentId": 1
}
```

`departmentId` là optional. Với `MANAGER`, nếu gửi thì phải thuộc scope manager.

### Cập nhật tài khoản sinh viên

`PUT /api/admin/students/{studentId}/account`

```json
{
  "username": "SV001",
  "email": "sv001@example.com",
  "studentCode": "SV001",
  "fullName": "Nguyen Van A",
  "departmentId": 1
}
```

Tất cả field đều optional. `departmentId` dùng để gán hoặc đổi khoa.

Với `MANAGER`:

- Sinh viên đã có khoa: manager chỉ sửa nếu sinh viên thuộc khoa trong scope.
- Sinh viên chưa có khoa: manager có thể gán khoa trong scope.

### StudentAccountResponse

```json
{
  "userId": 100,
  "studentId": 20,
  "username": "SV001",
  "email": "sv001@example.com",
  "studentCode": "SV001",
  "fullName": "Nguyen Van A",
  "password": "plain-password-when-created",
  "isActivated": true,
  "emailSent": false,
  "lastLogin": null,
  "createdAt": "2026-07-08T04:00:00",
  "departmentId": 1,
  "departmentName": "Khoa CNTT"
}
```

FE nên hiển thị dropdown khoa ở form tạo thủ công và form sửa tài khoản sinh viên. Form import Excel không cần cột khoa.

## 5. Student Profile: Sinh viên tự điền khoa

Base path: `/api/student/profile`

Sinh viên được tạo bằng Excel không cần có khoa ngay. Sau khi đăng nhập, FE cho sinh viên chọn khoa trong màn profile.

### Cập nhật profile hiện tại

`PUT /api/student/profile`

```json
{
  "studentCode": "SV001",
  "fullName": "Nguyen Van A",
  "departmentId": 1,
  "classId": 10,
  "phone": "0900000000",
  "dob": "2004-01-01",
  "avatarUrl": "https://example.com/avatar.png",
  "gender": "MALE"
}
```

Lưu ý: `studentCode` và `fullName` đang bắt buộc ở backend. Nếu FE chỉ cho sinh viên chọn khoa, vẫn cần gửi lại `studentCode` và `fullName` hiện tại trong request.

## 6. Score Management

Base path: `/api/scores`

### Ranking có filter khoa

`GET /api/scores/ranking`

Query params:

| Param | Required | Ghi chú |
|-------|----------|---------|
| `semesterId` | Yes | ID học kỳ |
| `scoreType` | No | `REN_LUYEN`, `CONG_TAC_XA_HOI`, `CHUYEN_DE`; bỏ trống là tổng |
| `departmentId` | No | ADMIN filter theo khoa; MANAGER chỉ được chọn khoa trong scope |
| `classId` | No | Filter lớp |
| `sortOrder` | No | `ASC` hoặc `DESC`, mặc định `DESC` |

Ví dụ:

```http
GET /api/scores/ranking?semesterId=1&scoreType=REN_LUYEN&departmentId=2&sortOrder=DESC
```

Response `body` là map gồm `semesterId`, `semesterName`, `scoreType`, `departmentId`, `classId`, `sortOrder`, `totalStudents`, `rankings`.

### Các endpoint điểm theo sinh viên

| API | FE note |
|-----|---------|
| `GET /api/scores/student/{studentId}/semester/{semesterId}` | MANAGER được xem nếu sinh viên thuộc scope |
| `GET /api/scores/student/{studentId}/semester/{semesterId}/total` | MANAGER được xem nếu sinh viên thuộc scope |
| `GET /api/scores/history/student/{studentId}` | Query `semesterId`, `scoreType`, `page`, `size`, `startDate`, `endDate`, `keyword`; MANAGER scoped theo sinh viên |
| `POST /api/scores/recalculate/student/{studentId}` | MANAGER scoped theo sinh viên |
| `POST /api/scores/recalculate/all` | MANAGER chỉ tính lại sinh viên trong khoa được phân công |
| `POST /api/scores/recalculate/async` | Hiện chưa hỗ trợ MANAGER; backend trả lỗi |

FE không cần tự thêm `departmentId` cho các endpoint theo `studentId`.

## 7. Statistics

Base path: `/api/statistics`

### Đã giới hạn MANAGER theo khoa

| API | Query params FE có thể gửi |
|-----|----------------------------|
| `GET /api/statistics/dashboard` | Không có — MANAGER: metrics scoped theo khoa; STUDENT: metrics cá nhân (đăng ký/tham gia của chính SV) |
| `GET /api/statistics/activities` | `activityType`, `scoreType`, `departmentId`, `startDate`, `endDate` |
| `GET /api/statistics/students` | `departmentId`, `classId`, `semesterId` |
| `GET /api/statistics/scores` | `scoreType`, `semesterId`, `departmentId`, `classId` |
| `GET /api/statistics/scores/breakdown` | `semesterId`, `studentId`, `departmentId` |
| `GET /api/statistics/series` | `seriesId`, `semesterId` — MANAGER scoped theo khoa organizer/target |
| `GET /api/statistics/minigames` | `miniGameId`, `startDate`, `endDate` — MANAGER scoped theo activity organizer |

Với `MANAGER`, FE có thể:

- Không gửi `departmentId` để xem tổng trong các khoa manager được phân công.
- Gửi `departmentId` để lọc xuống một khoa cụ thể trong scope.

Nếu manager gửi `departmentId` ngoài scope, backend sẽ trả lỗi quyền.

### Cần lưu ý hiện tại

| API | Trạng thái hiện tại |
|-----|---------------------|
| ADMIN filter ở `/statistics/activities`, `/statistics/students`, `/statistics/scores`, `/statistics/scores/breakdown` | Đã áp dụng `departmentId`, `classId`, `semesterId`, `activityType`, `scoreType`, `startDate`, `endDate`, `studentId` khi truyền param |
| MANAGER filter ở `/statistics/activities` | Đã áp dụng thêm `activityType`, `scoreType`, date range (ngoài scope khoa) |

Khi không truyền filter, ADMIN vẫn nhận dữ liệu toàn trường (path tối ưu bằng aggregate query).

## 8. Email và Notification Send

Base path: `/api/emails`

### Endpoint

| API | Content type | Ghi chú |
|-----|--------------|---------|
| `POST /api/emails/send` | `multipart/form-data` | Gửi email có thể kèm attachments. Part JSON tên `request`. |
| `POST /api/emails/send-json` | `application/json` | Gửi email không attachments. |
| `POST /api/emails/notifications/send` | `application/json` | Chỉ tạo notification, không gửi email. |
| `GET /api/emails/history?page=0&size=20` | - | MANAGER chỉ thấy lịch sử trong scope. |
| `GET /api/emails/history/{emailId}` | - | Hiện chưa áp department scope cho MANAGER. |
| `POST /api/emails/history/{emailId}/resend` | - | Hiện chưa áp department scope cho MANAGER. |

Không có endpoint `POST /api/admin/notifications/bulk`. Bulk notification dùng `POST /api/emails/notifications/send` với `recipientType = "BULK"`.

### RecipientType

```ts
type RecipientType =
  | "BULK"
  | "ACTIVITY_REGISTRATIONS"
  | "SERIES_REGISTRATIONS"
  | "ALL_STUDENTS"
  | "BY_CLASS"
  | "BY_DEPARTMENT";
```

Quy tắc với `MANAGER`:

| RecipientType | MANAGER |
|---------------|---------|
| `ALL_STUDENTS` | Không được dùng |
| `BY_DEPARTMENT` | `departmentId` bắt buộc và phải thuộc scope |
| `BY_CLASS` | `classId` bắt buộc và lớp phải thuộc scope |
| `BULK` | `recipientIds` phải là user sinh viên thuộc scope |
| `ACTIVITY_REGISTRATIONS` | `activityId` bắt buộc và activity phải thuộc scope organizer |
| `SERIES_REGISTRATIONS` | `seriesId` bắt buộc và series phải thuộc scope |

### SendEmailRequest

```json
{
  "recipientType": "BY_DEPARTMENT",
  "recipientIds": [100, 101],
  "activityId": 1,
  "seriesId": 2,
  "classId": 3,
  "departmentId": 1,
  "subject": "Thông báo",
  "content": "<p>Nội dung</p>",
  "isHtml": true,
  "templateVariables": {
    "customKey": "customValue"
  },
  "createNotification": true,
  "notificationTitle": "Thông báo mới",
  "notificationType": "GENERAL",
  "notificationActionUrl": "/manager/events/1"
}
```

FE chỉ gửi các field target phù hợp với `recipientType`.

### SendNotificationOnlyRequest

```json
{
  "recipientType": "ACTIVITY_REGISTRATIONS",
  "activityId": 1,
  "title": "Nhắc nhở sự kiện",
  "content": "Sự kiện sắp diễn ra",
  "type": "ACTIVITY_REMINDER",
  "actionUrl": "/student/events/1",
  "templateVariables": {
    "activityName": "Ngày hội"
  }
}
```

`NotificationType` hiện có:

```ts
type NotificationType =
  | "ACTIVITY_REGISTRATION"
  | "TASK_ASSIGNMENT"
  | "TASK_SUBMISSION"
  | "TASK_GRADING"
  | "ACTIVITY_REMINDER"
  | "REMINDER_1_DAY"
  | "REMINDER_1_HOUR"
  | "SYSTEM_ANNOUNCEMENT"
  | "PROFILE_UPDATE"
  | "SCORE_UPDATE"
  | "GENERAL"
  | "ARTICLE_PUBLISHED";
```

## 9. Notification URL Contract

Không thêm API lấy URL. FE chịu trách nhiệm tạo URL chính xác và gửi kèm request.

| Luồng | Field FE gửi |
|-------|--------------|
| Email có tạo notification | `notificationActionUrl` trong `SendEmailRequest` |
| Chỉ tạo notification | `actionUrl` trong `SendNotificationOnlyRequest` |

Backend chỉ lưu URL FE gửi. Nếu FE không gửi URL, backend vẫn có thể lưu metadata như `activityId` hoặc `seriesId`, nhưng không tự build route.

### Route registry FE cần quản lý

FE nên có một mapping tập trung để tạo URL theo role và entity:

| Use case | Student URL | Manager/Admin URL |
|----------|-------------|-------------------|
| Activity detail | `/student/events/{activityId}` | `/manager/events/{activityId}` |
| Series detail | `/student/series/{seriesId}` | `/manager/series/{seriesId}` |
| Article detail | `/articles/{articleId}` | `/admin/articles/{articleId}` |
| Task assignment | `/student/tasks/{taskId}` | `/manager/tasks/{taskId}` |
| Score detail | `/student/scores` | `/manager/scores/students/{studentId}` |
| Profile update | `/student/profile` | Không áp dụng |

Click notification nên xử lý theo thứ tự:

1. Nếu response có `actionUrl`, navigate đến `actionUrl`.
2. Nếu không có `actionUrl` nhưng có `activityId`, build route activity theo role hiện tại.
3. Nếu không có `actionUrl` nhưng có `seriesId`, build route series theo role hiện tại.
4. Nếu không đủ dữ liệu, mở trang chi tiết notification.

### NotificationDetailResponse

```json
{
  "id": 1,
  "title": "Nhắc nhở sự kiện",
  "content": "Sự kiện sắp diễn ra",
  "type": "ACTIVITY_REMINDER",
  "status": "UNREAD",
  "actionUrl": "/student/events/1",
  "metadata": {
    "activityId": 1
  },
  "activityId": 1,
  "seriesId": null,
  "createdAt": "2026-07-08T04:00:00",
  "updatedAt": "2026-07-08T04:00:00",
  "readAt": null
}
```

## 10. FE Checklist

- Manager UI không tự suy luận toàn trường; luôn dùng data backend trả về.
- Dropdown khoa cho manager chỉ nên hiển thị khoa được phân công nếu FE có dữ liệu đó; nếu không, có thể gọi API rồi để backend validate.
- Form tạo/sửa manager dùng `departmentIds`.
- Form tạo thủ công/sửa student account dùng `departmentId`.
- Import Excel student không cần field khoa.
- Profile sinh viên dùng `departmentId`; khi update phải gửi kèm `studentCode` và `fullName`.
- Ranking score dùng `departmentId` khi ADMIN muốn lọc hoặc MANAGER muốn lọc một khoa trong scope.
- Statistics manager có thể gửi `departmentId`; `series` và `minigames` đã scoped theo khoa organizer.
- Statistics ADMIN có thể gửi filter (`departmentId`, `classId`, date range, …); backend đã áp dụng khi có param.
- Email/notification manager không hiển thị option `ALL_STUDENTS`.
- Notification URL do FE build và gửi qua `actionUrl` hoặc `notificationActionUrl`.
