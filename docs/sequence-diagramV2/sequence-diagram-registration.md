# Sequence Diagram - Registration Module (Đăng ký & Check-in/out)

**Hệ thống:** CampusLife (Spring Boot + React)  
**Nhóm chức năng:** Registration (Đăng ký hoạt động, Phê duyệt, Điểm danh, Vé)  
**Các thành phần tham gia:**

| Ký hiệu | Thành phần | Vai trò |
|---------|-----------|---------|
| Student | Sinh viên | Người đăng ký, xem vé, xem lịch sử |
| Admin | Admin / Manager / Staff | Phê duyệt, check-in/out, quản lý |
| Client | React Frontend | Giao diện người dùng |
| Controller | RegistrationController / AdminRegistrationController | Tiếp nhận request, routing |
| RegService | RegistrationService | Xử lý nghiệp vụ đăng ký, vé, trạng thái |
| PartService | ParticipationService | Xử lý nghiệp vụ điểm danh, tính duration, valid |
| NotiService | NotificationService | Gửi thông báo cho sinh viên |
| RegRepo | RegistrationRepository | Truy vấn bảng `activity_registrations` |
| ActRepo | ActivityRepository | Truy vấn bảng `activities` |
| StuRepo | StudentRepository | Truy vấn bảng `students` |
| PartRepo | ParticipationRepository | Truy vấn bảng `participation_records` |
| NotiRepo | NotificationRepository | Truy vấn bảng `notifications` |
| DB | Database | Lưu trữ dữ liệu (MySQL/PostgreSQL) |
| QRScanner | Thiết bị quét QR | Quét mã QR, decode ticketCode |

---

## Mục lục

