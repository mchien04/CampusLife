# Sequence Diagram - Chức năng Quản lý Niên khóa - Học kì

## Mô tả
Sequence diagram mô tả luồng xử lý quản lý niên khóa (Academic Year) và học kì (Semester) trong hệ thống CampusLife (dành cho ADMIN và MANAGER).

## Sequence Diagrams

### Phần 1: Quản lý Niên khóa (Academic Year)

#### 1.1. Tạo, Cập nhật, Xóa niên khóa (Create/Update/Delete Academic Year)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant AcademicController as AcademicAdminController
    participant AcademicService as AcademicServiceImpl
    participant YearRepository as AcademicYearRepository
    participant Database as Database

    Note over Admin,Database: Tạo niên khóa
    
    Admin->>Client: Nhập thông tin niên khóa<br/>(name, startDate, endDate)
    Client->>AcademicController: POST /api/admin/academics/years<br/>(AcademicYearRequest)
    
    AcademicController->>AcademicService: createYear(AcademicYearRequest)
    
    Note over AcademicService: Tạo niên khóa mới
    AcademicService->>AcademicService: create AcademicYear<br/>(name, startDate, endDate)
    AcademicService->>YearRepository: save(academicYear)
    YearRepository->>Database: INSERT INTO academic_years<br/>(name, start_date, end_date)
    Database-->>YearRepository: AcademicYear saved
    YearRepository-->>AcademicService: AcademicYear
    
    AcademicService-->>AcademicController: Response(success, AcademicYear)
    AcademicController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
    
    Note over Admin,Database: Cập nhật niên khóa
    
    Admin->>Client: Chọn niên khóa và cập nhật thông tin
    Client->>AcademicController: PUT /api/admin/academics/years/{id}<br/>(AcademicYearRequest)
    
    AcademicController->>AcademicService: updateYear(id, AcademicYearRequest)
    
    AcademicService->>YearRepository: findById(id)
    YearRepository->>Database: SELECT * FROM academic_years WHERE id = ?
    Database-->>YearRepository: AcademicYear
    YearRepository-->>AcademicService: AcademicYear
    
    Note over AcademicService: Cập nhật thông tin
    AcademicService->>AcademicService: year.setName(name)<br/>year.setStartDate(startDate)<br/>year.setEndDate(endDate)
    AcademicService->>YearRepository: save(year)
    YearRepository->>Database: UPDATE academic_years SET<br/>name=?, start_date=?, end_date=? WHERE id=?
    Database-->>YearRepository: Updated
    YearRepository-->>AcademicService: AcademicYear updated
    
    AcademicService-->>AcademicController: Response(success, AcademicYear)
    AcademicController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
    
    Note over Admin,Database: Xóa niên khóa
    
    Admin->>Client: Chọn niên khóa và click xóa
    Client->>AcademicController: DELETE /api/admin/academics/years/{id}
    
    AcademicController->>AcademicService: deleteYear(id)
    
    AcademicService->>YearRepository: findById(id)
    YearRepository->>Database: SELECT * FROM academic_years WHERE id = ?
    Database-->>YearRepository: AcademicYear
    YearRepository-->>AcademicService: AcademicYear
    
    Note over AcademicService: Xóa niên khóa (hard delete)
    AcademicService->>YearRepository: delete(year)
    YearRepository->>Database: DELETE FROM academic_years WHERE id = ?
    Database-->>YearRepository: Deleted
    YearRepository-->>AcademicService: Deleted
    
    AcademicService-->>AcademicController: Response(success)
    AcademicController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
```

#### 1.2. Xem danh sách và chi tiết niên khóa (Get Years / Get Year By ID)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant AcademicController as AcademicAdminController
    participant AcademicService as AcademicServiceImpl
    participant YearRepository as AcademicYearRepository
    participant Database as Database

    Note over Admin,Database: Xem danh sách niên khóa
    
    Admin->>Client: Truy cập trang quản lý niên khóa
    Client->>AcademicController: GET /api/admin/academics/years
    
    AcademicController->>AcademicService: getYears()
    
    AcademicService->>YearRepository: findAll()
    YearRepository->>Database: SELECT * FROM academic_years
    Database-->>YearRepository: List<AcademicYear>
    YearRepository-->>AcademicService: List<AcademicYear>
    
    AcademicService-->>AcademicController: Response(success, List<AcademicYear>)
    AcademicController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị danh sách niên khóa
    
    Note over Admin,Database: Xem chi tiết niên khóa
    
    Admin->>Client: Click xem chi tiết niên khóa
    Client->>AcademicController: GET /api/admin/academics/years/{id}
    
    AcademicController->>AcademicService: getYear(id)
    
    AcademicService->>YearRepository: findById(id)
    YearRepository->>Database: SELECT * FROM academic_years WHERE id = ?
    Database-->>YearRepository: AcademicYear
    YearRepository-->>AcademicService: AcademicYear
    
    AcademicService-->>AcademicController: Response(success, AcademicYear)
    AcademicController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông tin chi tiết niên khóa
```

### Phần 2: Quản lý Học kì (Semester)

