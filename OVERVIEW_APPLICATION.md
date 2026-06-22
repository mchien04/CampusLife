# 📱 CampusLife - Báo Cáo Sơ Lược Ứng Dụng

## Agent Quick Start Context

Đọc phần này trước khi bắt đầu một conversation mới. Đây là bản tóm lược nhanh để agent nắm dự án trong vài phút, trước khi đi sâu vào controller/service cụ thể.

### Dự Án Là Gì

CampusLife là backend **Spring Boot 3.5.5 / Java 21 / Maven / MySQL** cho hệ thống quản lý hoạt động sinh viên. Backend cung cấp REST API cho frontend/mobile để quản lý sinh viên, khoa/lớp, học kỳ, hoạt động ngoại khóa, đăng ký/check-in, bài nộp, điểm rèn luyện, minigame, bài viết sự kiện, thông báo, email, FCM và báo cáo.

Ba role chính hiện tại:
- `ADMIN`: quản trị hệ thống, user, học kỳ, dữ liệu nền.
- `MANAGER`: quản lý hoạt động, check-in, chấm điểm, minigame, bài viết, preparation.
- `STUDENT`: đăng ký hoạt động, check-in, nộp minh chứng, làm quiz, xem điểm/thông báo.

Nếu bổ sung role mới, lưu ý DB thực tế dùng MySQL `ENUM` cho `users.role`, nên phải có migration `ALTER TABLE users MODIFY COLUMN role ENUM(...)`.

### Cấu Trúc Cần Nhớ

```text
src/main/java/vn/campuslife/
├─ controller/      REST endpoint, không nên chứa business logic nặng
├─ service/         Service interface
├─ service/impl/    Business logic, transaction, tính điểm, duyệt tiền
├─ repository/      Spring Data JPA query
├─ entity/          JPA entity
├─ model/           DTO request/response
├─ enumeration/     Role/status/type enum
├─ config/          Security, CORS, Firebase, JPA, upload static files
├─ filter/          JwtAuthenticationFilter
├─ util/            JWT, email, Excel, ticket, URL helper
└─ exception/       GlobalExceptionHandler và custom exceptions
```

Các thư mục ngoài source:
- `db/migration/`: migration SQL chính.
- `docs/`: tài liệu API, sequence diagram, preparation reports, FE guide.
- `uploads/`: file runtime do app tạo, không nên xem là source code.
- `.github/workflows/`: CI/CD GitHub Actions.
- `Dockerfile`: build/deploy app, hiện dùng Java 21.

### Luồng Nghiệp Vụ Cốt Lõi

```text
ADMIN/MANAGER tạo học kỳ, khoa/lớp, activity/series/minigame
→ STUDENT đăng ký activity hoặc series
→ STUDENT check-in bằng ticket/code/QR hoặc nộp submission
→ MANAGER/ADMIN xác nhận/chấm điểm
→ Service cập nhật ActivityParticipation/TaskSubmission/MiniGameAttempt
→ ScoreRuleEngine tính toán điểm theo các quy tắc cấu hình (ActivityScoreRule)
→ Hệ thống ghi nhận ScoreEntry và cập nhật StudentScore
→ Gửi Notification/Email/FCM nếu nghiệp vụ yêu cầu
→ Statistics/export đọc dữ liệu đã ghi nhận
```

Ba loại điểm chính:
- `REN_LUYEN`
- `CONG_TAC_XA_HOI`
- `CHUYEN_DE`

Điểm có thể đến từ participation, graded submission, minigame reward và milestone của activity series. Hệ thống sử dụng mô hình Rule Engine (`ScoreRuleEngineImpl`) để kích hoạt và tính điểm theo `ActivityScoreRule`, sau đó ghi nhận qua `ScoreEntryService`. Khi sửa logic điểm, hãy kiểm tra `ScoreRuleEngineImpl`, `ScoreEntryServiceImpl`, `ActivitySeriesServiceImpl` và các service trigger điểm.

### Module Chính

