# Hướng Dẫn Format Dữ Liệu - Email và Notification System

## 1. Kiểu Dữ Liệu Request

### 1.1. SendEmailRequest Format

```typescript
interface SendEmailRequest {
  recipientType: 'BULK' | 'ACTIVITY_REGISTRATIONS' | 'SERIES_REGISTRATIONS' | 
                 'ALL_STUDENTS' | 'BY_CLASS' | 'BY_DEPARTMENT';
  
  // Required fields based on recipientType
  recipientIds?: number[];        // Required for BULK (có thể 1 hoặc nhiều user IDs)
  activityId?: number;            // Required for ACTIVITY_REGISTRATIONS
  seriesId?: number;               // Required for SERIES_REGISTRATIONS
  classId?: number;                // Required for BY_CLASS
  departmentId?: number;           // Required for BY_DEPARTMENT
  
  // Email content
  subject: string;                 // Required - Tiêu đề email
  content: string;                 // Required - Nội dung email (Text or HTML)
  isHtml?: boolean;                // Default: false - true nếu content là HTML
  
  // Template variables (optional)
  templateVariables?: Record<string, string>; // Optional - Biến template để thay thế trong content
  
  // Notification options
  createNotification?: boolean;    // Default: false - Có tạo notification không
  notificationTitle?: string;      // Optional - Tiêu đề notification (nếu khác subject)
  notificationType?: string;        // Optional - Loại notification
  notificationActionUrl?: string;  // Optional - URL khi click notification
}
```

### 1.2. SendNotificationOnlyRequest Format

```typescript
interface SendNotificationOnlyRequest {
  recipientType: 'BULK' | 'ACTIVITY_REGISTRATIONS' | 'SERIES_REGISTRATIONS' | 
                 'ALL_STUDENTS' | 'BY_CLASS' | 'BY_DEPARTMENT';
  
  // Required fields based on recipientType
  recipientIds?: number[];        // Required for BULK (có thể 1 hoặc nhiều)
  activityId?: number;            // Required for ACTIVITY_REGISTRATIONS
  seriesId?: number;              // Required for SERIES_REGISTRATIONS
  classId?: number;               // Required for BY_CLASS
  departmentId?: number;          // Required for BY_DEPARTMENT
  
  // Notification content
  title: string;                  // Required - Tiêu đề notification (có thể dùng template variables)
  content: string;                // Required - Nội dung notification (có thể dùng template variables)
  type: string;                   // Required - Loại notification
  actionUrl?: string;            // Optional - URL khi click notification
  
  // Template variables (optional)
  templateVariables?: Record<string, string>; // Optional - Biến template để thay thế trong title và content
}
```

### 1.3. RecipientType Enum

```typescript
enum RecipientType {
  BULK = 'BULK',                           // Gửi theo danh sách user IDs (có thể 1 hoặc nhiều)
  ACTIVITY_REGISTRATIONS = 'ACTIVITY_REGISTRATIONS',  // Sinh viên đã đăng ký activity
  SERIES_REGISTRATIONS = 'SERIES_REGISTRATIONS',      // Sinh viên đã đăng ký series
  ALL_STUDENTS = 'ALL_STUDENTS',          // Tất cả sinh viên
  BY_CLASS = 'BY_CLASS',                  // Sinh viên theo lớp
  BY_DEPARTMENT = 'BY_DEPARTMENT'        // Sinh viên theo khoa
}
```

## 2. File Đính Kèm

### 2.1. Format và Cách Gửi

- **Format:** `MultipartFile[]` trong FormData
- **Tên field:** `attachments`
- **Endpoint:** `POST /api/emails/send` (multipart/form-data)

### 2.2. Code Example

