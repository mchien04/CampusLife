# Sequence Diagram - Student & Class Module (Nhóm Sinh viên & Lớp)

Hệ thống: **CampusLife** (Spring Boot + React)

---

## 1. Upload Excel tạo tài khoản hàng loạt (D.10)

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin/Manager
    participant Client as React Client
    participant Ctrl as AdminStudentController
    participant ExcelSvc as ExcelParserService
    participant ValSvc as ValidationService
    participant StuSvc as StudentService
    participant UserSvc as UserService
    participant StuRepo as StudentRepository
    participant UserRepo as UserRepository
    participant DB as Database
    participant EmailSvc as EmailService
    participant FileStorage as FileStorage (Local/S3)

    Note over Admin, FileStorage: Luồng 1A: Upload và parse file Excel
    Admin->>Client: Chọn file Excel danh sách sinh viên
    Client->>Client: Kiểm tra định dạng file (.xlsx/.xls)
    Client->>Ctrl: POST /api/admin/students/upload-excel<br/>(multipart/form-data: file)
    Ctrl->>Ctrl: Kiểm tra file không null, không rỗng
    Ctrl->>ExcelSvc: parseExcel(file)
    ExcelSvc->>ExcelSvc: Đọc workbook, lặp qua từng Sheet
    loop Duyệt từng row trong Excel
        ExcelSvc->>ExcelSvc: Trích xuất cell: fullName, studentCode,<br/>email, phone, classId, departmentId,...
        ExcelSvc->>ValSvc: validateRow(rowData)
        ValSvc->>ValSvc: Kiểm tra email hợp lệ (regex)
        ValSvc->>ValSvc: Kiểm tra studentCode không rỗng, không chứa khoảng trắng
        ValSvc->>StuRepo: existsByStudentCode(studentCode)
        StuRepo->>DB: SELECT * FROM students WHERE student_code = ?
        DB-->>StuRepo:' "result (boolean)"'
        StuRepo-->>ValSvc:' "exists (boolean)"'
        ValSvc->>StuRepo: existsByEmail(email)
        StuRepo->>DB: SELECT * FROM users WHERE email = ?
        DB-->>StuRepo:' "result (boolean)"'
        StuRepo-->>ValSvc:' "exists (boolean)"'
        alt Dữ liệu row hợp lệ
            ValSvc-->>ExcelSvc: valid = true
            ExcelSvc->>ExcelSvc: Thêm row vào validRows list
        else Dữ liệu row không hợp lệ
            ValSvc-->>ExcelSvc: valid = false, lý do lỗi
            ExcelSvc->>ExcelSvc: Thêm row vào invalidRows list (log lỗi)
        end
    end
    ExcelSvc-->>Ctrl: ParseResult(validRows, invalidRows, errorDetails)

    Note over Ctrl, DB: Luồng 1B: Batch tạo User + Student
    Ctrl->>StuSvc: batchCreateStudents(validRows)
    loop Duyệt từng valid row
        StuSvc->>UserSvc: createUserFromExcel(rowData)
        UserSvc->>UserSvc: Generate username = studentCode<br/>Generate random password (8-12 chars, aA1!)
        UserSvc->>UserSvc: BCrypt password = bcrypt(randomPassword)
        UserSvc->>UserSvc: Set role = STUDENT
        UserSvc->>UserRepo: save(User)
        UserRepo->>DB: INSERT INTO users (username, email, password, role, ...)
        DB-->>UserRepo:' "User persisted (return userId)"'
        UserRepo-->>UserSvc: User entity
        UserSvc-->>StuSvc: User entity
        StuSvc->>StuSvc: Tạo Student entity: userId, studentCode, fullName, classId,...
        StuSvc->>StuRepo: save(Student)
        StuRepo->>DB: INSERT INTO students (user_id, student_code, full_name, class_id, ...)
        DB-->>StuRepo:' "Student persisted"'
        StuRepo-->>StuSvc:' "Student entity"'
        StuSvc->>EmailSvc: queueEmail(username, randomPassword, email)
        EmailSvc->>EmailSvc: Tạo email nội dung thông tin đăng nhập
        EmailSvc->>EmailSvc: Gửi email bất đồng bộ (async)
    end
    StuSvc-->>Ctrl: BatchResult(successCount, failCount, failedRows)
    Ctrl-->>Client: 200 OK + JSON {successCount, failCount, totalRows, invalidRows[]}
    Client-->>Admin: Hiển thị kết quả: số tạo thành công, số lỗi, chi tiết lỗi từng row

    Note over Admin, FileStorage: Luồng 1C (Optional): Lưu file Excel backup
    Ctrl->>FileStorage: saveFile(file, "uploads/students/")
    FileStorage-->>Ctrl: filePath
    Ctrl->>DB: INSERT INTO upload_logs (file_name, file_path, success_count, fail_count, uploaded_by)
    DB-->>Ctrl:' "log saved"'
