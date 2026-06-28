# Sequence Diagram – Notification & Email Module (CampusLife)

> **Hệ thống:** CampusLife (Spring Boot + React)  
> **Module:** Notification & Email (Thông báo & Email)  
> **Ngày cập nhật:** 2025-06-26  
> **Phiên bản:** v2.0

---

## 1. Nhắc nhở (3.3.7) — Reminder / In-app notification

```mermaid
sequenceDiagram
    participant U as Admin/Manager/Student
    participant C as Client
    participant CT as Controller
    participant S as Service
    participant R as Repository
    participant DB as Database
    participant ES as EmailService
    participant FCM as FCMService

    Note over U,CT: Luồng 1a — Admin tạo reminder thủ công
    alt Admin tạo reminder thủ công
        U->>C: Nhập nội dung reminder (title, message, targetType, targetIds, activityId)
        C->>CT: POST /api/admin/reminders
        CT->>S: createReminder(dto)
    else Scheduler tự động trigger
        Note over CT,S: ScheduledTask (cron) trigger
        CT->>S: createReminder(activityId, title, message, targetType, targetIds)
    end

    Note over S,DB: Luồng 1b — Resolve target users
    S->>S: validateInput(dto)
    S->>R: findTargetUsers(targetType, targetIds)
    Note over R: targetType ∈ {ACTIVITY_REGISTRANTS, CLASS, DEPARTMENT, ALL}
    R->>DB: SELECT u.id FROM users u JOIN ... WHERE condition = ?
    DB-->>R: List<userId>
    R-->>S: List<User>

    Note over S,DB: Luồng 1c — Tạo và lưu Notification
    S->>S: for each user<br/>create Notification(<br/>userId, title, message,<br/>type=REMINDER,<br/>activityId, isRead=false<br/>)
    S->>R: saveAll(notifications)
    R->>DB: INSERT INTO notifications ...
    DB-->>R: List<Notification> (saved)
    R-->>S: return saved list

    Note over S,FCM: Luồng 1d — Gửi FCM Push (nếu có device token)
    S->>R: findDeviceTokensByUserIds(userIds)
    R->>DB: SELECT token FROM device_tokens WHERE user_id IN (...)
    DB-->>R: List<(userId, token, platform)>
    R-->>S: return tokens

    loop For each (userId, token)
        S->>FCM: sendPush(token, title, message, payload={activityId})
        FCM-->>S: success / fail
    end

    opt Nếu cấu hình gửi kèm Email
        Note over S,ES: Luồng 1e — Gửi email kèm theo
        S->>R: findEmailsByUserIds(userIds)
        R->>DB: SELECT email FROM users WHERE id IN (...)
        DB-->>R: List<email>
        R-->>S: return emails
        S->>ES: sendBulkEmails(emails, title, message, templateId)
        ES-->>S: success / fail
    end

    S-->>CT: return ReminderResult(count, fcmCount, emailCount)
    CT-->>C: 200 OK + JSON result
    C-->>U: Hiển thị "Đã tạo N nhắc nhở"
```

### Mô tả chi tiết

| Bước | Thành phần | Hành động |
|------|------------|-----------|
| 1 | Admin/Manager | Nhập nội dung reminder hoặc hệ thống Scheduler tự động kích hoạt |
| 2 | Client | POST đến `/api/admin/reminders` |
| 3 | Controller | Validate đầu vào và chuyển đến Service |
| 4 | Service | Gọi Repository để tìm target users theo tiêu chí (activity registration, class, department) |
| 5 | Repository | Query Database lấy danh sách userId |
| 6 | Service | Tạo Notification entity cho mỗi user với `type=REMINDER`, `isRead=false` |
| 7 | Service | Gọi `saveAll()` để batch insert vào Database |
| 8 | Service | Gọi FCMService gửi push notification cho các user có device token |
| 9 | Service | (Optional) Gọi EmailService gửi email nếu cấu hình bật |
| 10 | Controller | Trả về kết quả tổng hợp (số lượng notification đã tạo, FCM đã gửi, email đã gửi) |

---

## 2. Xem & đánh dấu thông báo đã đọc (M.48)

