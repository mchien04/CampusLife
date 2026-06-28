# Sequence Diagram — Activity Series (Chuỗi hoạt động)

Hệ thống **CampusLife** (Spring Boot + React)  
Nhóm chức năng: **Activity Series (F.20 → F.25)**  
Định dạng: `Mermaid sequenceDiagram`

---

## 1. Quản lý chuỗi hoạt động — CRUD ActivitySeries (F.20)

> **Actor:** Admin  
> **Endpoint:** `POST /api/admin/series` | `PUT /api/admin/series/{id}` | `DELETE /api/admin/series/{id}`  
> **Gộp 3 luồng (Tạo / Sửa / Xóa) trong một diagram. Mỗi luồng được bao trong `rect` để phân biệt.**

```mermaid
sequenceDiagram
    autonumber
    participant A as Admin
    participant C as Client (React)
    participant CTL as SeriesController
    participant S as ActivitySeriesService
    participant R as ActivitySeriesRepository
    participant SEM as SemesterRepository
    participant REG as SeriesRegistrationRepository
    participant DB as Database

    %% ==================== TẠO (CREATE) ====================
    rect rgb(230, 245, 255)
        Note over A,DB: **LUỒNG TẠO CHUỖI HOẠT ĐỘNG (CREATE)**
        A->>C: Nhập thông tin (name, description, semesterId, requiredActivityCount, milestoneScore) và click "Tạo"
        C->>CTL: POST /api/admin/series<br/>Body: ActivitySeriesDTO
        CTL->>S: createSeries(dto)
        S->>SEM: findById(semesterId)
        SEM->>DB: SELECT * FROM semesters WHERE id = ?
        DB-->>SEM: Semester entity
        SEM-->>S: Optional<Semester>
        alt Semester không tồn tại
            S-->>CTL: throw ResourceNotFoundException("Semester not found")
            CTL-->>C: 404 Not Found
            C-->>A: Hiển thị lỗi "Học kỳ không tồn tại"
        else Semester tồn tại
            S->>S: Validate dto (name not blank, requiredActivityCount > 0, milestoneScore >= 0)
            alt Validation thất bại
                S-->>CTL: throw ValidationException
                CTL-->>C: 400 Bad Request
                C-->>A: Hiển thị lỗi validation
            else Validation thành công
                S->>S: new ActivitySeries(name, description, semester, requiredActivityCount, milestoneScore)
                S->>R: save(series)
                R->>DB: INSERT INTO activity_series (...) VALUES (...)
                DB-->>R: ActivitySeries (đã có ID)
                R-->>S: ActivitySeries entity
                S->>S: map to ActivitySeriesResponseDTO
                S-->>CTL: ActivitySeriesResponseDTO
                CTL-->>C: 201 Created + ResponseDTO
                C-->>A: Hiển thị thông báo "Tạo chuỗi hoạt động thành công" + reload danh sách
            end
        end
    end

    %% ==================== SỬA (UPDATE) ====================
    rect rgb(255, 245, 230)
        Note over A,DB: **LUỒNG SỬA CHUỖI HOẠT ĐỘNG (UPDATE)**
        A->>C: Chọn series, chỉnh sửa thông tin và click "Lưu"
        C->>CTL: PUT /api/admin/series/{id}<br/>Body: ActivitySeriesDTO
        CTL->>S: updateSeries(id, dto)
        S->>R: findById(id)
        R->>DB: SELECT * FROM activity_series WHERE id = ?
        DB-->>R: ActivitySeries entity
        R-->>S: Optional<ActivitySeries>
        alt Series không tồn tại
            S-->>CTL: throw ResourceNotFoundException("Series not found")
            CTL-->>C: 404 Not Found
            C-->>A: Hiển thị lỗi "Chuỗi hoạt động không tồn tại"
        else Series tồn tại
            S->>SEM: findById(dto.semesterId)
            SEM->>DB: SELECT * FROM semesters WHERE id = ?
            DB-->>SEM: Semester entity
            SEM-->>S: Optional<Semester>
            alt Semester không tồn tại
                S-->>CTL: throw ResourceNotFoundException("Semester not found")
                CTL-->>C: 404 Not Found
                C-->>A: Hiển thị lỗi "Học kỳ không tồn tại"
            else Semester tồn tại
                S->>S: Validate dto
                alt Validation thất bại
                    S-->>CTL: throw ValidationException
                    CTL-->>C: 400 Bad Request
                    C-->>A: Hiển thị lỗi validation
                else Validation thành công
                    S->>S: series.setName(dto.name)<br/>series.setDescription(dto.description)<br/>series.setSemester(semester)<br/>series.setRequiredActivityCount(...)<br/>series.setMilestoneScore(...)
                    S->>R: save(series)
                    R->>DB: UPDATE activity_series SET ... WHERE id = ?
                    DB-->>R: Updated ActivitySeries
                    R-->>S: ActivitySeries entity
                    S->>S: map to ActivitySeriesResponseDTO
                    S-->>CTL: ActivitySeriesResponseDTO
                    CTL-->>C: 200 OK + ResponseDTO
                    C-->>A: Hiển thị thông báo "Cập nhật thành công"
                end
            end
        end
    end

    %% ==================== XÓA (DELETE) ====================
    rect rgb(255, 230, 230)
        Note over A,DB: **LUỒNG XÓA CHUỖI HOẠT ĐỘNG (DELETE)**
        A->>C: Chọn series và click "Xóa" + xác nhận
        C->>CTL: DELETE /api/admin/series/{id}
        CTL->>S: deleteSeries(id)
        S->>R: findById(id)
        R->>DB: SELECT * FROM activity_series WHERE id = ?
        DB-->>R: ActivitySeries entity
        R-->>S: Optional<ActivitySeries>
        alt Series không tồn tại
            S-->>CTL: throw ResourceNotFoundException("Series not found")
            CTL-->>C: 404 Not Found
            C-->>A: Hiển thị lỗi "Chuỗi hoạt động không tồn tại"
        else Series tồn tại
            S->>REG: existsBySeriesId(id)
            REG->>DB: SELECT COUNT(*) FROM series_registrations WHERE series_id = ?
            DB-->>REG: count (n)
            REG-->>S: boolean (n > 0)
            alt Đã có sinh viên đăng ký
                S-->>CTL: throw BusinessException("Không thể xóa: đã có đăng ký")
                CTL-->>C: 409 Conflict
                C-->>A: Hiển thị lỗi "Không thể xóa vì đã có sinh viên đăng ký"
            else Chưa có đăng ký
                S->>R: deleteById(id)
                R->>DB: DELETE FROM activity_series WHERE id = ?
                DB-->>R: void
                R-->>S: void
                S-->>CTL: void
                CTL-->>C: 204 No Content
                C-->>A: Hiển thị thông báo "Xóa thành công" + reload danh sách
            end
        end
    end
```

