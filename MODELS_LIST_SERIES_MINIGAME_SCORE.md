# Danh Sách Các Model Liên Quan Đến Chức Năng Mới

## 📋 TỔNG QUAN

Tài liệu này liệt kê **TẤT CẢ** các model (Entity, Request, Response, Enum) liên quan đến các chức năng mới:
- **Chuỗi sự kiện (Activity Series)**
- **Minigame Quiz**
- **Logic tính điểm đã cập nhật**
- **Chuyên đề doanh nghiệp (Dual Score)**

---

## 1️⃣ ENTITY MODELS (JPA Entities)

### A. Chuỗi Sự Kiện (Activity Series)

#### 1. `ActivitySeries`
**Path:** `src/main/java/vn/campuslife/entity/ActivitySeries.java`  
**Table:** `activity_series`

**Fields:**
- `id` (Long) - Khóa chính
- `name` (String) - Tên chuỗi sự kiện
- `description` (String) - Mô tả
- `milestonePoints` (String) - JSON: `{"3": 5, "4": 7, "5": 10}` - Mốc điểm thưởng
- `scoreType` (ScoreType) - Loại điểm để cộng milestone (REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE)
- `mainActivity` (Activity) - Activity chính (có thể null)
- `registrationStartDate` (LocalDateTime) - Ngày mở đăng ký
- `registrationDeadline` (LocalDateTime) - Hạn chót đăng ký
- `requiresApproval` (boolean) - Cần duyệt đăng ký
- `ticketQuantity` (Integer) - Số lượng vé/slot (null = không giới hạn)
- `createdAt` (LocalDateTime) - Ngày tạo

#### 2. `StudentSeriesProgress`
**Path:** `src/main/java/vn/campuslife/entity/StudentSeriesProgress.java`  
**Table:** `student_series_progress`

**Fields:**
- `id` (Long) - Khóa chính
- `student` (Student) - Sinh viên
- `series` (ActivitySeries) - Chuỗi sự kiện
- `completedActivityIds` (String) - JSON array: `[1,3,5]` - Danh sách activityId đã tham gia
- `completedCount` (Integer) - Số sự kiện đã tham gia
- `pointsEarned` (BigDecimal) - Điểm đã nhận từ milestone
- `lastUpdated` (LocalDateTime) - Ngày cập nhật

#### 3. `Activity` (Cập nhật)
**Path:** `src/main/java/vn/campuslife/entity/Activity.java`  
**Table:** `activities`

**Fields mới/thay đổi:**
- `seriesId` (Long) - ID chuỗi sự kiện (null = sự kiện đơn lẻ)
- `seriesOrder` (Integer) - Thứ tự trong chuỗi (1, 2, 3...)
- `type` (ActivityType) - **Cho phép null** (nếu thuộc series)
- `scoreType` (ScoreType) - **Cho phép null** (lấy từ series nếu thuộc series)
- `maxPoints` (BigDecimal) - **null** nếu thuộc series (không dùng để tính điểm)
- `registrationStartDate` (LocalDateTime) - **null** nếu thuộc series (lấy từ series)
- `registrationDeadline` (LocalDateTime) - **null** nếu thuộc series (lấy từ series)
- `requiresApproval` (boolean) - Lấy từ series nếu thuộc series
- `ticketQuantity` (Integer) - Lấy từ series nếu thuộc series

---

### B. Minigame Quiz

#### 4. `MiniGame`
**Path:** `src/main/java/vn/campuslife/entity/MiniGame.java`  
**Table:** `mini_games`

**Fields:**
- `id` (Long) - Khóa chính
- `title` (String) - Tiêu đề minigame
- `description` (String) - Mô tả
- `questionCount` (Integer) - Số lượng câu hỏi
- `timeLimit` (Integer) - Thời gian giới hạn (giây)
- `isActive` (boolean) - Đang hoạt động
- `type` (MiniGameType) - Loại minigame (QUIZ)
- `activity` (Activity) - Activity (OneToOne, unique)
- `requiredCorrectAnswers` (Integer) - Số câu đúng tối thiểu để đạt
- `rewardPoints` (BigDecimal) - Điểm thưởng nếu đạt

