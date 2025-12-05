# Logic Phân Công Nhiệm Vụ - Tóm Tắt

## 1. CÁC ENTITY VÀ ENUM

### 1.1. Entity

#### **TaskAssignment** (Phân công nhiệm vụ)
- `id`: Long
- `task`: ActivityTask (nhiệm vụ được phân công)
- `student`: Student (sinh viên được phân công)
- `status`: TaskStatus (trạng thái phân công)
- `updatedAt`: LocalDateTime

#### **TaskSubmission** (Bài nộp của sinh viên)
- `id`: Long
- `task`: ActivityTask
- `student`: Student
- `content`: String (nội dung bài nộp)
- `fileUrls`: String (JSON string chứa danh sách URL file)
- `score`: Double
- `isCompleted`: Boolean (Đạt/Không đạt)
- `feedback`: String (nhận xét từ manager)
- `grader`: User (người chấm điểm)
- `status`: SubmissionStatus (trạng thái bài nộp)
- `submittedAt`: LocalDateTime
- `updatedAt`: LocalDateTime
- `gradedAt`: LocalDateTime

#### **ActivityTask** (Nhiệm vụ)
- `id`: Long
- `activity`: Activity (sự kiện chứa nhiệm vụ này)
- `name`: String
- `description`: String
- `deadline`: LocalDateTime
- `createdAt`: LocalDateTime

### 1.2. Enum

#### **TaskStatus** (Trạng thái phân công)
```java
PENDING,        // Đã phân công, chưa làm gì
ASSIGNED,       // Đã nộp bài (tự động cập nhật khi submit)
IN_PROGRESS,    // Đang làm (có thể dùng trong tương lai)
COMPLETED,      // Đã hoàn thành (tự động cập nhật khi chấm điểm)
OVERDUE         // Quá hạn (có thể dùng trong tương lai)
```

#### **SubmissionStatus** (Trạng thái bài nộp)
```java
SUBMITTED,  // Đã nộp (mặc định khi sinh viên nộp bài)
GRADED,     // Đã chấm điểm (tự động cập nhật khi manager chấm)
RETURNED,   // Trả lại để sửa (có thể dùng trong tương lai)
LATE,       // Nộp muộn (có thể dùng trong tương lai)
MISSING     // Chưa nộp (có thể dùng trong tương lai)
```

---

## 2. LUỒNG XỬ LÝ

### 2.1. Luồng Phân Công Nhiệm Vụ

```
1. Manager/Admin phân công nhiệm vụ
   → POST /api/tasks/assign
   → Tạo TaskAssignment với status = PENDING
```

**Code:**
```java
// ActivityTaskServiceImpl.assignTask()
TaskAssignment assignment = new TaskAssignment();
assignment.setTask(task);
assignment.setStudent(student);
assignment.setStatus(TaskStatus.PENDING); // Luôn PENDING khi phân công
```

### 2.2. Luồng Sinh Viên Nộp Bài

```
1. Sinh viên nộp bài
   → POST /api/submissions/task/{taskId}
   → Tạo TaskSubmission với status = SUBMITTED
   ↓
2. Tự động cập nhật TaskAssignment
   → status: PENDING → ASSIGNED
```

**Code:**
```java
// TaskSubmissionServiceImpl.submitTask()
TaskSubmission submission = new TaskSubmission();
submission.setStatus(SubmissionStatus.SUBMITTED);

// Tự động cập nhật TaskAssignment
TaskAssignment assignment = assignmentOpt.get();
assignment.setStatus(TaskStatus.ASSIGNED); // PENDING → ASSIGNED
```

### 2.3. Luồng Manager Chấm Điểm

```
1. Manager chấm điểm
   → PUT /api/submissions/{submissionId}/grade
   → Cập nhật TaskSubmission:
     - status: SUBMITTED → GRADED
     - isCompleted: true/false
     - score: điểm (từ activity.maxPoints hoặc penaltyPointsIncomplete)
     - feedback: nhận xét
     - grader: manager
     - gradedAt: thời gian chấm
   ↓
2. Tự động cập nhật TaskAssignment
   → status: ASSIGNED → COMPLETED
   ↓
3. Tự động cập nhật ActivityParticipation và StudentScore
   → Nếu activity.requiresSubmission = true
   → Và registration.status = ATTENDED
   → Cập nhật participation và cộng điểm vào StudentScore
```

