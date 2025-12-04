# CURL Commands để Test QR Code Check-in

## Lưu ý

1. **Thay thế tokens:**
   - `{ADMIN_TOKEN}` - Token của user có role ADMIN
   - `{MANAGER_TOKEN}` - Token của user có role MANAGER
   - `{STUDENT_TOKEN}` - Token của user có role STUDENT

2. **Thay thế IDs:**
   - `{checkInCode}` - Mã QR code từ activity (ví dụ: `ACT-000123-A7B9C2D1`)

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

---

## 1. Backfill CheckInCodes (Admin/Manager only)

**Endpoint:** `POST /api/activities/backfill-checkin-codes`

**Mục đích:** Tạo checkInCode cho các activity đã tồn tại nhưng chưa có code. Chỉ cần gọi 1 lần sau khi deploy tính năng mới.

```bash
curl --location 'http://localhost:8080/api/activities/backfill-checkin-codes' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--request POST
```

**Response Success:**
```json
{
  "status": true,
  "message": "Đã tạo checkInCode cho 15 activity",
  "data": {
    "updatedCount": 15,
    "totalActivities": 15
  }
}
```

**Response khi tất cả đã có code:**
```json
{
  "status": true,
  "message": "Tất cả activities đã có checkInCode",
  "data": null
}
```

---

## 2. Lấy Activity để xem CheckInCode

**Endpoint:** `GET /api/activities/{activityId}`

**Mục đích:** Xem checkInCode của activity để tạo QR code

```bash
curl --location 'http://localhost:8080/api/activities/1' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Activity retrieved successfully",
  "data": {
    "id": 1,
    "name": "Sự kiện ABC",
    "description": "Mô tả sự kiện",
    "checkInCode": "ACT-000001-A7B9C2D1",  // ← Mã QR code
    // ... các field khác
  }
}
```

---

## 3. Check-in bằng QR Code (Student)

**Endpoint:** `POST /api/registrations/checkin/qr`

**Mục đích:** Sinh viên quét QR code để điểm danh (tự động set thành ATTENDED)

```bash
curl --location 'http://localhost:8080/api/registrations/checkin/qr' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "checkInCode": "ACT-000001-A7B9C2D1"
}'
```

**Response Success (200):**
```json
{
  "status": true,
  "message": "Điểm danh thành công bằng QR code",
  "data": {
    "id": 123,
    "registrationId": 456,
    "activityId": 1,
    "activityName": "Sự kiện ABC",
    "studentId": 789,
    "studentName": "Nguyễn Văn A",
    "participationType": "ATTENDED",
    "isCompleted": true,
    "pointsEarned": 10.0,
    "checkInTime": "2025-12-05T10:30:00",
    "checkOutTime": "2025-12-05T10:30:00",
    "date": "2025-12-05T10:30:00"
  }
}
```

**Response Error (400) - CheckInCode không tồn tại:**
```json
{
  "status": false,
  "message": "Không tìm thấy activity với mã QR này",
  "data": null
}
```

**Response Error (400) - Chưa đăng ký:**
```json
{
  "status": false,
  "message": "Bạn chưa đăng ký hoặc chưa được duyệt tham gia activity này",
  "data": null
}
```

**Response Error (400) - Đã check-in rồi:**
```json
{
  "status": false,
  "message": "Bạn đã điểm danh activity này rồi",
  "data": null
}
```

**Response Error (400) - Activity chưa được công bố:**
```json
{
  "status": false,
  "message": "Activity chưa được công bố",
  "data": null
}
```

---

## 4. Flow Test Hoàn Chỉnh

### Bước 1: Backfill CheckInCodes (Một lần duy nhất)

```bash
curl --location 'http://localhost:8080/api/activities/backfill-checkin-codes' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--request POST
```

### Bước 2: Lấy Activity để xem CheckInCode

