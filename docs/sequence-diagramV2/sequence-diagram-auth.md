# Sequence Diagram - Nhóm chức năng Auth (Xác thực)

---

## 1. Login (Đăng nhập) — 3.3.1

```mermaid
sequenceDiagram
    actor User as Người dùng
    participant Client as Client/Frontend
    participant AuthController as AuthController
    participant AuthService as AuthService
    participant UserRepository as UserRepository
    participant Database as Database

    User->>Client: Nhập username và password
    Client->>AuthController: POST /api/auth/login<br/>(LoginRequest)
    Note over AuthController: Validate request body
    AuthController->>AuthService: authenticate(username, password)
    
    AuthService->>UserRepository: findByUsername(username)
    UserRepository->>Database: SELECT * FROM users WHERE username = ?
    Database-->>UserRepository: User (hoặc null nếu không tồn tại)
    UserRepository-->>AuthService: Optional<User>
    
    Note over AuthService: Kiểm tra user tồn tại<br/>Nếu không tồn tại → throw AuthenticationException
    Note over AuthService: Verify password (BCrypt)<br/>Nếu sai → throw BadCredentialsException
    Note over AuthService: Kiểm tra user đã active/verified chưa<br/>Nếu chưa → throw UserNotVerifiedException
    
    AuthService->>AuthService: generate JWT access token<br/>(expires: 15 phút)
    AuthService->>AuthService: generate Refresh token<br/>(expires: 7 ngày)
    Note over AuthService: Lưu refresh token vào DB (nếu có lưu)
    
    AuthService-->>AuthController: TokenPair(accessToken, refreshToken, tokenType="Bearer", expiresIn)
    AuthController-->>Client: 200 OK<br/>TokenPair JSON
    Client-->>User: Hiển thị "Đăng nhập thành công"<br/>Lưu token vào localStorage/cookie
    Note over Client: Redirect đến trang chủ theo role
```

---

## 2. Logout (Đăng xuất) — 3.3.2

```mermaid
sequenceDiagram
    actor User as Người dùng
    participant Client as Client/Frontend
    participant AuthController as AuthController
    participant TokenService as TokenService
    participant UserRepository as UserRepository
    participant Database as Database

    User->>Client: Click "Đăng xuất"
    Client->>Client: Lấy access token từ localStorage
    Client->>AuthController: POST /api/auth/logout<br/>Header: Authorization: Bearer {accessToken}
    Note over AuthController: Extract token từ header
    AuthController->>TokenService: blacklistToken(token)
    
    Note over TokenService: Kiểm tra token hợp lệ<br/>Giải mã JWT để lấy jti (token id) và expiry
    TokenService->>Database: INSERT INTO token_blacklist (jti, expiry, created_at)
    Note over TokenService: Lưu jti vào blacklist (Redis/DB)<br/>để block token cho đến khi hết hạn
    TokenService-->>AuthController: Token blacklisted
    
    Note over AuthController: Xóa refresh token (nếu có lưu server-side)
    AuthController->>UserRepository: clearRefreshToken(userId)
    UserRepository->>Database: UPDATE users SET refresh_token = NULL WHERE id = ?
    Database-->>UserRepository: Updated
    UserRepository-->>AuthController: Done
    
    AuthController-->>Client: 200 OK<br/>{ "message": "Logout successful" }
    Client->>Client: Xóa token khỏi localStorage/cookie
    Client-->>User: Hiển thị "Đăng xuất thành công"<br/>Redirect về trang login
```

---

## 3. Đổi mật khẩu — 3.3.3

```mermaid
sequenceDiagram
    actor User as Người dùng
    participant Client as Client/Frontend
    participant AuthController as AuthController
    participant AuthService as AuthService
    participant UserRepository as UserRepository
    participant Database as Database

    User->>Client: Nhập mật khẩu cũ, mật khẩu mới, xác nhận mật khẩu mới
    Client->>Client: Validate new password match<br/>Validate password policy (độ dài, ký tự...)
    Client->>AuthController: POST /api/auth/change-password<br/>Header: Bearer {token}<br/>ChangePasswordRequest
    Note over AuthController: Extract userId từ JWT token
    AuthController->>AuthService: changePassword(userId, oldPassword, newPassword)
    
    AuthService->>UserRepository: findById(userId)
    UserRepository->>Database: SELECT * FROM users WHERE id = ?
    Database-->>UserRepository: User
    UserRepository-->>AuthService: User
    
    Note over AuthService: Verify old password (BCrypt)<br/>Nếu sai → throw InvalidPasswordException
    Note over AuthService: Kiểm tra newPassword ≠ oldPassword<br/>Nếu trùng → throw SamePasswordException
    
    AuthService->>AuthService: encode newPassword (BCrypt)
    AuthService->>UserRepository: updatePassword(userId, encodedNewPassword)
    UserRepository->>Database: UPDATE users SET password = ?, updated_at = NOW() WHERE id = ?
    Database-->>UserRepository: Rows updated
    UserRepository-->>AuthService: Updated
    
    AuthService-->>AuthController: Password changed
    AuthController-->>Client: 200 OK<br/>{ "message": "Password changed successfully" }
    Client-->>User: Hiển thị "Đổi mật khẩu thành công"
    Note over Client: Optional: Tự động logout yêu cầu đăng nhập lại
```

