# Sequence Diagram - Department (Khoa/Ngành) Module

> **Hệ thống:** CampusLife (Spring Boot + React)  
> **Module:** Department (Khoa/Ngành)  
> **Participant:** Admin/Manager, Client (React), Controller (Spring Boot), Service, Repository, Database

---

## 1. CRUD Department (C.8) — Thêm / Sửa / Xóa Khoa

```mermaid
sequenceDiagram
    autonumber
    participant A as Admin/Manager
    participant C as Client (React)
    participant CT as DepartmentController
    participant S as DepartmentService
    participant R as DepartmentRepository
    participant DB as Database

    %% ========== LUỒNG TẠO KHOA (CREATE) ==========
    Note over A,DB: === LUỒNG TẠO KHOA MỚI (POST /api/admin/departments) ===

    A->>C: 1. Nhập thông tin khoa<br/>(name, code, description)
    C->>C: 2. Validate form client-side<br/>(code định dạng, name không rỗng)
    C->>CT: 3. POST /api/admin/departments<br/>Header: Bearer <JWT_TOKEN><br/>Body: {name, code, description}

    CT->>CT: 4. @PreAuthorize("hasRole('ADMIN')")<br/>Validate JWT token
    CT->>S: 5. createDepartment(departmentDTO)

    S->>S: 6. Validate business rules<br/>(name not null, code not null, description length)
    S->>R: 7. existsByCode(code)
    R->>DB: 8. SELECT COUNT(*) FROM departments WHERE code = ?
    DB-->>R: 9. count = 0 (code chưa tồn tại)
    R-->>S: 10. false (code không trùng)

    alt Code không trùng
        S->>S: 11. Convert DTO → Entity<br/>(new Department(name, code, description))
        S->>R: 12. save(departmentEntity)
        R->>DB: 13. INSERT INTO departments (name, code, description, created_at) VALUES (?, ?, ?, ?)
        DB-->>R: 14. Return saved entity với generated ID
        R-->>S: 15. Department entity (đã có ID)
        S->>S: 16. Convert Entity → Response DTO
        S-->>CT: 17. DepartmentResponseDTO {id, name, code, description, createdAt}
        CT-->>C: 18. HTTP 201 Created<br/>Body: DepartmentResponseDTO
        C-->>A: 19. Hiển thị thông báo "Tạo khoa thành công"
    else Code đã tồn tại
        S-->>CT: 17a. Throw DuplicateCodeException("Mã khoa đã tồn tại")
        CT-->>C: 18a. HTTP 409 Conflict<br/>Body: {error: "Mã khoa đã tồn tại"}
        C-->>A: 19a. Hiển thị lỗi "Mã khoa đã tồn tại, vui lòng chọn mã khác"
    end

    %% ========== LUỒNG CẬP NHẬT KHOA (UPDATE) ==========
    Note over A,DB: === LUỒNG CẬP NHẬT KHOA (PUT /api/admin/departments/{id}) ===

    A->>C: 20. Chọn khoa cần sửa<br/>Nhập thông tin mới (name, code, description)
    C->>C: 21. Validate form client-side<br/>Kiểm tra id hợp lệ (UUID/Long)
    C->>CT: 22. PUT /api/admin/departments/{id}<br/>Header: Bearer <JWT_TOKEN><br/>Body: {name, code, description}

    CT->>CT: 23. @PreAuthorize("hasRole('ADMIN')")<br/>Validate JWT token<br/>Extract path variable {id}
    CT->>S: 24. updateDepartment(id, departmentDTO)

    S->>R: 25. findById(id)
    R->>DB: 26. SELECT * FROM departments WHERE id = ?
    DB-->>R: 27. Return department record (nếu tồn tại)
    R-->>S: 28. Optional<Department> (isPresent = true)

    alt Department tồn tại
        S->>S: 29. Lấy entity ra khỏi Optional<br/>department.setName(dto.name)<br/>department.setCode(dto.code)<br/>department.setDescription(dto.description)
        S->>S: 30. Validate new code không trùng (nếu code thay đổi)<br/>existsByCodeAndIdNot(newCode, id)
        S->>R: 31. save(departmentEntity)
        R->>DB: 32. UPDATE departments SET name=?, code=?, description=?, updated_at=? WHERE id=?
        DB-->>R: 33. Return updated entity
        R-->>S: 34. Department entity (đã cập nhật)
        S->>S: 35. Convert Entity → Response DTO
        S-->>CT: 36. DepartmentResponseDTO {id, name, code, description, updatedAt}
        CT-->>C: 37. HTTP 200 OK<br/>Body: DepartmentResponseDTO
        C-->>A: 38. Hiển thị thông báo "Cập nhật khoa thành công"
    else Department không tồn tại
        S-->>CT: 36a. Throw ResourceNotFoundException("Khoa không tồn tại")
        CT-->>C: 37a. HTTP 404 Not Found<br/>Body: {error: "Khoa không tồn tại"}
        C-->>A: 38a. Hiển thị lỗi "Không tìm thấy khoa cần cập nhật"
    end

    %% ========== LUỒNG XÓA KHOA (DELETE) ==========
    Note over A,DB: === LUỒNG XÓA KHOA (DELETE /api/admin/departments/{id}) ===

    A->>C: 39. Chọn khoa cần xóa<br/>Xác nhận xóa (Confirm Dialog)
    C->>CT: 40. DELETE /api/admin/departments/{id}<br/>Header: Bearer <JWT_TOKEN>

    CT->>CT: 41. @PreAuthorize("hasRole('ADMIN')")<br/>Validate JWT token<br/>Extract path variable {id}
    CT->>S: 42. deleteDepartment(id)

    S->>R: 43. findById(id)
    R->>DB: 44. SELECT * FROM departments WHERE id = ?
    DB-->>R: 45. Return department record (nếu tồn tại)
    R-->>S: 46. Optional<Department> (isPresent = true/false)

    alt Department tồn tại
        S->>R: 47. Kiểm tra ràng buộc: countClassesByDepartmentId(id)
        R->>DB: 48. SELECT COUNT(*) FROM classes WHERE department_id = ?
        DB-->>R: 49. classCount = 0 (không có lớp thuộc khoa)
        R-->>S: 50. classCount = 0

        S->>R: 51. Kiểm tra ràng buộc: countStudentsByDepartmentId(id)
        R->>DB: 52. SELECT COUNT(*) FROM students WHERE department_id = ?
        DB-->>R: 53. studentCount = 0 (không có sinh viên thuộc khoa)
        R-->>S: 54. studentCount = 0

        alt Không có ràng buộc (classCount == 0 && studentCount == 0)
            S->>R: 55. deleteById(id)
            R->>DB: 56. DELETE FROM departments WHERE id = ?
            DB-->>R: 57. Xóa thành công (affected rows = 1)
            R-->>S: 58. Delete success
            S-->>CT: 59. void (xoá thành công)
            CT-->>C: 60. HTTP 204 No Content
            C-->>A: 61. Hiển thị thông báo "Xóa khoa thành công"<br/>Refresh danh sách khoa
        else Có ràng buộc (classCount > 0 hoặc studentCount > 0)
            S-->>CT: 59a. Throw DataIntegrityException("Không thể xóa khoa đã có lớp/sinh viên")
            CT-->>C: 60a. HTTP 409 Conflict<br/>Body: {error: "Khoa đang có lớp hoặc sinh viên, không thể xóa"}
            C-->>A: 61a. Hiển thị lỗi "Không thể xóa khoa đang có dữ liệu liên quan"
        end
    else Department không tồn tại
        S-->>CT: 59b. Throw ResourceNotFoundException("Khoa không tồn tại")
        CT-->>C: 60b. HTTP 404 Not Found<br/>Body: {error: "Khoa không tồn tại"}
        C-->>A: 61b. Hiển thị lỗi "Không tìm thấy khoa cần xóa"
    end
```

