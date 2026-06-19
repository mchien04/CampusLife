# Sequence Diagram - Chức năng Quản lý Lớp học

## Mô tả
Sequence diagram mô tả luồng xử lý quản lý lớp học trong hệ thống CampusLife (dành cho ADMIN và MANAGER).

## Sequence Diagrams

### 1. Tạo lớp học (Create Class)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ClassController as StudentClassController
    participant ClassService as StudentClassServiceImpl
    participant ClassRepository as StudentClassRepository
    participant DeptRepository as DepartmentRepository
    participant Database as Database

    Admin->>Client: Nhập thông tin lớp học<br/>(className, description, departmentId)
    Client->>ClassController: POST /api/classes<br/>(className, description, departmentId)
    
    ClassController->>ClassService: createClass(className, description, departmentId)
    
    Note over ClassService: Kiểm tra department tồn tại
    ClassService->>DeptRepository: findById(departmentId)
    DeptRepository->>Database: SELECT * FROM departments WHERE id = ?
    Database-->>DeptRepository: Department hoặc null
    DeptRepository-->>ClassService: Optional<Department>
    
    Note over ClassService: Kiểm tra tên lớp chưa tồn tại
    ClassService->>ClassRepository: findByClassNameAndIsDeletedFalse(className)
    ClassRepository->>Database: SELECT * FROM student_classes<br/>WHERE class_name = ? AND is_deleted = false
    Database-->>ClassRepository: StudentClass hoặc null
    ClassRepository-->>ClassService: Optional<StudentClass>
    
    Note over ClassService: Tạo lớp học mới
    ClassService->>ClassService: create StudentClass<br/>(className, description, department)
    ClassService->>ClassRepository: save(studentClass)
    ClassRepository->>Database: INSERT INTO student_classes<br/>(class_name, description, department_id)
    Database-->>ClassRepository: StudentClass saved
    ClassRepository-->>ClassService: StudentClass
    
    ClassService-->>ClassController: Response(success, StudentClass)
    ClassController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
```

### 2. Cập nhật lớp học (Update Class)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ClassController as StudentClassController
    participant ClassService as StudentClassServiceImpl
    participant ClassRepository as StudentClassRepository
    participant Database as Database

    Admin->>Client: Chọn lớp và cập nhật thông tin
    Client->>ClassController: PUT /api/classes/{classId}<br/>(className, description)
    
    ClassController->>ClassService: updateClass(classId, className, description)
    
    ClassService->>ClassRepository: findById(classId)
    ClassRepository->>Database: SELECT * FROM student_classes WHERE id = ?
    Database-->>ClassRepository: StudentClass
    ClassRepository-->>ClassService: StudentClass
    
    Note over ClassService: Kiểm tra tên lớp mới chưa trùng<br/>(nếu thay đổi tên)
    alt Tên lớp thay đổi
        ClassService->>ClassRepository: findByClassNameAndIsDeletedFalse(className)
        ClassRepository->>Database: SELECT * FROM student_classes<br/>WHERE class_name = ? AND is_deleted = false
        Database-->>ClassRepository: StudentClass hoặc null
        ClassRepository-->>ClassService: Optional<StudentClass>
    end
    
    Note over ClassService: Cập nhật thông tin
    ClassService->>ClassService: studentClass.setClassName(className)<br/>studentClass.setDescription(description)
    ClassService->>ClassRepository: save(studentClass)
    ClassRepository->>Database: UPDATE student_classes SET<br/>class_name=?, description=? WHERE id=?
    Database-->>ClassRepository: Updated
    ClassRepository-->>ClassService: StudentClass updated
    
    ClassService-->>ClassController: Response(success, StudentClass)
    ClassController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
```

### 3. Xóa lớp học (Delete Class)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ClassController as StudentClassController
    participant ClassService as StudentClassServiceImpl
    participant ClassRepository as StudentClassRepository
    participant Database as Database

    Admin->>Client: Chọn lớp và click xóa
    Client->>ClassController: DELETE /api/classes/{classId}
    
    ClassController->>ClassService: deleteClass(classId)
    
    ClassService->>ClassRepository: findById(classId)
    ClassRepository->>Database: SELECT * FROM student_classes WHERE id = ?
    Database-->>ClassRepository: StudentClass
    ClassRepository-->>ClassService: StudentClass
    
    Note over ClassService: Soft delete:<br/>Set is_deleted = true
    ClassService->>ClassService: studentClass.setDeleted(true)
    ClassService->>ClassRepository: save(studentClass)
    ClassRepository->>Database: UPDATE student_classes<br/>SET is_deleted = true WHERE id = ?
    Database-->>ClassRepository: Updated
    ClassRepository-->>ClassService: StudentClass updated
    
    ClassService-->>ClassController: Response(success)
    ClassController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
