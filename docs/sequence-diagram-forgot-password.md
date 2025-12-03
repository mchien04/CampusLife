# Sequence Diagram - Chức năng Quên Mật Khẩu

## Mô tả
Sequence diagram mô tả luồng xử lý quên mật khẩu và đặt lại mật khẩu trong hệ thống CampusLife.

## Sequence Diagram

```mermaid
sequenceDiagram
    participant User as User
    participant Client as Client/Frontend
    participant AuthController as AuthController
    participant AuthService as AuthServiceImpl
    participant Repository as Repository<br/>(User, Token)
    participant Database as Database
    participant EmailUtil as EmailUtil

    Note over User,EmailUtil: Bước 1: Yêu cầu reset mật khẩu
    
    User->>Client: Nhập email và click "Quên mật khẩu"
    Client->>AuthController: POST /api/auth/forgot-password<br/>(email)
    
    AuthController->>AuthService: forgotPassword(email)
    
    AuthService->>Repository: findByEmail(email)
    Repository->>Database: SELECT * FROM users WHERE email = ?
    Database-->>Repository: User
    Repository-->>AuthService: User
    
    Note over AuthService: Nếu user tồn tại:<br/>- Tạo token reset (UUID)<br/>- Lưu token vào DB (hết hạn 1h)
    AuthService->>Repository: save(PasswordResetToken)
    Repository->>Database: INSERT INTO password_reset_tokens<br/>(user_id, token, expiry_date, used)
    Database-->>Repository: Token saved
    Repository-->>AuthService: Token saved
    
    AuthService->>EmailUtil: sendPasswordResetEmail(email, token)
    EmailUtil-->>AuthService: Email sent
    
    Note over AuthService: Luôn trả về success<br/>(kể cả khi email không tồn tại)
    AuthService-->>AuthController: Response(success)
    AuthController-->>Client: ResponseEntity.ok()
    Client-->>User: Hiển thị thông báo thành công
    
    Note over User,EmailUtil: Bước 2: Đặt lại mật khẩu
    
    User->>User: Click link trong email<br/>(/reset-password?token=...)
    User->>Client: Truy cập trang reset password
    Client->>User: Hiển thị form nhập mật khẩu mới
    
    User->>Client: Nhập mật khẩu mới và submit
    Client->>AuthController: POST /api/auth/reset-password<br/>(token, newPassword)
    
    AuthController->>AuthService: resetPassword(token, newPassword)
    
    AuthService->>Repository: findByTokenAndUsedFalse(token)
    Repository->>Database: SELECT * FROM password_reset_tokens<br/>WHERE token = ? AND used = false
    Database-->>Repository: PasswordResetToken
    Repository-->>AuthService: PasswordResetToken
    
    Note over AuthService: Validate token:<br/>- Token tồn tại<br/>- Chưa được sử dụng<br/>- Chưa hết hạn
    
    Note over AuthService: Cập nhật mật khẩu:<br/>- Mã hóa password (BCrypt)<br/>- Lưu vào DB<br/>- Đánh dấu token đã dùng
    AuthService->>Repository: save(user với password mới)
    Repository->>Database: UPDATE users SET password = ?
    Database-->>Repository: Updated
    AuthService->>Repository: save(token.setUsed(true))
    Repository->>Database: UPDATE password_reset_tokens SET used = true
    Database-->>Repository: Updated
    Repository-->>AuthService: Updated
    
    AuthService-->>AuthController: Response(success)
    AuthController-->>Client: ResponseEntity.ok()
    Client-->>User: Hiển thị thông báo thành công<br/>và redirect về login
```

## Các thành phần tham gia

1. **User**: Người dùng quên mật khẩu
2. **Client/Frontend**: Giao diện người dùng
3. **AuthController**: Controller nhận request
4. **AuthServiceImpl**: Service xử lý logic quên mật khẩu và reset mật khẩu
5. **Repository**: Repository truy cập database (UserRepository, PasswordResetTokenRepository)
6. **Database**: Cơ sở dữ liệu
7. **EmailUtil**: Utility gửi email

## Các bước xử lý

### Bước 1: Yêu cầu Reset Mật Khẩu (Forgot Password)

1. User nhập email và gửi request
2. Tìm user trong database theo email
3. Nếu user tồn tại: Tạo token reset (UUID), lưu vào database (hết hạn 1 giờ)
4. Gửi email chứa link reset mật khẩu
5. Luôn trả về success (kể cả khi email không tồn tại) để tránh email enumeration attack

### Bước 2: Đặt lại Mật Khẩu (Reset Password)

1. User click link trong email (chứa token)
2. Frontend hiển thị form nhập mật khẩu mới
3. User nhập mật khẩu mới và submit
4. Validate token: Kiểm tra token tồn tại, chưa được sử dụng, chưa hết hạn
5. Mã hóa mật khẩu mới (BCrypt) và cập nhật vào database
6. Đánh dấu token đã được sử dụng
7. Trả về kết quả thành công và redirect về trang login

## Đặc điểm bảo mật

- **Email Enumeration Prevention**: Luôn trả về success kể cả khi email không tồn tại
- **Token Security**: UUID ngẫu nhiên, chỉ dùng 1 lần, hết hạn sau 1 giờ
- **Password Security**: Mật khẩu tối thiểu 6 ký tự, được hash bằng BCrypt

