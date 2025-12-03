# Sequence Diagram - Chức năng Phê duyệt Đăng ký Tham gia

## Mô tả
Sequence diagram mô tả luồng xử lý phê duyệt đăng ký tham gia hoạt động (ActivityRegistration) trong hệ thống CampusLife. Chức năng này cho phép Admin/Manager phê duyệt hoặc từ chối đăng ký của sinh viên.

## Sequence Diagram

### Phê duyệt Đăng ký Tham gia (Approve Registration)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant RegistrationController as ActivityRegistrationController
    participant RegistrationService as ActivityRegistrationServiceImpl
    participant Repository as Repository
    participant Database as Database
    participant NotificationService as NotificationService

    Admin->>Client: Chọn đăng ký và phê duyệt/từ chối<br/>(status: APPROVED hoặc REJECTED)
    Client->>RegistrationController: PUT /api/registrations/{registrationId}/status<br/>(status)
    
    RegistrationController->>RegistrationService: updateRegistrationStatus(registrationId, status)
    
    Note over RegistrationService: Kiểm tra registration tồn tại
    RegistrationService->>Repository: findById(registrationId)
    Repository->>Database: SELECT * FROM activity_registrations<br/>WHERE id = ?
    Database-->>Repository: ActivityRegistration
    Repository-->>RegistrationService: ActivityRegistration
    
    Note over RegistrationService: Cập nhật trạng thái
    RegistrationService->>RegistrationService: registration.setStatus(newStatus)
    RegistrationService->>Repository: save(registration)
    Repository->>Database: UPDATE activity_registrations<br/>SET status = ?<br/>WHERE id = ?
    Database-->>Repository: ActivityRegistration updated
    Repository-->>RegistrationService: ActivityRegistration
    
    alt Status = APPROVED
        Note over RegistrationService: Kiểm tra participation đã tồn tại
        RegistrationService->>Repository: existsByRegistration(registration)
        Repository->>Database: SELECT COUNT(*) FROM activity_participations<br/>WHERE registration_id = ?
        Database-->>Repository: boolean
        Repository-->>RegistrationService: exists
        
        alt Participation chưa tồn tại
            Note over RegistrationService: Tạo participation mới
            RegistrationService->>RegistrationService: create ActivityParticipation<br/>(registration, REGISTERED, points = 0)
            RegistrationService->>Repository: save(participation)
            Repository->>Database: INSERT INTO activity_participations<br/>(registration_id, participation_type,<br/>points_earned, date)
            Database-->>Repository: ActivityParticipation saved
            Repository-->>RegistrationService: ActivityParticipation
        end
    end
    
    Note over RegistrationService: Gửi thông báo cho sinh viên
    RegistrationService->>NotificationService: sendNotification(userId, title, content, metadata)
    NotificationService->>Repository: save(notification)
    Repository->>Database: INSERT INTO notifications<br/>(user_id, title, content, metadata, read)
    Database-->>Repository: Notification saved
    Repository-->>NotificationService: Notification
    NotificationService-->>RegistrationService: Notification sent
    
    RegistrationService-->>RegistrationController: Response(success, ActivityRegistrationResponse)
    RegistrationController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo phê duyệt thành công
```

## Ghi chú

1. **Quyền truy cập**: Chỉ Admin và Manager mới có quyền phê duyệt/từ chối đăng ký.

2. **Trạng thái đăng ký**:
   - `PENDING`: Đang chờ phê duyệt
   - `APPROVED`: Đã được phê duyệt
   - `REJECTED`: Đã bị từ chối
   - `CANCELLED`: Đã hủy
   - `ATTENDED`: Đã tham gia

3. **Tự động tạo Participation**: 
   - Khi phê duyệt (APPROVED), hệ thống tự động tạo ActivityParticipation với:
     - `participationType = REGISTERED`
     - `pointsEarned = 0`
   - Chỉ tạo nếu chưa có participation

4. **Thông báo**: 
   - Gửi thông báo cho sinh viên khi status được cập nhật thành APPROVED hoặc REJECTED
   - Thông báo bao gồm tên hoạt động và trạng thái mới

5. **Activity requiresApproval**: 
   - Nếu activity có `requiresApproval = false`, đăng ký sẽ tự động được APPROVED khi sinh viên đăng ký
   - Nếu `requiresApproval = true`, đăng ký sẽ ở trạng thái PENDING và cần Admin/Manager phê duyệt