---

## 4. Đăng ký tài khoản (Self-register) — 3.3.4

```mermaid
sequenceDiagram
    actor User as Người dùng
    participant Client as Client/Frontend
    participant AuthController as AuthController
    participant AuthService as AuthService
    participant UserRepository as UserRepository
    participant Database as Database
    participant EmailService as EmailService

    User->>Client: Nhập thông tin đăng ký<br/>(email, username, password, fullName)
    Client->>Client: Validate form (email format, password strength, username rules)
    Client->>AuthController: POST /api/auth/register<br/>(RegisterRequest)
    Note over AuthController: Validate DTO (javax.validation)
    AuthController->>AuthService: register(registerRequest)
    
    AuthService->>UserRepository: existsByUsername(username)
    UserRepository->>Database: SELECT COUNT(*) FROM users WHERE username = ?
    Database-->>UserRepository: count
    UserRepository-->>AuthService: boolean
    Note over AuthService: Nếu username đã tồn tại → throw UsernameAlreadyExistsException
    
    AuthService->>UserRepository: existsByEmail(email)
    UserRepository->>Database: SELECT COUNT(*) FROM users WHERE email = ?
    Database-->>UserRepository: count
    UserRepository-->>AuthService: boolean
    Note over AuthService: Nếu email đã tồn tại → throw EmailAlreadyExistsException
    
    AuthService->>AuthService: encodePassword(password) (BCrypt)
    AuthService->>AuthService: generateVerificationToken()
    
    AuthService->>UserRepository: save(newUser)
    Note over UserRepository: Tạo User entity:<br/>- username, email, fullName<br/>- password (encoded)<br/>- role = PENDING hoặc USER<br/>- verified = false<br/>- verificationToken = UUID
    UserRepository->>Database: INSERT INTO users (...)
    Database-->>UserRepository: User (with generated id)
    UserRepository-->>AuthService: Saved User
    
    AuthService->>EmailService: sendVerificationEmail(email, fullName, verificationToken)
    Note over EmailService: Tạo nội dung email HTML<br/>Link: /api/auth/verify?token={verificationToken}
    EmailService-->>AuthService: Email sent
    AuthService-->>AuthController: RegistrationResult(userId, emailSent=true)
    
    AuthController-->>Client: 201 Created<br/>{ "message": "Registration successful. Please check your email to verify." }
    Client-->>User: Hiển thị "Đăng ký thành công. Vui lòng kiểm tra email để xác thực."
```

---

## 5. Quên mật khẩu — A.2

```mermaid
sequenceDiagram
    actor User as Người dùng
    participant Client as Client/Frontend
    participant AuthController as AuthController
    participant AuthService as AuthService
    participant UserRepository as UserRepository
    participant Database as Database
    participant EmailService as EmailService

    User->>Client: Nhập email đã đăng ký
    Client->>Client: Validate email format
    Client->>AuthController: POST /api/auth/forgot-password<br/>{ "email": "user@example.com" }
    Note over AuthController: Validate email string
    AuthController->>AuthService: forgotPassword(email)
    
    AuthService->>UserRepository: findByEmail(email)
    UserRepository->>Database: SELECT * FROM users WHERE email = ?
    Database-->>UserRepository: User (hoặc null)
    UserRepository-->>AuthService: Optional<User>
    
    Note over AuthService: Luôn trả về success message<br/>dù email có tồn tại hay không (bảo mật)
    alt Email tồn tại
        Note over AuthService: Kiểm tra user active/verified
        AuthService->>AuthService: generatePasswordResetToken()<br/>(UUID hoặc JWT với expiry 1 giờ)
        AuthService->>UserRepository: saveResetToken(userId, resetToken, expiry)
        UserRepository->>Database: UPDATE users SET reset_token = ?, reset_token_expiry = ? WHERE id = ?
        Database-->>UserRepository: Updated
        UserRepository-->>AuthService: Done
        
        AuthService->>EmailService: sendPasswordResetEmail(email, fullName, resetToken)
        Note over EmailService: Tạo email chứa link reset<br/>Link: /reset-password?token={resetToken}
        EmailService-->>AuthService: Email sent
    else Email không tồn tại
        Note over AuthService: Không làm gì thêm<br/>Trả về success để tránh leak thông tin
    end
    
    AuthService-->>AuthController: ForgotPasswordResult(success)
    AuthController-->>Client: 200 OK<br/>{ "message": "If your email exists, a password reset link has been sent." }
    Client-->>User: Hiển thị "Nếu email tồn tại, bạn sẽ nhận được link đặt lại mật khẩu."
```