#### 5. `MiniGameQuiz`
**Path:** `src/main/java/vn/campuslife/entity/MiniGameQuiz.java`  
**Table:** `mini_game_quizzes`

**Fields:**
- `id` (Long) - Khóa chính
- `miniGame` (MiniGame) - Minigame (OneToOne, unique)
- `questions` (Set<MiniGameQuizQuestion>) - Danh sách câu hỏi

#### 6. `MiniGameQuizQuestion`
**Path:** `src/main/java/vn/campuslife/entity/MiniGameQuizQuestion.java`  
**Table:** `mini_game_quiz_questions`

**Fields:**
- `id` (Long) - Khóa chính
- `questionText` (String) - Nội dung câu hỏi
- `options` (Set<MiniGameQuizOption>) - Danh sách lựa chọn
- `miniGameQuiz` (MiniGameQuiz) - Quiz
- `displayOrder` (Integer) - Thứ tự hiển thị

#### 7. `MiniGameQuizOption`
**Path:** `src/main/java/vn/campuslife/entity/MiniGameQuizOption.java`  
**Table:** `mini_game_quiz_options`

**Fields:**
- `id` (Long) - Khóa chính
- `text` (String) - Nội dung lựa chọn
- `isCorrect` (boolean) - Là đáp án đúng
- `question` (MiniGameQuizQuestion) - Câu hỏi

#### 8. `MiniGameAttempt`
**Path:** `src/main/java/vn/campuslife/entity/MiniGameAttempt.java`  
**Table:** `mini_game_attempts`

**Fields:**
- `id` (Long) - Khóa chính
- `miniGame` (MiniGame) - Minigame
- `student` (Student) - Sinh viên
- `correctCount` (Integer) - Số câu đúng
- `status` (AttemptStatus) - Trạng thái (IN_PROGRESS, PASSED, FAILED)
- `startedAt` (LocalDateTime) - Thời gian bắt đầu
- `submittedAt` (LocalDateTime) - Thời gian nộp bài

#### 9. `MiniGameAnswer`
**Path:** `src/main/java/vn/campuslife/entity/MiniGameAnswer.java`  
**Table:** `mini_game_answers`

**Fields:**
- `id` (Long) - Khóa chính
- `attempt` (MiniGameAttempt) - Lần làm bài
- `question` (MiniGameQuizQuestion) - Câu hỏi
- `selectedOption` (MiniGameQuizOption) - Lựa chọn đã chọn
- `isCorrect` (Boolean) - Đáp án đúng hay sai

---

### C. Logic Tính Điểm (Đã có sẵn, có cập nhật)

#### 10. `ActivityParticipation` (Cập nhật)
**Path:** `src/main/java/vn/campuslife/entity/ActivityParticipation.java`  
**Table:** `activity_participations`

**Fields:**
- `id` (Long) - Khóa chính
- `registration` (ActivityRegistration) - Đăng ký
- `participationType` (ParticipationType) - Loại tham gia (REGISTERED, CHECKED_IN, CHECKED_OUT, ATTENDED, COMPLETED)
- `pointsEarned` (BigDecimal) - **Điểm kiếm được (0 cho activity trong series)**
- `date` (LocalDateTime) - Ngày tham gia
- `isCompleted` (Boolean) - null = chưa chấm, true = đạt, false = không đạt
- `checkInTime` (LocalDateTime) - Thời gian check-in
- `checkOutTime` (LocalDateTime) - Thời gian check-out

**Logic mới:**
- Activity trong series: `pointsEarned = 0` (không tính từ maxPoints)
- Activity đơn lẻ: `pointsEarned = maxPoints` (nếu có)
- CHUYEN_DE_DOANH_NGHIEP: `pointsEarned = maxPoints` (để dùng cho REN_LUYEN)

#### 11. `ActivityRegistration` (Đã có sẵn)
**Path:** `src/main/java/vn/campuslife/entity/ActivityRegistration.java`  
**Table:** `activity_registrations`

