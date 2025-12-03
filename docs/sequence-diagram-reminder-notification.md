# Sequence Diagram - Chức năng Thông báo Nhắc nhở

## Mô tả
Sequence diagram mô tả luồng xử lý gửi thông báo nhắc nhở cho các sự kiện sắp diễn ra trong hệ thống CampusLife. Hệ thống tự động gửi thông báo nhắc nhở 1 ngày và 1 giờ trước khi sự kiện diễn ra.

## Sequence Diagram

### Gửi Thông báo Nhắc nhở (Send Reminder Notifications)

```mermaid
sequenceDiagram
    participant Scheduler as ReminderScheduler
    participant ReminderService as ReminderServiceImpl
    participant Repository as Repository
    participant Database as Database

    Note over Scheduler: Scheduled task chạy mỗi giờ<br/>(@Scheduled cron = "0 0 * * * ?")
    Scheduler->>ReminderService: sendReminderNotifications()
    
    Note over ReminderService: Tính toán thời gian và tìm registrations<br/>cần nhắc nhở (1 ngày và 1 giờ)
    ReminderService->>ReminderService: now = LocalDateTime.now()<br/>oneDayLater = now + 1 day<br/>oneHourLater = now + 1 hour
    
    ReminderService->>Repository: findRegistrationsFor1DayReminder(...)<br/>findRegistrationsFor1HourReminder(...)
    Repository->>Database: SELECT ar.* FROM activity_registrations ar<br/>JOIN activities a ON ar.activity_id = a.id<br/>WHERE ar.status = 'APPROVED'<br/>AND a.start_date BETWEEN ? AND ?<br/>AND a.is_deleted = false AND a.is_draft = false
    Database-->>Repository: List<ActivityRegistration>
    Repository-->>ReminderService: List<ActivityRegistration> (1-day và 1-hour)
    
    Note over ReminderService: Gửi thông báo nhắc nhở<br/>(1 ngày và 1 giờ)
    loop Cho mỗi registration
        ReminderService->>ReminderService: Kiểm tra đã gửi reminder chưa<br/>(hasReminderBeenSent)
        ReminderService->>Repository: findByUserIdAndTypeAndIsDeletedFalseOrderByCreatedAtDesc(...)
        Repository->>Database: SELECT * FROM notifications<br/>WHERE user_id = ? AND type = ?<br/>AND is_deleted = false<br/>ORDER BY created_at DESC
        Database-->>Repository: List<Notification>
        Repository-->>ReminderService: List<Notification>
        
        alt Chưa gửi reminder
            ReminderService->>ReminderService: sendReminderNotification<br/>(REMINDER_1_DAY hoặc REMINDER_1_HOUR)
            ReminderService->>ReminderService: create Notification<br/>(title, content, type, metadata)
            ReminderService->>Repository: save(notification)
            Repository->>Database: INSERT INTO notifications<br/>(user_id, title, content, type,<br/>action_url, metadata, status, created_at)<br/>VALUES (?, ?, ?, ?, ?, ?, 'UNREAD', ?)
            Database-->>Repository: Notification saved
            Repository-->>ReminderService: Notification
        end
    end
    
    ReminderService-->>Scheduler: Response(success, {oneDayCount, oneHourCount})
    Note over Scheduler: Log kết quả
```

## Ghi chú

1. **Scheduled Task**: 
   - Tự động chạy mỗi giờ (cron = "0 0 * * * ?")
   - Không có API endpoint để gọi thủ công (chỉ chạy tự động)

2. **Loại nhắc nhở**:
   - **Nhắc nhở 1 ngày**: Gửi cho các sự kiện sẽ diễn ra trong vòng 24 giờ tới
   - **Nhắc nhở 1 giờ**: Gửi cho các sự kiện sẽ diễn ra trong vòng 1 giờ tới

3. **Điều kiện gửi**:
   - Registration phải có status = `APPROVED`
   - Activity không được là draft (isDraft = false)
   - Activity không bị xóa (isDeleted = false)
   - Chưa gửi reminder cho activity đó (kiểm tra qua metadata)

4. **Kiểm tra trùng lặp**:
   - Kiểm tra xem đã gửi reminder cho user và activity đó chưa
   - Dựa vào metadata của notification (activityId và reminderType)
   - Tránh gửi nhiều lần cùng một reminder

5. **Nội dung thông báo**:
   - **Title**: "Nhắc nhở sự kiện"
   - **Content**: "Sự kiện \"{activityName}\" sẽ diễn ra sau {timeText}"
   - **Type**: `REMINDER_1_DAY` hoặc `REMINDER_1_HOUR`
   - **Metadata**: activityId, activityName, reminderType, registrationId

6. **Action URL**: 
   - Link đến trang chi tiết activity: `/activities/{activityId}`

7. **Trạng thái notification**: 
   - Status = `UNREAD` khi tạo mới

