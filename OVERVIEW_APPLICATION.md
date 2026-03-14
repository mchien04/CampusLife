# 📱 CampusLife - Báo Cáo Sơ Lược Ứng Dụng

## 🎯 Tổng Quan

**CampusLife** là một nền tảng quản lý hoạt động sinh viên toàn diện được xây dựng bằng **Spring Boot 3** với cơ sở dữ liệu **MySQL**. Ứng dụng giúp sinh viên:
- Đăng ký tham gia các hoạt động ngoại khóa
- Tham gia minigame giáo dục
- Nộp báo cáo/minh chứng hoàn thành
- Xem điểm tích lũy theo các hạng mục
- Nhận thông báo nhắc nhở

**Mục đích:** Tính điểm rèn luyện dựa trên hoạt động thực tế của sinh viên, bao gồm:
- Điểm Rèn Luyện (REN_LUYEN)
- Điểm Công Tác Xã Hội (CONG_TAC_XA_HOI)
- Điểm Chuyên Đề Doanh Nghiệp (CHUYEN_DE)

---

## 📊 Cấu Trúc Dữ Liệu

### 🔗 Các Bảng Chính

```
┌─────────────────────────────────────────┐
│ User (Người dùng: Admin, Giảng viên)     │
│ - email, password, role                  │
└────────┬────────────────────────────────┘
         │
         ├──→ Student (Sinh viên)
         │    - studentCode, fullName
         │    - department, class
         │    - phone, dob, gender, avatar
         │
         └──→ Department (Khoa)
              - name, type

┌────────────────────────────────────────────────────┐
│ Semester (Học kỳ)                                   │
│ - academicYear, semesterNumber                      │
│ - startDate, endDate                                │
└────────────────────────────────────────────────────┘
         │
         └──→ StudentScore (Bảng điểm sinh viên)
              - student, semester
              - scoreType (REN_LUYEN / CONG_TAC_XA_HOI / CHUYEN_DE)
              - score (tổng điểm)

┌────────────────────────────────────────────────────┐
│ Activity (Hoạt động/Sự kiện)                        │
│ - name, description                                 │
│ - type (SUKIEN, MINIGAME, CONG_TAC_XA_HOI, etc.)   │
│ - scoreType (loại điểm cộng)                        │
│ - maxPoints, requiresSubmission                     │
│ - startDate, endDate, registrationDates             │
└───────┬──────────────────────────────────────────┘
        │
        ├──→ ActivityRegistration (Đăng ký tham gia)
        │    - student, status
        │    - registeredDate, ticketCode
        │
        ├──→ ActivityParticipation (Tham gia thực tế)
        │    - participationType (CHECK_IN, COMPLETE, etc.)
        │    - pointsEarned (điểm được cộng)
        │    - isCompleted (true/false/null)
        │    - date, checkInTime, checkOutTime
        │
        └──→ TaskAssignment (Bài tập nộp)
             - submissions

┌────────────────────────────────────────────────────┐
│ ActivitySeries (Chuỗi sự kiện - Dự án lớn)        │
│ - name, description                                 │
│ - scoreType, milestonePoints                        │
│ - registrationDates, ticketQuantity                 │
│ - requiresApproval                                  │
└────────────────────────────────────────────────────┘
         │
         └──→ StudentSeriesProgress (Tiến độ sinh viên)
              - student, series
              - completedActivities, milestoneReached

┌────────────────────────────────────────────────────┐
│ MiniGame (Mini-game học tập)                        │
│ - title, description                                │
│ - questionCount, timeLimit                          │
│ - type (QUIZ, MATCH_PAIR, etc.)                     │
│ - rewardPoints (điểm thưởng nếu PASS)              │
│ - requiredCorrectAnswers, maxAttempts               │
└───────┬──────────────────────────────────────────┘
        │
        ├──→ MiniGameQuiz (Câu hỏi)
        │    - content, correctAnswer
        │    - options (MiniGameQuizOption)
        │
        └──→ MiniGameAttempt (Lần làm quiz)
             - student, attemptStatus
             - correctAnswerCount, score

┌────────────────────────────────────────────────────┐
│ TaskSubmission (Nộp bài)                            │
│ - student, taskAssignment                           │
│ - submissionFile, submissionStatus                  │
│ - isApproved, score                                 │
└────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────┐
│ Notification (Thông báo)                            │
│ - recipient, type, status                           │
│ - title, content, isRead                            │
└────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────┐
│ ScoreHistory (Lịch sử ghi điểm)                     │
│ - student, semester, scoreType                      │
│ - oldScore, newScore, reason                        │
│ - changedAt                                         │
└────────────────────────────────────────────────────┘
```

