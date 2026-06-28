# Sequence Diagram - Activity Module (Hoạt động)

Hệ thống: **CampusLife** (Spring Boot + React)

---

## Tóm tắt các Sequence Diagram

| STT | Chức năng | Mã yêu cầu | Ghi chú |
|-----|-----------|-----------|---------|
| 1 | Thêm/Sửa/Xóa hoạt động | 3.3.14 - 3.3.16 | Gộp CRUD |
| 2 | Xem danh sách hoạt động | 3.3.17 | Gộp public + admin |
| 3 | Công khai / Hủy công khai hoạt động | E.16 | Gộp publish + unpublish |
| 4 | Sao chép hoạt động | E.17 | Clone activity |
| 5 | Xem trước preset hoạt động | E.18 | Preview không lưu DB |
| 6 | Quản lý ảnh hoạt động | E.19 | Gộp upload/xóa/sắp xếp |

---

## 1. Thêm / Sửa / Xóa hoạt động (CRUD Activity)

> Gộp 3 luồng: Tạo (3.3.14), Sửa (3.3.15), Xóa (3.3.16)

```mermaid
sequenceDiagram
    autonumber
    participant Admin as Admin/Manager
    participant Client as React Client
    participant AC as ActivityController
    participant AS as ActivityService
    participant SR as SemesterRepository
    participant AR as ActivityRepository
    participant RR as RegistrationRepository
    participant DB as Database
    participant FS as FileStorage

    Note over Admin, FS: === LUỒNG TẠO HOẠT ĐỘNG (POST /api/admin/activities) ===

    Admin->>Client: Nhập form: name, description, startDate, endDate,<br/>location, maxParticipants, score, semesterId
    Client->>AC: POST /api/admin/activities<br/>Body: ActivityRequestDTO (JSON)
    AC->>AS: createActivity(requestDTO)
    AS->>SR: findById(semesterId)
    SR->>DB: SELECT * FROM semesters WHERE id = ?
    DB-->>SR:' "Semester record"'
    SR-->>AS: Optional<Semester>
    alt Semester không tồn tại
        AS-->>AC: throw SemesterNotFoundException
        AC-->>Client:' 400 Bad Request<br/>{"error": "Semester not found"}'
        Client-->>Admin: Hiển thị lỗi: Học kỳ không tồn tại
    else Semester tồn tại
        AS->>AS: Tạo Activity entity:<br/>- isPublished = false (mặc định)<br/>- registrations = 0<br/>- createdAt = now()
        AS->>AR: save(activity)
        AR->>DB: INSERT INTO activities (...)<br/>VALUES (...)
        DB-->>AR:' "Activity record (generated ID)"'
        AR-->>AS:' "Activity entity"'
        AS->>AS: Map Activity -> ActivityResponseDTO
        AS-->>AC: ActivityResponseDTO
        AC-->>Client: 201 Created<br/>Body: ActivityResponseDTO
        Client-->>Admin:' Hiển thị thông báo: "Tạo hoạt động thành công"'
    end

    Note over Admin, FS: === LUỒNG SỬA HOẠT ĐỘNG (PUT /api/admin/activities/{id}) ===

    Admin->>Client: Chọn hoạt động → Cập nhật fields
    Client->>AC: PUT /api/admin/activities/{id}<br/>Body: ActivityRequestDTO
    AC->>AS: updateActivity(id, requestDTO)
    AS->>AR: findById(id)
    AR->>DB: SELECT * FROM activities WHERE id = ?
    DB-->>AR:' "Activity record"'
    AR-->>AS:' "Optional<Activity>"'
    alt Activity không tồn tại
        AS-->>AC: throw ActivityNotFoundException
        AC-->>Client: 404 Not Found
        Client-->>Admin: Hiển thị lỗi: Hoạt động không tồn tại
    else Activity tồn tại
        AS->>SR: findById(semesterId) [nếu semesterId thay đổi]
        SR->>DB: SELECT * FROM semesters WHERE id = ?
        DB-->>SR:' "Semester record"'
        SR-->>AS: Optional<Semester>
        AS->>AS: Update fields:<br/>name, description, startDate, endDate,<br/>location, maxParticipants, score, semesterId<br/>updatedAt = now()
        AS->>AR: save(activity)
        AR->>DB: UPDATE activities SET ... WHERE id = ?
        DB-->>AR:' "Updated record"'
        AR-->>AS:' "Activity entity"'
        AS->>AS: Map -> ActivityResponseDTO
        AS-->>AC: ActivityResponseDTO
        AC-->>Client: 200 OK<br/>Body: ActivityResponseDTO
        Client-->>Admin:' Hiển thị thông báo: "Cập nhật hoạt động thành công"'
    end

    Note over Admin, FS: === LUỒNG XÓA HOẠT ĐỘNG (DELETE /api/admin/activities/{id}) ===

    Admin->>Client: Chọn hoạt động → Nhấn Xóa
    Client->>AC: DELETE /api/admin/activities/{id}
    AC->>AS: deleteActivity(id)
    AS->>AR: findById(id)
    AR->>DB: SELECT * FROM activities WHERE id = ?
    DB-->>AR:' "Activity record"'
    AR-->>AS:' "Optional<Activity>"'
    alt Activity không tồn tại
        AS-->>AC: throw ActivityNotFoundException
        AC-->>Client: 404 Not Found
        Client-->>Admin: Hiển thị lỗi: Hoạt động không tồn tại
    else Activity tồn tại
        AS->>RR: existsByActivityId(id)
        RR->>DB: SELECT COUNT(*) FROM registrations WHERE activity_id = ?
        DB-->>RR:' "count (0 hoặc >0)"'
        RR-->>AS:' "boolean (hasRegistrations)"'
        alt Có sinh viên đã đăng ký (hasRegistrations = true)
            AS->>AS: Soft delete:<br/>- isDeleted = true<br/>- deletedAt = now()
            AS->>AR: save(activity)
            AR->>DB: UPDATE activities SET is_deleted=1, deleted_at=... WHERE id = ?
            DB-->>AR:' "Updated record"'
            AS-->>AC:' Message: "Soft deleted (has registrations)"'
            AC-->>Client:' 200 OK<br/>{"message": "Hoạt động đã được chuyển vào thùng rác (có sinh viên đăng ký)"}'
        else Không có đăng ký (hasRegistrations = false)
            AS->>AR: deleteById(id)
            AR->>DB: DELETE FROM activities WHERE id = ?
            DB-->>AR:' "void"'
            AS->>AS: [Nếu có ảnh] Xóa file ảnh vật lý
            AS->>FS: deleteFiles(imageUrls)
            FS-->>AS: Xóa thành công / một số file không tồn tại
            AS-->>AC:' Message: "Hard deleted"'
            AC-->>Client:' 200 OK<br/>{"message": "Xóa hoạt động thành công"}'
        end
        Client-->>Admin: Hiển thị thông báo kết quả xóa
    end
```