**Code:**
```java
// TaskSubmissionServiceImpl.gradeSubmission()
submission.setStatus(SubmissionStatus.GRADED);
submission.setIsCompleted(isCompleted);
submission.setScore(points.doubleValue());
submission.setFeedback(feedback);
submission.setGrader(grader);
submission.setGradedAt(LocalDateTime.now());

// Tự động cập nhật TaskAssignment
TaskAssignment assignment = assignmentOpt.get();
assignment.setStatus(TaskStatus.COMPLETED); // ASSIGNED → COMPLETED
```

---

## 3. BẢNG TÓM TẮT CẬP NHẬT TRẠNG THÁI

### 3.1. TaskAssignment.status

| Hành động | Trạng thái cũ | Trạng thái mới | Ghi chú |
|-----------|---------------|----------------|---------|
| Manager phân công | - | `PENDING` | Mặc định khi tạo |
| Sinh viên nộp bài | `PENDING` | `ASSIGNED` | Tự động cập nhật |
| Manager chấm điểm | `ASSIGNED` | `COMPLETED` | Tự động cập nhật |

### 3.2. TaskSubmission.status

| Hành động | Trạng thái cũ | Trạng thái mới | Ghi chú |
|-----------|---------------|----------------|---------|
| Sinh viên nộp bài | - | `SUBMITTED` | Mặc định khi tạo |
| Manager chấm điểm | `SUBMITTED` | `GRADED` | Tự động cập nhật |

---

## 4. API CHO SINH VIÊN

### 4.1. Lấy Danh Sách Nhiệm Vụ Của Chính Mình (Sinh Viên)

**Endpoint:** `GET /api/assignments/my`

**Mô tả:** Lấy tất cả nhiệm vụ đã được phân công cho sinh viên (tự động lấy từ authentication)

**Response:**
```json
{
  "status": true,
  "message": "Student tasks retrieved successfully",
  "data": [
    {
      "id": 1,
      "taskId": 10,
      "taskName": "Viết báo cáo",
      "activityId": 5,
      "activityName": "Sự kiện ABC",
      "studentId": 123,
      "studentName": "Nguyễn Văn A",
      "studentCode": "SV001",
      "status": "PENDING",
      "updatedAt": "2025-01-15T10:00:00",
      "createdAt": "2025-01-15T10:00:00"
    }
  ]
}
```

**✅ ĐÃ CẬP NHẬT:** Response hiện tại **ĐÃ CÓ** thông tin Activity:
- `activityId`: Long - ID của sự kiện chứa nhiệm vụ này
- `activityName`: String - Tên sự kiện

### 4.1.1. Lấy Danh Sách Nhiệm Vụ Của Sinh Viên Khác (Admin/Manager)

**Endpoint:** `GET /api/assignments/student/{studentId}`

**Mô tả:** Admin/Manager có thể xem nhiệm vụ của bất kỳ sinh viên nào

**Response:** Tương tự như trên

### 4.2. Nộp Bài

**Endpoint:** `POST /api/submissions/task/{taskId}`

**Request:**
- `content`: String (tùy chọn)
- `files`: List<MultipartFile> (tùy chọn)

**Response:**
```json
{
  "status": true,
  "message": "Task submitted successfully",
  "data": {
    "id": 1,
    "taskId": 10,
    "studentId": 123,
    "content": "Nội dung bài nộp",
    "fileUrls": "/uploads/submissions/file1.pdf,/uploads/submissions/file2.pdf",
    "status": "SUBMITTED",
    "submittedAt": "2025-01-20T14:30:00"
  }
}
```

**Tự động cập nhật:**
- `TaskAssignment.status`: `PENDING` → `ASSIGNED`

