# Sequence Diagram - Chức năng Quản lý Nhiệm vụ

## Mô tả
Sequence diagram mô tả luồng xử lý quản lý nhiệm vụ (ActivityTask) và phân công nhiệm vụ (TaskAssignment) trong hệ thống CampusLife.

## Sequence Diagrams

### Phần 1: Quản lý Nhiệm vụ (ActivityTask)

#### 1.1. Tạo nhiệm vụ (Create Task)

```mermaid
sequenceDiagram
    participant Manager as Manager/Admin
    participant Client as Client/Frontend
    participant TaskController as ActivityTaskController
    participant TaskService as ActivityTaskServiceImpl
    participant ActivityRepository as ActivityRepository
    participant TaskRepository as ActivityTaskRepository
    participant Database as Database

    Manager->>Client: Nhập thông tin nhiệm vụ<br/>(activityId, name, description, deadline)
    Client->>TaskController: POST /api/tasks<br/>(CreateActivityTaskRequest)
    
    TaskController->>TaskService: createTask(request)
    
    Note over TaskService: Kiểm tra hoạt động tồn tại
    TaskService->>ActivityRepository: findByIdAndIsDeletedFalse(activityId)
    ActivityRepository->>Database: SELECT * FROM activities<br/>WHERE id = ? AND is_deleted = false
    Database-->>ActivityRepository: Activity
    ActivityRepository-->>TaskService: Activity
    
    Note over TaskService: Validate deadline:<br/>Deadline phải sau ngày kết thúc hoạt động
    
    Note over TaskService: Tạo nhiệm vụ mới
    TaskService->>TaskService: create ActivityTask<br/>(activity, name, description, deadline)
    TaskService->>TaskRepository: save(task)
    TaskRepository->>Database: INSERT INTO activity_tasks<br/>(activity_id, name, description, deadline)
    Database-->>TaskRepository: ActivityTask saved
    TaskRepository-->>TaskService: ActivityTask
    
    TaskService-->>TaskController: Response(success, ActivityTaskResponse)
    TaskController-->>Client: ResponseEntity.ok()
    Client-->>Manager: Hiển thị thông báo thành công
```

#### 1.2. Cập nhật nhiệm vụ (Update Task)

```mermaid
sequenceDiagram
    participant Manager as Manager/Admin
    participant Client as Client/Frontend
    participant TaskController as ActivityTaskController
    participant TaskService as ActivityTaskServiceImpl
    participant ActivityRepository as ActivityRepository
    participant TaskRepository as ActivityTaskRepository
    participant Database as Database

    Manager->>Client: Chọn nhiệm vụ và cập nhật thông tin
    Client->>TaskController: PUT /api/tasks/{taskId}<br/>(CreateActivityTaskRequest)
    
    TaskController->>TaskService: updateTask(taskId, request)
    
    TaskService->>TaskRepository: findByIdAndActivityIsDeletedFalse(taskId)
    TaskRepository->>Database: SELECT * FROM activity_tasks<br/>WHERE id = ? AND activity.is_deleted = false
    Database-->>TaskRepository: ActivityTask
    TaskRepository-->>TaskService: ActivityTask
    
    Note over TaskService: Validate deadline và activity
    
    Note over TaskService: Cập nhật thông tin
    TaskService->>TaskService: task.setName(name)<br/>task.setDescription(description)<br/>task.setDeadline(deadline)
    TaskService->>TaskRepository: save(task)
    TaskRepository->>Database: UPDATE activity_tasks SET<br/>name=?, description=?, deadline=? WHERE id=?
    Database-->>TaskRepository: Updated
    TaskRepository-->>TaskService: ActivityTask updated
    
    TaskService-->>TaskController: Response(success, ActivityTaskResponse)
    TaskController-->>Client: ResponseEntity.ok()
    Client-->>Manager: Hiển thị thông báo thành công
```

#### 1.3. Xóa nhiệm vụ (Delete Task)

