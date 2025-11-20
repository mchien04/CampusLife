# Thay Đổi API và Logic Hệ Thống Điểm - Hướng Dẫn Frontend

## Tổng Quan

Hệ thống điểm và check-in đã được refactor hoàn toàn với các thay đổi chính:
- **Check-in 2 lần**: Check-in → Check-out → ATTENDED (thay vì 1 lần như trước)
- **Tự động đăng ký**: Sự kiện quan trọng/bắt buộc tự động đăng ký tất cả sinh viên
- **Điểm trừ**: Thêm khả năng trừ điểm khi không hoàn thành
- **Chấm nhanh**: Admin tick đạt/không đạt thay vì nhập điểm chi tiết

---

## A. Activity Enhancements

### New/Updated Activity Fields
- startDate, endDate: now LocalDateTime (includes hours)
- registrationStartDate, registrationDeadline: now LocalDateTime
- isDraft: boolean (default true)
- requiresApproval: boolean (default true)

### New Endpoints
- PUT /api/activities/{id}/publish → publish activity (set isDraft=false)
- PUT /api/activities/{id}/unpublish → unpublish activity (set isDraft=true)
- POST /api/activities/{id}/copy?offsetDays=7 → duplicate an activity; shifts all date-times by offsetDays (0 if omitted). Returns the new draft activity.

### Registration Flow Update
- If activity.requiresApproval=false, student registration is auto-approved immediately (still enforces ticketQuantity and registration windows).
- Registration windows now use LocalDateTime.

### Security
- New endpoints require ADMIN or MANAGER. GET activity endpoints remain public as before.

---

## 1. Thay Đổi Check-in Flow

### Trước đây:
- Check-in 1 lần → status ngay thành `ATTENDED`

### Bây giờ:
- **Lần 1**: Check-in → status `CHECKED_IN`
- **Lần 2**: Check-out → status `CHECKED_OUT` → `ATTENDED`

### API: `POST /api/registrations/checkin`

**Request body:**
```json
{
  "ticketCode": "ABC123",
  "notes": "Ghi chú (optional)"
}
```

**Response lần 1 (Check-in):**
```json
{
  "status": true,
  "message": "Check-in thành công. Vui lòng check-out khi rời khỏi sự kiện.",
  "data": {
    "id": 1,
    "activityId": 5,
    "activityName": "Hội trại",
    "studentId": 10,
    "studentName": "Nguyễn Văn A",
    "studentCode": "SV001",
    "participationType": "CHECKED_IN",
    "pointsEarned": 0,
    "date": "2025-01-15T08:00:00",
    "notes": ""
  }
}
```

**Response lần 2 (Check-out):**
```json
{
  "status": true,
  "message": "Check-out thành công. Đã hoàn thành tham gia sự kiện.",
  "data": {
    "participationType": "ATTENDED",
    ...
  }
}
```

**📁 Files Backend liên quan:**
- `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java` - Method `checkIn()`
- `src/main/java/vn/campuslife/enumeration/ParticipationType.java` - Enum values mới
- `src/main/java/vn/campuslife/entity/ActivityParticipation.java` - Fields `checkInTime`, `checkOutTime`


**Thay đổi UI cần làm:**

1. Hiển thị button "Check-in" khi status là `REGISTERED` hoặc `PENDING`
2. Sau khi check-in lần 1 → disable button "Check-in", hiện button "Check-out"
3. Sau khi check-out lần 2 → hiển thị thông báo "Đã hoàn thành tham gia"
4. Kiểm tra `participationType` để biết trạng thái:
   - `REGISTERED` → Chưa check-in
   - `CHECKED_IN` → Đã check-in, cần check-out
   - `ATTENDED` → Đã hoàn thành cả 2 lần

---

## 2. API Mới: Chấm Điểm Completion (Đạt/Không Đạt)

### Endpoint: `PUT /api/registrations/participations/{participationId}/grade`

**Chức năng:** Admin/Manager chấm điểm nhanh bằng cách tick "Đạt" hoặc "Không đạt"

**Request:**
```javascript
PUT /api/registrations/participations/123/grade
Content-Type: application/x-www-form-urlencoded

isCompleted=true&notes=Hoàn thành xuất sắc
```

