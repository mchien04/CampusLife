# Hướng Dẫn Frontend - Hệ Thống Gửi Email và Thông Báo

## Tổng Quan

Hệ thống cho phép Admin/Manager gửi email và tạo thông báo hệ thống với nhiều tùy chọn người nhận:
- Gửi cá nhân hoặc bulk
- Gửi theo danh sách đăng ký activity/series
- Gửi theo class/department hoặc tất cả students
- Hỗ trợ HTML template với biến động
- Đính kèm file
- Tùy chọn tạo notification khi gửi email
- Chỉ tạo notification (không gửi email)

---

## 1. Models/DTOs

### 1.1. SendEmailRequest

```typescript
interface SendEmailRequest {
  recipientType: RecipientType; // Bắt buộc
  recipientIds?: number[]; // Cho INDIVIDUAL hoặc CUSTOM_LIST
  activityId?: number; // Cho ACTIVITY_REGISTRATIONS
  seriesId?: number; // Cho SERIES_REGISTRATIONS
  classId?: number; // Cho BY_CLASS
  departmentId?: number; // Cho BY_DEPARTMENT
  subject: string; // Bắt buộc
  content: string; // Bắt buộc - Text hoặc HTML
  isHtml?: boolean; // Mặc định: false
  templateVariables?: Record<string, string>; // Biến cho template
  createNotification?: boolean; // Mặc định: false
  notificationTitle?: string; // Title cho notification
  notificationType?: NotificationType; // Type cho notification
  notificationActionUrl?: string; // URL cho notification
}
```

### 1.2. SendNotificationOnlyRequest

```typescript
interface SendNotificationOnlyRequest {
  recipientType: RecipientType; // Bắt buộc
  recipientIds?: number[]; // Cho INDIVIDUAL hoặc CUSTOM_LIST
  activityId?: number; // Cho ACTIVITY_REGISTRATIONS
  seriesId?: number; // Cho SERIES_REGISTRATIONS
  classId?: number; // Cho BY_CLASS
  departmentId?: number; // Cho BY_DEPARTMENT
  title: string; // Bắt buộc
  content: string; // Bắt buộc
  type: NotificationType; // Bắt buộc
  actionUrl?: string; // Optional
}
```

### 1.3. RecipientType Enum

```typescript
enum RecipientType {
  INDIVIDUAL = "INDIVIDUAL",              // Gửi cá nhân
  BULK = "BULK",                          // Gửi bulk (nhiều người)
  ACTIVITY_REGISTRATIONS = "ACTIVITY_REGISTRATIONS",  // Danh sách đăng ký activity
  SERIES_REGISTRATIONS = "SERIES_REGISTRATIONS",    // Danh sách đăng ký series
  ALL_STUDENTS = "ALL_STUDENTS",          // Tất cả sinh viên
  BY_CLASS = "BY_CLASS",                  // Sinh viên theo lớp
  BY_DEPARTMENT = "BY_DEPARTMENT",        // Sinh viên theo khoa
  CUSTOM_LIST = "CUSTOM_LIST"             // Danh sách user IDs tùy chọn
}
```

### 1.3.1. Sự Khác Biệt Giữa INDIVIDUAL, BULK, và CUSTOM_LIST

**Về mặt logic backend:**
- Cả 3 loại đều sử dụng `recipientIds` (mảng user IDs) để xác định người nhận
- Backend xử lý giống nhau: `userRepository.findAllById(recipientIds)`

**Về mặt UI/UX (Frontend nên phân biệt):**

1. **INDIVIDUAL** - Gửi cá nhân:
   - **Mục đích:** Gửi cho 1 hoặc vài người cụ thể
   - **UI gợi ý:** 
     - Dropdown/autocomplete để chọn 1 người
     - Có thể cho phép chọn thêm vài người nữa (nhưng ít)
     - Hiển thị: "Gửi cho: [Tên người nhận]"
   - **Use case:** Gửi email cho 1 sinh viên cụ thể, gửi thông báo cho vài người

2. **BULK** - Gửi hàng loạt:
   - **Mục đích:** Gửi cho nhiều người (có thể hàng trăm, hàng nghìn)
   - **UI gợi ý:**
     - Multi-select với search/filter
     - Có thể import từ file Excel/CSV
     - Hiển thị số lượng: "Gửi cho: 150 người"
   - **Use case:** Gửi email cho danh sách sinh viên từ file Excel, gửi thông báo cho nhiều người cùng lúc

3. **CUSTOM_LIST** - Danh sách tùy chọn:
   - **Mục đích:** Tương tự BULK, nhưng nhấn mạnh tính "tùy chọn" của danh sách
   - **UI gợi ý:**
     - Cho phép tạo/save danh sách tùy chọn
     - Có thể load danh sách đã lưu trước đó
     - Có thể kết hợp nhiều nguồn (từ class, từ department, từ activity, v.v.)
   - **Use case:** Gửi email cho danh sách đã lưu, gửi cho nhóm tùy chọn

**Tóm lại:**
- **INDIVIDUAL:** 1-10 người → UI đơn giản, chọn từng người
- **BULK:** 10+ người → UI có search, filter, import file
- **CUSTOM_LIST:** Tương tự BULK nhưng có thể save/load danh sách

### 1.4. EmailHistoryResponse