```mermaid
sequenceDiagram
    participant Manager as Manager/Admin
    participant Client as Client/Frontend
    participant TaskController as ActivityTaskController
    participant TaskService as ActivityTaskServiceImpl
    participant TaskRepository as ActivityTaskRepository
    participant AssignmentRepository as TaskAssignmentRepository
    participant Database as Database

    Manager->>Client: Chọn nhiệm vụ và click xóa
    Client->>TaskController: DELETE /api/tasks/{taskId}
    
    TaskController->>TaskService: deleteTask(taskId)
    
    TaskService->>TaskRepository: findByIdAndActivityIsDeletedFalse(taskId)
    TaskRepository->>Database: SELECT * FROM activity_tasks WHERE id = ?
    Database-->>TaskRepository: ActivityTask
    TaskRepository-->>TaskService: ActivityTask
    
    Note over TaskService: Kiểm tra có phân công chưa
    TaskService->>AssignmentRepository: findByTaskId(taskId)
    AssignmentRepository->>Database: SELECT * FROM task_assignments<br/>WHERE task_id = ?
    Database-->>AssignmentRepository: List<TaskAssignment>
    AssignmentRepository-->>TaskService: List<TaskAssignment>
    
    Note over TaskService: Chỉ xóa được nếu chưa có phân công
    
    TaskService->>TaskRepository: deleteById(taskId)
    TaskRepository->>Database: DELETE FROM activity_tasks WHERE id = ?
    Database-->>TaskRepository: Deleted
    TaskRepository-->>TaskService: Deleted
    
    TaskService-->>TaskController: Response(success)
    TaskController-->>Client: ResponseEntity.ok()
    Client-->>Manager: Hiển thị thông báo thành công
```

#### 1.4. Xem danh sách và chi tiết nhiệm vụ (Get Tasks / Get Task By ID)

```mermaid
sequenceDiagram
    participant Manager as Manager/Admin
    participant Client as Client/Frontend
    participant TaskController as ActivityTaskController
    participant TaskService as ActivityTaskServiceImpl
    participant ActivityRepository as ActivityRepository
    participant TaskRepository as ActivityTaskRepository
    participant AssignmentRepository as TaskAssignmentRepository
    participant Database as Database

    Note over Manager,Database: Xem danh sách nhiệm vụ theo hoạt động
    
    Manager->>Client: Chọn hoạt động để xem nhiệm vụ
    Client->>TaskController: GET /api/tasks/activity/{activityId}
    
    TaskController->>TaskService: getTasksByActivity(activityId)
    
    Note over TaskService: Kiểm tra hoạt động tồn tại
    TaskService->>ActivityRepository: findByIdAndIsDeletedFalse(activityId)
    ActivityRepository->>Database: SELECT * FROM activities WHERE id = ?
    Database-->>ActivityRepository: Activity
    ActivityRepository-->>TaskService: Activity
    
    TaskService->>TaskRepository: findByActivityIdAndActivityIsDeletedFalse(activityId)
    TaskRepository->>Database: SELECT * FROM activity_tasks<br/>WHERE activity_id = ? AND activity.is_deleted = false
    Database-->>TaskRepository: List<ActivityTask>
    TaskRepository-->>TaskService: List<ActivityTask>
    
    Note over TaskService: Chuyển đổi sang ActivityTaskResponse
    TaskService->>TaskService: map to ActivityTaskResponse
    
    TaskService-->>TaskController: Response(success, List<ActivityTaskResponse>)
    TaskController-->>Client: ResponseEntity.ok()
    Client-->>Manager: Hiển thị danh sách nhiệm vụ
    
    Note over Manager,Database: Xem chi tiết nhiệm vụ
    
    Manager->>Client: Click xem chi tiết nhiệm vụ
    Client->>TaskController: GET /api/tasks/{taskId}
    
    TaskController->>TaskService: getTaskById(taskId)
    
    TaskService->>TaskRepository: findByIdAndActivityIsDeletedFalse(taskId)
    TaskRepository->>Database: SELECT * FROM activity_tasks WHERE id = ?
    Database-->>TaskRepository: ActivityTask
    TaskRepository-->>TaskService: ActivityTask
    
    Note over TaskService: Lấy thống kê phân công
    TaskService->>AssignmentRepository: findByTaskId(taskId)
    AssignmentRepository->>Database: SELECT * FROM task_assignments<br/>WHERE task_id = ?
    Database-->>AssignmentRepository: List<TaskAssignment>
    AssignmentRepository-->>TaskService: List<TaskAssignment>
    
    Note over TaskService: Tính toán thống kê:<br/>- Tổng số phân công<br/>- Số đã hoàn thành<br/>- Số đang chờ
    TaskService->>TaskService: toTaskResponse(task)
    
    TaskService-->>TaskController: Response(success, ActivityTaskResponse)
    TaskController-->>Client: ResponseEntity.ok()
    Client-->>Manager: Hiển thị thông tin chi tiết nhiệm vụ
```

