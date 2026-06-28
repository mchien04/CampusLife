# Sequence Diagram - Minigame / Quiz Module

> Hệ thống: **CampusLife** (Spring Boot + React)  
> Module: Minigame / Quiz  
> Các participant: `Admin/Manager/Student`, `Client`, `Controller`, `Service`, `Repository`, `Database`

---

## 1. Tạo minigame kèm quiz (G.26) — POST /api/admin/minigames

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin/Manager
    participant Client as React Client
    participant AC as AdminController<br/>(/api/admin/minigames)
    participant MS as MinigameService
    participant AS as ActivityService
    participant MR as MinigameRepository
    participant QR as QuestionRepository
    participant DB as Database

    Note over Admin, DB: ===== LUỒNG 1: Admin tạo Minigame kèm Quiz =====

    Admin->>Client: 1. Nhập form tạo minigame<br/>(title, description, activityId,<br/>startTime, endTime, questions[])
    Client->>Client: Validate form (required, date range)
    Client->>AC: 2. POST /api/admin/minigames<br/>+ RequestBody (MinigameCreateDTO)

    AC->>AC: 3. @Valid DTO, @PreAuthorize("hasRole('ADMIN')")
    AC->>MS: 4. createMinigameWithQuiz(dto)

    Note over AC, MS: ===== LUỒNG 2: Kiểm tra Activity =====

    MS->>AS: 5. validateActivityExists(activityId)
    AS->>MR: 6. findById(activityId) [hoặc ActivityRepository]
    MR->>DB: 7. SELECT * FROM activities WHERE id = ?
    DB-->>MR: 8. Trả về Activity record
    MR-->>AS: 9. Optional<Activity>
    AS-->>MS: 10. Activity exists / ActivityNotFoundException

    Note over MS, DB: ===== LUỒNG 3: Tạo Minigame =====

    MS->>MS: 11. Build Minigame entity<br/>(title, description, activityId,<br/>startTime, endTime, status=DRAFT)
    MS->>MR: 12. save(minigame)
    MR->>DB: 13. INSERT INTO minigames (...)
    DB-->>MR: 14. Trả về minigame_id
    MR-->>MS: 15. Minigame (đã có ID)

    Note over MS, DB: ===== LUỒNG 4: Tạo danh sách Question =====

    loop For each question in questions[]
        MS->>MS: 16a. Build Question entity<br/>(text, options[], correctAnswer,<br/>score, minigameId = savedMinigame.id)
        MS->>QR: 16b. save(question)
        QR->>DB: 16c. INSERT INTO questions (...)
        DB-->>QR: 16d. Trả về question_id
        QR-->>MS: 16e. Question (đã có ID)
    end

    MS->>MS: 17. Liên kết questions vào minigame.questions<br/>(Cascade hoặc manual mapping)

    Note over MS, DB: ===== LUỒNG 5: Trả về kết quả =====

    MS-->>AC: 18. MinigameResponseDTO<br/>(id, title, description, activityId,<br/>startTime, endTime, questions[])
    AC-->>Client: 19. ResponseEntity.ok(dto)<br/>HTTP 200 + JSON
    Client-->>Admin: 20. Hiển thị thông báo<br/>"Tạo minigame thành công!"