---

## 2. Thêm hoạt động vào chuỗi (F.21)

> **Actor:** Admin  
> **Endpoint:** `POST /api/admin/series/{seriesId}/activities/{activityId}`  
> **Mục tiêu:** Liên kết Activity gốc vào Series; nếu cần thì tạo bản sao (child activity) riêng cho series.

```mermaid
sequenceDiagram
    autonumber
    participant A as Admin
    participant C as Client (React)
    participant CTL as SeriesController
    participant S as ActivitySeriesService
    participant SER_R as ActivitySeriesRepository
    participant ACT_R as ActivityRepository
    participant ITEM_R as ActivitySeriesItemRepository
    participant DB as Database

    Note over A,DB: **LUỒNG THÊM HOẠT ĐỘNG VÀO CHUỖI (ATTACH ACTIVITY)**

    A->>C: Chọn Series → chọn Activity gốc → click "Thêm vào chuỗi"
    C->>CTL: POST /api/admin/series/{seriesId}/activities/{activityId}
    CTL->>S: attachActivityToSeries(seriesId, activityId, createChild)

    S->>SER_R: findById(seriesId)
    SER_R->>DB: SELECT * FROM activity_series WHERE id = ?
    DB-->>SER_R: ActivitySeries entity
    SER_R-->>S: Optional<ActivitySeries>
    alt Series không tồn tại
        S-->>CTL: throw ResourceNotFoundException("Series not found")
        CTL-->>C: 404 Not Found
        C-->>A: Hiển thị lỗi "Chuỗi hoạt động không tồn tại"
    else Series tồn tại
        S->>ACT_R: findById(activityId)
        ACT_R->>DB: SELECT * FROM activities WHERE id = ?
        DB-->>ACT_R: Activity entity
        ACT_R-->>S: Optional<Activity>
        alt Activity không tồn tại
            S-->>CTL: throw ResourceNotFoundException("Activity not found")
            CTL-->>C: 404 Not Found
            C-->>A: Hiển thị lỗi "Hoạt động không tồn tại"
        else Activity tồn tại
            S->>ITEM_R: existsBySeriesIdAndActivityId(seriesId, activityId)
            ITEM_R->>DB: SELECT COUNT(*) FROM activity_series_items WHERE series_id = ? AND activity_id = ?
            DB-->>ITEM_R: count
            ITEM_R-->>S: boolean
            alt Activity đã tồn tại trong chuỗi
                S-->>CTL: throw BusinessException("Activity đã có trong chuỗi")
                CTL-->>C: 409 Conflict
                C-->>A: Hiển thị lỗi "Hoạt động đã tồn tại trong chuỗi"
            else Activity chưa có trong chuỗi
                alt createChild == true
                    S->>S: new Activity(parentActivity, seriesId)<br/>// Tạo bản sao (child) gắn với series
                    S->>ACT_R: save(childActivity)
                    ACT_R->>DB: INSERT INTO activities (...) VALUES (...)
                    DB-->>ACT_R: childActivity (đã có ID)
                    ACT_R-->>S: Activity entity (child)
                    S->>S: activityToLink = childActivity
                else createChild == false
                    S->>S: activityToLink = originalActivity
                end
                S->>S: new ActivitySeriesItem(series, activityToLink, orderIndex)
                S->>ITEM_R: save(item)
                ITEM_R->>DB: INSERT INTO activity_series_items (...) VALUES (...)
                DB-->>ITEM_R: ActivitySeriesItem (đã có ID)
                ITEM_R-->>S: ActivitySeriesItem entity
                S->>S: map to ActivitySeriesItemResponseDTO
                S-->>CTL: ActivitySeriesItemResponseDTO
                CTL-->>C: 201 Created + ResponseDTO
                C-->>A: Hiển thị thông báo "Thêm hoạt động vào chuỗi thành công" + cập nhật danh sách
            end
        end
    end
```