### Phần 2: Quản lý Phân công Nhiệm vụ (TaskAssignment)

#### 2.1. Phân công nhiệm vụ (Assign Task)

```mermaid
sequenceDiagram
    participant Manager as Manager/Admin
    participant Client as Client/Frontend
    participant TaskController as ActivityTaskController
    participant TaskService as ActivityTaskServiceImpl
    participant TaskRepository as ActivityTaskRepository
    participant StudentRepository as StudentRepository
    participant AssignmentRepository as TaskAssignmentRepository
    participant Database as Database

    Manager->>Client: Chọn nhiệm vụ và sinh viên, click phân công
    Client->>TaskController: POST /api/tasks/assign<br/>(TaskAssignmentRequest: taskId, studentIds)
    
    TaskController->>TaskService: assignTask(request)
    
    Note over TaskService: Kiểm tra nhiệm vụ tồn tại
    TaskService->>TaskRepository: findByIdAndActivityIsDeletedFalse(taskId)
    TaskRepository->>Database: SELECT * FROM activity_tasks<br/>WHERE id = ? AND activity.is_deleted = false
    Database-->>TaskRepository: ActivityTask
    TaskRepository-->>TaskService: ActivityTask
    
    Note over TaskService: Kiểm tra sinh viên tồn tại
    TaskService->>StudentRepository: findAllById(studentIds)
    StudentRepository->>Database: SELECT * FROM students<br/>WHERE id IN (?)
    Database-->>StudentRepository: List<Student>
    StudentRepository-->>TaskService: List<Student>
    
    Note over TaskService: Tạo phân công:<br/>- Lọc sinh viên chưa được phân công<br/>- Set status = PENDING
    TaskService->>TaskService: filter students chưa có assignment<br/>create TaskAssignment (task, student, PENDING)
    TaskService->>AssignmentRepository: saveAll(assignments)
    AssignmentRepository->>Database: INSERT INTO task_assignments<br/>(task_id, student_id, status)
    Database-->>AssignmentRepository: List<TaskAssignment> saved
    AssignmentRepository-->>TaskService: List<TaskAssignment>
    
    TaskService-->>TaskController: Response(success, List<TaskAssignmentResponse>)
    TaskController-->>Client: ResponseEntity.ok()
    Client-->>Manager: Hiển thị thông báo thành công
```

#### 2.2. Cập nhật trạng thái nhiệm vụ (Update Task Status)

```mermaid
sequenceDiagram
    participant Student as Student
    participant Client as Client/Frontend
    participant AssignmentController as TaskAssignmentController
    participant TaskService as ActivityTaskServiceImpl
    participant AssignmentRepository as TaskAssignmentRepository
    participant Database as Database

    Student->>Client: Cập nhật trạng thái nhiệm vụ<br/>(PENDING/IN_PROGRESS/COMPLETED/OVERDUE)
    Client->>AssignmentController: PUT /api/assignments/{assignmentId}/status<br/>(?status=COMPLETED)
    
    AssignmentController->>TaskService: updateTaskStatus(assignmentId, status)
    
    TaskService->>AssignmentRepository: findById(assignmentId)
    AssignmentRepository->>Database: SELECT * FROM task_assignments WHERE id = ?
    Database-->>AssignmentRepository: TaskAssignment
    AssignmentRepository-->>TaskService: TaskAssignment
    
    Note over TaskService: Validate và cập nhật status
    TaskService->>TaskService: assignment.setStatus(TaskStatus.valueOf(status))
    TaskService->>AssignmentRepository: save(assignment)
    AssignmentRepository->>Database: UPDATE task_assignments<br/>SET status = ? WHERE id = ?
    Database-->>AssignmentRepository: Updated
    AssignmentRepository-->>TaskService: TaskAssignment updated
    
    TaskService-->>AssignmentController: Response(success, TaskAssignmentResponse)
    AssignmentController-->>Client: ResponseEntity.ok()
    Client-->>Student: Hiển thị thông báo thành công
```