---

## 🏗️ Kiến Trúc Ứng Dụng

### Layers (Tầng)

```
┌─────────────────────────────────────────┐
│     REST API Controllers                 │  ← HTTP Request/Response
│   (ActivityController, StudentController) │
└──────────────────┬──────────────────────┘
                   │
                   ↓
┌──────────────────────────────────────────────────┐
│     Business Logic (Services)                     │  ← Xử lý logic
│   - ActivityService                               │
│   - ActivityRegistrationService                   │
│   - StudentScoreService                           │
│   - MiniGameService                               │
│   - TaskSubmissionService                         │
│   - EmailService, NotificationService             │
│   - SemesterHelperService (xác định học kỳ)      │
└──────────────────┬───────────────────────────────┘
                   │
                   ↓
┌──────────────────────────────────────────────────┐
│     Database Access (Repository - JPA)            │  ← SQL Queries
│   - StudentRepository                             │
│   - ActivityRepository                            │
│   - StudentScoreRepository                        │
│   - etc...                                         │
└──────────────────┬───────────────────────────────┘
                   │
                   ↓
              ┌─────────────┐
              │  MySQL DB   │
              └─────────────┘
```

### Thư Mục Chính

```
src/main/java/vn/campuslife/
├── entity/                    # 32 lớp đại diện bảng trong DB
├── repository/                # 30+ interfaces JPA (truy vấn)
├── service/                   # Business logic (27 interfaces)
│   └── impl/                  # Triển khai service
├── controller/                # REST API endpoints (24+ controllers)
├── enumeration/               # 15 loại liệt kê (ScoreType, ActivityType, etc.)
├── config/                    # Cấu hình (Security, Database, etc.)
├── filter/                    # JWT Authentication Filter
├── model/                     # DTO (Data Transfer Objects)
└── util/                      # Các lớp tiện ích
```

---

## 🎮 Các Chức Năng Chính

### 1️⃣ **Quản Lý Người Dùng & Xác Thực**
- **Đăng ký/Đăng nhập**: Sử dụng JWT token
- **Quên mật khẩu**: Gửi email reset password
- **Quản lý hồ sơ**: Sinh viên cập nhật thông tin cá nhân (avatar, địa chỉ, SĐT)
- **Phân quyền**: Admin, Giảng viên, Sinh viên

### 2️⃣ **Quản Lý Hoạt Động**
- **Tạo sự kiện**: Admin tạo hoạt động với thông tin:
  - Tên, mô tả, thời gian, địa điểm
  - Loại (Sự kiện thường, Minigame, Công tác xã hội, Chuyên đề doanh nghiệp)
  - Điểm tối đa, có yêu cầu nộp báo cáo
  - Hạn đăng ký

- **Đăng ký tham gia**: Sinh viên đăng ký tham gia sự kiện
  - Tạo ticket code để check-in
  - Xem trạng thái đăng ký

- **Check-in/Check-out**: Sinh viên check-in khi tới sự kiện
  - Thường: Quét mã QR hoặc nhập mã
  - Tự động cập nhật tham gia (pointsEarned)

### 3️⃣ **Chuỗi Sự Kiện (Activity Series)**
- **Tạo chuỗi sự kiện**: Tập hợp nhiều hoạt động thành một dự án lớn
  - Ví dụ: "Dự án tổng vệ sinh" gồm 5 hoạt động liên quan
  
- **Milestone Points**: Thưởng điểm extra khi hoàn thành mốc
  - Ví dụ: Hoàn thành 3 hoạt động → cộng 5 điểm thưởng
  - Hoàn thành 4 hoạt động → cộng 7 điểm thưởng
  - Hoàn thành 5 hoạt động → cộng 10 điểm thưởng

- **Tiến độ theo dõi**: Xem sinh viên đã hoàn thành bao nhiêu hoạt động

### 4️⃣ **Minigame Học Tập**
- **Tạo minigame**: Admin tạo quiz với câu hỏi trắc nghiệm
  - Ví dụ: "Quiz Đạo Đức Cộng Hòa Xã Hội Chủ Nghĩa"
  - Có thời gian giới hạn
  - Điểm thưởng nếu đạt