**Parameters:**
- `isCompleted` (required): `true` = đạt, `false` = không đạt
- `notes` (optional): Ghi chú

**Response:**
```json
{
  "status": true,
  "message": "Đã chấm điểm completion",
  "data": {
    "id": 123,
    "isCompleted": true,
    "pointsEarned": 10,
    "participationType": "COMPLETED",
    ...
  }
}
```

**Logic điểm:**
- Nếu `isCompleted=true` → `pointsEarned = activity.maxPoints`
- Nếu `isCompleted=false` → `pointsEarned = -activity.penaltyPointsIncomplete`

**📁 Files Backend liên quan:**
- `src/main/java/vn/campuslife/controller/ActivityRegistrationController.java` - Endpoint mới
- `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java` - Method `gradeCompletion()`
- `src/main/java/vn/campuslife/config/SecurityConfig.java` - Security config
- `src/main/java/vn/campuslife/entity/Activity.java` - Field `penaltyPointsIncomplete`
- `src/main/java/vn/campuslife/entity/ActivityParticipation.java` - Field `isCompleted`


**Thay đổi UI cần làm:**

Thay form nhập điểm bằng radio button:

## 3. Tự Động Đăng Ký (Auto-register)

### Khi tạo activity mới:

**Fields mới trong CreateActivityRequest:**
```json
{
  "name": "Hội trại",
  ...
  "isImportant": true,                      // Tự động đăng ký TẤT CẢ sinh viên
  "mandatoryForFacultyStudents": false,     // Tự động đăng ký sinh viên thuộc khoa tổ chức
  "penaltyPointsIncomplete": 5              // Điểm trừ khi không hoàn thành
}
```

**Logic:**
- Nếu `isImportant=true` → Tất cả sinh viên được đăng ký tự động với status `APPROVED`
- Nếu `mandatoryForFacultyStudents=true` → Sinh viên thuộc khoa tổ chức được đăng ký tự động
- Registration status luôn là `APPROVED` (không cần duyệt thủ công)

**📁 Files Backend liên quan:**
- `src/main/java/vn/campuslife/service/impl/ActivityServiceImpl.java` - Method `createActivity()`, `autoRegisterStudents()`
- `src/main/java/vn/campuslife/entity/Activity.java` - Fields `isImportant`, `mandatoryForFacultyStudents`, `penaltyPointsIncomplete`


---

## 4. Flow Hoàn Chỉnh Mới

### Sự kiện KHÔNG yêu cầu submission:

```
1. Sinh viên đăng ký (hoặc auto-register) → status: REGISTERED
2. Check-in lần 1 → status: CHECKED_IN
3. Check-out lần 2 → status: ATTENDED
4. Admin chấm điểm:
   - Tick "Đạt" → pointsEarned = maxPoints
   - Tick "Không đạt" → pointsEarned = -penaltyPointsIncomplete
5. Status: COMPLETED
6. StudentScore tổng hợp tự động cập nhật
```

### Sự kiện YÊU CẦU submission:

```
1-3. Giống trên (phải hoàn thành check-in/check-out trước)
4. Sinh viên nộp bài submission
5. Admin chấm submission → pointsEarned = submission.score
   - NHƯNG phải kiểm tra registration.status == ATTENDED trước
6. Status: COMPLETED
7. StudentScore tổng hợp tự động cập nhật
```

---

## 5. Thay Đổi Response Data

### ActivityParticipation Response:

**📁 Files Backend liên quan:**
- `src/main/java/vn/campuslife/entity/ActivityParticipation.java` - All new fields
- `src/main/java/vn/campuslife/enumeration/ParticipationType.java` - Updated enum
- `src/main/java/vn/campuslife/model/ActivityParticipationResponse.java` - Response DTO

**Fields mới:**
```json
{
  "id": 123,
  "registrationId": 456,
  "participationType": "ATTENDED",          // ENUM: REGISTERED, CHECKED_IN, CHECKED_OUT, ATTENDED, COMPLETED
  "pointsEarned": 10,                       // Có thể là số dương hoặc âm
  "date": "2025-01-15T08:00:00",
  "isCompleted": true,                      // null=chưa chấm, true=đạt, false=không đạt
  "checkInTime": "2025-01-15T08:00:00",    // Thời gian check-in
  "checkOutTime": "2025-01-15T12:00:00"    // Thời gian check-out
}
```