```

---

## 2. Bắt đầu lượt chơi quiz (G.27) — POST /api/minigames/{id}/start

```mermaid
sequenceDiagram
    autonumber
    actor Student as Student
    participant Client as React Client
    participant MC as MinigameController<br/>(/api/minigames/{id}/start)
    participant MS as MinigameService
    participant AS as AuthService
    participant MR as MinigameRepository
    participant AR as AttemptRepository
    participant DB as Database

    Note over Student, DB: ===== LUỒNG 1: Student bắt đầu chơi =====

    Student->>Client: 1. Click "Bắt đầu Quiz"<br/>trên màn hình chi tiết minigame
    Client->>MC: 2. POST /api/minigames/{id}/start<br/>+ Bearer Token (JWT)

    MC->>MC: 3. @PreAuthorize("hasRole('STUDENT')")
    MC->>AS: 4. extractStudentIdFromToken(token)
    AS-->>MC: 5. studentId (Long)

    MC->>MS: 6. startQuiz(minigameId, studentId)

    Note over MS, DB: ===== LUỒNG 2: Kiểm tra Minigame =====

    MS->>MR: 7. findById(minigameId)
    MR->>DB: 8. SELECT * FROM minigames WHERE id = ?
    DB-->>MR: 9. Minigame record
    MR-->>MS: 10. Optional<Minigame>

    alt Minigame không tồn tại
        MS-->>MC: 10a. throw MinigameNotFoundException
        MC-->>Client: 10b. HTTP 404 Not Found
    else Minigame tồn tại
        MS->>MS: 11. Kiểm tra thời gian mở:<br/>startTime <= now <= endTime
    end

    alt Minigame chưa mở hoặc đã đóng
        MS-->>MC: 11a. throw MinigameNotOpenException<br/>("Minigame chưa mở" / "Minigame đã kết thúc")
        MC-->>Client: 11b. HTTP 400 Bad Request
    else Minigame đang mở
        Note over MS, DB: ===== LUỒNG 3: Kiểm tra lượt chơi trước =====

        MS->>AR: 12. findByStudentIdAndMinigameId(studentId, minigameId)
        AR->>DB: 13. SELECT * FROM minigame_attempts<br/>WHERE student_id = ? AND minigame_id = ?<br/>ORDER BY start_time DESC LIMIT 1
        DB-->>AR: 14. Attempt records (nếu có)
        AR-->>MS: 15. List<MinigameAttempt>

        alt Student đã có attempt COMPLETED và minigame cho phép chơi lại (allowRetry=true)
            MS->>MS: 15a. Cho phép tạo attempt mới
        else Student đã có attempt IN_PROGRESS
            MS-->>MC: 15b. throw AttemptInProgressException<br/>("Bạn đang có lượt chơi chưa hoàn thành")
            MC-->>Client: 15c. HTTP 409 Conflict
        else Student đã chơi và không cho phép chơi lại (allowRetry=false)
            MS-->>MC: 15d. throw AlreadyPlayedException<br/>("Bạn đã hoàn thành quiz này")
            MC-->>Client: 15e. HTTP 403 Forbidden
        end

        Note over MS, DB: ===== LUỒNG 4: Tạo MinigameAttempt =====

        MS->>MS: 16. Build MinigameAttempt:<br/>- studentId<br/>- minigameId<br/>- startTime = now()<br/>- status = IN_PROGRESS<br/>- score = 0<br/>- answers = []
        MS->>AR: 17. save(attempt)
        AR->>DB: 18. INSERT INTO minigame_attempts (...)
        DB-->>AR: 19. attempt_id
        AR-->>MS: 20. MinigameAttempt (đã có ID)

        Note over MS, DB: ===== LUỒNG 5: Chuẩn bị danh sách câu hỏi (ẩn correctAnswer) =====

        MS->>MR: 21. getQuestionsByMinigameId(minigameId)<br/>[hoặc QuestionRepository]
        MR->>DB: 22. SELECT id, text, options, score<br/>FROM questions WHERE minigame_id = ?<br/>(KHÔNG SELECT correctAnswer)
        DB-->>MR: 23. Question records (ẩn đáp án đúng)
        MR-->>MS: 24. List<QuestionDTO>

        MS->>MS: 25. Build StartQuizResponse:<br/>- attemptId<br/>- questions[] (id, text, options, score)<br/>- totalQuestions<br/>- timeLimit (nếu có)

        MS-->>MC: 26. StartQuizResponseDTO
        MC-->>Client: 27. ResponseEntity.ok(dto)<br/>HTTP 200 + JSON
        Client-->>Student: 28. Chuyển màn hình quiz<br/>Hiển thị câu hỏi + countdown timer
    end