### 4.3. Cập Nhật Bài Nộp

**Endpoint:** `PUT /api/submissions/{submissionId}`

**Request:**
- `content`: String (tùy chọn)
- `files`: List<MultipartFile> (tùy chọn)

**Lưu ý:** Chỉ có thể cập nhật khi `status = SUBMITTED` (chưa được chấm)

### 4.4. Xem Bài Nộp Của Mình

**Endpoint:** `GET /api/submissions/task/{taskId}/my`

**Response:** Tương tự như response của nộp bài

### 4.5. Xem Chi Tiết Bài Nộp

**Endpoint:** `GET /api/submissions/{submissionId}`

**Response:**
```json
{
  "status": true,
  "message": "Submission details retrieved successfully",
  "data": {
    "id": 1,
    "taskId": 10,
    "taskName": "Viết báo cáo",
    "studentId": 123,
    "studentName": "Nguyễn Văn A",
    "content": "Nội dung bài nộp",
    "fileUrls": "/uploads/submissions/file1.pdf",
    "score": 8.5,
    "isCompleted": true,
    "feedback": "Bài làm tốt",
    "graderId": 5,
    "graderName": "Manager A",
    "status": "GRADED",
    "submittedAt": "2025-01-20T14:30:00",
    "gradedAt": "2025-01-22T10:00:00"
  }
}
```

---

## 5. API CHO MANAGER/ADMIN

### 5.1. Chấm Điểm Bài Nộp

**Endpoint:** `PUT /api/submissions/{submissionId}/grade`

**Request:**
- `isCompleted`: boolean (true = đạt, false = không đạt)
- `feedback`: String (tùy chọn)

**Response:**
```json
{
  "status": true,
  "message": "Submission graded successfully",
  "data": {
    "id": 1,
    "status": "GRADED",
    "isCompleted": true,
    "score": 8.5,
    "feedback": "Bài làm tốt"
  }
}
```

**Tự động cập nhật:**
1. `TaskSubmission.status`: `SUBMITTED` → `GRADED`
2. `TaskAssignment.status`: `ASSIGNED` → `COMPLETED`
3. `ActivityParticipation` và `StudentScore` (nếu đủ điều kiện)

**Logic tính điểm:**
- Nếu `isCompleted = true`: Điểm = `activity.maxPoints` (nếu có)
- Nếu `isCompleted = false`: Điểm = `-activity.penaltyPointsIncomplete` (số âm)

### 5.2. Xem Tất Cả Bài Nộp Của Một Task

**Endpoint:** `GET /api/submissions/task/{taskId}`

**Response:**
```json
{
  "status": true,
  "message": "Task submissions retrieved successfully",
  "data": [
    {
      "id": 1,
      "taskId": 10,
      "studentId": 123,
      "studentName": "Nguyễn Văn A",
      "status": "GRADED",
      "isCompleted": true,
      "score": 8.5,
      "submittedAt": "2025-01-20T14:30:00"
    }
  ]
}
```

---

## 6. VẤN ĐỀ VÀ GIẢI PHÁP

### 6.1. ✅ ĐÃ CẬP NHẬT: Response Đã Có Thông Tin Activity

**Đã thực hiện:**
1. ✅ Cập nhật `TaskAssignmentResponse` để thêm:
   - `activityId`: Long
   - `activityName`: String
2. ✅ Cập nhật method `toAssignmentResponse()` trong `ActivityTaskServiceImpl`:
   ```java
   response.setActivityId(assignment.getTask().getActivity().getId());
   response.setActivityName(assignment.getTask().getActivity().getName());
   ```

### 6.2. ✅ ĐÃ CẬP NHẬT: API Lấy Nhiệm Vụ Của Sinh Viên

**Đã thực hiện:**
1. ✅ Thêm endpoint mới: `GET /api/assignments/my`
   - Lấy `studentId` từ authentication
   - Chỉ trả về nhiệm vụ của chính sinh viên đó
   - An toàn hơn vì không cần truyền `studentId` trong URL
