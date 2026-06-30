# Sequence Diagram - Score & Submission (Điểm & Bài thu hoạch)

> Hệ thống: CampusLife (Spring Boot + React)  
> Nhóm chức năng: Score & Submission  
> Các participant: Admin/Manager/Student, Client, Controller, Service, Repository, Database, FileStorage

---

## 1. Chấm điểm bài thu hoạch (POST /api/admin/submissions/{id}/grade)

```mermaid
sequenceDiagram
    autonumber
    actor AM as Admin/Manager
    participant C as Client
    participant CTL as SubmissionController
    participant SS as SubmissionService
    participant SRS as ScoreRecordService
    participant SSS as StudentSemesterScoreService
    participant NS as NotificationService
    participant SR as SubmissionRepository
    participant SCR as ScoreRecordRepository
    participant SSR as StudentSemesterScoreRepository
    participant AR as ActivityRepository
    participant DB as Database

    Note over AM,DB: 1.1 Admin/Manager mở danh sách bài nộp cần chấm điểm
    AM->>C: Chọn bài nộp và nhấn "Chấm điểm"
    C->>CTL: POST /api/admin/submissions/{id}/grade<br/>Body: { score, feedback }
    CTL->>SS: gradeSubmission(id, gradeDTO, auth)

    Note over SS,DB: 1.2 Tìm và kiểm tra Submission
    SS->>SR: findById(id)
    SR->>DB: SELECT * FROM submission WHERE id = ?
    DB-->>SR:' "Submission record"'
    SR-->>SS: Optional<Submission>
    alt Submission không tồn tại
        SS-->>CTL: throw ResourceNotFoundException
        CTL-->>C:' "404 Not Found"'
        C-->>AM:' "Hiển thị thông báo lỗi"'
    else Submission đã được chấm điểm
        SS-->>CTL:' throw BusinessException("Already graded")'
        CTL-->>C:' "409 Conflict"'
        C-->>AM:' "Hiển thị thông báo lỗi"'
    else Hợp lệ
        Note over SS,DB: 1.3 Cập nhật Submission
        SS->>SS: submission.setStatus(GRADED)<br/>submission.setScore(score)<br/>submission.setFeedback(feedback)<br/>submission.setGradedAt(now)
        SS->>SR: save(submission)
        SR->>DB: UPDATE submission SET ... WHERE id = ?
        DB-->>SR:' "Updated submission"'
        SR-->>SS: Submission đã cập nhật

        Note over SRS,DB: 1.4 Tạo ScoreRecord
        SS->>SRS: createScoreRecord(studentId, activityId, score, SUBMISSION, semesterId)
        SRS->>SCR: save(scoreRecord)
        SCR->>DB: INSERT INTO score_record VALUES (...)
        DB-->>SCR:' "ScoreRecord record"'
        SCR-->>SRS:' "ScoreRecord đã lưu"'
        SRS-->>SS: ScoreRecord

        Note over SSS,DB: 1.5 Cập nhật tổng điểm StudentSemesterScore
        SS->>SSS: updateTotalScore(studentId, semesterId)
        SSS->>SCR: findAllByStudentIdAndSemesterId(studentId, semesterId)
        SCR->>DB: SELECT * FROM score_record WHERE student_id = ? AND semester_id = ?
        DB-->>SCR:' "List<ScoreRecord>"'
        SCR-->>SSS:' "List<ScoreRecord>"'
        SSS->>SSS: totalScore = sum(score)<br/>activityCount = count(records)
        SSS->>SSR: findByStudentIdAndSemesterId(studentId, semesterId)
        SSR->>DB: SELECT * FROM student_semester_score WHERE student_id = ? AND semester_id = ?
        DB-->>SSR:' "StudentSemesterScore record"'
        SSR-->>SSS:' "Optional<StudentSemesterScore>"'
        alt Chưa có bản ghi
            SSS->>SSS: Tạo mới StudentSemesterScore
        end
        SSS->>SSS: sss.setTotalScore(totalScore)<br/>sss.setActivityCount(activityCount)<br/>sss.setUpdatedAt(now)
        SSS->>SSR: save(sss)
        SSR->>DB: UPDATE/INSERT student_semester_score SET ...
        DB-->>SSR:' "StudentSemesterScore đã lưu"'
        SSR-->>SSS:' "StudentSemesterScore"'
        SSS-->>SS: StudentSemesterScore đã cập nhật

        Note over NS,DB: 1.6 Gửi notification cho sinh viên
        SS->>AR: findById(activityId)
        AR->>DB: SELECT * FROM activity WHERE id = ?
        DB-->>AR:' "Activity record"'
        AR-->>SS:' "Activity"'
        SS->>NS: sendNotification(studentId, "Bài nộp đã được chấm điểm",<br/>"Activity: " + activityName + " - Điểm: " + score)
        NS-->>SS: Notification sent

        SS-->>CTL: SubmissionDTO (graded)
        CTL-->>C:' "200 OK + SubmissionDTO"'
        C-->>AM:' "Hiển thị thông báo chấm điểm thành công"'
    end
```

