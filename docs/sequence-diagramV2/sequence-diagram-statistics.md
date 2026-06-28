# Sequence Diagram - Statistics Module (Thống kê)

Hệ thống: **CampusLife** (Spring Boot + React)

Module: **Statistics (Thống kê)**

Các participant chuẩn: `Admin`, `Client` (React App), `Controller`, `Service`, `Repository`, `Database`

---

## 1. Dashboard Tổng quan (K.43)

**Endpoint:** `GET /api/admin/statistics/dashboard`

**Mô tả:** Admin truy cập dashboard → hệ thống tổng hợp đồng thời nhiều metrics từ nhiều nguồn dữ liệu → trả về `DashboardResponse` tổng hợp.

```mermaid
sequenceDiagram
    autonumber
    participant A as Admin/Manager
    participant C as Client (React)
    participant Ctrl as StatisticsController
    participant S as StatisticsService
    participant AR as ActivityRepository
    participant RR as RegistrationRepository
    participant SR as ScoreRepository
    participant DB as Database

    Note over A, DB: === LUỒNG 1: DASHBOARD TỔNG QUAN (K.43) ===
    Note over A, DB: Admin truy cập Dashboard để xem tổng quan hệ thống

    A->>C: Truy cập trang Dashboard
    C->>Ctrl: GET /api/admin/statistics/dashboard
    activate Ctrl

    Ctrl->>S: getDashboardStatistics()
    activate S

    Note over S, DB: 1. Tổng hợp số liệu Hoạt động
    S->>AR: countTotalActivities()
    AR->>DB: SELECT COUNT(*) FROM activities
    DB-->>AR: totalCount
    AR-->>S: total

    S->>AR: countByStatus("UPCOMING")
    AR->>DB: SELECT COUNT(*) FROM activities WHERE status = 'UPCOMING'
    DB-->>AR: upcomingCount
    AR-->>S: upcoming

    S->>AR: countByStatus("ONGOING")
    AR->>DB: SELECT COUNT(*) FROM activities WHERE status = 'ONGOING'
    DB-->>AR: ongoingCount
    AR-->>S: ongoing

    S->>AR: countByStatus("COMPLETED")
    AR->>DB: SELECT COUNT(*) FROM activities WHERE status = 'COMPLETED'
    DB-->>AR: completedCount
    AR-->>S: completed

    Note over S, DB: 2. Tổng hợp số liệu Đăng ký tham gia
    S->>RR: countTotalRegistrations()
    RR->>DB: SELECT COUNT(*) FROM registrations
    DB-->>RR: totalRegistrations
    RR-->>S: totalRegistrations

    S->>RR: countByStatus("APPROVED")
    RR->>DB: SELECT COUNT(*) FROM registrations WHERE status = 'APPROVED'
    DB-->>RR: approvedCount
    RR-->>S: approved

    S->>RR: countAttended()
    RR->>DB: SELECT COUNT(*) FROM registrations WHERE attended = true
    DB-->>RR: attendedCount
    RR-->>S: attended

    Note over S, DB: 3. Tổng điểm đã phát
    S->>SR: sumTotalScoreDistributed()
    SR->>DB: SELECT SUM(score) FROM score_records
    DB-->>SR: totalScore
    SR-->>S: totalScoreDistributed

    Note over S, DB: 4. Top hoạt động (Most Registered)
    S->>AR: findTopActivitiesByRegistration(limit)
    AR->>DB: SELECT a.*, COUNT(r.id) as reg_count FROM activities a JOIN registrations r ON a.id = r.activity_id GROUP BY a.id ORDER BY reg_count DESC LIMIT ?
    DB-->>AR: topRegisteredActivities
    AR-->>S: List<ActivityStatDTO>

    Note over S, DB: 5. Top hoạt động (Most Attended)
    S->>AR: findTopActivitiesByAttendance(limit)
    AR->>DB: SELECT a.*, COUNT(r.id) as attend_count FROM activities a JOIN registrations r ON a.id = r.activity_id WHERE r.attended = true GROUP BY a.id ORDER BY attend_count DESC LIMIT ?
    DB-->>AR: topAttendedActivities
    AR-->>S: List<ActivityStatDTO>

    Note over S, DB: 6. Top sinh viên (Highest Score)
    S->>SR: findTopStudentsByTotalScore(limit)
    SR->>DB: SELECT s.*, SUM(sr.score) as total_score FROM students s JOIN score_records sr ON s.id = sr.student_id GROUP BY s.id ORDER BY total_score DESC LIMIT ?
    DB-->>SR: topStudents
    SR-->>S: List<StudentScoreDTO>

    S->>S: aggregateAllMetricsIntoDashboardResponse()

    S-->>Ctrl: DashboardResponse
    deactivate S

    Ctrl-->>C: 200 OK + DashboardResponse (JSON)
    deactivate Ctrl

    C-->>A: Hiển thị Dashboard với các widgets, biểu đồ, bảng xếp hạng
```

