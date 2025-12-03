# Hướng Dẫn API Quản Lý Tài Khoản Sinh Viên (Excel Import)

## Tổng Quan

Hệ thống quản lý tài khoản sinh viên cho phép Admin:
1. **Upload file Excel** để import danh sách sinh viên
2. **Tạo tài khoản hàng loạt** từ danh sách đã parse
3. **Review và chỉnh sửa** thông tin tài khoản trước khi gửi email
4. **Gửi email credentials** (username và password) cho từng sinh viên hoặc hàng loạt

---

## Lưu Ý Quan Trọng

1. **Thay thế tokens:**
   - `{ADMIN_TOKEN}` - Token của user có role ADMIN

2. **Thay thế IDs:**
   - `{studentId}` - ID của student

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

5. **Tất cả endpoints yêu cầu role ADMIN**

---

## Luồng Hoạt Động

### Flow 1: Tạo Tài Khoản Từ Excel

```
1. Admin upload file Excel
   ↓
2. Hệ thống parse và validate dữ liệu
   ↓
3. Admin review danh sách hợp lệ/không hợp lệ
   ↓
4. Admin chọn các dòng hợp lệ để tạo tài khoản
   ↓
5. Hệ thống tạo User và Student, khởi tạo điểm
   ↓
6. Admin review danh sách tài khoản đã tạo
   ↓
7. Admin chỉnh sửa (nếu cần) và gửi email credentials
```

### Flow 2: Gửi Email Credentials

```
1. Admin xem danh sách tài khoản đã tạo
   ↓
2. Admin chọn sinh viên cần gửi email
   ↓
3. Hệ thống tạo password mới và gửi email
   ↓
4. Sinh viên nhận email với username và password
```

---

## Format File Excel

### Cấu Trúc File

File Excel (.xlsx hoặc .xls) có **3 cột** theo thứ tự:

| Cột A | Cột B | Cột C |
|-------|-------|-------|
| Mã số sinh viên | Họ tên | Email |

### Ví Dụ File Excel

**Có Header:**
```
| Mã số sinh viên | Họ tên          | Email                    |
|----------------|-----------------|--------------------------|
| SV001          | Nguyễn Văn A    | sv001@example.com        |
| SV002          | Trần Thị B       | sv002@example.com        |
| SV003          | Lê Văn C         | sv003@example.com        |
```

**Không có Header:**
```
| SV001 | Nguyễn Văn A | sv001@example.com |
| SV002 | Trần Thị B   | sv002@example.com |
| SV003 | Lê Văn C     | sv003@example.com |
```

### Lưu Ý

- Hệ thống tự động detect header (nếu dòng đầu tiên chứa các từ khóa: "mã", "họ tên", "email", "mssv", "student code", "full name", "tên")
- Nếu có header, dòng đầu tiên sẽ được bỏ qua
- Nếu không có header, tất cả dòng đều được coi là dữ liệu
- Các cột có thể ở bất kỳ vị trí nào (hệ thống tự động tìm theo tên cột)

---

## API Endpoints

### 1. Upload và Parse File Excel

**Endpoint:** `POST /api/admin/students/upload-excel`

**Mô tả:** Upload file Excel và parse để lấy danh sách sinh viên. Hệ thống sẽ validate và phân loại các dòng hợp lệ/không hợp lệ.

**Request:**
```bash
curl --location 'http://localhost:8080/api/admin/students/upload-excel' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--form 'file=@"/path/to/students.xlsx"'
```

**Response thành công:**
```json
{
  "status": true,
  "message": "Excel parsed successfully",
  "data": {
    "totalRows": 5,
    "validRows": [
      {
        "studentCode": "SV001",
        "fullName": "Nguyễn Văn A",
        "email": "sv001@example.com"
      },
      {
        "studentCode": "SV002",
        "fullName": "Trần Thị B",
        "email": "sv002@example.com"
      }
    ],
    "invalidRows": [
      {
        "studentCode": "SV003",
        "fullName": "",
        "email": "sv003@example.com"
      }
    ],
    "errors": {
      "3": "Họ tên không được để trống"
    }
  }
}
```

**Response lỗi:**
```json
{
  "status": false,
  "message": "File must be Excel format (.xlsx or .xls)",
  "data": null
}
```

---

### 2. Tạo Tài Khoản Hàng Loạt

**Endpoint:** `POST /api/admin/students/bulk-create`

**Mô tả:** Tạo tài khoản User và Student từ danh sách sinh viên đã parse. Mỗi tài khoản sẽ có:
- Username = mã số sinh viên
- Email = email từ Excel
- Password = password ngẫu nhiên (8-12 ký tự)
- Role = STUDENT
- isActivated = true (không cần email confirmation)
- Student.fullName = họ tên từ Excel
- Student.studentCode = mã số sinh viên
- Tự động khởi tạo điểm cho semester hiện tại

