# CURL Commands để Xem Lịch Sử Điểm

## LƯU Ý QUAN TRỌNG

1. **Thay thế tokens:**
   - `{ADMIN_TOKEN}` hoặc `{MANAGER_TOKEN}` - Token của user có role ADMIN hoặc MANAGER
   - `{STUDENT_TOKEN}` - Token của user có role STUDENT

2. **Thay thế IDs:**
   - `{studentId}` - ID của student
   - `{semesterId}` - ID của semester (required)
   - `{scoreType}` - Loại điểm (optional): `REN_LUYEN`, `CONG_TAC_XA_HOI`, `CHUYEN_DE`, `KHAC`

3. **Base URL:** `http://localhost:8080` (thay đổi nếu cần)

4. **Lấy Token:** Đăng nhập trước để lấy JWT token
   ```bash
   curl --location 'http://localhost:8080/api/auth/login' \
   --header 'Content-Type: application/json' \
   --data '{
     "username": "admin",
     "password": "password"
   }'
   ```

---

## PHẦN 1: XEM LỊCH SỬ ĐIỂM

### 1.1. Xem lịch sử điểm của student (tất cả loại điểm)

**API:** `GET /api/scores/history/student/{studentId}`

**Yêu cầu:**
- Role: `STUDENT` (chỉ xem được của chính mình), `ADMIN`, hoặc `MANAGER`
- `semesterId` (required): ID của semester
- `scoreType` (optional): Loại điểm (null = tất cả loại)
- `page` (optional, default 0): Số trang
- `size` (optional, default 20): Số bản ghi mỗi trang

**CURL Command:**
```bash
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1&page=0&size=20' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Score history retrieved successfully",
  "data": {
    "studentId": 1,
    "studentCode": "SV001",
    "studentName": "Nguyễn Văn A",
    "semesterId": 1,
    "semesterName": "Học kỳ 1 - 2025",
    "scoreType": null,
    "currentScore": 0.0,
    "scoreHistories": [
      {
        "id": 10,
        "oldScore": 0.0,
        "newScore": 10.0,
        "changeDate": "2025-02-05T10:00:00",
        "reason": "Score from minigame quiz: Quiz kiến thức IT",
        "activityId": 5,
        "activityName": "Quiz kiến thức IT",
        "seriesId": null,
        "seriesName": null,
        "sourceType": "MINIGAME",
        "changedByUsername": "admin",
        "changedByFullName": null
      },
      {
        "id": 11,
        "oldScore": 10.0,
        "newScore": 15.0,
        "changeDate": "2025-02-06T14:30:00",
        "reason": "Milestone points from series: 1",
        "activityId": null,
        "activityName": null,
        "seriesId": 1,
        "seriesName": "Chuỗi sự kiện mùa hè 2025",
        "sourceType": "MILESTONE",
        "changedByUsername": "admin",
        "changedByFullName": null
      },
      {
        "id": 12,
        "oldScore": 15.0,
        "newScore": 20.0,
        "changeDate": "2025-02-07T09:15:00",
        "reason": "Recalculated score: Participation (15.0) + Milestone (5.0)",
        "activityId": null,
        "activityName": null,
        "seriesId": null,
        "seriesName": null,
        "sourceType": "RECALCULATED",
        "changedByUsername": "admin",
        "changedByFullName": null
      },
      {
        "id": 13,
        "oldScore": 0.0,
        "newScore": 5.0,
        "changeDate": "2025-02-08T11:20:00",
        "reason": "Recalculated from activity participation: Sự kiện đơn lẻ",
        "activityId": 3,
        "activityName": "Sự kiện đơn lẻ",
        "seriesId": null,
        "seriesName": null,
        "sourceType": "ACTIVITY",
        "changedByUsername": "admin",
        "changedByFullName": null
      }
    ],
    "activityParticipations": [
      {
        "id": 100,
        "activityId": 5,
        "activityName": "Quiz kiến thức IT",
        "activityType": "MINIGAME",
        "seriesId": null,
        "seriesName": null,
        "pointsEarned": 10.0,
        "participationType": "COMPLETED",
        "date": "2025-02-05T10:00:00",
        "isCompleted": true,
        "sourceType": "MINIGAME"
      },
      {
        "id": 101,
        "activityId": 3,
        "activityName": "Sự kiện đơn lẻ",
        "activityType": "SUKIEN",
        "seriesId": null,
        "seriesName": null,
        "pointsEarned": 5.0,
        "participationType": "COMPLETED",
        "date": "2025-02-08T11:20:00",
        "isCompleted": true,
        "sourceType": "ACTIVITY"
      },
      {
        "id": 102,
        "activityId": 2,
        "activityName": "Sự kiện 1 trong chuỗi",
        "activityType": null,
        "seriesId": 1,
        "seriesName": "Chuỗi sự kiện mùa hè 2025",
        "pointsEarned": 0.0,
        "participationType": "COMPLETED",
        "date": "2025-02-06T14:30:00",
        "isCompleted": true,
        "sourceType": "ACTIVITY"
      }
    ],
    "totalRecords": 7,
    "page": 0,
    "size": 20,
    "totalPages": 1
  }
}
```