---

## 2. Xem Danh Sách Khoa (C.9) — Public API

```mermaid
sequenceDiagram
    autonumber
    actor U as User (Public)
    participant C as Client (React)
    participant CT as DepartmentController
    participant S as DepartmentService
    participant R as DepartmentRepository
    participant DB as Database

    Note over U,DB: === LUỒNG XEM DANH SÁCH KHOA (GET /api/departments) ===<br/>Public API — Không yêu cầu Authentication

    U->>C: 1. Truy cập trang "Danh sách khoa/ngành"<br/>(hoặc component hiển thị khoa)
    C->>C: 2. Component mount / useEffect triggered<br/>Gọi API lấy danh sách khoa
    C->>CT: 3. GET /api/departments<br/>Không có Authorization Header<br/>(Public endpoint, permitAll)

    CT->>CT: 4. Không cần validate JWT<br/>(Endpoint public, không yêu cầu auth)
    CT->>S: 5. getAllDepartments()

    S->>R: 6. findAll()
    R->>DB: 7. SELECT * FROM departments ORDER BY name ASC
    DB-->>R: 8. Trả về list các department records<br/>[Department1, Department2, ...]
    R-->>S: 9. List<Department> entities

    S->>S: 10. Stream/map entities → List<DepartmentResponseDTO><br/>For each entity:<br/>- map id, name, code, description<br/>- format createdAt/updatedAt nếu cần
    S-->>CT: 11. List<DepartmentResponseDTO><br/>[ {id, name, code, description}, ... ]
    CT-->>C: 12. HTTP 200 OK<br/>Body: [ {id, name, code, description}, ... ]

    C->>C: 13. Nhận response, cập nhật state<br/>setDepartments(data)
    C->>C: 14. Render component<br/>- Hiển thị table/grid danh sách khoa<br/>- Có thể phân trang (page, size) hoặc tìm kiếm (search keyword)
    C-->>U: 15. Hiển thị danh sách khoa lên UI

    %% Optional: Pagination / Search flow
    Note over U,DB: === (Mở Rộng) Phân Trang và Tìm Kiếm ===

    U->>C: 16. Nhập từ khóa tìm kiếm / chuyển trang
    C->>CT: 17. GET /api/departments?page=0&size=10&sort=name&search=keyword
    CT->>S: 18. getAllDepartments(Pageable pageable, String search)
    S->>R: 19. findByNameContainingIgnoreCase(search, pageable)
    R->>DB: 20. SELECT * FROM departments WHERE LOWER(name) LIKE LOWER('%keyword%') ORDER BY name LIMIT 10 OFFSET 0
    DB-->>R: 21. Page<Department> (content, totalElements, totalPages)
    R-->>S: 22. Page<Department>
    S->>S: 23. Map Page content → List<DepartmentResponseDTO><br/>Wrap vào PageResponseDTO
    S-->>CT: 24. PageResponseDTO { content, totalElements, totalPages, currentPage, size }
    CT-->>C: 25. HTTP 200 OK<br/>Body: PageResponseDTO
    C->>C: 26. Cập nhật state: setDepartments(pageContent)<br/>setPagination({page, totalPages, totalElements})
    C-->>U: 27. Hiển thị danh sách khoa đã lọc/phân trang
```