---

## 2. Nộp bài thu hoạch (POST /api/student/submissions)

```mermaid
sequenceDiagram
    autonumber
    actor S as Student
    participant C as Client
    participant CTL as SubmissionController
    participant SS as SubmissionService
    participant FS as FileStorageService
    participant AS as ActivityService
    participant NS as NotificationService
    participant SR as SubmissionRepository
    participant AR as ActivityRepository
    participant NR as NotificationRepository
    participant DB as Database

    Note over S,DB: 2.1 Student chọn hoạt động cần nộp bài
    S->>C: Chọn activity (yêu cầu nộp bài) + chọn file<br/>Nhấn "Nộp bài"
    C->>CTL: POST /api/student/submissions<br/>Multipart: { activityId, file }
    CTL->>SS: createSubmission(submissionDTO, file, auth)

    Note over SS,DB: 2.2 Kiểm tra Activity
    SS->>AS: findById(activityId)
    AS->>AR: findById(activityId)
    AR->>DB: SELECT * FROM activity WHERE id = ?
    DB-->>AR:' "Activity record"'
    AR-->>AS:' "Optional<Activity>"'
    AS-->>SS: Optional<Activity>
    alt Activity không tồn tại
        SS-->>CTL: throw ResourceNotFoundException
        CTL-->>C:' "404 Not Found"'
        C-->>S:' "Hiển thị lỗi"'
    else Activity không yêu cầu nộp bài
        SS-->>CTL:' throw BusinessException("Activity không yêu cầu nộp bài")'
        CTL-->>C:' "400 Bad Request"'
        C-->>S:' "Hiển thị lỗi"'
    else Đã quá hạn nộp bài
        SS-->>CTL:' throw BusinessException("Đã quá hạn nộp bài")'
        CTL-->>C:' "400 Bad Request"'
        C-->>S:' "Hiển thị lỗi"'
    else Hợp lệ
        Note over FS,DB: 2.3 Upload file
        SS->>FS: storeFile(file, "submissions/")
        FS->>FS: Validate file (type, size)<br/>Generate unique filename<br/>Save to storage
        FS-->>SS: fileUrl

        Note over SS,DB: 2.4 Tạo Submission
        SS->>SS: submission = new Submission()<br/>submission.setActivityId(activityId)<br/>submission.setStudentId(studentId)<br/>submission.setFileUrl(fileUrl)<br/>submission.setStatus(SUBMITTED)<br/>submission.setSubmittedAt(now)
        SS->>SR: save(submission)
        SR->>DB: INSERT INTO submission VALUES (...)
        DB-->>SR:' "Submission record"'
        SR-->>SS: Submission đã lưu

        Note over NS,DB: 2.5 Gửi notification cho Admin/Manager
        SS->>AS: getActivityManagers(activityId)
        AS->>AR: findById(activityId)
        AR->>DB: SELECT * FROM activity WHERE id = ?
        DB-->>AR:' "Activity (with managerIds)"'
        AR-->>AS:' "Activity"'
        AS-->>SS: List<managerIds>
        loop Với mỗi manager
            SS->>NS: sendNotification(managerId,<br/>"Có bài nộp mới",<br/>"Sinh viên " + studentName + " đã nộp bài cho activity: " + activityName)
            NS->>NR: save(notification)
            NR->>DB: INSERT INTO notification VALUES (...)
            DB-->>NR:' "Notification record"'
            NR-->>NS:' "Notification đã lưu"'
            NS-->>SS: Notification sent
        end

        SS-->>CTL: SubmissionDTO
        CTL-->>C:' "201 Created + SubmissionDTO"'
        C-->>S:' "Hiển thị thông báo nộp bài thành công"'
    end
```

