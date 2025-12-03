# Sequence Diagram - Chức năng Đăng nhập

## Mô tả
Sequence diagram mô tả luồng xử lý đăng nhập trong hệ thống CampusLife.

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Client as Client/Frontend
    participant AuthController as AuthController
    participant AuthService as AuthServiceImpl
    participant Repository as Repository<br/>(User)
    participant Database as Database
    participant JwtUtil as JwtUtil

    Client->>AuthController: POST /api/auth/login<br/>(username, password)
    
    AuthController->>AuthService: login(username, password)
    
    Note over AuthService: Validate và tìm user
    AuthService->>Repository: findByUsernameAndIsDeletedFalse(username)
    Repository->>Database: SELECT * FROM users WHERE username = ?
    Database-->>Repository: User
    Repository-->>AuthService: User
    
    Note over AuthService: Kiểm tra:<br/>- User tồn tại<br/>- Tài khoản đã kích hoạt<br/>- Mật khẩu đúng
    
    Note over AuthService: Cập nhật last login
    AuthService->>Repository: save(user với lastLogin)
    Repository->>Database: UPDATE users SET last_login = ?
    Database-->>Repository: Updated
    Repository-->>AuthService: User saved
    
    Note over AuthService: Tạo JWT token<br/>(chứa username, role, hết hạn 24h)
    AuthService->>JwtUtil: generateToken(userDetails)
    JwtUtil-->>AuthService: Token
    
    AuthService-->>AuthController: Response(success, token)
    AuthController-->>Client: ResponseEntity.ok()
    
    Note over Client: Lưu token vào local storage
    Client->>Client: Save token
```

## Các thành phần tham gia

1. **Client/Frontend**: Giao diện người dùng gửi yêu cầu đăng nhập
2. **AuthController**: Controller nhận request và trả về response
3. **AuthServiceImpl**: Service xử lý logic đăng nhập
4. **Repository**: Repository truy cập database (UserRepository)
5. **Database**: Cơ sở dữ liệu lưu trữ thông tin user
6. **JwtUtil**: Utility tạo và quản lý JWT token

## Các bước xử lý

1. Client gửi username và password
2. Tìm user trong database và validate (user tồn tại, đã kích hoạt, mật khẩu đúng)
3. Cập nhật thời gian đăng nhập cuối
4. Tạo JWT token (chứa username, role, hết hạn 24h)
5. Trả về token cho client
6. Client lưu token vào local storage

