# CURL Commands để Test API Địa Chỉ (Address)

## LƯU Ý QUAN TRỌNG

1. **Thay thế tokens:**
   - `{STUDENT_TOKEN}` - Token của user có role STUDENT
   - `{ADMIN_TOKEN}` hoặc `{MANAGER_TOKEN}` - Token của user có role ADMIN hoặc MANAGER (nếu cần)

2. **Thay thế IDs:**
   - `{studentId}` - ID của student (nếu cần)

3. **Base URL:** `http://localhost:8080` (thay đổi nếu cần)

4. **Lấy Token:** Đăng nhập trước để lấy JWT token
   ```bash
   curl --location 'http://localhost:8080/api/auth/login' \
   --header 'Content-Type: application/json' \
   --data '{
     "username": "student",
     "password": "password"
   }'
   ```

---

## PHẦN 1: QUẢN LÝ TỈNH/THÀNH PHỐ VÀ PHƯỜNG/XÃ

### 1.1. Lấy danh sách tất cả tỉnh/thành phố

**API:** `GET /api/addresses/provinces`

**Yêu cầu:**
- Không cần authentication (hoặc tùy theo cấu hình security)

```bash
curl --location 'http://localhost:8080/api/addresses/provinces' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Provinces retrieved successfully",
  "data": [
    {
      "code": 79,
      "name": "Thành phố Hồ Chí Minh"
    },
    {
      "code": 1,
      "name": "Thành phố Hà Nội"
    },
    ...
  ]
}
```

### 1.2. Lấy danh sách phường/xã theo mã tỉnh

**API:** `GET /api/addresses/provinces/{provinceCode}/wards`

**Yêu cầu:**
- `provinceCode`: Mã tỉnh/thành phố (ví dụ: 79 cho TP.HCM)

```bash
curl --location 'http://localhost:8080/api/addresses/provinces/79/wards' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Wards retrieved successfully",
  "data": [
    {
      "maphuongxa": 12345,
      "tenphuongxa": "Phường 1",
      ...
    },
    {
      "maphuongxa": 12346,
      "tenphuongxa": "Phường 2",
      ...
    },
    ...
  ]
}
```

### 1.3. Tải dữ liệu tỉnh/thành phố từ GitHub (Admin/Manager)

**API:** `POST /api/addresses/load-data`

**Yêu cầu:**
- Role: `ADMIN` hoặc `MANAGER` (tùy theo cấu hình)

```bash
curl --location --request POST 'http://localhost:8080/api/addresses/load-data' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Province data loaded successfully",
  "data": [...]
}
```

---

## PHẦN 2: QUẢN LÝ ĐỊA CHỈ CỦA STUDENT

### 2.1. Lấy địa chỉ của student hiện tại

**API:** `GET /api/addresses/my`

**Yêu cầu:**
- Role: `STUDENT`
- Lấy địa chỉ của chính student đang đăng nhập