---

## 2. Xem danh sách hoạt động (Public + Admin)

> Gộp 2 luồng: GET /api/activities (public, cho Student) và GET /api/admin/activities (cho Admin)

```mermaid
sequenceDiagram
    autonumber
    participant Student as Student
    participant Admin as Admin/Manager
    participant Client as React Client
    participant AC as ActivityController
    participant AS as ActivityService
    participant AR as ActivityRepository
    participant SR as SemesterRepository
    participant DB as Database

    Note over Student, DB: === LUỒNG SINH VIÊN XEM HOẠT ĐỘNG (GET /api/activities) ===

    Student->>Client: Truy cập trang Danh sách hoạt động
    Client->>AC: GET /api/activities<br/>Query: ?semesterId=&search=&page=&size=
    AC->>AS: getPublishedActivities(filterDTO)
    AS->>SR: findActiveSemester()
    SR->>DB: SELECT * FROM semesters<br/>WHERE status = 'OPEN' ORDER BY start_date DESC LIMIT 1
    DB-->>SR:' "Semester record (semester đang mở)"'
    SR-->>AS: Optional<Semester>
    alt Không có semester đang mở
        AS->>AS: fallback: lấy semester gần nhất
    end
    AS->>AS: Xây dựng điều kiện:<br/>- isPublished = true<br/>- isDeleted = false<br/>- semesterId = currentSemesterId<br/>- [optional] search keyword (name LIKE)
    AS->>AR: findAllByConditions(spec, pageable)
    AR->>DB: SELECT * FROM activities<br/>WHERE is_published=1 AND is_deleted=0<br/>AND semester_id = ? AND name LIKE ?<br/>LIMIT ? OFFSET ?
    DB-->>AR:' "List<Activity> + Total count"'
    AR-->>AS:' "Page<Activity>"'
    AS->>AS: Map mỗi Activity -> ActivitySummaryDTO<br/>(id, name, startDate, endDate, location,<br/>maxParticipants, registrations, score, isOpen)
    AS-->>AC: Page<ActivitySummaryDTO>
    AC-->>Client: 200 OK<br/>Body: {content:[], totalElements, totalPages, ...}
    Client->>Client: Render danh sách cards/list
    Client-->>Student: Hiển thị các hoạt động đang mở<br/>có thể đăng ký

    Note over Student, DB: === LUỒNG ADMIN XEM TẤT CẢ HOẠT ĐỘNG (GET /api/admin/activities) ===

    Admin->>Client: Truy cập trang Quản lý hoạt động
    Client->>AC: GET /api/admin/activities<br/>Query: ?semesterId=&isPublished=&search=&page=&size=&sort=
    AC->>AS: getAllActivitiesForAdmin(filterDTO)
    AS->>AS: Kiểm tra quyền Admin/Manager
    AS->>AS: Xây dựng điều kiện (không filter isDeleted mặc định<br/>trừ khi có param includeDeleted=true):<br/>- [optional] semesterId<br/>- [optional] isPublished (true/false)<br/>- [optional] search keyword<br/>- isDeleted = false (default)
    AS->>AR: findAllByConditions(spec, pageable)
    AR->>DB: SELECT * FROM activities<br/>WHERE semester_id = ? AND is_published = ?<br/>AND name LIKE ? AND is_deleted = 0<br/>ORDER BY ? LIMIT ? OFFSET ?
    DB-->>AR:' "List<Activity> + Total count"'
    AR-->>AS:' "Page<Activity>"'
    AS->>AS: Map mỗi Activity -> ActivityAdminDTO<br/>(thêm: isPublished, createdAt, updatedAt,<br/>registrationCount, createdBy)
    AS-->>AC: Page<ActivityAdminDTO>
    AC-->>Client: 200 OK<br/>Body: {content:[], totalElements, totalPages, ...}
    Client->>Client: Render bảng quản lý với các action<br/>(Edit, Publish, Delete, Copy)
    Client-->>Admin: Hiển thị toàn bộ hoạt động<br/>theo bộ lọc đã chọn
```

