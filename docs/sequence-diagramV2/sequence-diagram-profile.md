# Sequence Diagram - Profile Module (Thông tin cá nhân)

**Hệ thống:** CampusLife (Spring Boot + React)  
**Module:** Profile (Thông tin cá nhân)  
**Các Actor:** Admin, Manager, Student  
**Ngày cập nhật:** 2025-08-06

---

## Tổng quan Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant A as Admin/Manager
    participant S as Student
    participant C as Client (React)
    participant CTL as Controller
    participant SV as Service
    participant RP as Repository
    participant DB as Database

    %% ============================================================
    Note over A, DB: LUỒNG 1: XEM THÔNG TIN CÁ NHÂN THEO USERNAME (Admin/Manager)<br/>GET /api/admin/profiles/{username} (3.3.27)
    %% ============================================================

    A->>C: 1. Nhập username, click "Tìm kiếm"
    C->>CTL: 2. GET /api/admin/profiles/{username}<br/>Authorization: Bearer {admin/manager_token}
    CTL->>SV: 3. getProfileByUsername(username)

    SV->>RP: 4. findUserByUsername(username)
    RP->>DB: 5. SELECT * FROM users WHERE username = ?
    DB-->>RP:' "6. User record (id, username, email, role, status, ...)"'
    RP-->>SV:' "7. Optional<User>"'

    alt User không tồn tại
        SV-->>CTL:' 8a. Throw ResourceNotFoundException("User not found")'
        CTL-->>C:' "9a. 404 Not Found + ErrorResponse"'
        C-->>A:' "10a. Hiển thị thông báo "Không tìm thấy người dùng""'
    else User tồn tại
        SV->>SV: 8b. Extract role from User

        alt Role = STUDENT
            SV->>RP: 9b.1. findStudentProfileByUserId(userId)
            RP->>DB: 10b.1. SELECT sp.*, sc.name AS class_name, d.name AS dept_name<br/>FROM student_profiles sp<br/>JOIN student_classes sc ON sp.class_id = sc.id<br/>JOIN departments d ON sp.department_id = d.id<br/>WHERE sp.user_id = ?
            DB-->>RP:' "11b.1. StudentProfile + StudentClass + Department"'
            RP-->>SV:' "12b.1. StudentProfile entity"'

            SV->>RP: 13b.1. calculateTotalScore(studentId)
            RP->>DB: 14b.1. SELECT SUM(score) FROM student_semester_scores<br/>WHERE student_id = ?
            DB-->>RP:' "15b.1. totalScore (BigDecimal/Double)"'
            RP-->>SV:' "16b.1. totalScore"'

            SV->>SV: 17b.1. mapToStudentProfileResponse()<br/>(personalInfo, academicInfo, className, departmentName, totalScore)
            SV-->>CTL: 18b.1. StudentProfileResponse

        else Role = STAFF
            SV->>RP: 9b.2. findStaffProfileByUserId(userId)
            RP->>DB: 10b.2. SELECT sp.*, d.name AS dept_name<br/>FROM staff_profiles sp<br/>JOIN departments d ON sp.department_id = d.id<br/>WHERE sp.user_id = ?
            DB-->>RP:' "11b.2. StaffProfile + Department"'
            RP-->>SV:' "12b.2. StaffProfile entity"'

            SV->>SV: 13b.2. mapToStaffProfileResponse()<br/>(personalInfo, staffInfo, departmentName)
            SV-->>CTL: 14b.2. StaffProfileResponse

        else Role = ADMIN hoặc Role = MANAGER
            SV->>SV: 9b.3. buildUserProfileResponse(user)<br/>(basic info: id, username, email, fullName, role, status)
            SV-->>CTL: 10b.3. UserProfileResponse
        end

        CTL-->>C:' "11b. 200 OK + ProfileResponse (StudentProfileResponse | StaffProfileResponse | UserProfileResponse)"'
        C-->>A:' "12b. Hiển thị thông tin chi tiết theo từng role"'
    end

    %% ============================================================
    Note over S, DB: LUỒNG 2: XEM THÔNG TIN CÁ NHÂN (Student)<br/>GET /api/student/profile (3.3.26)
    %% ============================================================

    S->>C: 1. Truy cập trang "Thông tin cá nhân"
    C->>C: 2. Extract userId từ JWT Token / Auth Context
    C->>CTL: 3. GET /api/student/profile<br/>Authorization: Bearer {student_token}
    CTL->>SV: 4. getCurrentStudentProfile(userId)

    SV->>RP: 5. findUserById(userId)
    RP->>DB: 6. SELECT * FROM users WHERE id = ?
    DB-->>RP:' "7. User record"'
    RP-->>SV:' "8. User entity"'

    SV->>RP: 9. findStudentProfileByUserId(userId)
    RP->>DB: 10. SELECT sp.*, sc.name AS class_name, sc.code AS class_code,<br/>d.name AS dept_name, d.code AS dept_code<br/>FROM student_profiles sp<br/>JOIN student_classes sc ON sp.class_id = sc.id<br/>JOIN departments d ON sp.department_id = d.id<br/>WHERE sp.user_id = ?
    DB-->>RP:' "11. StudentProfile + StudentClass + Department"'
    RP-->>SV:' "12. StudentProfile entity"'

    SV->>RP: 13. findAddressByStudentProfileId(profileId)
    RP->>DB: 14. SELECT * FROM addresses WHERE profile_id = ?<br/>AND is_primary = true
    DB-->>RP:' "15. Address record (street, ward, district, province, country)"'
    RP-->>SV:' "16. Address entity"'

    SV->>RP: 17. getCurrentSemesterScore(studentId)
    RP->>DB: 18. SELECT * FROM student_semester_scores<br/>WHERE student_id = ?<br/>AND semester = (SELECT MAX(semester) FROM student_semester_scores WHERE student_id = ?)
    DB-->>RP:' "19. StudentSemesterScore (semester, score, rank, credits)"'
    RP-->>SV:' "20. StudentSemesterScore entity"'

    SV->>RP: 21. findTaskAssignmentsByStudentId(studentId)
    RP->>DB: 22. SELECT ta.*, t.title, t.description, t.deadline, t.status<br/>FROM task_assignments ta<br/>JOIN tasks t ON ta.task_id = t.id<br/>WHERE ta.student_id = ?<br/>ORDER BY t.deadline DESC
    DB-->>RP:' "23. List<TaskAssignment + Task>"'
    RP-->>SV:' "24. List<TaskAssignment> entities"'

    SV->>SV: 25. buildFullStudentProfileResponse()<br/>- personalInfo: User (fullName, dob, gender, email, phone)<br/>- academicInfo: StudentProfile (studentCode, enrollmentYear, status)<br/>- classInfo: StudentClass (className, classCode)<br/>- departmentInfo: Department (departmentName, departmentCode)<br/>- contact: phone, email<br/>- address: Address (street, ward, district, province)<br/>- scores: StudentSemesterScore (currentSemester, totalScore, credits)<br/>- tasks: List<TaskAssignment> (taskId, title, status, deadline)

    SV-->>CTL: 26. StudentProfileResponse đầy đủ
    CTL-->>C:' "27. 200 OK + StudentProfileResponse"'
    C-->>S:' "28. Hiển thị thông tin cá nhân đầy đủ:<br/>- Thông tin cá nhân<br/>- Thông tin học tập<br/>- Thông tin liên hệ & địa chỉ<br/>- Điểm số kỳ hiện tại<br/>- Danh sách nhiệm vụ được phân công"'