**Fields:**
- `id` (Long) - Khóa chính
- `activity` (Activity) - Activity
- `student` (Student) - Sinh viên
- `registeredDate` (LocalDateTime) - Ngày đăng ký
- `status` (RegistrationStatus) - Trạng thái (PENDING, APPROVED, REJECTED, CANCELLED)
- `createdAt` (LocalDateTime) - Ngày tạo
- `ticketCode` (String) - Mã vé (unique)

**Logic mới:**
- Khi đăng ký series → Tự động tạo registration cho tất cả activities trong series

#### 12. `StudentScore` (Đã có sẵn)
**Path:** `src/main/java/vn/campuslife/entity/StudentScore.java`  
**Table:** `student_scores`

**Fields:**
- `id` (Long) - Khóa chính
- `student` (Student) - Sinh viên
- `semester` (Semester) - Học kỳ
- `scoreType` (ScoreType) - Loại điểm (REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE)
- `score` (BigDecimal) - Điểm
- `activityIds` (String) - JSON array: `[1,5,10]` - Danh sách activity IDs đóng góp điểm
- `notes` (String) - Ghi chú
- `createdAt` (LocalDateTime) - Ngày tạo
- `updatedAt` (LocalDateTime) - Ngày cập nhật

**Logic mới:**
- Milestone points từ series được cộng vào StudentScore (scoreType từ series.scoreType)
- CHUYEN_DE: score = số buổi đã tham gia (đếm participation COMPLETED)
- REN_LUYEN: score = tổng điểm từ maxPoints của các activity CHUYEN_DE_DOANH_NGHIEP

#### 13. `ScoreHistory` (Đã có sẵn)
**Path:** `src/main/java/vn/campuslife/entity/ScoreHistory.java`  
**Table:** `score_histories`

**Fields:**
- `id` (Long) - Khóa chính
- `score` (StudentScore) - Điểm
- `oldScore` (BigDecimal) - Điểm cũ
- `newScore` (BigDecimal) - Điểm mới
- `changedBy` (User) - Người thay đổi
- `changeDate` (LocalDateTime) - Ngày thay đổi
- `reason` (String) - Lý do thay đổi
- `activityId` (Long) - ID activity gây ra thay đổi (optional)

**Logic mới:**
- Ghi lại lịch sử khi cộng milestone points từ series
- Ghi lại lịch sử khi cộng điểm từ minigame
- Ghi lại lịch sử khi cập nhật CHUYEN_DE score (đếm số buổi)

---

## 2️⃣ REQUEST MODELS (DTO cho API Input)

### A. Chuỗi Sự Kiện

#### 14. Không có Request riêng cho Series
**Lưu ý:** Series được tạo qua `Map<String, Object>` trong controller, không có Request class riêng.

**Fields trong request body:**
- `name` (String)
- `description` (String)
- `milestonePoints` (String) - JSON string
- `scoreType` (String) - "REN_LUYEN", "CONG_TAC_XA_HOI", "CHUYEN_DE"
- `mainActivityId` (Long) - optional
- `registrationStartDate` (String) - ISO DateTime format
- `registrationDeadline` (String) - ISO DateTime format
- `requiresApproval` (Boolean) - default: true
- `ticketQuantity` (Integer) - optional

**Tạo Activity trong Series:**
- `name` (String)
- `description` (String)
- `startDate` (String) - ISO DateTime format
- `endDate` (String) - ISO DateTime format
- `location` (String)
- `order` (Integer)

**Thêm Activity vào Series:**
- `activityId` (Long)
- `order` (Integer)

### B. Minigame

#### 15. Không có Request riêng cho Minigame
**Lưu ý:** Minigame được tạo qua `Map<String, Object>` trong controller.

**Fields trong request body:**
- `activityId` (Long)
- `title` (String)
- `description` (String)
- `questionCount` (Integer)
- `timeLimit` (Integer) - optional
- `requiredCorrectAnswers` (Integer) - optional
- `rewardPoints` (BigDecimal) - optional
- `questions` (List<Map>) - Danh sách câu hỏi
  - `questionText` (String)
  - `options` (List<Map>) - Danh sách lựa chọn
    - `text` (String)
    - `isCorrect` (Boolean)

**Submit Attempt:**
- `answers` (Map<Long, Long>) - Key: questionId, Value: optionId