---

## 3. Sửa bài thu hoạch (PUT /api/student/submissions/{id})

```mermaid
sequenceDiagram
    autonumber
    actor S as Student
    participant C as Client
    participant CTL as SubmissionController
    participant SS as SubmissionService
    participant FS as FileStorageService
    participant AS as ActivityService
    participant NS as NotificationService
    participant SR as SubmissionRepository
    participant AR as ActivityRepository
    participant NR as NotificationRepository
    participant DB as Database

    Note over S,DB: 3.1 Student chọn bài đã nộp để sửa
    S->>C: Chọn bài đã nộp (chưa chấm điểm)<br/>Chọn file mới + Nhấn "Cập nhật"
    C->>CTL: PUT /api/student/submissions/{id}<br/>Multipart: { file }
    CTL->>SS: updateSubmission(id, file, auth)

    Note over SS,DB: 3.2 Tìm và kiểm tra Submission
    SS->>SR: findById(id)
    SR->>DB: SELECT * FROM submission WHERE id = ?
    DB-->>SR:' "Submission record"'
    SR-->>SS: Optional<Submission>
    alt Submission không tồn tại
        SS-->>CTL: throw ResourceNotFoundException
        CTL-->>C:' "404 Not Found"'
        C-->>S:' "Hiển thị lỗi"'
    else Không phải bài của student này
        SS-->>CTL: throw AccessDeniedException
        CTL-->>C:' "403 Forbidden"'
        C-->>S:' "Hiển thị lỗi"'
    else Đã được chấm điểm
        SS-->>CTL:' throw BusinessException("Bài đã được chấm điểm, không thể sửa")'
        CTL-->>C:' "409 Conflict"'
        C-->>S:' "Hiển thị lỗi"'
    else Hợp lệ
        Note over FS,DB: 3.3 Xóa file cũ (nếu có)
        alt fileUrl cũ != null
            SS->>FS: deleteFile(oldFileUrl)
            FS->>FS: Xóa file khỏi storage
            FS-->>SS: deleted
        end

        Note over FS,DB: 3.4 Upload file mới
        SS->>FS: storeFile(file, "submissions/")
        FS->>FS: Validate file (type, size)<br/>Generate unique filename<br/>Save to storage
        FS-->>SS: newFileUrl

        Note over SS,DB: 3.5 Cập nhật Submission
        SS->>SS: submission.setFileUrl(newFileUrl)<br/>submission.setStatus(RESUBMITTED)<br/>submission.setUpdatedAt(now)
        SS->>SR: save(submission)
        SR->>DB: UPDATE submission SET file_url = ?, status = ?, updated_at = ? WHERE id = ?
        DB-->>SR:' "Updated submission"'
        SR-->>SS: Submission đã cập nhật

        Note over NS,DB: 3.6 Gửi notification cho Admin/Manager
        SS->>AS: getActivityManagers(activityId)
        AS->>AR: findById(activityId)
        AR->>DB: SELECT * FROM activity WHERE id = ?
        DB-->>AR:' "Activity (with managerIds)"'
        AR-->>AS:' "Activity"'
        AS-->>SS: List<managerIds>
        loop Với mỗi manager
            SS->>NS: sendNotification(managerId,<br/>"Bài nộp đã được cập nhật",<br/>"Sinh viên " + studentName + " đã cập nhật bài nộp cho activity: " + activityName)
            NS->>NR: save(notification)
            NR->>DB: INSERT INTO notification VALUES (...)
            DB-->>NR:' "Notification record"'
            NR-->>NS:' "Notification đã lưu"'
            NS-->>SS: Notification sent
        end

        SS-->>CTL: SubmissionDTO
        CTL-->>C:' "200 OK + SubmissionDTO"'
        C-->>S:' "Hiển thị thông báo cập nhật thành công"'
    end
```