```

---

## Giải thích chi tiết từng luồng

### Luồng 1: Admin/Manager xem thông tin cá nhân theo username (3.3.27)

| Bước | Thành phần | Mô tả |
|------|-----------|-------|
| 1-2 | Client → Controller | Admin/Manager nhập username trên giao diện, Client gọi API `GET /api/admin/profiles/{username}` kèm token xác thực |
| 3 | Controller → Service | Chuyển tiếp yêu cầu đến Service layer |
| 4-7 | Service → Repository → Database | Tìm User theo username trong database. Trả về Optional<User> |
| 8a-10a | Error Flow | Nếu user không tồn tại, ném exception và trả về 404 |
| 8b | Service | Extract role từ User entity để xác định loại profile cần lấy |
| 9b.1-18b.1 | **STUDENT branch** | Lấy StudentProfile JOIN với StudentClass và Department. Tính tổng điểm từ StudentSemesterScore. Trả về StudentProfileResponse |
| 9b.2-14b.2 | **STAFF branch** | Lấy StaffProfile JOIN với Department. Trả về StaffProfileResponse |
| 9b.3-10b.3 | **ADMIN/MANAGER branch** | Không có bảng profile riêng, trả về UserProfileResponse chứa thông tin cơ bản + role |
| 11b-12b | Response | Controller trả về 200 OK với profile data tương ứng. Client render giao diện phù hợp theo role |

### Luồng 2: Student xem thông tin cá nhân (3.3.26)

| Bước | Thành phần | Mô tả |
|------|-----------|-------|
| 1-3 | Client → Controller | Student truy cập trang cá nhân, Client extract userId từ JWT token và gọi `GET /api/student/profile` |
| 4-8 | Service → Repository → Database | Tìm User theo ID từ token để xác thực và lấy thông tin cơ bản |
| 9-12 | StudentProfile query | Lấy StudentProfile JOIN StudentClass và Department để có thông tin lớp và khoa |
| 13-16 | Address query | Lấy địa chỉ (Address) liên kết với StudentProfile |
| 17-20 | SemesterScore query | Lấy điểm số kỳ hiện tại từ StudentSemesterScore (subquery để lấy semester mới nhất) |
| 21-24 | TaskAssignment query | Lấy danh sách nhiệm vụ được phân công từ TaskAssignment JOIN Task |
| 25-26 | Aggregation | Service gộp tất cả dữ liệu và build StudentProfileResponse đầy đủ |
| 27-28 | Response | Trả về 200 OK + StudentProfileResponse. Client hiển thị đầy đủ: personal, academic, contact, address, scores, tasks |

---

## Tóm tắt thành phần và chức năng

### Participants

| Participant | Vai trò | Chức năng chính |
|-------------|---------|-----------------|
| **Admin/Manager** | Actor | Quản trị viên/Quản lý có quyền tra cứu thông tin cá nhân của bất kỳ user nào trong hệ thống bằng username |
| **Student** | Actor | Sinh viên xem thông tin cá nhân của chính mình, bao gồm đầy đủ thông tin học tập, địa chỉ, điểm số và nhiệm vụ |
| **Client (React)** | Frontend | Giao diện người dùng, quản lý trạng thái, gọi API, render dữ liệu profile. Extract JWT token để lấy userId với student |
| **Controller** | REST API Layer | Nhận request HTTP, validate input, điều phối đến Service, trả về ResponseEntity (200 OK, 404 Not Found, v.v.) |
| **Service** | Business Logic Layer | Chứa toàn bộ logic nghiệp vụ: xác định role, điều phối query, aggregate dữ liệu từ nhiều nguồn, mapping sang DTO/Response |
| **Repository** | Data Access Layer | Định nghĩa các phương thức truy vấn dữ liệu sử dụng Spring Data JPA, JPQL/Native Query với JOIN |
| **Database** | Persistence | Lưu trữ dữ liệu: `users`, `student_profiles`, `staff_profiles`, `student_classes`, `departments`, `addresses`, `student_semester_scores`, `task_assignments`, `tasks` |

### Các Entity/Table liên quan

| Entity | Mối quan hệ | Mô tả |
|--------|-------------|-------|
| `User` | Base entity | Chứa thông tin cơ bản: id, username, email, password, role (STUDENT, STAFF, ADMIN, MANAGER), status |
| `StudentProfile` | Many-to-One với User | Thông tin sinh viên: studentCode, enrollmentYear, class_id, department_id |
| `StaffProfile` | Many-to-One với User | Thông tin nhân viên: staffCode, position, department_id |
| `StudentClass` | One-to-Many với StudentProfile | Thông tin lớp: name, code, department_id |
| `Department` | One-to-Many | Thông tin khoa/phòng: name, code |
| `Address` | Many-to-One với StudentProfile | Địa chỉ: street, ward, district, province, country, is_primary |
| `StudentSemesterScore` | Many-to-One với StudentProfile | Điểm số theo kỳ: semester, score, rank, credits |
| `TaskAssignment` | Many-to-One với StudentProfile | Phân công nhiệm vụ: task_id, student_id, assignedDate, status |
| `Task` | One-to-Many với TaskAssignment | Nhiệm vụ: title, description, deadline, status |

### DTO/Response Objects

| Response Object | Trường dữ liệu | Dùng cho |
|-----------------|----------------|----------|
| `StudentProfileResponse` | personalInfo, academicInfo, className, departmentName, totalScore, address, scores, tasks | Student xem profile; Admin/Manager tra cứu student |
| `StaffProfileResponse` | personalInfo, staffInfo, departmentName | Admin/Manager tra cứu staff |
| `UserProfileResponse` | id, username, email, fullName, role, status | Admin/Manager tra cứu admin/manager |
| `ErrorResponse` | code, message, timestamp, path | Trả về khi có lỗi (404, 403, 500) |

### Security & Authorization

| API Endpoint | Role cho phép | Mô tả |
|--------------|---------------|-------|
| `GET /api/admin/profiles/{username}` | ADMIN, MANAGER | PreAuthorize hasRole('ADMIN') hoặc hasRole('MANAGER'). Có quyền xem profile của mọi user |
| `GET /api/student/profile` | STUDENT | PreAuthorize hasRole('STUDENT'). Chỉ được xem profile của chính mình (dựa trên userId từ JWT) |

---

*File được tạo bởi chuyên gia Sequence Diagram cho hệ thống CampusLife.*