### C. Logic Tính Điểm

#### 16. `ActivityParticipationRequest`
**Path:** `src/main/java/vn/campuslife/model/ActivityParticipationRequest.java`

**Fields:**
- `ticketCode` (String) - Mã vé để check-in
- `studentId` (Long) - optional (lấy từ authentication)
- `participationType` (ParticipationType) - optional (tự động xác định)
- `pointsEarned` (BigDecimal) - optional (tự động tính)

#### 17. `CreateActivityRequest` (Đã có sẵn)
**Path:** `src/main/java/vn/campuslife/model/CreateActivityRequest.java`

**Fields liên quan:**
- `type` (ActivityType) - "MINIGAME", "CHUYEN_DE_DOANH_NGHIEP", etc.
- `scoreType` (ScoreType) - "REN_LUYEN", "CONG_TAC_XA_HOI", "CHUYEN_DE"
- `maxPoints` (BigDecimal) - Điểm tối đa
- `seriesId` (Long) - **KHÔNG có trong request** (chỉ set khi thêm vào series)

#### 18. `ActivityRegistrationRequest` (Đã có sẵn)
**Path:** `src/main/java/vn/campuslife/model/ActivityRegistrationRequest.java`

**Fields:**
- `activityId` (Long) - ID activity để đăng ký

**Lưu ý:** Không dùng để đăng ký series (dùng endpoint riêng)

---

## 3️⃣ RESPONSE MODELS (DTO cho API Output)

### A. Chuỗi Sự Kiện

#### 19. Không có Response riêng cho Series
**Lưu ý:** Series trả về qua `Response.data` với object `ActivitySeries` entity.

### B. Minigame

#### 20. Không có Response riêng cho Minigame
**Lưu ý:** Minigame trả về qua `Response.data` với object `MiniGame` entity (có nested questions và options).

### C. Logic Tính Điểm

#### 21. `ActivityParticipationResponse`
**Path:** `src/main/java/vn/campuslife/model/ActivityParticipationResponse.java`

**Fields:**
- `id` (Long)
- `activityId` (Long)
- `activityName` (String)
- `studentId` (Long)
- `studentName` (String)
- `studentCode` (String)
- `participationType` (ParticipationType) - REGISTERED, CHECKED_IN, CHECKED_OUT, ATTENDED, COMPLETED
- `pointsEarned` (BigDecimal) - **0 cho activity trong series**
- `date` (LocalDateTime)
- `isCompleted` (Boolean) - null = chưa chấm, true = đạt, false = không đạt
- `checkInTime` (LocalDateTime)
- `checkOutTime` (LocalDateTime)

#### 22. `ActivityResponse` (Đã có sẵn)
**Path:** `src/main/java/vn/campuslife/model/ActivityResponse.java`

**Fields liên quan:**
- `type` (ActivityType) - có thể null nếu thuộc series
- `scoreType` (ScoreType) - có thể null nếu thuộc series
- `maxPoints` (BigDecimal) - null nếu thuộc series
- `seriesId` (Long) - ID series nếu thuộc series
- `seriesOrder` (Integer) - Thứ tự trong series

#### 23. `ActivityRegistrationResponse` (Đã có sẵn)
**Path:** `src/main/java/vn/campuslife/model/ActivityRegistrationResponse.java`

**Fields:**
- `id` (Long)
- `activityId` (Long)
- `activityName` (String)
- `studentId` (Long)
- `studentName` (String)
- `status` (RegistrationStatus)
- `registeredDate` (LocalDateTime)
- `ticketCode` (String)

#### 24. `ScoreViewResponse` (Đã có sẵn)
**Path:** `src/main/java/vn/campuslife/model/ScoreViewResponse.java`

**Fields:**
- `scoreType` (ScoreType) - REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE
- `score` (BigDecimal) - Điểm hiện tại
- Các fields khác...

---

## 4️⃣ ENUMERATION MODELS

### A. Chuỗi Sự Kiện

#### 25. `ScoreType` (Đã có sẵn)
**Path:** `src/main/java/vn/campuslife/enumeration/ScoreType.java`