```

---

## 2. Tạo / Sửa / Xóa lớp học (D.11)

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin/Manager
    participant Client as React Client
    participant Ctrl as AdminClassController
    participant ClassSvc as StudentClassService
    participant DeptSvc as DepartmentService
    participant ClassRepo as StudentClassRepository
    participant DB as Database

    Note over Admin, DB: Luồng 2A: Tạo lớp học (CREATE)
    Admin->>Client: Nhập thông tin lớp: name, code, departmentId, year, capacity,...
    Client->>Client: Validate form (required fields, min/max length)
    Client->>Ctrl: POST /api/admin/classes<br/>JSON {name, code, departmentId, year, ...}
    Ctrl->>Ctrl: Validate request body (@Valid)
    Ctrl->>ClassSvc: createClass(classDTO)
    ClassSvc->>DeptSvc: findById(departmentId)
    DeptSvc->>DB: SELECT * FROM departments WHERE id = ?
    DB-->>DeptSvc:' "Department entity (hoặc null)"'
    alt Department không tồn tại
        DeptSvc-->>ClassSvc: throw DepartmentNotFoundException
        ClassSvc-->>Ctrl: Exception
        Ctrl-->>Client:' 404 Not Found + {error: "Department not found"}'
        Client-->>Admin:' Hiển thị lỗi: "Khoa không tồn tại"'
    else Department tồn tại
        DeptSvc-->>ClassSvc: Department entity
        ClassSvc->>ClassRepo: existsByCode(code)
        ClassRepo->>DB: SELECT * FROM student_classes WHERE code = ?
        DB-->>ClassRepo:' "true (đã tồn tại)"'
        alt Class code đã tồn tại
            ClassRepo-->>ClassSvc: true
            ClassSvc-->>Ctrl: throw DuplicateCodeException
            Ctrl-->>Client:' 409 Conflict + {error: "Class code already exists"}'
            Client-->>Admin:' Hiển thị lỗi: "Mã lớp đã tồn tại"'
        else Class code chưa tồn tại
            ClassRepo-->>ClassSvc: false
            ClassSvc->>ClassSvc: Tạo StudentClass entity từ DTO
            ClassSvc->>ClassRepo: save(StudentClass)
            ClassRepo->>DB: INSERT INTO student_classes (name, code, department_id, year, ...)
            DB-->>ClassRepo:' "StudentClass persisted (id generated)"'
            ClassRepo-->>ClassSvc: StudentClass entity
            ClassSvc-->>Ctrl: ClassResponseDTO
            Ctrl-->>Client: 201 Created + JSON ClassResponseDTO
            Client-->>Admin:' Hiển thị thông báo "Tạo lớp thành công", cập nhật danh sách lớp'
        end
    end

    Note over Admin, DB: Luồng 2B: Sửa lớp học (UPDATE)
    Admin->>Client: Chọn lớp cần sửa, thay đổi thông tin
    Client->>Ctrl: PUT /api/admin/classes/{id}<br/>JSON {name, code, departmentId, year, ...}
    Ctrl->>Ctrl: Validate @PathVariable id, @RequestBody DTO
    Ctrl->>ClassSvc: updateClass(id, classDTO)
    ClassSvc->>ClassRepo: findById(id)
    ClassRepo->>DB: SELECT * FROM student_classes WHERE id = ?
    DB-->>ClassRepo:' "StudentClass entity (hoặc null)"'
    alt Class không tồn tại
        ClassRepo-->>ClassSvc: Optional.empty
        ClassSvc-->>Ctrl: throw ClassNotFoundException
        Ctrl-->>Client:' 404 Not Found + {error: "Class not found"}'
        Client-->>Admin:' Hiển thị lỗi: "Lớp không tồn tại"'
    else Class tồn tại
        ClassRepo-->>ClassSvc: StudentClass entity
        ClassSvc->>ClassRepo: existsByCodeAndIdNot(code, id)
        ClassRepo->>DB: SELECT * FROM student_classes WHERE code = ? AND id != ?
        DB-->>ClassRepo:' "true (code đã được lớp khác dùng)"'
        alt Code bị trùng với lớp khác
            ClassRepo-->>ClassSvc: true
            ClassSvc-->>Ctrl: throw DuplicateCodeException
            Ctrl-->>Client:' 409 Conflict + {error: "Code already used by another class"}'
            Client-->>Admin:' Hiển thị lỗi: "Mã lớp đã được sử dụng"'
        else Code hợp lệ
            ClassRepo-->>ClassSvc: false
            ClassSvc->>DeptSvc: findById(departmentId) (nếu departmentId thay đổi)
            DeptSvc->>DB: SELECT * FROM departments WHERE id = ?
            DB-->>DeptSvc:' "Department entity"'
            DeptSvc-->>ClassSvc: Department
            ClassSvc->>ClassSvc: Update entity: setName, setCode, setDepartmentId, setYear, setCapacity,...
            ClassSvc->>ClassRepo: save(StudentClass)
            ClassRepo->>DB: UPDATE student_classes SET name=?, code=?, department_id=?, year=?, updated_at=NOW() WHERE id=?
            DB-->>ClassRepo:' "updated rows"'
            ClassRepo-->>ClassSvc: StudentClass entity (updated)
            ClassSvc-->>Ctrl: ClassResponseDTO
            Ctrl-->>Client: 200 OK + JSON ClassResponseDTO
            Client-->>Admin:' Hiển thị thông báo "Cập nhật lớp thành công", refresh bảng'
        end
    end

    Note over Admin, DB: Luồng 2C: Xóa lớp học (DELETE)
    Admin->>Client: Chọn lớp cần xóa, click "Xóa"
    Client->>Client: Hiển thị dialog xác nhận: "Bạn có chắc muốn xóa lớp này?"
    Admin->>Client: Xác nhận xóa
    Client->>Ctrl: DELETE /api/admin/classes/{id}
    Ctrl->>Ctrl: Validate id
    Ctrl->>ClassSvc: deleteClass(id)
    ClassSvc->>ClassRepo: findById(id)
    ClassRepo->>DB: SELECT * FROM student_classes WHERE id = ?
    DB-->>ClassRepo:' "StudentClass entity (hoặc null)"'
    alt Class không tồn tại
        ClassRepo-->>ClassSvc: Optional.empty
        ClassSvc-->>Ctrl: throw ClassNotFoundException
        Ctrl-->>Client: 404 Not Found
        Client-->>Admin:' Hiển thị lỗi: "Lớp không tồn tại"'
    else Class tồn tại
        ClassRepo-->>ClassSvc: StudentClass entity
        ClassSvc->>ClassRepo: countStudentsByClassId(id)
        ClassRepo->>DB: SELECT COUNT(*) FROM students WHERE class_id = ?
        DB-->>ClassRepo:' "count (n)"'
        alt Class có sinh viên (n > 0)
            ClassRepo-->>ClassSvc: n > 0
            ClassSvc-->>Ctrl:' throw ClassHasStudentsException("Lớp đang có sinh viên, không thể xóa")'
            Ctrl-->>Client:' 409 Conflict + {error: "Class has students, cannot delete"}'
            Client-->>Admin:' Hiển thị lỗi: "Lớp đang có sinh viên, không thể xóa. Vui lòng chuyển sinh viên sang lớp khác trước."'
        else Class không có sinh viên (n = 0)
            ClassRepo-->>ClassSvc: n = 0
            ClassSvc->>ClassRepo: deleteById(id)
            ClassRepo->>DB: DELETE FROM student_classes WHERE id = ?
            DB-->>ClassRepo:' "deleted rows"'
            ClassRepo-->>ClassSvc: void
            ClassSvc-->>Ctrl: void
            Ctrl-->>Client:' 204 No Content (hoặc 200 OK + {message: "Deleted"})'
            Client-->>Admin:' Hiển thị thông báo "Xóa lớp thành công", remove khỏi bảng'
        end
    end
```