```typescript
const sendEmailWithAttachments = async (
  request: SendEmailRequest,
  attachments?: File[]
): Promise<Response<EmailSendResult>> => {
  const formData = new FormData();
  
  // Convert request to JSON string và append với Blob có Content-Type application/json
  // QUAN TRỌNG: Phải dùng Blob với Content-Type để Spring có thể parse đúng
  const requestBlob = new Blob([JSON.stringify(request)], { 
    type: 'application/json' 
  });
  formData.append('request', requestBlob);
  
  // Thêm file đính kèm nếu có
  if (attachments && attachments.length > 0) {
    attachments.forEach((file) => {
      formData.append('attachments', file);
    });
  }
  
  const response = await fetch('/api/emails/send', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`,
      // KHÔNG set Content-Type header manually khi dùng FormData
      // Browser sẽ tự động set với boundary
    },
    body: formData,
  });
  
  return response.json();
};
```

### 2.3. Giới Hạn

- **Max file size:** Theo cấu hình `spring.servlet.multipart.max-file-size` (mặc định: 10MB)
- **Max request size:** Theo cấu hình `spring.servlet.multipart.max-request-size` (mặc định: 10MB)
- **Các định dạng hỗ trợ:** Tất cả (PDF, DOCX, XLSX, images, etc.)

### 2.4. Lưu Ý

- Nếu không có file đính kèm, có thể dùng endpoint JSON: `POST /api/emails/send-json`
- File đính kèm sẽ được lưu trên server và link được lưu trong `EmailHistory`
- Có thể download file đính kèm qua endpoint: `GET /api/emails/attachments/{attachmentId}/download`

## 3. HTML Content

### 3.1. Cách Sử Dụng

- Set `isHtml: true` khi content là HTML
- Content sẽ được render như HTML trong email client

### 3.2. Ví Dụ

```typescript
const request: SendEmailRequest = {
  recipientType: 'BULK',
  recipientIds: [123],
  subject: "Thông báo quan trọng",
  content: `
    <h1 style="color: #007bff;">Xin chào!</h1>
    <p>Đây là nội dung email với <strong>HTML</strong>.</p>
    <ul>
      <li>Mục 1</li>
      <li>Mục 2</li>
    </ul>
    <a href="https://example.com" style="color: #007bff;">Click here</a>
  `,
  isHtml: true
};
```

### 3.3. Best Practices

- Sử dụng inline CSS (nhiều email client không hỗ trợ external CSS)
- Tránh JavaScript (sẽ bị loại bỏ)
- Test trên nhiều email client (Gmail, Outlook, etc.)
- Sử dụng table layout cho responsive design

## 4. Biến Template

### 4.1. Format

- **Pattern:** `{{variableName}}` trong content
- **Ví dụ:** `"Xin chào {{studentName}}, bạn đã đăng ký {{activityName}}"`

### 4.2. Cách Cung Cấp Giá Trị

```typescript
const request: SendEmailRequest = {
  recipientType: 'BULK',
  recipientIds: [123],
  subject: "Thông báo đăng ký",
  content: "Xin chào {{studentName}}, bạn đã đăng ký {{activityName}} thành công.",
  templateVariables: {
    studentName: "Nguyễn Văn A",
    activityName: "Sự kiện mùa hè 2025"
  },
  isHtml: false
};
```

### 4.3. Các Biến Tự Động Có Sẵn

Khi gửi email, hệ thống tự động thêm các biến sau vào `templateVariables` (nếu có):

- `{{email}}` - Email của người nhận (luôn có)
- `{{studentName}}` - Tên sinh viên (nếu recipient là student)
- `{{studentCode}}` - Mã sinh viên (nếu recipient là student)
- `{{className}}` - Tên lớp (nếu recipient là student và có lớp)
- `{{departmentName}}` - Tên khoa (nếu recipient là student và có khoa)
- `{{activityName}}` - Tên activity (nếu gửi theo `ACTIVITY_REGISTRATIONS`)
- `{{activityDate}}` - Ngày bắt đầu activity (nếu có)
- `{{seriesName}}` - Tên series (nếu gửi theo `SERIES_REGISTRATIONS`)

**Lưu ý:** Các biến tự động sẽ được merge với `templateVariables` bạn cung cấp. Nếu trùng tên, giá trị bạn cung cấp sẽ được ưu tiên.

### 4.4. Ví Dụ Với Biến Tự Động

```typescript
// Gửi email cho sinh viên đã đăng ký activity
const request: SendEmailRequest = {
  recipientType: 'ACTIVITY_REGISTRATIONS',
  activityId: 1,
  subject: "Xác nhận đăng ký",
  content: `
    <h1>Xin chào {{studentName}}!</h1>
    <p>Bạn đã đăng ký thành công cho sự kiện: <strong>{{activityName}}</strong></p>
    <p>Mã sinh viên: {{studentCode}}</p>
  `,
  isHtml: true
  // Không cần cung cấp templateVariables cho studentName, activityName, studentCode
  // Hệ thống sẽ tự động lấy từ database
};
```

## 5. Ví Dụ Cụ Thể

### 5.1. Gửi Email Cá Nhân (1 user)

```typescript
const request: SendEmailRequest = {
  recipientType: 'BULK',
  recipientIds: [123], // 1 user ID
  subject: "Thông báo cá nhân",
  content: "Xin chào {{studentName}}, đây là thông báo dành riêng cho bạn.",
  templateVariables: {
    studentName: "Nguyễn Văn A"
  },
  isHtml: false,
  createNotification: true
};