**Values:**
- `REN_LUYEN` - Điểm rèn luyện
- `CONG_TAC_XA_HOI` - Điểm công tác xã hội
- `CHUYEN_DE` - Điểm chuyên đề (đếm số buổi)

**Sử dụng:**
- `ActivitySeries.scoreType` - Loại điểm để cộng milestone
- `Activity.scoreType` - Loại điểm của activity (null nếu thuộc series)
- `StudentScore.scoreType` - Loại điểm trong bảng điểm

#### 26. `ActivityType` (Đã có sẵn, cập nhật)
**Path:** `src/main/java/vn/campuslife/enumeration/ActivityType.java`

**Values:**
- `SUKIEN` - Sự kiện
- `MINIGAME` - Minigame
- `CONG_TAC_XA_HOI` - Công tác xã hội
- `CHUYEN_DE_DOANH_NGHIEP` - Chuyên đề doanh nghiệp

**Lưu ý:**
- `Activity.type` - **Cho phép null** nếu thuộc series

### B. Minigame

#### 27. `MiniGameType`
**Path:** `src/main/java/vn/campuslife/enumeration/MiniGameType.java`

**Values:**
- `QUIZ` - Quiz/Trắc nghiệm
- (Có thể thêm: MEMORY_GAME, PUZZLE, etc.)

#### 28. `AttemptStatus`
**Path:** `src/main/java/vn/campuslife/enumeration/AttemptStatus.java`

**Values:**
- `IN_PROGRESS` - Đang làm
- `PASSED` - Đã đạt
- `FAILED` - Không đạt

### C. Logic Tính Điểm

#### 29. `ParticipationType` (Đã có sẵn, cập nhật)
**Path:** `src/main/java/vn/campuslife/enumeration/ParticipationType.java`

**Values:**
- `REGISTERED` - Đã đăng ký
- `CHECKED_IN` - Đã check-in (lần 1)
- `CHECKED_OUT` - Đã check-out (lần 2)
- `ATTENDED` - Hoàn thành cả 2 lần check
- `COMPLETED` - Đã chấm điểm (đạt hoặc không đạt)

**Logic mới:**
- Check-in lần 1: `REGISTERED` → `CHECKED_IN`
- Check-out lần 2: `CHECKED_IN` → `ATTENDED`
- Activity trong series: `pointsEarned = 0` khi `ATTENDED`

#### 30. `RegistrationStatus` (Đã có sẵn)
**Path:** `src/main/java/vn/campuslife/enumeration/RegistrationStatus.java`

**Values:**
- `PENDING` - Chờ duyệt
- `APPROVED` - Đã duyệt
- `REJECTED` - Từ chối
- `CANCELLED` - Đã hủy

**Logic mới:**
- Khi đăng ký series: Status phụ thuộc vào `series.requiresApproval`
  - `requiresApproval = false` → `APPROVED`
  - `requiresApproval = true` → `PENDING`

---

## 5️⃣ REPOSITORY INTERFACES

### A. Chuỗi Sự Kiện

#### 31. `ActivitySeriesRepository`
**Path:** `src/main/java/vn/campuslife/repository/ActivitySeriesRepository.java`
- Extends `JpaRepository<ActivitySeries, Long>`

#### 32. `StudentSeriesProgressRepository`
**Path:** `src/main/java/vn/campuslife/repository/StudentSeriesProgressRepository.java`
- Extends `JpaRepository<StudentSeriesProgress, Long>`
- Method: `findByStudentIdAndSeriesId(Long studentId, Long seriesId)`

#### 33. `ActivityRepository` (Cập nhật)
**Path:** `src/main/java/vn/campuslife/repository/ActivityRepository.java`
- Method mới: `findBySeriesIdAndIsDeletedFalse(Long seriesId)`

### B. Minigame

#### 34. `MiniGameRepository`
**Path:** `src/main/java/vn/campuslife/repository/MiniGameRepository.java`
- Extends `JpaRepository<MiniGame, Long>`
- Method: `findByActivityId(Long activityId)`

#### 35. `MiniGameQuizRepository`
**Path:** `src/main/java/vn/campuslife/repository/MiniGameQuizRepository.java`
- Extends `JpaRepository<MiniGameQuiz, Long>`