---

## Tóm Tắt Thành Phần và Chức Năng

### Thành phần tham gia

| Thành phần | Vai trò | Trách nhiệm chính |
|---|---|---|
| **Admin/Manager** | Actor | Người dùng có quyền ADMIN, thực hiện các thao tác CRUD trên khoa. |
| **User (Public)** | Actor | Người dùng bất kỳ (không cần đăng nhập), xem danh sách khoa. |
| **Client (React)** | Frontend | UI nhập liệu, validate form, gọi HTTP request, hiển thị response, xử lý loading/error state. |
| **DepartmentController** | REST Controller | Nhận HTTP request, validate JWT (@PreAuthorize), extract path/query params, gọi Service, trả về ResponseEntity. |
| **DepartmentService** | Business Logic Layer | Xử lý nghiệp vụ: validate DTO, kiểm tra trùng lặp code, kiểm tra ràng buộc trước khi xóa, convert DTO ↔ Entity, xử lý pagination. |
| **DepartmentRepository** | Data Access Layer | Interface extends JpaRepository, thực hiện các thao tác: findAll, findById, save, deleteById, existsByCode, countByDepartmentId, findByNameContaining. |
| **Database** | Persistence | Hệ quản trị CSDL (MySQL/PostgreSQL), lưu trữ bảng `departments` và các bảng liên quan (`classes`, `students`). |

### Chức năng tương ứng (theo mã C.8, C.9)

| Mã | Chức năng | HTTP Method | Endpoint | Auth Required | Luồng chính |
|---|---|---|---|---|---|
| **C.8** | Thêm khoa | POST | `/api/admin/departments` | ✅ ADMIN | Validate form → Check code trùng → Save → Return 201 |
| **C.8** | Sửa khoa | PUT | `/api/admin/departments/{id}` | ✅ ADMIN | FindById → Check code trùng (nếu đổi) → Update → Save → Return 200 |
| **C.8** | Xóa khoa | DELETE | `/api/admin/departments/{id}` | ✅ ADMIN | FindById → Check có lớp/sinh viên → Nếu không có → Delete → Return 204 |
| **C.9** | Xem danh sách khoa | GET | `/api/departments` | ❌ Public | findAll → Map to DTO list → Return 200 (Hỗ trợ phân trang, tìm kiếm) |

### Luồng xử lý lỗi chính

| Lỗi | Mã HTTP | Nguyên nhân | Xử lý tại Service |
|---|---|---|---|
| DuplicateCodeException | 409 Conflict | Mã khoa đã tồn tại khi tạo/sửa | `existsByCode()` trả về true |
| ResourceNotFoundException | 404 Not Found | Khoa không tồn tại (sửa/xóa) | `findById()` trả về Optional.empty |
| DataIntegrityException | 409 Conflict | Khoa đang có lớp hoặc sinh viên | `countClasses/StudentsByDepartmentId() > 0` |
| ValidationException | 400 Bad Request | Dữ liệu đầu vào không hợp lệ | `@Valid` DTO + custom validation |
| AccessDeniedException | 403 Forbidden | User không có quyền ADMIN | `@PreAuthorize` Spring Security |

---

> **Ghi chú:**  
> - Các endpoint `/api/admin/**` yêu cầu JWT token và role ADMIN (Spring Security `@PreAuthorize`).  
> - Endpoint `/api/departments` (GET) là public API, không yêu cầu authentication (`permitAll()`).  
> - Trước khi xóa khoa, hệ thống kiểm tra ràng buộc với bảng `classes` và `students` để tránh mất dữ liệu liên quan.  
> - Các thao tác CRUD đều sử dụng DTO (Data Transfer Object) để tách biệt giữa Request/Response và Entity.