- **Làm minigame**: Sinh viên làm bài
  - PASS lần đầu: Cộng điểm thưởng (rewardPoints)
  - Re-attempt: Không cộng thêm, chỉ update cao nhất
  - Có thể giới hạn số lần làm

### 5️⃣ **Nộp Báo Cáo/Minh Chứng**
- **Yêu cầu nộp**: Một số sự kiện bắt buộc nộp báo cáo
- **Nộp bài**: Sinh viên upload file (ảnh, PDF, etc.)
- **Chấm bài**: Giảng viên xem, chấm điểm, phê duyệt
- **Cập nhật điểm**: Khi chấm đạt → cộng điểm vào StudentScore

### 6️⃣ **Tính Điểm & Bảng Điểm**
- **Ba loại điểm**:
  - **REN_LUYEN**: Rèn luyện (từ các sự kiện thường)
  - **CONG_TAC_XA_HOI**: Công tác xã hội
  - **CHUYEN_DE**: Chuyên đề doanh nghiệp (có thể cộng kép vào REN_LUYEN nếu loại sự kiện là CHUYEN_DE_DOANH_NGHIEP)

- **Tính điểm từ nhiều nguồn**:
  - Tham gia sự kiện (participation points)
  - Nộp báo cáo được chấm (submission score)
  - Minigame PASS (reward points)
  - **Milestone từ series**: Thưởng extra khi hoàn thành mốc

- **Giữ nguyên milestone**: Khi một sự kiện bị xóa hoặc điểm được cập nhật, milestone points phải được bảo toàn

- **Theo học kỳ**: Mỗi StudentScore được ghi theo `(student, semester, scoreType)`

### 7️⃣ **Thông Báo & Email**
- **Email**: Gửi khi đăng ký thành công, reset mật khẩu, chấm bài hoàn thành
- **Thông báo trong app**: Nhắc nhở sự kiện sắp diễn ra, cập nhật điểm
- **Push notification**: Thông báo qua Firebase Cloud Messaging (FCM)

### 8️⃣ **Thống Kê & Báo Cáo**
- **Xem điểm cá nhân**: Sinh viên xem tổng điểm từng loại theo học kỳ
- **Lịch sử ghi điểm** (ScoreHistory): Ghi log từng lần cộng/trừ điểm
- **Thống kê quản trị**: Admin xem thống kê hoạt động, tỷ lệ tham gia

---

## 🔄 Quy Trình Cộng Điểm

### Quy Trình Chung

```
1. Sinh viên đăng ký sự kiện
   ↓
2. Sinh viên tham gia (check-in)
   ↓
3. Tham gia được ghi nhận → pointsEarned được set
   ↓
4. (Nếu requiresSubmission = true)
   └→ Sinh viên nộp báo cáo
      └→ Giảng viên chấm → score được set
      └→ isCompleted = true/false
   (Nếu requiresSubmission = false)
   └→ Tham gia = hoàn thành, isCompleted = true
   ↓
5. StudentScore được cập nhật (cộng pointsEarned vào score)
   ↓
6. Nếu thuộc series và đạt mốc → milestone points được cộng
   ↓
7. ScoreHistory ghi lại lần cộng điểm này
```

### Ví Dụ Cụ Thể

**Trường hợp 1: Sự kiện thường (SUKIEN)**
```
Activity: "Lớp học thêm tiếng Anh"
- scoreType: REN_LUYEN
- maxPoints: 5
- requiresSubmission: false

Sinh viên A:
1. Đăng ký → ActivityRegistration tạo
2. Check-in → ActivityParticipation: participationType=CHECK_IN
3. Check-out → pointsEarned = 5
4. StudentScore (REN_LUYEN, học kỳ 1): score += 5
5. ScoreHistory: "Cộng 5 điểm từ sự kiện Lớp học..."
```

**Trường hợp 2: Sự kiện có nộp báo cáo (requiresSubmission=true)**
```
Activity: "Báo cáo dự án"
- scoreType: CONG_TAC_XA_HOI
- maxPoints: 10
- requiresSubmission: true

Sinh viên B:
1. Đăng ký → ActivityRegistration tạo
2. Check-in → ActivityParticipation: participationType=CHECK_IN
3. Nộp báo cáo → TaskSubmission: submissionStatus=PENDING
4. Giảng viên chấm → score=8, isApproved=true
5. StudentScore (CONG_TAC_XA_HOI, học kỳ 1): score += 8
6. ScoreHistory: "Cộng 8 điểm từ báo cáo dự án..."
```

