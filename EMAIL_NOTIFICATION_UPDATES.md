# Cập Nhật Hệ Thống Email và Notification

## Tổng Quan

Tài liệu này mô tả các cập nhật mới nhất cho hệ thống gửi email và notification, bao gồm các cải tiến về DTO, template variables, logic lấy danh sách recipients, và tối ưu hóa chức năng gửi thông báo.

---

## 1. DTO Mới: StudentListResponse

### Mô Tả
Tạo DTO mới `StudentListResponse` để chuẩn hóa response khi lấy danh sách sinh viên theo khoa, thay thế cho việc trả về `Map` trước đây.

### File
- `src/main/java/vn/campuslife/model/StudentListResponse.java`

### Cấu Trúc
```java
public class StudentListResponse {
    private List<StudentResponse> content;
    private Long totalElements;
    private Integer totalPages;
    private Integer size;
    private Integer number;
    private Boolean first;
    private Boolean last;
}
```

### API Cập Nhật
- **Endpoint:** `GET /api/students/department/{departmentId}?page=0&size=20`
- **Response Format:**
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
          "phone": "0123456789",
          "departmentName": "Khoa Công nghệ Thông tin",
          "className": "CNTT2024A",
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

### Lợi Ích
- Response structure rõ ràng và nhất quán
- Dễ dàng parse và sử dụng ở frontend
- Type-safe với DTO thay vì Map

---

## 2. Biến Template {{email}}

### Mô Tả
Thêm biến template `{{email}}` vào hệ thống, cho phép sử dụng email của người nhận trong nội dung email và notification.

### Thay Đổi Code
- **File:** `src/main/java/vn/campuslife/service/impl/EmailServiceImpl.java`
- **Method:** `buildTemplateVariables()` và `buildTemplateVariablesForNotification()`
- **Thay đổi:**
  ```java
  // Add email for all recipients
  vars.put("email", recipient.getEmail() != null ? recipient.getEmail() : "");
  ```

### Cách Sử Dụng
```typescript
const request: SendEmailRequest = {
  recipientType: 'BULK',
  recipientIds: [123],
  subject: "Thông báo quan trọng",
  content: "Xin chào, email của bạn là: {{email}}",
  isHtml: false
};
```

### Các Biến Template Có Sẵn
Bây giờ hệ thống hỗ trợ các biến sau:

- `{{email}}` - **MỚI** - Email của người nhận (luôn có)
- `{{studentName}}` - Tên sinh viên (nếu recipient là student)
- `{{studentCode}}` - Mã sinh viên (nếu recipient là student)
- `{{className}}` - Tên lớp (nếu recipient là student và có lớp)
- `{{departmentName}}` - Tên khoa (nếu recipient là student và có khoa)
- `{{activityName}}` - Tên activity (nếu gửi theo `ACTIVITY_REGISTRATIONS`)
- `{{activityDate}}` - Ngày bắt đầu activity (nếu có)
- `{{seriesName}}` - Tên series (nếu gửi theo `SERIES_REGISTRATIONS`)

---

## 3. Logic Lấy Danh Sách Đăng Ký

### Mô Tả
Làm rõ logic lấy danh sách sinh viên đã đăng ký activity/series khi gửi email/notification.

### Logic Hiện Tại
- **Không filter theo status**: Hệ thống lấy tất cả registrations, bao gồm:
  - `PENDING` - Đang chờ duyệt
  - `APPROVED` - Đã được duyệt
  - `REJECTED` - Đã bị từ chối
  - `CANCELLED` - Đã hủy

- **Gửi cho tất cả đã đăng ký**: Dù chưa duyệt hay đã duyệt, tất cả sinh viên đã đăng ký đều nhận được email/notification.

### Code Comments
Đã thêm comment rõ ràng trong code:
```java
case ACTIVITY_REGISTRATIONS:
    // Lấy tất cả sinh viên đã đăng ký activity (không filter theo status)
    // Bao gồm cả PENDING, APPROVED, REJECTED, CANCELLED
    // Logic: Gửi cho tất cả đã đăng ký, dù chưa duyệt hay đã duyệt
    ...
```

### Use Cases
- **Thông báo nhắc nhở**: Gửi cho tất cả đã đăng ký, kể cả chưa duyệt
- **Thông báo thay đổi**: Thông báo thay đổi lịch cho tất cả đã đăng ký
- **Thông báo hủy**: Thông báo hủy sự kiện cho tất cả đã đăng ký

### Lưu Ý
Nếu cần filter theo status (ví dụ: chỉ gửi cho `APPROVED`), cần:
1. Tạo query mới trong `ActivityRegistrationRepository`
2. Cập nhật logic trong `EmailServiceImpl.getRecipients()`

---

## 4. API Lấy Khoa Cho Tất Cả Roles

### Mô Tả
Xác nhận và làm rõ API lấy danh sách khoa có thể truy cập bởi tất cả roles.

### Endpoints