---

## 3. Đăng ký toàn bộ chuỗi (F.22)

> **Actor:** Student  
> **Endpoint:** `POST /api/series/{seriesId}/register`  
> **Mục tiêu:** Sinh viên đăng ký một lần cho toàn bộ chuỗi; hệ thống tự động tạo Registration cho từng Activity trong chuỗi.

```mermaid
sequenceDiagram
    autonumber
    participant St as Student
    participant C as Client (React)
    participant CTL as SeriesController
    participant S as ActivitySeriesService
    participant SER_R as ActivitySeriesRepository
    participant REG_S as SeriesRegistrationService
    participant REG_R as SeriesRegistrationRepository
    participant ACT_REG_S as RegistrationService
    participant ACT_REG_R as RegistrationRepository
    participant ITEM_R as ActivitySeriesItemRepository
    participant DB as Database

    Note over St,DB: **LUỒNG ĐĂNG KÝ TOÀN BỘ CHUỖI (BULK REGISTER)**

    St->>C: Xem chi tiết chuỗi → click "Đăng ký chuỗi"
    C->>CTL: POST /api/series/{seriesId}/register<br/>Header: Bearer <token>
    CTL->>CTL: Extract studentId from JWT Authentication
    CTL->>S: registerForSeries(seriesId, studentId)

    S->>SER_R: findById(seriesId)
    SER_R->>DB: SELECT * FROM activity_series WHERE id = ?
    DB-->>SER_R: ActivitySeries entity
    SER_R-->>S: Optional<ActivitySeries>
    alt Series không tồn tại
        S-->>CTL: throw ResourceNotFoundException("Series not found")
        CTL-->>C: 404 Not Found
        C-->>St: Hiển thị lỗi "Chuỗi hoạt động không tồn tại"
    else Series tồn tại
        alt Series không ở trạng thái OPEN
            S-->>CTL: throw BusinessException("Chuỗi hoạt động không mở đăng ký")
            CTL-->>C: 403 Forbidden / 409 Conflict
            C-->>St: Hiển thị lỗi "Chuỗi hoạt động chưa mở đăng ký hoặc đã đóng"
        else Series đang mở
            S->>REG_R: findBySeriesIdAndStudentId(seriesId, studentId)
            REG_R->>DB: SELECT * FROM series_registrations WHERE series_id = ? AND student_id = ?
            DB-->>REG_R: SeriesRegistration entity (nếu có)
            REG_R-->>S: Optional<SeriesRegistration>
            alt Student đã đăng ký
                S-->>CTL: throw BusinessException("Bạn đã đăng ký chuỗi này")
                CTL-->>C: 409 Conflict
                C-->>St: Hiển thị lỗi "Bạn đã đăng ký chuỗi hoạt động này"
            else Student chưa đăng ký
                S->>REG_S: createSeriesRegistration(seriesId, studentId)
                REG_S->>REG_S: new SeriesRegistration(series, studentId, status=PENDING)
                REG_S->>REG_R: save(seriesRegistration)
                REG_R->>DB: INSERT INTO series_registrations (...) VALUES (...)
                DB-->>REG_R: SeriesRegistration (đã có ID)
                REG_R-->>REG_S: SeriesRegistration entity
                REG_S-->>S: SeriesRegistration entity

                S->>ITEM_R: findBySeriesId(seriesId)
                ITEM_R->>DB: SELECT * FROM activity_series_items WHERE series_id = ?<br/>JOIN activities ON ...
                DB-->>ITEM_R: List<ActivitySeriesItem> (các activity trong chuỗi)
                ITEM_R-->>S: List<ActivitySeriesItem>

                S->>ACT_REG_S: bulkCreateRegistrations(studentId, activities, seriesRegistration)
                loop Mỗi Activity trong chuỗi
                    ACT_REG_S->>ACT_REG_S: new Registration(studentId, activity, seriesRegistration, status=PENDING)
                    ACT_REG_S->>ACT_REG_R: save(registration)
                    ACT_REG_R->>DB: INSERT INTO registrations (...) VALUES (...)
                    DB-->>ACT_REG_R: Registration (đã có ID)
                    ACT_REG_R-->>ACT_REG_S: Registration entity
                end
                ACT_REG_S-->>S: List<Registration>

                S->>S: map to SeriesRegistrationResponseDTO
                S-->>CTL: SeriesRegistrationResponseDTO (bao gồm series + danh sách registrations)
                CTL-->>C: 201 Created + ResponseDTO
                C-->>St: Hiển thị thông báo "Đăng ký chuỗi hoạt động thành công" + hiển thị danh sách hoạt động đã đăng ký
            end
        end
    end
```