---

## 3. Thêm / Xóa sinh viên khỏi lớp (D.12)

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin/Manager
    participant Client as React Client
    participant Ctrl as AdminClassController
    participant ClassSvc as StudentClassService
    participant StuSvc as StudentService
    participant ClassRepo as StudentClassRepository
    participant StuRepo as StudentRepository
    participant DB as Database

    Note over Admin, DB: Luồng 3A: Thêm sinh viên vào lớp
    Admin->>Client: Chọn lớp, tìm sinh viên, click "Thêm vào lớp"
    Client->>Ctrl: POST /api/admin/classes/{classId}/students/{studentId}
    Ctrl->>Ctrl: Validate classId, studentId (@Positive, @NotNull)
    Ctrl->>ClassSvc: addStudentToClass(classId, studentId)
    ClassSvc->>ClassRepo: findById(classId)
    ClassRepo->>DB: SELECT * FROM student_classes WHERE id = ?
    DB-->>ClassRepo:' "StudentClass entity (hoặc null)"'
    alt Class không tồn tại
        ClassRepo-->>ClassSvc: Optional.empty
        ClassSvc-->>Ctrl: throw ClassNotFoundException
        Ctrl-->>Client:' 404 Not Found + {error: "Class not found"}'
        Client-->>Admin:' Hiển thị lỗi: "Lớp không tồn tại"'
    else Class tồn tại
        ClassRepo-->>ClassSvc: StudentClass entity
        ClassSvc->>StuSvc: findById(studentId)
        StuSvc->>StuRepo: findById(studentId)
        StuRepo->>DB: SELECT * FROM students WHERE id = ?
        DB-->>StuRepo:' "Student entity (hoặc null)"'
        alt Student không tồn tại
            StuRepo-->>StuSvc:' "Optional.empty"'
            StuSvc-->>ClassSvc: throw StudentNotFoundException
            ClassSvc-->>Ctrl: Exception
            Ctrl-->>Client:' 404 Not Found + {error: "Student not found"}'
            Client-->>Admin:' Hiển thị lỗi: "Sinh viên không tồn tại"'
        else Student tồn tại
            StuRepo-->>StuSvc:' "Student entity"'
            StuSvc-->>ClassSvc: Student entity
            ClassSvc->>StuRepo: existsByClassIdAndStudentId(classId, studentId)
            StuRepo->>DB: SELECT * FROM students WHERE class_id = ? AND id = ?
            DB-->>StuRepo:' "true (đã trong lớp)"'
            alt Student đã trong lớp
                StuRepo-->>ClassSvc:' "true"'
                ClassSvc-->>Ctrl: throw DuplicateMembershipException
                Ctrl-->>Client:' 409 Conflict + {error: "Student already in class"}'
                Client-->>Admin:' Hiển thị lỗi: "Sinh viên đã thuộc lớp này"'
            else Student chưa trong lớp
                StuRepo-->>ClassSvc:' "false"'
                ClassSvc->>StuSvc: assignClass(studentId, classId)
                StuSvc->>StuRepo: findById(studentId)
                StuRepo->>DB: SELECT * FROM students WHERE id = ? FOR UPDATE
                DB-->>StuRepo:' "Student entity"'
                StuRepo-->>StuSvc:' "Student entity"'
                StuSvc->>StuSvc: student.setClassId(classId)
                StuSvc->>StuRepo: save(Student)
                StuRepo->>DB: UPDATE students SET class_id = ?, updated_at = NOW() WHERE id = ?
                DB-->>StuRepo:' "updated rows"'
                StuRepo-->>StuSvc:' "Student entity (updated)"'
                StuSvc-->>ClassSvc: StudentResponseDTO
                ClassSvc-->>Ctrl: Success result
                Ctrl-->>Client:' 200 OK + JSON {message: "Student added to class", student: {...}}'
                Client-->>Admin:' Hiển thị thông báo "Thêm sinh viên vào lớp thành công", cập nhật danh sách lớp'
            end
        end
    end

    Note over Admin, DB: Luồng 3B: Xóa sinh viên khỏi lớp
    Admin->>Client: Chọn sinh viên trong lớp, click "Xóa khỏi lớp"
    Client->>Client: Hiển thị dialog xác nhận
    Admin->>Client: Xác nhận xóa
    Client->>Ctrl: DELETE /api/admin/classes/{classId}/students/{studentId}
    Ctrl->>Ctrl: Validate classId, studentId
    Ctrl->>ClassSvc: removeStudentFromClass(classId, studentId)
    ClassSvc->>ClassRepo: findById(classId)
    ClassRepo->>DB: SELECT * FROM student_classes WHERE id = ?
    DB-->>ClassRepo:' "StudentClass entity (hoặc null)"'
    alt Class không tồn tại
        ClassRepo-->>ClassSvc: Optional.empty
        ClassSvc-->>Ctrl: throw ClassNotFoundException
        Ctrl-->>Client: 404 Not Found
        Client-->>Admin:' Hiển thị lỗi: "Lớp không tồn tại"'
    else Class tồn tại
        ClassRepo-->>ClassSvc: StudentClass entity
        ClassSvc->>StuRepo: findByIdAndClassId(studentId, classId)
        StuRepo->>DB: SELECT * FROM students WHERE id = ? AND class_id = ?
        DB-->>StuRepo:' "Student entity (hoặc null)"'
        alt Student không thuộc lớp hoặc không tồn tại
            StuRepo-->>ClassSvc:' "Optional.empty"'
            ClassSvc-->>Ctrl: throw StudentNotInClassException
            Ctrl-->>Client:' 404 Not Found + {error: "Student not found in this class"}'
            Client-->>Admin:' Hiển thị lỗi: "Sinh viên không thuộc lớp này"'
        else Student thuộc lớp
            StuRepo-->>ClassSvc:' "Student entity"'
            ClassSvc->>StuSvc: removeClassAssignment(studentId)
            StuSvc->>StuRepo: findById(studentId)
            StuRepo->>DB: SELECT * FROM students WHERE id = ? FOR UPDATE
            DB-->>StuRepo:' "Student entity"'
            StuRepo-->>StuSvc:' "Student entity"'
            StuSvc->>StuSvc: student.setClassId(null)
            StuSvc->>StuRepo: save(Student)
            StuRepo->>DB: UPDATE students SET class_id = NULL, updated_at = NOW() WHERE id = ?
            DB-->>StuRepo:' "updated rows"'
            StuRepo-->>StuSvc:' "Student entity (updated)"'
            StuSvc-->>ClassSvc: StudentResponseDTO
            ClassSvc-->>Ctrl: Success result
            Ctrl-->>Client:' 200 OK + JSON {message: "Student removed from class", student: {...}}'
            Client-->>Admin:' Hiển thị thông báo "Xóa sinh viên khỏi lớp thành công", refresh danh sách lớp'
        end
    end