---

## 2. Thống kê Hoạt động & Sinh viên (K.44)

**Endpoints:**
- `GET /api/admin/statistics/activities`
- `GET /api/admin/statistics/students`

**Mô tả:** Admin xem thống kê chi tiết về hoạt động và sinh viên, có thể lọc theo khoảng thời gian, phân loại theo kỳ/department/lớp, có phân trang.

```mermaid
sequenceDiagram
    autonumber
    participant A as Admin/Manager
    participant C as Client (React)
    participant Ctrl as StatisticsController
    participant S as StatisticsService
    participant AR as ActivityRepository
    participant RR as RegistrationRepository
    participant SR as ScoreRepository
    participant StR as StudentRepository
    participant SemR as SemesterRepository
    participant DB as Database

    Note over A, DB: === LUỒNG 2A: THỐNG KÊ HOẠT ĐỘNG (K.44) ===
    Note over A, DB: Admin lọc và xem thống kê chi tiết các hoạt động

    A->>C: Chọn bộ lọc thời gian / kỳ học / department
    C->>Ctrl: GET /api/admin/statistics/activities?startDate=...&endDate=...&semesterId=...&department=...
    activate Ctrl

    Ctrl->>S: getActivityStatistics(startDate, endDate, semesterId, department)
    activate S

    alt Có semesterId
        S->>SemR: findById(semesterId)
        SemR->>DB: SELECT * FROM semesters WHERE id = ?
        DB-->>SemR: semester
        SemR-->>S: Semester
    end

    S->>AR: findActivitiesByFilter(startDate, endDate, semesterId, department)
    AR->>DB: SELECT * FROM activities WHERE ... ORDER BY start_date
    DB-->>AR: List<Activity>
    AR-->>S: List<Activity>

    loop Với mỗi Activity
        S->>RR: countRegistrationsByActivityId(activityId)
        RR->>DB: SELECT COUNT(*) FROM registrations WHERE activity_id = ?
        DB-->>RR: registrationCount
        RR-->>S: registrationCount

        S->>RR: countApprovedByActivityId(activityId)
        RR->>DB: SELECT COUNT(*) FROM registrations WHERE activity_id = ? AND status = 'APPROVED'
        DB-->>RR: approvedCount
        RR-->>S: approvedCount

        S->>RR: countAttendedByActivityId(activityId)
        RR->>DB: SELECT COUNT(*) FROM registrations WHERE activity_id = ? AND attended = true
        DB-->>RR: attendedCount
        RR-->>S: attendedCount

        S->>SR: getAverageScoreByActivityId(activityId)
        SR->>DB: SELECT AVG(score) FROM score_records WHERE activity_id = ?
        DB-->>SR: avgScore
        SR-->>S: avgScore
    end

    Note over S, DB: Tính tỷ lệ tham gia và group by
    S->>S: calculateParticipationRate(attended, approved)
    S->>S: groupBySemesterAndDepartment(activities)

    S-->>Ctrl: List<ActivityStatisticsDTO>
    deactivate S

    Ctrl-->>C: 200 OK + List<ActivityStatisticsDTO> (JSON)
    deactivate Ctrl
    C-->>A: Hiển thị bảng thống kê hoạt động + biểu đồ

    Note over A, DB: === LUỒNG 2B: THỐNG KÊ SINH VIÊN (K.44) ===
    Note over A, DB: Admin xem thống kê sinh viên theo lớp/department

    A->>C: Chọn bộ lọc lớp / department / kỳ học + phân trang
    C->>Ctrl: GET /api/admin/statistics/students?classId=...&department=...&semesterId=...&page=...&size=...
    activate Ctrl

    Ctrl->>S: getStudentStatistics(classId, department, semesterId, pageable)
    activate S

    alt Có semesterId
        S->>SemR: findById(semesterId)
        SemR->>DB: SELECT * FROM semesters WHERE id = ?
        DB-->>SemR: semester
        SemR-->>S: Semester
    end

    S->>StR: findStudentsByFilter(classId, department, pageable)
    StR->>DB: SELECT * FROM students WHERE ... LIMIT ? OFFSET ?
    DB-->>StR: Page<Student>
    StR-->>S: Page<Student>

    S->>StR: countTotalStudentsByFilter(classId, department)
    StR->>DB: SELECT COUNT(*) FROM students WHERE ...
    DB-->>StR: totalCount
    StR-->>S: totalCount

    loop Với mỗi Student
        S->>SR: sumTotalScoreByStudentId(studentId, semesterId)
        SR->>DB: SELECT SUM(score) FROM score_records WHERE student_id = ? AND (semester_id = ? OR ? IS NULL)
        DB-->>SR: totalScore
        SR-->>S: totalScore

        S->>RR: countAttendedActivitiesByStudentId(studentId, semesterId)
        RR->>DB: SELECT COUNT(DISTINCT activity_id) FROM registrations WHERE student_id = ? AND attended = true ...
        DB-->>RR: attendedActivityCount
        RR-->>S: attendedActivityCount

        S->>RR: countApprovedActivitiesByStudentId(studentId, semesterId)
        RR->>DB: SELECT COUNT(DISTINCT activity_id) FROM registrations WHERE student_id = ? AND status = 'APPROVED' ...
        DB-->>RR: approvedActivityCount
        RR-->>S: approvedActivityCount

        S->>S: calculateParticipationRate(attended, approved)
    end

    Note over S, DB: Tính xếp hạng và group by
    S->>S: calculateStudentRanking(students)
    S->>S: groupByClassAndDepartment(students)

    S-->>Ctrl: Page<StudentStatisticsDTO>
    deactivate S

    Ctrl-->>C: 200 OK + Page<StudentStatisticsDTO> (JSON)
    deactivate Ctrl
    C-->>A: Hiển thị bảng thống kê sinh viên + phân trang + biểu đồ
```