**Request:**
```bash
curl --location 'http://localhost:8080/api/admin/students/bulk-create' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "students": [
    {
      "studentCode": "SV001",
      "fullName": "Nguyễn Văn A",
      "email": "sv001@example.com"
    },
    {
      "studentCode": "SV002",
      "fullName": "Trần Thị B",
      "email": "sv002@example.com"
    }
  ]
}'
```

**Response thành công:**
```json
{
  "status": true,
  "message": "Created 2 accounts successfully. 0 errors.",
  "data": {
    "createdAccounts": [
      {
        "userId": 1,
        "studentId": 1,
        "username": "SV001",
        "email": "sv001@example.com",
        "studentCode": "SV001",
        "fullName": "Nguyễn Văn A",
        "password": "aB3xY9mK2p",
        "isActivated": true,
        "emailSent": false,
        "createdAt": "2025-12-04T10:00:00"
      },
      {
        "userId": 2,
        "studentId": 2,
        "username": "SV002",
        "email": "sv002@example.com",
        "studentCode": "SV002",
        "fullName": "Trần Thị B",
        "password": "mN8pQ4rT7w",
        "isActivated": true,
        "emailSent": false,
        "createdAt": "2025-12-04T10:00:01"
      }
    ],
    "errors": [],
    "successCount": 2,
    "errorCount": 0
  }
}
```

**Response có lỗi:**
```json
{
  "status": true,
  "message": "Created 1 accounts successfully. 1 errors.",
  "data": {
    "createdAccounts": [
      {
        "userId": 1,
        "studentId": 1,
        "username": "SV001",
        "email": "sv001@example.com",
        "studentCode": "SV001",
        "fullName": "Nguyễn Văn A",
        "password": "aB3xY9mK2p",
        "isActivated": true,
        "emailSent": false,
        "createdAt": "2025-12-04T10:00:00"
      }
    ],
    "errors": [
      "Dòng SV002: Email đã tồn tại"
    ],
    "successCount": 1,
    "errorCount": 1
  }
}
```

**Lưu ý:**
- Password được trả về trong response để admin có thể review (chỉ hiển thị lần đầu)
- Các tài khoản đã tạo sẽ tự động có `isActivated = true`
- Hệ thống tự động khởi tạo 3 loại điểm (REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE) cho semester hiện tại

---

### 3. Lấy Danh Sách Tài Khoản Chờ Review

**Endpoint:** `GET /api/admin/students/pending`

**Mô tả:** Lấy danh sách tất cả tài khoản sinh viên đã tạo (chưa bị xóa), sắp xếp theo thời gian tạo (mới nhất trước).

**Request:**
```bash
curl --location 'http://localhost:8080/api/admin/students/pending' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Pending accounts retrieved successfully",
  "data": [
    {
      "userId": 1,
      "studentId": 1,
      "username": "SV001",
      "email": "sv001@example.com",
      "studentCode": "SV001",
      "fullName": "Nguyễn Văn A",
      "password": null,
      "isActivated": true,
      "emailSent": false,
      "createdAt": "2025-12-04T10:00:00"
    },
    {
      "userId": 2,
      "studentId": 2,
      "username": "SV002",
      "email": "sv002@example.com",
      "studentCode": "SV002",
      "fullName": "Trần Thị B",
      "password": null,
      "isActivated": true,
      "emailSent": true,
      "createdAt": "2025-12-04T09:00:00"
    }
  ]
}
```

**Lưu ý:**
- `password` = `null` (không hiển thị password sau khi đã tạo)
- `emailSent` = `true` nếu user đã đăng nhập ít nhất 1 lần (kiểm tra qua `user.lastLogin != null`), `false` nếu chưa đăng nhập
  - **Cách hoạt động:** Khi user login thành công, hệ thống tự động cập nhật `user.lastLogin = thời gian hiện tại`
  - **Logic:** Nếu `lastLogin != null` → User đã từng login → Có nghĩa là đã có credentials (từ email hoặc admin) → `emailSent = true`

---

### 4. Chỉnh Sửa Thông Tin Tài Khoản

**Endpoint:** `PUT /api/admin/students/{studentId}/account`

**Mô tả:** Chỉnh sửa thông tin tài khoản sinh viên. Tất cả các field đều optional.

**Request:**
```bash
curl --location --request PUT 'http://localhost:8080/api/admin/students/1/account' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "username": "SV001_UPDATED",
  "email": "sv001_updated@example.com",
  "studentCode": "SV001_UPDATED",
  "fullName": "Nguyễn Văn A (Updated)"
}'
```

**Request chỉ sửa một số field:**
```bash
curl --location --request PUT 'http://localhost:8080/api/admin/students/1/account' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "fullName": "Nguyễn Văn A (Updated)"
}'
```