---

## 6. Xác thực tài khoản (Verify Email) — A.3

```mermaid
sequenceDiagram
    actor User as Người dùng
    participant Client as Client/Frontend
    participant AuthController as AuthController
    participant AuthService as AuthService
    participant UserRepository as UserRepository
    participant Database as Database

    User->>Client: Click link xác thực trong email<br/>URL: /api/auth/verify?token={verificationToken}
    Client->>AuthController: GET /api/auth/verify?token={verificationToken}
    Note over AuthController: Extract token từ query param
    AuthController->>AuthService: verifyAccount(token)
    
    AuthService->>UserRepository: findByVerificationToken(token)
    UserRepository->>Database: SELECT * FROM users WHERE verification_token = ?
    Database-->>UserRepository: User (hoặc null)
    UserRepository-->>AuthService: Optional<User>
    
    Note over AuthService: Nếu token không tồn tại → throw InvalidTokenException
    Note over AuthService: Kiểm tra token đã hết hạn chưa<br/>Nếu hết hạn → throw TokenExpiredException
    Note over AuthService: Kiểm tra user đã verified chưa<br/>Nếu đã verified → throw AlreadyVerifiedException
    
    AuthService->>UserRepository: activateUser(userId)
    Note over UserRepository: Cập nhật User:<br/>- verified = true<br/>- verification_token = NULL<br/>- role = USER (nếu đang là PENDING)<br/>- updated_at = NOW()
    UserRepository->>Database: UPDATE users SET verified = true, verification_token = NULL, role = 'USER', updated_at = NOW() WHERE id = ?
    Database-->>UserRepository: Rows updated
    UserRepository-->>AuthService: Activated
    
    AuthService-->>AuthController: Account verified successfully
    AuthController-->>Client: 200 OK<br/>{ "message": "Account verified successfully. You can now login." }
    Client-->>User: Hiển thị "Xác thực tài khoản thành công!"<br/>Redirect đến trang login
```

---

## 7. Tạo tài khoản bởi Admin — 3.3.18

```mermaid
sequenceDiagram
    actor Admin as Admin
    participant Client as Client/Frontend
    participant AdminUserController as AdminUserController
    participant AdminUserService as AdminUserService
    participant UserRepository as UserRepository
    participant StudentRepository as StudentRepository
    participant StaffRepository as StaffRepository
    participant Database as Database
    participant EmailService as EmailService

    Admin->>Client: Nhập thông tin user mới<br/>(email, username, fullName, role, department/class...)
    Client->>Client: Validate form<br/>Kiểm tra quyền Admin
    Client->>AdminUserController: POST /api/admin/users<br/>Header: Bearer {adminToken}<br/>(CreateUserRequest)
    Note over AdminUserController: @PreAuthorize("hasRole('ADMIN')")<br/>Validate DTO
    AdminUserController->>AdminUserService: createUserByAdmin(createUserRequest)
    
    AdminUserService->>UserRepository: existsByUsername(username)
    UserRepository->>Database: SELECT COUNT(*) FROM users WHERE username = ?
    Database-->>UserRepository: count
    UserRepository-->>AdminUserService: boolean
    Note over AdminUserService: Nếu username đã tồn tại → throw UsernameAlreadyExistsException
    
    AdminUserService->>UserRepository: existsByEmail(email)
    UserRepository->>Database: SELECT COUNT(*) FROM users WHERE email = ?
    Database-->>UserRepository: count
    UserRepository-->>AdminUserService: boolean
    Note over AdminUserService: Nếu email đã tồn tại → throw EmailAlreadyExistsException
    
    AdminUserService->>AdminUserService: generateTemporaryPassword()<br/>encodePassword(tempPassword) (BCrypt)
    
    AdminUserService->>UserRepository: save(newUser)
    Note over UserRepository: Tạo User entity:<br/>- username, email, fullName<br/>- password (encoded temp)<br/>- role = {specifiedRole} (STUDENT/STAFF/ADMIN...)<br/>- verified = true<br/>- created_by = adminId
    UserRepository->>Database: INSERT INTO users (...)
    Database-->>UserRepository: User (with generated id)
    UserRepository-->>AdminUserService: Saved User
    
    alt Role = STUDENT
        AdminUserService->>StudentRepository: save(new StudentProfile)<br/>Link user_id với class, major, studentCode
        StudentRepository->>Database: INSERT INTO student_profiles (...)
        Database-->>StudentRepository: StudentProfile
        StudentRepository-->>AdminUserService: Saved Profile
    else Role = STAFF hoặc TEACHER
        AdminUserService->>StaffRepository: save(new StaffProfile)<br/>Link user_id với department, position
        StaffRepository->>Database: INSERT INTO staff_profiles (...)
        Database-->>StaffRepository: StaffProfile
        StaffRepository-->>AdminUserService: Saved Profile
    end
    
    AdminUserService->>EmailService: sendAccountCredentialsEmail(email, fullName, username, tempPassword)
    Note over EmailService: Tạo email HTML chứa:<br/>- Username<br/>- Temporary password<br/>- Link đổi mật khẩu lần đầu<br/>- Hướng dẫn đăng nhập
    EmailService-->>AdminUserService: Email sent
    
    AdminUserService-->>AdminUserController: CreatedUserResult(userId, role, emailSent=true)
    AdminUserController-->>Client: 201 Created<br/>{ "message": "User created successfully. Credentials sent to email." }
    Client-->>Admin: Hiển thị "Tạo tài khoản thành công. Thông tin đăng nhập đã gửi qua email."
```