#### 2.1. Tạo, Cập nhật, Xóa học kì (Create/Update/Delete Semester)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant AcademicController as AcademicAdminController
    participant AcademicService as AcademicServiceImpl
    participant YearRepository as AcademicYearRepository
    participant SemesterRepository as SemesterRepository
    participant Database as Database

    Note over Admin,Database: Tạo học kì
    
    Admin->>Client: Nhập thông tin học kì<br/>(yearId, name, startDate, endDate, open)
    Client->>AcademicController: POST /api/admin/academics/semesters<br/>(SemesterRequest)
    
    AcademicController->>AcademicService: createSemester(SemesterRequest)
    
    Note over AcademicService: Kiểm tra niên khóa tồn tại
    AcademicService->>YearRepository: findById(yearId)
    YearRepository->>Database: SELECT * FROM academic_years WHERE id = ?
    Database-->>YearRepository: AcademicYear hoặc null
    YearRepository-->>AcademicService: AcademicYear
    
    Note over AcademicService: Tạo học kì mới
    AcademicService->>AcademicService: create Semester<br/>(year, name, startDate, endDate, open)
    AcademicService->>SemesterRepository: save(semester)
    SemesterRepository->>Database: INSERT INTO semesters<br/>(year_id, name, start_date, end_date, is_open)
    Database-->>SemesterRepository: Semester saved
    SemesterRepository-->>AcademicService: Semester
    
    AcademicService-->>AcademicController: Response(success, Semester)
    AcademicController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
    
    Note over Admin,Database: Cập nhật học kì
    
    Admin->>Client: Chọn học kì và cập nhật thông tin
    Client->>AcademicController: PUT /api/admin/academics/semesters/{id}<br/>(SemesterRequest)
    
    AcademicController->>AcademicService: updateSemester(id, SemesterRequest)
    
    AcademicService->>SemesterRepository: findById(id)
    SemesterRepository->>Database: SELECT * FROM semesters WHERE id = ?
    Database-->>SemesterRepository: Semester
    SemesterRepository-->>AcademicService: Semester
    
    Note over AcademicService: Kiểm tra niên khóa tồn tại
    AcademicService->>YearRepository: findById(yearId)
    YearRepository->>Database: SELECT * FROM academic_years WHERE id = ?
    Database-->>YearRepository: AcademicYear
    YearRepository-->>AcademicService: AcademicYear
    
    Note over AcademicService: Cập nhật thông tin
    AcademicService->>AcademicService: semester.setYear(year)<br/>semester.setName(name)<br/>semester.setStartDate(startDate)<br/>semester.setEndDate(endDate)<br/>semester.setOpen(open)
    AcademicService->>SemesterRepository: save(semester)
    SemesterRepository->>Database: UPDATE semesters SET<br/>year_id=?, name=?, start_date=?, end_date=?, is_open=? WHERE id=?
    Database-->>SemesterRepository: Updated
    SemesterRepository-->>AcademicService: Semester updated
    
    AcademicService-->>AcademicController: Response(success, Semester)
    AcademicController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
    
    Note over Admin,Database: Xóa học kì
    
    Admin->>Client: Chọn học kì và click xóa
    Client->>AcademicController: DELETE /api/admin/academics/semesters/{id}
    
    AcademicController->>AcademicService: deleteSemester(id)
    
    AcademicService->>SemesterRepository: findById(id)
    SemesterRepository->>Database: SELECT * FROM semesters WHERE id = ?
    Database-->>SemesterRepository: Semester
    SemesterRepository-->>AcademicService: Semester
    
    Note over AcademicService: Xóa học kì (hard delete)
    AcademicService->>SemesterRepository: delete(semester)
    SemesterRepository->>Database: DELETE FROM semesters WHERE id = ?
    Database-->>SemesterRepository: Deleted
    SemesterRepository-->>AcademicService: Deleted
    
    AcademicService-->>AcademicController: Response(success)
    AcademicController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
```

#### 2.2. Xem danh sách và chi tiết học kì (Get Semesters / Get Semester By ID)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant AcademicController as AcademicAdminController
    participant AcademicService as AcademicServiceImpl
    participant YearRepository as AcademicYearRepository
    participant SemesterRepository as SemesterRepository
    participant Database as Database

    Note over Admin,Database: Xem danh sách học kì theo niên khóa
    
    Admin->>Client: Chọn niên khóa để xem học kì
    Client->>AcademicController: GET /api/admin/academics/years/{yearId}/semesters
    
    AcademicController->>AcademicService: getSemestersByYear(yearId)
    
    AcademicService->>YearRepository: findById(yearId)
    YearRepository->>Database: SELECT * FROM academic_years WHERE id = ?
    Database-->>YearRepository: AcademicYear
    YearRepository-->>AcademicService: AcademicYear
    
    AcademicService->>SemesterRepository: findAll()
    SemesterRepository->>Database: SELECT * FROM semesters
    Database-->>SemesterRepository: List<Semester>
    SemesterRepository-->>AcademicService: List<Semester>
    
    Note over AcademicService: Lọc học kì theo yearId
    AcademicService->>AcademicService: filter(s -> s.getYear().getId() == yearId)
    
    AcademicService-->>AcademicController: Response(success, List<Semester>)
    AcademicController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị danh sách học kì
    
    Note over Admin,Database: Xem chi tiết học kì
    
    Admin->>Client: Click xem chi tiết học kì
    Client->>AcademicController: GET /api/admin/academics/semesters/{id}
    
    AcademicController->>AcademicService: getSemester(id)
    
    AcademicService->>SemesterRepository: findById(id)
    SemesterRepository->>Database: SELECT * FROM semesters WHERE id = ?
    Database-->>SemesterRepository: Semester
    SemesterRepository-->>AcademicService: Semester
    
    AcademicService-->>AcademicController: Response(success, Semester)
    AcademicController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông tin chi tiết học kì
```