**Trường hợp 3: Minigame**
```
MiniGame: "Quiz Tư Tưởng"
- rewardPoints: 3
- requiredCorrectAnswers: 7 (trên 10 câu)

Sinh viên C:
1. Làm lần 1: Đúng 8/10 → PASS → MiniGameAttempt ghi nhận
2. StudentScore (REN_LUYEN, học kỳ 1): score += 3
3. Làm lần 2: Đúng 7/10 → PASS → MiniGameAttempt ghi nhận
   (Không cộng thêm vì đã PASS lần 1, chỉ update best score)
```

**Trường hợp 4: Series với Milestone**
```
ActivitySeries: "Dự án tổng vệ sinh"
- scoreType: REN_LUYEN
- milestonePoints: {"3": 5, "4": 7, "5": 10}

Sinh viên D:
1. Hoàn thành hoạt động 1 → score += 2
2. Hoàn thành hoạt động 2 → score += 2 (tổng: 4)
3. Hoàn thành hoạt động 3 → score += 2, + MILESTONE 5 (tổng: 11)
4. Hoàn thành hoạt động 4 → score += 2, + MILESTONE 7 (tổng: 20)
5. Hoàn thành hoạt động 5 → score += 2, + MILESTONE 10 (tổng: 32)
```

---

## 📁 Luồng Xử Lý Chính

### 1. Đăng Ký Tham Gia
```
ActivityRegistrationController.registerActivity()
  ↓
ActivityRegistrationService.registerActivity()
  ├→ Kiểm tra hạn đăng ký
  ├→ Tạo ActivityRegistration
  ├→ Tạo ticketCode
  ├→ Nếu series:
  │   └→ Tạo đăng ký cho tất cả activities trong series
  └→ Gửi email thông báo
```

### 2. Check-in
```
ActivityRegistrationController.checkIn() / checkInWithQR()
  ↓
ActivityRegistrationService.processCheckIn()
  ├→ Kiểm tra time (phải trong startDate - endDate)
  ├→ Tạo ActivityParticipation: participationType=CHECK_IN
  ├→ Set pointsEarned (nếu là sự kiện đơn)
  │   └→ Gọi updateStudentScoreFromParticipation()
  └→ Gửi notification
```

### 3. Tính Điểm (Cập nhật StudentScore)
```
updateStudentScoreFromParticipation() / createScoreFromSubmission()
  ├→ Xác định semester từ activity.startDate (SemesterHelperService)
  ├→ Kiểm tra ActivityParticipation đã hoàn thành (isCompleted=true)
  ├→ Tìm StudentScore (student, semester, scoreType)
  ├→ Nếu chưa tồn tại:
  │   └→ Tạo mới với score=0
  ├→ Cộng pointsEarned vào score
  ├→ Ghi ScoreHistory (oldScore, newScore, reason)
  └→ Save StudentScore
```

### 4. Cập Nhật Milestone (Series)
```
ActivitySeriesService.updateStudentProgress()
  ├→ Đếm số hoạt động hoàn thành
  ├→ Kiểm tra có đạt mốc nào không
  ├→ Nếu có mốc mới:
  │   ├→ Tính milestone points từ config
  │   ├→ Cộng vào StudentScore
  │   ├→ Ghi ScoreHistory
  │   └→ Ghi StudentSeriesProgress
  └→ Gửi notification "Bạn đạt mốc..."
```

---

## 🔐 Bảo Mật

- **JWT Token**: Xác thực người dùng qua token trong header
- **Spring Security**: Kiểm soát quyền truy cập (ROLE_ADMIN, ROLE_STUDENT, etc.)
- **Filter**: JwtAuthenticationFilter kiểm tra token mỗi request
- **Hashed Password**: Mật khẩu được mã hóa bằng BCrypt
- **Soft Delete**: Dữ liệu không xóa vật lý, chỉ đánh dấu `isDeleted=true`

---

## 🚀 Công Nghệ

| Thành Phần | Công Nghệ |
|---|---|
| **Framework** | Spring Boot 3.5.5 |
| **Database** | MySQL 8.0+ |
| **ORM** | JPA/Hibernate |
| **Authentication** | JWT (jjwt 0.11.5) |
| **Security** | Spring Security |
| **Email** | Spring Mail |
| **Push Notification** | Firebase Cloud Messaging (FCM) |
| **Code Generator** | Lombok |
| **API Documentation** | REST API |
| **Build Tool** | Maven |