```mermaid
sequenceDiagram
    participant U as Admin/Manager/Student
    participant C as Client
    participant CT as Controller
    participant S as Service
    participant R as Repository
    participant DB as Database

    Note over U,CT: Luồng 2a — Xem danh sách thông báo
    U->>C: Mở màn hình Thông báo
    C->>CT: GET /api/notifications?page=0&size=20
    CT->>S: getNotifications(authUserId, pageable)
    S->>S: extractUserIdFromJWT(token)
    S->>R: findByUserIdOrderByCreatedAtDesc(userId, pageable)
    R->>DB: SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?
    DB-->>R: Page<Notification>
    R-->>S: return Page
    S->>S: map to NotificationDTO(title, message, type, isRead, createdAt, activityId)
    S-->>CT: return Page<NotificationDTO>
    CT-->>C: 200 OK + JSON
    C-->>U: Hiển thị danh sách thông báo (mới nhất trước, badge số chưa đọc)

    Note over U,CT: Luồng 2b — Đánh dấu 1 thông báo đã đọc
    U->>C: Click "Đánh dấu đã đọc" trên 1 notification
    C->>CT: PUT /api/notifications/{id}/read
    CT->>S: markAsRead(notificationId, authUserId)
    S->>R: findById(notificationId)
    R->>DB: SELECT * FROM notifications WHERE id = ?
    DB-->>R: Notification
    R-->>S: Optional<Notification>
    S->>S: validate notification.userId == authUserId (403 nếu không khớp)
    S->>S: notification.setIsRead(true)
    S->>R: save(notification)
    R->>DB: UPDATE notifications SET is_read = true WHERE id = ?
    DB-->>R: updated row
    R-->>S: Notification (updated)
    S-->>CT: return success
    CT-->>C: 200 OK
    C-->>U: Cập nhật UI (badge -1, gạch chân/đổi màu)

    Note over U,CT: Luồng 2c — Đánh dấu tất cả đã đọc
    U->>C: Click "Đánh dấu tất cả đã đọc"
    C->>CT: PUT /api/notifications/read-all
    CT->>S: markAllAsRead(authUserId)
    S->>R: updateIsReadByUserId(userId, true)
    R->>DB: UPDATE notifications SET is_read = true WHERE user_id = ? AND is_read = false
    DB-->>R: updatedCount
    R-->>S: return count
    S-->>CT: return {updatedCount: N}
    CT-->>C: 200 OK
    C-->>U: Ẩn badge, đánh dấu tất cả item đã đọc
```

### Mô tả chi tiết

| Bước | API | Thành phần | Hành động |
|------|-----|------------|-----------|
| 1 | GET /api/notifications | Client | Gửi request lấy danh sách với pagination |
| 2 | GET /api/notifications | Service | Extract userId từ JWT, gọi Repository query theo `userId` sort `createdAt` DESC |
| 3 | GET /api/notifications | Repository | Query DB với LIMIT/OFFSET, trả về Page |
| 4 | PUT /api/notifications/{id}/read | Service | Validate notification thuộc về user hiện tại (chống unauthorized) |
| 5 | PUT /api/notifications/{id}/read | Service | Set `isRead=true` và save vào DB |
| 6 | PUT /api/notifications/read-all | Service | Batch update tất cả notification của user thành `isRead=true` |

---

## 3. Gửi thông báo hàng loạt (M.49) — POST /api/admin/notifications/bulk