---

## 4. Xem tiến độ trong chuỗi (F.23)

> **Actor:** Student  
> **Endpoint:** `GET /api/series/{seriesId}/progress/my`  
> **Mục tiêu:** Sinh viên xem tỷ lệ hoàn thành (%) và trạng thái từng activity trong chuỗi.

```mermaid
sequenceDiagram
    autonumber
    participant St as Student
    participant C as Client (React)
    participant CTL as SeriesController
    participant S as ActivitySeriesService
    participant SER_R as ActivitySeriesRepository
    participant REG_R as SeriesRegistrationRepository
    participant ACT_REG_R as RegistrationRepository
    participant ITEM_R as ActivitySeriesItemRepository
    participant DB as Database

    Note over St,DB: **LUỒNG XEM TIẾN ĐỘ TRONG CHUỖI (MY PROGRESS)**

    St->>C: Truy cập trang "Tiến độ chuỗi hoạt động"
    C->>CTL: GET /api/series/{seriesId}/progress/my<br/>Header: Bearer <token>
    CTL->>CTL: Extract studentId from JWT Authentication
    CTL->>S: getMyProgress(seriesId, studentId)

    S->>SER_R: findById(seriesId)
    SER_R->>DB: SELECT * FROM activity_series WHERE id = ?
    DB-->>SER_R: ActivitySeries entity
    SER_R-->>S: Optional<ActivitySeries>
    alt Series không tồn tại
        S-->>CTL: throw ResourceNotFoundException("Series not found")
        CTL-->>C: 404 Not Found
        C-->>St: Hiển thị lỗi "Chuỗi hoạt động không tồn tại"
    else Series tồn tại
        S->>REG_R: findBySeriesIdAndStudentId(seriesId, studentId)
        REG_R->>DB: SELECT * FROM series_registrations WHERE series_id = ? AND student_id = ?
        DB-->>REG_R: SeriesRegistration entity (nếu có)
        REG_R-->>S: Optional<SeriesRegistration>
        alt Student chưa đăng ký chuỗi
            S-->>CTL: throw BusinessException("Bạn chưa đăng ký chuỗi này")
            CTL-->>C: 403 Forbidden / 409 Conflict
            C-->>St: Hiển thị lỗi "Bạn chưa đăng ký chuỗi hoạt động này"
        else Student đã đăng ký
            S->>ITEM_R: findBySeriesId(seriesId)
            ITEM_R->>DB: SELECT * FROM activity_series_items WHERE series_id = ?<br/>JOIN activities ON ... ORDER BY order_index
            DB-->>ITEM_R: List<ActivitySeriesItem>
            ITEM_R-->>S: List<ActivitySeriesItem> (totalItems = N)

            S->>ACT_REG_R: findByStudentIdAndSeriesRegistration(studentId, seriesRegistration)
            ACT_REG_R->>DB: SELECT * FROM registrations WHERE student_id = ? AND series_registration_id = ?
            DB-->>ACT_REG_R: List<Registration>
            ACT_REG_R-->>S: List<Registration>

            S->>S: Đếm số registration có status = ATTENDED
            S->>S: attendedCount = count(ATTENDED)<br/>totalCount = N<br/>progressPercent = (attendedCount / N) * 100

            S->>S: Map từng Activity → RegistrationStatus<br/>Tạo List<ActivityProgressDTO><br/>(activityId, name, status, checkInTime)

            S->>S: new SeriesProgressDTO(seriesId, seriesName, attendedCount, totalCount, progressPercent, activityProgressList)
            S-->>CTL: SeriesProgressDTO
            CTL-->>C: 200 OK + SeriesProgressDTO
            C-->>St: Hiển thị:<br/>- Progress bar (progressPercent%)<br/>- attendedCount / totalCount<br/>- Danh sách activity với trạng thái (Đã tham gia / Chưa tham gia / Chưa diễn ra)
        end
    end
```