// Gửi không có file đính kèm
const response = await fetch('/api/emails/send-json', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  },
  body: JSON.stringify(request),
});
```

### 5.2. Gửi Email Hàng Loạt (nhiều users)

```typescript
const request: SendEmailRequest = {
  recipientType: 'BULK',
  recipientIds: [123, 456, 789], // Nhiều user IDs
  subject: "Thông báo chung",
  content: `
    <h1 style="color: #007bff;">Thông báo quan trọng</h1>
    <p>Đây là thông báo chung cho tất cả các bạn.</p>
    <p>Vui lòng chú ý theo dõi.</p>
  `,
  isHtml: true,
  createNotification: false
};

// Gửi với file đính kèm
const formData = new FormData();
const requestBlob = new Blob([JSON.stringify(request)], { 
  type: 'application/json' 
});
formData.append('request', requestBlob);

const files = [file1, file2]; // File objects
files.forEach(file => {
  formData.append('attachments', file);
});

const response = await fetch('/api/emails/send', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
  },
  body: formData,
});
```

### 5.3. Gửi Email Theo Khoa

```typescript
const request: SendEmailRequest = {
  recipientType: 'BY_DEPARTMENT',
  departmentId: 1, // ID của khoa
  subject: "Thông báo khoa",
  content: "Đây là thông báo dành cho tất cả sinh viên trong khoa.",
  isHtml: false
};

const response = await fetch('/api/emails/send-json', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  },
  body: JSON.stringify(request),
});
```

### 5.4. Gửi Email Cho Sinh Viên Đã Đăng Ký Activity

```typescript
const request: SendEmailRequest = {
  recipientType: 'ACTIVITY_REGISTRATIONS',
  activityId: 10, // ID của activity
  subject: "Nhắc nhở sự kiện",
  content: `
    <h1>Xin chào {{studentName}}!</h1>
    <p>Bạn đã đăng ký sự kiện: <strong>{{activityName}}</strong></p>
    <p>Vui lòng có mặt đúng giờ.</p>
  `,
  isHtml: true,
  createNotification: true,
  notificationTitle: "Nhắc nhở sự kiện",
  notificationType: "ACTIVITY_REMINDER"
};
```

### 5.5. Gửi Email Cho Tất Cả Sinh Viên

```typescript
const request: SendEmailRequest = {
  recipientType: 'ALL_STUDENTS',
  subject: "Thông báo chung",
  content: "Đây là thông báo dành cho tất cả sinh viên trong hệ thống.",
  isHtml: false
};
```

### 5.6. Gửi Email Cho Sinh Viên Theo Lớp

```typescript
const request: SendEmailRequest = {
  recipientType: 'BY_CLASS',
  classId: 5, // ID của lớp
  subject: "Thông báo lớp",
  content: "Đây là thông báo dành cho tất cả sinh viên trong lớp.",
  isHtml: false
};
```

### 5.7. Chỉ Gửi Notification (Không Gửi Email)

```typescript
const request: SendNotificationOnlyRequest = {
  recipientType: 'BULK',
  recipientIds: [123, 456],
  title: "Thông báo mới",
  content: "Bạn có thông báo mới trong hệ thống.",
  type: "GENERAL",
  actionUrl: "/notifications/123"
};