#### 1. Public Endpoint (Tất Cả Roles)
- **Endpoint:** `GET /api/departments`
- **Access:** Public (permitAll)
- **Roles:** ADMIN, MANAGER, STUDENT đều có thể truy cập
- **Response:** `List<Department>`
- **Example:**
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

#### 2. Admin Endpoint (ADMIN, MANAGER)
- **Endpoint:** `GET /api/admin/departments`
- **Access:** ADMIN, MANAGER
- **Response:** `Response` wrapper với `List<Department>`
- **Example:**
  ```json
  {
    "status": true,
    "message": "Departments retrieved",
    "data": [...]
  }
  ```

### Security Config
```java
.requestMatchers("/api/departments/**").permitAll()
.requestMatchers("/api/admin/departments/**").hasAnyRole("ADMIN", "MANAGER")
```

### Lợi Ích
- Frontend có thể sử dụng `/api/departments` cho tất cả users
- Không cần authentication để lấy danh sách khoa
- Admin/Manager có thể dùng `/api/admin/departments` nếu cần Response wrapper

---

## 5. Tối Ưu Phần Gửi Thông Báo

### Mô Tả
Cải thiện chức năng gửi notification-only với hỗ trợ template variables và cấu trúc request rõ ràng hơn.

### Thay Đổi

#### 1. SendNotificationOnlyRequest
- **File:** `src/main/java/vn/campuslife/model/SendNotificationOnlyRequest.java`
- **Thay đổi:**
  - Thêm field `templateVariables` để hỗ trợ template variables
  - Cập nhật comments cho các fields
  - Làm rõ `recipientIds` dùng cho BULK (có thể 1 hoặc nhiều)

**Trước:**
```java
private String title;
private String content;
private NotificationType type;
private String actionUrl;
```

**Sau:**
```java
private String title; // required - có thể dùng template variables
private String content; // required - có thể dùng template variables
private NotificationType type; // required
private String actionUrl; // optional
private Map<String, String> templateVariables; // optional - để thay thế {{variableName}}
```

#### 2. Template Variables Support
- **File:** `src/main/java/vn/campuslife/service/impl/EmailServiceImpl.java`
- **Method mới:** `buildTemplateVariablesForNotification()`
- **Chức năng:** Xử lý template variables cho notification, tương tự như email

**Code:**
```java
private Map<String, String> buildTemplateVariablesForNotification(
    User recipient, 
    SendNotificationOnlyRequest request
) {
    Map<String, String> vars = new HashMap<>();
    
    // Add email for all recipients
    vars.put("email", recipient.getEmail() != null ? recipient.getEmail() : "");
    
    // Get student info if recipient is a student
    // ... (tương tự buildTemplateVariables)
    
    // Add activity/series info if applicable
    // ...
    
    // Add custom template variables
    if (request.getTemplateVariables() != null) {
        vars.putAll(request.getTemplateVariables());
    }
    
    return vars;
}
```

#### 3. Process Template trong sendNotificationOnly
- **Thay đổi:** Xử lý template variables trước khi gửi notification
- **Code:**
  ```java
  // Build template variables
  Map<String, String> templateVars = buildTemplateVariablesForNotification(recipient, request);
  
  // Process template
  String processedContent = emailUtil.processTemplate(request.getContent(), templateVars);
  String processedTitle = emailUtil.processTemplate(request.getTitle(), templateVars);
  
  notificationService.sendNotification(
      recipient.getId(),
      processedTitle,
      processedContent,
      ...
  );
  ```

### Cách Sử Dụng

#### Ví Dụ 1: Notification với Template Variables
```typescript
const request: SendNotificationOnlyRequest = {
  recipientType: 'BULK',
  recipientIds: [123],
  title: "Thông báo cho {{studentName}}",
  content: "Xin chào {{studentName}}, email của bạn là {{email}}",
  type: "GENERAL",
  templateVariables: {
    customVar: "Giá trị tùy chỉnh"
  }
};
```

#### Ví Dụ 2: Notification cho Activity Registrations
```typescript
const request: SendNotificationOnlyRequest = {
  recipientType: 'ACTIVITY_REGISTRATIONS',
  activityId: 10,
  title: "Nhắc nhở sự kiện",
  content: "Xin chào {{studentName}}, sự kiện {{activityName}} sẽ diễn ra vào {{activityDate}}",
  type: "ACTIVITY_REMINDER"
  // Không cần templateVariables, hệ thống tự động lấy studentName, activityName, activityDate
};
```

### Lợi Ích
- Notification giờ hỗ trợ template variables giống email
- Có thể personalize notification cho từng recipient
- Code nhất quán giữa email và notification
- Dễ dàng mở rộng thêm biến template mới

---

## 6. Cập Nhật Tài Liệu

