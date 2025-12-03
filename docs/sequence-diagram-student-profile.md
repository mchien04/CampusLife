# Sequence Diagram - Chức năng Thông tin Cá nhân

## Mô tả
Sequence diagram mô tả luồng xử lý quản lý thông tin cá nhân (Student Profile) trong hệ thống CampusLife. Bao gồm các chức năng xem và cập nhật thông tin profile của sinh viên.

## Sequence Diagrams

### 1. Xem thông tin cá nhân (Get Profile)

```mermaid
sequenceDiagram
    participant Student as Student
    participant Client as Client/Frontend
    participant ProfileController as StudentProfileController
    participant ProfileService as StudentProfileServiceImpl
    participant Repository as Repository
    participant Database as Database

    Student->>Client: Xem thông tin cá nhân
    Client->>ProfileController: GET /api/student/profile
    
    Note over ProfileController: Lấy studentId từ authentication
    ProfileController->>ProfileController: getStudentIdFromAuth(authentication)
    ProfileController->>Repository: getStudentIdByUsername(username)
    Repository->>Database: SELECT id FROM students<br/>WHERE user_id = (SELECT id FROM users<br/>WHERE username = ?)
    Database-->>Repository: studentId
    Repository-->>ProfileController: studentId
    
    ProfileController->>ProfileService: getStudentProfile(studentId)
    
    ProfileService->>Repository: findByIdAndIsDeletedFalse(studentId)
    Repository->>Database: SELECT * FROM students<br/>WHERE id = ? AND is_deleted = false
    Database-->>Repository: Student
    Repository-->>ProfileService: Student
    
    Note over ProfileService: Map Student to StudentProfileResponse<br/>(bao gồm user, department, class, address)
    ProfileService->>ProfileService: toProfileResponse(student)
    
    ProfileService-->>ProfileController: Response(success, StudentProfileResponse)
    ProfileController-->>Client: ResponseEntity.ok()
    Client-->>Student: Hiển thị thông tin cá nhân
```

### 2. Cập nhật thông tin cá nhân (Update Profile)

```mermaid
sequenceDiagram
    participant Student as Student
    participant Client as Client/Frontend
    participant ProfileController as StudentProfileController
    participant ProfileService as StudentProfileServiceImpl
    participant Repository as Repository
    participant Database as Database

    Student->>Client: Cập nhật thông tin cá nhân<br/>(studentCode, fullName, phone, dob,<br/>gender, departmentId, classId, avatarUrl)
    Client->>ProfileController: PUT /api/student/profile<br/>(StudentProfileUpdateRequest)
    
    Note over ProfileController: Lấy studentId từ authentication
    ProfileController->>ProfileController: getStudentIdFromAuth(authentication)
    ProfileController->>Repository: getStudentIdByUsername(username)
    Repository->>Database: SELECT id FROM students<br/>WHERE user_id = (SELECT id FROM users<br/>WHERE username = ?)
    Database-->>Repository: studentId
    Repository-->>ProfileController: studentId
    
    ProfileController->>ProfileService: updateStudentProfile(studentId, request)
    
    Note over ProfileService: Kiểm tra student tồn tại
    ProfileService->>Repository: findByIdAndIsDeletedFalse(studentId)
    Repository->>Database: SELECT * FROM students<br/>WHERE id = ? AND is_deleted = false
    Database-->>Repository: Student
    Repository-->>ProfileService: Student
    
    Note over ProfileService: Cập nhật thông tin cơ bản
    ProfileService->>ProfileService: student.setStudentCode(...)<br/>setFullName(...)<br/>setPhone(...)<br/>setDob(...)<br/>setGender(...)<br/>setAvatarUrl(...)
    
    alt Có departmentId
        ProfileService->>Repository: findById(departmentId)
        Repository->>Database: SELECT * FROM departments<br/>WHERE id = ?
        Database-->>Repository: Department
        Repository-->>ProfileService: Department
        ProfileService->>ProfileService: student.setDepartment(department)
    end
    
    alt Có classId
        ProfileService->>Repository: findById(classId)
        Repository->>Database: SELECT * FROM student_classes<br/>WHERE id = ?
        Database-->>Repository: StudentClass
        Repository-->>ProfileService: StudentClass
        ProfileService->>ProfileService: student.setStudentClass(studentClass)
    end
    
    Note over ProfileService: Lưu thông tin đã cập nhật
    ProfileService->>Repository: save(student)
    Repository->>Database: UPDATE students<br/>SET student_code = ?, full_name = ?,<br/>phone = ?, dob = ?, gender = ?,<br/>avatar_url = ?, department_id = ?,<br/>class_id = ?<br/>WHERE id = ?
    Database-->>Repository: Student updated
    Repository-->>ProfileService: Student
    
    ProfileService->>ProfileService: toProfileResponse(student)
    ProfileService-->>ProfileController: Response(success, StudentProfileResponse)
    ProfileController-->>Client: ResponseEntity.ok()
    Client-->>Student: Hiển thị thông báo cập nhật thành công
```

### 3. Xem thông tin cá nhân theo username (Admin/Manager)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ProfileController as StudentProfileController
    participant ProfileService as StudentProfileServiceImpl
    participant Repository as Repository
    participant Database as Database

    Admin->>Client: Xem thông tin sinh viên theo username
    Client->>ProfileController: GET /api/student/profile/{username}
    
    ProfileController->>ProfileService: getStudentProfileByUsername(username)
    
    ProfileService->>Repository: findByUserUsernameAndIsDeletedFalse(username)
    Repository->>Database: SELECT s.* FROM students s<br/>JOIN users u ON s.user_id = u.id<br/>WHERE u.username = ? AND s.is_deleted = false
    Database-->>Repository: Student
    Repository-->>ProfileService: Student
    
    Note over ProfileService: Map Student to StudentProfileResponse
    ProfileService->>ProfileService: toProfileResponse(student)
    
    ProfileService-->>ProfileController: Response(success, StudentProfileResponse)
    ProfileController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông tin sinh viên
```

## Ghi chú

1. **Quyền truy cập**:
   - Xem và cập nhật profile của chính mình: Chỉ Student
   - Xem profile theo username: Admin và Manager

2. **Thông tin profile bao gồm**:
   - Thông tin cơ bản: studentCode, fullName, phone, dob, gender, avatarUrl
   - Thông tin liên kết: department, studentClass, address
   - Thông tin từ User: username, email

3. **Validation**:
   - studentCode và fullName là bắt buộc
   - departmentId và classId phải tồn tại trong hệ thống

4. **Profile Complete**:
   - Profile được coi là hoàn chỉnh khi có: studentCode, fullName, và department

5. **Address**:
   - Địa chỉ được quản lý riêng thông qua entity Address
   - Khi hiển thị, địa chỉ được ghép thành chuỗi đầy đủ (street, ward, province)