```

---

## 4. Tìm kiếm sinh viên (D.13)

```mermaid
sequenceDiagram
    autonumber
    actor User as Admin/Manager/Staff
    participant Client as React Client
    participant Ctrl as StudentController
    participant StuSvc as StudentService
    participant StuRepo as StudentRepository
    participant DB as Database

    Note over User, DB: Luồng tìm kiếm sinh viên theo keyword + phân trang
    User->>Client: Nhập keyword vào ô tìm kiếm (tên, mã SV, email)
    User->>Client: Nhấn Enter hoặc click nút "Tìm kiếm"
    Client->>Client: Debounce 300ms (nếu search-as-you-type)
    Client->>Client: Kiểm tra keyword length >= 2 (hoặc cho phép rỗng = list all)
    Client->>Ctrl: GET /api/students/search?keyword={kw}&page={p}&size={s}&sort={sortField},{direction}
    Ctrl->>Ctrl: Extract @RequestParam: keyword, page, size, sort<br/>Mặc định: page=0, size=10, sort=createdAt,desc
    Ctrl->>StuSvc: searchStudents(keyword, pageable)
    StuSvc->>StuRepo: searchByKeyword(keyword, pageable)
    alt Keyword rỗng hoặc null
        StuRepo->>DB: SELECT * FROM students ORDER BY ? LIMIT ? OFFSET ?
    else Keyword không rỗng
        StuRepo->>DB: SELECT * FROM students WHERE<br/>LOWER(full_name) LIKE LOWER(?)<br/>OR LOWER(student_code) LIKE LOWER(?)<br/>OR LOWER(email) LIKE LOWER(?)<br/>ORDER BY ? LIMIT ? OFFSET ?
        Note right of DB: Search pattern: %keyword%
    end
    DB-->>StuRepo:' "List<Student> + totalCount"'
    StuRepo-->>StuSvc:' "Page<Student> entity"'
    StuSvc->>StuSvc: Map từng Student -> StudentResponseDTO
    StuSvc->>StuSvc: Enrich DTO: className, departmentName, userInfo (email, username)
    StuSvc-->>Ctrl: Page<StudentResponseDTO> {content, totalElements, totalPages, number, size, first, last}
    Ctrl-->>Client: 200 OK + JSON PageResponse<StudentResponseDTO>
    Client->>Client: Cập nhật state: danh sách sinh viên, tổng số trang, trang hiện tại
    Client-->>User: Hiển thị bảng kết quả tìm kiếm với phân trang, sortable columns
