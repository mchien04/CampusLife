# CURL Commands để Rà Soát và Tính Lại Điểm

## LƯU Ý QUAN TRỌNG

1. **Thay thế tokens:**
   - `{ADMIN_TOKEN}` hoặc `{MANAGER_TOKEN}` - Token của user có role ADMIN hoặc MANAGER
   - `{STUDENT_TOKEN}` - Token của user có role STUDENT

2. **Thay thế IDs:**
   - `{studentId}` - ID của student
   - `{semesterId}` - ID của semester (optional, null = học kỳ hiện tại)

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

## PHẦN 1: RÀ SOÁT ĐIỂM CHO MỘT STUDENT

### 1.1. Rà soát và tính lại điểm cho một student

**API:** `POST /api/scores/recalculate/student/{studentId}`

**Yêu cầu:**
- Role: `ADMIN` hoặc `MANAGER`
- `studentId`: ID của student cần rà soát
- `semesterId` (optional): ID của semester (null = học kỳ hiện tại)

**CURL Command:**
```bash
# Rà soát với học kỳ hiện tại
curl --location --request POST 'http://localhost:8080/api/scores/recalculate/student/{studentId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Hoặc chỉ định semester cụ thể:**
```bash
curl --location --request POST 'http://localhost:8080/api/scores/recalculate/student/{studentId}?semesterId={semesterId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Recalculated student score successfully",
  "data": {
    "studentId": 1,
    "studentCode": "SV001",
    "studentName": "Nguyễn Văn A",
    "semesterId": 1,
    "semesterName": "Học kỳ 1 - 2025",
    "totalScoreTypes": 3,
    "updatedScores": [
      {
        "scoreType": "REN_LUYEN",
        "participationScore": 25.0,
        "milestoneScore": 10.0,
        "totalScore": 35.0,
        "oldScore": 20.0,
        "updated": true
      },
      {
        "scoreType": "CONG_TAC_XA_HOI",
        "participationScore": 15.0,
        "milestoneScore": 0.0,
        "totalScore": 15.0,
        "oldScore": 15.0,
        "updated": false
      },
      {
        "scoreType": "CHUYEN_DE",
        "participationScore": 5.0,
        "milestoneScore": 0.0,
        "totalScore": 5.0,
        "oldScore": 0.0,
        "updated": true
      }
    ]
  }
}
```

**Giải thích các trường:**
- `participationScore`: Tổng điểm từ ActivityParticipation COMPLETED (minigame, activity thường)
- `milestoneScore`: Tổng điểm từ milestone của các series
- `totalScore`: Tổng điểm = participationScore + milestoneScore
- `oldScore`: Điểm cũ trước khi recalculate
- `updated`: `true` nếu điểm đã thay đổi, `false` nếu không đổi

---

## PHẦN 2: RÀ SOÁT ĐIỂM CHO TẤT CẢ STUDENTS

### 2.1. Rà soát và tính lại điểm cho tất cả students

**API:** `POST /api/scores/recalculate/all`

**Yêu cầu:**
- Role: `ADMIN` hoặc `MANAGER`
- `semesterId` (optional): ID của semester (null = học kỳ hiện tại)

**⚠️ CẢNH BÁO:** API này sẽ tính lại điểm cho **TẤT CẢ** students trong hệ thống. Có thể mất thời gian nếu có nhiều students.

**CURL Command:**
```bash
# Rà soát tất cả students với học kỳ hiện tại
curl --location --request POST 'http://localhost:8080/api/scores/recalculate/all' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Hoặc chỉ định semester cụ thể:**
```bash
curl --location --request POST 'http://localhost:8080/api/scores/recalculate/all?semesterId={semesterId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Recalculated all student scores",
  "data": {
    "semesterId": 1,
    "semesterName": "Học kỳ 1 - 2025",
    "totalStudents": 100,
    "successCount": 98,
    "errorCount": 2,
    "errors": [
      {
        "studentId": 50,
        "studentCode": "SV050",
        "error": "No semester found"
      },
      {
        "studentId": 75,
        "studentCode": "SV075",
        "error": "Student not found"
      }
    ]
  }
}
```

**Giải thích các trường:**
- `totalStudents`: Tổng số students được rà soát
- `successCount`: Số students được cập nhật thành công
- `errorCount`: Số students gặp lỗi
- `errors`: Danh sách lỗi (nếu có)

---

## PHẦN 3: KIỂM TRA ĐIỂM SAU KHI RÀ SOÁT

### 3.1. Xem điểm của student

**API:** `GET /api/scores/student/{studentId}/semester/{semesterId}`

**Yêu cầu:**
- Role: `STUDENT` (chỉ xem được điểm của chính mình), `ADMIN`, hoặc `MANAGER`

```bash
curl --location 'http://localhost:8080/api/scores/student/{studentId}/semester/{semesterId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Scores retrieved",
  "data": {
    "studentId": 1,
    "semesterId": 1,
    "summaries": [
      {
        "scoreType": "REN_LUYEN",
        "total": 35.0,
        "items": [
          {
            "score": 35.0,
            "activityIds": [],
            "notes": null
          }
        ]
      },
      {
        "scoreType": "CONG_TAC_XA_HOI",
        "total": 15.0,
        "items": [...]
      },
      {
        "scoreType": "CHUYEN_DE",
        "total": 5.0,
        "items": [...]
      }
    ]
  }
}
```

### 3.2. Xem tổng điểm của student