```

---

## 3. Nộp bài quiz (G.28) — POST /api/minigames/attempts/{attemptId}/submit

```mermaid
sequenceDiagram
    autonumber
    actor Student as Student
    participant Client as React Client
    participant MC as MinigameController<br/>(/api/minigames/attempts/{attemptId}/submit)
    participant MS as MinigameService
    participant SS as ScoreService
    participant AS as AuthService
    participant AR as AttemptRepository
    participant QR as QuestionRepository
    participant SR as ScoreRecordRepository
    participant DB as Database

    Note over Student, DB: ===== LUỒNG 1: Student nộp bài =====

    Student->>Client: 1. Chọn đáp án từng câu hỏi<br/>Click "Nộp bài"
    Client->>Client: 2. Validate: tất cả câu hỏi đã trả lời? (optional)
    Client->>MC: 3. POST /api/minigames/attempts/{attemptId}/submit<br/>+ RequestBody (answers: [{questionId, selectedAnswer}])<br/>+ Bearer Token

    MC->>MC: 4. @PreAuthorize("hasRole('STUDENT')")
    MC->>AS: 5. extractStudentIdFromToken(token)
    AS-->>MC: 6. studentId (Long)

    MC->>MS: 7. submitQuiz(attemptId, studentId, answers[])

    Note over MS, DB: ===== LUỒNG 2: Kiểm tra Attempt =====

    MS->>AR: 8. findById(attemptId)
    AR->>DB: 9. SELECT * FROM minigame_attempts WHERE id = ?
    DB-->>AR: 10. Attempt record
    AR-->>MS: 11. Optional<MinigameAttempt>

    alt Attempt không tồn tại
        MS-->>MC: 11a. throw AttemptNotFoundException
        MC-->>Client: 11b. HTTP 404 Not Found
    else Attempt tồn tại nhưng thuộc về student khác
        MS-->>MC: 11c. throw UnauthorizedAttemptException
        MC-->>Client: 11d. HTTP 403 Forbidden
    else Attempt tồn tại và đúng student
        MS->>MS: 12. Kiểm tra status == IN_PROGRESS
    end

    alt Status != IN_PROGRESS (đã nộp hoặc hủy)
        MS-->>MC: 12a. throw InvalidAttemptStatusException<br/>("Lượt chơi đã kết thúc")
        MC-->>Client: 12b. HTTP 400 Bad Request
    else Status = IN_PROGRESS
        Note over MS, DB: ===== LUỒNG 3: Kiểm tra thời gian =====

        MS->>MS: 13. Kiểm tra hết thời gian chưa:<br/>now() <= attempt.startTime + timeLimit<br/>(hoặc minigame.endTime)

        alt Hết thời gian (auto-submit)
            MS->>MS: 13a. Đánh dấu isTimeExpired = true<br/>Chỉ tính câu đã trả lời trước khi hết giờ
        else Còn thời gian
            MS->>MS: 13b. Tiếp tục chấm điểm toàn bộ
        end

        Note over MS, DB: ===== LUỒNG 4: Chấm điểm từng câu =====

        MS->>QR: 14. findAllByMinigameId(attempt.minigameId)<br/>(lấy tất cả questions + correctAnswer)
        QR->>DB: 15. SELECT * FROM questions WHERE minigame_id = ?
        DB-->>QR: 16. Full Question records (có correctAnswer)
        QR-->>MS: 17. List<Question> (đáp án đúng)

        MS->>MS: 18. Map questionsById để tra cứu nhanh
        MS->>MS: 19. totalScore = 0<br/>processedAnswers = []

        loop For each answer in answers[]
            MS->>MS: 20a. Tìm question = questionsById[answer.questionId]
            alt Question không tồn tại trong minigame
                MS->>MS: 20b. Bỏ qua / throw InvalidQuestionException
            else Question tồn tại
                MS->>MS: 20c. So sánh answer.selectedAnswer vs question.correctAnswer
                alt selectedAnswer == correctAnswer
                    MS->>MS: 20d. isCorrect = true<br/>totalScore += question.score
                else selectedAnswer != correctAnswer
                    MS->>MS: 20e. isCorrect = false<br/>score += 0
                end
                MS->>MS: 20f. Thêm vào processedAnswers:<br/>{questionId, selectedAnswer, correctAnswer, isCorrect, scoreEarned}
            end
        end

        Note over MS, DB: ===== LUỒNG 5: Cập nhật Attempt =====

        MS->>MS: 21. Build update Attempt:<br/>- answers = processedAnswers[]<br/>- score = totalScore<br/>- endTime = now()<br/>- status = COMPLETED<br/>- isTimeExpired (nếu có)
        MS->>AR: 22. save(attempt)
        AR->>DB: 23. UPDATE minigame_attempts<br/>SET score = ?, answers = ?,<br/>end_time = ?, status = 'COMPLETED'<br/>WHERE id = ?
        DB-->>AR: 24. Update success
        AR-->>MS: 25. Updated Attempt

        Note over MS, DB: ===== LUỒNG 6: Cộng điểm vào ScoreRecord (nếu đạt) =====

        alt totalScore >= minigame.passingScore (hoặc luôn cộng)
            MS->>SS: 26. addScoreToStudent(studentId, minigameId, totalScore)
            SS->>SR: 27. findByStudentIdAndSource(studentId, "MINIGAME", minigameId)
            SR->>DB: 28. SELECT * FROM score_records<br/>WHERE student_id = ? AND source_type = 'MINIGAME'<br/>AND source_id = ?
            DB-->>SR: 29. ScoreRecord (nếu đã tồn tại) / empty
            SR-->>SS: 30. Optional<ScoreRecord>

            alt ScoreRecord chưa tồn tại (lần đầu hoàn thành)
                SS->>SS: 30a. Build ScoreRecord:<br/>- studentId<br/>- score = totalScore<br/>- sourceType = "MINIGAME"<br/>- sourceId = minigameId<br/>- earnedAt = now()<br/>- description = "Hoàn thành quiz: {minigame.title}"
                SS->>SR: 30b. save(scoreRecord)
                SR->>DB: 30c. INSERT INTO score_records (...)
                DB-->>SR: 30d. score_record_id
                SR-->>SS: 30e. ScoreRecord đã lưu
            else ScoreRecord đã tồn tại (chơi lại và điểm cao hơn)
                SS->>SS: 30f. Chỉ cập nhật nếu totalScore > record.score<br/>(tùy policy)
                SS->>SR: 30g. save(scoreRecord) [update]
                SR->>DB: 30h. UPDATE score_records SET score = ? ...
                DB-->>SR: 30i. Update success
            end
            SS-->>MS: 31. ScoreRecord saved/updated
        else Không đạt điểm tối thiểu
            MS->>MS: 26a. Bỏ qua cập nhật ScoreRecord
        end

        Note over MS, DB: ===== LUỒNG 7: Trả về kết quả =====

        MS->>MS: 32. Build QuizResultResponse:<br/>- attemptId<br/>- totalScore<br/>- maxPossibleScore<br/>- correctCount / totalQuestions<br/>- passingScore<br/>- isPassed<br/>- answers[] (câu đúng/sai)<br/>- scoreRecordUpdated (boolean)

        MS-->>MC: 33. QuizResultResponseDTO
        MC-->>Client: 34. ResponseEntity.ok(dto)<br/>HTTP 200 + JSON
        Client-->>Student: 35. Hiển thị kết quả:<br/>"Điểm: 8/10 - Đạt!" + chi tiết từng câu
    end