#### 2.3. Bật/Tắt học kì (Toggle Semester Open/Close)

```mermaid
sequenceDiagram
    participant Admin as Admin/Manager
    participant Client as Client/Frontend
    participant AcademicController as AcademicAdminController
    participant AcademicService as AcademicServiceImpl
    participant SemesterRepository as SemesterRepository
    participant Database as Database

    Admin->>Client: Chọn học kì và bật/tắt
    Client->>AcademicController: POST /api/admin/academics/semesters/{id}/toggle<br/>(?open=true/false)
    
    AcademicController->>AcademicService: toggleSemesterOpen(id, open)
    
    AcademicService->>SemesterRepository: findById(id)
    SemesterRepository->>Database: SELECT * FROM semesters WHERE id = ?
    Database-->>SemesterRepository: Semester
    SemesterRepository-->>AcademicService: Semester
    
    Note over AcademicService: Cập nhật trạng thái:<br/>Set is_open = open
    AcademicService->>AcademicService: semester.setOpen(open)
    AcademicService->>SemesterRepository: save(semester)
    SemesterRepository->>Database: UPDATE semesters SET is_open = ?<br/>WHERE id = ?
    Database-->>SemesterRepository: Updated
    SemesterRepository-->>AcademicService: Semester updated
    
    AcademicService-->>AcademicController: Response(success, Semester)
    AcademicController-->>Client: ResponseEntity.ok()
    Client-->>Admin: Hiển thị thông báo thành công
```

## Các thành phần tham gia

1. **Admin/Manager**: Người quản trị thực hiện quản lý niên khóa và học kì
2. **Client/Frontend**: Giao diện người dùng
3. **AcademicAdminController**: Controller nhận request quản lý niên khóa và học kì
4. **AcademicServiceImpl**: Service xử lý logic quản lý niên khóa và học kì
5. **AcademicYearRepository**: Repository truy cập database cho niên khóa
6. **SemesterRepository**: Repository truy cập database cho học kì
7. **Database**: Cơ sở dữ liệu

## Các chức năng

### Phần 1: Quản lý Niên khóa (Academic Year)

#### 1.1. Tạo, Cập nhật, Xóa niên khóa
- **Tạo**: Admin nhập thông tin → Tạo AcademicYear → Lưu vào DB
- **Cập nhật**: Admin chọn niên khóa → Tìm theo ID → Cập nhật thông tin → Lưu vào DB
- **Xóa**: Admin chọn niên khóa → Tìm theo ID → Hard delete → Xóa khỏi DB

#### 1.2. Xem danh sách và chi tiết niên khóa
- **Xem danh sách**: Lấy tất cả niên khóa từ database
- **Xem chi tiết**: Tìm niên khóa theo ID và trả về thông tin

### Phần 2: Quản lý Học kì (Semester)

#### 2.1. Tạo, Cập nhật, Xóa học kì
- **Tạo**: Admin nhập thông tin → Kiểm tra niên khóa tồn tại → Tạo Semester → Lưu vào DB
- **Cập nhật**: Admin chọn học kì → Tìm theo ID → Kiểm tra niên khóa → Cập nhật thông tin → Lưu vào DB
- **Xóa**: Admin chọn học kì → Tìm theo ID → Hard delete → Xóa khỏi DB

#### 2.2. Xem danh sách và chi tiết học kì
- **Xem danh sách theo niên khóa**: Lấy tất cả học kì, lọc theo yearId
- **Xem chi tiết**: Tìm học kì theo ID và trả về thông tin

#### 2.3. Bật/Tắt học kì
1. Admin chọn học kì và bật/tắt
2. Tìm học kì theo ID
3. Cập nhật trạng thái is_open (true/false)
4. Lưu vào database
5. Trả về kết quả thành công

## Đặc điểm

- **Chỉ ADMIN và MANAGER có quyền**: Tất cả endpoint yêu cầu role ADMIN hoặc MANAGER
- **Hard Delete**: Xóa niên khóa và học kì bằng cách xóa thật khỏi database
- **Quan hệ**: Học kì thuộc về một niên khóa (AcademicYear)
- **Trạng thái học kì**: Học kì có trạng thái is_open để bật/tắt (chỉ học kì đang mở mới có thể nhận điểm)
- **Validation**: Kiểm tra niên khóa tồn tại trước khi tạo/cập nhật học kì

