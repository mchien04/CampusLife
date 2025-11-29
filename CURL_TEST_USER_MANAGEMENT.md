# CURL Commands để Test Quản Lý Tài Khoản (Admin/Manager)

## LƯU Ý QUAN TRỌNG

1. **Thay thế tokens:**
   - `{ADMIN_TOKEN}` - Token của user có role ADMIN (bắt buộc cho tất cả các API này)

2. **Thay thế IDs:**
   - `{userId}` - ID của user cần quản lý

3. **Base URL:** `http://localhost:8080` (thay đổi nếu cần)

4. **Lấy Token:** Đăng nhập trước để lấy JWT token
   ```bash
   curl --location 'http://localhost:8080/api/auth/login' \
   --header 'Content-Type: application/json' \
   --data '{
     "username": "admin",
     "password": "password"
   }'
   ```

5. **Quyền truy cập:** Tất cả các API này chỉ dành cho **ADMIN** role.

6. **Kích hoạt tài khoản:** Khi admin tạo tài khoản ADMIN/MANAGER, tài khoản sẽ **tự động được kích hoạt** (`isActivated = true`) và **không cần xác nhận qua email**. User có thể đăng nhập ngay sau khi tạo.

---

## PHẦN 1: TẠO TÀI KHOẢN (CREATE USER)

### 1.1. Tạo tài khoản ADMIN

**API:** `POST /api/admin/users`

```bash
curl --location 'http://localhost:8080/api/admin/users' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "username": "admin2",
  "email": "admin2@example.com",
  "password": "password123",
  "role": "ADMIN"
}'
```

**Lưu ý:** Không cần truyền `isActivated`, tài khoản sẽ tự động được kích hoạt (`isActivated = true`) và có thể đăng nhập ngay.
```

**Response thành công:**
```json
{
  "status": true,
  "message": "User created successfully",
  "data": {
    "id": 1,
    "username": "admin2",
    "email": "admin2@example.com",
    "role": "ADMIN",
    "isActivated": true,
    "lastLogin": null,
    "createdAt": "2025-01-15T10:00:00",
    "updatedAt": "2025-01-15T10:00:00",
    "isDeleted": false
  }
}
```

### 1.2. Tạo tài khoản MANAGER

```bash
curl --location 'http://localhost:8080/api/admin/users' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "username": "manager1",
  "email": "manager1@example.com",
  "password": "password123",
  "role": "MANAGER"
}'
```

**Lưu ý:** Tài khoản sẽ tự động được kích hoạt, có thể đăng nhập ngay sau khi tạo.
```

**Lưu ý:**
- `username`: Bắt buộc, phải unique
- `email`: Bắt buộc, phải unique
- `password`: Bắt buộc, tối thiểu 6 ký tự
- `role`: Bắt buộc, chỉ được là `ADMIN` hoặc `MANAGER`
- `isActivated`: Tùy chọn, **mặc định `true`** (tài khoản được kích hoạt ngay, không cần xác nhận email)

**Response lỗi (username đã tồn tại):**
```json
{
  "status": false,
  "message": "Username already exists",
  "data": null
}
```

**Response lỗi (email đã tồn tại):**
```json
{
  "status": false,
  "message": "Email already exists",
  "data": null
}
```

**Response lỗi (role không hợp lệ):**
```json
{
  "status": false,
  "message": "Role must be ADMIN or MANAGER",
  "data": null
}
```

---

## PHẦN 2: CẬP NHẬT TÀI KHOẢN (UPDATE USER)

### 2.1. Cập nhật thông tin tài khoản

**API:** `PUT /api/admin/users/{userId}`

```bash
curl --location --request PUT 'http://localhost:8080/api/admin/users/{userId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "username": "admin2_updated",
  "email": "admin2_updated@example.com",
  "role": "MANAGER",
  "isActivated": false
}'
```

**Lưu ý:**
- Tất cả các trường đều tùy chọn (có thể cập nhật một phần)
- `password`: Nếu cung cấp, sẽ được hash và cập nhật. Nếu không cung cấp, mật khẩu giữ nguyên
- `username` và `email`: Nếu cung cấp, sẽ kiểm tra unique (trừ user hiện tại)

**Response thành công:**
```json
{
  "status": true,
  "message": "User updated successfully",
  "data": {
    "id": 1,
    "username": "admin2_updated",
    "email": "admin2_updated@example.com",
    "role": "MANAGER",
    "isActivated": false,
    "lastLogin": null,
    "createdAt": "2025-01-15T10:00:00",
    "updatedAt": "2025-01-15T10:30:00",
    "isDeleted": false
  }
}
```

### 2.2. Chỉ cập nhật mật khẩu

```bash
curl --location --request PUT 'http://localhost:8080/api/admin/users/{userId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "password": "newpassword123"
}'
```

### 2.3. Chỉ cập nhật trạng thái kích hoạt

```bash
curl --location --request PUT 'http://localhost:8080/api/admin/users/{userId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "isActivated": true
}'
```