---

## 4. Xem điểm chi tiết (GET /api/student/scores/detail)

```mermaid
sequenceDiagram
    autonumber
    actor S as Student
    participant C as Client
    participant CTL as ScoreController
    participant SRS as ScoreRecordService
    participant AR as ActivityRepository
    participant SCR as ScoreRecordRepository
    participant DB as Database

    Note over S,DB: 4.1 Student xem điểm chi tiết
    S->>C: Truy cập trang "Điểm chi tiết"
    C->>CTL: GET /api/student/scores/detail
    CTL->>SRS: getStudentScoreDetails(auth)

    Note over SRS,DB: 4.2 Lấy studentId và semester đang mở
    SRS->>SRS: studentId = getStudentIdFromAuth(auth)<br/>semesterId = getCurrentOpenSemester()

    Note over SRS,DB: 4.3 Tìm tất cả ScoreRecord
    SRS->>SCR: findAllByStudentIdAndSemesterId(studentId, semesterId)
    SCR->>DB: SELECT * FROM score_record WHERE student_id = ? AND semester_id = ? ORDER BY scored_at DESC
    DB-->>SCR:' "List<ScoreRecord>"'
    SCR-->>SRS:' "List<ScoreRecord>"'

    Note over SRS,DB: 4.4 Lấy thông tin Activity cho từng record
    loop Với mỗi ScoreRecord
        SRS->>AR: findById(record.getActivityId())
        AR->>DB: SELECT * FROM activity WHERE id = ?
        DB-->>AR:' "Activity record"'
        AR-->>SRS:' "Optional<Activity>"'
        SRS->>SRS: Build ScoreDetailDTO<br/>(activityName, score, sourceType, scoredAt, feedback)
    end

    SRS-->>CTL: List<ScoreDetailDTO>
    CTL-->>C:' "200 OK + List<ScoreDetailDTO>"'
    C-->>S:' "Hiển thị bảng điểm chi tiết<br/>(activityName, score, sourceType, scoredAt, feedback)"'
```

---

## 5. Xem tổng điểm (GET /api/student/scores/total)

```mermaid
sequenceDiagram
    autonumber
    actor S as Student
    participant C as Client
    participant CTL as ScoreController
    participant SSS as StudentSemesterScoreService
    participant SSR as StudentSemesterScoreRepository
    participant SR as SemesterRepository
    participant DB as Database

    Note over S,DB: 5.1 Student xem tổng điểm
    S->>C: Truy cập trang "Tổng điểm"
    C->>CTL: GET /api/student/scores/total
    CTL->>SSS: getStudentTotalScore(auth)

    Note over SSS,DB: 5.2 Lấy studentId và semester đang mở
    SSS->>SSS: studentId = getStudentIdFromAuth(auth)<br/>semesterId = getCurrentOpenSemester()

    Note over SSS,DB: 5.3 Tìm StudentSemesterScore
    SSS->>SSR: findByStudentIdAndSemesterId(studentId, semesterId)
    SSR->>DB: SELECT * FROM student_semester_score WHERE student_id = ? AND semester_id = ?
    DB-->>SSR:' "StudentSemesterScore record"'
    SSR-->>SSS:' "Optional<StudentSemesterScore>"'

    alt Không có bản ghi
        SSS->>SSS: Tạo StudentSemesterScoreDTO<br/>totalScore = 0<br/>rank = "-"<br/>semesterName = getSemesterName(semesterId)
    else Có bản ghi
        Note over SSS,DB: 5.4 Lấy tên semester và tính rank
        SSS->>SR: findById(semesterId)
        SR->>DB: SELECT * FROM semester WHERE id = ?
        DB-->>SR:' "Semester record"'
        SR-->>SSS: Semester
        SSS->>SSR: countStudentsWithHigherScore(studentId, semesterId, totalScore)
        SSR->>DB: SELECT COUNT(*) FROM student_semester_score WHERE semester_id = ? AND total_score > ?
        DB-->>SSR:' "count"'
        SSR-->>SSS:' "rank = count + 1"'
        SSS->>SSS: Build StudentSemesterScoreDTO<br/>(totalScore, rank, semesterName, activityCount)
    end

    SSS-->>CTL: StudentSemesterScoreDTO
    CTL-->>C:' "200 OK + StudentSemesterScoreDTO"'
    C-->>S:' "Hiển thị tổng điểm, xếp hạng, tên học kỳ"'
```