### ParticipationType Values:
- `REGISTERED`: Đã đăng ký, chưa check-in
- `CHECKED_IN`: Đã check-in lần 1
- `CHECKED_OUT`: Đã check-out lần 2 (tạm thời)
- `ATTENDED`: Đã hoàn thành cả 2 lần check (chưa chấm điểm)
- `COMPLETED`: Đã được chấm điểm (đạt hoặc không đạt)

---

## 6. Security Changes

**Endpoint mới cần authentication:**
```
PUT /api/registrations/participations/{id}/grade
→ Requires: ADMIN hoặc MANAGER role
```

---

## 7. Checklist Frontend Cần Làm

### Màn hình Check-in:
- [ ] Kiểm tra `participationType` để hiển thị đúng button
- [ ] Check-in lần 1 → disable button "Check-in", hiện button "Check-out"
- [ ] Check-out lần 2 → hiện thông báo "Đã hoàn thành"
- [ ] Hiển thị thời gian check-in và check-out nếu có

### Màn hình Chấm Điểm:
- [ ] Thay form nhập điểm bằng radio button "Đạt/Không đạt"
- [ ] Hiển thị preview điểm: `+maxPoints` hoặc `-penaltyPointsIncomplete`
- [ ] Kiểm tra `participationType == ATTENDED` trước khi cho phép chấm

### Form Tạo Activity:
- [ ] Thêm checkbox "Sự kiện quan trọng" (`isImportant`)
- [ ] Thêm checkbox "Bắt buộc cho sinh viên khoa" (`mandatoryForFacultyStudents`)
- [ ] Thêm input "Điểm trừ khi không hoàn thành" (`penaltyPointsIncomplete`)

### Màn hình Danh Sách Đăng Ký:
- [ ] Hiển thị status mới: `CHECKED_IN`, `CHECKED_OUT`, `ATTENDED`, `COMPLETED`
- [ ] Filter theo participationType
- [ ] Hiển thị `isCompleted` và `pointsEarned` khi đã chấm

### Màn hình Submission:
- [ ] Kiểm tra `registration.status == ATTENDED` trước khi cho phép nộp bài
- [ ] Nếu yêu cầu submission → chỉ chấm submission, không dùng API gradeCompletion

---

## 8. Example API Calls

### Check-in Flow:
```javascript
// Lần 1: Check-in
POST /api/registrations/checkin
{ "ticketCode": "ABC123" }
→ Response: participationType = "CHECKED_IN"

// Lần 2: Check-out (gọi lại cùng API)
POST /api/registrations/checkin
{ "ticketCode": "ABC123" }
→ Response: participationType = "ATTENDED"
```

### Chấm Điểm Completion:
```javascript
// Chấm đạt
PUT /api/registrations/participations/123/grade?isCompleted=true&notes=Hoàn thành tốt

// Chấm không đạt
PUT /api/registrations/participations/123/grade?isCompleted=false&notes=Chưa đạt yêu cầu
```

---

## 9. Breaking Changes

### ❌ KHÔNG CÒN HOẠT ĐỘNG:
- Check-in 1 lần để nhận điểm ngay
- API chấm điểm chi tiết (nhập số điểm bất kỳ)

### ✅ MỚI:
- Check-in 2 lần mới hoàn thành
- Chấm điểm bằng tick "Đạt/Không đạt"
- Tự động đăng ký cho sự kiện quan trọng
- Điểm có thể là số âm (điểm trừ)

---

## 10. Lưu Ý Quan Trọng

1. **Check-in bắt buộc 2 lần**: Không thể nhận điểm nếu chỉ check-in 1 lần
2. **Submission chỉ sau khi ATTENDED**: Nếu yêu cầu submission, phải hoàn thành check-in/check-out trước
3. **Auto-register**: Sinh viên không cần tự đăng ký sự kiện quan trọng/bắt buộc
4. **Điểm trừ**: Có thể set điểm âm nếu không hoàn thành
5. **ParticipationType**: Luôn check status này thay vì chỉ dựa vào registration.status

---