2. ✅ Giữ nguyên endpoint cũ: `GET /api/assignments/student/{studentId}`
   - Dành cho Admin/Manager xem nhiệm vụ của sinh viên khác

---

## 7. TÓM TẮT API

### 7.1. API Cho Sinh Viên

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/assignments/my` | ✅ Lấy danh sách nhiệm vụ của chính mình (mới) |
| POST | `/api/submissions/task/{taskId}` | Nộp bài |
| PUT | `/api/submissions/{submissionId}` | Cập nhật bài nộp |
| GET | `/api/submissions/task/{taskId}/my` | Xem bài nộp của mình |
| GET | `/api/submissions/{submissionId}` | Xem chi tiết bài nộp |
| DELETE | `/api/submissions/{submissionId}` | Xóa bài nộp |

### 7.2. API Cho Manager/Admin

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/tasks/assign` | Phân công nhiệm vụ |
| PUT | `/api/submissions/{submissionId}/grade` | Chấm điểm bài nộp |
| GET | `/api/submissions/task/{taskId}` | Xem tất cả bài nộp của task |
| GET | `/api/tasks/{taskId}/assignments` | Xem danh sách phân công của task |

---

## 8. ✅ CẬP NHẬT ĐÃ THỰC HIỆN

### 8.1. ✅ Cập Nhật TaskAssignmentResponse

**File:** `src/main/java/vn/campuslife/model/TaskAssignmentResponse.java`

**Đã thêm:**
```java
private Long activityId; // ID của sự kiện chứa nhiệm vụ này
private String activityName; // Tên sự kiện
```

### 8.2. ✅ Cập Nhật toAssignmentResponse()

**File:** `src/main/java/vn/campuslife/service/impl/ActivityTaskServiceImpl.java`

**Đã cập nhật:**
```java
// Thêm thông tin Activity để sinh viên biết nhiệm vụ thuộc sự kiện nào
response.setActivityId(assignment.getTask().getActivity().getId());
response.setActivityName(assignment.getTask().getActivity().getName());
```

### 8.3. ✅ Thêm Endpoint Mới Cho Sinh Viên

**File:** `src/main/java/vn/campuslife/controller/TaskAssignmentController.java`

**Đã thêm:**
- `GET /api/assignments/my` - Lấy nhiệm vụ của chính mình (tự động lấy studentId từ authentication)
- Helper method `getStudentIdFromAuth()` để lấy studentId từ authentication

---

## 9. FLOW HOÀN CHỈNH

### 9.1. Flow Phân Công và Nộp Bài

```
1. Manager phân công nhiệm vụ
   POST /api/tasks/assign
   → TaskAssignment: status = PENDING
   
2. Sinh viên xem nhiệm vụ của mình
   GET /api/assignments/my
   → Response có: taskId, taskName, activityId, activityName, status
   → Sinh viên biết nhiệm vụ thuộc sự kiện nào
   
3. Sinh viên nộp bài
   POST /api/submissions/task/{taskId}
   → TaskSubmission: status = SUBMITTED
   → TaskAssignment: status = PENDING → ASSIGNED
   
4. Manager chấm điểm
   PUT /api/submissions/{submissionId}/grade
   → TaskSubmission: status = SUBMITTED → GRADED
   → TaskAssignment: status = ASSIGNED → COMPLETED
   → ActivityParticipation và StudentScore được cập nhật (nếu đủ điều kiện)
```

---

## 10. KIỂM TRA SECURITY

### 10.1. Endpoint Hiện Tại

Cần kiểm tra `SecurityConfig` để đảm bảo:
- Sinh viên chỉ có thể xem/nộp bài của chính mình
- Manager/Admin có thể chấm điểm và xem tất cả bài nộp

**File:** `src/main/java/vn/campuslife/config/SecurityConfig.java`

Cần đảm bảo các rule:
- `/api/assignments/**` - Cho phép STUDENT, ADMIN, MANAGER
- `/api/submissions/**` - Cho phép STUDENT (xem của mình), ADMIN, MANAGER (xem tất cả)