```bash
curl --location 'http://localhost:8080/api/activities/1' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

Lưu lại `checkInCode` từ response (ví dụ: `ACT-000001-A7B9C2D1`)

### Bước 3: Student đăng ký Activity (nếu chưa đăng ký)

```bash
curl --location 'http://localhost:8080/api/registrations' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "activityId": 1
}'
```

### Bước 4: Admin/Manager duyệt đăng ký (nếu activity cần duyệt)

```bash
curl --location --request PUT 'http://localhost:8080/api/registrations/{registrationId}/status?status=APPROVED' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### Bước 5: Student check-in bằng QR Code

```bash
curl --location 'http://localhost:8080/api/registrations/checkin/qr' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "checkInCode": "ACT-000001-A7B9C2D1"
}'
```

### Bước 6: Kiểm tra kết quả

```bash
# Lấy danh sách participations của activity
curl --location 'http://localhost:8080/api/registrations/activities/1/participations' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

Response sẽ có participation với `participationType = "ATTENDED"` và `isCompleted = true`.

---

## 5. So Sánh với Check-in Bình Thường

### Check-in Bình Thường (2 lần):

**Lần 1 - Check-in:**
```bash
curl --location 'http://localhost:8080/api/registrations/checkin' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "ticketCode": "ABC12345"
}'
```

Response: `participationType = "CHECKED_IN"`

**Lần 2 - Check-out:**
```bash
curl --location 'http://localhost:8080/api/registrations/checkin' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "ticketCode": "ABC12345"
}'
```

Response: `participationType = "ATTENDED"`

### QR Code Check-in (1 lần):

**Một lần duy nhất:**
```bash
curl --location 'http://localhost:8080/api/registrations/checkin/qr' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "checkInCode": "ACT-000001-A7B9C2D1"
}'
```

Response: `participationType = "ATTENDED"` (tự động, bỏ qua CHECKED_IN)

---

## 6. Test Cases

### Test Case 1: CheckInCode không tồn tại
```bash
curl --location 'http://localhost:8080/api/registrations/checkin/qr' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "checkInCode": "INVALID-CODE"
}'
```

Expected: Error "Không tìm thấy activity với mã QR này"

### Test Case 2: Sinh viên chưa đăng ký
```bash
# Sử dụng checkInCode hợp lệ nhưng sinh viên chưa đăng ký activity đó
curl --location 'http://localhost:8080/api/registrations/checkin/qr' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "checkInCode": "ACT-000001-A7B9C2D1"
}'
```

Expected: Error "Bạn chưa đăng ký hoặc chưa được duyệt tham gia activity này"

### Test Case 3: Đã check-in rồi
```bash
# Gọi lại với cùng checkInCode
curl --location 'http://localhost:8080/api/registrations/checkin/qr' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "checkInCode": "ACT-000001-A7B9C2D1"
}'
```

Expected: Error "Bạn đã điểm danh activity này rồi"

### Test Case 4: Activity là draft
```bash
# Tạo activity với isDraft = true, sau đó thử check-in
curl --location 'http://localhost:8080/api/registrations/checkin/qr' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "checkInCode": "ACT-000001-A7B9C2D1"
}'
```

Expected: Error "Activity chưa được công bố"

---

## 7. Tạo QR Code Image (Optional)

Để tạo QR code image từ checkInCode, có thể dùng các công cụ online hoặc thư viện:

**Online QR Code Generator:**
- https://www.qr-code-generator.com/
- https://www.the-qrcode-generator.com/

**Format:** Nhập checkInCode (ví dụ: `ACT-000001-A7B9C2D1`) và generate QR code image.

**Lưu ý:** Đảm bảo QR code đủ lớn để dễ quét (tối thiểu 200x200px).

---

## Tóm Tắt

- ✅ `POST /api/activities/backfill-checkin-codes` - Tạo checkInCode cho activity cũ (Admin/Manager)
- ✅ `GET /api/activities/{id}` - Xem checkInCode của activity
- ✅ `POST /api/registrations/checkin/qr` - Check-in bằng QR code (Student)
- ✅ QR code check-in tự động set thành ATTENDED (1 lần thay vì 2 lần)
- ✅ Xử lý điểm tự động giống check-out bình thường