- **Auth/User**: `AuthController`, `AuthServiceImpl`, `UserManagementController`, `UserManagementServiceImpl`, `JwtAuthenticationFilter`, `SecurityConfig`.
- **Academic**: năm học/học kỳ, auto-init `StudentScore`.
- **Student/Class/Department**: quản lý sinh viên, khoa, lớp, profile.
- **Activity**: tạo/sửa/xóa/publish hoạt động, ảnh hoạt động, tháng/upcoming.
- **Registration/Participation**: đăng ký, hủy đăng ký, check-in, QR, report, grading.
- **Submission/TaskAssignment**: giao task/nộp minh chứng/chấm bài.
- **ActivitySeries**: chuỗi hoạt động, đăng ký series, milestone progress/points.
- **MiniGame**: quiz, attempt, reward point, max attempts.
- **Score**: xem điểm, ranking, history, recalculate.
- **Notification/Email/DeviceToken/FCM**: thông báo trong app, email, push.
- **EventArticle**: bài viết sự kiện, category, tag, image, wishlist/waitlist.
- **Preparation**: bật chuẩn bị sự kiện, BTC, task, leader/member, workload, ngân sách, tạm ứng, chi phí, duyệt hai cấp, audit, export Excel/PDF.
- **Statistics**: dashboard và thống kê theo activity/student/score/series/minigame.

### Files Nên Mở Đầu Tiên Khi Debug

```text
pom.xml
src/main/resources/application.properties
src/main/java/vn/campuslife/config/SecurityConfig.java
src/main/java/vn/campuslife/entity/User.java
src/main/java/vn/campuslife/enumeration/Role.java
src/main/java/vn/campuslife/enumeration/ScoreType.java
src/main/java/vn/campuslife/enumeration/ActivityType.java
src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java
src/main/java/vn/campuslife/service/impl/ScoreRuleEngineImpl.java
src/main/java/vn/campuslife/service/impl/ScoreEntryServiceImpl.java
src/main/java/vn/campuslife/service/impl/PreparationServiceImpl.java
src/main/java/vn/campuslife/service/impl/PreparationFinanceServiceImpl.java
src/main/java/vn/campuslife/service/impl/PreparationExportServiceImpl.java
docs/preparation-overview.md
docs/preparation-fe-guide.md
```

### Quy Tắc Khi Sửa Code

- Controller chỉ điều phối request/response; logic nghiệp vụ đặt trong service.
- Method ghi dữ liệu nên có `@Transactional`.
- API mới phải cập nhật `SecurityConfig`.
- Không trả entity phức tạp trực tiếp nếu dễ dính lazy loading/recursive JSON; ưu tiên DTO trong `model/`.
- Thay đổi điểm phải đi qua `ScoreRuleEngine` và được ghi nhận bằng `ScoreEntry`.
- Thay đổi tài chính/preparation quan trọng phải ghi `AuditLog` nếu là hành động duyệt/phân bổ/chi tiêu.
- Không sửa migration cũ đã chạy; thêm migration mới.
- Không commit secret trong `application.properties`; production dùng env vars.
- Không coi `uploads/` là dữ liệu nguồn ổn định.
- Nếu thay đổi enum lưu trong MySQL `ENUM`, phải thêm migration `ALTER TABLE ... MODIFY COLUMN ... ENUM(...)`.

### Build/Test Nhanh

```bash
./mvnw test
./mvnw -DskipTests clean package
```

Trên Windows có thể dùng:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests clean package
```

CI hiện chạy test và package qua GitHub Actions. CD trigger Render deploy hook khi push `main`.

### Ghi Chú Hiện Trạng Quan Trọng

- `application.properties` vẫn có `spring.jpa.hibernate.ddl-auto=update`; production nên dựa vào migration rõ ràng hơn là Hibernate tự update schema.
- `users.role` trong DB thực tế là MySQL `ENUM`, không chỉ là `VARCHAR`.
- `PreparationExportServiceImpl.exportOperational` hiện có section task status trong Excel sheet `Tasks` và PDF section `Task List`.
- Nếu cần export `completionProofUrls`, hiện code chưa có field này trong `PreparationTask`; cần thêm entity field, migration, request/service cập nhật dữ liệu rồi mới thêm cột export.

---

## Chi Tiết Mô Hình & Nghiệp Vụ (Dành cho việc tra cứu sâu)

Phần này tóm lược cấu trúc dữ liệu và các luồng nghiệp vụ phức tạp để hỗ trợ xử lý bug hoặc phát triển tính năng mới.

### 1. Mô Hình Dữ Liệu Chính (Data Model)

```text
User (email, password, role) 
  ├── Student (studentCode, fullName, department, class)
  └── Department (name, type)