---

### 1.2. Xem lịch sử điểm theo loại điểm cụ thể

**API:** `GET /api/scores/history/student/{studentId}?semesterId={semesterId}&scoreType={scoreType}`

**CURL Command:**
```bash
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1&scoreType=REN_LUYEN&page=0&size=20' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Score history retrieved successfully",
  "data": {
    "studentId": 1,
    "studentCode": "SV001",
    "studentName": "Nguyễn Văn A",
    "semesterId": 1,
    "semesterName": "Học kỳ 1 - 2025",
    "scoreType": "REN_LUYEN",
    "currentScore": 20.0,
    "scoreHistories": [
      {
        "id": 10,
        "oldScore": 0.0,
        "newScore": 10.0,
        "changeDate": "2025-02-05T10:00:00",
        "reason": "Score from minigame quiz: Quiz kiến thức IT",
        "activityId": 5,
        "activityName": "Quiz kiến thức IT",
        "seriesId": null,
        "seriesName": null,
        "sourceType": "MINIGAME",
        "changedByUsername": "admin",
        "changedByFullName": null
      },
      {
        "id": 11,
        "oldScore": 10.0,
        "newScore": 15.0,
        "changeDate": "2025-02-06T14:30:00",
        "reason": "Milestone points from series: 1",
        "activityId": null,
        "activityName": null,
        "seriesId": 1,
        "seriesName": "Chuỗi sự kiện mùa hè 2025",
        "sourceType": "MILESTONE",
        "changedByUsername": "admin",
        "changedByFullName": null
      }
    ],
    "activityParticipations": [
      {
        "id": 100,
        "activityId": 5,
        "activityName": "Quiz kiến thức IT",
        "activityType": "MINIGAME",
        "seriesId": null,
        "seriesName": null,
        "pointsEarned": 10.0,
        "participationType": "COMPLETED",
        "date": "2025-02-05T10:00:00",
        "isCompleted": true,
        "sourceType": "MINIGAME"
      },
      {
        "id": 101,
        "activityId": 3,
        "activityName": "Sự kiện đơn lẻ",
        "activityType": "SUKIEN",
        "seriesId": null,
        "seriesName": null,
        "pointsEarned": 5.0,
        "participationType": "COMPLETED",
        "date": "2025-02-08T11:20:00",
        "isCompleted": true,
        "sourceType": "ACTIVITY"
      }
    ],
    "totalRecords": 4,
    "page": 0,
    "size": 20,
    "totalPages": 1
  }
}
```

---

### 1.3. Student xem lịch sử điểm của chính mình

**CURL Command:**
```bash
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1&scoreType=REN_LUYEN' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Lưu ý:**
- Student chỉ có thể xem lịch sử của chính mình
- Nếu student cố gắng xem lịch sử của student khác → trả về lỗi: `"You can only view your own score history"`

---

### 1.4. Pagination

**CURL Command với pagination:**
```bash
# Trang đầu tiên (20 bản ghi)
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1&page=0&size=20' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'

# Trang thứ 2
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1&page=1&size=20' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'

# Trang thứ 3 (10 bản ghi mỗi trang)
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1&page=2&size=10' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

---

## PHẦN 2: GIẢI THÍCH CÁC TRƯỜNG TRONG RESPONSE

### 2.1. ScoreHistoryDetailResponse