---

## 5. Tổng quan chuỗi hoạt động (F.24)

> **Actor:** Admin / Student  
> **Endpoint:** `GET /api/series/{seriesId}/overview`  
> **Mục tiêu:** Xem thông tin tổng quan: danh sách activity, tổng số đăng ký, số đã hoàn thành, tỷ lệ hoàn thành.

```mermaid
sequenceDiagram
    autonumber
    participant U as Admin/Student
    participant C as Client (React)
    participant CTL as SeriesController
    participant S as ActivitySeriesService
    participant SER_R as ActivitySeriesRepository
    participant REG_R as SeriesRegistrationRepository
    participant ACT_REG_R as RegistrationRepository
    participant ITEM_R as ActivitySeriesItemRepository
    participant DB as Database

    Note over U,DB: **LUỒNG XEM TỔNG QUAN CHUỖI (SERIES OVERVIEW)**

    U->>C: Truy cập trang "Tổng quan chuỗi hoạt động"
    C->>CTL: GET /api/series/{seriesId}/overview
    CTL->>S: getSeriesOverview(seriesId)

    S->>SER_R: findById(seriesId)
    SER_R->>DB: SELECT * FROM activity_series WHERE id = ?
    DB-->>SER_R: ActivitySeries entity
    SER_R-->>S: Optional<ActivitySeries>
    alt Series không tồn tại
        S-->>CTL: throw ResourceNotFoundException("Series not found")
        CTL-->>C: 404 Not Found
        C-->>U: Hiển thị lỗi "Chuỗi hoạt động không tồn tại"
    else Series tồn tại
        S->>ITEM_R: findBySeriesId(seriesId)
        ITEM_R->>DB: SELECT * FROM activity_series_items WHERE series_id = ?<br/>JOIN activities ON ... ORDER BY order_index
        DB-->>ITEM_R: List<ActivitySeriesItem>
        ITEM_R-->>S: List<ActivitySeriesItem> (activityList)

        S->>REG_R: countBySeriesId(seriesId)
        REG_R->>DB: SELECT COUNT(*) FROM series_registrations WHERE series_id = ?
        DB-->>REG_R: totalRegistrations
        REG_R-->>S: long totalRegistrations

        S->>REG_R: countBySeriesIdAndStatus(seriesId, COMPLETED)
        REG_R->>DB: SELECT COUNT(*) FROM series_registrations WHERE series_id = ? AND status = 'COMPLETED'
        DB-->>REG_R: completedRegistrations
        REG_R-->>S: long completedRegistrations

        alt totalRegistrations > 0
            S->>S: completionRate = (completedRegistrations / totalRegistrations) * 100
        else totalRegistrations == 0
            S->>S: completionRate = 0.0
        end

        S->>ACT_REG_R: countAttendedBySeries(seriesId)
        ACT_REG_R->>DB: SELECT COUNT(*) FROM registrations r<br/>JOIN series_registrations sr ON r.series_registration_id = sr.id<br/>WHERE sr.series_id = ? AND r.status = 'ATTENDED'
        DB-->>ACT_REG_R: totalAttendedCount
        ACT_REG_R-->>S: long totalAttendedCount

        S->>S: new SeriesOverviewDTO(<br/>seriesId, seriesName, description, semesterName,<br/>requiredActivityCount, milestoneScore,<br/>activityList,<br/>totalRegistrations, completedRegistrations, completionRate,<br/>totalAttendedCount<br/>)
        S-->>CTL: SeriesOverviewDTO
        CTL-->>C: 200 OK + SeriesOverviewDTO
        C-->>U: Hiển thị tổng quan:<br/>- Thông tin chuỗi (tên, mô tả, học kỳ, yêu cầu, điểm milestone)<br/>- Danh sách activity trong chuỗi<br/>- Thống kê: Tổng đăng ký, Đã hoàn thành, Tỷ lệ hoàn thành (%)<br/>- Tổng số lượt check-in
    end
```