---

## 3. Công khai / Hủy công khai hoạt động (Publish / Unpublish)

> Gộp 2 luồng: PUT /api/admin/activities/{id}/publish và PUT /api/admin/activities/{id}/unpublish

```mermaid
sequenceDiagram
    autonumber
    participant Admin as Admin/Manager
    participant Client as React Client
    participant AC as ActivityController
    participant AS as ActivityService
    participant AR as ActivityRepository
    participant NR as NotificationService
    participant DB as Database

    Note over Admin, DB: === LUỒNG CÔNG KHAI HOẠT ĐỘNG (Publish) ===

    Admin->>Client: Chọn hoạt động → Nhấn "Công khai"
    Client->>AC: PUT /api/admin/activities/{id}/publish
    AC->>AS: publishActivity(id)
    AS->>AR: findById(id)
    AR->>DB: SELECT * FROM activities WHERE id = ?
    DB-->>AR:' "Activity record"'
    AR-->>AS:' "Optional<Activity>"'
    alt Activity không tồn tại
        AS-->>AC: throw ActivityNotFoundException
        AC-->>Client: 404 Not Found
        Client-->>Admin: Hiển thị lỗi: Hoạt động không tồn tại
    else Activity tồn tại
        alt isPublished = true (đã công khai)
            AS-->>AC: throw AlreadyPublishedException
            AC-->>Client:' 409 Conflict<br/>{"error": "Hoạt động đã được công khai"}'
        else isPublished = false
            AS->>AS: activity.setIsPublished(true)<br/>activity.setPublishedAt(now())
            AS->>AR: save(activity)
            AR->>DB: UPDATE activities SET is_published=1, published_at=... WHERE id = ?
            DB-->>AR:' "Updated record"'
            AR-->>AS:' "Activity entity"'
            AS->>NR: sendNotificationToAllStudents(activity)
            NR->>NR: Tạo notification:<br/>"Hoạt động mới: [activity.name]"<br/>type: NEW_ACTIVITY<br/>target: ALL_STUDENTS
            NR->>DB: INSERT INTO notifications (...)
            DB-->>NR:' "Notification record"'
            NR-->>AS:' "void"'
            AS->>AS: Map -> ActivityResponseDTO
            AS-->>AC: ActivityResponseDTO
            AC-->>Client: 200 OK<br/>Body: ActivityResponseDTO + message published
            Client-->>Admin:' Hiển thị thông báo: "Công khai thành công + Đã gửi thông báo"'
        end
    end

    Note over Admin, DB: === LUỒNG HỦY CÔNG KHAI HOẠT ĐỘNG (Unpublish) ===

    Admin->>Client: Chọn hoạt động → Nhấn "Hủy công khai"
    Client->>AC: PUT /api/admin/activities/{id}/unpublish
    AC->>AS: unpublishActivity(id)
    AS->>AR: findById(id)
    AR->>DB: SELECT * FROM activities WHERE id = ?
    DB-->>AR:' "Activity record"'
    AR-->>AS:' "Optional<Activity>"'
    alt Activity không tồn tại
        AS-->>AC: throw ActivityNotFoundException
        AC-->>Client: 404 Not Found
        Client-->>Admin: Hiển thị lỗi: Hoạt động không tồn tại
    else Activity tồn tại
        alt isPublished = false (chưa công khai)
            AS-->>AC: throw NotPublishedException
            AC-->>Client:' 409 Conflict<br/>{"error": "Hoạt động chưa được công khai"}'
        else isPublished = true
            AS->>AS: activity.setIsPublished(false)<br/>activity.setPublishedAt(null)
            AS->>AR: save(activity)
            AR->>DB: UPDATE activities SET is_published=0, published_at=NULL WHERE id = ?
            DB-->>AR:' "Updated record"'
            AR-->>AS:' "Activity entity"'
            AS->>AS: Map -> ActivityResponseDTO
            AS-->>AC: ActivityResponseDTO
            AC-->>Client: 200 OK<br/>Body: ActivityResponseDTO + message unpublish
            Client-->>Admin:' Hiển thị thông báo: "Hủy công khai thành công"'
        end
    end
```