**Các trường:**
- `id`: ID của bản ghi ScoreHistory
- `oldScore`: Điểm cũ trước khi thay đổi
- `newScore`: Điểm mới sau khi thay đổi
- `changeDate`: Ngày giờ thay đổi
- `reason`: Lý do thay đổi (từ hệ thống)
- `activityId`: ID của activity gây ra thay đổi (optional)
- `activityName`: Tên activity (optional, lấy từ Activity nếu có activityId)
- `seriesId`: ID của series (optional, parse từ reason nếu là milestone)
- `seriesName`: Tên series (optional, lấy từ ActivitySeries nếu có seriesId)
- `sourceType`: Loại nguồn điểm
  - `"MINIGAME"`: Điểm từ minigame quiz
  - `"MILESTONE"`: Điểm từ milestone của series
  - `"RECALCULATED"`: Điểm được tính lại (recalculate)
  - `"ACTIVITY"`: Điểm từ activity thường
- `changedByUsername`: Username của người thay đổi (thường là admin/system)
- `changedByFullName`: Full name (null vì User không có fullName)

### 2.2. ActivityParticipationDetailResponse

**Các trường:**
- `id`: ID của ActivityParticipation
- `activityId`: ID của activity
- `activityName`: Tên activity
- `activityType`: Loại activity (SUKIEN, MINIGAME, CONG_TAC_XA_HOI, CHUYEN_DE_DOANH_NGHIEP)
- `seriesId`: ID của series (optional, nếu activity thuộc series)
- `seriesName`: Tên series (optional)
- `pointsEarned`: Điểm đã nhận
- `participationType`: Loại participation (COMPLETED)
- `date`: Ngày tham gia
- `isCompleted`: Đã hoàn thành hay chưa
- `sourceType`: Loại nguồn điểm
  - `"MINIGAME"`: Nếu activity.type = MINIGAME
  - `"ACTIVITY"`: Các trường hợp khác

### 2.3. ScoreHistoryViewResponse

**Các trường:**
- `studentId`: ID của student
- `studentCode`: Mã sinh viên
- `studentName`: Tên sinh viên
- `semesterId`: ID của semester
- `semesterName`: Tên semester
- `scoreType`: Loại điểm (null nếu xem tất cả loại)
- `currentScore`: Điểm hiện tại của student (cho scoreType đó)
- `scoreHistories`: Danh sách ScoreHistory (tổng hợp)
- `activityParticipations`: Danh sách ActivityParticipation (chi tiết)
- `totalRecords`: Tổng số bản ghi (scoreHistories + activityParticipations)
- `page`: Số trang hiện tại
- `size`: Số bản ghi mỗi trang
- `totalPages`: Tổng số trang

---

## PHẦN 3: LOGIC PHÂN LOẠI SOURCE TYPE

### 3.1. Source Type trong ScoreHistory

**MINIGAME:**
- `reason` chứa `"minigame quiz"` (case-insensitive)
- Ví dụ: `"Score from minigame quiz: Quiz kiến thức IT"`

**MILESTONE:**
- `reason` chứa `"Milestone points from series"` (case-insensitive)
- `seriesId` được parse từ reason: `"Milestone points from series: {seriesId}"`
- Ví dụ: `"Milestone points from series: 1"` → `seriesId = 1`

**RECALCULATED:**
- `reason` chứa `"Recalculated score"` (case-insensitive)
- Ví dụ: `"Recalculated score: Participation (15.0) + Milestone (5.0)"`

**ACTIVITY:**
- Các trường hợp còn lại
- Ví dụ: `"Recalculated from activity participation: Sự kiện đơn lẻ"`

### 3.2. Source Type trong ActivityParticipation

**MINIGAME:**
- `activity.type = MINIGAME`
- Ví dụ: Activity có type = MINIGAME → `sourceType = "MINIGAME"`

**ACTIVITY:**
- Các trường hợp khác (SUKIEN, CONG_TAC_XA_HOI, CHUYEN_DE_DOANH_NGHIEP, hoặc null)
- Ví dụ: Activity có type = SUKIEN → `sourceType = "ACTIVITY"`

---

## PHẦN 4: VÍ DỤ SỬ DỤNG

### 4.1. Student xem lịch sử điểm của mình