---

## 6. Tính điểm milestone (F.25)

> **Actor:** Admin / Manager  
> **Endpoint:** `POST /api/series/{seriesId}/students/{id}/calculate-milestone`  
> **Mục tiêu:** Hệ thống kiểm tra sinh viên đã đủ số activity bắt buộc (ATTENDED) chưa; nếu đủ thì cấp điểm milestone.

```mermaid
sequenceDiagram
    autonumber
    participant U as Admin/Manager
    participant C as Client (React)
    participant CTL as SeriesController
    participant S as ActivitySeriesService
    participant SER_R as ActivitySeriesRepository
    participant REG_R as SeriesRegistrationRepository
    participant ACT_REG_R as RegistrationRepository
    participant SCORE_S as ScoreRecordService
    participant SCORE_R as ScoreRecordRepository
    participant DB as Database

    Note over U,DB: **LUỒNG TÍNH ĐIỂM MILESTONE (CALCULATE MILESTONE SCORE)**

    U->>C: Chọn Series → chọn Student → click "Tính điểm milestone"
    C->>CTL: POST /api/series/{seriesId}/students/{studentId}/calculate-milestone
    CTL->>S: calculateMilestoneScore(seriesId, studentId)

    S->>SER_R: findById(seriesId)
    SER_R->>DB: SELECT * FROM activity_series WHERE id = ?
    DB-->>SER_R: ActivitySeries entity
    SER_R-->>S: Optional<ActivitySeries>
    alt Series không tồn tại
        S-->>CTL: throw ResourceNotFoundException("Series not found")
        CTL-->>C: 404 Not Found
        C-->>U: Hiển thị lỗi "Chuỗi hoạt động không tồn tại"
    else Series tồn tại
        S->>REG_R: findBySeriesIdAndStudentId(seriesId, studentId)
        REG_R->>DB: SELECT * FROM series_registrations WHERE series_id = ? AND student_id = ?
        DB-->>REG_R: SeriesRegistration entity
        REG_R-->>S: Optional<SeriesRegistration>
        alt Student chưa đăng ký chuỗi
            S-->>CTL: throw ResourceNotFoundException("Student chưa đăng ký chuỗi")
            CTL-->>C: 404 Not Found
            C-->>U: Hiển thị lỗi "Sinh viên chưa đăng ký chuỗi này"
        else Student đã đăng ký
            S->>ACT_REG_R: countAttendedByStudentAndSeries(studentId, seriesRegistration)
            ACT_REG_R->>DB: SELECT COUNT(*) FROM registrations WHERE student_id = ? AND series_registration_id = ? AND status = 'ATTENDED'
            DB-->>ACT_REG_R: attendedCount
            ACT_REG_R-->>S: int attendedCount

            S->>S: requiredCount = series.getRequiredActivityCount()
            alt attendedCount < requiredCount
                S-->>CTL: throw BusinessException(<br/>"Sinh viên chưa đủ số hoạt động bắt buộc. " +<br/>"Đã tham gia: " + attendedCount + "/" + requiredCount)
                CTL-->>C: 409 Conflict
                C-->>U: Hiển thị lỗi:<br/>"Sinh viên chưa đủ điều kiện nhận điểm milestone.<br/>Đã tham gia: X/Y hoạt động"
            else attendedCount >= requiredCount
                S->>SCORE_S: findOrCreateScoreRecord(studentId, series)
                SCORE_S->>SCORE_R: findByStudentIdAndSeriesId(studentId, seriesId)
                SCORE_R->>DB: SELECT * FROM score_records WHERE student_id = ? AND series_id = ?
                DB-->>SCORE_R: ScoreRecord entity (nếu có)
                SCORE_R-->>SCORE_S: Optional<ScoreRecord>
                alt ScoreRecord đã tồn tại
                    SCORE_S->>SCORE_S: scoreRecord.setScore(series.getMilestoneScore())<br/>scoreRecord.setCalculatedAt(now)<br/>scoreRecord.setSource("MILESTONE")
                else ScoreRecord chưa tồn tại
                    SCORE_S->>SCORE_S: new ScoreRecord(studentId, series, milestoneScore, now, "MILESTONE")
                end
                SCORE_S->>SCORE_R: save(scoreRecord)
                SCORE_R->>DB: INSERT/UPDATE score_records SET ...
                DB-->>SCORE_R: ScoreRecord entity
                SCORE_R-->>SCORE_S: ScoreRecord entity
                SCORE_S-->>S: ScoreRecord entity

                S->>S: map to MilestoneScoreDTO(studentId, seriesId, milestoneScore, attendedCount, requiredCount, calculatedAt)
                S-->>CTL: MilestoneScoreDTO
                CTL-->>C: 200 OK + MilestoneScoreDTO
                C-->>U: Hiển thị thông báo:<br/>"Cấp điểm milestone thành công!"<br/>- Điểm: milestoneScore<br/>- Đã tham gia: attendedCount/requiredCount<br/>- Thời gian tính điểm: calculatedAt
            end
        end
    end
```