```

---

## 4. Xem lịch sử lượt chơi (G.29) — GET /api/minigames/{id}/attempts/my

```mermaid
sequenceDiagram
    autonumber
    actor Student as Student
    participant Client as React Client
    participant MC as MinigameController<br/>(/api/minigames/{id}/attempts/my)
    participant MS as MinigameService
    participant AS as AuthService
    participant AR as AttemptRepository
    participant DB as Database

    Note over Student, DB: ===== LUỒNG 1: Student xem lịch sử =====

    Student->>Client: 1. Click "Lịch sử" / "Xem kết quả"<br/>trên màn hình minigame
    Client->>MC: 2. GET /api/minigames/{id}/attempts/my<br/>+ Bearer Token (JWT)

    MC->>MC: 3. @PreAuthorize("hasRole('STUDENT')")
    MC->>AS: 4. extractStudentIdFromToken(token)
    AS-->>MC: 5. studentId (Long)

    MC->>MS: 6. getMyAttempts(minigameId, studentId)

    Note over MS, DB: ===== LUỒNG 2: Truy vấn lịch sử =====

    MS->>AR: 7. findByStudentIdAndMinigameId(studentId, minigameId)<br/>[hoặc findByStudentIdAndMinigameIdOrderByStartTimeDesc]
    AR->>DB: 8. SELECT id, score, start_time, end_time, status<br/>FROM minigame_attempts<br/>WHERE student_id = ? AND minigame_id = ?<br/>ORDER BY start_time DESC
    DB-->>AR: 9. List<Attempt> records
    AR-->>MS: 10. List<MinigameAttempt>

    alt Không có lịch sử chơi
        MS->>MS: 10a. Trả về empty list []
    else Có lịch sử chơi
        MS->>MS: 11. Map từng Attempt sang AttemptHistoryDTO:<br/>- attemptId<br/>- score<br/>- startTime<br/>- endTime<br/>- status<br/>- duration (endTime - startTime, nếu completed)
    end

    Note over MS, DB: ===== LUỒNG 3: Trả về kết quả =====

    MS-->>MC: 12. List<AttemptHistoryDTO>
    MC-->>Client: 13. ResponseEntity.ok(list)<br/>HTTP 200 + JSON
    Client-->>Student: 14. Hiển thị bảng lịch sử:<br/>| Lần | Điểm | Thời gian | Trạng thái |<br/>có thể có nút "Xem chi tiết"