```bash
# 1. Student đăng nhập
curl --location 'http://localhost:8080/api/auth/login' \
--header 'Content-Type: application/json' \
--data '{
  "username": "student001",
  "password": "password"
}'
# → Lưu lại token từ response

# 2. Xem lịch sử điểm (tất cả loại)
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'

# 3. Xem lịch sử điểm REN_LUYEN
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1&scoreType=REN_LUYEN' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

### 4.2. Admin xem lịch sử điểm của student

```bash
# Xem lịch sử điểm của student ID = 1
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1&scoreType=REN_LUYEN&page=0&size=20' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 4.3. Xem lịch sử với pagination

```bash
# Trang 1: 10 bản ghi đầu tiên
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1&page=0&size=10' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'

# Trang 2: 10 bản ghi tiếp theo
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1&page=1&size=10' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

---

## PHẦN 5: ERROR HANDLING

### 5.1. Student không tồn tại

**Request:**
```bash
curl --location 'http://localhost:8080/api/scores/history/student/999?semesterId=1' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": false,
  "message": "Student not found",
  "data": null
}
```

### 5.2. Semester không tồn tại

**Request:**
```bash
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=999' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": false,
  "message": "Semester not found",
  "data": null
}
```

### 5.3. Student cố gắng xem lịch sử của student khác

**Request:**
```bash
# Student ID = 1 cố gắng xem lịch sử của student ID = 2
curl --location 'http://localhost:8080/api/scores/history/student/2?semesterId=1' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response:**
```json
{
  "status": false,
  "message": "You can only view your own score history",
  "data": null
}
```

### 5.4. Invalid scoreType

**Request:**
```bash
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1&scoreType=INVALID' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": false,
  "message": "Invalid scoreType: INVALID",
  "data": null
}
```

### 5.5. Unauthorized (thiếu token hoặc token không hợp lệ)

**Request:**
```bash
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1'
```

**Response:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

---

## PHẦN 6: SO SÁNH SCOREHISTORY VÀ ACTIVITYPARTICIPATION

### 6.1. ScoreHistory (Tổng hợp)

**Đặc điểm:**
- Ghi lại mỗi lần điểm thay đổi (oldScore → newScore)
- Có thể là tổng hợp từ nhiều nguồn (participation + milestone)
- Có `reason` mô tả lý do thay đổi
- Có thể có `activityId` hoặc `seriesId` (tùy nguồn)

**Khi nào được tạo:**
- Khi student làm quiz PASSED → tạo ScoreHistory với reason "Score from minigame quiz"
- Khi student đạt milestone → tạo ScoreHistory với reason "Milestone points from series"
- Khi recalculate điểm → tạo ScoreHistory với reason "Recalculated score"
- Khi check-in/check-out activity → tạo ScoreHistory với reason "Recalculated from activity participation"

### 6.2. ActivityParticipation (Chi tiết)

**Đặc điểm:**
- Ghi lại từng lần tham gia activity cụ thể
- Có `pointsEarned` từ activity đó
- Có `activityId`, `activityName`, `activityType`
- Có thể có `seriesId` nếu activity thuộc series

**Khi nào được tạo:**
- Khi student làm quiz PASSED → tạo ActivityParticipation với pointsEarned = rewardPoints
- Khi student check-in/check-out activity → tạo ActivityParticipation với pointsEarned = maxPoints (hoặc 0 nếu trong series)
- Khi admin chấm điểm completion → tạo ActivityParticipation với pointsEarned = điểm được chấm

### 6.3. Mối quan hệ

- **ActivityParticipation** là nguồn gốc của điểm (chi tiết từng activity)
- **ScoreHistory** là lịch sử thay đổi điểm (tổng hợp, có thể từ nhiều participation)
- Một ScoreHistory có thể liên quan đến nhiều ActivityParticipation
- Một ActivityParticipation có thể gây ra một hoặc nhiều ScoreHistory

---

## PHẦN 7: USE CASES

### 7.1. Student muốn xem điểm của mình đến từ đâu

**Scenario:** Student muốn biết điểm REN_LUYEN của mình đến từ những activity nào

**Solution:**
```bash
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1&scoreType=REN_LUYEN' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response sẽ cho biết:**
- `scoreHistories`: Các lần điểm thay đổi (tổng hợp)
- `activityParticipations`: Các activity cụ thể đã tham gia (chi tiết)
- Mỗi bản ghi có `sourceType`, `activityName`, `seriesName` để biết nguồn gốc

### 7.2. Admin muốn kiểm tra điểm của student có đúng không

**Scenario:** Admin muốn kiểm tra xem điểm của student có được tính đúng từ các activity và milestone không

**Solution:**
```bash
# Xem lịch sử điểm
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1&scoreType=REN_LUYEN' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'