```bash
curl --location 'http://localhost:8080/api/addresses/my' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response khi có địa chỉ:**
```json
{
  "status": true,
  "message": "Address retrieved successfully",
  "data": {
    "id": 1,
    "studentId": 123,
    "provinceCode": 79,
    "provinceName": "Thành phố Hồ Chí Minh",
    "wardCode": 12345,
    "wardName": "Phường 1",
    "street": "123 Đường ABC",
    "note": "Gần trường học",
    "createdAt": "2025-02-05T10:00:00",
    "updatedAt": "2025-02-05T10:00:00"
  }
}
```

**Response khi chưa có địa chỉ:**
```json
{
  "status": false,
  "message": "Address not found",
  "data": null
}
```

### 2.2. Tạo địa chỉ mới cho student hiện tại

**API:** `POST /api/addresses/my`

**Yêu cầu:**
- Role: `STUDENT`
- Tạo địa chỉ mới cho chính student đang đăng nhập

**Request Parameters:**
- `provinceCode` (bắt buộc): Mã tỉnh/thành phố
- `provinceName` (bắt buộc): Tên tỉnh/thành phố
- `wardCode` (bắt buộc): Mã phường/xã
- `wardName` (bắt buộc): Tên phường/xã
- `street` (tùy chọn): Địa chỉ cụ thể (số nhà, tên đường)
- `note` (tùy chọn): Ghi chú thêm

```bash
curl --location --request POST 'http://localhost:8080/api/addresses/my' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'provinceCode=79' \
--data-urlencode 'provinceName=Thành phố Hồ Chí Minh' \
--data-urlencode 'wardCode=12345' \
--data-urlencode 'wardName=Phường 1' \
--data-urlencode 'street=123 Đường ABC' \
--data-urlencode 'note=Gần trường học'
```

**Hoặc dùng JSON (nếu controller hỗ trợ):**
```bash
curl --location --request POST 'http://localhost:8080/api/addresses/my' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "provinceCode": 79,
  "provinceName": "Thành phố Hồ Chí Minh",
  "wardCode": 12345,
  "wardName": "Phường 1",
  "street": "123 Đường ABC",
  "note": "Gần trường học"
}'
```

**Response khi thành công:**
```json
{
  "status": true,
  "message": "Address created successfully",
  "data": {
    "id": 1,
    "studentId": 123,
    "provinceCode": 79,
    "provinceName": "Thành phố Hồ Chí Minh",
    "wardCode": 12345,
    "wardName": "Phường 1",
    "street": "123 Đường ABC",
    "note": "Gần trường học",
    "createdAt": "2025-02-05T10:00:00",
    "updatedAt": "2025-02-05T10:00:00"
  }
}
```

**Response khi đã có địa chỉ:**
```json
{
  "status": false,
  "message": "Address already exists for this student",
  "data": null
}
```

### 2.3. Cập nhật địa chỉ của student hiện tại

**API:** `PUT /api/addresses/my`

**Yêu cầu:**
- Role: `STUDENT`
- Cập nhật địa chỉ của chính student đang đăng nhập
- Nếu chưa có địa chỉ, sẽ tự động tạo mới

**Request Parameters:**
- `provinceCode` (bắt buộc): Mã tỉnh/thành phố
- `provinceName` (bắt buộc): Tên tỉnh/thành phố
- `wardCode` (bắt buộc): Mã phường/xã
- `wardName` (bắt buộc): Tên phường/xã
- `street` (tùy chọn): Địa chỉ cụ thể
- `note` (tùy chọn): Ghi chú thêm

```bash
curl --location --request PUT 'http://localhost:8080/api/addresses/my' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'provinceCode=1' \
--data-urlencode 'provinceName=Thành phố Hà Nội' \
--data-urlencode 'wardCode=54321' \
--data-urlencode 'wardName=Phường Hoàn Kiếm' \
--data-urlencode 'street=456 Đường XYZ' \
--data-urlencode 'note=Gần hồ Hoàn Kiếm'
```

**Response khi thành công:**
```json
{
  "status": true,
  "message": "Address updated successfully",
  "data": {
    "id": 1,
    "studentId": 123,
    "provinceCode": 1,
    "provinceName": "Thành phố Hà Nội",
    "wardCode": 54321,
    "wardName": "Phường Hoàn Kiếm",
    "street": "456 Đường XYZ",
    "note": "Gần hồ Hoàn Kiếm",
    "createdAt": "2025-02-05T10:00:00",
    "updatedAt": "2025-02-05T11:00:00"
  }
}
```

### 2.4. Xóa địa chỉ của student hiện tại

**API:** `DELETE /api/addresses/my`

**Yêu cầu:**
- Role: `STUDENT`
- Xóa địa chỉ của chính student đang đăng nhập (soft delete)

```bash
curl --location --request DELETE 'http://localhost:8080/api/addresses/my' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response khi thành công:**
```json
{
  "status": true,
  "message": "Address deleted successfully",
  "data": null
}
```

**Response khi không tìm thấy:**
```json
{
  "status": false,
  "message": "Address not found",
  "data": null
}
```

---

## PHẦN 3: TÌM KIẾM ĐỊA CHỈ

### 3.1. Tìm kiếm địa chỉ theo từ khóa