```typescript
interface EmailHistoryResponse {
  id: number;
  senderId: number;
  senderName: string;
  recipientId?: number; // nullable
  recipientEmail: string;
  subject: string;
  content: string;
  isHtml: boolean;
  recipientType: RecipientType;
  recipientCount: number; // Số lượng người nhận
  sentAt: string; // ISO datetime string
  status: EmailStatus;
  errorMessage?: string; // nullable
  notificationCreated: boolean;
  attachments: EmailAttachmentResponse[];
}
```

### 1.5. EmailAttachmentResponse

```typescript
interface EmailAttachmentResponse {
  id: number;
  fileName: string;
  fileUrl: string; // URL để download
  fileSize: number;
  contentType: string;
}
```

### 1.6. EmailStatus Enum

```typescript
enum EmailStatus {
  SUCCESS = "SUCCESS",   // Gửi thành công
  FAILED = "FAILED",     // Gửi thất bại
  PARTIAL = "PARTIAL"    // Gửi một phần (một số thành công, một số thất bại)
}
```

---

## 2. APIs Để Lấy Danh Sách Cho Các Tùy Chọn Gửi Mail/Thông Báo

### 2.1. Lấy Danh Sách Users/Students

#### 2.1.1. Lấy Tất Cả Users
**Endpoint:** `GET /api/admin/users`
**Query Params:**
- `role` (optional): Filter theo role (STUDENT, ADMIN, MANAGER)

**Response:**
```json
{
  "status": true,
  "message": "Users retrieved successfully",
  "data": [
    {
      "id": 1,
      "username": "student001",
      "email": "student001@example.com",
      "role": "STUDENT",
      ...
    }
  ]
}
```

#### 2.1.2. Lấy Tất Cả Students (Có Phân Trang)
**Endpoint:** `GET /api/students`
**Query Params:**
- `page` (default: 0)
- `size` (default: 20)
- `sortBy` (default: "id")
- `sortDir` (default: "asc")

**Response:**
```json
{
  "status": true,
  "message": "Students retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "studentCode": "SV001",
        "fullName": "Nguyễn Văn A",
        "email": "student001@example.com",
        "userId": 1,
        "classId": 1,
        "departmentId": 1,
        ...
      }
    ],
    "totalElements": 100,
    "totalPages": 5,
    "size": 20,
    "number": 0
  }
}
```

#### 2.1.3. Tìm Kiếm Students
**Endpoint:** `GET /api/students/search`
**Query Params:**
- `keyword` (required): Tìm theo tên hoặc mã sinh viên
- `page` (default: 0)
- `size` (default: 20)

**Ví dụ:**
```bash
GET /api/students/search?keyword=Nguyễn&page=0&size=20
```

#### 2.1.4. Lấy Students Theo Department
**Endpoint:** `GET /api/students/department/{departmentId}`
**Query Params:**
- `page` (default: 0)
- `size` (default: 20)

#### 2.1.5. Lấy Students Trong Class
**Endpoint:** `GET /api/classes/{classId}/students`

**Response:**
```json
{
  "status": true,
  "message": "Students retrieved successfully",
  "data": [
    {
      "id": 1,
      "studentCode": "SV001",
      "fullName": "Nguyễn Văn A",
      ...
    }
  ]
}
```

### 2.2. Lấy Danh Sách Departments

#### 2.2.1. Lấy Tất Cả Departments
**Endpoint:** `GET /api/departments`

**Response:**
```json
[
  {
    "id": 1,
    "name": "Khoa Công nghệ Thông tin",
    "code": "CNTT",
    ...
  }
]
```

#### 2.2.2. Lấy Department Theo ID
**Endpoint:** `GET /api/departments/{id}`

### 2.3. Lấy Danh Sách Classes

#### 2.3.1. Lấy Tất Cả Classes
**Endpoint:** `GET /api/classes`

**Response:**
```json
{
  "status": true,
  "message": "Classes retrieved successfully",
  "data": [
    {
      "id": 1,
      "className": "CNTT2024A",
      "description": "Lớp Công nghệ Thông tin 2024A",
      "departmentId": 1,
      ...
    }
  ]
}
```

#### 2.3.2. Lấy Classes Theo Department
**Endpoint:** `GET /api/classes/department/{departmentId}`

#### 2.3.3. Lấy Class Theo ID
**Endpoint:** `GET /api/classes/{classId}`

### 2.4. Lấy Danh Sách Activities

#### 2.4.1. Lấy Tất Cả Activities
**Endpoint:** `GET /api/activities`

**Response:**
```json
{
  "status": true,
  "message": "Activities retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "Sự kiện ABC",
      "type": "WORKSHOP",
      "startDate": "2025-02-01T08:00:00",
      "endDate": "2025-02-01T17:00:00",
      ...
    }
  ]
}
```

#### 2.4.2. Lấy Activity Theo ID
**Endpoint:** `GET /api/activities/{id}`

#### 2.4.3. Lấy Activities Theo ScoreType
**Endpoint:** `GET /api/activities/score-type/{scoreType}`
**Path Variables:**
- `scoreType`: `REN_LUYEN`, `CONG_TAC_XA_HOI`, hoặc `CHUYEN_DE`