```

### 4. Xem danh sách lớp học (Get All Classes)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ClassController as StudentClassController
    participant ClassService as StudentClassServiceImpl
    participant ClassRepository as StudentClassRepository
    participant Database as Database

    Admin->>Client: Truy cập trang quản lý lớp học
    Client->>ClassController: GET /api/classes
    
    ClassController->>ClassService: getAllClasses()
    
    ClassService->>ClassRepository: findByIsDeletedFalse()
    ClassRepository->>Database: SELECT * FROM student_classes<br/>WHERE is_deleted = false
    Database-->>ClassRepository: List<StudentClass>
    ClassRepository-->>ClassService: List<StudentClass>
    
    ClassService-->>ClassController: Response(success, List<StudentClass>)
    ClassController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị danh sách lớp học
```

### 5. Xem danh sách lớp học theo khoa (Get Classes By Department)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ClassController as StudentClassController
    participant ClassService as StudentClassServiceImpl
    participant ClassRepository as StudentClassRepository
    participant Database as Database

    Admin->>Client: Chọn khoa để xem lớp học
    Client->>ClassController: GET /api/classes/department/{departmentId}
    
    ClassController->>ClassService: getClassesByDepartment(departmentId)
    
    ClassService->>ClassRepository: findByDepartmentIdAndIsDeletedFalse(departmentId)
    ClassRepository->>Database: SELECT * FROM student_classes<br/>WHERE department_id = ? AND is_deleted = false
    Database-->>ClassRepository: List<StudentClass>
    ClassRepository-->>ClassService: List<StudentClass>
    
    ClassService-->>ClassController: Response(success, List<StudentClass>)
    ClassController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị danh sách lớp học theo khoa
```

### 6. Xem chi tiết lớp học (Get Class By ID)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ClassController as StudentClassController
    participant ClassService as StudentClassServiceImpl
    participant ClassRepository as StudentClassRepository
    participant Database as Database

    Admin->>Client: Click xem chi tiết lớp
    Client->>ClassController: GET /api/classes/{classId}
    
    ClassController->>ClassService: getClassById(classId)
    
    ClassService->>ClassRepository: findById(classId)
    ClassRepository->>Database: SELECT * FROM student_classes WHERE id = ?
    Database-->>ClassRepository: StudentClass
    ClassRepository-->>ClassService: StudentClass
    
    ClassService-->>ClassController: Response(success, StudentClass)
    ClassController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông tin chi tiết lớp
```

### 7. Xem danh sách sinh viên trong lớp (Get Students In Class)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ClassController as StudentClassController
    participant ClassService as StudentClassServiceImpl
    participant ClassRepository as StudentClassRepository
    participant StudentRepository as StudentRepository
    participant Database as Database

    Admin->>Client: Click xem danh sách sinh viên
    Client->>ClassController: GET /api/classes/{classId}/students
    
    ClassController->>ClassService: getStudentsInClass(classId)
    
    Note over ClassService: Kiểm tra lớp tồn tại
    ClassService->>ClassRepository: findByIdAndIsDeletedFalse(classId)
    ClassRepository->>Database: SELECT * FROM student_classes<br/>WHERE id = ? AND is_deleted = false
    Database-->>ClassRepository: StudentClass
    ClassRepository-->>ClassService: StudentClass
    
    Note over ClassService: Lấy danh sách sinh viên
    ClassService->>StudentRepository: findByClassId(classId)
    StudentRepository->>Database: SELECT * FROM students<br/>WHERE class_id = ?
    Database-->>StudentRepository: List<Student>
    StudentRepository-->>ClassService: List<Student>
    
    Note over ClassService: Chuyển đổi sang StudentResponse
    ClassService->>ClassService: map to StudentResponse
    
    ClassService-->>ClassController: Response(success, List<StudentResponse>)
    ClassController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị danh sách sinh viên
```

### 8. Thêm sinh viên vào lớp (Add Student To Class)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ClassController as StudentClassController
    participant ClassService as StudentClassServiceImpl
    participant ClassRepository as StudentClassRepository
    participant StudentRepository as StudentRepository
    participant Database as Database

    Admin->>Client: Chọn sinh viên và lớp, click thêm
    Client->>ClassController: POST /api/classes/{classId}/students/{studentId}
    
    ClassController->>ClassService: addStudentToClass(classId, studentId)
    
    Note over ClassService: Kiểm tra lớp tồn tại
    ClassService->>ClassRepository: findById(classId)
    ClassRepository->>Database: SELECT * FROM student_classes WHERE id = ?
    Database-->>ClassRepository: StudentClass
    ClassRepository-->>ClassService: StudentClass
    
    Note over ClassService: Kiểm tra sinh viên tồn tại
    ClassService->>StudentRepository: findByIdAndIsDeletedFalse(studentId)
    StudentRepository->>Database: SELECT * FROM students<br/>WHERE id = ? AND is_deleted = false
    Database-->>StudentRepository: Student
    StudentRepository-->>ClassService: Student
    
    Note over ClassService: Gán lớp cho sinh viên
    ClassService->>ClassService: student.setStudentClass(studentClass)
    ClassService->>StudentRepository: save(student)
    StudentRepository->>Database: UPDATE students SET class_id = ?<br/>WHERE id = ?
    Database-->>StudentRepository: Updated
    StudentRepository-->>ClassService: Student updated
    
    ClassService-->>ClassController: Response(success)
    ClassController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
```