---

## 3. Phân tích Nguồn điểm (K.45)

**Endpoint:** `GET /api/admin/statistics/scores/breakdown?semesterId=...`

**Mô tả:** Admin chọn kỳ học → hệ thống lấy tất cả `ScoreRecord` trong kỳ → group by `sourceType` (ACTIVITY, MINIGAME, MILESTONE, BONUS) → tính tổng điểm và % đóng góp → trả về dữ liệu biểu đồ phân tích.

```mermaid
sequenceDiagram
    autonumber
    participant A as Admin/Manager
    participant C as Client (React)
    participant Ctrl as StatisticsController
    participant S as StatisticsService
    participant SR as ScoreRepository
    participant SemR as SemesterRepository
    participant DB as Database

    Note over A, DB: === LUỒNG 3: PHÂN TÍCH NGUỒN ĐIỂM (K.45) ===
    Note over A, DB: Admin phân tích cấu trúc nguồn điểm theo kỳ học

    A->>C: Chọn kỳ học từ dropdown → click "Phân tích"
    C->>Ctrl: GET /api/admin/statistics/scores/breakdown?semesterId=...
    activate Ctrl

    Ctrl->>S: getScoreBreakdownBySemester(semesterId)
    activate S

    S->>SemR: findById(semesterId)
    SemR->>DB: SELECT * FROM semesters WHERE id = ?
    DB-->>SemR: semester
    SemR-->>S: Semester (validate tồn tại)

    S->>SR: findAllScoreRecordsBySemesterId(semesterId)
    SR->>DB: SELECT * FROM score_records WHERE semester_id = ?
    DB-->>SR: List<ScoreRecord>
    SR-->>S: List<ScoreRecord>

    Note over S, DB: Group by sourceType và tính tổng điểm
    S->>S: groupBySourceType(scoreRecords)

    Note over S, DB: Các sourceType: ACTIVITY, MINIGAME, MILESTONE, BONUS
    S->>S: sumScoreByType("ACTIVITY")
    S->>S: sumScoreByType("MINIGAME")
    S->>S: sumScoreByType("MILESTONE")
    S->>S: sumScoreByType("BONUS")

    S->>S: calculateTotalScore(sumByType)

    Note over S, DB: Tính phần trăm đóng góp cho mỗi loại
    S->>S: calculateContributionPercentage(typeScore, totalScore)

    Note over S, DB: Chuẩn bị dữ liệu cho biểu đồ
    S->>S: buildBreakdownChartData(breakdownMap, percentages)

    S-->>Ctrl: ScoreBreakdownResponse
    deactivate S

    Ctrl-->>C: 200 OK + ScoreBreakdownResponse (JSON)
    deactivate Ctrl

    C-->>A: Hiển thị biểu đồ tròn/cột phân tích nguồn điểm
```

---

## Tóm tắt Thành phần và Chức năng