---

## 4. Sao chép hoạt động (Copy Activity)

> Mã: E.17 — POST /api/admin/activities/{id}/copy

```mermaid
sequenceDiagram
    autonumber
    participant Admin as Admin/Manager
    participant Client as React Client
    participant AC as ActivityController
    participant AS as ActivityService
    participant AR as ActivityRepository
    participant AIR as ActivityImageRepository
    participant APR as ActivityPresetRepository
    participant DB as Database
    participant FS as FileStorage

    Note over Admin, FS: === LUỒNG SAO CHÉP HOẠT ĐỘNG ===

    Admin->>Client: Chọn hoạt động → Nhấn "Sao chép"
    Client->>AC: POST /api/admin/activities/{id}/copy
    AC->>AS: copyActivity(id)
    AS->>AR: findById(id)
    AR->>DB: SELECT * FROM activities WHERE id = ?
    DB-->>AR:' "Activity record (original)"'
    AR-->>AS:' "Optional<Activity>"'
    alt Activity gốc không tồn tại hoặc đã bị xóa
        AS-->>AC: throw ActivityNotFoundException
        AC-->>Client: 404 Not Found
        Client-->>Admin: Hiển thị lỗi: Hoạt động gốc không tồn tại
    else Activity gốc tồn tại
        AS->>AS: Tạo Activity mới (newActivity):<br/>- name = original.name + " (Copy)"<br/>- description = original.description<br/>- startDate = original.startDate<br/>- endDate = original.endDate<br/>- location = original.location<br/>- maxParticipants = original.maxParticipants<br/>- score = original.score<br/>- semesterId = original.semesterId<br/>- registrations = 0<br/>- isPublished = false<br/>- createdAt = now()<br/>- createdBy = currentAdminId
        AS->>AR: save(newActivity)
        AR->>DB: INSERT INTO activities (...) VALUES (...)
        DB-->>AR:' "newActivity record (new ID)"'
        AR-->>AS:' "newActivity entity"'

        AS->>AIR: findAllByActivityId(original.id)
        AIR->>DB: SELECT * FROM activity_images WHERE activity_id = ? ORDER BY display_order
        DB-->>AIR:' "List<ActivityImage> (original images)"'
        AIR-->>AS:' "List<ActivityImage>"'
        loop Với mỗi ảnh của activity gốc
            AS->>FS: copyFile(originalImage.url, newPath)
            FS-->>AS: newFileUrl
            AS->>AS: Tạo ActivityImage mới:<br/>- activityId = newActivity.id<br/>- url = newFileUrl<br/>- displayOrder = original.displayOrder<br/>- createdAt = now()
            AS->>AIR: save(newImage)
            AIR->>DB: INSERT INTO activity_images (...) VALUES (...)
            DB-->>AIR:' "newImage record"'
        end

        AS->>APR: findAllByActivityId(original.id) [nếu có preset liên kết]
        APR->>DB: SELECT * FROM activity_presets WHERE activity_id = ?
        DB-->>APR:' "List<ActivityPreset> (original presets)"'
        APR-->>AS:' "List<ActivityPreset>"'
        opt Nếu có preset liên kết
            loop Với mỗi preset
                AS->>AS: Tạo ActivityPreset mới:<br/>- activityId = newActivity.id<br/>- presetId = original.presetId<br/>- customFields = original.customFields
                AS->>APR: save(newPreset)
                APR->>DB: INSERT INTO activity_presets (...) VALUES (...)
                DB-->>APR:' "newPreset record"'
            end
        end

        AS->>AS: Map newActivity -> ActivityResponseDTO
        AS-->>AC: ActivityResponseDTO
        AC-->>Client: 201 Created<br/>Body: ActivityResponseDTO
        Client-->>Admin:' Hiển thị thông báo: "Sao chép hoạt động thành công"<br/>+ Điều hướng đến trang sửa hoạt động mới'
    end
```

