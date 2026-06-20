# Sequence Diagram - Chức năng Quản lý Phòng ban/Khoa

## Mô tả
Sequence diagram mô tả luồng xử lý quản lý phòng ban/khoa (Department) trong hệ thống CampusLife.

## Sequence Diagrams

### 1. Tạo phòng ban/khoa (Create Department)

```mermaid
sequenceDiagram
    participant Admin as Admin
    participant Client as Client/Frontend
    participant DeptController as DepartmentAdminController
    participant DeptService as DepartmentServiceImpl
    participant DeptRepository as DepartmentRepository
    participant Database as Database

    Admin->>Client: Nhập thông tin phòng ban/khoa<br/>(name, type, description)
    Client->>DeptController: POST /api/admin/departments<br/>(DepartmentRequest)
    
    DeptController->>DeptService: create(DepartmentRequest)
    
    Note over DeptService: Tạo phòng ban/khoa mới
    DeptService->>DeptService: create Department<br/>(name, type, description)
    DeptService->>DeptRepository: save(department)
    DeptRepository->>Database: INSERT INTO departments<br/>(name, type, description)
    Database-->>DeptRepository: Department saved
    DeptRepository-->>DeptService: Department
    
    DeptService-->>DeptController: Response(success, Department)
    DeptController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
```

### 2. Cập nhật phòng ban/khoa (Update Department)

```mermaid
sequenceDiagram
    participant Admin as Admin
    participant Client as Client/Frontend
    participant DeptController as DepartmentAdminController
    participant DeptService as DepartmentServiceImpl
    participant DeptRepository as DepartmentRepository
    participant Database as Database

    Admin->>Client: Chọn phòng ban/khoa và cập nhật thông tin
    Client->>DeptController: PUT /api/admin/departments/{id}<br/>(DepartmentRequest)
    
    DeptController->>DeptService: update(id, DepartmentRequest)
    
    DeptService->>DeptRepository: findById(id)
    DeptRepository->>Database: SELECT * FROM departments WHERE id = ?
    Database-->>DeptRepository: Department
    DeptRepository-->>DeptService: Department
    
    Note over DeptService: Kiểm tra phòng ban/khoa chưa bị xóa
    
    Note over DeptService: Cập nhật thông tin
    DeptService->>DeptService: existing.setName(name)<br/>existing.setType(type)<br/>existing.setDescription(description)
    DeptService->>DeptRepository: save(existing)
    DeptRepository->>Database: UPDATE departments SET<br/>name=?, type=?, description=? WHERE id=?
    Database-->>DeptRepository: Updated
    DeptRepository-->>DeptService: Department updated
    
    DeptService-->>DeptController: Response(success, Department)
    DeptController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
```

### 3. Xóa phòng ban/khoa (Delete Department)

```mermaid
sequenceDiagram
    participant Admin as Admin
    participant Client as Client/Frontend
    participant DeptController as DepartmentAdminController
    participant DeptService as DepartmentServiceImpl
    participant DeptRepository as DepartmentRepository
    participant Database as Database

    Admin->>Client: Chọn phòng ban/khoa và click xóa
    Client->>DeptController: DELETE /api/admin/departments/{id}
    
    DeptController->>DeptService: delete(id)
    
    DeptService->>DeptRepository: findById(id)
    DeptRepository->>Database: SELECT * FROM departments WHERE id = ?
    Database-->>DeptRepository: Department
    DeptRepository-->>DeptService: Department
    
    Note over DeptService: Kiểm tra phòng ban/khoa chưa bị xóa
    
    Note over DeptService: Soft delete:<br/>Set is_deleted = true
    DeptService->>DeptService: existing.setDeleted(true)
    DeptService->>DeptRepository: save(existing)
    DeptRepository->>Database: UPDATE departments<br/>SET is_deleted = true WHERE id = ?
    Database-->>DeptRepository: Updated
    DeptRepository-->>DeptService: Department updated
    
    DeptService-->>DeptController: Response(success)
    DeptController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
```

### 4. Xem danh sách và chi tiết phòng ban/khoa (Get All / Get By ID)

