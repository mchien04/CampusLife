# Sequence Diagram - Chức năng Điểm danh (Check-in/Check-out)

## Mô tả
Sequence diagram mô tả luồng xử lý điểm danh tham gia hoạt động trong hệ thống CampusLife. Hệ thống sử dụng check-in 2 lần: lần 1 (check-in) và lần 2 (check-out) để hoàn thành tham gia sự kiện.

## Sequence Diagrams

### 1. Check-in (Lần 1)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant RegistrationController as ActivityRegistrationController
    participant RegistrationService as ActivityRegistrationServiceImpl
    participant Repository as Repository
    participant Database as Database

    Admin->>Client: Quét QR code hoặc nhập ticketCode
    Client->>RegistrationController: POST /api/registrations/checkin<br/>(ticketCode hoặc studentId)
    
    RegistrationController->>RegistrationService: checkIn(request)
    
    Note over RegistrationService: Tìm registration theo ticketCode hoặc studentId
    alt Tìm theo ticketCode
        RegistrationService->>Repository: findByTicketCode(ticketCode)
        Repository->>Database: SELECT * FROM activity_registrations<br/>WHERE ticket_code = ?
        Database-->>Repository: ActivityRegistration
        Repository-->>RegistrationService: ActivityRegistration
    else Tìm theo studentId
        RegistrationService->>Repository: findByStudentIdAndStatus(studentId, APPROVED)
        Repository->>Database: SELECT * FROM activity_registrations<br/>WHERE student_id = ? AND status = 'APPROVED'
        Database-->>Repository: ActivityRegistration
        Repository-->>RegistrationService: ActivityRegistration
    end
    
    Note over RegistrationService: Kiểm tra activity không phải draft
    
    Note over RegistrationService: Lấy participation
    RegistrationService->>Repository: findByRegistration(registration)
    Repository->>Database: SELECT * FROM activity_participations<br/>WHERE registration_id = ?
    Database-->>Repository: ActivityParticipation
    Repository-->>RegistrationService: ActivityParticipation
    
    Note over RegistrationService: Kiểm tra participationType = REGISTERED
    
    Note over RegistrationService: Check-in (lần 1)
    RegistrationService->>RegistrationService: participation.setParticipationType(CHECKED_IN)<br/>setCheckInTime(now)<br/>setDate(now)
    RegistrationService->>Repository: save(participation)
    Repository->>Database: UPDATE activity_participations<br/>SET participation_type = 'CHECKED_IN',<br/>check_in_time = ?, date = ?<br/>WHERE id = ?
    Database-->>Repository: ActivityParticipation updated
    Repository-->>RegistrationService: ActivityParticipation
    
    RegistrationService-->>RegistrationController: Response(success, "Check-in thành công.<br/>Vui lòng check-out khi rời khỏi sự kiện.")
    RegistrationController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo check-in thành công
```

### 2. Check-out (Lần 2)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant RegistrationController as ActivityRegistrationController
    participant RegistrationService as ActivityRegistrationServiceImpl
    participant Repository as Repository
    participant Database as Database

    Admin->>Client: Quét QR code hoặc nhập ticketCode<br/>(lần 2 - check-out)
    Client->>RegistrationController: POST /api/registrations/checkin<br/>(ticketCode hoặc studentId)
    
    RegistrationController->>RegistrationService: checkIn(request)
    
    Note over RegistrationService: Tìm registration và participation<br/>(tương tự check-in lần 1)
    Note over RegistrationService: Kiểm tra participationType = CHECKED_IN
    
    Note over RegistrationService: Check-out và cập nhật trạng thái
    RegistrationService->>RegistrationService: participation.setParticipationType(ATTENDED)<br/>setCheckOutTime(now)<br/>registration.setStatus(ATTENDED)
    RegistrationService->>Repository: save(participation)
    Repository->>Database: UPDATE activity_participations<br/>SET participation_type = 'ATTENDED',<br/>check_out_time = ?<br/>WHERE id = ?
    Database-->>Repository: ActivityParticipation updated
    Repository-->>RegistrationService: ActivityParticipation
    
    RegistrationService->>Repository: save(registration)
    Repository->>Database: UPDATE activity_registrations<br/>SET status = 'ATTENDED'<br/>WHERE id = ?
    Database-->>Repository: ActivityRegistration updated
    Repository-->>RegistrationService: ActivityRegistration
    
    alt Activity không yêu cầu submission
        Note over RegistrationService: Tự động cập nhật điểm và isCompleted<br/>(tùy loại activity: series, CHUYEN_DE_DOANH_NGHIEP, hoặc đơn lẻ)
        RegistrationService->>RegistrationService: participation.setIsCompleted(true)<br/>setParticipationType(COMPLETED)<br/>setPointsEarned(...)
        RegistrationService->>Repository: save(participation)
        Repository->>Database: UPDATE activity_participations<br/>SET is_completed = true,<br/>participation_type = 'COMPLETED',<br/>points_earned = ?<br/>WHERE id = ?
        Database-->>Repository: ActivityParticipation updated
        Repository-->>RegistrationService: ActivityParticipation
        
        Note over RegistrationService: Cập nhật StudentScore hoặc series progress
        RegistrationService->>Repository: Cập nhật điểm tổng hợp
        Repository->>Database: UPDATE student_scores<br/>SET score = ?, ...<br/>WHERE ...
        Database-->>Repository: Updated
        Repository-->>RegistrationService: Success
    end
    
    RegistrationService-->>RegistrationController: Response(success, "Check-out thành công.<br/>Đã hoàn thành tham gia sự kiện.")
    RegistrationController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo check-out thành công
```

## Ghi chú

1. **Quy trình 2 lần**:
   - **Lần 1 (Check-in)**: REGISTERED → CHECKED_IN
   - **Lần 2 (Check-out)**: CHECKED_IN → CHECKED_OUT → ATTENDED

2. **Tìm registration**:
   - Có thể tìm theo `ticketCode` (quét QR code) hoặc `studentId`
   - Registration phải có status = APPROVED

3. **Kiểm tra**:
   - Activity không được là draft
   - Participation phải tồn tại (được tạo khi phê duyệt)

4. **Tự động cập nhật điểm** (khi check-out và activity không yêu cầu submission):
   - **Activity trong series**: `pointsEarned = 0`, chỉ cập nhật series progress
   - **Activity CHUYEN_DE_DOANH_NGHIEP**: Dual score (CHUYEN_DE count + REN_LUYEN points)
   - **Activity đơn lẻ khác**: Tính điểm từ `maxPoints` và cập nhật StudentScore

5. **Trạng thái**:
   - Sau check-in: `participationType = CHECKED_IN`
   - Sau check-out: `registration.status = ATTENDED`, `participationType = ATTENDED` (hoặc `COMPLETED` nếu không yêu cầu submission)

6. **Activity yêu cầu submission**:
   - Sau check-out, sinh viên cần nộp bài và được chấm điểm
   - Không tự động cập nhật điểm khi check-out