### 9. Xóa sinh viên khỏi lớp (Remove Student From Class)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ClassController as StudentClassController
    participant ClassService as StudentClassServiceImpl
    participant StudentRepository as StudentRepository
    participant Database as Database

    Admin->>Client: Chọn sinh viên và click xóa khỏi lớp
    Client->>ClassController: DELETE /api/classes/{classId}/students/{studentId}
    
    ClassController->>ClassService: removeStudentFromClass(classId, studentId)
    
    Note over ClassService: Kiểm tra sinh viên tồn tại
    ClassService->>StudentRepository: findByIdAndIsDeletedFalse(studentId)
    StudentRepository->>Database: SELECT * FROM students<br/>WHERE id = ? AND is_deleted = false
    Database-->>StudentRepository: Student
    StudentRepository-->>ClassService: Student
    
    Note over ClassService: Kiểm tra sinh viên thuộc lớp này
    ClassService->>ClassService: check student.getStudentClass().getId() == classId
    
    Note over ClassService: Xóa lớp khỏi sinh viên
    ClassService->>ClassService: student.setStudentClass(null)
    ClassService->>StudentRepository: save(student)
    StudentRepository->>Database: UPDATE students SET class_id = NULL<br/>WHERE id = ?
    Database-->>StudentRepository: Updated
    StudentRepository-->>ClassService: Student updated
    
    ClassService-->>ClassController: Response(success)
    ClassController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
```

## Các thành phần tham gia

1. **Admin/Manager**: Người quản trị thực hiện quản lý lớp học
2. **Client/Frontend**: Giao diện người dùng
3. **StudentClassController**: Controller nhận request quản lý lớp học
4. **StudentClassServiceImpl**: Service xử lý logic quản lý lớp học
5. **StudentClassRepository**: Repository truy cập database cho lớp học
6. **DepartmentRepository**: Repository truy cập database cho khoa
7. **StudentRepository**: Repository truy cập database cho sinh viên
8. **Database**: Cơ sở dữ liệu

## Các chức năng

### 1. Tạo lớp học
1. Admin nhập thông tin lớp học (className, description, departmentId)
2. Kiểm tra department tồn tại
3. Kiểm tra tên lớp chưa tồn tại
4. Tạo lớp học mới và lưu vào database
5. Trả về thông tin lớp đã tạo

### 2. Cập nhật lớp học
1. Admin chọn lớp và cập nhật thông tin
2. Tìm lớp theo ID
3. Nếu thay đổi tên: Kiểm tra tên mới chưa trùng
4. Cập nhật thông tin và lưu vào database
5. Trả về thông tin lớp đã cập nhật

### 3. Xóa lớp học
1. Admin chọn lớp và click xóa
2. Tìm lớp theo ID
3. Thực hiện soft delete (set is_deleted = true)
4. Lưu vào database
5. Trả về kết quả thành công

### 4. Xem danh sách lớp học
1. Admin truy cập trang quản lý lớp học
2. Lấy tất cả lớp học chưa bị xóa từ database
3. Trả về danh sách lớp học

### 5. Xem danh sách lớp học theo khoa
1. Admin chọn khoa để xem lớp học
2. Lấy danh sách lớp học theo departmentId và chưa bị xóa
3. Trả về danh sách lớp học

### 6. Xem chi tiết lớp học
1. Admin click xem chi tiết lớp
2. Tìm lớp theo ID
3. Trả về thông tin chi tiết lớp

### 7. Xem danh sách sinh viên trong lớp
1. Admin click xem danh sách sinh viên
2. Kiểm tra lớp tồn tại
3. Lấy danh sách sinh viên trong lớp
4. Chuyển đổi sang StudentResponse
5. Trả về danh sách sinh viên

### 8. Thêm sinh viên vào lớp
1. Admin chọn sinh viên và lớp, click thêm
2. Kiểm tra lớp và sinh viên tồn tại
3. Gán lớp cho sinh viên (set studentClass)
4. Lưu vào database
5. Trả về kết quả thành công

### 9. Xóa sinh viên khỏi lớp
1. Admin chọn sinh viên và click xóa khỏi lớp
2. Kiểm tra sinh viên tồn tại và thuộc lớp này
3. Xóa lớp khỏi sinh viên (set studentClass = null)
4. Lưu vào database
5. Trả về kết quả thành công

## Đặc điểm

- **Chỉ ADMIN và MANAGER có quyền**: Tất cả endpoint yêu cầu role ADMIN hoặc MANAGER
- **Soft Delete**: Xóa lớp học bằng cách đánh dấu is_deleted = true, không xóa thật
- **Validation**: Kiểm tra tên lớp không trùng lặp, department tồn tại
- **Quan hệ**: Lớp học thuộc về một khoa (Department), có nhiều sinh viên (Student)