#### 2.3. Hủy phân công nhiệm vụ (Remove Task Assignment)

```mermaid
sequenceDiagram
    participant Manager as Manager/Admin
    participant Client as Client/Frontend
    participant AssignmentController as TaskAssignmentController
    participant TaskService as ActivityTaskServiceImpl
    participant AssignmentRepository as TaskAssignmentRepository
    participant Database as Database

    Manager->>Client: Chọn phân công và click hủy
    Client->>AssignmentController: DELETE /api/assignments/{assignmentId}
    
    AssignmentController->>TaskService: removeTaskAssignment(assignmentId)
    
    TaskService->>AssignmentRepository: findById(assignmentId)
    AssignmentRepository->>Database: SELECT * FROM task_assignments WHERE id = ?
    Database-->>AssignmentRepository: TaskAssignment
    AssignmentRepository-->>TaskService: TaskAssignment
    
    Note over TaskService: Xóa phân công
    TaskService->>AssignmentRepository: deleteById(assignmentId)
    AssignmentRepository->>Database: DELETE FROM task_assignments WHERE id = ?
    Database-->>AssignmentRepository: Deleted
    AssignmentRepository-->>TaskService: Deleted
    
    TaskService-->>AssignmentController: Response(success)
    AssignmentController-->>Client: ResponseEntity.ok()
    Client-->>Manager: Hiển thị thông báo thành công
```

#### 2.4. Xem danh sách phân công (Get Task Assignments / Get Student Tasks)

```mermaid
sequenceDiagram
    participant User as Manager/Student
    participant Client as Client/Frontend
    participant TaskController as ActivityTaskController<br/>hoặc TaskAssignmentController
    participant TaskService as ActivityTaskServiceImpl
    participant AssignmentRepository as TaskAssignmentRepository
    participant Database as Database

    Note over User,Database: Xem danh sách phân công theo nhiệm vụ
    
    User->>Client: Chọn nhiệm vụ để xem phân công
    Client->>TaskController: GET /api/tasks/{taskId}/assignments
    
    TaskController->>TaskService: getTaskAssignments(taskId)
    
    TaskService->>AssignmentRepository: findByTaskId(taskId)
    AssignmentRepository->>Database: SELECT * FROM task_assignments<br/>WHERE task_id = ?
    Database-->>AssignmentRepository: List<TaskAssignment>
    AssignmentRepository-->>TaskService: List<TaskAssignment>
    
    Note over TaskService: Chuyển đổi sang TaskAssignmentResponse
    TaskService->>TaskService: map to TaskAssignmentResponse
    
    TaskService-->>TaskController: Response(success, List<TaskAssignmentResponse>)
    TaskController-->>Client: ResponseEntity.ok()
    Client-->>User: Hiển thị danh sách phân công
    
    Note over User,Database: Xem danh sách nhiệm vụ của sinh viên
    
    User->>Client: Xem nhiệm vụ của tôi
    Client->>AssignmentController: GET /api/assignments/student/{studentId}
    
    AssignmentController->>TaskService: getStudentTasks(studentId)
    
    TaskService->>AssignmentRepository: findByStudentIdAndTaskActivityIsDeletedFalse(studentId)
    AssignmentRepository->>Database: SELECT * FROM task_assignments<br/>WHERE student_id = ? AND task.activity.is_deleted = false
    Database-->>AssignmentRepository: List<TaskAssignment>
    AssignmentRepository-->>TaskService: List<TaskAssignment>
    
    Note over TaskService: Chuyển đổi sang TaskAssignmentResponse
    TaskService->>TaskService: map to TaskAssignmentResponse
    
    TaskService-->>AssignmentController: Response(success, List<TaskAssignmentResponse>)
    AssignmentController-->>Client: ResponseEntity.ok()
    Client-->>User: Hiển thị danh sách nhiệm vụ
```