```mermaid
sequenceDiagram
    participant U as Admin/Manager/Student
    participant C as Client
    participant CT as Controller
    participant S as Service
    participant R as Repository
    participant DB as Database
    participant ES as EmailService
    participant FCM as FCMService

    Note over U,DB: Luồng 3a — Admin nhập nội dung và chọn target
    U->>C: Chọn target (ALL_STUDENTS / BY_DEPARTMENT / BY_CLASS / BY_ACTIVITY_REGISTRANTS)<br/>Nhập title, message, type<br/>Toggle gửi FCM / Email
    C->>CT: POST /api/admin/notifications/bulk (BulkNotificationDTO)
    CT->>S: sendBulkNotification(dto)
    S->>S: validateTargets(dto.targets)
    alt Targets không hợp lệ
        S-->>CT: throw ValidationException
        CT-->>C: 400 Bad Request
        C-->>U: Hiển thị lỗi validate
    end

    Note over S,DB: Luồng 3b — Resolve target users
    S->>R: resolveTargetUsers(dto.targetType, dto.targetIds)
    Note over R: Ví dụ:<br/>BY_DEPARTMENT → JOIN users u JOIN departments d<br/>BY_CLASS → JOIN users u JOIN classes c<br/>BY_ACTIVITY_REGISTRANTS → JOIN activity_registrations ar
    R->>DB: SELECT DISTINCT u.id FROM users u ... WHERE condition = ?
    DB-->>R: List<userId>
    R-->>S: List<User>

    Note over S,DB: Luồng 3c — Batch tạo Notification
    S->>S: List<Notification> notifications = new ArrayList<>()
    loop For each user (batch size = 100)
        S->>S: create Notification(userId, title, message, type, isRead=false)
        S->>R: saveAll(batch)
        R->>DB: INSERT INTO notifications ...
        DB-->>R: saved batch
        R-->>S: return
    end

    Note over S,FCM: Luồng 3d — Gửi FCM push async
    S->>R: findDeviceTokensByUserIds(userIds)
    R->>DB: SELECT user_id, token, platform FROM device_tokens WHERE user_id IN (...)
    DB-->>R: List<DeviceToken>
    R-->>S: return tokens

    par Async FCM Push
        loop For each token
            S->>FCM: sendPushAsync(token, title, message, payload)
            FCM-->>S: success / fail
        end
    and Async Email
        opt Nếu admin chọn gửi email
            S->>R: findEmailsByUserIds(userIds)
            R->>DB: SELECT email FROM users WHERE id IN (...)
            DB-->>R: List<email>
            R-->>S: return emails
            S->>ES: sendBulkEmailsAsync(emails, subject, body, templateId)
            ES-->>S: success / fail
        end
    end

    S->>R: saveBulkNotificationLog(adminId, targetType, title, message, userCount, sentAt)
    R->>DB: INSERT INTO notification_logs ...
    DB-->>R: saved

    S-->>CT: return BulkResult {sentCount: N, fcmCount: M, emailCount: K}
    CT-->>C: 200 OK + JSON result
    C-->>U: Hiển thị "Đã gửi N thông báo đến M người"
```

### Mô tả chi tiết

| Bước | Thành phần | Hành động |
|------|------------|-----------|
| 1 | Admin | Chọn target audience và nhập nội dung thông báo |
| 2 | Controller | Validate DTO và chuyển đến Service |
| 3 | Service | Resolve target users: query DB với JOIN phù hợp (department, class, activity registrations) |
| 4 | Service | Loop/batch tạo Notification entity cho từng user và `saveAll()` vào DB |
| 5 | Service | Query DB lấy device tokens của target users |
| 6 | Service | **Parallel** gọi FCMService gửi push async + (optional) EmailService gửi email async |
| 7 | Service | Lưu log gửi hàng loạt vào `notification_logs` |
| 8 | Controller | Trả về tổng số đã gửi (notification count, FCM count, email count) |

---

## 4. Gửi email (M.50) — POST /api/admin/emails/send