const response = await fetch('/api/emails/notifications/send', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  },
  body: JSON.stringify(request),
});
```

## 6. API Endpoints

### 6.1. Gửi Email

- **Endpoint:** `POST /api/emails/send` (multipart/form-data - có file đính kèm)
- **Endpoint:** `POST /api/emails/send-json` (application/json - không có file đính kèm)
- **Role:** ADMIN, MANAGER
- **Request Body:** `SendEmailRequest` + `attachments` (optional)

### 6.2. Gửi Notification Only

- **Endpoint:** `POST /api/emails/notifications/send`
- **Role:** ADMIN, MANAGER
- **Request Body:** `SendNotificationOnlyRequest` (JSON)

### 6.3. Lấy Lịch Sử Email

- **Endpoint:** `GET /api/emails/history?page=0&size=20`
- **Role:** ADMIN, MANAGER
- **Response:** Paginated list of `EmailHistory`

### 6.4. Xem Chi Tiết Email

- **Endpoint:** `GET /api/emails/history/{emailId}`
- **Role:** ADMIN, MANAGER
- **Response:** `EmailHistory` với attachments

### 6.5. Download File Đính Kèm

- **Endpoint:** `GET /api/emails/attachments/{attachmentId}/download`
- **Role:** ADMIN, MANAGER
- **Response:** File download

## 7. APIs Hỗ Trợ Lấy Danh Sách Recipients

### 7.1. Lấy Tất Cả Users (Bao Gồm ADMIN, MANAGER, STUDENT)

- **Endpoint:** `GET /api/admin/users?includeStudents=true`
- **Role:** ADMIN, MANAGER
- **Response:** List of `UserResponse` (tất cả users)

### 7.2. Lấy Users Theo Role

- **Endpoint:** `GET /api/admin/users?role=ADMIN` hoặc `?role=MANAGER`
- **Role:** ADMIN, MANAGER
- **Response:** List of `UserResponse` (chỉ ADMIN hoặc MANAGER)

### 7.3. Lấy Sinh Viên Theo Khoa

- **Endpoint:** `GET /api/students/department/{departmentId}?page=0&size=20`
- **Role:** ADMIN, MANAGER
- **Response:** `StudentListResponse` với pagination:
  ```json
  {
    "status": true,
    "message": "Students by department retrieved successfully",
    "data": {
      "content": [
        {
          "id": 1,
          "studentCode": "SV001",
          "fullName": "Nguyễn Văn A",
          "email": "student001@example.com",
          ...
        }
      ],
      "totalElements": 100,
      "totalPages": 5,
      "size": 20,
      "number": 0,
      "first": true,
      "last": false
    }
  }
  ```

### 7.4. Lấy Danh Sách Khoa

- **Endpoint:** `GET /api/departments` (public)
- **Endpoint:** `GET /api/admin/departments` (ADMIN, MANAGER)
- **Response:** List of `Department`

### 7.5. Lấy Sinh Viên Đã Đăng Ký Activity

- **Endpoint:** `GET /api/registrations/activity/{activityId}`
- **Role:** ADMIN, MANAGER
- **Response:** List of `ActivityRegistration`
- **Lưu ý:** API này trả về tất cả registrations (bao gồm PENDING, APPROVED, REJECTED, CANCELLED). Khi gửi email/notification theo `ACTIVITY_REGISTRATIONS`, hệ thống sẽ gửi cho tất cả sinh viên đã đăng ký, không phân biệt trạng thái.

### 7.6. Lấy Sinh Viên Đã Đăng Ký Series

- **Endpoint:** `GET /api/registrations/series/{seriesId}`
- **Role:** ADMIN, MANAGER
- **Response:** List of `ActivityRegistration`
- **Lưu ý:** API này trả về tất cả registrations (bao gồm PENDING, APPROVED, REJECTED, CANCELLED). Khi gửi email/notification theo `SERIES_REGISTRATIONS`, hệ thống sẽ gửi cho tất cả sinh viên đã đăng ký, không phân biệt trạng thái.

## 8. Error Handling

### 8.1. Common Errors

- **400 Bad Request:** Request body không hợp lệ, thiếu required fields
- **403 Forbidden:** Không có quyền (không phải ADMIN hoặc MANAGER)
- **404 Not Found:** Activity/Series/Class/Department không tồn tại
- **415 Unsupported Media Type:** Content-Type không đúng (phải là multipart/form-data hoặc application/json)
- **500 Internal Server Error:** Lỗi server

### 8.2. Response Format

```typescript
interface Response<T> {
  status: boolean;
  message: string;
  data?: T;
}

// Success response
{
  "status": true,
  "message": "Email sent successfully",
  "data": {
    "totalRecipients": 10,
    "successCount": 10,
    "failedCount": 0,
    "emailHistoryId": 123
  }
}

// Error response
{
  "status": false,
  "message": "Subject is required",
  "data": null
}
```

## 9. Best Practices

1. **Luôn validate dữ liệu trước khi gửi:**
   - Kiểm tra `recipientType` và các fields tương ứng
   - Kiểm tra `subject` và `content` không rỗng
   - Kiểm tra `recipientIds` không rỗng nếu `recipientType = BULK`

2. **Sử dụng endpoint phù hợp:**
   - Dùng `/api/emails/send-json` nếu không có file đính kèm (nhanh hơn)
   - Dùng `/api/emails/send` nếu có file đính kèm

3. **Xử lý file đính kèm:**
   - Kiểm tra kích thước file trước khi upload
   - Validate định dạng file nếu cần
   - Hiển thị progress bar khi upload file lớn

4. **Template variables:**
   - Luôn escape HTML nếu không dùng `isHtml: true`
   - Test template với nhiều giá trị khác nhau
   - Sử dụng biến tự động khi có thể

5. **Error handling:**
   - Hiển thị thông báo lỗi rõ ràng cho user
   - Log lỗi để debug
   - Retry mechanism cho lỗi tạm thời

6. **Performance:**
   - Gửi email hàng loạt nên được xử lý async
   - Hiển thị loading state khi đang gửi
   - Pagination cho danh sách recipients lớn

