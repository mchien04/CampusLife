# Sequence Diagram - Chức năng Quản lý Tài khoản

## Mô tả
Sequence diagram mô tả luồng xử lý quản lý tài khoản trong hệ thống CampusLife (chỉ dành cho ADMIN).

## Sequence Diagrams

### 1. Tạo tài khoản (Create User)

```mermaid
sequenceDiagram
    participant Admin as Admin
    participant Client as Client/Frontend
    participant UserController as UserManagementController
    participant UserService as UserManagementServiceImpl
    participant Repository as Repository<br/>(User)
    participant Database as Database
    participant PasswordEncoder as PasswordEncoder

    Admin->>Client: Nhập thông tin tài khoản<br/>(username, email, password, role)
    Client->>UserController: POST /api/admin/users<br/>(CreateUserRequest)
    
    UserController->>UserService: createUser(request)
    
    Note over UserService: Validate request:<br/>- Username, email, password không rỗng<br/>- Password >= 6 ký tự<br/>- Role = ADMIN hoặc MANAGER
    
    Note over UserService: Kiểm tra trùng lặp
    UserService->>Repository: findByUsername(username)
    Repository->>Database: SELECT * FROM users WHERE username = ?
    Database-->>Repository: User hoặc null
    Repository-->>UserService: Optional<User>
    
    UserService->>Repository: findByEmail(email)
    Repository->>Database: SELECT * FROM users WHERE email = ?
    Database-->>Repository: User hoặc null
    Repository-->>UserService: Optional<User>
    
    Note over UserService: Tạo user mới:<br/>- Mã hóa password (BCrypt)<br/>- Set activated = true<br/>- Set deleted = false
    UserService->>PasswordEncoder: encode(password)
    PasswordEncoder-->>UserService: Hashed password
    
    UserService->>Repository: save(user)
    Repository->>Database: INSERT INTO users<br/>(username, email, password, role, is_activated)
    Database-->>Repository: User saved
    Repository-->>UserService: User
    
    UserService-->>UserController: Response(success, UserResponse)
    UserController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
```

### 2. Cập nhật tài khoản (Update User)

```mermaid
sequenceDiagram
    participant Admin as Admin
    participant Client as Client/Frontend
    participant UserController as UserManagementController
    participant UserService as UserManagementServiceImpl
    participant Repository as Repository<br/>(User)
    participant Database as Database
    participant PasswordEncoder as PasswordEncoder

    Admin->>Client: Chọn user và cập nhật thông tin
    Client->>UserController: PUT /api/admin/users/{userId}<br/>(UpdateUserRequest)
    
    UserController->>UserService: updateUser(userId, request)
    
    UserService->>Repository: findById(userId)
    Repository->>Database: SELECT * FROM users WHERE id = ?
    Database-->>Repository: User
    Repository-->>UserService: User
    
    Note over UserService: Kiểm tra user chưa bị xóa
    
    Note over UserService: Cập nhật các trường:<br/>- Username (nếu có, kiểm tra trùng)<br/>- Email (nếu có, kiểm tra trùng)<br/>- Password (nếu có, mã hóa)<br/>- Role (nếu có)<br/>- Activation status (nếu có)
    
    alt Cập nhật password
        UserService->>PasswordEncoder: encode(newPassword)
        PasswordEncoder-->>UserService: Hashed password
    end
    
    UserService->>Repository: save(user)
    Repository->>Database: UPDATE users SET<br/>username=?, email=?, password=?, role=?, is_activated=?
    Database-->>Repository: Updated
    Repository-->>UserService: User updated
    
    UserService-->>UserController: Response(success, UserResponse)
    UserController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
```

### 3. Xóa tài khoản (Delete User)

```mermaid
sequenceDiagram
    participant Admin as Admin
    participant Client as Client/Frontend
    participant UserController as UserManagementController
    participant UserService as UserManagementServiceImpl
    participant Repository as Repository<br/>(User)
    participant Database as Database

    Admin->>Client: Chọn user và click xóa
    Client->>UserController: DELETE /api/admin/users/{userId}
    
    UserController->>UserService: deleteUser(userId)
    
    UserService->>Repository: findById(userId)
    Repository->>Database: SELECT * FROM users WHERE id = ?
    Database-->>Repository: User
    Repository-->>UserService: User
    
    Note over UserService: Soft delete:<br/>Set is_deleted = true
    UserService->>UserService: user.setDeleted(true)
    UserService->>Repository: save(user)
    Repository->>Database: UPDATE users SET is_deleted = true WHERE id = ?
    Database-->>Repository: Updated
    Repository-->>UserService: User updated
    
    UserService-->>UserController: Response(success)
    UserController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
```

### 4. Xem danh sách tài khoản (Get Users)