---

## 5. Xem trước Preset hoạt động (Preview Activity Preset)

> Mã: E.18 — POST /api/activities/presets/preview

```mermaid
sequenceDiagram
    autonumber
    participant Admin as Admin/Manager
    participant Client as React Client
    participant AC as ActivityController
    participant AS as ActivityService
    participant PR as PresetRepository
    participant DB as Database

    Note over Admin, DB: === LUỒNG XEM TRƯỚC PRESET ===

    Admin->>Client: Chọn template preset từ danh sách<br/>(ví dụ: "Hội thảo", "Thể thao", "Tình nguyện")
    Client->>AC: POST /api/activities/presets/preview<br/>Body: { "presetId": 123 }
    AC->>AS: previewPreset(presetId)
    AS->>PR: findById(presetId)
    PR->>DB: SELECT * FROM activity_presets WHERE id = ?
    DB-->>PR:' "Preset record"'
    PR-->>AS: Optional<Preset>
    alt Preset không tồn tại
        AS-->>AC: throw PresetNotFoundException
        AC-->>Client:' 404 Not Found<br/>{"error": "Preset không tồn tại"}'
        Client-->>Admin: Hiển thị lỗi: Template không tìm thấy
    else Preset tồn tại
        AS->>AS: Generate PreviewDataDTO:<br/>- suggestedName = preset.name + " [YYYY-MM-DD]"<br/>- suggestedDescription = preset.descriptionTemplate<br/>- suggestedScore = preset.defaultScore<br/>- suggestedCategory = preset.category<br/>- suggestedMaxParticipants = preset.defaultMaxParticipants<br/>- suggestedLocation = preset.defaultLocation<br/>- suggestedDuration = preset.defaultDurationHours<br/>- defaultFields = preset.customFields<br/>- previewOnly = true (không lưu DB)
        AS-->>AC: PresetPreviewDTO
        AC-->>Client: 200 OK<br/>Body: PresetPreviewDTO
        Client->>Client: Render preview form (read-only/demo)<br/>với dữ liệu đề xuất
        Client-->>Admin:' Hiển thị preview:<br/>tên gợi ý, mô tả, điểm, loại hoạt động...<br/>Admin có thể bấm "Áp dụng" để điền vào form tạo'
    end
```