```

---

## Tóm tắt thành phần và chức năng

### Thành phần (Participants)

| Thành phần | Vai trò | Framework / Công nghệ |
|-----------|---------|----------------------|
| **Admin/Manager** | Người dùng có quyền quản trị, tạo và quản lý minigame/quiz | React UI |
| **Student** | Người dùng sinh viên, tham gia chơi quiz và xem lịch sử | React UI |
| **Client** | Ứng dụng frontend React, xử lý UI/UX, validate form, gọi API | React.js, Axios/Fetch |
| **Controller** | Lớp REST Controller trong Spring Boot, nhận request, trả response, phân quyền | Spring Boot (`@RestController`, `@PreAuthorize`) |
| **Service** | Lớp Business Logic, xử lý nghiệp vụ, validation, tính toán điểm | Spring Boot (`@Service`) |
| **Repository** | Lớp Data Access, giao tiếp với Database qua JPA/Hibernate | Spring Data JPA (`@Repository`) |
| **Database** | Hệ quản trị CSDL, lưu trữ dữ liệu minigame, questions, attempts, scores | MySQL/PostgreSQL (tùy cấu hình) |

### Chức năng theo Sequence

| Sequence | Mã | Chức năng | Endpoint | Actor | Điểm chính |
|----------|-----|-----------|----------|-------|-----------|
| **1** | G.26 | Tạo minigame kèm quiz | `POST /api/admin/minigames` | Admin/Manager | Validate Activity → Tạo Minigame → Tạo Questions (batch) → Save cascade |
| **2** | G.27 | Bắt đầu lượt chơi quiz | `POST /api/minigames/{id}/start` | Student | Validate minigame đang mở → Kiểm tra lượt chơi trước → Tạo Attempt (IN_PROGRESS) → Trả về questions (ẩn đáp án) |
| **3** | G.28 | Nộp bài quiz | `POST /api/minigames/attempts/{attemptId}/submit` | Student | Validate attempt IN_PROGRESS → Kiểm tra timeout → Chấm điểm từng câu → Cập nhật attempt COMPLETED → Cộng điểm ScoreRecord |
| **4** | G.29 | Xem lịch sử lượt chơi | `GET /api/minigames/{id}/attempts/my` | Student | Lấy studentId từ auth → Truy vấn attempts theo minigame → Trả về list lịch sử (score, time, status) |

### Các Entity chính trong Module

| Entity | Mô tả | Trường chính |
|--------|-------|-------------|
| **Minigame** | Định nghĩa một trò chơi/quiz | `id`, `title`, `description`, `activityId`, `startTime`, `endTime`, `status`, `allowRetry`, `passingScore`, `timeLimit` |
| **Question** | Câu hỏi trong quiz | `id`, `minigameId`, `text`, `options[]`, `correctAnswer`, `score`, `orderIndex` |
| **MinigameAttempt** | Lượt chơi của sinh viên | `id`, `studentId`, `minigameId`, `startTime`, `endTime`, `status` (IN_PROGRESS/COMPLETED/EXPIRED), `answers[]`, `score`, `isTimeExpired` |
| **ScoreRecord** | Bản ghi điểm thưởng của sinh viên | `id`, `studentId`, `score`, `sourceType` (MINIGAME), `sourceId`, `earnedAt`, `description` |

### Các Exception / Business Rule chính

| Exception | Tình huống | HTTP Status |
|-----------|-----------|-------------|
| `ActivityNotFoundException` | Activity liên kết không tồn tại | 404 |
| `MinigameNotFoundException` | Minigame không tồn tại | 404 |
| `MinigameNotOpenException` | Chưa đến giờ hoặc đã hết thời gian mở | 400 |
| `AttemptNotFoundException` | Attempt không tồn tại | 404 |
| `UnauthorizedAttemptException` | Attempt thuộc về student khác | 403 |
| `InvalidAttemptStatusException` | Attempt không ở trạng thái IN_PROGRESS | 400 |
| `AttemptInProgressException` | Student đang có lượt chơi chưa hoàn thành | 409 |
| `AlreadyPlayedException` | Student đã chơi và không cho phép chơi lại | 403 |
| `InvalidQuestionException` | Câu trả lời thuộc question không hợp lệ | 400 |

---

*Generated by CampusLife System Analyst*  
*Module: Minigame / Quiz*  
*Format: Mermaid Sequence Diagram v2*