#### 36. `MiniGameQuizQuestionRepository`
**Path:** `src/main/java/vn/campuslife/repository/MiniGameQuizQuestionRepository.java`
- Extends `JpaRepository<MiniGameQuizQuestion, Long>`

#### 37. `MiniGameQuizOptionRepository`
**Path:** `src/main/java/vn/campuslife/repository/MiniGameQuizOptionRepository.java`
- Extends `JpaRepository<MiniGameQuizOption, Long>`

#### 38. `MiniGameAttemptRepository`
**Path:** `src/main/java/vn/campuslife/repository/MiniGameAttemptRepository.java`
- Extends `JpaRepository<MiniGameAttempt, Long>`
- Methods:
  - `findInProgressAttempt(Long studentId, Long miniGameId, AttemptStatus status)`
  - `findByStudentIdAndMiniGameId(Long studentId, Long miniGameId)`

#### 39. `MiniGameAnswerRepository`
**Path:** `src/main/java/vn/campuslife/repository/MiniGameAnswerRepository.java`
- Extends `JpaRepository<MiniGameAnswer, Long>`

### C. Logic Tính Điểm (Đã có sẵn)

#### 40. `ActivityParticipationRepository` (Đã có sẵn)
**Path:** `src/main/java/vn/campuslife/repository/ActivityParticipationRepository.java`

#### 41. `ActivityRegistrationRepository` (Đã có sẵn)
**Path:** `src/main/java/vn/campuslife/repository/ActivityRegistrationRepository.java`
- Methods liên quan:
  - `findByActivityIdAndActivityIsDeletedFalse(Long activityId)`
  - `existsByActivityIdAndStudentId(Long activityId, Long studentId)`

#### 42. `StudentScoreRepository` (Đã có sẵn)
**Path:** `src/main/java/vn/campuslife/repository/StudentScoreRepository.java`
- Method: `findByStudentIdAndSemesterIdAndScoreType(Long studentId, Long semesterId, ScoreType scoreType)`

#### 43. `ScoreHistoryRepository` (Đã có sẵn)
**Path:** `src/main/java/vn/campuslife/repository/ScoreHistoryRepository.java`

---

## 6️⃣ SERVICE INTERFACES & IMPLEMENTATIONS

### A. Chuỗi Sự Kiện

#### 44. `ActivitySeriesService`
**Path:** `src/main/java/vn/campuslife/service/ActivitySeriesService.java`

**Methods:**
- `createSeries(...)` - Tạo chuỗi sự kiện
- `createActivityInSeries(...)` - Tạo activity trong series (mới)
- `addActivityToSeries(...)` - Thêm activity vào series
- `registerForSeries(...)` - Student đăng ký series (mới)
- `updateStudentProgress(...)` - Cập nhật tiến độ sinh viên
- `calculateMilestonePoints(...)` - Tính điểm milestone
- `checkMinimumRequirement(...)` - Kiểm tra yêu cầu tối thiểu

#### 45. `ActivitySeriesServiceImpl`
**Path:** `src/main/java/vn/campuslife/service/impl/ActivitySeriesServiceImpl.java`

### B. Minigame

#### 46. `MiniGameService`
**Path:** `src/main/java/vn/campuslife/service/MiniGameService.java`

**Methods:**
- `createMiniGame(...)` - Tạo minigame với quiz
- `getMiniGameByActivity(...)` - Lấy minigame theo activity
- `startAttempt(...)` - Student bắt đầu làm quiz
- `submitAttempt(...)` - Student nộp bài quiz
- `getStudentAttempts(...)` - Lấy lịch sử attempts
- `calculateScoreAndCreateParticipation(...)` - Tính điểm và tạo participation

#### 47. `MiniGameServiceImpl`
**Path:** `src/main/java/vn/campuslife/service/impl/MiniGameServiceImpl.java`

### C. Logic Tính Điểm (Đã có sẵn, có cập nhật)

#### 48. `ActivityRegistrationService` (Cập nhật)
**Path:** `src/main/java/vn/campuslife/service/ActivityRegistrationService.java`