```mermaid
sequenceDiagram
    participant Admin as Admin
    participant Client as Client/Frontend
    participant UserController as UserManagementController
    participant UserService as UserManagementServiceImpl
    participant Repository as Repository<br/>(User)
    participant Database as Database

    Admin->>Client: Truy cập trang quản lý tài khoản
    Client->>UserController: GET /api/admin/users<br/>(?role=ADMIN hoặc MANAGER)
    
    UserController->>UserService: getAllUsers() hoặc getUsersByRole(role)
    
    UserService->>Repository: findAll()
    Repository->>Database: SELECT * FROM users
    Database-->>Repository: List<User>
    Repository-->>UserService: List<User>
    
    Note over UserService: Lọc:<br/>- Chỉ lấy ADMIN và MANAGER<br/>- Loại bỏ user đã bị xóa
    UserService->>UserService: filter(!isDeleted && (role == ADMIN || role == MANAGER))
    UserService->>UserService: map to UserResponse
    
    UserService-->>UserController: Response(success, List<UserResponse>)
    UserController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị danh sách tài khoản
```

### 5. Xem chi tiết tài khoản (Get User By ID)

```mermaid
sequenceDiagram
    participant Admin as Admin
    participant Client as Client/Frontend
    participant UserController as UserManagementController
    participant UserService as UserManagementServiceImpl
    participant Repository as Repository<br/>(User)
    participant Database as Database

    Admin->>Client: Click xem chi tiết user
    Client->>UserController: GET /api/admin/users/{userId}
    
    UserController->>UserService: getUserById(userId)
    
    UserService->>Repository: findById(userId)
    Repository->>Database: SELECT * FROM users WHERE id = ?
    Database-->>Repository: User
    Repository-->>UserService: User
    
    Note over UserService: Kiểm tra user chưa bị xóa
    UserService->>UserService: toUserResponse(user)
    
    UserService-->>UserController: Response(success, UserResponse)
    UserController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông tin chi tiết user
```

## Các thành phần tham gia

1. **Admin**: Người quản trị thực hiện quản lý tài khoản
2. **Client/Frontend**: Giao diện người dùng
3. **UserManagementController**: Controller nhận request quản lý tài khoản
4. **UserManagementServiceImpl**: Service xử lý logic quản lý tài khoản
5. **Repository**: Repository truy cập database (UserRepository)
6. **Database**: Cơ sở dữ liệu
7. **PasswordEncoder**: Utility mã hóa mật khẩu (BCrypt)

## Các chức năng

### 1. Tạo tài khoản (Create User)
1. Admin nhập thông tin tài khoản (username, email, password, role)
2. Validate request (username, email, password không rỗng, password >= 6 ký tự, role = ADMIN/MANAGER)
3. Kiểm tra username và email chưa tồn tại
4. Mã hóa password bằng BCrypt
5. Tạo user mới với activated = true, deleted = false
6. Lưu vào database
7. Trả về thông tin user đã tạo

### 2. Cập nhật tài khoản (Update User)
1. Admin chọn user và cập nhật thông tin
2. Tìm user theo ID
3. Kiểm tra user chưa bị xóa
4. Cập nhật các trường (username, email, password, role, activation status)
5. Nếu cập nhật password: Mã hóa password mới
6. Nếu cập nhật username/email: Kiểm tra không trùng với user khác
7. Lưu vào database
8. Trả về thông tin user đã cập nhật

### 3. Xóa tài khoản (Delete User)
1. Admin chọn user và click xóa
2. Tìm user theo ID
3. Thực hiện soft delete (set is_deleted = true)
4. Lưu vào database
5. Trả về kết quả thành công

### 4. Xem danh sách tài khoản (Get Users)
1. Admin truy cập trang quản lý tài khoản
2. Lấy tất cả user từ database
3. Lọc chỉ lấy ADMIN và MANAGER, loại bỏ user đã bị xóa
4. Chuyển đổi sang UserResponse
5. Trả về danh sách user

### 5. Xem chi tiết tài khoản (Get User By ID)
1. Admin click xem chi tiết user
2. Tìm user theo ID
3. Kiểm tra user chưa bị xóa
4. Chuyển đổi sang UserResponse
5. Trả về thông tin chi tiết user

## Đặc điểm

- **Chỉ ADMIN có quyền**: Tất cả endpoint yêu cầu role ADMIN
- **Soft Delete**: Xóa tài khoản bằng cách đánh dấu is_deleted = true, không xóa thật
- **Password Security**: Mật khẩu được mã hóa bằng BCrypt trước khi lưu
- **Validation**: Kiểm tra username và email không trùng lặp
- **Role Restriction**: Chỉ có thể tạo và quản lý tài khoản ADMIN và MANAGER