**API:** `GET /api/addresses/search?keyword={keyword}`

**Yêu cầu:**
- `keyword`: Từ khóa tìm kiếm (tên tỉnh, phường/xã, hoặc đường)

```bash
curl --location 'http://localhost:8080/api/addresses/search?keyword=Hồ Chí Minh' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Search completed",
  "data": [
    {
      "id": 1,
      "provinceName": "Thành phố Hồ Chí Minh",
      "wardName": "Phường 1",
      "street": "123 Đường ABC"
    },
    {
      "id": 2,
      "provinceName": "Thành phố Hồ Chí Minh",
      "wardName": "Phường 2",
      "street": "456 Đường XYZ"
    },
    ...
  ]
}
```

---

## PHẦN 4: FLOW HOÀN CHỈNH

### Flow tạo địa chỉ cho student mới:

```bash
# Bước 1: Lấy danh sách tỉnh/thành phố
curl --location 'http://localhost:8080/api/addresses/provinces' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
# → Lưu lại provinceCode (ví dụ: 79)

# Bước 2: Lấy danh sách phường/xã theo tỉnh
curl --location 'http://localhost:8080/api/addresses/provinces/79/wards' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
# → Lưu lại wardCode (ví dụ: 12345)

# Bước 3: Tạo địa chỉ mới
curl --location --request POST 'http://localhost:8080/api/addresses/my' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'provinceCode=79' \
--data-urlencode 'provinceName=Thành phố Hồ Chí Minh' \
--data-urlencode 'wardCode=12345' \
--data-urlencode 'wardName=Phường 1' \
--data-urlencode 'street=123 Đường ABC' \
--data-urlencode 'note=Gần trường học'

# Bước 4: Kiểm tra địa chỉ đã tạo
curl --location 'http://localhost:8080/api/addresses/my' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

### Flow cập nhật địa chỉ:

```bash
# Bước 1: Lấy địa chỉ hiện tại
curl --location 'http://localhost:8080/api/addresses/my' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'

# Bước 2: Cập nhật địa chỉ
curl --location --request PUT 'http://localhost:8080/api/addresses/my' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'provinceCode=1' \
--data-urlencode 'provinceName=Thành phố Hà Nội' \
--data-urlencode 'wardCode=54321' \
--data-urlencode 'wardName=Phường Hoàn Kiếm' \
--data-urlencode 'street=456 Đường XYZ'
```

---

## LƯU Ý

1. **Request Body Format:**
   - Controller hiện tại dùng `@RequestParam`, nên phải dùng `application/x-www-form-urlencoded`
   - Nếu muốn dùng JSON, cần tạo Request DTO và cập nhật controller

2. **Authentication:**
   - Tất cả API đều cần JWT token
   - Student chỉ có thể quản lý địa chỉ của chính mình

3. **Validation:**
   - `provinceCode`, `provinceName`, `wardCode`, `wardName` là bắt buộc
   - `street` và `note` là tùy chọn

4. **Soft Delete:**
   - Xóa địa chỉ chỉ set `isDeleted = true`, không xóa thực sự khỏi database

5. **Bidirectional Relationship:**
   - Khi tạo/cập nhật địa chỉ, hệ thống tự động cập nhật relationship với Student

---

## TÓM TẮT CÁC API

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/addresses/provinces` | Lấy danh sách tỉnh/thành phố | All |
| GET | `/api/addresses/provinces/{code}/wards` | Lấy danh sách phường/xã | All |
| GET | `/api/addresses/my` | Lấy địa chỉ của student hiện tại | STUDENT |
| POST | `/api/addresses/my` | Tạo địa chỉ mới | STUDENT |
| PUT | `/api/addresses/my` | Cập nhật địa chỉ | STUDENT |
| DELETE | `/api/addresses/my` | Xóa địa chỉ | STUDENT |
| GET | `/api/addresses/search?keyword={keyword}` | Tìm kiếm địa chỉ | All |
| POST | `/api/addresses/load-data` | Tải dữ liệu tỉnh/thành phố | ADMIN/MANAGER |