```

---

## 5. Gửi thông tin đăng nhập (D.14)

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin/Manager
    participant Client as React Client
    participant Ctrl as AdminStudentController
    participant StuSvc as StudentService
    participant UserSvc as UserService
    participant AuthSvc as AuthService/PasswordUtil
    participant StuRepo as StudentRepository
    participant UserRepo as UserRepository
    participant DB as Database
    participant EmailSvc as EmailService
    participant MailProvider as Mail Provider (SMTP/SendGrid/SES)

    Note over Admin, MailProvider: Luồng gửi lại thông tin đăng nhập cho sinh viên
    Admin->>Client: Tìm sinh viên trong danh sách, click "Gửi thông tin đăng nhập"
    Client->>Client: Hiển thị dialog xác nhận: "Gửi email thông tin đăng nhập cho sinh viên [Tên]?"
    Admin->>Client: Xác nhận gửi
    Client->>Ctrl: POST /api/admin/students/{id}/send-credentials
    Ctrl->>Ctrl: Validate @PathVariable id (@Positive)
    Ctrl->>StuSvc: findById(id)
    StuSvc->>StuRepo: findById(id)
    StuRepo->>DB: SELECT * FROM students WHERE id = ?
    DB-->>StuRepo:' "Student entity (hoặc null)"'
    alt Student không tồn tại
        StuRepo-->>StuSvc:' "Optional.empty"'
        StuSvc-->>Ctrl: throw StudentNotFoundException
        Ctrl-->>Client:' 404 Not Found + {error: "Student not found"}'
        Client-->>Admin:' Hiển thị lỗi: "Sinh viên không tồn tại"'
    else Student tồn tại
        StuRepo-->>StuSvc:' "Student entity"'
        StuSvc->>StuSvc: Lấy userId từ Student
        StuSvc->>UserSvc: findById(userId)
        UserSvc->>UserRepo: findById(userId)
        UserRepo->>DB: SELECT * FROM users WHERE id = ?
        DB-->>UserRepo:' "User entity (hoặc null)"'
        alt User không tồn tại hoặc bị vô hiệu hóa
            UserRepo-->>UserSvc: Optional.empty / User disabled
            UserSvc-->>StuSvc: throw UserNotFoundException / UserDisabledException
            StuSvc-->>Ctrl: Exception
            Ctrl-->>Client:' 404/403 + {error: "User account not found or disabled"}'
            Client-->>Admin:' Hiển thị lỗi: "Tài khoản không tồn tại hoặc đã bị vô hiệu hóa"'
        else User tồn tại và active
            UserRepo-->>UserSvc: User entity
            UserSvc-->>StuSvc: User entity
            StuSvc->>AuthSvc: generateRandomPassword(length=10)
            AuthSvc->>AuthSvc: Tạo chuỗi ngẫu nhiên: [a-zA-Z0-9!@#$%^&*]
            AuthSvc-->>StuSvc: plainPassword (raw text)
            StuSvc->>AuthSvc: bcryptEncode(plainPassword)
            AuthSvc->>AuthSvc: BCryptPasswordEncoder.encode(plainPassword)
            AuthSvc-->>StuSvc: encryptedPassword
            StuSvc->>UserSvc: updatePassword(userId, encryptedPassword)
            UserSvc->>UserRepo: updatePassword(userId, encryptedPassword)
            UserRepo->>DB: UPDATE users SET password = ?, updated_at = NOW() WHERE id = ?
            DB-->>UserRepo:' "updated rows"'
            UserRepo-->>UserSvc: void
            UserSvc-->>StuSvc: void
            StuSvc->>StuSvc: Chuẩn bị dữ liệu email: username, plainPassword, fullName, email
            StuSvc->>EmailSvc: sendCredentialsEmail(email, username, plainPassword, fullName)
            EmailSvc->>EmailSvc: Build email template (HTML/Text):<br/>Subject: "Thông tin đăng nhập hệ thống CampusLife"<br/>Body: Xin chào [fullName],<br/>Tài khoản: [username]<br/>Mật khẩu mới: [plainPassword]<br/>Vui lòng đổi mật khẩu sau khi đăng nhập.
            EmailSvc->>MailProvider: send(emailMessage)
            MailProvider-->>EmailSvc: Email sent / queued successfully
            EmailSvc-->>StuSvc: void
            StuSvc-->>Ctrl:' SendCredentialResult{success: true, emailSentTo: email, message: "Credentials sent successfully"}'
            Ctrl-->>Client:' 200 OK + JSON {success: true, message: "Thông tin đăng nhập đã được gửi qua email"}'
            Client-->>Admin: Hiển thị thông báo thành công, có thể log lịch sử gửi
        end
    end

    Note over Admin, MailProvider: Luồng ghi log (Optional)
    StuSvc->>DB: INSERT INTO credential_logs (student_id, sent_by, sent_at, email, status)
    DB-->>StuSvc:' "log saved"'
```