# So sánh với:
# - Danh sách ActivityParticipation COMPLETED
# - Danh sách StudentSeriesProgress (milestone points)
# - Tổng điểm = Participation Score + Milestone Score
```

### 7.3. Xem điểm từ minigame quiz

**Scenario:** Student muốn xem các quiz đã làm và điểm đã nhận

**Solution:**
```bash
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Filter trong response:**
- `scoreHistories` với `sourceType = "MINIGAME"`
- `activityParticipations` với `sourceType = "MINIGAME"` và `activityType = "MINIGAME"`

### 7.4. Xem điểm từ milestone của series

**Scenario:** Student muốn xem điểm milestone đã nhận từ các series

**Solution:**
```bash
curl --location 'http://localhost:8080/api/scores/history/student/1?semesterId=1' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Filter trong response:**
- `scoreHistories` với `sourceType = "MILESTONE"`
- Mỗi bản ghi có `seriesId` và `seriesName` để biết series nào

---

## PHẦN 8: BEST PRACTICES

1. **Luôn chỉ định semesterId:**
   - Để đảm bảo lấy đúng lịch sử điểm của semester cụ thể

2. **Sử dụng scoreType khi cần:**
   - Nếu chỉ quan tâm một loại điểm (REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE)
   - Giảm số lượng dữ liệu trả về

3. **Sử dụng pagination:**
   - Nếu có nhiều bản ghi, nên dùng pagination để tải từng phần
   - Default: page=0, size=20

4. **Kiểm tra access control:**
   - Student chỉ có thể xem lịch sử của chính mình
   - Admin/Manager có thể xem lịch sử của bất kỳ student nào

5. **Kết hợp với API khác:**
   - Có thể kết hợp với `GET /api/scores/student/{studentId}/semester/{semesterId}` để xem điểm hiện tại
   - Có thể kết hợp với `GET /api/series/{seriesId}/progress/my` để xem progress trong series

---

## PHẦN 9: TÓM TẮT

### API Endpoint:
```
GET /api/scores/history/student/{studentId}?semesterId={semesterId}&scoreType={scoreType}&page={page}&size={size}
```

### Query Parameters:
- `semesterId` (required): ID của semester
- `scoreType` (optional): Loại điểm (REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE, KHAC)
- `page` (optional, default 0): Số trang
- `size` (optional, default 20): Số bản ghi mỗi trang

### Access Control:
- **STUDENT**: Chỉ xem được lịch sử của chính mình
- **ADMIN/MANAGER**: Xem được lịch sử của bất kỳ student nào

### Response Structure:
- `scoreHistories`: Danh sách ScoreHistory (tổng hợp, ghi lại mỗi lần điểm thay đổi)
- `activityParticipations`: Danh sách ActivityParticipation (chi tiết, từng activity đã tham gia)
- Mỗi bản ghi có `sourceType`, `activityName`, `seriesName` để biết nguồn gốc điểm

### Source Types:
- `MINIGAME`: Điểm từ minigame quiz
- `MILESTONE`: Điểm từ milestone của series
- `RECALCULATED`: Điểm được tính lại
- `ACTIVITY`: Điểm từ activity thường

---

## PHẦN 10: VÍ DỤ JSON RESPONSE ĐẦY ĐỦ

### 10.1. Response với tất cả loại điểm

```json
{
  "status": true,
  "message": "Score history retrieved successfully",
  "data": {
    "studentId": 1,
    "studentCode": "SV001",
    "studentName": "Nguyễn Văn A",
    "semesterId": 1,
    "semesterName": "Học kỳ 1 - 2025",
    "scoreType": null,
    "currentScore": 0.0,
    "scoreHistories": [
      {
        "id": 10,
        "oldScore": 0.0,
        "newScore": 10.0,
        "changeDate": "2025-02-05T10:00:00",
        "reason": "Score from minigame quiz: Quiz kiến thức IT",
        "activityId": 5,
        "activityName": "Quiz kiến thức IT",
        "seriesId": null,
        "seriesName": null,
        "sourceType": "MINIGAME",
        "changedByUsername": "admin",
        "changedByFullName": null
      },
      {
        "id": 11,
        "oldScore": 10.0,
        "newScore": 15.0,
        "changeDate": "2025-02-06T14:30:00",
        "reason": "Milestone points from series: 1",
        "activityId": null,
        "activityName": null,
        "seriesId": 1,
        "seriesName": "Chuỗi sự kiện mùa hè 2025",
        "sourceType": "MILESTONE",
        "changedByUsername": "admin",
        "changedByFullName": null
      }
    ],
    "activityParticipations": [
      {
        "id": 100,
        "activityId": 5,
        "activityName": "Quiz kiến thức IT",
        "activityType": "MINIGAME",
        "seriesId": null,
        "seriesName": null,
        "pointsEarned": 10.0,
        "participationType": "COMPLETED",
        "date": "2025-02-05T10:00:00",
        "isCompleted": true,
        "sourceType": "MINIGAME"
      },
      {
        "id": 101,
        "activityId": 3,
        "activityName": "Sự kiện đơn lẻ",
        "activityType": "SUKIEN",
        "seriesId": null,
        "seriesName": null,
        "pointsEarned": 5.0,
        "participationType": "COMPLETED",
        "date": "2025-02-08T11:20:00",
        "isCompleted": true,
        "sourceType": "ACTIVITY"
      },
      {
        "id": 102,
        "activityId": 2,
        "activityName": "Sự kiện 1 trong chuỗi",
        "activityType": null,
        "seriesId": 1,
        "seriesName": "Chuỗi sự kiện mùa hè 2025",
        "pointsEarned": 0.0,
        "participationType": "COMPLETED",
        "date": "2025-02-06T14:30:00",
        "isCompleted": true,
        "sourceType": "ACTIVITY"
      }
    ],
    "totalRecords": 5,
    "page": 0,
    "size": 20,
    "totalPages": 1
  }
}
```

### 10.2. Response với scoreType = REN_LUYEN

```json
{
  "status": true,
  "message": "Score history retrieved successfully",
  "data": {
    "studentId": 1,
    "studentCode": "SV001",
    "studentName": "Nguyễn Văn A",
    "semesterId": 1,
    "semesterName": "Học kỳ 1 - 2025",
    "scoreType": "REN_LUYEN",
    "currentScore": 20.0,
    "scoreHistories": [
      {
        "id": 10,
        "oldScore": 0.0,
        "newScore": 10.0,
        "changeDate": "2025-02-05T10:00:00",
        "reason": "Score from minigame quiz: Quiz kiến thức IT",
        "activityId": 5,
        "activityName": "Quiz kiến thức IT",
        "seriesId": null,
        "seriesName": null,
        "sourceType": "MINIGAME",
        "changedByUsername": "admin",
        "changedByFullName": null
      },
      {
        "id": 11,
        "oldScore": 10.0,
        "newScore": 15.0,
        "changeDate": "2025-02-06T14:30:00",
        "reason": "Milestone points from series: 1",
        "activityId": null,
        "activityName": null,
        "seriesId": 1,
        "seriesName": "Chuỗi sự kiện mùa hè 2025",
        "sourceType": "MILESTONE",
        "changedByUsername": "admin",
        "changedByFullName": null
      }
    ],
    "activityParticipations": [
      {
        "id": 100,
        "activityId": 5,
        "activityName": "Quiz kiến thức IT",
        "activityType": "MINIGAME",
        "seriesId": null,
        "seriesName": null,
        "pointsEarned": 10.0,
        "participationType": "COMPLETED",
        "date": "2025-02-05T10:00:00",
        "isCompleted": true,
        "sourceType": "MINIGAME"
      },
      {
        "id": 101,
        "activityId": 3,
        "activityName": "Sự kiện đơn lẻ",
        "activityType": "SUKIEN",
        "seriesId": null,
        "seriesName": null,
        "pointsEarned": 5.0,
        "participationType": "COMPLETED",
        "date": "2025-02-08T11:20:00",
        "isCompleted": true,
        "sourceType": "ACTIVITY"
      }
    ],
    "totalRecords": 4,
    "page": 0,
    "size": 20,
    "totalPages": 1
  }
}
```

---

## PHẦN 11: LIÊN HỆ

Nếu có thắc mắc hoặc cần hỗ trợ, vui lòng liên hệ team Backend.