### Thành phần tham gia

| Thành phần | Vai trò | Chức năng chính |
|---|---|---|
| **Admin/Manager** | Actor | Người dùng cuối có quyền truy cập module Thống kê. Tương tác với giao diện React để xem dashboard, lọc dữ liệu, phân tích. |
| **Client (React)** | Presentation Layer | Giao diện người dùng. Gửi request HTTP đến backend, nhận JSON response, render biểu đồ, bảng dữ liệu, widgets. |
| **StatisticsController** | Controller Layer | Nhận request từ Client, định tuyến đến Service phù hợp, trả về ResponseEntity. Xử lý validate input (semesterId, date range, pagination). |
| **StatisticsService** | Business Logic Layer | Chứa toàn bộ logic tính toán thống kê: tổng hợp metrics, group by, tính tỷ lệ, xếp hạng, phân trang, build DTO response. Điều phối nhiều Repository. |
| **ActivityRepository** | Data Access Layer | Truy vấn dữ liệu hoạt động (Activity): count, find by filter, top activities by registration/attendance. |
| **RegistrationRepository** | Data Access Layer | Truy vấn dữ liệu đăng ký (Registration): count registrations, approved, attended, theo activity hoặc student. |
| **ScoreRepository** | Data Access Layer | Truy vấn dữ liệu điểm (ScoreRecord): tổng điểm, average, sum by sourceType, top students by score. |
| **StudentRepository** | Data Access Layer | Truy vấn dữ liệu sinh viên (Student): find by filter, phân trang, count total. |
| **SemesterRepository** | Data Access Layer | Truy vấn dữ liệu kỳ học (Semester): validate semesterId, lấy thông tin kỳ học để giới hạn phạm vi thống kê. |
| **Database** | Persistence Layer | Cơ sở dữ liệu (MySQL/PostgreSQL). Lưu trữ và trả về dữ liệu thô theo truy vấn SQL từ các Repository. |

### Chức năng từng Sequence

| STT | Sequence | Mã chức năng | Endpoint | Đặc điểm kỹ thuật |
|---|---|---|---|---|
| 1 | **Dashboard Tổng quan** | K.43 | `GET /api/admin/statistics/dashboard` | Tổng hợp song song nhiều metrics từ nhiều repository khác nhau. Trả về 1 `DashboardResponse` duy nhất chứa tất cả số liệu. Không có tham số lọc. |
| 2 | **Thống kê Hoạt động** | K.44 | `GET /api/admin/statistics/activities` | Hỗ trợ lọc theo thời gian, kỳ học, department. Tính toán chi tiết: registrations, approved, attended, participation rate, average score. Group by semester/department. Trả về list. |
| 2 | **Thống kê Sinh viên** | K.44 | `GET /api/admin/statistics/students` | Hỗ trợ lọc theo lớp, department, kỳ học. Tính toán: total score, attended activities, participation rate, ranking. Có phân trang (page/size). Group by class/department. |
| 3 | **Phân tích Nguồn điểm** | K.45 | `GET /api/admin/statistics/scores/breakdown` | Yêu cầu `semesterId`. Lấy tất cả `ScoreRecord` trong kỳ → group by `sourceType` (ACTIVITY, MINIGAME, MILESTONE, BONUS). Tính tổng điểm và phần trăm đóng góp. Trả về dữ liệu biểu đồ. |

### Ghi chú thiết kế

1. **Parallel Aggregation (K.43):** Trong Dashboard, các metrics được tổng hợp từ nhiều repository. Trong triển khai thực tế có thể tối ưu bằng cách dùng `CompletableFuture` hoặc native SQL JOIN để giảm số lượng query.
2. **Pagination (K.44 - Students):** Thống kê sinh viên có phân trang vì dữ liệu có thể rất lớn. Thống kê hoạt động thường ít hơn nên có thể trả về toàn bộ list.
3. **Filter by Semester:** Cả 3 sequence đều hỗ trợ hoặc bắt buộc `semesterId` để giới hạn phạm vi dữ liệu, tránh truy vấn toàn bộ bảng lớn.
4. **DTO Pattern:** Service layer tổng hợp dữ liệu thô từ Entity thành các DTO (`DashboardResponse`, `ActivityStatisticsDTO`, `StudentStatisticsDTO`, `ScoreBreakdownResponse`) trước khi trả về Controller.
5. **Read-Only Operations:** Tất cả các sequence trong module Statistics đều là thao tác đọc (READ). Không có thay đổi dữ liệu (CREATE/UPDATE/DELETE), phù hợp để caching và tối ưu truy vấn.