1. [Hủy đăng ký hoạt động (3.3.5)](#1-hủy-đăng-ký-hoạt-động-335)
2. [Phê duyệt đăng ký (3.3.6)](#2-phê-duyệt-đăng-ký-336)
3. [Lịch sử tham gia (3.3.8)](#3-lịch-sử-tham-gia-338)
4. [Check-in & Check-out (3.3.12 & 3.3.13)](#4-check-in--check-out-3312--3313)
5. [Xem vé tham gia hoạt động (3.3.22)](#5-xem-vé-tham-gia-hoạt-động-3322)
6. [Kiểm tra trạng thái vé (3.3.23)](#6-kiểm-tra-trạng-thái-vé-3323)
7. [Check-in bằng QR & Xác thực mã vé (N.52 & N.53)](#7-check-in-bằng-qr--xác-thực-mã-vé-n52--n53)
8. [Đồng bộ dữ liệu điểm danh (N.54)](#8-đồng-bộ-dữ-liệu-điểm-danh-n54)

---

## 1. Hủy đăng ký hoạt động (3.3.5)

**Endpoint:** `DELETE /api/registrations/{id}`  
**Actor:** Student  
**Điều kiện:** Registration status = PENDING hoặc APPROVED, Activity chưa bắt đầu.

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant Client
    participant Controller as RegistrationController
    participant RegService as RegistrationService
    participant ActRepo as ActivityRepository
    participant RegRepo as RegistrationRepository
    participant DB as Database

    Note over Student, DB: Luồng 1: Hủy đăng ký hoạt động

    Student->>Client: Chọn registration đã đăng ký<br/>trong danh sách "Hoạt động của tôi"
    Student->>Client: Nhấn "Hủy đăng ký"
    Client->>Client: Kiểm tra status<br/>(PENDING hoặc APPROVED)
    Client->>Client: Xác nhận hủy (dialog)

    Client->>Controller: DELETE /api/registrations/{id}
    Controller->>Controller: @PreAuthorize("hasRole('STUDENT')")
    Controller->>Controller: Lấy studentId từ JWT Authentication

    Controller->>RegService: cancelRegistration(id, studentId)
    RegService->>RegRepo: findById(id)
    RegRepo->>DB: SELECT * FROM activity_registrations WHERE id = ?
    DB-->>RegRepo:' "Registration entity"'
    RegRepo-->>RegService:' "Optional<Registration>"'

    RegService->>RegService: Kiểm tra tồn tại<br/>Nếu empty → throw RegistrationNotFoundException
    RegService->>RegService: Kiểm tra registration.studentId == studentId<br/>Nếu không khớp → throw AccessDeniedException

    RegService->>ActRepo: findById(registration.activityId)
    ActRepo->>DB: SELECT * FROM activities WHERE id = ?
    DB-->>ActRepo:' "Activity entity"'
    ActRepo-->>RegService: Optional<Activity>

    RegService->>RegService: Kiểm tra activity.startDate > now()<br/>Nếu đã bắt đầu → throw ActivityAlreadyStartedException

    RegService->>RegService: registration.setStatus(CANCELLED)
    RegService->>RegRepo: save(registration)
    RegRepo->>DB: UPDATE activity_registrations<br/>SET status = 'CANCELLED', updated_at = now()<br/>WHERE id = ?
    DB-->>RegRepo:' "Updated row"'
    RegRepo-->>RegService:' "Registration (saved)"'

    RegService-->>Controller:' "RegistrationDTO"'
    Controller-->>Client: 200 OK + ApiResponse(success)
    Client-->>Student:' Hiển thị thông báo<br/>"Hủy đăng ký thành công"'
```

---

## 2. Phê duyệt đăng ký (3.3.6)

**Endpoint:** `PUT /api/admin/registrations/{id}/approve` (hoặc `.../reject`)  
**Actor:** Admin  
**Mô tả:** Admin phê duyệt hoặc từ chối đăng ký. Nếu APPROVED, tạo ticketCode (UUID) và gửi notification.

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant Client
    participant Controller as AdminRegistrationController
    participant RegService as RegistrationService
    participant NotiService as NotificationService
    participant RegRepo as RegistrationRepository
    participant NotiRepo as NotificationRepository
    participant DB as Database

    Note over Admin, DB: Luồng 2: Phê duyệt / Từ chối đăng ký

    Admin->>Client: Truy cập trang "Quản lý đăng ký"
    Client->>Controller: GET /api/admin/registrations?status=PENDING
    Controller->>RegService: getPendingRegistrations(pageable)
    RegService->>RegRepo: findByStatus(PENDING, pageable)
    RegRepo->>DB: SELECT ... WHERE status = 'PENDING'
    DB-->>RegRepo:' "Page<Registration>"'
    RegRepo-->>RegService:' "Page<Registration>"'
    RegService-->>Controller:' "Page<RegistrationDTO>"'
    Controller-->>Client: 200 OK + List<PENDING registrations>
    Client-->>Admin: Hiển thị danh sách chờ phê duyệt

    Admin->>Client: Chọn 1 registration → Nhấn "Duyệt" (hoặc "Từ chối")
    Client->>Controller: PUT /api/admin/registrations/{id}/approve<br/>Body: { action: "APPROVE" }
    Controller->>Controller: @PreAuthorize("hasRole('ADMIN')")
    Controller->>RegService: approveRegistration(id, action)

    RegService->>RegRepo: findById(id)
    RegRepo->>DB: SELECT * FROM activity_registrations WHERE id = ?
    DB-->>RegRepo:' "Registration entity"'
    RegRepo-->>RegService:' "Optional<Registration>"'

    RegService->>RegService: Kiểm tra tồn tại<br/>Nếu empty → throw RegistrationNotFoundException
    RegService->>RegService: Kiểm tra status != APPROVED && status != REJECTED<br/>Nếu đã phê duyệt/từ chối → throw AlreadyProcessedException

    alt action == APPROVE
        RegService->>RegService: registration.setStatus(APPROVED)
        RegService->>RegService: ticketCode = UUID.randomUUID().toString()
        RegService->>RegService: registration.setTicketCode(ticketCode)
    else action == REJECT
        RegService->>RegService: registration.setStatus(REJECTED)
        RegService->>RegService: registration.setTicketCode(null)
    end

    RegService->>RegRepo: save(registration)
    RegRepo->>DB: UPDATE activity_registrations<br/>SET status = ?, ticket_code = ?, updated_at = now()<br/>WHERE id = ?
    DB-->>RegRepo:' "Updated row"'
    RegRepo-->>RegService:' "Registration (saved)"'

    RegService->>NotiService: sendNotification(studentId, title, message)
    NotiService->>NotiRepo: save(notification)
    NotiRepo->>DB: INSERT INTO notifications ...
    DB-->>NotiRepo:' "Notification entity"'
    NotiRepo-->>NotiService: Notification
    NotiService-->>RegService: void

    RegService-->>Controller:' "RegistrationDTO"'
    Controller-->>Client: 200 OK + ApiResponse(success)
    Client-->>Admin:' Hiển thị thông báo<br/>"Đã phê duyệt / Từ chối thành công"'
```

---

## 3. Lịch sử tham gia (3.3.8)

**Endpoint:** `GET /api/registrations/my`  
**Actor:** Student  
**Mô tả:** Student xem toàn bộ lịch sử đăng ký hoạt động của mình.

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant Client
    participant Controller as RegistrationController
    participant RegService as RegistrationService
    participant RegRepo as RegistrationRepository
    participant StuRepo as StudentRepository
    participant DB as Database

    Note over Student, DB: Luồng 3: Lịch sử tham gia hoạt động

    Student->>Client: Truy cập trang "Lịch sử tham gia"
    Client->>Controller: GET /api/registrations/my
    Controller->>Controller: @PreAuthorize("hasRole('STUDENT')")
    Controller->>Controller: Lấy studentId từ JWT

    Controller->>RegService: getMyRegistrationHistory(studentId)
    RegService->>RegRepo: findByStudentId(studentId, pageable)
    RegRepo->>DB: SELECT * FROM activity_registrations<br/>WHERE student_id = ? AND is_deleted = false<br/>ORDER BY registered_date DESC
    DB-->>RegRepo:' "List<Registration>"'
    RegRepo-->>RegService:' "List<Registration>"'

    RegService->>StuRepo: findById(studentId)
    StuRepo->>DB: SELECT * FROM students WHERE id = ?
    DB-->>StuRepo:' "Student entity"'
    StuRepo-->>RegService:' "Student"'

    RegService->>RegService: Map từng Registration → RegistrationHistoryDTO<br/>(activityName, status, ticketCode, registeredDate,<br/>activityStartDate, activityLocation)

    RegService-->>Controller:' "List<RegistrationHistoryDTO>"'
    Controller-->>Client: 200 OK + List<RegistrationHistoryDTO>
    Client-->>Student: Hiển thị bảng lịch sử<br/>có phân trang và filter
```

---

## 4. Check-in & Check-out (3.3.12 & 3.3.13)

**Endpoint:**
- Check-in: `POST /api/registrations/checkin`
- Check-out: `POST /api/registrations/checkout`

**Actor:** Admin / Staff  
**Mô tả:** Điểm danh vào và ra hoạt động. Check-in tạo ParticipationRecord. Check-out cập nhật checkOutTime, tính duration, đánh dấu valid nếu đủ thời gian.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin/Staff
    participant Client
    participant Controller as RegistrationController
    participant RegService as RegistrationService
    participant PartService as ParticipationService
    participant RegRepo as RegistrationRepository
    participant ActRepo as ActivityRepository
    participant PartRepo as ParticipationRepository
    participant DB as Database

    Note over Admin, DB: Luồng 4.1: Check-in hoạt động (3.3.12)

    Admin->>Client: Nhập ticketCode / Quét QR
    Client->>Controller: POST /api/registrations/checkin<br/>Body: { ticketCode, activityId }
    Controller->>Controller: @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    Controller->>RegService: checkIn(ticketCode, activityId)

    RegService->>RegRepo: findByTicketCodeAndActivityId(ticketCode, activityId)
    RegRepo->>DB: SELECT * FROM activity_registrations<br/>WHERE ticket_code = ? AND activity_id = ?
    DB-->>RegRepo:' "Registration entity"'
    RegRepo-->>RegService:' "Optional<Registration>"'
    RegService->>RegService: Kiểm tra tồn tại<br/>Nếu empty → throw RegistrationNotFoundException

    RegService->>RegService: Kiểm tra status == APPROVED<br/>Nếu không → throw InvalidStatusException
    RegService->>RegService: Kiểm tra checkInTime == null<br/>Nếu đã check-in → throw AlreadyCheckedInException

    RegService->>ActRepo: findById(activityId)
    ActRepo->>DB: SELECT * FROM activities WHERE id = ?
    DB-->>ActRepo:' "Activity entity"'
    ActRepo-->>RegService: Optional<Activity>
    RegService->>RegService: Kiểm tra now() trong [startDate, endDate]<br/>Nếu ngoài thời gian → throw ActivityNotInProgressException

    RegService->>RegService: registration.setStatus(ATTENDED)
    RegService->>RegService: registration.setCheckInTime(now())
    RegService->>RegRepo: save(registration)
    RegRepo->>DB: UPDATE activity_registrations<br/>SET status = 'ATTENDED', check_in_time = now()<br/>WHERE id = ?
    DB-->>RegRepo:' "Updated row"'
    RegRepo-->>RegService:' "Registration (saved)"'

    RegService->>PartService: createParticipationRecord(studentId, activityId, checkInTime)
    PartService->>PartRepo: save(participationRecord)
    PartRepo->>DB: INSERT INTO participation_records<br/>(student_id, activity_id, check_in_time, valid, created_at)<br/>VALUES (?, ?, ?, false, now())
    DB-->>PartRepo:' "ParticipationRecord entity"'
    PartRepo-->>PartService: ParticipationRecord
    PartService-->>RegService: ParticipationRecord

    RegService-->>Controller:' "CheckInResponseDTO<br/>(studentName, activityName, checkInTime, status)"'
    Controller-->>Client: 200 OK + ApiResponse(success)
    Client-->>Admin: Hiển thị thông tin check-in thành công<br/>+ Gửi notification cho Student

    Note over Admin, DB: Luồng 4.2: Check-out hoạt động (3.3.13)

    Admin->>Client: Nhập ticketCode → Nhấn "Check-out"
    Client->>Controller: POST /api/registrations/checkout<br/>Body: { ticketCode }
    Controller->>RegService: checkOut(ticketCode)

    RegService->>RegRepo: findByTicketCode(ticketCode)
    RegRepo->>DB: SELECT * FROM activity_registrations WHERE ticket_code = ?
    DB-->>RegRepo:' "Registration entity"'
    RegRepo-->>RegService:' "Optional<Registration>"'
    RegService->>RegService: Kiểm tra tồn tại<br/>Nếu empty → throw RegistrationNotFoundException

    RegService->>RegService: Kiểm tra status == ATTENDED<br/>Nếu không → throw InvalidStatusException
    RegService->>RegService: Kiểm tra checkOutTime == null<br/>Nếu đã check-out → throw AlreadyCheckedOutException

    RegService->>RegService: registration.setCheckOutTime(now())
    RegService->>RegRepo: save(registration)
    RegRepo->>DB: UPDATE activity_registrations<br/>SET check_out_time = now()<br/>WHERE id = ?
    DB-->>RegRepo:' "Updated row"'
    RegRepo-->>RegService:' "Registration (saved)"'

    RegService->>PartService: updateCheckOut(participationRecordId, checkOutTime)
    PartService->>PartRepo: findByStudentIdAndActivityId(studentId, activityId)
    PartRepo->>DB: SELECT * FROM participation_records<br/>WHERE student_id = ? AND activity_id = ?
    DB-->>PartRepo:' "ParticipationRecord"'
    PartRepo-->>PartService: ParticipationRecord

    PartService->>PartService: record.setCheckOutTime(now())
    PartService->>PartService: duration = checkOutTime - checkInTime (minutes)
    PartService->>ActRepo: findById(activityId)
    ActRepo->>DB: SELECT min_duration FROM activities WHERE id = ?
    DB-->>ActRepo:' "minDuration"'
    ActRepo-->>PartService: minDuration

    PartService->>PartService: Nếu duration >= minDuration<br/>record.setValid(true)
    PartService->>PartRepo: save(record)
    PartRepo->>DB: UPDATE participation_records<br/>SET check_out_time = ?, valid = ?, updated_at = now()<br/>WHERE id = ?
    DB-->>PartRepo:' "Updated row"'
    PartRepo-->>PartService: ParticipationRecord (saved)
    PartService-->>RegService: ParticipationRecord

    RegService-->>Controller:' "CheckOutResponseDTO<br/>(studentName, checkInTime, checkOutTime, duration, valid)"'
    Controller-->>Client: 200 OK + ApiResponse(success)
    Client-->>Admin: Hiển thị thông tin check-out + valid status<br/>+ Gửi notification cho Student
```

---

## 5. Xem vé tham gia hoạt động (3.3.22)

**Endpoint:** `GET /api/registrations/my/tickets`  
**Actor:** Student  
**Mô tả:** Student xem danh sách vé đã được phê duyệt (APPROVED hoặc ATTENDED). Mỗi vé có thể hiển thị QR code.

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant Client
    participant Controller as RegistrationController
    participant RegService as RegistrationService
    participant RegRepo as RegistrationRepository
    participant ActRepo as ActivityRepository
    participant DB as Database

    Note over Student, DB: Luồng 5: Xem vé tham gia hoạt động

    Student->>Client: Truy cập trang "Vé của tôi"
    Client->>Controller: GET /api/registrations/my/tickets
    Controller->>Controller: @PreAuthorize("hasRole('STUDENT')")
    Controller->>Controller: Lấy studentId từ JWT

    Controller->>RegService: getMyTickets(studentId)
    RegService->>RegRepo: findByStudentIdAndStatusIn(studentId, [APPROVED, ATTENDED])
    RegRepo->>DB: SELECT * FROM activity_registrations<br/>WHERE student_id = ? AND status IN ('APPROVED','ATTENDED')<br/>AND is_deleted = false<br/>ORDER BY registered_date DESC
    DB-->>RegRepo:' "List<Registration>"'
    RegRepo-->>RegService:' "List<Registration>"'

    loop Map từng Registration → TicketResponse
        RegService->>ActRepo: findById(registration.activityId)
        ActRepo->>DB: SELECT * FROM activities WHERE id = ?
        DB-->>ActRepo:' "Activity"'
        ActRepo-->>RegService: Activity
        RegService->>RegService: Tạo QRCodeImage từ ticketCode<br/>(dùng thư viện ZXing/QRCode generation)
    end

    RegService->>RegService: Build List<TicketResponse><br/>(ticketCode, activityName, startDate, location, qrCodeImageBase64)
    RegService-->>Controller:' "List<TicketResponse>"'
    Controller-->>Client: 200 OK + List<TicketResponse>
    Client-->>Student:' Hiển thị danh sách vé dạng card/grid<br/>có QR code + nút "Tải về / Chia sẻ"'
```

---

## 6. Kiểm tra trạng thái vé (3.3.23)

**Endpoint:** `GET /api/registrations/{ticketCode}/status`  
**Actor:** Admin / Staff  
**Mô tả:** Kiểm tra nhanh trạng thái vé để xác minh quyền tham gia.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin/Staff
    participant Client
    participant Controller as AdminRegistrationController
    participant RegService as RegistrationService
    participant RegRepo as RegistrationRepository
    participant StuRepo as StudentRepository
    participant ActRepo as ActivityRepository
    participant DB as Database

    Note over Admin, DB: Luồng 6: Kiểm tra trạng thái vé

    Admin->>Client: Nhập ticketCode vào ô tìm kiếm
    Client->>Controller: GET /api/registrations/{ticketCode}/status
    Controller->>Controller: @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    Controller->>RegService: getTicketStatus(ticketCode)

    RegService->>RegRepo: findByTicketCode(ticketCode)
    RegRepo->>DB: SELECT * FROM activity_registrations<br/>WHERE ticket_code = ?
    DB-->>RegRepo:' "Registration entity"'
    RegRepo-->>RegService:' "Optional<Registration>"'
    RegService->>RegService: Kiểm tra tồn tại<br/>Nếu empty → throw RegistrationNotFoundException

    RegService->>StuRepo: findById(registration.studentId)
    StuRepo->>DB: SELECT * FROM students WHERE id = ?
    DB-->>StuRepo:' "Student"'
    StuRepo-->>RegService:' "Student"'

    RegService->>ActRepo: findById(registration.activityId)
    ActRepo->>DB: SELECT * FROM activities WHERE id = ?
    DB-->>ActRepo:' "Activity"'
    ActRepo-->>RegService: Activity

    RegService->>RegService: Build TicketStatusDTO<br/>(status, studentName, activityName,<br/>ticketCode, registeredDate, checkInTime, checkOutTime)
    RegService-->>Controller:' "TicketStatusDTO"'
    Controller-->>Client: 200 OK + TicketStatusDTO
    Client-->>Admin: Hiển thị thông tin vé<br/>màu sắc theo status (xanh=APPROVED, đỏ=CANCELLED, ...)
```

---

## 7. Check-in bằng QR & Xác thực mã vé (N.52 & N.53)

**Endpoint:**
- QR Check-in: `POST /api/registrations/checkin/qr`
- Validate ticket: `GET /api/registrations/checkin/validate?ticketCode=...`

**Actor:** Admin / Staff + QRScanner  
**Mô tả:** Quét QR để check-in nhanh, hoặc xác thực mã vé thủ công trước khi check-in.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin/Staff
    actor QRScanner
    participant Client
    participant Controller as RegistrationController
    participant RegService as RegistrationService
    participant PartService as ParticipationService
    participant RegRepo as RegistrationRepository
    participant ActRepo as ActivityRepository
    participant PartRepo as ParticipationRepository
    participant DB as Database

    Note over Admin, DB: Luồng 7.1: Xác thực mã vé thủ công (N.53)

    Admin->>Client: Nhập mã vé vào ô "Kiểm tra vé"
    Client->>Controller: GET /api/registrations/checkin/validate?ticketCode=XYZ
    Controller->>Controller: @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    Controller->>RegService: validateTicket(ticketCode)

    RegService->>RegRepo: findByTicketCode(ticketCode)
    RegRepo->>DB: SELECT * FROM activity_registrations WHERE ticket_code = ?
    DB-->>RegRepo:' "Registration"'
    RegRepo-->>RegService:' "Optional<Registration>"'
    RegService->>RegService: Kiểm tra tồn tại<br/>Nếu empty → throw RegistrationNotFoundException

    RegService->>ActRepo: findById(registration.activityId)
    ActRepo->>DB: SELECT * FROM activities WHERE id = ?
    DB-->>ActRepo:' "Activity"'
    ActRepo-->>RegService: Activity
    RegService->>RegService: Kiểm tra activity đang check-in<br/>Nếu không trong thời gian → throw ActivityNotInProgressException

    RegService->>RegService: Kiểm tra status == APPROVED<br/>Nếu không → throw InvalidStatusException
    RegService->>RegService: Kiểm tra checkInTime == null<br/>Nếu đã check-in → throw AlreadyCheckedInException

    RegService->>RegService: Build TicketValidationDTO<br/>(valid=true, studentName, studentId, activityName, activityId, status)
    RegService-->>Controller:' "TicketValidationDTO"'
    Controller-->>Client: 200 OK + TicketValidationDTO
    Client-->>Admin:' Hiển thị thông tin sinh viên + trạng thái valid<br/>+ Nút "Check-in ngay"'

    Note over Admin, DB: Luồng 7.2: Check-in bằng QR Code (N.52)

    Admin->>QRScanner: Đưa vé QR cho sinh viên
    QRScanner->>QRScanner: Quét QR → Decode ticketCode
    QRScanner-->>Admin: Hiển thị ticketCode
    Admin->>Client: Nhấn "Check-in QR" (hoặc QRScanner gọi API trực tiếp)
    Client->>Controller: POST /api/registrations/checkin/qr<br/>Body: { ticketCode, deviceId }
    Controller->>Controller: @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    Controller->>RegService: checkInByQR(ticketCode, deviceId)

    RegService->>RegRepo: findByTicketCode(ticketCode)
    RegRepo->>DB: SELECT * FROM activity_registrations WHERE ticket_code = ?
    DB-->>RegRepo:' "Registration"'
    RegRepo-->>RegService:' "Optional<Registration>"'
    RegService->>RegService: Kiểm tra tồn tại<br/>Nếu empty → throw RegistrationNotFoundException

    RegService->>RegService: Kiểm tra status == APPROVED<br/>Nếu không → throw InvalidStatusException
    RegService->>RegService: Kiểm tra checkInTime == null<br/>Nếu đã check-in → throw AlreadyCheckedInException

    RegService->>ActRepo: findById(registration.activityId)
    ActRepo->>DB: SELECT * FROM activities WHERE id = ?
    DB-->>ActRepo:' "Activity"'
    ActRepo-->>RegService: Activity
    RegService->>RegService: Kiểm tra now() trong [startDate, endDate]<br/>Nếu ngoài → throw ActivityNotInProgressException

    RegService->>RegService: registration.setStatus(ATTENDED)
    RegService->>RegService: registration.setCheckInTime(now())
    RegService->>RegService: registration.setCheckInDevice(deviceId)
    RegService->>RegRepo: save(registration)
    RegRepo->>DB: UPDATE activity_registrations<br/>SET status='ATTENDED', check_in_time=now(),<br/>check_in_device = ? WHERE id = ?
    DB-->>RegRepo:' "Updated row"'
    RegRepo-->>RegService:' "Registration (saved)"'

    RegService->>PartService: createParticipationRecord(studentId, activityId, checkInTime)
    PartService->>PartRepo: save(participationRecord)
    PartRepo->>DB: INSERT INTO participation_records ...
    DB-->>PartRepo:' "ParticipationRecord"'
    PartRepo-->>PartService: ParticipationRecord
    PartService-->>RegService: ParticipationRecord

    RegService->>RegService: Build QRCheckInResponse<br/>(success=true, studentInfo, activityName, checkInTime, deviceId)
    RegService-->>Controller:' "QRCheckInResponse"'
    Controller-->>Client: 200 OK + QRCheckInResponse
    Client-->>Admin: Hiển thị thông tin sinh viên + ảnh đại diện<br/>+ Trạng thái check-in thành công
    Client->>Client: Phát âm thanh / hiệu ứng check-in thành công
```

---

## 8. Đồng bộ dữ liệu điểm danh (N.54)

**Endpoint:** `POST /api/registrations/backfill/participations`  
**Actor:** Admin  
**Mô tả:** Đồng bộ hóa dữ liệu: tìm tất cả registration đã ATTENDED nhưng chưa có ParticipationRecord, sau đó tạo bổ sung.

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant Client
    participant Controller as AdminRegistrationController
    participant RegService as RegistrationService
    participant PartService as ParticipationService
    participant RegRepo as RegistrationRepository
    participant PartRepo as ParticipationRepository
    participant DB as Database

    Note over Admin, DB: Luồng 8: Đồng bộ dữ liệu điểm danh (Backfill)

    Admin->>Client: Truy cập trang "Quản lý điểm danh"
    Admin->>Client: Nhấn "Đồng bộ dữ liệu"
    Client->>Controller: POST /api/registrations/backfill/participations
    Controller->>Controller: @PreAuthorize("hasRole('ADMIN')")
    Controller->>RegService: backfillParticipationRecords()

    RegService->>RegRepo: findAttendedWithoutParticipation()
    RegRepo->>DB: SELECT r.* FROM activity_registrations r<br/>LEFT JOIN participation_records p<br/>ON r.student_id = p.student_id AND r.activity_id = p.activity_id<br/>WHERE r.status = 'ATTENDED' AND p.id IS NULL
    DB-->>RegRepo:' "List<Registration>"'
    RegRepo-->>RegService:' "List<Registration>"'

    RegService->>RegService: Kiểm tra nếu list rỗng → return 0

    RegService->>PartService: batchCreateParticipationRecords(registrations)
    loop Từng registration trong list
        PartService->>PartService: Build ParticipationRecord<br/>(studentId, activityId, checkInTime, valid=false)
        PartService->>PartRepo: save(record)
        PartRepo->>DB: INSERT INTO participation_records ...
        DB-->>PartRepo:' "ParticipationRecord"'
        PartRepo-->>PartService: ParticipationRecord
    end

    PartService-->>RegService: List<ParticipationRecord> (đã tạo)
    RegService->>RegService: Đếm số lượng đã tạo = count
    RegService-->>Controller:' "BackfillResultDTO (syncedCount=count)"'
    Controller-->>Client: 200 OK + BackfillResultDTO
    Client-->>Admin:' Hiển thị thông báo<br/>"Đã đồng bộ X bản ghi điểm danh"'
```

---

## Phụ lục: Tóm tắt thành phần và chức năng

### A. Các Actor

| Actor | Vai trò trong module Registration |
|-------|-------------------------------------|
| **Student** | Đăng ký hoạt động, hủy đăng ký, xem lịch sử, xem vé, nhận notification |
| **Admin / Manager / Staff** | Phê duyệt/từ chối đăng ký, check-in/out, kiểm tra vé, đồng bộ dữ liệu |
| **QRScanner** | Thiết bị phần cứng quét QR, decode ticketCode, gửi lên hệ thống |

### B. Các Controller

| Controller | Endpoint chính | Phân quyền |
|------------|---------------|-----------|
| **RegistrationController** | `/api/registrations/**` | Student (JWT) + Admin/Staff |
| **AdminRegistrationController** | `/api/admin/registrations/**`, `/api/registrations/backfill/**` | Admin/Staff |

### C. Các Service

| Service | Chức năng chính | Luồng sử dụng |
|---------|----------------|--------------|
| **RegistrationService** | CRUD đăng ký, phê duyệt, hủy, xem vé, kiểm tra vé, check-in/out, QR check-in | 1, 2, 3, 4, 5, 6, 7, 8 |
| **ParticipationService** | Tạo/cập nhật ParticipationRecord, tính duration, đánh dấu valid | 4, 7, 8 |
| **NotificationService** | Gửi thông báo cho Student khi phê duyệt, check-in, check-out | 2, 4 |

### D. Các Repository

| Repository | Bảng tương ứng | Mô tả |
|------------|---------------|-------|
| **RegistrationRepository** | `activity_registrations` | Lưu trạng thái đăng ký, ticketCode, checkIn/checkOut time |
| **ActivityRepository** | `activities` | Kiểm tra thời gian hoạt động, minDuration, lấy thông tin activity |
| **StudentRepository** | `students` | Lấy thông tin sinh viên (name, id) cho response |
| **ParticipationRepository** | `participation_records` | Lưu bản ghi điểm danh, valid status, duration |
| **NotificationRepository** | `notifications` | Lưu thông báo gửi đi cho Student |

### E. Các Entity chính

| Entity | Trường quan trọng | Mô tả |
|--------|-------------------|-------|
| **ActivityRegistration** | `id`, `studentId`, `activityId`, `status` (PENDING/APPROVED/REJECTED/CANCELLED/ATTENDED), `ticketCode`, `registeredDate`, `checkInTime`, `checkOutTime`, `checkInDevice` | Trung tâm của module Registration |
| **ParticipationRecord** | `id`, `studentId`, `activityId`, `checkInTime`, `checkOutTime`, `valid` | Lưu dữ liệu điểm danh thực tế, dùng đánh giá tham gia |
| **Activity** | `id`, `name`, `startDate`, `endDate`, `location`, `minDuration` | Thông tin hoạt động, dùng validate thời gian check-in |
| **Notification** | `id`, `userId`, `title`, `message`, `createdAt`, `isRead` | Thông báo gửi cho Student |

### F. Trạng thái (Status Flow) của Registration

```
[PENDING] --(Admin Approve)--> [APPROVED] --(Check-in)--> [ATTENDED] --(Check-out)--> [ATTENDED]
[PENDING] --(Admin Reject)--> [REJECTED]
[PENDING] --(Student Cancel)--> [CANCELLED]
[APPROVED] --(Student Cancel, trước startDate)--> [CANCELLED]
```

### G. Điều kiện nghiệp vụ quan trọng (Business Rules)

| STT | Quy tắc | Áp dụng cho |
|-----|---------|-------------|
| 1 | Student chỉ được hủy registration khi status là PENDING hoặc APPROVED, và activity chưa bắt đầu | Hủy đăng ký |
| 2 | Admin không thể phê duyệt lại registration đã APPROVED hoặc REJECTED | Phê duyệt |
| 3 | TicketCode chỉ được tạo khi APPROVED; bị xóa khi REJECTED | Phê duyệt |
| 4 | Check-in chỉ được thực hiện khi status = APPROVED, activity đang diễn ra, và chưa check-in | Check-in |
| 5 | Check-out chỉ được thực hiện khi status = ATTENDED và chưa check-out | Check-out |
| 6 | ParticipationRecord.valid = true chỉ khi duration >= activity.minDuration | Check-out |
| 7 | Student chỉ xem được vé của chính mình (status APPROVED hoặc ATTENDED) | Xem vé |
| 8 | QR check-in và xác thực mã vé đều yêu cầu activity đang diễn ra và chưa check-in | QR & Validation |
| 9 | Backfill chỉ tạo ParticipationRecord cho registration có status = ATTENDED nhưng chưa có record | Đồng bộ |

---

*File được tạo bởi: Sequence Diagram Specialist — CampusLife System*  
*Module: Registration (Đăng ký & Check-in/out)*  
*Phiên bản: V2.0*