#### 2.4.4. Lấy Activities Theo Department
**Endpoint:** `GET /api/activities/department/{deptId}`

### 2.5. Lấy Danh Sách Activity Series

#### 2.5.1. Lấy Tất Cả Series
**Endpoint:** `GET /api/series`

**Response:**
```json
{
  "status": true,
  "message": "Series retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "Chuỗi sự kiện mùa hè 2025",
      "description": "Các sự kiện trong mùa hè",
      "milestonePoints": "{\"3\": 5, \"4\": 7, \"5\": 10}",
      "scoreType": "REN_LUYEN",
      ...
    }
  ]
}
```

#### 2.5.2. Lấy Series Theo ID
**Endpoint:** `GET /api/series/{seriesId}`

#### 2.5.3. Lấy Activities Trong Series
**Endpoint:** `GET /api/series/{seriesId}/activities`

### 2.6. Lấy Danh Sách Activity Registrations

#### 2.6.1. Lấy Registrations Theo Activity
**Endpoint:** `GET /api/registrations/activity/{activityId}`

**Response:**
```json
{
  "status": true,
  "message": "Registrations retrieved successfully",
  "data": [
    {
      "id": 1,
      "activityId": 1,
      "studentId": 123,
      "status": "APPROVED",
      "ticketCode": "TICKET001",
      "student": {
        "id": 123,
        "studentCode": "SV001",
        "fullName": "Nguyễn Văn A",
        "userId": 1,
        ...
      },
      ...
    }
  ]
}
```

**Lưu ý:** API này trả về danh sách đăng ký, từ đó có thể extract danh sách `studentId` hoặc `userId` để dùng cho `recipientIds`.

### 2.7. Service Functions Để Lấy Danh Sách

```typescript
// services/emailRecipientService.ts

// Lấy tất cả users
export const getAllUsers = async (role?: string): Promise<User[]> => {
  const url = role 
    ? `/api/admin/users?role=${role}`
    : '/api/admin/users';
  
  const response = await fetch(url, {
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`,
    },
  });
  
  const data = await response.json();
  return data.status ? data.data : [];
};

// Lấy tất cả students (có phân trang)
export const getAllStudents = async (
  page: number = 0,
  size: number = 20
): Promise<{ content: Student[], totalElements: number }> => {
  const response = await fetch(`/api/students?page=${page}&size=${size}`, {
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`,
    },
  });
  
  const data = await response.json();
  return data.status ? data.data : { content: [], totalElements: 0 };
};

// Tìm kiếm students
export const searchStudents = async (
  keyword: string,
  page: number = 0,
  size: number = 20
): Promise<{ content: Student[], totalElements: number }> => {
  const response = await fetch(
    `/api/students/search?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`,
    {
      headers: {
        'Authorization': `Bearer ${getAuthToken()}`,
      },
    }
  );
  
  const data = await response.json();
  return data.status ? data.data : { content: [], totalElements: 0 };
};

// Lấy students theo department
export const getStudentsByDepartment = async (
  departmentId: number,
  page: number = 0,
  size: number = 20
): Promise<{ content: Student[], totalElements: number }> => {
  const response = await fetch(
    `/api/students/department/${departmentId}?page=${page}&size=${size}`,
    {
      headers: {
        'Authorization': `Bearer ${getAuthToken()}`,
      },
    }
  );
  
  const data = await response.json();
  return data.status ? data.data : { content: [], totalElements: 0 };
};

// Lấy students trong class
export const getStudentsInClass = async (classId: number): Promise<Student[]> => {
  const response = await fetch(`/api/classes/${classId}/students`, {
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`,
    },
  });
  
  const data = await response.json();
  return data.status ? data.data : [];
};

// Lấy tất cả departments
export const getAllDepartments = async (): Promise<Department[]> => {
  const response = await fetch('/api/departments', {
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`,
    },
  });
  
  return response.json();
};

// Lấy tất cả classes
export const getAllClasses = async (): Promise<StudentClass[]> => {
  const response = await fetch('/api/classes', {
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`,
    },
  });
  
  const data = await response.json();
  return data.status ? data.data : [];
};

// Lấy classes theo department
export const getClassesByDepartment = async (departmentId: number): Promise<StudentClass[]> => {
  const response = await fetch(`/api/classes/department/${departmentId}`, {
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`,
    },
  });
  
  const data = await response.json();
  return data.status ? data.data : [];
};

// Lấy tất cả activities
export const getAllActivities = async (): Promise<Activity[]> => {
  const response = await fetch('/api/activities', {
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`,
    },
  });
  
  const data = await response.json();
  return data.status ? data.data : [];
};