---

## 6. Quản lý ảnh hoạt động (Upload / Xóa / Sắp xếp)

> Gộp 3 luồng: Upload, Delete, Reorder — Mã: E.19

```mermaid
sequenceDiagram
    autonumber
    participant Admin as Admin/Manager
    participant Client as React Client
    participant AC as ActivityController
    participant AS as ActivityService
    participant AR as ActivityRepository
    participant AIR as ActivityImageRepository
    participant DB as Database
    participant FS as FileStorage

    Note over Admin, FS: === LUỒNG UPLOAD ẢNH (POST /api/admin/activities/{id}/images) ===

    Admin->>Client: Chọn hoạt động → Upload ảnh (drag & drop / chọn file)
    Client->>Client: Validate:<br/>- file type: jpg, png, webp<br/>- max size: 5MB<br/>- max 10 ảnh
    Client->>AC: POST /api/admin/activities/{id}/images<br/>Content-Type: multipart/form-data<br/>Body: files[] (MultipartFile)
    AC->>AS: uploadImages(id, files)
    AS->>AR: findById(id)
    AR->>DB: SELECT * FROM activities WHERE id = ?
    DB-->>AR:' "Activity record"'
    AR-->>AS:' "Optional<Activity>"'
    alt Activity không tồn tại
        AS-->>AC: throw ActivityNotFoundException
        AC-->>Client: 404 Not Found
        Client-->>Admin: Hiển thị lỗi: Hoạt động không tồn tại
    else Activity tồn tại
        AS->>AIR: countByActivityId(id)
        AIR->>DB: SELECT COUNT(*) FROM activity_images WHERE activity_id = ?
        DB-->>AIR:' "currentCount"'
        AIR-->>AS:' "int currentCount"'
        alt currentCount + files.length > maxImages (10)
            AS-->>AC: throw MaxImagesExceededException
            AC-->>Client:' 400 Bad Request<br/>{"error": "Vượt quá số lượng ảnh cho phép"}'
        else Số lượng hợp lệ
            loop Với mỗi file multipart
                AS->>FS: storeFile(file, "activities/{id}/images/")
                FS-->>AS: fileUrl (e.g., /uploads/activities/123/images/uuid.jpg)
                AS->>AS: Tạo ActivityImage:<br/>- activityId = id<br/>- url = fileUrl<br/>- displayOrder = currentCount + index<br/>- fileName = originalFilename<br/>- fileSize = file.size<br/>- createdAt = now()
                AS->>AIR: save(activityImage)
                AIR->>DB: INSERT INTO activity_images (...) VALUES (...)
                DB-->>AIR:' "ActivityImage record"'
            end
            AS->>AIR: findAllByActivityIdOrderByDisplayOrder(id)
            AIR->>DB: SELECT * FROM activity_images WHERE activity_id = ? ORDER BY display_order
            DB-->>AIR:' "List<ActivityImage>"'
            AIR-->>AS:' "List<ActivityImage>"'
            AS->>AS: Map -> List<ActivityImageDTO>
            AS-->>AC: List<ActivityImageDTO>
            AC-->>Client: 201 Created<br/>Body: {images: [...], activityId: id}
            Client->>Client: Render gallery ảnh mới
            Client-->>Admin: Hiển thị ảnh đã upload + thông báo thành công
        end
    end

    Note over Admin, FS: === LUỒNG XÓA ẢNH (DELETE /api/admin/activities/{id}/images/{imageId}) ===

    Admin->>Client: Chọn ảnh trong gallery → Nhấn "Xóa"
    Client->>AC: DELETE /api/admin/activities/{id}/images/{imageId}
    AC->>AS: deleteImage(id, imageId)
    AS->>AR: findById(id)
    AR->>DB: SELECT * FROM activities WHERE id = ?
    DB-->>AR:' "Activity record"'
    AR-->>AS:' "Optional<Activity>"'
    alt Activity không tồn tại
        AS-->>AC: throw ActivityNotFoundException
        AC-->>Client: 404 Not Found
    else Activity tồn tại
        AS->>AIR: findByIdAndActivityId(imageId, id)
        AIR->>DB: SELECT * FROM activity_images WHERE id = ? AND activity_id = ?
        DB-->>AIR:' "ActivityImage record"'
        AIR-->>AS:' "Optional<ActivityImage>"'
        alt Image không tồn tại hoặc không thuộc activity
            AS-->>AC: throw ImageNotFoundException
            AC-->>Client:' 404 Not Found<br/>{"error": "Ảnh không tồn tại"}'
        else Image tồn tại
            AS->>FS: deleteFile(image.url)
            FS-->>AS: deleted / file not found (log warning)
            AS->>AIR: deleteById(imageId)
            AIR->>DB: DELETE FROM activity_images WHERE id = ?
            DB-->>AIR:' "void"'
            AS->>AIR: findAllByActivityIdOrderByDisplayOrder(id)
            AIR->>DB: SELECT * FROM activity_images WHERE activity_id = ? ORDER BY display_order
            DB-->>AIR:' "List<ActivityImage>"'
            AIR-->>AS:' "List<ActivityImage>"'
            AS->>AS: Cập nhật lại displayOrder liên tục (0, 1, 2...)
            loop Với mỗi ảnh còn lại
                AS->>AIR: updateDisplayOrder(image.id, newOrder)
                AIR->>DB: UPDATE activity_images SET display_order = ? WHERE id = ?
                DB-->>AIR:' "Updated"'
            end
            AS-->>AC:' Message: "Xóa ảnh thành công"'
            AC-->>Client:' 200 OK<br/>{"message": "Xóa ảnh thành công", remainingCount: N}'
            Client->>Client: Cập nhật gallery (xoá ảnh khỏi UI)
            Client-->>Admin: Hiển thị thông báo xóa thành công
        end
    end

    Note over Admin, FS: === LUỒNG SẮP XẾP ẢNH (PUT /api/admin/activities/{id}/images/reorder) ===

    Admin->>Client: Drag & drop thay đổi thứ tự ảnh trong gallery
    Client->>Client: Cập nhật mảng imageIds theo thứ tự mới
    Client->>AC: PUT /api/admin/activities/{id}/images/reorder<br/>Body: { "imageIds": [5, 2, 8, 1] }
    AC->>AS: reorderImages(id, imageIds)
    AS->>AR: findById(id)
    AR->>DB: SELECT * FROM activities WHERE id = ?
    DB-->>AR:' "Activity record"'
    AR-->>AS:' "Optional<Activity>"'
    alt Activity không tồn tại
        AS-->>AC: throw ActivityNotFoundException
        AC-->>Client: 404 Not Found
    else Activity tồn tại
        AS->>AIR: findAllByActivityId(id)
        AIR->>DB: SELECT * FROM activity_images WHERE activity_id = ?
        DB-->>AIR:' "List<ActivityImage>"'
        AIR-->>AS:' "List<ActivityImage>"'
        AS->>AS: Validate:<br/>- imageIds.length == existingImages.size<br/>- Tất cả ID trong imageIds phải thuộc về activity này
        alt Validation thất bại
            AS-->>AC: throw InvalidReorderException
            AC-->>Client:' 400 Bad Request<br/>{"error": "Danh sách ảnh không hợp lệ"}'
        else Validation thành công
            loop Với mỗi index, imageId trong imageIds
                AS->>AIR: updateDisplayOrder(imageId, index)
                AIR->>DB: UPDATE activity_images SET display_order = ? WHERE id = ?
                DB-->>AIR:' "Updated"'
            end
            AS->>AIR: findAllByActivityIdOrderByDisplayOrder(id)
            AIR->>DB: SELECT * FROM activity_images WHERE activity_id = ? ORDER BY display_order
            DB-->>AIR:' "List<ActivityImage>"'
            AIR-->>AS:' "List<ActivityImage>"'
            AS->>AS: Map -> List<ActivityImageDTO>
            AS-->>AC: List<ActivityImageDTO>
            AC-->>Client: 200 OK<br/>Body: {images: [...], reordered: true}
            Client->>Client: Render gallery theo thứ tự mới
            Client-->>Admin:' Hiển thị thông báo: "Cập nhật thứ tự ảnh thành công"'
        end
    end
```