**Methods liên quan:**
- `checkIn(...)` - Check-in/check-out (có logic mới cho series và CHUYEN_DE_DOANH_NGHIEP)

#### 49. `ActivityRegistrationServiceImpl` (Cập nhật)
**Path:** `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`

**Logic mới:**
- Check-in activity trong series: `pointsEarned = 0`, gọi `activitySeriesService.updateStudentProgress()`
- Check-in CHUYEN_DE_DOANH_NGHIEP: Dual score (CHUYEN_DE count + REN_LUYEN points)

---

## 7️⃣ CONTROLLER CLASSES

### A. Chuỗi Sự Kiện

#### 50. `ActivitySeriesController`
**Path:** `src/main/java/vn/campuslife/controller/ActivitySeriesController.java`

**Endpoints:**
- `POST /api/series` - Tạo chuỗi sự kiện
- `POST /api/series/{seriesId}/activities/create` - Tạo activity trong series (mới)
- `POST /api/series/{seriesId}/activities` - Thêm activity vào series
- `POST /api/series/{seriesId}/register` - Student đăng ký series (mới)
- `POST /api/series/{seriesId}/students/{studentId}/calculate-milestone` - Tính milestone

### B. Minigame

#### 51. `MiniGameController`
**Path:** `src/main/java/vn/campuslife/controller/MiniGameController.java`

**Endpoints:**
- `POST /api/minigames` - Tạo minigame
- `GET /api/minigames/activity/{activityId}` - Lấy minigame theo activity
- `POST /api/minigames/{miniGameId}/start` - Bắt đầu làm quiz
- `POST /api/minigames/attempts/{attemptId}/submit` - Nộp bài quiz
- `GET /api/minigames/{miniGameId}/attempts/my` - Lấy lịch sử attempts

### C. Logic Tính Điểm (Đã có sẵn)

#### 52. `ActivityRegistrationController` (Cập nhật)
**Path:** `src/main/java/vn/campuslife/controller/ActivityRegistrationController.java`

**Endpoints liên quan:**
- `POST /api/registrations/checkin` - Check-in/check-out (có logic mới)

---

## 8️⃣ DATABASE MIGRATIONS

### A. Chuỗi Sự Kiện

#### 53. `V1003__create_activity_series_tables.sql`
- Tạo bảng `activity_series`
- Tạo bảng `student_series_progress`
- Thêm cột `series_id`, `series_order` vào `activities`

#### 54. `V1005__add_series_registration_fields.sql`
- Thêm các cột vào `activity_series`:
  - `registration_start_date` (DATETIME)
  - `registration_deadline` (DATETIME)
  - `requires_approval` (BOOLEAN)
  - `ticket_quantity` (INT)

#### 55. `V1006__allow_null_type_scoretype_for_series_activities.sql`
- Cho phép null cho `type` và `score_type` trong `activities`

### B. Minigame

#### 56. `V1004__create_minigame_tables.sql`
- Tạo bảng `mini_games`
- Tạo bảng `mini_game_quizzes`
- Tạo bảng `mini_game_quiz_questions`
- Tạo bảng `mini_game_quiz_options`
- Tạo bảng `mini_game_attempts`
- Tạo bảng `mini_game_answers`

### C. Logic Tính Điểm

#### 57. `V999__activity_datetime_and_flags.sql` (Đã có sẵn)
- Convert date columns to datetime
- Thêm các flags: `is_draft`, `requires_approval`

#### 58. `V1007__change_task_deadline_to_datetime.sql` (Mới)
- Đổi cột `deadline` từ DATE sang DATETIME trong `activity_tasks`

---

## 📊 TÓM TẮT THEO CHỨC NĂNG

### Chuỗi Sự Kiện (Activity Series):
**Entities:** 2 mới
- `ActivitySeries`
- `StudentSeriesProgress`

**Entities cập nhật:** 1
- `Activity` (thêm `seriesId`, `seriesOrder`, cho phép null các field)

**Repositories:** 3
- `ActivitySeriesRepository` (mới)
- `StudentSeriesProgressRepository` (mới)
- `ActivityRepository` (thêm method)

