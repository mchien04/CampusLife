# Sequence Diagram - Chức năng Xem Vé Sự kiện

## Mô tả
Sequence diagram mô tả luồng xử lý xem vé sự kiện trong hệ thống CampusLife. Bao gồm các chức năng xem vé của chính mình và validate/xem thông tin vé bằng ticketCode.

## Sequence Diagrams

### 1. Xem Vé của Chính mình (View My Ticket)

```mermaid
sequenceDiagram
    participant Student as Student
    participant Client as Client/Frontend
    participant RegistrationController as ActivityRegistrationController
    participant RegistrationService as ActivityRegistrationServiceImpl
    participant Repository as Repository
    participant Database as Database

    Student->>Client: Xem vé sự kiện đã đăng ký
    Client->>RegistrationController: GET /api/registrations/my<br/>(hoặc GET /api/registrations/{registrationId})
    
    Note over RegistrationController: Lấy studentId từ authentication
    RegistrationController->>RegistrationController: getStudentIdFromAuth(authentication)
    RegistrationController->>Repository: getStudentIdByUsername(username)
    Repository->>Database: SELECT id FROM students<br/>WHERE user_id = (SELECT id FROM users<br/>WHERE username = ?)
    Database-->>Repository: studentId
    Repository-->>RegistrationController: studentId
    
    RegistrationController->>RegistrationService: getStudentRegistrations(studentId)<br/>(hoặc getRegistrationById(registrationId))
    
    RegistrationService->>Repository: findByStudentIdAndStudentIsDeletedFalse(studentId)<br/>(hoặc findById(registrationId))
    Repository->>Database: SELECT ar.* FROM activity_registrations ar<br/>JOIN students s ON ar.student_id = s.id<br/>WHERE ar.student_id = ? AND s.is_deleted = false
    Database-->>Repository: List<ActivityRegistration>
    Repository-->>RegistrationService: List<ActivityRegistration>
    
    Note over RegistrationService: Map sang ActivityRegistrationResponse<br/>(bao gồm ticketCode, activity info, status)
    RegistrationService->>RegistrationService: toRegistrationResponse(registration)
    
    RegistrationService-->>RegistrationController: Response(success, ActivityRegistrationResponse)
    RegistrationController-->>Client: ResponseEntity.ok()
    Client-->>Student: Hiển thị thông tin vé<br/>(ticketCode, activity, status)
```

### 2. Validate/Xem Thông tin Vé bằng TicketCode

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant RegistrationController as ActivityRegistrationController
    participant RegistrationService as ActivityRegistrationServiceImpl
    participant Repository as Repository
    participant Database as Database

    Admin->>Client: Quét QR code hoặc nhập ticketCode
    Client->>RegistrationController: GET /api/registrations/checkin/validate<br/>?ticketCode=ABC123
    
    RegistrationController->>RegistrationService: validateTicketCode(ticketCode)
    
    RegistrationService->>Repository: findByTicketCode(ticketCode)
    Repository->>Database: SELECT * FROM activity_registrations<br/>WHERE ticket_code = ?
    Database-->>Repository: ActivityRegistration
    Repository-->>RegistrationService: ActivityRegistration
    
    Note over RegistrationService: Kiểm tra:<br/>- Activity không phải draft<br/>- Registration status = APPROVED
    
    alt Activity là draft
        RegistrationService-->>RegistrationController: Response(false, "Sự kiện chưa được công bố")
        RegistrationController-->>Client: ResponseEntity.badRequest()
    else Status != APPROVED
        RegistrationService-->>RegistrationController: Response(false, "Đăng ký chưa được duyệt")
        RegistrationController-->>Client: ResponseEntity.badRequest()
    else Hợp lệ
        Note over RegistrationService: Kiểm tra participation tồn tại
        RegistrationService->>Repository: findByRegistration(registration)
        Repository->>Database: SELECT * FROM activity_participations<br/>WHERE registration_id = ?
        Database-->>Repository: ActivityParticipation hoặc null
        Repository-->>RegistrationService: Optional<ActivityParticipation>
        
        alt Participation chưa tồn tại
            Note over RegistrationService: Tự động tạo participation
            RegistrationService->>RegistrationService: create ActivityParticipation<br/>(registration, REGISTERED, points = 0)
            RegistrationService->>Repository: save(participation)
            Repository->>Database: INSERT INTO activity_participations<br/>(registration_id, participation_type,<br/>points_earned, date)
            Database-->>Repository: ActivityParticipation saved
            Repository-->>RegistrationService: ActivityParticipation
        end
        
        Note over RegistrationService: Tạo response với thông tin vé
        RegistrationService->>RegistrationService: Build info map<br/>(ticketCode, student info,<br/>activity info, participation status,<br/>canCheckIn, canCheckOut)
        
        RegistrationService-->>RegistrationController: Response(success, ticketInfo)
        RegistrationController-->>Client: ResponseEntity.ok()
        Client-->>Admin: Hiển thị thông tin vé<br/>(student, activity, status,<br/>canCheckIn, canCheckOut)
    end
```

## Ghi chú

1. **Quyền truy cập**: 
   - Xem vé của chính mình: Chỉ Student
   - Validate ticketCode: Admin và Manager (dùng để check-in)

2. **Xem vé của chính mình**:
   - Lấy từ danh sách đăng ký của sinh viên
   - Hiển thị ticketCode, thông tin activity, và trạng thái đăng ký
   - Có thể xem chi tiết một registration cụ thể

3. **Validate ticketCode**:
   - **Mục đích**: Xem thông tin vé trước khi check-in (quét QR code hoặc nhập code)
   - **Kiểm tra**:
     - Activity không phải draft
     - Registration status = APPROVED
   - **Tự động tạo participation**: Nếu chưa có participation, tự động tạo với participationType = REGISTERED
   - **Thông tin trả về**:
     - ticketCode
     - Thông tin sinh viên (studentId, studentName, studentCode)
     - Thông tin hoạt động (activityId, activityName)
     - Trạng thái participation hiện tại (currentStatus)
     - canCheckIn: có thể check-in không (participationType = REGISTERED)
     - canCheckOut: có thể check-out không (participationType = CHECKED_IN)

4. **TicketCode**:
   - Mã vé duy nhất cho mỗi registration
   - Được tạo tự động khi đăng ký
   - Có thể được mã hóa thành QR code để quét khi check-in

5. **Participation Status**:
   - `REGISTERED`: Đã đăng ký, có thể check-in
   - `CHECKED_IN`: Đã check-in, có thể check-out
   - `ATTENDED`: Đã hoàn thành check-in/check-out

