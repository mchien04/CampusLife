# CURL Commands để Test Chức Năng Reset Mật Khẩu

## LƯU Ý QUAN TRỌNG

1. **Base URL:** `http://localhost:8080` (thay đổi nếu cần)
2. **Frontend URL:** `http://localhost:3000` (theo cấu hình trong `application.properties`)
3. **Các endpoint này KHÔNG cần authentication** (public endpoints)

---

## FLOW HOÀN CHỈNH

### Bước 1: User yêu cầu reset mật khẩu (Forgot Password)

**API:** `POST /api/auth/forgot-password`

**Mô tả:** User nhập email, hệ thống sẽ gửi email chứa link reset mật khẩu (nếu email tồn tại).

**Request:**
```bash
curl --location 'http://localhost:8080/api/auth/forgot-password' \
--header 'Content-Type: application/json' \
--data '{
  "email": "user@example.com"
}'
```

**Response khi thành công:**
```json
{
  "status": true,
  "message": "If an account with that email exists, a password reset link has been sent.",
  "data": null
}
```

**Lưu ý:**
- ✅ Luôn trả về success message (kể cả khi email không tồn tại) để tránh email enumeration attacks
- ✅ Nếu email tồn tại, hệ thống sẽ:
  - Tạo token reset mật khẩu (UUID)
  - Lưu token vào database với thời gian hết hạn là 1 giờ
  - Gửi email chứa link reset: `{frontendUrl}/reset-password?token={token}`
- ✅ Nếu email không tồn tại, chỉ trả về success message (không gửi email)

---

### Bước 2: User click vào link trong email

**Link trong email sẽ có dạng:**
```
http://localhost:3000/reset-password?token=abc123-def456-ghi789-...
```

**Frontend sẽ:**
1. Lấy token từ URL parameter
2. Hiển thị form nhập mật khẩu mới
3. Gọi API `POST /api/auth/reset-password` với token và mật khẩu mới

---

### Bước 3: User đổi mật khẩu (Reset Password)

**API:** `POST /api/auth/reset-password`

**Mô tả:** User nhập token và mật khẩu mới, hệ thống sẽ cập nhật mật khẩu.

**Request:**
```bash
curl --location 'http://localhost:8080/api/auth/reset-password' \
--header 'Content-Type: application/json' \
--data '{
  "token": "abc123-def456-ghi789-...",
  "newPassword": "NewPassword123!"
}'
```

**Response khi thành công:**
```json
{
  "status": true,
  "message": "Password reset successfully. You can now login with your new password.",
  "data": null
}
```

**Response khi token không hợp lệ:**
```json
{
  "status": false,
  "message": "Invalid or used token",
  "data": null
}
```

**Response khi token đã hết hạn:**
```json
{
  "status": false,
  "message": "Token has expired. Please request a new password reset.",
  "data": null
}
```

**Response khi mật khẩu không hợp lệ:**
```json
{
  "status": false,
  "message": "Password must be at least 6 characters long",
  "data": null
}
```

**Lưu ý:**
- ✅ Token chỉ có thể sử dụng 1 lần (sau khi reset thành công, token sẽ bị đánh dấu `used = true`)
- ✅ Token có thời gian hết hạn là 1 giờ
- ✅ Mật khẩu mới phải có ít nhất 6 ký tự
- ✅ Sau khi reset thành công, user có thể đăng nhập với mật khẩu mới

---

## TEST SCENARIOS

### Scenario 1: Reset mật khẩu thành công

```bash
# Bước 1: Yêu cầu reset mật khẩu
curl --location 'http://localhost:8080/api/auth/forgot-password' \
--header 'Content-Type: application/json' \
--data '{
  "email": "student@example.com"
}'

# → Kiểm tra email, copy token từ link

# Bước 2: Reset mật khẩu với token
curl --location 'http://localhost:8080/api/auth/reset-password' \
--header 'Content-Type: application/json' \
--data '{
  "token": "PASTE_TOKEN_HERE",
  "newPassword": "NewPassword123!"
}'

# Bước 3: Đăng nhập với mật khẩu mới
curl --location 'http://localhost:8080/api/auth/login' \
--header 'Content-Type: application/json' \
--data '{
  "username": "student",
  "password": "NewPassword123!"
}'
```

### Scenario 2: Token đã hết hạn

