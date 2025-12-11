# Hướng Dẫn Debug Lỗi Khi Gửi Email

## Vấn Đề

### Lỗi 403 (Forbidden)
Khi gửi email, nhận được lỗi `403 (Forbidden)` dù đang đăng nhập với role ADMIN hoặc MANAGER.

### Lỗi 415 (Unsupported Media Type)
Khi gửi email, nhận được lỗi `415 (Unsupported Media Type)` - server không chấp nhận Content-Type của request.

## Các Thay Đổi Đã Thực Hiện

### 1. Cập Nhật SecurityConfig
- Di chuyển rule `/api/emails/**` lên trước các rule khác để tránh conflict
- Thêm explicit method matching cho các endpoint email cụ thể
- Thêm rule cho endpoint `/api/emails/send-json`

### 2. Thêm Logging
- Thêm logging vào `EmailController` để debug authentication

### 3. Thêm Endpoint Test
- Thêm endpoint `GET /api/emails/test-auth` để kiểm tra authentication

### 4. Thêm Endpoint JSON (Fix Lỗi 415)
- Thêm endpoint `POST /api/emails/send-json` để chấp nhận JSON (không có attachments)
- Cập nhật frontend guide để sử dụng Blob với Content-Type khi append vào FormData

## Các Bước Debug

### Bước 1: Kiểm Tra Authentication
Gọi endpoint test để xem authentication có hoạt động không:

```bash
curl --location 'http://localhost:8080/api/emails/test-auth' \
--header 'Authorization: Bearer {YOUR_TOKEN}'
```

**Response mong đợi:**
```json
{
  "status": true,
  "message": "Authentication successful",
  "data": {
    "username": "admin",
    "authorities": ["ROLE_ADMIN"]
  }
}
```

**Nếu nhận được 403:**
- Token không hợp lệ hoặc đã hết hạn
- User không có role ADMIN hoặc MANAGER
- Authentication không được set đúng

### Bước 2: Kiểm Tra Token
Decode JWT token để xem role có đúng không:

1. Vào https://jwt.io/
2. Paste token vào
3. Kiểm tra claim `role` trong payload:
   ```json
   {
     "sub": "admin",
     "role": "ADMIN",  // Phải là "ADMIN" hoặc "MANAGER"
     "exp": 1234567890
   }
   ```

### Bước 3: Kiểm Tra User Role Trong Database
```sql
SELECT u.id, u.username, u.role 
FROM users u 
WHERE u.username = 'admin' AND u.is_deleted = false;
```

Role phải là `ADMIN` hoặc `MANAGER`.

### Bước 4: Kiểm Tra Logs
Xem logs của server khi gửi request:

```bash
# Trong logs, tìm các dòng:
# - "Processing request: POST /api/emails/send"
# - "Authentication set successfully for user: ..."
# - "Email send request received. Authentication: ..."
```

### Bước 5: Kiểm Tra Request Format

#### Nếu KHÔNG có attachments (Dùng JSON endpoint):
```typescript
const response = await fetch('/api/emails/send-json', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  },
  body: JSON.stringify({
    recipientType: 'INDIVIDUAL',
    recipientIds: [1, 2, 3],
    subject: 'Test Email',
    content: 'Test content',
    isHtml: false,
    createNotification: false
  })
});
```

#### Nếu CÓ attachments (Dùng Multipart endpoint):
**Frontend (TypeScript/JavaScript):**
```typescript
const formData = new FormData();

// QUAN TRỌNG: Phải dùng Blob với Content-Type application/json
const requestBlob = new Blob([JSON.stringify({
  recipientType: 'INDIVIDUAL',
  recipientIds: [1, 2, 3],
  subject: 'Test Email',
  content: 'Test content',
  isHtml: false,
  createNotification: false
})], { type: 'application/json' });

formData.append('request', requestBlob);

// Nếu có attachments
if (attachments && attachments.length > 0) {
  attachments.forEach(file => {
    formData.append('attachments', file);
  });
}

const response = await fetch('/api/emails/send', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    // KHÔNG set Content-Type header, browser sẽ tự động set với boundary
  },
  body: formData
});
```