**Response thành công:**
```json
{
  "status": true,
  "message": "Student account updated successfully",
  "data": {
    "userId": 1,
    "studentId": 1,
    "username": "SV001_UPDATED",
    "email": "sv001_updated@example.com",
    "studentCode": "SV001_UPDATED",
    "fullName": "Nguyễn Văn A (Updated)",
    "password": null,
    "isActivated": true,
    "emailSent": false,
    "createdAt": "2025-12-04T10:00:00"
  }
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

**Lưu ý:**
- Hệ thống sẽ validate: username, email, studentCode không được trùng với tài khoản khác
- Chỉ cập nhật các field được gửi trong request

---

### 5. Xóa Tài Khoản (Soft Delete)

**Endpoint:** `DELETE /api/admin/students/{studentId}/account`

**Mô tả:** Xóa tài khoản sinh viên (soft delete: set `isDeleted = true` cho cả User và Student).

**Request:**
```bash
curl --location --request DELETE 'http://localhost:8080/api/admin/students/1/account' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response thành công:**
```json
{
  "status": true,
  "message": "Student account deleted successfully",
  "data": null
}
```

**Response lỗi:**
```json
{
  "status": false,
  "message": "Student not found",
  "data": null
}
```

---

### 6. Gửi Email Credentials Cho 1 Sinh Viên

**Endpoint:** `POST /api/admin/students/{studentId}/send-credentials`

**Mô tả:** Gửi email chứa username và password cho 1 sinh viên. Hệ thống sẽ tạo password mới trước khi gửi.

**Request:**
```bash
curl --location --request POST 'http://localhost:8080/api/admin/students/1/send-credentials' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response thành công:**
```json
{
  "status": true,
  "message": "Credentials sent successfully",
  "data": null
}
```

**Response lỗi:**
```json
{
  "status": false,
  "message": "Student not found",
  "data": null
}
```

**Email gửi đến sinh viên:**
```
Subject: Thông tin tài khoản CampusLife

Chào mừng đến với CampusLife!

Xin chào,

Bạn đã được tạo tài khoản trên hệ thống CampusLife. Dưới đây là thông tin đăng nhập của bạn:

Tên đăng nhập: SV001
Mật khẩu: aB3xY9mK2p

Vui lòng đăng nhập tại: http://localhost:3000/login

⚠️ Lưu ý: Vui lòng đổi mật khẩu sau khi đăng nhập lần đầu để bảo mật tài khoản.

Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ với quản trị viên.