## Các thành phần tham gia

1. **Manager/Admin/Student**: Người dùng thực hiện quản lý nhiệm vụ
2. **Client/Frontend**: Giao diện người dùng
3. **ActivityTaskController**: Controller nhận request quản lý nhiệm vụ
4. **TaskAssignmentController**: Controller nhận request quản lý phân công
5. **ActivityTaskServiceImpl**: Service xử lý logic quản lý nhiệm vụ và phân công
6. **ActivityRepository**: Repository truy cập database cho hoạt động
7. **ActivityTaskRepository**: Repository truy cập database cho nhiệm vụ
8. **TaskAssignmentRepository**: Repository truy cập database cho phân công
9. **StudentRepository**: Repository truy cập database cho sinh viên
10. **Database**: Cơ sở dữ liệu

## Các chức năng

### Phần 1: Quản lý Nhiệm vụ (ActivityTask)

#### 1.1. Tạo nhiệm vụ
1. Manager nhập thông tin nhiệm vụ (activityId, name, description, deadline)
2. Kiểm tra hoạt động tồn tại
3. Validate deadline phải sau ngày kết thúc hoạt động
4. Tạo ActivityTask mới
5. Lưu vào database
6. Trả về thông tin nhiệm vụ đã tạo

#### 1.2. Cập nhật nhiệm vụ
1. Manager chọn nhiệm vụ và cập nhật thông tin
2. Tìm nhiệm vụ theo ID
3. Validate deadline và activity
4. Cập nhật thông tin (name, description, deadline)
5. Lưu vào database
6. Trả về thông tin đã cập nhật

#### 1.3. Xóa nhiệm vụ
1. Manager chọn nhiệm vụ và click xóa
2. Tìm nhiệm vụ theo ID
3. Kiểm tra chưa có phân công
4. Xóa nhiệm vụ khỏi database
5. Trả về kết quả thành công

#### 1.4. Xem danh sách và chi tiết nhiệm vụ
- **Xem danh sách theo hoạt động**: Lấy tất cả nhiệm vụ của hoạt động, chuyển đổi sang Response
- **Xem chi tiết**: Tìm nhiệm vụ theo ID, lấy thống kê phân công (tổng số, đã hoàn thành, đang chờ)

### Phần 2: Quản lý Phân công Nhiệm vụ (TaskAssignment)

#### 2.1. Phân công nhiệm vụ
1. Manager chọn nhiệm vụ và sinh viên
2. Kiểm tra nhiệm vụ và sinh viên tồn tại
3. Lọc sinh viên chưa được phân công
4. Tạo TaskAssignment với status = PENDING
5. Lưu vào database
6. Trả về danh sách phân công đã tạo

#### 2.2. Cập nhật trạng thái nhiệm vụ
1. Student/Manager cập nhật trạng thái (PENDING/IN_PROGRESS/COMPLETED/OVERDUE)
2. Tìm phân công theo ID
3. Validate và cập nhật status
4. Lưu vào database
5. Trả về kết quả thành công

#### 2.3. Hủy phân công nhiệm vụ
1. Manager chọn phân công và click hủy
2. Tìm phân công theo ID
3. Xóa phân công khỏi database
4. Trả về kết quả thành công

#### 2.4. Xem danh sách phân công
- **Xem theo nhiệm vụ**: Lấy tất cả phân công của một nhiệm vụ
- **Xem theo sinh viên**: Lấy tất cả nhiệm vụ được phân công cho một sinh viên

## Đặc điểm

- **Phân quyền**: 
  - Manager/Admin có quyền tạo, sửa, xóa nhiệm vụ và phân công
  - Student có quyền xem nhiệm vụ của mình và cập nhật trạng thái
- **Validation**: 
  - Deadline phải sau ngày kết thúc hoạt động
  - Chỉ xóa được nhiệm vụ chưa có phân công
  - Không phân công trùng lặp cho cùng một sinh viên
- **Trạng thái nhiệm vụ**: PENDING → IN_PROGRESS → COMPLETED hoặc OVERDUE
- **Quan hệ**: Nhiệm vụ thuộc về một hoạt động, có nhiều phân công cho sinh viên