---

## Tóm tắt thành phần và chức năng

### Participants (Actors & Hệ thống)

| Participant | Vai trò | Mô tả |
|-------------|---------|-------|
| **Admin/Manager** | Actor | Người quản trị hệ thống, có quyền CRUD hoạt động, publish/unpublish, quản lý ảnh, sao chép, xem preset. |
| **Student** | Actor | Sinh viên, chỉ có quyền xem danh sách hoạt động đã công khai. |
| **Client** | React Frontend | Ứng dụng React giao tiếp với backend qua REST API. Xử lý form, validation, render UI. |
| **Controller** | Spring REST | Lớp Controller tiếp nhận HTTP request, validate input, gọi Service, trả về ResponseEntity. |
| **Service** | Business Logic | Chứa toàn bộ logic nghiệp vụ: CRUD, validation, soft delete, copy, preview, notification, reorder. |
| **Repository** | Data Access | Interface JPA Repository giao tiếp với Database (Spring Data JPA). Có thể dùng Specification/Paging. |
| **Database** | PostgreSQL/MySQL | Cơ sở dữ liệu quan hệ lưu trữ bảng: activities, semesters, registrations, activity_images, notifications, presets. |
| **FileStorage** | Local / S3 | Lưu trữ file vật lý (ảnh hoạt động). Hỗ trợ upload, delete, copy file. |