---

## 6. Xếp hạng theo điểm (GET /api/rankings)

```mermaid
sequenceDiagram
    autonumber
    actor U as Sinh viên/Admin
    participant C as Client
    participant CTL as RankingController
    participant SSS as StudentSemesterScoreService
    participant SSR as StudentSemesterScoreRepository
    participant SR as SemesterRepository
    participant UR as UserRepository
    participant DB as Database

    Note over U,DB: 6.1 Sinh viên/Admin xem bảng xếp hạng
    U->>C: Truy cập trang "Bảng xếp hạng"
    C->>CTL: GET /api/rankings?page=0&size=20
    CTL->>SSS: getRankings(pageable, semesterId)

    Note over SSS,DB: 6.2 Lấy semester đang mở
    alt semesterId không được truyền
        SSS->>SSS: semesterId = getCurrentOpenSemester()
    end

    Note over SSS,DB: 6.3 Lấy danh sách xếp hạng
    SSS->>SSR: findAllBySemesterIdOrderByTotalScoreDesc(semesterId, pageable)
    SSR->>DB: SELECT sss.*, u.full_name, u.student_code, u.class_name<br/>FROM student_semester_score sss<br/>JOIN users u ON sss.student_id = u.id<br/>WHERE sss.semester_id = ?<br/>ORDER BY sss.total_score DESC<br/>LIMIT ? OFFSET ?
    DB-->>SSR:' "Page<StudentSemesterScore> (with user info)"'
    SSR-->>SSS:' "Page<StudentSemesterScore>"'

    Note over SSS,DB: 6.4 Build RankingDTO với rank
    SSS->>SSS: startRank = page * size + 1<br/>rank = startRank + index
    loop Với mỗi record trong page
        SSS->>SSS: Build RankingDTO<br/>(rank, studentName, studentCode, className, totalScore, activityCount)
    end

    SSS-->>CTL: Page<RankingDTO>
    CTL-->>C:' "200 OK + Page<RankingDTO>"'
    C-->>U:' "Hiển thị bảng xếp hạng<br/>(rank, studentName, studentCode, className, totalScore, activityCount)"'
```

---

## 7. Lịch sử điểm (GET /api/admin/scores/history/student/{id})