Semester (academicYear, semesterNumber, startDate, endDate)
  └── StudentScore (student, semester, scoreType, score)

Activity (name, type, scoreType, maxPoints, requiresSubmission)
  ├── ActivityRegistration (student, status, ticketCode)
  ├── ActivityParticipation (participationType, pointsEarned, isCompleted)
  └── TaskAssignment → TaskSubmission (submissionFile, score, isApproved)

ActivitySeries (name, milestonePoints)
  └── StudentSeriesProgress (completedActivities, milestoneReached)

MiniGame (questionCount, timeLimit, rewardPoints, maxAttempts)
  ├── MiniGameQuiz → options
  └── MiniGameAttempt (attemptStatus, score)

ScoreEntry (student, semester, scoreType, points, reason)
ActivityScoreRule (activityType, scoreType, triggerEvent, fixedPoints)
Notification (recipient, type, title, content)
```

### 2. Chi Tiết Logic Tính Điểm

**Quy trình ghi nhận điểm chung:**
1. Sinh viên đăng ký → `ActivityRegistration`
2. Tham gia (check-in) → `ActivityParticipation` (với `pointsEarned`)
3. Nếu `requiresSubmission = true`: nộp `TaskSubmission` → Giảng viên chấm điểm (`score`), set `isCompleted = true`. Nếu không yêu cầu nộp, check-in có thể được xem là hoàn thành tùy cấu hình.
4. Hệ thống đi qua `ScoreRuleEngine` để tính toán số điểm được cộng hoặc trừ.
5. Nếu thuộc chuỗi sự kiện (`ActivitySeries`) và đạt mốc tiến độ → `ScoreRuleEngine` xử lý trigger `SERIES_MILESTONE` để cộng `milestonePoints`.
6. Ghi lại `ScoreEntry` và cập nhật `StudentScore` thông qua `ScoreEntryService` mỗi khi có thay đổi điểm.

**Các nghiệp vụ đặc thù cần lưu ý:**
- **Xác định Học kỳ (Semester):** Sử dụng `SemesterHelperService.getSemesterForActivity(activity)` dựa vào `startDate` của hoạt động để xác định điểm sẽ được cộng vào học kỳ nào, kể cả khi hiện tại đang ở học kỳ khác.
- **Dual-Score (Cộng điểm kép):** Nếu `ActivityType` là `CHUYEN_DE_DOANH_NGHIEP`, hệ thống sẽ cộng điểm vào cả 2 loại: `REN_LUYEN` và `CHUYEN_DE`.
- **Bảo toàn Milestone:** Nếu sinh viên bị hủy một `participation` hoặc update lại điểm, các điểm thưởng (milestone) đã đạt từ series không bị ảnh hưởng. Logic update điểm tổng phải tách bạch phần điểm tham gia sự kiện với điểm milestone.
- **Re-attempt Minigame:** Sinh viên vượt qua (`PASS`) minigame lần đầu sẽ được cộng `rewardPoints`. Các lần sau chỉ cập nhật điểm cao nhất (`best score`) và số câu đúng, không cộng dồn thêm điểm vào `StudentScore`.
- **Auto-Score Init:** Khi Admin tạo `Semester` mới, hệ thống tự khởi tạo các bản ghi `StudentScore` với `score = 0` cho toàn bộ sinh viên đang hoạt động.

### 3. Quy Ước Hệ Thống Khác

- **Bảo mật:** Sử dụng JWT (qua `JwtAuthenticationFilter`) và Spring Security (phân quyền theo `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_STUDENT`).
- **Xóa Dữ Liệu:** Đa số sử dụng **Soft Delete** (`isDeleted = true`) thay vì xóa vật lý khỏi database.
- **Mật khẩu:** Mã hóa một chiều bằng thuật toán BCrypt.
- **Thông báo:** Hệ thống hỗ trợ đa kênh qua App Notification nội bộ, Email (Spring Mail) và Push Notification (Firebase Cloud Messaging - FCM).

---
**Phiên Bản**: 0.0.1-SNAPSHOT | **Ngôn Ngữ**: Java 21 | **Framework**: Spring Boot 3.5.5 | **Cơ Sở Dữ Liệu**: MySQL