### Chức năng từng Sequence Diagram

| # | Tên | Endpoint chính | Đặc điểm kỹ thuật |
|---|-----|-----------------|-------------------|
| 1 | CRUD Activity | `POST/PUT/DELETE /api/admin/activities` | Tạo mặc định isPublished=false; Xóa kiểm tra registration để quyết định hard/soft delete. |
| 2 | Xem danh sách | `GET /api/activities` (public)<br>`GET /api/admin/activities` (admin) | Public filter theo semester OPEN; Admin filter đa chiều (semester, publish status, search). |
| 3 | Publish/Unpublish | `PUT /api/admin/activities/{id}/publish`<br>`PUT /api/admin/activities/{id}/unpublish` | Publish kèm gửi notification cho tất cả sinh viên; Unpublish đơn giản cập nhật flag. |
| 4 | Copy Activity | `POST /api/admin/activities/{id}/copy` | Deep copy: clone entity + copy ảnh vật lý + clone preset liên kết. Reset registrations=0, isPublished=false. |
| 5 | Preview Preset | `POST /api/activities/presets/preview` | Chỉ trả về preview data, không persist vào DB. Hỗ trợ Admin quyết định trước khi tạo. |
| 6 | Quản lý ảnh | `POST /api/admin/activities/{id}/images`<br>`DELETE /api/admin/activities/{id}/images/{imageId}`<br>`PUT /api/admin/activities/{id}/images/reorder` | Upload multipart với validate type/size/count; Xóa xóa cả file vật lý + DB record; Reorder cập nhật displayOrder theo mảng mới. |