---

## Các thành phần tham gia

| Thành phần | Vai trò |
|------------|---------|
| **Actor** | Người dùng cuối (User) hoặc Admin thực hiện thao tác |
| **Client/Frontend** | Ứng dụng React, thu thập input, gọi API, hiển thị phản hồi |
| **AuthController** | REST Controller nhận request auth, validate input, gọi Service |
| **AdminUserController** | REST Controller dành cho Admin quản lý user |
| **AuthService** | Business logic xác thực: đăng nhập, đăng xuất, đổi mật khẩu, đăng ký, quên mật khẩu, verify |
| **AdminUserService** | Business logic quản lý user dành cho Admin: tạo user, gán role, tạo profile liên quan |
| **TokenService** | Quản lý token: blacklist JWT, validate token, xóa refresh token |
| **UserRepository** | Tầng truy cập dữ liệu cho entity User (Spring Data JPA) |
| **StudentRepository** | Tầng truy cập dữ liệu cho entity StudentProfile |
| **StaffRepository** | Tầng truy cập dữ liệu cho entity StaffProfile |
| **Database** | Hệ quản trị CSDL (MySQL/PostgreSQL) lưu trữ dữ liệu |
| **EmailService** | Gửi email thông báo (xác thực, reset password, credentials) |

---

## Các chức năng

| STT | Chức năng | Mã tham chiếu | Mô tả ngắn |
|-----|-----------|---------------|------------|
| 1 | **Login (Đăng nhập)** | 3.3.1 | Xác thực username/password, trả về JWT + refresh token |
| 2 | **Logout (Đăng xuất)** | 3.3.2 | Blacklist token, xóa refresh token, đăng xuất user |
| 3 | **Đổi mật khẩu** | 3.3.3 | Xác minh mật khẩu cũ, cập nhật mật khẩu mới (BCrypt) |
| 4 | **Đăng ký tài khoản (Self-register)** | 3.3.4 | Tạo tài khoản mới, kiểm tra trùng lặp, gửi email xác thực |
| 5 | **Quên mật khẩu** | A.2 | Tạo reset token, gửi email link đặt lại mật khẩu |
| 6 | **Xác thực tài khoản (Verify Email)** | A.3 | Kích hoạt tài khoản qua link email, set verified=true |
| 7 | **Tạo tài khoản bởi Admin** | 3.3.18 | Admin tạo user với role cụ thể, tạo profile liên quan, gửi email credentials |

---

## Ghi chú kỹ thuật

- **Security**: Mật khẩu luôn được mã hóa bằng **BCrypt** trước khi lưu vào Database.
- **JWT Token**: Access token có thời hạn ngắn (15 phút), refresh token có thời hạn dài hơn (7 ngày).
- **Token Blacklist**: Khi logout, JWT token được đưa vào blacklist (Redis/Database) để vô hiệu hóa ngay lập tức.
- **Email Verification**: Tài khoản tự đăng ký có `verified=false` và `role=PENDING` cho đến khi click link xác thực.
- **Admin Create User**: Tài khoản được Admin tạo có `verified=true` ngay từ đầu, mật khẩu tạm thời được gửi qua email.
- **Role-based Profile**: Khi Admin tạo user với role STUDENT/STAFF, hệ thống tự động tạo record tương ứng trong bảng `student_profiles` hoặc `staff_profiles`.
- **Bảo mật Forgot Password**: API luôn trả về thông báo chung chung dù email có tồn tại hay không, tránh lộ thông tin người dùng.