Trân trọng,
CampusLife Team
```

---

### 7. Gửi Email Credentials Hàng Loạt

**Endpoint:** `POST /api/admin/students/bulk-send-credentials`

**Mô tả:** Gửi email credentials cho nhiều sinh viên cùng lúc. Hệ thống sẽ tạo password mới cho từng sinh viên.

**Request:**
```bash
curl --location 'http://localhost:8080/api/admin/students/bulk-send-credentials' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "studentIds": [1, 2, 3]
}'
```

**Response thành công:**
```json
{
  "status": true,
  "message": "Sent credentials to 2 students. 1 errors.",
  "data": {
    "successList": [
      "Student ID 1 (sv001@example.com)",
      "Student ID 2 (sv002@example.com)"
    ],
    "errorList": [
      "Student ID 3: Not found"
    ],
    "successCount": 2,
    "errorCount": 1
  }
}
```

**Lưu ý:**
- Hệ thống sẽ gửi email tuần tự (không async) để tránh quá tải
- Mỗi sinh viên sẽ nhận password mới (khác nhau)
- Response trả về danh sách thành công và lỗi

---

## Các File Liên Quan

### 1. DTOs (Data Transfer Objects)

**Location:** `src/main/java/vn/campuslife/model/student/`

- **ExcelStudentRow.java** - DTO cho một dòng trong Excel
- **UploadExcelResponse.java** - Response sau khi upload Excel
- **BulkCreateStudentsRequest.java** - Request để tạo tài khoản hàng loạt
- **StudentAccountResponse.java** - Response cho tài khoản sinh viên (để review)
- **UpdateStudentAccountRequest.java** - Request để chỉnh sửa thông tin
- **BulkSendCredentialsRequest.java** - Request để gửi email hàng loạt

## Validation Rules

### 1. File Excel

- File phải là định dạng `.xlsx` hoặc `.xls`
- File không được rỗng
- Mỗi dòng phải có đủ 3 thông tin: mã số sinh viên, họ tên, email

### 2. Tạo Tài Khoản

- **Mã số sinh viên:** Không được để trống, không được trùng
- **Email:** Không được để trống, phải đúng format email, không được trùng
- **Họ tên:** Không được để trống

### 3. Chỉnh Sửa Tài Khoản

- **Username:** Không được trùng với username khác (trừ chính nó)
- **Email:** Phải đúng format, không được trùng với email khác (trừ chính nó)
- **StudentCode:** Không được trùng với studentCode khác (trừ chính nó)

---

## Error Handling

### Common Errors

1. **File không đúng format:**
   ```json
   {
     "status": false,
     "message": "File must be Excel format (.xlsx or .xls)",
     "data": null
   }
   ```

2. **Dữ liệu không hợp lệ:**
   - Response sẽ chứa `invalidRows` và `errors` với chi tiết lỗi từng dòng

3. **Tài khoản đã tồn tại:**
   ```json
   {
     "status": true,
     "message": "Created 1 accounts successfully. 1 errors.",
     "data": {
       "errors": ["Dòng SV001: Mã số sinh viên đã tồn tại"]
     }
   }
   ```

4. **Gửi email thất bại:**
   - Response sẽ chứa `errorList` với chi tiết lỗi từng sinh viên
   - Tài khoản vẫn được tạo thành công (không rollback)

---

## Best Practices cho Frontend

### 1. Upload Excel

- Hiển thị progress bar khi upload
- Sau khi upload thành công, hiển thị:
  - Tổng số dòng
  - Số dòng hợp lệ
  - Số dòng không hợp lệ với chi tiết lỗi
- Cho phép admin chọn các dòng hợp lệ để tạo tài khoản

### 2. Review Tài Khoản

- Hiển thị danh sách tài khoản dạng table với các cột:
  - Username
  - Email
  - Student Code
  - Full Name
  - Email Sent (icon check/x)
  - Created At
- Cho phép:
  - Sort theo các cột
  - Filter theo emailSent
  - Edit inline hoặc modal
  - Delete với confirmation

### 3. Gửi Email

- Hiển thị danh sách sinh viên chưa gửi email (emailSent = false)
- Cho phép:
  - Chọn từng sinh viên để gửi
  - Chọn tất cả để gửi hàng loạt
- Hiển thị progress khi gửi hàng loạt
- Hiển thị kết quả (thành công/thất bại) sau khi gửi

### 4. Error Handling

- Hiển thị lỗi validation ngay khi upload Excel
- Hiển thị lỗi khi tạo tài khoản (nếu có)
- Hiển thị lỗi khi gửi email (nếu có)
- Cho phép retry cho các lỗi có thể retry được

---

## Example Flow (Frontend)

### Step 1: Upload Excel

```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

const response = await fetch('http://localhost:8080/api/admin/students/upload-excel', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${adminToken}`
  },
  body: formData
});

const data = await response.json();
// Hiển thị validRows và invalidRows
```

### Step 2: Create Accounts

```javascript
const response = await fetch('http://localhost:8080/api/admin/students/bulk-create', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${adminToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    students: selectedValidRows // Từ step 1
  })
});

const data = await response.json();
// Hiển thị createdAccounts và errors
```

### Step 3: Review Accounts

```javascript
const response = await fetch('http://localhost:8080/api/admin/students/pending', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${adminToken}`
  }
});

const data = await response.json();
// Hiển thị danh sách tài khoản
```

### Step 4: Send Credentials

```javascript
// Gửi cho 1 sinh viên
const response = await fetch(`http://localhost:8080/api/admin/students/${studentId}/send-credentials`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${adminToken}`
  }
});

// Hoặc gửi hàng loạt
const response = await fetch('http://localhost:8080/api/admin/students/bulk-send-credentials', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${adminToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    studentIds: [1, 2, 3]
  })
});
```

---

## Tóm Tắt

### Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/admin/students/upload-excel` | Upload và parse Excel |
| POST | `/api/admin/students/bulk-create` | Tạo tài khoản hàng loạt |
| GET | `/api/admin/students/pending` | Lấy danh sách tài khoản |
| PUT | `/api/admin/students/{studentId}/account` | Chỉnh sửa tài khoản |
| DELETE | `/api/admin/students/{studentId}/account` | Xóa tài khoản |
| POST | `/api/admin/students/{studentId}/send-credentials` | Gửi email cho 1 sinh viên |
| POST | `/api/admin/students/bulk-send-credentials` | Gửi email hàng loạt |

### Security

- Tất cả endpoints yêu cầu authentication (JWT token)
- Chỉ role ADMIN mới có quyền truy cập

### Data Flow

1. **Upload Excel** → Parse và validate → Trả về validRows/invalidRows
2. **Bulk Create** → Tạo User + Student → Khởi tạo điểm → Trả về createdAccounts
3. **Review** → Xem danh sách → Chỉnh sửa (nếu cần)
4. **Send Email** → Tạo password mới → Gửi email → Cập nhật trạng thái

---