```bash
# Giả sử token đã hết hạn (sau 1 giờ)
curl --location 'http://localhost:8080/api/auth/reset-password' \
--header 'Content-Type: application/json' \
--data '{
  "token": "EXPIRED_TOKEN",
  "newPassword": "NewPassword123!"
}'

# Response: "Token has expired. Please request a new password reset."
```

### Scenario 3: Token đã được sử dụng

```bash
# Sau khi reset thành công, token bị đánh dấu used = true
# Nếu cố gắng dùng lại token đó:
curl --location 'http://localhost:8080/api/auth/reset-password' \
--header 'Content-Type: application/json' \
--data '{
  "token": "ALREADY_USED_TOKEN",
  "newPassword": "NewPassword123!"
}'

# Response: "Invalid or used token"
```

### Scenario 4: Email không tồn tại

```bash
# Gửi request với email không tồn tại
curl --location 'http://localhost:8080/api/auth/forgot-password' \
--header 'Content-Type: application/json' \
--data '{
  "email": "nonexistent@example.com"
}'

# Response: Vẫn trả về success (để tránh email enumeration)
# Không có email nào được gửi
```

### Scenario 5: Mật khẩu quá ngắn

```bash
curl --location 'http://localhost:8080/api/auth/reset-password' \
--header 'Content-Type: application/json' \
--data '{
  "token": "VALID_TOKEN",
  "newPassword": "123"
}'

# Response: "Password must be at least 6 characters long"
```

---

## CẤU TRÚC DATABASE

### Bảng `password_reset_tokens`:

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key |
| `user_id` | BIGINT | Foreign key to `users.id` |
| `token` | VARCHAR(255) | Unique token (UUID) |
| `expiry_date` | DATETIME | Thời gian hết hạn (1 giờ sau khi tạo) |
| `used` | BOOLEAN | Đã sử dụng chưa (default: false) |
| `created_at` | DATETIME | Thời gian tạo token |

**Indexes:**
- `idx_token` trên `token`
- `idx_user_id` trên `user_id`
- `idx_expiry_date` trên `expiry_date`

---

## EMAIL TEMPLATE

Email reset mật khẩu sẽ có nội dung:

**Subject:** `Reset Your CampusLife Password`

**Body (HTML):**
```html
<h3>Password Reset Request</h3>
<p>You have requested to reset your password. Please click the link below to reset your password:</p>
<a href="http://localhost:3000/reset-password?token=abc123...">Reset Password</a>
<p>If you did not request this, please ignore this email.</p>
<p>This link will expire in 1 hour.</p>
<p>For security reasons, please do not share this link with anyone.</p>
```

---

## BẢO MẬT

### Các biện pháp bảo mật đã implement:

1. ✅ **Email Enumeration Prevention:**
   - Luôn trả về success message kể cả khi email không tồn tại
   - Không tiết lộ thông tin về việc email có tồn tại hay không

2. ✅ **Token Security:**
   - Token là UUID ngẫu nhiên (khó đoán)
   - Token chỉ có thể sử dụng 1 lần
   - Token có thời gian hết hạn (1 giờ)

3. ✅ **Password Validation:**
   - Mật khẩu mới phải có ít nhất 6 ký tự
   - Mật khẩu được hash bằng BCrypt trước khi lưu

4. ✅ **Token Cleanup:**
   - Có thể tạo scheduled task để xóa các token đã hết hạn (tùy chọn)

---

## TÓM TẮT API

| Method | Endpoint | Mô tả | Authentication |
|--------|----------|-------|----------------|
| POST | `/api/auth/forgot-password` | Yêu cầu reset mật khẩu | Không cần |
| POST | `/api/auth/reset-password` | Đổi mật khẩu với token | Không cần |

---

## LƯU Ý

1. **Frontend Integration:**
   - Frontend cần tạo trang `/reset-password` để hiển thị form nhập mật khẩu mới
   - Lấy token từ URL parameter: `?token=...`
   - Gọi API `POST /api/auth/reset-password` khi user submit form

2. **Email Configuration:**
   - Đảm bảo cấu hình email trong `application.properties` đúng
   - `app.frontend-url` phải trỏ đến frontend URL để tạo link reset

3. **Token Expiry:**
   - Token có thời gian hết hạn là 1 giờ
   - User cần request lại nếu token hết hạn

4. **Multiple Requests:**
   - Nếu user request reset nhiều lần, token cũ sẽ bị invalidate
   - Chỉ token mới nhất mới có hiệu lực