### Files Đã Cập Nhật
1. **FE_EMAIL_NOTIFICATION_FORMAT_GUIDE.md**
   - Thêm `{{email}}` vào danh sách biến tự động
   - Cập nhật `SendNotificationOnlyRequest` format
   - Thêm lưu ý về logic lấy registrations
   - Cập nhật API lấy khoa và sinh viên theo khoa
   - Thêm ví dụ về `StudentListResponse`

### Nội Dung Mới
- Hướng dẫn sử dụng `{{email}}` template variable
- Giải thích logic lấy registrations (không filter status)
- API endpoints cho departments
- Format response mới cho students by department

---

## 7. Tóm Tắt Thay Đổi

### Files Mới
- `src/main/java/vn/campuslife/model/StudentListResponse.java`


### Tính Năng Mới
1. ✅ DTO `StudentListResponse` cho response chuẩn hóa
2. ✅ Biến template `{{email}}` cho email và notification
3. ✅ Template variables support cho notification
4. ✅ Comments rõ ràng về logic lấy registrations

### Cải Tiến
1. ✅ Response structure nhất quán hơn
2. ✅ Template variables đầy đủ hơn
3. ✅ Code documentation tốt hơn
4. ✅ Tài liệu cập nhật đầy đủ

---

## 8. Migration Guide

### Frontend Changes

#### 1. Update Student List Response Parsing
**Trước:**
```typescript
const data = await response.json();
const students = data.data.content; // Map structure
```

**Sau:**
```typescript
const data = await response.json();
const students = data.data.content; // StudentListResponse structure
// data.data.totalElements, data.data.totalPages, etc.
```

#### 2. Use {{email}} Template Variable
**Trước:**
```typescript
content: "Xin chào {{studentName}}"
```

**Sau:**
```typescript
content: "Xin chào {{studentName}}, email của bạn là {{email}}"
```

#### 3. Use Template Variables in Notifications
**Trước:**
```typescript
const request: SendNotificationOnlyRequest = {
  title: "Thông báo",
  content: "Nội dung cố định"
};
```

**Sau:**
```typescript
const request: SendNotificationOnlyRequest = {
  title: "Thông báo cho {{studentName}}",
  content: "Xin chào {{studentName}}, email: {{email}}",
  templateVariables: {
    customVar: "value"
  }
};
```

---

## 9. Testing Checklist

### DTO StudentListResponse
- [ ] Test API `GET /api/students/department/{departmentId}`
- [ ] Verify response structure matches `StudentListResponse`
- [ ] Test pagination (page, size)
- [ ] Verify `content` contains `StudentResponse` objects

### Template Variable {{email}}
- [ ] Test email với `{{email}}` trong content
- [ ] Test notification với `{{email}}` trong content
- [ ] Verify email được thay thế đúng
- [ ] Test với recipient không có email (null check)

### Logic Lấy Registrations
- [ ] Test gửi email cho `ACTIVITY_REGISTRATIONS`
- [ ] Verify gửi cho tất cả status (PENDING, APPROVED, REJECTED, CANCELLED)
- [ ] Test gửi notification cho `SERIES_REGISTRATIONS`
- [ ] Verify không filter theo status

### API Departments
- [ ] Test `GET /api/departments` với STUDENT role
- [ ] Test `GET /api/departments` với MANAGER role
- [ ] Test `GET /api/departments` với ADMIN role
- [ ] Test `GET /api/admin/departments` với MANAGER role
- [ ] Test `GET /api/admin/departments` với ADMIN role

### Notification Template Variables
- [ ] Test notification với `{{email}}`
- [ ] Test notification với `{{studentName}}`
- [ ] Test notification với `{{activityName}}`
- [ ] Test notification với custom `templateVariables`
- [ ] Verify template processing hoạt động đúng

---

## 10. Breaking Changes

### Không Có Breaking Changes

Tất cả các thay đổi đều **backward compatible**:
- `StudentListResponse` chỉ thay đổi structure của response, không thay đổi API endpoint
- Template variables là tính năng mới, không ảnh hưởng code cũ
- Logic lấy registrations giữ nguyên (chỉ thêm comments)
- API departments không thay đổi

### Recommendations
- Frontend nên cập nhật để sử dụng `StudentListResponse` structure
- Nên sử dụng `{{email}}` template variable khi cần
- Nên sử dụng template variables trong notifications để personalize

---

## 11. Future Enhancements

### Có Thể Cải Thiện Thêm
1. **Filter Registrations by Status**
   - Thêm option để filter registrations theo status khi gửi email/notification
   - Ví dụ: chỉ gửi cho `APPROVED` registrations

2. **More Template Variables**
   - `{{userName}}` - Username của recipient
   - `{{registrationStatus}}` - Trạng thái đăng ký
   - `{{registrationDate}}` - Ngày đăng ký

3. **Batch Processing**
   - Xử lý gửi email/notification theo batch để tối ưu performance
   - Queue system cho large-scale sending

4. **Template Library**
   - Pre-defined templates cho các use cases phổ biến
   - Template editor trong admin panel

---