// Lấy tất cả series
export const getAllSeries = async (): Promise<ActivitySeries[]> => {
  const response = await fetch('/api/series', {
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`,
    },
  });
  
  const data = await response.json();
  return data.status ? data.data : [];
};

// Lấy registrations theo activity (để extract danh sách students)
export const getActivityRegistrations = async (activityId: number): Promise<ActivityRegistration[]> => {
  const response = await fetch(`/api/registrations/activity/${activityId}`, {
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`,
    },
  });
  
  const data = await response.json();
  return data.status ? data.data : [];
};
```

---     

## 3. API Endpoints (Email & Notification)

### 2.1. Gửi Email

**Endpoint:** `POST /api/emails/send`

**Authentication:** Required (ADMIN, MANAGER only)

**Content-Type:** `multipart/form-data`

**Request:**
- `request` (JSON string): SendEmailRequest object
- `attachments` (File[]): Danh sách file đính kèm (optional, max 10MB per file)

**Response Success (200):**
```json
{
  "status": true,
  "message": "Email sent to 50 recipients (48 success, 2 failed)",
  "data": {
    "totalRecipients": 50,
    "successCount": 48,
    "failedCount": 2,
    "status": "PARTIAL",
    "emailHistories": [
      {
        "id": 1,
        "senderId": 1,
        "senderName": "admin",
        "recipientId": 123,
        "recipientEmail": "student@example.com",
        "subject": "Thông báo sự kiện",
        "content": "Nội dung email...",
        "isHtml": true,
        "recipientType": "ACTIVITY_REGISTRATIONS",
        "recipientCount": 1,
        "sentAt": "2025-12-05T10:30:00",
        "status": "SUCCESS",
        "notificationCreated": true,
        "attachments": []
      }
    ]
  }
}
```

**Response Error (400):**
```json
{
  "status": false,
  "message": "Subject is required",
  "data": null
}
```

**Các lỗi có thể xảy ra:**
- `"Subject is required"` - Thiếu subject
- `"Content is required"` - Thiếu content
- `"Recipient type is required"` - Thiếu recipientType
- `"No recipients found"` - Không tìm thấy người nhận
- `"User not found"` - Không tìm thấy user từ authentication

**Ví dụ sử dụng với FormData:**

```typescript
const sendEmail = async (request: SendEmailRequest, attachments?: File[]) => {
  const formData = new FormData();
  
  // Convert request to JSON string
  formData.append('request', JSON.stringify(request));
  
  // Add attachments if any
  if (attachments && attachments.length > 0) {
    attachments.forEach((file) => {
      formData.append('attachments', file);
    });
  }
  
  const response = await fetch('/api/emails/send', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`,
    },
    body: formData,
  });
  
  return response.json();
};
```

### 2.2. Chỉ Tạo Notification (Không Gửi Email)

**Endpoint:** `POST /api/emails/notifications/send`

**Authentication:** Required (ADMIN, MANAGER only)

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "recipientType": "ACTIVITY_REGISTRATIONS",
  "activityId": 1,
  "title": "Thông báo sự kiện",
  "content": "Nội dung thông báo",
  "type": "SYSTEM_ANNOUNCEMENT",
  "actionUrl": "/activities/1"
}
```

**Response Success (200):**
```json
{
  "status": true,
  "message": "Notification sent to 50 recipients (50 success, 0 failed)",
  "data": {
    "totalRecipients": 50,
    "successCount": 50,
    "failedCount": 0
  }
}
```

### 2.3. Lấy Lịch Sử Email

**Endpoint:** `GET /api/emails/history`

**Authentication:** Required (ADMIN, MANAGER only)

**Query Parameters:**
- `page` (optional, default: 0) - Số trang
- `size` (optional, default: 20) - Số lượng mỗi trang

**Response Success (200):**
```json
{
  "status": true,
  "message": "Email history retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "senderId": 1,
        "senderName": "admin",
        "recipientId": 123,
        "recipientEmail": "student@example.com",
        "subject": "Thông báo sự kiện",
        "content": "Nội dung email...",
        "isHtml": true,
        "recipientType": "ACTIVITY_REGISTRATIONS",
        "recipientCount": 1,
        "sentAt": "2025-12-05T10:30:00",
        "status": "SUCCESS",
        "notificationCreated": true,
        "attachments": []
      }
    ],
    "totalElements": 100,
    "totalPages": 5,
    "size": 20,
    "number": 0
  }
}
```

### 2.4. Xem Chi Tiết Email

**Endpoint:** `GET /api/emails/history/{emailId}`

**Authentication:** Required (ADMIN, MANAGER only)

**Response Success (200):**
```json
{
  "status": true,
  "message": "Email history retrieved successfully",
  "data": {
    "id": 1,
    "senderId": 1,
    "senderName": "admin",
    "recipientId": 123,
    "recipientEmail": "student@example.com",
    "subject": "Thông báo sự kiện",
    "content": "Nội dung email...",
    "isHtml": true,
    "recipientType": "ACTIVITY_REGISTRATIONS",
    "recipientCount": 1,
    "sentAt": "2025-12-05T10:30:00",
    "status": "SUCCESS",
    "notificationCreated": true,
    "attachments": [
      {
        "id": 1,
        "fileName": "document.pdf",
        "fileUrl": "http://localhost:8080/api/emails/attachments/1/download",
        "fileSize": 1024000,
        "contentType": "application/pdf"
      }
    ]
  }
}
```

### 2.5. Gửi Lại Email

**Endpoint:** `POST /api/emails/history/{emailId}/resend`

**Authentication:** Required (ADMIN, MANAGER only)

**Response Success (200):**
```json
{
  "status": true,
  "message": "Email resent successfully",
  "data": {
    "id": 1,
    "senderId": 1,
    "senderName": "admin",
    "recipientId": 123,
    "recipientEmail": "student@example.com",
    "subject": "Thông báo sự kiện",
    "content": "Nội dung email...",
    "isHtml": true,
    "recipientType": "ACTIVITY_REGISTRATIONS",
    "recipientCount": 1,
    "sentAt": "2025-12-05T11:00:00",
    "status": "SUCCESS",
    "notificationCreated": true,
    "attachments": []
  }
}
```

### 2.6. Download File Đính Kèm

**Endpoint:** `GET /api/emails/attachments/{attachmentId}/download`

**Authentication:** Required (ADMIN, MANAGER only)

**Response:** File download (binary)

**Ví dụ sử dụng:**
```typescript
const downloadAttachment = async (attachmentId: number, fileName: string) => {
  const response = await fetch(`/api/emails/attachments/${attachmentId}/download`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`,
    },
  });
  
  if (response.ok) {
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  }
};
```

---

## 4. Template Variables

Hệ thống hỗ trợ các biến template trong email content và subject:

### 3.1. Biến Mặc Định

- `{{studentName}}` - Tên sinh viên
- `{{studentCode}}` - Mã sinh viên
- `{{activityName}}` - Tên sự kiện (nếu có activityId)
- `{{activityDate}}` - Ngày sự kiện (nếu có activityId)
- `{{seriesName}}` - Tên series (nếu có seriesId)
- `{{className}}` - Tên lớp (nếu student có class)
- `{{departmentName}}` - Tên khoa (nếu student có department)

### 3.2. Biến Tùy Chọn

Có thể thêm biến tùy chọn qua `templateVariables`:

```typescript
const request: SendEmailRequest = {
  recipientType: RecipientType.ACTIVITY_REGISTRATIONS,
  activityId: 1,
  subject: "Thông báo sự kiện {{activityName}}",
  content: "Xin chào {{studentName}}, bạn đã đăng ký sự kiện {{activityName}} vào ngày {{activityDate}}.",
  isHtml: true,
  templateVariables: {
    customVariable: "Giá trị tùy chọn"
  }
};
```

### 3.3. Ví Dụ Template HTML

```html
<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
  <h2 style="color: #007bff;">Xin chào {{studentName}}!</h2>
  <p>Bạn đã đăng ký thành công sự kiện <strong>{{activityName}}</strong>.</p>
  <p>Thời gian: {{activityDate}}</p>
  <p>Lớp: {{className}}</p>
  <p>Trân trọng,<br>CampusLife Team</p>
</div>
```

---

## 5. UI Components

### 4.1. Component Chọn Recipients

```typescript
import { useState, useEffect } from 'react';
import { getAllUsers, getAllStudents, searchStudents, getAllDepartments, getAllClasses, getAllActivities, getAllSeries } from '../services/emailRecipientService';

const RecipientSelector = ({ 
  recipientType, 
  onRecipientsChange 
}: { 
  recipientType: RecipientType;
  onRecipientsChange: (recipientIds: number[]) => void;
}) => {
  const [selectedRecipientIds, setSelectedRecipientIds] = useState<number[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [classes, setClasses] = useState<StudentClass[]>([]);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [series, setSeries] = useState<ActivitySeries[]>([]);
  const [students, setStudents] = useState<Student[]>([]);
  const [searchKeyword, setSearchKeyword] = useState('');

  useEffect(() => {
    loadInitialData();
  }, [recipientType]);

  const loadInitialData = async () => {
    if (recipientType === RecipientType.BY_DEPARTMENT) {
      const depts = await getAllDepartments();
      setDepartments(depts);
    } else if (recipientType === RecipientType.BY_CLASS) {
      const cls = await getAllClasses();
      setClasses(cls);
    } else if (recipientType === RecipientType.ACTIVITY_REGISTRATIONS) {
      const acts = await getAllActivities();
      setActivities(acts);
    } else if (recipientType === RecipientType.SERIES_REGISTRATIONS) {
      const srs = await getAllSeries();
      setSeries(srs);
    }
  };

  const handleSearchStudents = async () => {
    if (searchKeyword.trim()) {
      const result = await searchStudents(searchKeyword, 0, 50);
      setStudents(result.content);
    }
  };

  const handleSelectRecipient = (userId: number) => {
    const newIds = selectedRecipientIds.includes(userId)
      ? selectedRecipientIds.filter(id => id !== userId)
      : [...selectedRecipientIds, userId];
    setSelectedRecipientIds(newIds);
    onRecipientsChange(newIds);
  };

  const renderSelector = () => {
    switch (recipientType) {
      case RecipientType.INDIVIDUAL:
      case RecipientType.BULK:
      case RecipientType.CUSTOM_LIST:
        return (
          <div>
            <div>
              <input
                type="text"
                placeholder="Tìm kiếm sinh viên..."
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
              />
              <button onClick={handleSearchStudents}>Tìm kiếm</button>
            </div>
            <div>
              {students.map((student) => (
                <div key={student.id}>
                  <label>
                    <input
                      type="checkbox"
                      checked={selectedRecipientIds.includes(student.userId)}
                      onChange={() => handleSelectRecipient(student.userId)}
                    />
                    {student.fullName} ({student.studentCode})
                  </label>
                </div>
              ))}
            </div>
            <p>Đã chọn: {selectedRecipientIds.length} người</p>
          </div>
        );

      case RecipientType.BY_DEPARTMENT:
        return (
          <select onChange={(e) => {
            const deptId = parseInt(e.target.value);
            // Load students by department
            getStudentsByDepartment(deptId).then(result => {
              const userIds = result.content.map(s => s.userId).filter(id => id != null);
              setSelectedRecipientIds(userIds);
              onRecipientsChange(userIds);
            });
          }}>
            <option value="">Chọn khoa</option>
            {departments.map((dept) => (
              <option key={dept.id} value={dept.id}>
                {dept.name}
              </option>
            ))}
          </select>
        );

      case RecipientType.BY_CLASS:
        return (
          <select onChange={(e) => {
            const classId = parseInt(e.target.value);
            // Load students in class
            getStudentsInClass(classId).then(students => {
              const userIds = students.map(s => s.userId).filter(id => id != null);
              setSelectedRecipientIds(userIds);
              onRecipientsChange(userIds);
            });
          }}>
            <option value="">Chọn lớp</option>
            {classes.map((cls) => (
              <option key={cls.id} value={cls.id}>
                {cls.className}
              </option>
            ))}
          </select>
        );

      case RecipientType.ACTIVITY_REGISTRATIONS:
        return (
          <select onChange={(e) => {
            const activityId = parseInt(e.target.value);
            // Load registrations and extract user IDs
            getActivityRegistrations(activityId).then(regs => {
              const userIds = regs
                .map(reg => reg.student?.userId)
                .filter(id => id != null) as number[];
              setSelectedRecipientIds(userIds);
              onRecipientsChange(userIds);
            });
          }}>
            <option value="">Chọn sự kiện</option>
            {activities.map((act) => (
              <option key={act.id} value={act.id}>
                {act.name}
              </option>
            ))}
          </select>
        );

      case RecipientType.SERIES_REGISTRATIONS:
        return (
          <select onChange={(e) => {
            const seriesId = parseInt(e.target.value);
            // Load series registrations (similar to activity)
            // Note: You may need to create a similar API for series registrations
          }}>
            <option value="">Chọn chuỗi sự kiện</option>
            {series.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        );

      case RecipientType.ALL_STUDENTS:
        return (
          <div>
            <p>Tất cả sinh viên sẽ nhận email/thông báo</p>
            <p>Không cần chọn thêm</p>
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div>
      <h3>Chọn người nhận</h3>
      {renderSelector()}
    </div>
  );
};
```

### 4.2. Form Gửi Email

```typescript
import { useState } from 'react';

const SendEmailForm = () => {
  const [formData, setFormData] = useState<SendEmailRequest>({
    recipientType: RecipientType.INDIVIDUAL,
    subject: '',
    content: '',
    isHtml: false,
    createNotification: false,
  });
  const [attachments, setAttachments] = useState<File[]>([]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    try {
      const response = await sendEmail(formData, attachments);
      if (response.status) {
        alert('Email sent successfully!');
        // Reset form
      } else {
        alert(`Error: ${response.message}`);
      }
    } catch (error) {
      alert('Failed to send email');
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {/* Recipient Type Selection */}
      <div>
        <label>Recipient Type:</label>
        <select
          value={formData.recipientType}
          onChange={(e) => setFormData({ ...formData, recipientType: e.target.value as RecipientType })}
        >
          <option value={RecipientType.INDIVIDUAL}>Individual</option>
          <option value={RecipientType.ACTIVITY_REGISTRATIONS}>Activity Registrations</option>
          <option value={RecipientType.SERIES_REGISTRATIONS}>Series Registrations</option>
          <option value={RecipientType.ALL_STUDENTS}>All Students</option>
          <option value={RecipientType.BY_CLASS}>By Class</option>
          <option value={RecipientType.BY_DEPARTMENT}>By Department</option>
          <option value={RecipientType.CUSTOM_LIST}>Custom List</option>
        </select>
      </div>

      {/* Recipient Selector Component */}
      <RecipientSelector
        recipientType={formData.recipientType}
        onRecipientsChange={(recipientIds) => {
          setFormData({ ...formData, recipientIds });
        }}
      />

      {/* Conditional Fields based on recipientType */}
      {(formData.recipientType === RecipientType.BY_CLASS || 
        formData.recipientType === RecipientType.BY_DEPARTMENT ||
        formData.recipientType === RecipientType.ACTIVITY_REGISTRATIONS ||
        formData.recipientType === RecipientType.SERIES_REGISTRATIONS) && (
        <div>
          <p>Vui lòng chọn từ dropdown ở trên</p>
        </div>
      )}

      {/* Subject */}
      <div>
        <label>Subject:</label>
        <input
          type="text"
          value={formData.subject}
          onChange={(e) => setFormData({ ...formData, subject: e.target.value })}
          required
        />
      </div>

      {/* Content */}
      <div>
        <label>Content:</label>
        {formData.isHtml ? (
          <textarea
            value={formData.content}
            onChange={(e) => setFormData({ ...formData, content: e.target.value })}
            rows={10}
            required
          />
        ) : (
          <textarea
            value={formData.content}
            onChange={(e) => setFormData({ ...formData, content: e.target.value })}
            rows={10}
            required
          />
        )}
      </div>

      {/* HTML Toggle */}
      <div>
        <label>
          <input
            type="checkbox"
            checked={formData.isHtml}
            onChange={(e) => setFormData({ ...formData, isHtml: e.target.checked })}
          />
          HTML Content
        </label>
      </div>

      {/* Attachments */}
      <div>
        <label>Attachments (max 10MB per file):</label>
        <input
          type="file"
          multiple
          onChange={(e) => {
            const files = Array.from(e.target.files || []);
            setAttachments(files);
          }}
        />
      </div>

      {/* Notification Options */}
      <div>
        <label>
          <input
            type="checkbox"
            checked={formData.createNotification}
            onChange={(e) => setFormData({ ...formData, createNotification: e.target.checked })}
          />
          Create Notification
        </label>
      </div>

      {formData.createNotification && (
        <>
          <div>
            <label>Notification Title:</label>
            <input
              type="text"
              value={formData.notificationTitle || ''}
              onChange={(e) => setFormData({ ...formData, notificationTitle: e.target.value })}
            />
          </div>
          <div>
            <label>Notification Type:</label>
            <select
              value={formData.notificationType || 'SYSTEM_ANNOUNCEMENT'}
              onChange={(e) => setFormData({ ...formData, notificationType: e.target.value as NotificationType })}
            >
              <option value="SYSTEM_ANNOUNCEMENT">System Announcement</option>
              <option value="ACTIVITY_REGISTRATION">Activity Registration</option>
              <option value="GENERAL">General</option>
            </select>
          </div>
        </>
      )}

      <button type="submit">Send Email</button>
    </form>
  );
};
```

### 4.3. Form Tạo Notification (Không Gửi Email)

```typescript
const SendNotificationOnlyForm = () => {
  const [formData, setFormData] = useState<SendNotificationOnlyRequest>({
    recipientType: RecipientType.INDIVIDUAL,
    title: '',
    content: '',
    type: NotificationType.SYSTEM_ANNOUNCEMENT,
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    try {
      const response = await fetch('/api/emails/notifications/send', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getAuthToken()}`,
        },
        body: JSON.stringify(formData),
      });
      
      const data = await response.json();
      if (data.status) {
        alert('Notification sent successfully!');
      } else {
        alert(`Error: ${data.message}`);
      }
    } catch (error) {
      alert('Failed to send notification');
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {/* Similar fields as SendEmailForm but without email-specific fields */}
      {/* ... */}
    </form>
  );
};
```

### 4.4. Email History List

```typescript
const EmailHistoryList = () => {
  const [history, setHistory] = useState<EmailHistoryResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    fetchHistory();
  }, [page]);

  const fetchHistory = async () => {
    try {
      const response = await fetch(`/api/emails/history?page=${page}&size=20`, {
        headers: {
          'Authorization': `Bearer ${getAuthToken()}`,
        },
      });
      
      const data = await response.json();
      if (data.status) {
        setHistory(data.data.content);
        setTotalPages(data.data.totalPages);
      }
    } catch (error) {
      console.error('Failed to fetch email history:', error);
    }
  };

  return (
    <div>
      <h2>Email History</h2>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Recipient</th>
            <th>Subject</th>
            <th>Status</th>
            <th>Sent At</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {history.map((email) => (
            <tr key={email.id}>
              <td>{email.id}</td>
              <td>{email.recipientEmail}</td>
              <td>{email.subject}</td>
              <td>{email.status}</td>
              <td>{new Date(email.sentAt).toLocaleString()}</td>
              <td>
                <button onClick={() => viewEmailDetail(email.id)}>View</button>
                {email.status === EmailStatus.FAILED && (
                  <button onClick={() => resendEmail(email.id)}>Resend</button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      
      {/* Pagination */}
      <div>
        <button disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
        <span>Page {page + 1} of {totalPages}</span>
        <button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>Next</button>
      </div>
    </div>
  );
};
```

---

## 6. Service Functions (Email & Notification)

### 5.1. Send Email Service

```typescript
// services/emailService.ts

export const sendEmail = async (
  request: SendEmailRequest,
  attachments?: File[]
): Promise<Response<EmailSendResult>> => {
  const formData = new FormData();
  
  // Convert request to JSON string
  formData.append('request', JSON.stringify(request));
  
  // Add attachments if any
  if (attachments && attachments.length > 0) {
    attachments.forEach((file) => {
      formData.append('attachments', file);
    });
  }
  
  const response = await fetch('/api/emails/send', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`,
    },
    body: formData,
  });
  
  return response.json();
};
```

### 5.2. Send Notification Only Service

```typescript
export const sendNotificationOnly = async (
  request: SendNotificationOnlyRequest
): Promise<Response<NotificationSendResult>> => {
  const response = await fetch('/api/emails/notifications/send', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getAuthToken()}`,
    },
    body: JSON.stringify(request),
  });
  
  return response.json();
};
```

### 5.3. Get Email History Service

```typescript
export const getEmailHistory = async (
  page: number = 0,
  size: number = 20
): Promise<Response<EmailHistoryPage>> => {
  const response = await fetch(`/api/emails/history?page=${page}&size=${size}`, {
    headers: {
      'Authorization': `Bearer ${getAuthToken()}`,
    },
  });
  
  return response.json();
};
```

---

## 7. Use Cases

### 6.1. Gửi Email Cho Tất Cả Sinh Viên Đã Đăng Ký Activity

```typescript
const sendEmailToActivityRegistrations = async (activityId: number) => {
  const request: SendEmailRequest = {
    recipientType: RecipientType.ACTIVITY_REGISTRATIONS,
    activityId: activityId,
    subject: "Thông báo sự kiện {{activityName}}",
    content: `
      <div>
        <h2>Xin chào {{studentName}}!</h2>
        <p>Bạn đã đăng ký thành công sự kiện <strong>{{activityName}}</strong>.</p>
        <p>Thời gian: {{activityDate}}</p>
        <p>Vui lòng có mặt đúng giờ!</p>
      </div>
    `,
    isHtml: true,
    createNotification: true,
    notificationTitle: "Thông báo sự kiện {{activityName}}",
    notificationType: NotificationType.ACTIVITY_REGISTRATION,
    notificationActionUrl: `/activities/${activityId}`,
  };
  
  const response = await sendEmail(request);
  return response;
};
```

### 6.2. Gửi Email Cho Sinh Viên Theo Lớp

```typescript
const sendEmailToClass = async (classId: number) => {
  const request: SendEmailRequest = {
    recipientType: RecipientType.BY_CLASS,
    classId: classId,
    subject: "Thông báo quan trọng cho lớp {{className}}",
    content: "Nội dung thông báo...",
    isHtml: false,
    createNotification: true,
    notificationType: NotificationType.SYSTEM_ANNOUNCEMENT,
  };
  
  const response = await sendEmail(request);
  return response;
};
```

### 6.3. Chỉ Tạo Notification (Không Gửi Email)

```typescript
const createNotificationOnly = async () => {
  const request: SendNotificationOnlyRequest = {
    recipientType: RecipientType.ALL_STUDENTS,
    title: "Thông báo hệ thống",
    content: "Hệ thống sẽ bảo trì vào ngày mai.",
    type: NotificationType.SYSTEM_ANNOUNCEMENT,
    actionUrl: "/announcements",
  };
  
  const response = await sendNotificationOnly(request);
  return response;
};
```

---

## 8. Error Handling

### 7.1. Validation Errors

```typescript
try {
  const response = await sendEmail(request, attachments);
  if (!response.status) {
    // Handle validation errors
    switch (response.message) {
      case "Subject is required":
        alert("Vui lòng nhập tiêu đề email");
        break;
      case "Content is required":
        alert("Vui lòng nhập nội dung email");
        break;
      case "No recipients found":
        alert("Không tìm thấy người nhận. Vui lòng kiểm tra lại filter.");
        break;
      default:
        alert(`Lỗi: ${response.message}`);
    }
  }
} catch (error) {
  console.error("Failed to send email:", error);
  alert("Đã xảy ra lỗi khi gửi email. Vui lòng thử lại.");
}
```

### 7.2. File Size Validation

```typescript
const validateAttachments = (files: File[]): string | null => {
  const maxSize = 10 * 1024 * 1024; // 10MB
  
  for (const file of files) {
    if (file.size > maxSize) {
      return `File ${file.name} vượt quá 10MB. Vui lòng chọn file nhỏ hơn.`;
    }
  }
  
  return null;
};

// Usage
const files = Array.from(e.target.files || []);
const error = validateAttachments(files);
if (error) {
  alert(error);
  return;
}
setAttachments(files);
```

---

## 9. Best Practices

1. **Validate Input:** Luôn validate recipientType và các filter tương ứng trước khi gửi
2. **Show Loading State:** Hiển thị loading khi đang gửi email (có thể mất thời gian với nhiều recipients)
3. **Preview Template:** Cho phép preview email với sample data trước khi gửi
4. **File Size Warning:** Cảnh báo user nếu file đính kèm quá lớn
5. **Template Editor:** Sử dụng rich text editor cho HTML content
6. **Recipient Count:** Hiển thị số lượng recipients sẽ nhận email trước khi gửi
7. **Error Recovery:** Cho phép resend email nếu gửi thất bại
8. **History Tracking:** Lưu draft email để có thể chỉnh sửa và gửi lại sau

---

## 10. Testing

### 9.1. Test với cURL

**Gửi email:**
```bash
curl --location 'http://localhost:8080/api/emails/send' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--form 'request="{\"recipientType\":\"INDIVIDUAL\",\"recipientIds\":[1,2,3],\"subject\":\"Test Email\",\"content\":\"This is a test email\",\"isHtml\":false}"' \
--form 'attachments=@"/path/to/file.pdf"'
```

**Chỉ tạo notification:**
```bash
curl --location 'http://localhost:8080/api/emails/notifications/send' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "recipientType": "ALL_STUDENTS",
  "title": "Test Notification",
  "content": "This is a test notification",
  "type": "SYSTEM_ANNOUNCEMENT"
}'
```

---

## Tóm Tắt

- ✅ Hỗ trợ nhiều loại recipients (individual, activity registrations, series registrations, all students, by class, by department, custom list)
- ✅ Gửi email với HTML template và biến động
- ✅ Đính kèm file (max 10MB per file)
- ✅ Tùy chọn tạo notification khi gửi email
- ✅ Chỉ tạo notification (không gửi email)
- ✅ Lưu lịch sử email và cho phép resend
- ✅ Download attachments
- ✅ Phân trang lịch sử email

