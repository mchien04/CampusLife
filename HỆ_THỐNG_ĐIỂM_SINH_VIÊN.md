# Hệ Thống Điểm Sinh Viên - Tài Liệu Chi Tiết

## 1. Cấu Trúc Dữ Liệu

### StudentScore Entity
```java
- id: Long
- student: Student (ManyToOne)
- semester: Semester (ManyToOne)
- scoreType: ScoreType (REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE)
- score: BigDecimal (Tổng điểm tích lũy)
- activityIds: String (JSON array: "[1,5,10]" - Danh sách activity đã nhận điểm)
- criterion: Criterion (Nullable - Dùng cho điểm rèn luyện theo tiêu chí)
- notes: String (Ghi chú)
- createdAt, updatedAt: LocalDateTime
```

**Quan trọng:** Mỗi sinh viên có **3 bản ghi điểm** cho mỗi học kỳ (REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE)

---

## 2. Cách Tính Điểm

### 2.1. Khởi Tạo Điểm
**Khi nào:** Sinh viên đăng ký tài khoản lần đầu

**File:** `AuthServiceImpl.java` → Gọi `StudentScoreInitService.initializeStudentScoresForCurrentSemester()`

**Logic:**
- Tự động tạo 3 bản ghi `StudentScore`:
  - `ScoreType.REN_LUYEN` → score = 0
  - `ScoreType.CONG_TAC_XA_HOI` → score = 0
  - `ScoreType.CHUYEN_DE` → score = 0
- Tất cả bắt đầu với `activityIds = "[]"` (chưa có activity nào)

---

### 2.2. Cộng Điểm Từ Hoạt Động (Activity Check-in)

**API:** `POST /api/registrations/checkin`

**Khi nào:** Sinh viên check-in thành công tại sự kiện

**File:** `ActivityRegistrationServiceImpl.java` → Method `createScoreFromCheckIn()`

**Logic:**
1. Lấy activity và `activity.scoreType` (REN_LUYEN, CONG_TAC_XA_HOI, hoặc CHUYEN_DE)
2. Tìm bản ghi `StudentScore` tổng hợp: `studentId + semesterId + scoreType`
3. Parse JSON `activityIds`, kiểm tra nếu đã có điểm từ activity này → Dừng
4. Thêm `activityId` vào list
5. **Cộng điểm:** `score = score + activity.maxPoints`
6. Lưu lại JSON mới vào `activityIds`
7. Tạo `ScoreHistory` để ghi lại lịch sử

**Ví dụ:**
```
Activity: {id: 1, name: "Hội trại", maxPoints: 10, scoreType: REN_LUYEN}
Student: id=5

Trước:
  score = 20
  activityIds = "[3,7]"

Sau check-in:
  score = 30
  activityIds = "[3,7,1]"
```

---

### 2.3. Cộng Điểm Từ Nộp Bài (Submission)

**API:** `PUT /api/submissions/{submissionId}/grade`

**Khi nào:** Admin/Manager chấm điểm bài nộp (grade > 0)

**File:** `TaskSubmissionServiceImpl.java` → Method `createScoreFromSubmission()`

**Logic:**
1. Lấy `task.getActivity().getScoreType()`
2. Tìm bản ghi `StudentScore` tổng hợp theo `scoreType`
3. Parse JSON `activityIds`, kiểm tra nếu đã có điểm từ activity này → Dừng
4. Thêm `activityId` vào list
5. **Cộng điểm:** `score = score + submission.score`
6. Lưu lại JSON mới
7. Tạo `ScoreHistory`

**Ví dụ:**
```
Task: id=5, activity.id=2, activity.scoreType=CONG_TAC_XA_HOI
Submission: score=15

Trước:
  score = 0
  activityIds = "[]"

Sau khi grade submission:
  score = 15
  activityIds = "[2]"
```

---

### 2.4. Điểm Rèn Luyện Theo Tiêu Chí

**API:** `POST /api/scores/training/calculate`

**Khi nào:** Admin/Manager tính điểm rèn luyện chi tiết theo criteria

**File:** `ScoreServiceImpl.java` → Method `calculateTrainingScore()`

**Logic:**
1. Lấy tất cả `Criterion` có `scoreGroupType = TRAINING`
2. Với mỗi criterion (trừ những cái trong `excludedCriterionIds`):
   - Tạo bản ghi `StudentScore` với `score = criterion.maxScore`
   - Lưu `criterion` reference để biết điểm này từ tiêu chí nào
3. Tính tổng điểm

**Lưu ý:** Điểm rèn luyện có thể có nhiều bản ghi (mỗi criterion 1 bản), khác với 2 loại điểm kia

---

## 3. Các API Liên Quan

### 3.1. Khởi Tạo Điểm
- **Tự động** khi sinh viên đăng ký tài khoản
- Không có API riêng