---

## Tóm tắt thành phần và chức năng

| Thành phần | Vai trò | Chức năng chính trong Activity Series |
|---|---|---|
| **Admin/Manager** | Actor | Tạo, sửa, xóa chuỗi hoạt động; thêm hoạt động vào chuỗi; tính điểm milestone cho sinh viên. |
| **Student** | Actor | Đăng ký toàn bộ chuỗi; xem tiến độ cá nhân trong chuỗi; xem tổng quan chuỗi. |
| **Client (React)** | Frontend | Render UI form, hiển thị danh sách, progress bar, tổng quan; gọi API đến backend. |
| **Controller** | API Layer | Nhận request HTTP, extract JWT/auth info, điều hướng đến Service, trả về HTTP response. |
| **Service** | Business Logic | Xử lý nghiệp vụ: validate, kiểm tra điều kiện (tồn tại, trạng thái, quyền), tính toán %, điểm milestone. |
| **Repository** | Data Access | Truy vấn CRUD đến Database (JPA/Hibernate): `findById`, `save`, `delete`, `count`, `exists`. |
| **Database** | Persistence | Lưu trữ dữ liệu: `activity_series`, `activity_series_items`, `series_registrations`, `registrations`, `score_records`, `semesters`, `activities`. |

### Các bảng dữ liệu chính liên quan

| Bảng | Mô tả |
|---|---|
| `activity_series` | Lưu thông tin chuỗi hoạt động (name, description, semester_id, required_activity_count, milestone_score). |
| `activity_series_items` | Liên kết nhiều-nhiều giữa `activity_series` và `activities` (thứ tự, bản sao child). |
| `series_registrations` | Lưu đăng ký của sinh viên cho một chuỗi (student_id, series_id, status, registered_at). |
| `registrations` | Lưu đăng ký của sinh viên cho từng activity (được tạo bulk khi đăng ký chuỗi). |
| `score_records` | Lưu điểm milestone được tính cho sinh viên (student_id, series_id, score, source, calculated_at). |
| `semesters` | Lưu thông tin học kỳ (được tham chiếu bởi activity_series). |
| `activities` | Lưu thông tin hoạt động gốc (được tham chiếu qua activity_series_items). |