```mermaid
sequenceDiagram
    participant U as Admin/Manager/Student
    participant C as Client
    participant CT as Controller
    participant S as Service
    participant R as Repository
    participant DB as Database
    participant ES as EmailService

    Note over U,ES: Luồng 4a — Admin nhập nội dung email
    U->>C: Nhập toEmails[], subject, body, templateId, attachments<br/>Chọn chế độ: Individual hoặc BCC
    C->>CT: POST /api/admin/emails/send (EmailSendDTO)
    CT->>S: sendEmail(dto)
    S->>S: validateEmails(dto.toEmails)
    alt Email không hợp lệ
        S-->>CT: throw ValidationException
        CT-->>C: 400 Bad Request
        C-->>U: Hiển thị lỗi email
    end

    S->>S: loadTemplate(dto.templateId) nếu có

    alt Chế độ Individual (gửi từng người)
        Note over S,ES: Luồng 4b — Gửi riêng lẻ từng email
        loop For each toEmail in dto.toEmails
            S->>ES: send(email, subject, renderedBody, attachments)
            Note over ES: JavaMailSender / SendGrid / AWS SES<br/>sendMimeMessage
            ES-->>S: success / fail
            S->>R: saveEmailLog(to=email, subject, status=SENT/FAILED, sentAt=now, errorMsg)
            R->>DB: INSERT INTO email_logs ...
            DB-->>R: saved
        end
    else Chế độ BCC (gửi 1 lần, nhiều người nhận BCC)
        Note over S,ES: Luồng 4c — Gửi BCC bulk
        S->>ES: sendBCC(dto.toEmails, subject, renderedBody, attachments)
        Note over ES: Tạo 1 message với To=admin@campus.edu<br/>BCC = [email1, email2, ...]
        ES-->>S: success / fail
        S->>R: saveEmailLog(to="BCC_LIST", subject, status, sentAt, recipientCount)
        R->>DB: INSERT INTO email_logs ...
        DB-->>R: saved
    end

    S-->>CT: return EmailSendResult {total, successCount, failCount, logs}
    CT-->>C: 200 OK + JSON result
    C-->>U: Hiển thị "Gửi thành công X/Y email"
```

### Mô tả chi tiết

| Bước | Thành phần | Hành động |
|------|------------|-----------|
| 1 | Admin | Nhập danh sách email, subject, body, chọn template, upload attachments |
| 2 | Service | Validate định dạng email (regex), kiểm tra templateId |
| 3 | Service | Render template (nếu dùng template) thay thế các biến `{{name}}`, `{{activity}}`... |
| 4 | Service | **Alt:** Gửi individual (loop từng email) hoặc BCC (1 message nhiều BCC) |
| 5 | EmailService | Gọi provider gửi email (JavaMailSender / SendGrid / AWS SES) |
| 6 | Service | Lưu `EmailLog` vào DB với trạng thái SENT/FAILED cho từng lần gửi |
| 7 | Controller | Trả về tổng kết số lượng gửi thành công / thất bại + danh sách log |

---

## 5. Đăng ký device token (FCM push) (M.51) — POST /api/device-tokens

```mermaid
sequenceDiagram
    participant U as Admin/Manager/Student
    participant C as Client
    participant CT as Controller
    participant S as Service
    participant R as Repository
    participant DB as Database
    participant FCM as FCMService

    Note over U,FCM: Luồng 5a — Client lấy FCM token sau đăng nhập
    U->>C: Đăng nhập trên Mobile / Web
    C->>C: Firebase SDK initialize
    C->>FCM: getToken() (hoặc onTokenRefresh)
    FCM-->>C: return FCM token string
    C->>C: Detect platform = ANDROID / IOS / WEB

    Note over C,DB: Luồng 5b — Gửi token lên server đăng ký
    C->>CT: POST /api/device-tokens<br/>{userId, token, platform}
    CT->>S: registerDeviceToken(userId, token, platform)
    S->>R: findByToken(token)
    R->>DB: SELECT * FROM device_tokens WHERE token = ?
    DB-->>R: Optional<DeviceToken>

    alt Token chưa tồn tại trong DB
        R-->>S: Optional.empty
        S->>S: create DeviceToken(<br/>userId, token, platform,<br/>createdAt=now, updatedAt=now<br/>)
        S->>R: save(deviceToken)
        R->>DB: INSERT INTO device_tokens ...
        DB-->>R: DeviceToken (saved)
        R-->>S: return entity
        S-->>CT: return {success: true, action: "CREATED"}
    else Token đã tồn tại
        R-->>S: Optional<DeviceToken>
        S->>S: Nếu userId khác hoặc platform khác<br/>→ cập nhật lại thông tin<br/>(tránh duplicate token giữa các user)
        S->>R: save(existingToken)
        R->>DB: UPDATE device_tokens SET user_id = ?, platform = ?, updated_at = now WHERE token = ?
        DB-->>R: updated row
        R-->>S: DeviceToken (updated)
        S-->>CT: return {success: true, action: "UPDATED"}
    end

    CT-->>C: 200 OK
    C-->>U: Device token đã đăng ký (ẩn dưới nền, user không cần thao tác)
```