```mermaid
sequenceDiagram
    autonumber
    actor A as Admin
    participant C as Client
    participant CTL as ScoreController
    participant SRS as ScoreRecordService
    participant SS as StudentService
    participant SCR as ScoreRecordRepository
    participant UR as UserRepository
    participant AR as ActivityRepository
    participant SR as SemesterRepository
    participant DB as Database

    Note over A,DB: 7.1 Admin xem lịch sử điểm của sinh viên
    A->>C: Chọn sinh viên + Nhấn "Lịch sử điểm"
    C->>CTL: GET /api/admin/scores/history/student/{id}?semesterId=?
    CTL->>SRS: getStudentScoreHistory(studentId, semesterId)

    Note over SRS,DB: 7.2 Kiểm tra sinh viên tồn tại
    SRS->>SS: findById(studentId)
    SS->>UR: findById(studentId)
    UR->>DB: SELECT * FROM users WHERE id = ? AND role = 'STUDENT'
    DB-->>UR:' "User record"'
    UR-->>SS: Optional<User>
    SS-->>SRS: Optional<User>
    alt Sinh viên không tồn tại
        SRS-->>CTL: throw ResourceNotFoundException
        CTL-->>C:' "404 Not Found"'
        C-->>A:' "Hiển thị lỗi"'
    else Hợp lệ
        Note over SRS,DB: 7.3 Lấy tất cả ScoreRecord của sinh viên
        alt semesterId có giá trị
            SRS->>SCR: findAllByStudentIdAndSemesterId(studentId, semesterId)
            SCR->>DB: SELECT * FROM score_record WHERE student_id = ? AND semester_id = ? ORDER BY scored_at DESC
        else Không lọc theo semester
            SRS->>SCR: findAllByStudentId(studentId)
            SCR->>DB: SELECT * FROM score_record WHERE student_id = ? ORDER BY scored_at DESC
        end
        DB-->>SCR:' "List<ScoreRecord>"'
        SCR-->>SRS:' "List<ScoreRecord>"'

        Note over SRS,DB: 7.4 Lấy thông tin bổ sung cho từng record
        loop Với mỗi ScoreRecord
            SRS->>AR: findById(record.getActivityId())
            AR->>DB: SELECT * FROM activity WHERE id = ?
            DB-->>AR:' "Activity record"'
            AR-->>SRS:' "Optional<Activity>"'
            SRS->>SR: findById(record.getSemesterId())
            SR->>DB: SELECT * FROM semester WHERE id = ?
            DB-->>SR:' "Semester record"'
            SR-->>SRS: Semester
            SRS->>SRS: Build ScoreHistoryDTO<br/>(activityName, score, sourceType, scoredAt, feedback, semesterName, gradedBy)
        end

        SRS-->>CTL: List<ScoreHistoryDTO>
        CTL-->>C:' "200 OK + List<ScoreHistoryDTO>"'
        C-->>A:' "Hiển thị lịch sử điểm chi tiết<br/>(có thể filter theo học kỳ)"'
    end
```

---

## 8. Tính lại điểm (POST /api/admin/scores/recalculate)

```mermaid
sequenceDiagram
    autonumber
    actor A as Admin
    participant C as Client
    participant CTL as ScoreController
    participant SSS as StudentSemesterScoreService
    participant SRS as ScoreRecordService
    participant SSR as StudentSemesterScoreRepository
    participant SCR as ScoreRecordRepository
    participant SR as SemesterRepository
    participant DB as Database

    Note over A,DB: 8.1 Admin click tính lại điểm
    A->>C: Chọn học kỳ + Nhấn "Tính lại điểm"
    C->>CTL: POST /api/admin/scores/recalculate<br/>Body: { semesterId }
    CTL->>SSS: recalculateScores(semesterId)

    Note over SSS,DB: 8.2 Kiểm tra semester
    SSS->>SR: findById(semesterId)
    SR->>DB: SELECT * FROM semester WHERE id = ?
    DB-->>SR:' "Semester record"'
    SR-->>SSS: Optional<Semester>
    alt Semester không tồn tại
        SSS-->>CTL: throw ResourceNotFoundException
        CTL-->>C:' "404 Not Found"'
        C-->>A:' "Hiển thị lỗi"'
    else Hợp lệ
        Note over SSS,DB: 8.3 Lấy tất cả StudentSemesterScore trong semester
        SSS->>SSR: findAllBySemesterId(semesterId)
        SSR->>DB: SELECT * FROM student_semester_score WHERE semester_id = ?
        DB-->>SSR:' "List<StudentSemesterScore>"'
        SSR-->>SSS:' "List<StudentSemesterScore>"'

        Note over SSS,DB: 8.4 Tính lại điểm cho từng sinh viên
        SSS->>SSS: recalculatedCount = 0
        loop Với mỗi StudentSemesterScore
            SSS->>SCR: findAllByStudentIdAndSemesterId(studentId, semesterId)
            SCR->>DB: SELECT * FROM score_record WHERE student_id = ? AND semester_id = ?
            DB-->>SCR:' "List<ScoreRecord>"'
            SCR-->>SSS:' "List<ScoreRecord>"'

            SSS->>SSS: totalScore = sum(score for each record)<br/>activityCount = records.size()
            SSS->>SSS: sss.setTotalScore(totalScore)<br/>sss.setActivityCount(activityCount)<br/>sss.setUpdatedAt(now)<br/>sss.setRecalculatedAt(now)
            SSS->>SSS: recalculatedCount++
        end

        Note over SSS,DB: 8.5 Lưu tất cả (batch update)
        SSS->>SSR: saveAll(list)
        SSR->>DB: BEGIN TRANSACTION<br/>UPDATE student_semester_score SET ...<br/>COMMIT
        DB-->>SSR:' "Batch updated"'
        SSR-->>SSS:' "List<StudentSemesterScore> đã lưu"'

        SSS-->>CTL: RecalculateResultDTO<br/>(recalculatedCount, semesterName, timestamp)
        CTL-->>C:' "200 OK + RecalculateResultDTO"'
        C-->>A:' "Hiển thị thông báo<br/>"Đã tính lại điểm cho X sinh viên""'
    end
```