**cURL (Multipart):**
```bash
curl --location 'http://localhost:8080/api/emails/send' \
--header 'Authorization: Bearer {YOUR_TOKEN}' \
--form 'request="{\"recipientType\":\"INDIVIDUAL\",\"recipientIds\":[1],\"subject\":\"Test\",\"content\":\"Test content\"}"' \
--form 'attachments=@"/path/to/file.pdf"'
```

**cURL (JSON - không có attachments):**
```bash
curl --location 'http://localhost:8080/api/emails/send-json' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {YOUR_TOKEN}' \
--data '{
  "recipientType": "INDIVIDUAL",
  "recipientIds": [1],
  "subject": "Test",
  "content": "Test content",
  "isHtml": false,
  "createNotification": false
}'
```

## Các Nguyên Nhân Thường Gặp

### 1. Token Hết Hạn
- **Giải pháp:** Đăng nhập lại để lấy token mới

### 2. Role Không Đúng
- **Kiểm tra:** User trong database có role `ADMIN` hoặc `MANAGER` không
- **Giải pháp:** Cập nhật role trong database hoặc đăng nhập với user có role đúng

### 3. Request Format Sai (Lỗi 415)
- **Kiểm tra:** Content-Type header có đúng không
  - Multipart: `multipart/form-data` (browser tự động set)
  - JSON: `application/json`
- **Giải pháp:** 
  - Nếu không có attachments: Dùng endpoint `/api/emails/send-json` với JSON
  - Nếu có attachments: Dùng endpoint `/api/emails/send` với FormData và Blob có Content-Type `application/json`
  - **QUAN TRỌNG:** Khi append JSON vào FormData, phải dùng `Blob` với `type: 'application/json'`

### 4. CORS Issues
- **Kiểm tra:** Request có bị chặn bởi CORS không
- **Giải pháp:** Kiểm tra CORS configuration trong `CorsConfig.java`

### 5. Authentication Không Được Set
- **Kiểm tra:** JWT filter có chạy không
- **Giải pháp:** Kiểm tra logs để xem authentication có được set không

## Kiểm Tra Nhanh

1. **Test Authentication:**
   ```bash
   curl --location 'http://localhost:8080/api/emails/test-auth' \
   --header 'Authorization: Bearer {YOUR_TOKEN}'
   ```

2. **Test Send Email (JSON - Không có attachments):**
   ```bash
   curl --location 'http://localhost:8080/api/emails/send-json' \
   --header 'Content-Type: application/json' \
   --header 'Authorization: Bearer {YOUR_TOKEN}' \
   --data '{"recipientType":"INDIVIDUAL","recipientIds":[1],"subject":"Test","content":"Test","isHtml":false,"createNotification":false}'
   ```

3. **Test Send Email (Multipart - Có attachments):**
   ```bash
   curl --location 'http://localhost:8080/api/emails/send' \
   --header 'Authorization: Bearer {YOUR_TOKEN}' \
   --form 'request="{\"recipientType\":\"INDIVIDUAL\",\"recipientIds\":[1],\"subject\":\"Test\",\"content\":\"Test\"}"'
   ```

3. **Kiểm Tra Logs:**
   - Xem logs server khi gửi request
   - Tìm các dòng có chứa "Email send request received" hoặc "Authentication set successfully"

## Nếu Vẫn Gặp Lỗi

1. **Restart Server:**
   ```bash
   mvn spring-boot:run
   ```

2. **Kiểm Tra SecurityConfig:**
   - Đảm bảo rule `/api/emails/**` được đặt đúng
   - Kiểm tra không có rule nào khác chặn trước

3. **Kiểm Tra JWT Filter:**
   - Đảm bảo JWT filter được add vào filter chain
   - Kiểm tra JWT secret key có đúng không

4. **Kiểm Tra UserDetailsService:**
   - Đảm bảo `getAuthorities()` trả về `ROLE_ADMIN` hoặc `ROLE_MANAGER`

## Liên Hệ
Nếu vẫn gặp vấn đề, cung cấp:
- Logs từ server
- Response từ endpoint `/api/emails/test-auth`
- Token payload (decode từ jwt.io)
- Request format đang sử dụng