**Services:** 1
- `ActivitySeriesService` + `ActivitySeriesServiceImpl` (mới)

**Controllers:** 1
- `ActivitySeriesController` (mới)

**Migrations:** 3
- `V1003__create_activity_series_tables.sql`
- `V1005__add_series_registration_fields.sql`
- `V1006__allow_null_type_scoretype_for_series_activities.sql`

---

### Minigame Quiz:
**Entities:** 6 mới
- `MiniGame`
- `MiniGameQuiz`
- `MiniGameQuizQuestion`
- `MiniGameQuizOption`
- `MiniGameAttempt`
- `MiniGameAnswer`

**Enums:** 2 mới
- `MiniGameType`
- `AttemptStatus`

**Repositories:** 6 mới
- `MiniGameRepository`
- `MiniGameQuizRepository`
- `MiniGameQuizQuestionRepository`
- `MiniGameQuizOptionRepository`
- `MiniGameAttemptRepository`
- `MiniGameAnswerRepository`

**Services:** 1
- `MiniGameService` + `MiniGameServiceImpl` (mới)

**Controllers:** 1
- `MiniGameController` (mới)

**Migrations:** 1
- `V1004__create_minigame_tables.sql`

---

### Logic Tính Điểm Đã Cập Nhật:
**Entities cập nhật:** 3
- `ActivityParticipation` (logic mới cho series và CHUYEN_DE_DOANH_NGHIEP)
- `ActivityRegistration` (logic tự động đăng ký series)
- `StudentScore` (logic milestone và dual score)

**Enums cập nhật:** 1
- `ParticipationType` (thêm CHECKED_IN, CHECKED_OUT)

**Services cập nhật:** 1
- `ActivityRegistrationService` + `ActivityRegistrationServiceImpl` (logic mới)

**Migrations:** 1
- `V1007__change_task_deadline_to_datetime.sql` (liên quan đến LocalDateTime)

---

## ✅ TỔNG KẾT

### Tổng số Models:
- **Entities mới:** 8 (ActivitySeries, StudentSeriesProgress, 6 Minigame entities)
- **Entities cập nhật:** 4 (Activity, ActivityParticipation, ActivityRegistration, StudentScore)
- **Request Models:** 1 (ActivityParticipationRequest - đã có sẵn)
- **Response Models:** 1 (ActivityParticipationResponse - đã có sẵn)
- **Enums mới:** 2 (MiniGameType, AttemptStatus)
- **Enums cập nhật:** 2 (ParticipationType, ActivityType - cho phép null)
- **Repositories mới:** 9
- **Repositories cập nhật:** 1 (ActivityRepository)
- **Services mới:** 2 (ActivitySeriesService, MiniGameService)
- **Services cập nhật:** 1 (ActivityRegistrationService)
- **Controllers mới:** 2 (ActivitySeriesController, MiniGameController)
- **Controllers cập nhật:** 1 (ActivityRegistrationController)
- **Migrations:** 5

---

## 📝 LƯU Ý QUAN TRỌNG

1. **Activity trong Series:**
   - Vẫn dùng entity `Activity` có sẵn
   - Cho phép null các thuộc tính: `type`, `scoreType`, `maxPoints`, `registrationStartDate`, `registrationDeadline`, `ticketQuantity`, `penaltyPointsIncomplete`
   - Các thuộc tính này được lấy từ `ActivitySeries` hoặc không dùng

2. **Không có Request/Response riêng:**
   - Series và Minigame dùng `Map<String, Object>` trong controller
   - Có thể tạo DTO riêng sau nếu cần

3. **Dual Score (CHUYEN_DE_DOANH_NGHIEP):**
   - Dùng `ActivityParticipation` để đếm số buổi (CHUYEN_DE)
   - Dùng `maxPoints` để cộng điểm REN_LUYEN
   - Cả 2 đều cập nhật vào `StudentScore` với `scoreType` khác nhau

4. **Milestone Points:**
   - Lưu trong `ActivitySeries.milestonePoints` (JSON string)
   - Tính và cộng vào `StudentScore` với `scoreType` từ `ActivitySeries.scoreType`
   - Lịch sử được ghi vào `ScoreHistory`