### 3.2. Check-in Sự Kiện (Cộng điểm)
```http
POST /api/registrations/checkin
Authorization: Bearer {token}
Content-Type: application/json

{
  "registrationId": 123,
  "checkInTime": "2025-01-15T08:00:00",
  "location": "Hội trường A"
}
```
**Kết quả:** Tự động cộng điểm vào `StudentScore` nếu check-in thành công

### 3.3. Chấm Điểm Bài Nộp (Cộng điểm)
```http
PUT /api/submissions/{submissionId}/grade
Authorization: Bearer {token}
Content-Type: application/x-www-form-urlencoded

score=15&feedback=Xuất sắc
```
**Kết quả:** Tự động cộng điểm vào `StudentScore` nếu score > 0

### 3.4. Tính Điểm Rèn Luyện
```http
POST /api/scores/training/calculate?studentId=5&semesterId=1
Authorization: Bearer {token}
Content-Type: application/json

[7, 9]  // excludedCriterionIds
```
**Response:**
```json
{
  "status": true,
  "message": "Training score calculated",
  "data": {
    "total": 100,
    "items": [
      {"criterionId": 1, "criterionName": "Chuyên cần", "score": 30},
      {"criterionId": 2, "criterionName": "Thái độ", "score": 40},
      ...
    ]
  }
}
```

### 3.5. Xem Điểm Chi Tiết
```http
GET /api/scores/student/{studentId}/semester/{semesterId}
Authorization: Bearer {token}
```
**Response:**
```json
{
  "status": true,
  "message": "Scores retrieved",
  "data": {
    "studentId": 5,
    "semesterId": 1,
    "summaries": [
      {
        "scoreType": "REN_LUYEN",
        "total": 50,
        "items": [
          {
            "score": 50,
            "activityIds": [1, 3, 5],
            "criterionId": null,
            "notes": null
          }
        ]
      },
      {
        "scoreType": "CONG_TAC_XA_HOI",
        "total": 25,
        "items": [
          {
            "score": 15,
            "activityIds": [2],
            "criterionId": null,
            "notes": null
          },
          {
            "score": 10,
            "activityIds": [4],
            "criterionId": null,
            "notes": null
          }
        ]
      },
      {
        "scoreType": "CHUYEN_DE",
        "total": 0,
        "items": []
      }
    ]
  }
}
```

### 3.6. Xem Tổng Điểm
```http
GET /api/scores/student/{studentId}/semester/{semesterId}/total
Authorization: Bearer {token}
```
**Response:**
```json
{
  "status": true,
  "message": "Total score calculated",
  "data": {
    "studentId": 5,
    "semesterId": 1,
    "grandTotal": 75,
    "totalsByType": {
      "REN_LUYEN": 50,
      "CONG_TAC_XA_HOI": 25,
      "CHUYEN_DE": 0
    },
    "scoreCount": 2
  }
}
```

---

## 4. Cơ Chế Bảo Vệ

### 4.1. Tránh Cộng Điểm Trùng Lặp
- Parse `activityIds` JSON trước khi cộng điểm
- Kiểm tra nếu `activityId` đã tồn tại trong list → **Dừng, không cộng lại**
- Mỗi activity chỉ được nhận điểm 1 lần

### 4.2. Khởi Tạo Tự Động
- Khi sinh viên đăng ký, tự động tạo 3 bản ghi điểm
- Không cần gọi API thủ công

### 4.3. Ghi Lịch Sử
- Mỗi lần cộng điểm tạo `ScoreHistory` record
- Lưu lại: `oldScore`, `newScore`, `changedBy`, `reason`, `activityId`

---

## 5. Lưu Ý Quan Trọng

1. **Mỗi sinh viên/semester chỉ có TỐI ĐA 3 bản ghi điểm** (trừ điểm rèn luyện chi tiết theo criterion)
2. **Điểm được cộng dồn** vào trường `score` thay vì tạo nhiều bản ghi
3. **`activityIds` JSON lưu danh sách activity đã nhận điểm** để truy vết
4. **Điểm rèn luyện có thể có nhiều bản ghi** (mỗi criterion 1 bản) khi dùng API calculate
5. **Khi chuyển học kỳ mới**, cần khởi tạo lại 3 bản ghi điểm cho semester mới

---

## 6. Workflow Điểm

```
Bước 1: Sinh viên đăng ký tài khoản
        ↓
        Tạo 3 bản ghi điểm (score = 0)
        
Bước 2: Sinh viên check-in sự kiện
        ↓
        Cộng RFscore = score + activity.maxPoints
        Thêm activityId vào activityIds list
        
Bước 3: Sinh viên nộp bài và được chấm điểm
        ↓
        Cộng score = score + submission.score
        Thêm activityId vào activityIds list
        
Bước 4: Sinh viên xem điểm
        ↓
        GET /api/scores/student/{id}/semester/{id}
        Trả về danh sách activity đã nhận điểm
```