---

## 6. Quản lý địa chỉ sinh viên (D.15)

```mermaid
sequenceDiagram
    autonumber
    actor Student as Student (Sinh viên)
    participant Client as React Client
    participant Ctrl as StudentController
    participant StuSvc as StudentService
    participant StuRepo as StudentRepository
    participant DB as Database

    Note over Student, DB: Luồng 6A: Thêm địa chỉ cho sinh viên (CREATE/UPDATE Address)
    Student->>Client: Vào trang hồ sơ cá nhân, tab "Địa chỉ"
    Student->>Client: Nhập địa chỉ: permanentAddress, temporaryAddress, hometown<br/>+ Các trường: province, district, ward, street, detail
    Client->>Client: Validate form (max length, required fields)
    Client->>Ctrl: POST /api/students/{id}/address<br/>JSON {permanentAddress, temporaryAddress, hometown, province, district, ward, street, detail}
    Ctrl->>Ctrl: Validate @PathVariable id, @RequestBody DTO (@Valid)
    Ctrl->>StuSvc: addOrUpdateAddress(id, addressDTO)
    StuSvc->>StuRepo: findById(id)
    StuRepo->>DB: SELECT * FROM students WHERE id = ?
    DB-->>StuRepo:' "Student entity (hoặc null)"'
    alt Student không tồn tại
        StuRepo-->>StuSvc:' "Optional.empty"'
        StuSvc-->>Ctrl: throw StudentNotFoundException
        Ctrl-->>Client:' 404 Not Found + {error: "Student not found"}'
        Client-->>Student:' Hiển thị lỗi: "Không tìm thấy thông tin sinh viên"'
    else Student tồn tại
        StuRepo-->>StuSvc:' "Student entity"'
        alt Sinh viên đang cập nhật địa chỉ của chính mình (self-service)
            StuSvc->>StuSvc: Kiểm tra: student.userId == currentUser.id (Security check)
            alt Không có quyền (cố gắng sửa địa chỉ người khác)
                StuSvc-->>Ctrl: throw AccessDeniedException
                Ctrl-->>Client:' 403 Forbidden + {error: "Access denied"}'
                Client-->>Student:' Hiển thị lỗi: "Bạn không có quyền thực hiện thao tác này"'
            else Có quyền
                StuSvc->>StuSvc: Cập nhật các trường địa chỉ:<br/>setPermanentAddress(?)<br/>setTemporaryAddress(?)<br/>setHometown(?)<br/>setProvince(?)<br/>setDistrict(?)<br/>setWard(?)<br/>setStreet(?)<br/>setDetailAddress(?)
                StuSvc->>StuRepo: save(Student)
                StuRepo->>DB: UPDATE students SET<br/>permanent_address = ?,<br/>temporary_address = ?,<br/>hometown = ?,<br/>province = ?,<br/>district = ?,<br/>ward = ?,<br/>street = ?,<br/>detail_address = ?,<br/>updated_at = NOW()<br/>WHERE id = ?
                DB-->>StuRepo:' "updated rows"'
                StuRepo-->>StuSvc:' "Student entity (updated)"'
                StuSvc-->>Ctrl: StudentResponseDTO (bao gồm address info)
                Ctrl-->>Client: 200 OK + JSON StudentResponseDTO
                Client-->>Student:' Hiển thị thông báo "Cập nhật địa chỉ thành công", refresh form'
            end
        else Admin đang cập nhật địa chỉ cho sinh viên (admin override)
            Note right of StuSvc: Admin có role MANAGER/ADMIN, bỏ qua self-check
            StuSvc->>StuSvc: Cập nhật các trường địa chỉ tương tự
            StuSvc->>StuRepo: save(Student)
            StuRepo->>DB: UPDATE students SET ... WHERE id = ?
            DB-->>StuRepo:' "updated rows"'
            StuRepo-->>StuSvc:' "Student entity (updated)"'
            StuSvc-->>Ctrl: StudentResponseDTO
            Ctrl-->>Client: 200 OK + JSON StudentResponseDTO
            Client-->>Student: Hiển thị thông báo thành công
        end
    end

    Note over Student, DB: Luồng 6B: Xóa địa chỉ (Clear Address Fields)
    Student->>Client: Chọn địa chỉ cần xóa, click "Xóa" (hoặc "Xóa tất cả")
    Client->>Client: Hiển thị dialog xác nhận
    Student->>Client: Xác nhận xóa
    Client->>Ctrl: DELETE /api/students/{id}/address<br/>Query param: type=permanent | temporary | hometown | all
    Ctrl->>Ctrl: Validate id, type
    Ctrl->>StuSvc: clearAddress(id, type)
    StuSvc->>StuRepo: findById(id)
    StuRepo->>DB: SELECT * FROM students WHERE id = ?
    DB-->>StuRepo:' "Student entity (hoặc null)"'
    alt Student không tồn tại
        StuRepo-->>StuSvc:' "Optional.empty"'
        StuSvc-->>Ctrl: throw StudentNotFoundException
        Ctrl-->>Client: 404 Not Found
        Client-->>Student:' Hiển thị lỗi: "Không tìm thấy sinh viên"'
    else Student tồn tại
        StuRepo-->>StuSvc:' "Student entity"'
        StuSvc->>StuSvc: Security check (self hoặc admin)
        alt type = "permanent"
            StuSvc->>StuSvc: student.setPermanentAddress(null)
        else type = "temporary"
            StuSvc->>StuSvc: student.setTemporaryAddress(null)
        else type = "hometown"
            StuSvc->>StuSvc: student.setHometown(null)
        else type = "all"
            StuSvc->>StuSvc: student.setPermanentAddress(null)<br/>student.setTemporaryAddress(null)<br/>student.setHometown(null)<br/>student.setProvince(null)<br/>student.setDistrict(null)<br/>student.setWard(null)<br/>student.setStreet(null)<br/>student.setDetailAddress(null)
        end
        StuSvc->>StuRepo: save(Student)
        StuRepo->>DB: UPDATE students SET ... = NULL, updated_at = NOW() WHERE id = ?
        DB-->>StuRepo:' "updated rows"'
        StuRepo-->>StuSvc:' "Student entity (updated)"'
        StuSvc-->>Ctrl: StudentResponseDTO
        Ctrl-->>Client:' 200 OK + JSON {message: "Address cleared successfully", student: {...}}'
        Client-->>Student:' Hiển thị thông báo "Xóa địa chỉ thành công", clear form fields'
    end

    Note over Student, DB: Luồng 6C: Lấy thông tin địa chỉ (READ)
    Student->>Client: Vào trang hồ sơ, tab địa chỉ
    Client->>Ctrl: GET /api/students/{id}/address
    Ctrl->>StuSvc: getAddress(id)
    StuSvc->>StuRepo: findById(id)
    StuRepo->>DB: SELECT * FROM students WHERE id = ?
    DB-->>StuRepo:' "Student entity (hoặc null)"'
    alt Student không tồn tại
        StuRepo-->>StuSvc:' "Optional.empty"'
        StuSvc-->>Ctrl: throw StudentNotFoundException
        Ctrl-->>Client: 404 Not Found
        Client-->>Student: Hiển thị lỗi
    else Student tồn tại
        StuRepo-->>StuSvc:' "Student entity"'
        StuSvc->>StuSvc: Map address fields -> AddressDTO
        StuSvc-->>Ctrl: AddressResponseDTO
        Ctrl-->>Client: 200 OK + JSON AddressResponseDTO
        Client-->>Student: Hiển thị form địa chỉ với dữ liệu hiện tại
    end
```

