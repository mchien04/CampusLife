# Sequence Diagram - Chức năng Đăng ký và Hủy Đăng ký Tham gia Hoạt động

## Mô tả
Sequence diagram mô tả luồng xử lý đăng ký và hủy đăng ký tham gia hoạt động trong hệ thống CampusLife. Bao gồm các chức năng đăng ký tham gia và hủy đăng ký của sinh viên.

## Sequence Diagrams

### 1. Đăng ký Tham gia Hoạt động (Register for Activity)

```mermaid
sequenceDiagram
    participant Student as Student
    participant Client as Client/Frontend
    participant RegistrationController as ActivityRegistrationController
    participant RegistrationService as ActivityRegistrationServiceImpl
    participant Repository as Repository
    participant Database as Database
    participant NotificationService as NotificationService

    Student->>Client: Chọn hoạt động và đăng ký
    Client->>RegistrationController: POST /api/registrations<br/>(ActivityRegistrationRequest: activityId)
    
    Note over RegistrationController: Lấy studentId từ authentication
    RegistrationController->>RegistrationController: getStudentIdFromAuth(authentication)
    RegistrationController->>Repository: getStudentIdByUsername(username)
    Repository->>Database: SELECT id FROM students<br/>WHERE user_id = (SELECT id FROM users<br/>WHERE username = ?)
    Database-->>Repository: studentId
    Repository-->>RegistrationController: studentId
    
    RegistrationController->>RegistrationService: registerForActivity(request, studentId)
    
    Note over RegistrationService: Kiểm tra activity, student,<br/>điều kiện đăng ký (draft, important,<br/>thời gian, số lượng, đã đăng ký)
    RegistrationService->>Repository: findByIdAndIsDeletedFalse(activityId)
    Repository->>Database: SELECT * FROM activities<br/>WHERE id = ? AND is_deleted = false
    Database-->>Repository: Activity
    Repository-->>RegistrationService: Activity
    
    Note over RegistrationService: Tạo registration với<br/>status = PENDING/APPROVED<br/>và generate ticketCode
    RegistrationService->>Repository: save(registration)
    Repository->>Database: INSERT INTO activity_registrations<br/>(activity_id, student_id, registered_date,<br/>status, ticket_code)<br/>VALUES (?, ?, ?, ?, ?)
    Database-->>Repository: ActivityRegistration saved
    Repository-->>RegistrationService: ActivityRegistration
    
    alt Status = APPROVED
        Note over RegistrationService: Tự động tạo participation
        RegistrationService->>Repository: save(participation) với REGISTERED
        Repository->>Database: INSERT INTO activity_participations<br/>(registration_id, participation_type,<br/>points_earned, date)
        Database-->>Repository: ActivityParticipation saved
        Repository-->>RegistrationService: ActivityParticipation
    end
    
    Note over RegistrationService: Gửi thông báo cho sinh viên
    RegistrationService->>NotificationService: sendNotification(...)
    NotificationService->>Repository: save(notification)
    Repository->>Database: INSERT INTO notifications<br/>(user_id, title, content, metadata, read)
    Database-->>Repository: Notification saved
    Repository-->>NotificationService: Notification
    NotificationService-->>RegistrationService: Notification sent
    
    RegistrationService-->>RegistrationController: Response(success, ActivityRegistrationResponse)
    RegistrationController-->>Client: ResponseEntity.status(201)
    Client-->>Student: Hiển thị thông báo đăng ký thành công
```

### 2. Hủy Đăng ký Tham gia Hoạt động (Cancel Registration)

```mermaid
sequenceDiagram
    participant Student as Student
    participant Client as Client/Frontend
    participant RegistrationController as ActivityRegistrationController
    participant RegistrationService as ActivityRegistrationServiceImpl
    participant Repository as Repository
    participant Database as Database

    Student->>Client: Hủy đăng ký hoạt động
    Client->>RegistrationController: DELETE /api/registrations/activity/{activityId}
    
    Note over RegistrationController: Lấy studentId từ authentication
    RegistrationController->>RegistrationController: getStudentIdFromAuth(authentication)
    RegistrationController->>Repository: getStudentIdByUsername(username)
    Repository->>Database: SELECT id FROM students<br/>WHERE user_id = (SELECT id FROM users<br/>WHERE username = ?)
    Database-->>Repository: studentId
    Repository-->>RegistrationController: studentId
    
    RegistrationController->>RegistrationService: cancelRegistration(activityId, studentId)
    
    Note over RegistrationService: Tìm registration và kiểm tra<br/>status = PENDING (chỉ PENDING mới hủy được)
    RegistrationService->>Repository: findByActivityIdAndStudentId(activityId, studentId)
    Repository->>Database: SELECT * FROM activity_registrations<br/>WHERE activity_id = ? AND student_id = ?
    Database-->>Repository: ActivityRegistration
    Repository-->>RegistrationService: ActivityRegistration
    
    Note over RegistrationService: Cập nhật status = CANCELLED
    RegistrationService->>Repository: save(registration) với status = CANCELLED
    Repository->>Database: UPDATE activity_registrations<br/>SET status = 'CANCELLED'<br/>WHERE id = ?
    Database-->>Repository: ActivityRegistration updated
    Repository-->>RegistrationService: ActivityRegistration
    
    RegistrationService-->>RegistrationController: Response(success, null)
    RegistrationController-->>Client: ResponseEntity.ok()
    Client-->>Student: Hiển thị thông báo hủy đăng ký thành công
```

## Ghi chú

1. **Quyền truy cập**: 
   - Đăng ký và hủy đăng ký: Chỉ Student có thể thực hiện cho chính mình

2. **Đăng ký tham gia**:
   - **Kiểm tra điều kiện**:
     - Activity không phải important/mandatory (tự động đăng ký)
     - Activity không phải draft (phải đã publish)
     - Sinh viên chưa đăng ký
     - Trong thời gian đăng ký (registrationStartDate đến registrationDeadline)
     - Còn chỗ (nếu activity có giới hạn ticketQuantity)
   
   - **Trạng thái đăng ký**:
     - `PENDING`: Nếu activity.requiresApproval = true
     - `APPROVED`: Nếu activity.requiresApproval = false (tự động duyệt)
   
   - **Ticket Code**: 
     - Tự động tạo mã vé duy nhất cho mỗi registration
     - Được sử dụng để check-in/check-out
   
   - **Tự động tạo Participation**:
     - Nếu status = APPROVED, tự động tạo ActivityParticipation với participationType = REGISTERED
   
   - **Thông báo**: 
     - Gửi thông báo cho sinh viên sau khi đăng ký thành công
     - Nội dung thông báo khác nhau tùy theo status (APPROVED hoặc PENDING)

3. **Hủy đăng ký**:
   - **Điều kiện hủy**:
     - Chỉ có thể hủy khi status = PENDING
     - Không thể hủy khi status = APPROVED (đã được duyệt)
     - Không thể hủy khi status = CANCELLED (đã hủy rồi)
   
   - **Cập nhật**: 
     - Chỉ cập nhật status = CANCELLED
     - Không xóa registration khỏi database

4. **Auto-registration**:
   - Hoạt động có `isImportant = true` hoặc `mandatoryForFacultyStudents = true` sẽ tự động đăng ký cho sinh viên
   - Sinh viên không thể tự đăng ký hoặc hủy các hoạt động này