---

## 9. Xem điểm bài thu hoạch (Admin) (GET /api/admin/submissions)

```mermaid
sequenceDiagram
    autonumber
    actor A as Admin
    participant C as Client
    participant CTL as SubmissionController
    participant SS as SubmissionService
    participant SR as SubmissionRepository
    participant UR as UserRepository
    participant AR as ActivityRepository
    participant DB as Database

    Note over A,DB: 9.1 Admin xem danh sách bài nộp
    A->>C: Truy cập trang "Quản lý bài nộp"<br/>Filter: activityId, status, pagination
    C->>CTL: GET /api/admin/submissions?activityId=&status=&page=0&size=20
    CTL->>SS: getSubmissions(activityId, status, pageable)

    Note over SS,DB: 9.2 Truy vấn với filter và pagination
    alt Có activityId và status
        SS->>SR: findByActivityIdAndStatus(activityId, status, pageable)
        SR->>DB: SELECT s.*, u.full_name, u.student_code, a.title<br/>FROM submission s<br/>JOIN users u ON s.student_id = u.id<br/>JOIN activity a ON s.activity_id = a.id<br/>WHERE s.activity_id = ? AND s.status = ?<br/>ORDER BY s.submitted_at DESC<br/>LIMIT ? OFFSET ?
    else Chỉ có activityId
        SS->>SR: findByActivityId(activityId, pageable)
        SR->>DB: SELECT ... WHERE s.activity_id = ? ...
    else Chỉ có status
        SS->>SR: findByStatus(status, pageable)
        SR->>DB: SELECT ... WHERE s.status = ? ...
    else Không filter
        SS->>SR: findAllByOrderBySubmittedAtDesc(pageable)
        SR->>DB: SELECT ... ORDER BY s.submitted_at DESC ...
    end
    DB-->>SR:' "Page<Submission> (with joins)"'
    SR-->>SS: Page<Submission>

    Note over SS,DB: 9.3 Build SubmissionAdminDTO
    loop Với mỗi Submission trong page
        SS->>SS: Build SubmissionAdminDTO<br/>(studentName, activityName, fileUrl, status, submittedAt, score, feedback, gradedAt)
    end

    SS-->>CTL: Page<SubmissionAdminDTO>
    CTL-->>C:' "200 OK + Page<SubmissionAdminDTO>"'
    C-->>A:' "Hiển thị danh sách bài nộp<br/>(studentName, activityName, fileUrl, status, submittedAt, score)<br/>với pagination và filter"'
```

---

## Tóm tắt thành phần và chức năng

### Thành phần tham gia (Participants)