**Response lỗi (user không tồn tại):**
```json
{
  "status": false,
  "message": "User not found",
  "data": null
}
```

**Response lỗi (username đã tồn tại):**
```json
{
  "status": false,
  "message": "Username already exists",
  "data": null
}
```

---

## PHẦN 3: XÓA TÀI KHOẢN (DELETE USER)

### 3.1. Xóa tài khoản (Soft Delete)

**API:** `DELETE /api/admin/users/{userId}`

```bash
curl --location --request DELETE 'http://localhost:8080/api/admin/users/{userId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response thành công:**
```json
{
  "status": true,
  "message": "User deleted successfully",
  "data": null
}
```

**Lưu ý:**
- Xóa mềm (soft delete): Chỉ set `isDeleted = true`
- User vẫn tồn tại trong database nhưng không còn active
- User đã xóa sẽ không xuất hiện trong danh sách users

**Response lỗi (user không tồn tại):**
```json
{
  "status": false,
  "message": "User not found",
  "data": null
}
```

**Response lỗi (user đã bị xóa):**
```json
{
  "status": false,
  "message": "User has already been deleted",
  "data": null
}
```

---

## PHẦN 4: XEM THÔNG TIN TÀI KHOẢN (GET USER)

### 4.1. Lấy thông tin một user theo ID

**API:** `GET /api/admin/users/{userId}`

```bash
curl --location 'http://localhost:8080/api/admin/users/{userId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response thành công:**
```json
{
  "status": true,
  "message": "User retrieved successfully",
  "data": {
    "id": 1,
    "username": "admin2",
    "email": "admin2@example.com",
    "role": "ADMIN",
    "isActivated": true,
    "lastLogin": "2025-01-15T11:00:00",
    "createdAt": "2025-01-15T10:00:00",
    "updatedAt": "2025-01-15T10:00:00",
    "isDeleted": false
  }
}
```

**Response lỗi (user không tồn tại):**
```json
{
  "status": false,
  "message": "User not found",
  "data": null
}
```

**Response lỗi (user đã bị xóa):**
```json
{
  "status": false,
  "message": "User has been deleted",
  "data": null
}
```

---

## PHẦN 5: LẤY DANH SÁCH TÀI KHOẢN (GET ALL USERS)

### 5.1. Lấy tất cả users (ADMIN và MANAGER)

**API:** `GET /api/admin/users`

```bash
curl --location 'http://localhost:8080/api/admin/users' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response thành công:**
```json
{
  "status": true,
  "message": "Users retrieved successfully",
  "data": [
    {
      "id": 1,
      "username": "admin1",
      "email": "admin1@example.com",
      "role": "ADMIN",
      "isActivated": true,
      "lastLogin": "2025-01-15T11:00:00",
      "createdAt": "2025-01-15T10:00:00",
      "updatedAt": "2025-01-15T10:00:00",
      "isDeleted": false
    },
    {
      "id": 2,
      "username": "manager1",
      "email": "manager1@example.com",
      "role": "MANAGER",
      "isActivated": true,
      "lastLogin": null,
      "createdAt": "2025-01-15T10:30:00",
      "updatedAt": "2025-01-15T10:30:00",
      "isDeleted": false
    }
  ]
}
```

**Lưu ý:**
- Chỉ trả về users có role `ADMIN` hoặc `MANAGER`
- Không trả về users đã bị xóa (`isDeleted = true`)
- Không trả về users có role `STUDENT`

### 5.2. Lấy users theo role

**API:** `GET /api/admin/users?role=ADMIN`

```bash
curl --location 'http://localhost:8080/api/admin/users?role=ADMIN' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Lấy tất cả ADMIN:**
```bash
curl --location 'http://localhost:8080/api/admin/users?role=ADMIN' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Lấy tất cả MANAGER:**
```bash
curl --location 'http://localhost:8080/api/admin/users?role=MANAGER' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response thành công (lọc theo role):**
```json
{
  "status": true,
  "message": "Users retrieved successfully",
  "data": [
    {
      "id": 1,
      "username": "admin1",
      "email": "admin1@example.com",
      "role": "ADMIN",
      "isActivated": true,
      "lastLogin": "2025-01-15T11:00:00",
      "createdAt": "2025-01-15T10:00:00",
      "updatedAt": "2025-01-15T10:00:00",
      "isDeleted": false
    },
    {
      "id": 3,
      "username": "admin2",
      "email": "admin2@example.com",
      "role": "ADMIN",
      "isActivated": true,
      "lastLogin": null,
      "createdAt": "2025-01-15T11:00:00",
      "updatedAt": "2025-01-15T11:00:00",
      "isDeleted": false
    }
  ]
}
```

**Response lỗi (role không hợp lệ):**
```json
{
  "status": false,
  "message": "Invalid role. Must be ADMIN or MANAGER",
  "data": null
}
```

---

