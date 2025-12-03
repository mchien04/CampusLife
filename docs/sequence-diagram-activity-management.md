# Sequence Diagram - Chức năng Quản lý Hoạt động

## Mô tả
Sequence diagram mô tả luồng xử lý quản lý hoạt động (Activity) trong hệ thống CampusLife. Bao gồm các chức năng tạo, cập nhật, xóa, xem danh sách, xem chi tiết, publish, unpublish và copy hoạt động.

## Sequence Diagrams

### 1. Tạo hoạt động (Create Activity)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ActivityController as ActivityController
    participant ActivityService as ActivityServiceImpl
    participant Repository as Repository
    participant Database as Database

    Admin->>Client: Nhập thông tin hoạt động<br/>(name, type, scoreType, dates, organizers...)
    Client->>ActivityController: POST /api/activities<br/>(CreateActivityRequest)
    
    ActivityController->>ActivityService: createActivity(request)
    
    Note over ActivityService: Validate request:<br/>- Tên, type, scoreType không rỗng<br/>- Dates hợp lệ<br/>- OrganizerIds tồn tại
    
    Note over ActivityService: Resolve organizers
    ActivityService->>Repository: findById(organizerId) cho mỗi organizerId
    Repository->>Database: SELECT * FROM departments<br/>WHERE id = ?
    Database-->>Repository: Department
    Repository-->>ActivityService: Set<Department>
    
    Note over ActivityService: Tạo Activity mới
    ActivityService->>ActivityService: create Activity<br/>(name, type, scoreType, dates,<br/>organizers, isDraft...)
    ActivityService->>Repository: save(activity)
    Repository->>Database: INSERT INTO activities<br/>(name, type, score_type, start_date,<br/>end_date, is_draft, is_deleted...)
    Database-->>Repository: Activity saved
    Repository-->>ActivityService: Activity
    
    Note over ActivityService: Auto-register students<br/>(nếu isImportant hoặc<br/>mandatoryForFacultyStudents)
    
    ActivityService-->>ActivityController: Response(success, ActivityResponse)
    ActivityController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
```

### 2. Cập nhật hoạt động (Update Activity)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ActivityController as ActivityController
    participant ActivityService as ActivityServiceImpl
    participant Repository as Repository
    participant Database as Database

    Admin->>Client: Chỉnh sửa thông tin hoạt động
    Client->>ActivityController: PUT /api/activities/{id}<br/>(CreateActivityRequest)
    
    ActivityController->>ActivityService: updateActivity(id, request)
    
    Note over ActivityService: Kiểm tra activity tồn tại
    ActivityService->>Repository: findByIdAndIsDeletedFalse(id)
    Repository->>Database: SELECT * FROM activities<br/>WHERE id = ? AND is_deleted = false
    Database-->>Repository: Activity
    Repository-->>ActivityService: Activity
    
    Note over ActivityService: Validate request
    
    Note over ActivityService: Resolve organizers mới
    ActivityService->>Repository: findById(organizerId) cho mỗi organizerId
    Repository->>Database: SELECT * FROM departments<br/>WHERE id = ?
    Database-->>Repository: Department
    Repository-->>ActivityService: Set<Department>
    
    Note over ActivityService: Cập nhật activity
    ActivityService->>ActivityService: applyRequestToEntity(request, activity)<br/>clear và add organizers mới
    ActivityService->>Repository: save(activity)
    Repository->>Database: UPDATE activities<br/>SET name = ?, type = ?,<br/>score_type = ?, start_date = ?,<br/>end_date = ?, ...<br/>WHERE id = ?
    Database-->>Repository: Activity updated
    Repository-->>ActivityService: Activity
    
    Note over ActivityService: Auto-register students<br/>(nếu flags thay đổi)
    
    ActivityService-->>ActivityController: Response(success, ActivityResponse)
    ActivityController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo cập nhật thành công
```