---

## 📊 Luồng Điểm Chi Tiết

### Các Loại Điểm

| Loại | Viết tắt | Mô tả | Nguồn |
|---|---|---|---|
| **Rèn Luyện** | REN_LUYEN | Từ các hoạt động ngoại khóa | SUKIEN, MINIGAME (nếu type=SUKIEN) |
| **Công Tác Xã Hội** | CONG_TAC_XA_HOI | Từ các hoạt động xã hội | ActivityType.CONG_TAC_XA_HOI |
| **Chuyên Đề** | CHUYEN_DE | Từ hoạt động chuyên đề doanh nghiệp | ActivityType.CHUYEN_DE_DOANH_NGHIEP |

### Cách Xác Định Semester

Khi cộng điểm, hệ thống dùng `SemesterHelperService.getSemesterForActivity(activity)`:

```java
Semester semester = semesterHelperService.getSemesterForActivity(activity);
```

Nguyên tắc:
- Dựa vào `activity.startDate` để xác định thuộc học kỳ nào
- Nếu startDate nằm giữa `semester.startDate` và `semester.endDate` → thuộc semester đó
- Nếu startDate nằm ngoài → tìm semester gần nhất hoặc semester mở

### Bảo Toàn Milestone

Khi cập nhật điểm (ví dụ: sinh viên xóa participation):

```
oldScore = 20 (bao gồm: 15 từ participations + 5 milestone)
↓
Xóa participation -5 points
↓
oldParticipationScore = 15
milestonePoints = 20 - 15 = 5 ✓ (bảo toàn)
↓
newScore = 10 (new participation points) + 5 (milestone) = 15
```

---

## 📌 Các Tính Năng Đặc Biệt

### ✅ Dual-Score (CHUYEN_DE_DOANH_NGHIEP)

Khi activity có `ActivityType.CHUYEN_DE_DOANH_NGHIEP`:
- Cộng điểm vào cả **REN_LUYEN** và **CHUYEN_DE**
- Không phải lỗi, đây là thiết kế có chủ ý

### ✅ Auto-Score Init

Khi tạo semester mới:
- Tự động tạo StudentScore cho tất cả sinh viên
- scoreType: REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE
- score: 0 (khởi tạo)

### ✅ Re-attempt Minigame

- PASS lần 1: Cộng reward points
- PASS lần 2+: Không cộng thêm, chỉ update best score
- Được kiểm soát bằng `MiniGame.maxAttempts`

### ✅ Series Milestone

- Khi hoàn thành mốc số X activity → thưởng extra points
- Config dạng JSON: `{"3": 5, "4": 7, "5": 10}`
- Milestone points được cộng riêng, không trùng với participation points

---

## 🎓 Lưu Ý Khi Sử Dụng

### Cho Admin
1. **Tạo semester** → Auto-init StudentScore cho tất cả sinh viên
2. **Tạo activity** → Set rõ `scoreType`, `maxPoints`, `requiresSubmission`
3. **Tạo series** → Cấu hình `milestonePoints` dạng JSON
4. **Tạo minigame** → Set `rewardPoints`, `requiredCorrectAnswers`, `maxAttempts`

### Cho Sinh Viên
1. **Đăng ký hoạt động** → Nhận ticketCode để check-in
2. **Check-in đúng giờ** → Tự động cộng điểm
3. **Nộp báo cáo (nếu yêu cầu)** → Upload file, chờ giáo viên chấm
4. **Xem bảng điểm** → Theo dõi tiến độ từng loại điểm

### Cho Giáo Viên
1. **Quản lý hoạt động** → Tạo, sửa, xóa hoạt động
2. **Check-in/Check-out** → Quét QR code, xác nhận tham gia
3. **Chấm báo cáo** → Xem file nộp, ghi điểm, phê duyệt

---

## 📞 Hỗ Trợ & Liên Lạc

- **Lỗi báo cáo**: Debug controller có sẵn
- **Cấu hình email**: Trong `application.properties`
- **Firebase FCM**: Cần setup Firebase project và credentials

---

**Phiên Bản**: 0.0.1-SNAPSHOT  
**Ngôn Ngữ**: Java 21  
**Framework**: Spring Boot 3.5.5  
**Cơ Sở Dữ Liệu**: MySQL  
**Tác Giả**: CampusLife Team  
**Cập Nhật**: 14/03/2026