**API:** `GET /api/scores/student/{studentId}/semester/{semesterId}/total`

```bash
curl --location 'http://localhost:8080/api/scores/student/{studentId}/semester/{semesterId}/total' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Total score calculated",
  "data": {
    "studentId": 1,
    "semesterId": 1,
    "grandTotal": 55.0,
    "totalsByType": {
      "REN_LUYEN": 35.0,
      "CONG_TAC_XA_HOI": 15.0,
      "CHUYEN_DE": 5.0
    },
    "scoreCount": 3
  }
}
```

---

## PHẦN 4: LOGIC TÍNH ĐIỂM

### 4.1. Các nguồn điểm được tính:

1. **ActivityParticipation COMPLETED:**
   - Điểm từ minigame quiz (khi PASSED)
   - Điểm từ activity thường (check-in/check-out)
   - Điểm từ activity có submission (khi được chấm điểm)

2. **Milestone Points từ Series:**
   - Điểm từ `StudentSeriesProgress.pointsEarned`
   - Được cộng vào `scoreType` của series (thường là REN_LUYEN)

### 4.2. Cách tính:

```
Tổng điểm (scoreType) = 
    Tổng điểm từ ActivityParticipation (scoreType) 
    + Tổng điểm milestone từ các series (scoreType)
```

### 4.3. ScoreHistory:

Mỗi lần recalculate, nếu điểm thay đổi, hệ thống sẽ tạo một bản ghi `ScoreHistory` với:
- `oldScore`: Điểm cũ
- `newScore`: Điểm mới
- `reason`: "Recalculated score: Participation (X) + Milestone (Y)"

---

## PHẦN 5: FLOW TEST HOÀN CHỈNH

### Flow 1: Rà soát điểm cho một student cụ thể

```bash
# 1. Rà soát điểm cho student
curl --location --request POST 'http://localhost:8080/api/scores/recalculate/student/1' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'

# 2. Kiểm tra điểm sau khi rà soát
curl --location 'http://localhost:8080/api/scores/student/1/semester/1' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'

# 3. Kiểm tra tổng điểm
curl --location 'http://localhost:8080/api/scores/student/1/semester/1/total' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### Flow 2: Rà soát điểm cho tất cả students

```bash
# 1. Rà soát tất cả students
curl --location --request POST 'http://localhost:8080/api/scores/recalculate/all' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'

# 2. Kiểm tra kết quả (xem successCount và errorCount)
```

---

## PHẦN 6: TROUBLESHOOTING

### 6.1. Student không có điểm sau khi recalculate

**Nguyên nhân có thể:**
1. Student chưa có `StudentScore` record cho `scoreType` đó
   - **Giải pháp:** Service sẽ tự động tạo `StudentScore` mới nếu chưa có
2. Activity không có `scoreType`
   - **Giải thích:** Chỉ các activity có `scoreType` mới được tính vào điểm
3. Participation không có `ParticipationType.COMPLETED`
   - **Giải thích:** Chỉ các participation có status `COMPLETED` mới được tính

### 6.2. Milestone points không được cộng

**Nguyên nhân có thể:**
1. Series không có `scoreType`
   - **Giải thích:** Series phải có `scoreType` để milestone points được cộng vào đúng loại điểm
2. `StudentSeriesProgress.pointsEarned` = 0 hoặc null
   - **Giải thích:** Chỉ các series có milestone points > 0 mới được cộng

### 6.3. Điểm bị sai sau khi recalculate

**Kiểm tra:**
1. Xem `updatedScores` trong response để biết:
   - `participationScore`: Điểm từ participations
   - `milestoneScore`: Điểm từ milestones
   - `totalScore`: Tổng điểm
2. So sánh với dữ liệu thực tế:
   - Kiểm tra `activity_participations` table
   - Kiểm tra `student_series_progress` table

---

## PHẦN 7: BEST PRACTICES

1. **Nên recalculate sau khi:**
   - Cập nhật logic tính điểm
   - Thêm/sửa/xóa ActivityParticipation
   - Thêm/sửa milestone points trong series
   - Import dữ liệu từ hệ thống cũ

2. **Không nên recalculate quá thường xuyên:**
   - Chỉ recalculate khi cần thiết (sau khi có thay đổi lớn)
   - Recalculate tất cả students có thể mất thời gian

3. **Kiểm tra kỹ trước khi recalculate tất cả:**
   - Test với 1-2 students trước
   - Đảm bảo logic tính điểm đúng
   - Backup database nếu cần

---

## TÓM TẮT

### API Endpoints:

1. **`POST /api/scores/recalculate/student/{studentId}`**
   - Rà soát và tính lại điểm cho một student
   - Role: ADMIN, MANAGER

2. **`POST /api/scores/recalculate/all`**
   - Rà soát và tính lại điểm cho tất cả students
   - Role: ADMIN, MANAGER

3. **`GET /api/scores/student/{studentId}/semester/{semesterId}`**
   - Xem điểm chi tiết của student
   - Role: STUDENT (chỉ mình), ADMIN, MANAGER

4. **`GET /api/scores/student/{studentId}/semester/{semesterId}/total`**
   - Xem tổng điểm của student
   - Role: STUDENT (chỉ mình), ADMIN, MANAGER

### Logic tính điểm:

```
StudentScore (scoreType) = 
    SUM(ActivityParticipation.pointsEarned WHERE scoreType = X AND participationType = COMPLETED)
    + SUM(StudentSeriesProgress.pointsEarned WHERE series.scoreType = X)
```