### 3. Xóa hoạt động (Delete Activity)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ActivityController as ActivityController
    participant ActivityService as ActivityServiceImpl
    participant Repository as Repository
    participant Database as Database

    Admin->>Client: Chọn hoạt động cần xóa
    Client->>ActivityController: DELETE /api/activities/{id}
    
    ActivityController->>ActivityService: deleteActivity(id)
    
    Note over ActivityService: Kiểm tra activity tồn tại
    ActivityService->>Repository: findByIdAndIsDeletedFalse(id)
    Repository->>Database: SELECT * FROM activities<br/>WHERE id = ? AND is_deleted = false
    Database-->>Repository: Activity
    Repository-->>ActivityService: Activity
    
    Note over ActivityService: Soft delete
    ActivityService->>ActivityService: activity.setDeleted(true)
    ActivityService->>Repository: save(activity)
    Repository->>Database: UPDATE activities<br/>SET is_deleted = true<br/>WHERE id = ?
    Database-->>Repository: Activity updated
    Repository-->>ActivityService: Activity
    
    ActivityService-->>ActivityController: Response(success, null)
    ActivityController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo xóa thành công
```

### 4. Xem danh sách hoạt động (Get All Activities)

```mermaid
sequenceDiagram
    participant User as User/Admin/Manager
    participant Client as Client/Frontend
    participant ActivityController as ActivityController
    participant ActivityService as ActivityServiceImpl
    participant Repository as Repository
    participant Database as Database

    User->>Client: Xem danh sách hoạt động
    Client->>ActivityController: GET /api/activities<br/>(Authentication optional)
    
    Note over ActivityController: Lấy username từ authentication
    ActivityController->>ActivityService: getAllActivities(username)
    
    ActivityService->>Repository: findByIsDeletedFalseOrderByStartDateAsc()
    Repository->>Database: SELECT * FROM activities<br/>WHERE is_deleted = false<br/>ORDER BY start_date ASC
    Database-->>Repository: List<Activity>
    Repository-->>ActivityService: List<Activity>
    
    Note over ActivityService: Filter drafts cho sinh viên<br/>(Admin/Manager thấy tất cả)
    
    alt User là Student
        ActivityService->>ActivityService: filter(!isDraft)
    end
    
    ActivityService->>ActivityService: map to ActivityResponse
    ActivityService-->>ActivityController: Response(success, List<ActivityResponse>)
    ActivityController-->>Client: ResponseEntity.ok()
    Client-->>User: Hiển thị danh sách hoạt động
```

### 5. Xem chi tiết hoạt động (Get Activity By ID)

```mermaid
sequenceDiagram
    participant User as User/Admin/Manager
    participant Client as Client/Frontend
    participant ActivityController as ActivityController
    participant ActivityService as ActivityServiceImpl
    participant Repository as Repository
    participant Database as Database

    User->>Client: Xem chi tiết hoạt động
    Client->>ActivityController: GET /api/activities/{id}<br/>(Authentication optional)
    
    Note over ActivityController: Lấy username từ authentication
    ActivityController->>ActivityService: getActivityById(id, username)
    
    ActivityService->>Repository: findByIdAndIsDeletedFalse(id)
    Repository->>Database: SELECT * FROM activities<br/>WHERE id = ? AND is_deleted = false
    Database-->>Repository: Activity
    Repository-->>ActivityService: Activity
    
    Note over ActivityService: Kiểm tra quyền xem draft
    alt Activity là draft và User không phải Admin/Manager
        ActivityService-->>ActivityController: Response(false, "Activity not found")
        ActivityController-->>Client: ResponseEntity.notFound()
    else Activity không phải draft hoặc User là Admin/Manager
        ActivityService->>ActivityService: map to ActivityResponse
        ActivityService-->>ActivityController: Response(success, ActivityResponse)
        ActivityController-->>Client: ResponseEntity.ok()
        Client-->>User: Hiển thị chi tiết hoạt động
    end
```

### 6. Publish hoạt động (Publish Activity)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ActivityController as ActivityController
    participant ActivityService as ActivityServiceImpl
    participant Repository as Repository
    participant Database as Database

    Admin->>Client: Publish hoạt động
    Client->>ActivityController: PUT /api/activities/{id}/publish
    
    ActivityController->>ActivityService: publishActivity(id)
    
    ActivityService->>Repository: findByIdAndIsDeletedFalse(id)
    Repository->>Database: SELECT * FROM activities<br/>WHERE id = ? AND is_deleted = false
    Database-->>Repository: Activity
    Repository-->>ActivityService: Activity
    
    Note over ActivityService: Lưu trạng thái draft cũ
    ActivityService->>ActivityService: wasDraft = activity.isDraft()
    
    Note over ActivityService: Publish activity
    ActivityService->>ActivityService: activity.setDraft(false)
    ActivityService->>Repository: save(activity)
    Repository->>Database: UPDATE activities<br/>SET is_draft = false<br/>WHERE id = ?
    Database-->>Repository: Activity updated
    Repository-->>ActivityService: Activity
    
    Note over ActivityService: Auto-register students<br/>(nếu vừa publish và có flags)
    alt wasDraft và (isImportant hoặc mandatoryForFacultyStudents)
        ActivityService->>ActivityService: autoRegisterStudents(activity)
    end
    
    ActivityService-->>ActivityController: Response(success, ActivityResponse)
    ActivityController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo publish thành công
```