| Thành phần | Vai trò |
|------------|---------|
| **Admin/Manager** | Người quản lý hệ thống, có quyền chấm điểm, xem lịch sử điểm, tính lại điểm, quản lý bài nộp |
| **Student** | Sinh viên, có quyền nộp bài, sửa bài, xem điểm chi tiết và tổng điểm của mình |
| **Client** | Ứng dụng React frontend, giao diện người dùng, xử lý form upload file và hiển thị dữ liệu |
| **Controller** | Lớp REST API Controller (Spring Boot), tiếp nhận request, validate input, trả về response HTTP |
| **Service** | Lớp Business Logic, xử lý nghiệp vụ chính: validation, tính toán, orchestrate repository calls |
| **Repository** | Lớp Data Access (Spring Data JPA), thực hiện truy vấn CSDL, mapping Entity ↔ DTO |
| **Database** | Cơ sở dữ liệu relational (PostgreSQL/MySQL), lưu trữ dữ liệu persistence |
| **FileStorage** | Hệ thống lưu trữ file (local filesystem / cloud storage: S3, MinIO), xử lý upload/download/xóa file |

### Chức năng chính (Use Cases)

| STT | Chức năng | Actor | Endpoint | Mô tả |
|-----|-----------|-------|----------|-------|
| 1 | Chấm điểm bài thu hoạch | Admin/Manager | `POST /api/admin/submissions/{id}/grade` | Chấm điểm, tạo ScoreRecord, cập nhật tổng điểm, gửi notification |
| 2 | Nộp bài thu hoạch | Student | `POST /api/student/submissions` | Upload file, tạo Submission, gửi notification cho manager |
| 3 | Sửa bài thu hoạch | Student | `PUT /api/student/submissions/{id}` | Xóa file cũ, upload file mới, cập nhật Submission, gửi notification |
| 4 | Xem điểm chi tiết | Student | `GET /api/student/scores/detail` | Xem tất cả điểm theo từng hoạt động trong học kỳ đang mở |
| 5 | Xem tổng điểm | Student | `GET /api/student/scores/total` | Xem tổng điểm tích lũy và xếp hạng trong học kỳ |
| 6 | Xếp hạng theo điểm | Sinh viên/Admin | `GET /api/rankings` | Bảng xếp hạng toàn hệ thống, có pagination |
| 7 | Lịch sử điểm (Admin) | Admin | `GET /api/admin/scores/history/student/{id}` | Xem toàn bộ lịch sử điểm của 1 sinh viên, có filter theo học kỳ |
| 8 | Tính lại điểm | Admin | `POST /api/admin/scores/recalculate` | Tính lại tổng điểm cho tất cả sinh viên trong học kỳ, batch update |
| 9 | Xem điểm bài thu hoạch (Admin) | Admin | `GET /api/admin/submissions` | Danh sách bài nộp với filter (activity, status) và pagination |

### Luồng dữ liệu chính

```
[Actor] → [Client] → [Controller] → [Service] → [Repository] → [Database]
                                    ↓
                              [FileStorage] (cho upload/download file)
                                    ↓
                              [NotificationService] (gửi thông báo real-time)
```

### Các trạng thái Submission

```
[PENDING] → [SUBMITTED] → [GRADED]
                ↓
           [RESUBMITTED] → [GRADED]
```

- **PENDING**: Chưa nộp bài (trạng thái mặc định)
- **SUBMITTED**: Đã nộp bài lần đầu
- **RESUBMITTED**: Đã cập nhật bài nộp (sửa bài)
- **GRADED**: Đã được chấm điểm (không thể sửa bài)

### Các bảng dữ liệu liên quan

| Bảng | Mô tả |
|------|-------|
| `submission` | Lưu thông tin bài nộp (studentId, activityId, fileUrl, status, score, feedback) |
| `score_record` | Lưu chi tiết từng điểm (studentId, activityId, score, sourceType, semesterId, scoredAt) |
| `student_semester_score` | Lưu tổng điểm tích lũy của sinh viên theo học kỳ (totalScore, activityCount, rank) |
| `activity` | Lưu thông tin hoạt động (title, requireSubmission, deadline, managerIds) |
| `semester` | Lưu thông tin học kỳ (name, startDate, endDate, isActive) |
| `users` | Lưu thông tin người dùng (fullName, studentCode, className, role) |
| `notification` | Lưu thông báo gửi đến người dùng |

---

*File được tạo cho hệ thống CampusLife - Nhóm chức năng Score & Submission*