```mermaid
sequenceDiagram
    participant User as User/Admin
    participant Client as Client/Frontend
    participant DeptController as DepartmentController<br/>hoặc DepartmentAdminController
    participant DeptService as DepartmentServiceImpl
    participant DeptRepository as DepartmentRepository
    participant Database as Database

    Note over User,Database: Xem danh sách phòng ban/khoa
    
    User->>Client: Truy cập trang danh sách phòng ban/khoa
    Client->>DeptController: GET /api/departments<br/>hoặc GET /api/admin/departments
    
    DeptController->>DeptService: getAll() hoặc findAll()
    
    DeptService->>DeptRepository: findAll()
    DeptRepository->>Database: SELECT * FROM departments
    Database-->>DeptRepository: List<Department>
    DeptRepository-->>DeptService: List<Department>
    
    Note over DeptService: Lọc và sắp xếp:<br/>- Loại bỏ đã bị xóa (is_deleted = false)<br/>- Sắp xếp theo tên (A-Z)
    DeptService->>DeptService: filter(!isDeleted)<br/>sort by name
    
    DeptService-->>DeptController: Response(success, List<Department>)
    DeptController-->>Client: ResponseEntity.ok()
    Client-->>User: Hiển thị danh sách phòng ban/khoa
    
    Note over User,Database: Xem chi tiết phòng ban/khoa
    
    User->>Client: Click xem chi tiết phòng ban/khoa
    Client->>DeptController: GET /api/departments/{id}<br/>hoặc GET /api/admin/departments/{id}
    
    DeptController->>DeptService: getById(id) hoặc findById(id)
    
    DeptService->>DeptRepository: findById(id)
    DeptRepository->>Database: SELECT * FROM departments WHERE id = ?
    Database-->>DeptRepository: Department
    DeptRepository-->>DeptService: Department
    
    Note over DeptService: Kiểm tra phòng ban/khoa chưa bị xóa
    DeptService->>DeptService: filter(!isDeleted)
    
    DeptService-->>DeptController: Response(success, Department)<br/>hoặc Optional<Department>
    DeptController-->>Client: ResponseEntity.ok()
    Client-->>User: Hiển thị thông tin chi tiết phòng ban/khoa
```

## Các thành phần tham gia

1. **Admin/User**: Người dùng (Admin có quyền CRUD, User chỉ xem)
2. **Client/Frontend**: Giao diện người dùng
3. **DepartmentAdminController**: Controller nhận request quản lý phòng ban/khoa (Admin)
4. **DepartmentController**: Controller nhận request xem phòng ban/khoa (Public)
5. **DepartmentServiceImpl**: Service xử lý logic quản lý phòng ban/khoa
6. **DepartmentRepository**: Repository truy cập database
7. **Database**: Cơ sở dữ liệu

## Các chức năng

### 1. Tạo phòng ban/khoa (Create Department)
1. Admin nhập thông tin phòng ban/khoa (name, type, description)
2. Tạo Department mới
3. Lưu vào database
4. Trả về thông tin phòng ban/khoa đã tạo

### 2. Cập nhật phòng ban/khoa (Update Department)
1. Admin chọn phòng ban/khoa và cập nhật thông tin
2. Tìm phòng ban/khoa theo ID
3. Kiểm tra chưa bị xóa
4. Cập nhật thông tin (name, type, description)
5. Lưu vào database
6. Trả về thông tin đã cập nhật

### 3. Xóa phòng ban/khoa (Delete Department)
1. Admin chọn phòng ban/khoa và click xóa
2. Tìm phòng ban/khoa theo ID
3. Kiểm tra chưa bị xóa
4. Thực hiện soft delete (set is_deleted = true)
5. Lưu vào database
6. Trả về kết quả thành công

### 4. Xem danh sách và chi tiết phòng ban/khoa (Get All / Get By ID)

**Xem danh sách:**
1. User/Admin truy cập trang danh sách
2. Lấy tất cả phòng ban/khoa từ database
3. Lọc loại bỏ đã bị xóa (is_deleted = false)
4. Sắp xếp theo tên (A-Z)
5. Trả về danh sách phòng ban/khoa

**Xem chi tiết:**
1. User/Admin click xem chi tiết
2. Tìm phòng ban/khoa theo ID
3. Kiểm tra chưa bị xóa
4. Trả về thông tin chi tiết phòng ban/khoa

## Đặc điểm

- **Phân quyền**: 
  - Admin có quyền CRUD đầy đủ (tạo, sửa, xóa, xem)
  - User chỉ có quyền xem (GET)
- **Soft Delete**: Xóa phòng ban/khoa bằng cách đánh dấu is_deleted = true, không xóa thật
- **Sắp xếp**: Danh sách được sắp xếp theo tên (A-Z)
- **Public API**: Có endpoint public cho việc xem danh sách và chi tiết phòng ban/khoa
- **Admin API**: Có endpoint riêng cho admin để quản lý đầy đủ