## PHẦN 6: TEST FLOW HOÀN CHỈNH

### Flow 1: Tạo và quản lý tài khoản ADMIN

```bash
# 1. Tạo tài khoản ADMIN
curl --location 'http://localhost:8080/api/admin/users' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "username": "newadmin",
  "email": "newadmin@example.com",
  "password": "password123",
  "role": "ADMIN",
  "isActivated": true
}'
# → Lưu lại userId từ response

# 2. Xem thông tin user vừa tạo
curl --location 'http://localhost:8080/api/admin/users/{userId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'

# 3. Cập nhật thông tin user
curl --location --request PUT 'http://localhost:8080/api/admin/users/{userId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "email": "newadmin_updated@example.com",
  "isActivated": false
}'

# 4. Xem danh sách tất cả ADMIN
curl --location 'http://localhost:8080/api/admin/users?role=ADMIN' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'

# 5. Xóa user (nếu cần)
curl --location --request DELETE 'http://localhost:8080/api/admin/users/{userId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### Flow 2: Tạo và quản lý tài khoản MANAGER

```bash
# 1. Tạo tài khoản MANAGER
curl --location 'http://localhost:8080/api/admin/users' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "username": "newmanager",
  "email": "newmanager@example.com",
  "password": "password123",
  "role": "MANAGER",
  "isActivated": true
}'
# → Lưu lại userId từ response

# 2. Đăng nhập với tài khoản MANAGER mới tạo
curl --location 'http://localhost:8080/api/auth/login' \
--header 'Content-Type: application/json' \
--data '{
  "username": "newmanager",
  "password": "password123"
}'

# 3. Cập nhật mật khẩu cho MANAGER
curl --location --request PUT 'http://localhost:8080/api/admin/users/{userId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "password": "newpassword456"
}'

# 4. Xem danh sách tất cả MANAGER
curl --location 'http://localhost:8080/api/admin/users?role=MANAGER' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

---

## PHẦN 7: XỬ LÝ LỖI VÀ VALIDATION

### 7.1. Validation Errors

**Username trống:**
```json
{
  "status": false,
  "message": "Username is required",
  "data": null
}
```

**Email trống:**
```json
{
  "status": false,
  "message": "Email is required",
  "data": null
}
```

**Password trống:**
```json
{
  "status": false,
  "message": "Password is required",
  "data": null
}
```

**Password quá ngắn:**
```json
{
  "status": false,
  "message": "Password must be at least 6 characters long",
  "data": null
}
```

**Role không hợp lệ:**
```json
{
  "status": false,
  "message": "Role must be ADMIN or MANAGER",
  "data": null
}
```

### 7.2. Authorization Errors

**Không có token:**
```json
{
  "status": false,
  "message": "Unauthorized",
  "data": null
}
```

**Token không hợp lệ:**
```json
{
  "status": false,
  "message": "Invalid token",
  "data": null
}
```

**Không có quyền ADMIN:**
```json
{
  "status": false,
  "message": "Access denied. Admin role required.",
  "data": null
}
```

---

## TÓM TẮT CÁC API

| Method | Endpoint | Mô tả | Quyền |
|--------|----------|-------|-------|
| POST | `/api/admin/users` | Tạo tài khoản ADMIN/MANAGER | ADMIN |
| PUT | `/api/admin/users/{userId}` | Cập nhật thông tin user | ADMIN |
| DELETE | `/api/admin/users/{userId}` | Xóa user (soft delete) | ADMIN |
| GET | `/api/admin/users/{userId}` | Lấy thông tin một user | ADMIN |
| GET | `/api/admin/users` | Lấy tất cả users | ADMIN |
| GET | `/api/admin/users?role=ADMIN` | Lấy users theo role | ADMIN |
| GET | `/api/admin/users?role=MANAGER` | Lấy users theo role | ADMIN |

---

## LƯU Ý QUAN TRỌNG

1. **Chỉ ADMIN mới có quyền quản lý tài khoản:**
   - Tất cả các API này yêu cầu role `ADMIN`
   - MANAGER và STUDENT không thể truy cập

2. **Soft Delete:**
   - Xóa user chỉ set `isDeleted = true`
   - User đã xóa không xuất hiện trong danh sách
   - Có thể khôi phục bằng cách cập nhật `isDeleted = false` (nếu cần)

3. **Validation:**
   - Username và email phải unique
   - Password tối thiểu 6 ký tự
   - Role chỉ được là ADMIN hoặc MANAGER (không thể tạo STUDENT qua API này)

4. **Security:**
   - Mật khẩu được hash bằng BCrypt trước khi lưu
   - Không trả về mật khẩu trong response
   - Cần JWT token hợp lệ để truy cập

5. **Filtering:**
   - API `GET /api/admin/users` chỉ trả về ADMIN và MANAGER
   - Không trả về STUDENT và users đã bị xóa
   - Có thể lọc theo role bằng query parameter `?role=ADMIN` hoặc `?role=MANAGER`