### 7. Unpublish hoạt động (Unpublish Activity)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ActivityController as ActivityController
    participant ActivityService as ActivityServiceImpl
    participant Repository as Repository
    participant Database as Database

    Admin->>Client: Unpublish hoạt động
    Client->>ActivityController: PUT /api/activities/{id}/unpublish
    
    ActivityController->>ActivityService: unpublishActivity(id)
    
    ActivityService->>Repository: findByIdAndIsDeletedFalse(id)
    Repository->>Database: SELECT * FROM activities<br/>WHERE id = ? AND is_deleted = false
    Database-->>Repository: Activity
    Repository-->>ActivityService: Activity
    
    Note over ActivityService: Unpublish activity
    ActivityService->>ActivityService: activity.setDraft(true)
    ActivityService->>Repository: save(activity)
    Repository->>Database: UPDATE activities<br/>SET is_draft = true<br/>WHERE id = ?
    Database-->>Repository: Activity updated
    Repository-->>ActivityService: Activity
    
    ActivityService-->>ActivityController: Response(success, ActivityResponse)
    ActivityController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo unpublish thành công
```

### 8. Copy hoạt động (Copy Activity)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant ActivityController as ActivityController
    participant ActivityService as ActivityServiceImpl
    participant Repository as Repository
    participant Database as Database

    Admin->>Client: Copy hoạt động<br/>(có thể offset days)
    Client->>ActivityController: POST /api/activities/{id}/copy<br/>(offsetDays optional)
    
    ActivityController->>ActivityService: copyActivity(id, offsetDays)
    
    ActivityService->>Repository: findByIdAndIsDeletedFalse(id)
    Repository->>Database: SELECT * FROM activities<br/>WHERE id = ? AND is_deleted = false
    Database-->>Repository: Activity
    Repository-->>ActivityService: Activity (source)
    
    Note over ActivityService: Tạo Activity mới từ source
    ActivityService->>ActivityService: create Activity copy<br/>(name + " (Copy)",<br/>dates + offsetDays,<br/>isDraft = true,<br/>copy organizers...)
    
    ActivityService->>Repository: save(copy)
    Repository->>Database: INSERT INTO activities<br/>(name, type, score_type, start_date,<br/>end_date, is_draft = true, ...)
    Database-->>Repository: Activity saved
    Repository-->>ActivityService: Activity (copy)
    
    ActivityService-->>ActivityController: Response(success, ActivityResponse)
    ActivityController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo copy thành công
```

## Ghi chú

1. **Quyền truy cập**: 
   - Tạo, cập nhật, xóa, publish, unpublish, copy: Chỉ Admin và Manager
   - Xem danh sách và chi tiết: Tất cả người dùng (nhưng sinh viên không thấy draft)

2. **Soft Delete**: Hoạt động sử dụng soft delete (is_deleted = true), không xóa vĩnh viễn.

3. **Draft Mode**: 
   - Hoạt động có thể ở chế độ draft (isDraft = true)
   - Sinh viên không thể xem hoạt động ở chế độ draft
   - Admin/Manager có thể xem và quản lý tất cả hoạt động

4. **Auto-register**: 
   - Khi tạo hoặc cập nhật hoạt động với `isImportant = true` hoặc `mandatoryForFacultyStudents = true`, hệ thống tự động đăng ký cho sinh viên
   - Chỉ áp dụng khi hoạt động không phải draft

5. **Copy Activity**: 
   - Copy hoạt động cùng với organizers
   - Có thể offset dates (thêm số ngày vào các ngày của hoạt động gốc)
   - Hoạt động copy mặc định ở chế độ draft

