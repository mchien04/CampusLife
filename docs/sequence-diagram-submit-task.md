# Sequence Diagram - Chức năng Nộp Bài thu hoạch

## Mô tả
Sequence diagram mô tả luồng xử lý nộp bài thu hoạch (TaskSubmission) trong hệ thống CampusLife. Chức năng này cho phép sinh viên nộp bài cho nhiệm vụ đã được phân công.

## Sequence Diagrams

### 1. Nộp Bài thu hoạch (Submit Task)

```mermaid
sequenceDiagram
    participant Student as Student
    participant Client as Client/Frontend
    participant SubmissionController as TaskSubmissionController
    participant SubmissionService as TaskSubmissionServiceImpl
    participant Repository as Repository
    participant Database as Database

    Student->>Client: Nhập nội dung và chọn file<br/>(content, files)
    Client->>SubmissionController: POST /api/submissions/task/{taskId}<br/>(content, files)
    
    Note over SubmissionController: Lấy studentId từ authentication
    SubmissionController->>SubmissionController: getStudentIdFromAuth(authentication)
    SubmissionController->>Repository: getStudentIdByUsername(username)
    Repository->>Database: SELECT id FROM students<br/>WHERE user_id = (SELECT id FROM users<br/>WHERE username = ?)
    Database-->>Repository: studentId
    Repository-->>SubmissionController: studentId
    
    SubmissionController->>SubmissionService: submitTask(taskId, studentId, content, files)
    
    Note over SubmissionService: Kiểm tra task, student tồn tại<br/>và chưa nộp bài
    SubmissionService->>Repository: findById(taskId), findByIdAndIsDeletedFalse(studentId),<br/>findByTaskIdAndStudentIdAndIsDeletedFalse(taskId, studentId)
    Repository->>Database: SELECT * FROM activity_tasks WHERE id = ?<br/>SELECT * FROM students WHERE id = ? AND is_deleted = false<br/>SELECT * FROM task_submissions WHERE task_id = ? AND student_id = ? AND is_deleted = false
    Database-->>Repository: ActivityTask, Student, Optional<TaskSubmission>
    Repository-->>SubmissionService: ActivityTask, Student, Optional<TaskSubmission>
    
    Note over SubmissionService: Tạo submission và upload files (nếu có)
    SubmissionService->>SubmissionService: create TaskSubmission<br/>(task, student, content, status = SUBMITTED)<br/>Upload files to uploads/submissions/
    SubmissionService->>Repository: save(submission)
    Repository->>Database: INSERT INTO task_submissions<br/>(task_id, student_id, content,<br/>file_urls, status, submitted_at)<br/>VALUES (?, ?, ?, ?, 'SUBMITTED', ?)
    Database-->>Repository: TaskSubmission saved
    Repository-->>SubmissionService: TaskSubmission
    
    Note over SubmissionService: Cập nhật TaskAssignment status = ASSIGNED
    SubmissionService->>Repository: findByTaskIdAndStudentId(taskId, studentId)
    Repository->>Database: SELECT * FROM task_assignments<br/>WHERE task_id = ? AND student_id = ?
    Database-->>Repository: TaskAssignment
    Repository-->>SubmissionService: TaskAssignment
    
    SubmissionService->>Repository: save(assignment) với status = ASSIGNED
    Repository->>Database: UPDATE task_assignments<br/>SET status = 'ASSIGNED'<br/>WHERE id = ?
    Database-->>Repository: TaskAssignment updated
    Repository-->>SubmissionService: TaskAssignment
    
    SubmissionService-->>SubmissionController: Response(success, TaskSubmissionResponse)
    SubmissionController-->>Client: ResponseEntity.ok()
    Client-->>Student: Hiển thị thông báo nộp bài thành công
```

### 2. Cập nhật Bài nộp (Update Submission)

```mermaid
sequenceDiagram
    participant Student as Student
    participant Client as Client/Frontend
    participant SubmissionController as TaskSubmissionController
    participant SubmissionService as TaskSubmissionServiceImpl
    participant Repository as Repository
    participant Database as Database

    Student->>Client: Cập nhật nội dung và file
    Client->>SubmissionController: PUT /api/submissions/{submissionId}<br/>(content, files)
    
    Note over SubmissionController: Lấy studentId từ authentication
    SubmissionController->>SubmissionController: getStudentIdFromAuth(authentication)
    SubmissionController->>Repository: getStudentIdByUsername(username)
    Repository->>Database: SELECT id FROM students<br/>WHERE user_id = (SELECT id FROM users<br/>WHERE username = ?)
    Database-->>Repository: studentId
    Repository-->>SubmissionController: studentId
    
    SubmissionController->>SubmissionService: updateSubmission(submissionId, studentId, content, files)
    
    Note over SubmissionService: Kiểm tra submission tồn tại<br/>và quyền (chỉ student đã nộp mới cập nhật được)
    SubmissionService->>Repository: findById(submissionId)
    Repository->>Database: SELECT * FROM task_submissions<br/>WHERE id = ?
    Database-->>Repository: TaskSubmission
    Repository-->>SubmissionService: TaskSubmission
    
    Note over SubmissionService: Cập nhật content và upload files mới (nếu có)
    SubmissionService->>SubmissionService: submission.setContent(content)<br/>Upload files to uploads/submissions/
    SubmissionService->>Repository: save(submission)
    Repository->>Database: UPDATE task_submissions<br/>SET content = ?, file_urls = ?,<br/>updated_at = ?<br/>WHERE id = ?
    Database-->>Repository: TaskSubmission updated
    Repository-->>SubmissionService: TaskSubmission
    
    SubmissionService-->>SubmissionController: Response(success, TaskSubmissionResponse)
    SubmissionController-->>Client: ResponseEntity.ok()
    Client-->>Student: Hiển thị thông báo cập nhật thành công
```

## Ghi chú

1. **Quyền truy cập**: 
   - Nộp bài và cập nhật bài: Chỉ Student có thể thực hiện cho chính mình

2. **Nộp bài thu hoạch**:
   - **Kiểm tra điều kiện**:
     - Task tồn tại
     - Student tồn tại
     - Chưa nộp bài cho task này (mỗi task chỉ nộp 1 lần)
   
   - **File upload**:
     - Hỗ trợ upload nhiều file
     - File được lưu vào thư mục `uploads/submissions/`
     - Tên file: UUID + tên file gốc để tránh trùng lặp
     - File URLs được lưu dưới dạng string (phân cách bằng dấu phẩy)
   
   - **Trạng thái submission**: 
     - Status = `SUBMITTED` khi nộp bài
   
   - **Cập nhật TaskAssignment**:
     - Tự động cập nhật TaskAssignment status = `ASSIGNED` khi sinh viên nộp bài

3. **Cập nhật bài nộp**:
   - **Kiểm tra quyền**: 
     - Chỉ sinh viên đã nộp bài mới có thể cập nhật
     - Kiểm tra submission thuộc về student đó
   
   - **Cập nhật**:
     - Có thể cập nhật content và files
     - Files mới sẽ thay thế files cũ
     - Status vẫn giữ nguyên (SUBMITTED)

4. **File Storage**:
   - Files được lưu trên file system (không phải database)
   - Đường dẫn: `uploads/submissions/{UUID}_{originalFileName}`
   - File URLs được lưu trong database dưới dạng string

5. **Trạng thái Submission**:
   - `SUBMITTED`: Đã nộp bài
   - `GRADED`: Đã được chấm điểm
   - `RETURNED`: Trả lại để sửa
   - `LATE`: Nộp muộn
   - `MISSING`: Chưa nộp

