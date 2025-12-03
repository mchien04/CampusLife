# Sequence Diagram - Chức năng Chấm điểm Bài thu hoạch

## Mô tả
Sequence diagram mô tả luồng xử lý chấm điểm bài thu hoạch (TaskSubmission) trong hệ thống CampusLife. Chức năng này cho phép Admin/Manager chấm điểm bài nộp của sinh viên với kết quả đạt/không đạt và nhận xét.

## Sequence Diagram

### Chấm điểm Bài thu hoạch (Grade Submission)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant SubmissionController as TaskSubmissionController
    participant SubmissionService as TaskSubmissionServiceImpl
    participant Repository as Repository
    participant Database as Database

    Admin->>Client: Chọn bài nộp và nhập điểm<br/>(isCompleted, feedback)
    Client->>SubmissionController: PUT /api/submissions/{submissionId}/grade<br/>(isCompleted, feedback)
    
    Note over SubmissionController: Lấy graderId từ authentication
    SubmissionController->>Repository: findByUsernameAndIsDeletedFalse(username)
    Repository->>Database: SELECT * FROM users<br/>WHERE username = ? AND is_deleted = false
    Database-->>Repository: User
    Repository-->>SubmissionController: User (graderId)
    
    SubmissionController->>SubmissionService: gradeSubmission(submissionId, graderId, isCompleted, feedback)
    
    Note over SubmissionService: Kiểm tra submission và grader
    SubmissionService->>Repository: findById(submissionId)
    Repository->>Database: SELECT * FROM task_submissions<br/>WHERE id = ?
    Database-->>Repository: TaskSubmission
    Repository-->>SubmissionService: TaskSubmission
    
    SubmissionService->>Repository: findById(graderId)
    Repository->>Database: SELECT * FROM users<br/>WHERE id = ?
    Database-->>Repository: User
    Repository-->>SubmissionService: User (grader)
    
    Note over SubmissionService: Tính điểm:<br/>- Đạt: maxPoints<br/>- Không đạt: -penaltyPointsIncomplete
    
    Note over SubmissionService: Cập nhật submission
    SubmissionService->>SubmissionService: setScore, setFeedback,<br/>setGrader, setStatus(GRADED)
    SubmissionService->>Repository: save(submission)
    Repository->>Database: UPDATE task_submissions<br/>SET is_completed = ?, score = ?,<br/>feedback = ?, grader_id = ?,<br/>status = 'GRADED', graded_at = ?<br/>WHERE id = ?
    Database-->>Repository: TaskSubmission updated
    Repository-->>SubmissionService: TaskSubmission
    
    Note over SubmissionService: Cập nhật TaskAssignment status
    SubmissionService->>Repository: findByTaskIdAndStudentId(taskId, studentId)
    Repository->>Database: SELECT * FROM task_assignments<br/>WHERE task_id = ? AND student_id = ?
    Database-->>Repository: TaskAssignment
    Repository-->>SubmissionService: TaskAssignment
    
    SubmissionService->>Repository: save(assignment) với status = COMPLETED
    Repository->>Database: UPDATE task_assignments<br/>SET status = 'COMPLETED'<br/>WHERE id = ?
    Database-->>Repository: TaskAssignment updated
    Repository-->>SubmissionService: TaskAssignment
    
    Note over SubmissionService: Tự động cập nhật ActivityParticipation<br/>và StudentScore (nếu đủ điều kiện)
    
    alt Activity requires submission và registration = ATTENDED
        SubmissionService->>Repository: findByRegistration(registration)
        Repository->>Database: SELECT * FROM activity_participations<br/>WHERE registration_id = ?
        Database-->>Repository: ActivityParticipation
        Repository-->>SubmissionService: ActivityParticipation
        
        SubmissionService->>Repository: save(participation) với<br/>isCompleted, pointsEarned, COMPLETED
        Repository->>Database: UPDATE activity_participations<br/>SET is_completed = ?, points_earned = ?,<br/>participation_type = 'COMPLETED'<br/>WHERE id = ?
        Database-->>Repository: ActivityParticipation updated
        Repository-->>SubmissionService: ActivityParticipation
        
        Note over SubmissionService: Tính tổng điểm và cập nhật StudentScore
        SubmissionService->>Repository: findByStudentIdAndScoreType(studentId, scoreType)
        Repository->>Database: SELECT * FROM activity_participations<br/>WHERE student_id = ? AND score_type = ?
        Database-->>Repository: List<ActivityParticipation>
        Repository-->>SubmissionService: List<ActivityParticipation>
        
        SubmissionService->>Repository: findByStudentIdAndSemesterIdAndScoreType(...)
        Repository->>Database: SELECT * FROM student_scores<br/>WHERE student_id = ? AND semester_id = ?<br/>AND score_type = ?
        Database-->>Repository: StudentScore
        Repository-->>SubmissionService: StudentScore
        
        SubmissionService->>Repository: save(score) với tổng điểm mới
        Repository->>Database: UPDATE student_scores<br/>SET score = ?, activity_ids = ?<br/>WHERE id = ?
        Database-->>Repository: StudentScore updated
        Repository-->>SubmissionService: StudentScore
        
        SubmissionService->>Repository: save(scoreHistory)
        Repository->>Database: INSERT INTO score_histories<br/>(score_id, old_score, new_score,<br/>changed_by_id, change_date, reason, activity_id)
        Database-->>Repository: ScoreHistory saved
        Repository-->>SubmissionService: ScoreHistory
    end
    
    SubmissionService-->>SubmissionController: Response(success, TaskSubmissionResponse)
    SubmissionController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo chấm điểm thành công
```

## Ghi chú

1. **Quyền truy cập**: Chỉ Admin và Manager mới có quyền chấm điểm bài thu hoạch.

2. **Tính điểm**:
   - **Đạt (isCompleted = true)**: Điểm = `maxPoints` từ Activity
   - **Không đạt (isCompleted = false)**: Điểm = `-penaltyPointsIncomplete` (số âm)

3. **Cập nhật tự động**:
   - TaskAssignment status được cập nhật thành `COMPLETED`
   - ActivityParticipation được cập nhật nếu activity yêu cầu submission và registration status = `ATTENDED`
   - StudentScore được cập nhật tổng hợp điểm theo scoreType và học kỳ hiện tại
   - ScoreHistory được tạo để lưu lịch sử thay đổi điểm

4. **Trạng thái submission**: Sau khi chấm điểm, status của submission được cập nhật thành `GRADED`.