---

## Tóm tắt thành phần và chức năng

| Thành phần | Vai trò | Chức năng chính trong module Student & Class |
|------------|---------|-----------------------------------------------|
| **Admin/Manager** | Actor | Người dùng có quyền quản trị: upload Excel, CRUD lớp, gửi credentials, thêm/xóa sinh viên khỏi lớp. |
| **Student** | Actor | Sinh viên tự quản lý địa chỉ cá nhân (thêm, sửa, xóa, xem địa chỉ). |
| **React Client** | Frontend | UI nhập liệu, validate form, gọi API, hiển thị kết quả, dialog xác nhận, debounce search. |
| **AdminStudentController** | Controller | Nhận request từ admin liên quan đến sinh viên: upload Excel, gửi credentials, tìm kiếm. |
| **AdminClassController** | Controller | Nhận request từ admin liên quan đến lớp học: CRUD lớp, thêm/xóa sinh viên khỏi lớp. |
| **StudentController** | Controller | Nhận request từ sinh viên/staff: tìm kiếm, quản lý địa chỉ. |
| **StudentService** | Service | Business logic chính cho sinh viên: tìm kiếm, cập nhật địa chỉ, kiểm tra quyền, map DTO. |
| **StudentClassService** | Service | Business logic cho lớp học: CRUD lớp, kiểm tra code trùng, kiểm tra sinh viên trong lớp trước khi xóa. |
| **UserService** | Service | Quản lý tài khoản user: tạo user, cập nhật password, tìm user theo id. |
| **ExcelParserService** | Service | Đọc và parse file Excel, duyệt từng row, trích xuất dữ liệu sinh viên. |
| **ValidationService** | Service | Validate dữ liệu từng row Excel: email regex, studentCode format, kiểm tra trùng lặp. |
| **DepartmentService** | Service | Kiểm tra sự tồn tại của khoa/phòng ban khi tạo/sửa lớp. |
| **AuthService / PasswordUtil** | Service/Util | Sinh mật khẩu ngẫu nhiên, mã hóa BCrypt. |
| **EmailService** | Service | Xây dựng template và gửi email thông tin đăng nhập (async). |
| **StudentRepository** | Repository | Truy cập DB cho bảng students: CRUD, search, tìm theo classId, exists queries. |
| **StudentClassRepository** | Repository | Truy cập DB cho bảng student_classes: CRUD, existsByCode, countStudentsByClassId. |
| **UserRepository** | Repository | Truy cập DB cho bảng users: save, findById, updatePassword, existsByEmail. |
| **Database** | Database | Lưu trữ dữ liệu: students, student_classes, users, departments, credential_logs. |
| **FileStorage** | External | Lưu trữ file Excel upload (local disk hoặc S3) để backup. |
| **Mail Provider** | External | Dịch vụ gửi email bên ngoài (SMTP, SendGrid, AWS SES). |

### Các luồng nghiệp vụ (Use Cases) đã mô tả

| Mã | Tên luồng | Endpoint chính | Actor |
|----|-----------|---------------|-------|
| D.10 | Upload Excel tạo tài khoản hàng loạt | `POST /api/admin/students/upload-excel` | Admin/Manager |
| D.11 | Tạo/Sửa/Xóa lớp học | `POST /api/admin/classes`, `PUT /api/admin/classes/{id}`, `DELETE /api/admin/classes/{id}` | Admin/Manager |
| D.12 | Thêm/Xóa sinh viên khỏi lớp | `POST /api/admin/classes/{classId}/students/{studentId}`, `DELETE /api/admin/classes/{classId}/students/{studentId}` | Admin/Manager |
| D.13 | Tìm kiếm sinh viên | `GET /api/students/search?keyword=&page=&size=` | Admin/Manager/Staff |
| D.14 | Gửi thông tin đăng nhập | `POST /api/admin/students/{id}/send-credentials` | Admin/Manager |
| D.15 | Quản lý địa chỉ sinh viên | `POST /api/students/{id}/address`, `DELETE /api/students/{id}/address`, `GET /api/students/{id}/address` | Student (Self) / Admin |