### Mô tả chi tiết

| Bước | Thành phần | Hành động |
|------|------------|-----------|
| 1 | Client (React/Web/Mobile) | Sau đăng nhập, gọi Firebase SDK `getToken()` để lấy FCM token |
| 2 | Client | POST lên server kèm `userId`, `token`, `platform` (ANDROID/IOS/WEB) |
| 3 | Service | Kiểm tra token đã tồn tại trong `device_tokens` chưa |
| 4 | Service | **Nếu chưa:** tạo mới `DeviceToken` và INSERT vào DB |
| 5 | Service | **Nếu đã tồn tại:** update `userId` và `platform` (xử lý trường hợp user đăng nhập lại trên thiết bị khác hoặc token được reuse) |
| 6 | Controller | Trả về 200 OK với flag `action: CREATED` hoặc `UPDATED` |

---

## Tóm tắt Thành phần & Chức năng

### Thành phần hệ thống

| Thành phần | Vai trò | Công nghệ tham khảo |
|------------|---------|-------------------|
| **Admin/Manager/Student** | Người dùng tương tác với hệ thống; Admin/Manager có quyền gửi thông báo/email hàng loạt | React UI |
| **Client** | Frontend React (Web) hoặc Mobile App (Android/iOS) gọi REST API, tích hợp Firebase SDK để lấy FCM token | React, Firebase Cloud Messaging SDK |
| **Controller** | Spring Boot REST Controllers tiếp nhận request, validate JWT, routing đến Service | `@RestController` (NotificationController, EmailController, DeviceTokenController) |
| **Service** | Business Logic Layer: tạo notification, resolve targets, batch insert, gọi FCM & Email service, xử lý scheduled tasks | `@Service` (NotificationService, EmailService, DeviceTokenService, ScheduledReminderService) |
| **Repository** | Data Access Layer dùng Spring Data JPA để query/insert/update | `JpaRepository` / `@Repository` (NotificationRepository, UserRepository, DeviceTokenRepository, EmailLogRepository) |
| **Database** | Lưu trữ persistent data: `notifications`, `device_tokens`, `email_logs`, `notification_logs`, `users` | PostgreSQL / MySQL |
| **EmailService** | Adapter gửi email qua các provider: JavaMailSender (SMTP), SendGrid API, hoặc AWS SES | `JavaMailSender`, SendGrid SDK, AWS SES SDK |
| **FCMService** | Adapter gửi push notification qua Firebase Cloud Messaging HTTP v1 API | `FirebaseMessaging` (Firebase Admin SDK) |

### Chức năng chính của module

| STT | Chức năng | Mã chức năng | Endpoint chính | Ghi chú |
|-----|-----------|-------------|----------------|---------|
| 1 | **Nhắc nhở (Reminder)** | 3.3.7 | `POST /api/admin/reminders` | Scheduler tự động hoặc Admin tạo thủ công; tự động resolve targets |
| 2 | **Xem danh sách thông báo** | M.48 | `GET /api/notifications` | Pagination, sort mới nhất trước; lọc theo `userId` từ JWT |
| 3 | **Đánh dấu đã đọc (1)** | M.48 | `PUT /api/notifications/{id}/read` | Validate ownership trước khi update `isRead=true` |
| 4 | **Đánh dấu tất cả đã đọc** | M.48 | `PUT /api/notifications/read-all` | Batch update `isRead=true` cho toàn bộ notification của user |
| 5 | **Gửi thông báo hàng loạt** | M.49 | `POST /api/admin/notifications/bulk` | Target by department/class/activity registrants; batch insert + async FCM/Email |
| 6 | **Gửi email** | M.50 | `POST /api/admin/emails/send` | Hỗ trợ Individual (loop) hoặc BCC; lưu `EmailLog` sau mỗi lần gửi |
| 7 | **Đăng ký FCM device token** | M.51 | `POST /api/device-tokens` | Client gửi token sau login; server kiểm tra duplicate và upsert |

---

*End of document — Notification & Email Sequence Diagrams v2.0*
