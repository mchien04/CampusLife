# CURL Commands để Test Chuỗi Sự Kiện, Minigame, Logic Tính Điểm

## LƯU Ý QUAN TRỌNG

1. **Thay thế tokens:**
   - `{ADMIN_TOKEN}` hoặc `{MANAGER_TOKEN}` - Token của user có role ADMIN hoặc MANAGER
   - `{STUDENT_TOKEN}` - Token của user có role STUDENT

2. **Thay thế IDs:**
   - `{activityId}` - ID của activity
   - `{seriesId}` - ID của series
   - `{studentId}` - ID của student
   - `{miniGameId}` - ID của minigame
   - `{attemptId}` - ID của attempt
   - `{ticketCode}` - Mã ticket từ ActivityRegistration

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

## PHẦN 1: CHUỖI SỰ KIỆN (ACTIVITY SERIES)

### 1.1. Tạo chuỗi sự kiện mới

```bash
curl --location 'http://localhost:8080/api/series' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "name": "Chuỗi sự kiện mùa hè 2025",
  "description": "Các sự kiện trong mùa hè",
  "milestonePoints": "{\"3\": 5, \"4\": 7, \"5\": 10}",
  "scoreType": "REN_LUYEN",
  "mainActivityId": null,
  "registrationStartDate": "2025-01-20T00:00:00",
  "registrationDeadline": "2025-02-15T23:59:59",
  "requiresApproval": false,
  "ticketQuantity": 100
}'
```

**Lưu ý:**
- `milestonePoints`: JSON string định nghĩa điểm theo số sự kiện đã hoàn thành
  - `{"3": 5}` = Hoàn thành 3 sự kiện → 5 điểm
  - `{"4": 7}` = Hoàn thành 4 sự kiện → 7 điểm
  - `{"5": 10}` = Hoàn thành 5 sự kiện → 10 điểm
- `scoreType`: Loại điểm để cộng milestone (`REN_LUYEN`, `CONG_TAC_XA_HOI`, `CHUYEN_DE`)
- `registrationStartDate`, `registrationDeadline`: Thời gian đăng ký cho cả chuỗi (các activity trong series sẽ dùng chung)
- `requiresApproval`: Có cần duyệt đăng ký không (mặc định: `true`)
- `ticketQuantity`: Số lượng vé/slot (null = không giới hạn)

### 1.2. Tạo Activity trong Series (API mới - Tối giản)

**API mới:** `POST /api/series/{seriesId}/activities/create` - Tạo activity trực tiếp trong series với các thuộc tính tối giản.

```bash
curl --location 'http://localhost:8080/api/series/{seriesId}/activities/create' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "name": "Sự kiện 1 trong chuỗi",
  "description": "Mô tả sự kiện 1",
  "startDate": "2025-02-01T08:00:00",
  "endDate": "2025-02-01T17:00:00",
  "location": "Phòng A101",
  "order": 1
}'
```

**Lưu ý:**
- Chỉ cần các thuộc tính cơ bản: `name`, `description`, `startDate`, `endDate`, `location`, `order`
- Các thuộc tính khác sẽ được lấy từ series:
  - `registrationStartDate`, `registrationDeadline` → từ series
  - `requiresApproval` → từ series
  - `ticketQuantity` → từ series
  - `scoreType` → từ series
- Các thuộc tính không cần (tự động null):
  - `type` → null
  - `maxPoints` → null (không dùng để tính điểm)
  - `isImportant` → false
  - `mandatoryForFacultyStudents` → false
  - `penaltyPointsIncomplete` → null

### 1.3. Thêm Activity đã tồn tại vào Series (Nếu cần)

**Nếu đã có activity sẵn, dùng endpoint này để thêm vào series:**

```bash
curl --location 'http://localhost:8080/api/series/{seriesId}/activities' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "activityId": 1,
  "order": 1
}'
```

**Lưu ý:**
- `activityId`: ID của activity vừa tạo ở bước 1.2
- `order`: Thứ tự trong series (1, 2, 3...)
- Sau khi thêm vào series, activity sẽ có `seriesId` và `seriesOrder` được set tự động
- Activity trong series sẽ không dùng `maxPoints` để tính điểm (dùng milestone points thay thế)

### 1.4. Student đăng ký Series (Tự động đăng ký tất cả Activities)

**API mới:** `POST /api/series/{seriesId}/register` - Đăng ký series sẽ tự động đăng ký tất cả activities trong series.

```bash
curl --location --request POST 'http://localhost:8080/api/series/{seriesId}/register' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Lưu ý:** 
- Tự động tạo `ActivityRegistration` cho **TẤT CẢ** activities trong series
- Nếu series có `requiresApproval = false` → Tất cả registrations sẽ tự động `APPROVED`
- Nếu series có `requiresApproval = true` → Tất cả registrations sẽ có status `PENDING` (cần admin/manager approve)
- Kiểm tra `registrationStartDate`, `registrationDeadline` của series
- Kiểm tra `ticketQuantity` của series (đếm số student đã đăng ký)
- Bỏ qua các activity đã đăng ký trước đó

**Response:**
```json
{
  "status": true,
  "message": "Registered for series successfully. 5 activities registered.",
  "data": [
    {
      "id": 1,
      "activityId": 1,
      "studentId": 1,
      "status": "APPROVED",
      ...
    },
    ...
  ]
}
```

### 1.5. Admin/Manager duyệt đăng ký (nếu cần)

```bash
curl --location --request PUT 'http://localhost:8080/api/registrations/{registrationId}/status?status=APPROVED' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 1.6. Student Check-in Activity trong Series (Lần 1)

```bash
curl --location 'http://localhost:8080/api/registrations/checkin' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "ticketCode": "{ticketCode}"
}'
```

**Response lần 1:**
- `participationType`: `CHECKED_IN`
- `pointsEarned`: `0` (KHÔNG tính từ maxPoints vì activity trong series)

### 1.7. Student Check-out Activity trong Series (Lần 2)

```bash
curl --location 'http://localhost:8080/api/registrations/checkin' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "ticketCode": "{ticketCode}"
}'
```

**Response lần 2:**
- `participationType`: `ATTENDED`
- `pointsEarned`: `0` (vẫn là 0)
- Hệ thống tự động:
  - Update Series Progress (`completedCount++`)
  - Tính Milestone Points (nếu đạt mốc)

### 1.8. Tính điểm Milestone cho Student (Manual trigger)

```bash
curl --location --request POST 'http://localhost:8080/api/series/{seriesId}/students/{studentId}/calculate-milestone' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Lưu ý:** 
- Endpoint này thường được gọi tự động sau khi check-out
- Có thể dùng để test hoặc recalculate

### 1.9. Lấy tất cả chuỗi sự kiện

```bash
curl --location 'http://localhost:8080/api/series' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Lưu ý:**
- STUDENT, ADMIN, MANAGER đều có thể xem
- Trả về danh sách tất cả chuỗi sự kiện

### 1.10. Lấy chuỗi sự kiện theo ID

```bash
curl --location 'http://localhost:8080/api/series/{seriesId}' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Series retrieved successfully",
  "data": {
    "id": 1,
    "name": "Chuỗi sự kiện mùa hè 2025",
    "description": "Các sự kiện trong mùa hè",
    "milestonePoints": "{\"3\": 5, \"4\": 7, \"5\": 10}",
    "scoreType": "REN_LUYEN",
    "registrationStartDate": "2025-01-20T00:00:00",
    "registrationDeadline": "2025-02-15T23:59:59",
    "requiresApproval": false,
    "ticketQuantity": 100,
    "createdAt": "2025-01-15T10:00:00"
  }
}
```

### 1.11. Lấy danh sách Activities trong Series

```bash
curl --location 'http://localhost:8080/api/series/{seriesId}/activities' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Activities in series retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "Sự kiện 1 trong chuỗi",
      "description": "Mô tả sự kiện 1",
      "startDate": "2025-02-01T08:00:00",
      "endDate": "2025-02-01T17:00:00",
      "location": "Phòng A101",
      "seriesId": 1,
      "seriesOrder": 1,
      "type": null,
      "scoreType": null,
      "maxPoints": null,
      ...
    },
    ...
  ]
}
```

**Lưu ý:**
- Activities được sắp xếp theo `seriesOrder` (1, 2, 3...)
- Các activities trong series có `type`, `scoreType`, `maxPoints` = null

### 1.12. Kiểm tra Student Progress trong Series

**Lưu ý:** Hiện tại chưa có endpoint GET để lấy progress, nhưng có thể kiểm tra qua:
- StudentScore (điểm milestone đã được cộng)
- ActivityParticipation (các activity đã tham gia)

---

## PHẦN 2: MINIGAME QUIZ

**Lưu ý quan trọng:** Phải tạo Activity với `type = MINIGAME` trước, sau đó mới tạo minigame với quiz.

### 2.1. Tạo Activity cho Minigame (Bước 1: Tạo Activity)

```bash
curl --location 'http://localhost:8080/api/activities' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "name": "Quiz kiến thức IT",
  "description": "Bài quiz về kiến thức IT cơ bản",
  "type": "MINIGAME",
  "scoreType": "REN_LUYEN",
  "startDate": "2025-02-01T08:00:00",
  "endDate": "2025-02-01T23:59:59",
  "registrationStartDate": "2025-01-20T00:00:00",
  "registrationDeadline": "2025-02-01T23:59:59",
  "requiresSubmission": false,
  "maxPoints": 10.0,
  "isDraft": false,
  "requiresApproval": false,
  "location": "Online",
  "ticketQuantity": 1000
}'
```

**Lưu ý:**
- `type`: **BẮT BUỘC** phải là `"MINIGAME"`
- `maxPoints`: **KHÔNG CẦN** (có thể để null hoặc bất kỳ giá trị nào, không được dùng để tính điểm)
- `penaltyPointsIncomplete`: **KHÔNG DÙNG** cho minigame (không trừ điểm khi không đạt quiz)
- Điểm thực tế (khi đạt) sẽ lấy từ `rewardPoints` của MiniGame entity (bước 2.2)
- Lưu lại `activityId` từ response để dùng ở bước 2.2

### 2.2. Tạo Minigame với Quiz (Bước 2: Tạo Quiz sau khi có Activity)

```bash
curl --location 'http://localhost:8080/api/minigames' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "activityId": 2,
  "title": "Quiz kiến thức IT",
  "description": "Bài quiz về kiến thức IT cơ bản",
  "questionCount": 5,
  "timeLimit": 300,
  "requiredCorrectAnswers": 3,
  "rewardPoints": 10.0,
  "questions": [
    {
      "questionText": "HTML là viết tắt của gì?",
      "options": [
        {"text": "HyperText Markup Language", "isCorrect": true},
        {"text": "High Tech Modern Language", "isCorrect": false},
        {"text": "Home Tool Markup Language", "isCorrect": false},
        {"text": "Hyperlink and Text Markup Language", "isCorrect": false}
      ]
    },
    {
      "questionText": "CSS được dùng để làm gì?",
      "options": [
        {"text": "Tạo cấu trúc trang web", "isCorrect": false},
        {"text": "Tạo style cho trang web", "isCorrect": true},
        {"text": "Xử lý logic", "isCorrect": false},
        {"text": "Lưu trữ dữ liệu", "isCorrect": false}
      ]
    },
    {
      "questionText": "JavaScript là ngôn ngữ gì?",
      "options": [
        {"text": "Ngôn ngữ biên dịch", "isCorrect": false},
        {"text": "Ngôn ngữ thông dịch", "isCorrect": true},
        {"text": "Ngôn ngữ đánh dấu", "isCorrect": false},
        {"text": "Ngôn ngữ kiểu dữ liệu", "isCorrect": false}
      ]
    },
    {
      "questionText": "React là gì?",
      "options": [
        {"text": "Một ngôn ngữ lập trình", "isCorrect": false},
        {"text": "Một framework JavaScript", "isCorrect": true},
        {"text": "Một database", "isCorrect": false},
        {"text": "Một hệ điều hành", "isCorrect": false}
      ]
    },
    {
      "questionText": "API là viết tắt của gì?",
      "options": [
        {"text": "Application Programming Interface", "isCorrect": true},
        {"text": "Advanced Programming Interface", "isCorrect": false},
        {"text": "Application Program Integration", "isCorrect": false},
        {"text": "Automated Program Interface", "isCorrect": false}
      ]
    }
  ]
}'
```

### 2.3. Lấy Minigame theo Activity ID

```bash
curl --location 'http://localhost:8080/api/minigames/activity/{activityId}' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

### 2.4. Student bắt đầu làm Quiz

```bash
curl --location --request POST 'http://localhost:8080/api/minigames/{miniGameId}/start' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response:** Trả về `attemptId` và thời gian bắt đầu

### 2.5. Student nộp bài Quiz

```bash
curl --location 'http://localhost:8080/api/minigames/attempts/{attemptId}/submit' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "answers": {
    "1": 1,
    "2": 2,
    "3": 2,
    "4": 2,
    "5": 1
  }
}'
```

**Lưu ý:**
- Key trong `answers` là `questionId` (Long) - ID của câu hỏi
- Value là `optionId` (Long) - ID của option đã chọn
- Sau khi submit, hệ thống sẽ:
  - Tính điểm (số câu đúng)
  - Kiểm tra `requiredCorrectAnswers`
  - **Nếu đạt (PASSED):**
    - Tạo ActivityParticipation với `pointsEarned = rewardPoints` (số dương)
    - `isCompleted = true`
    - Cộng điểm vào StudentScore
  - **Nếu không đạt (FAILED):**
    - Không làm gì (không trừ điểm, không tạo participation)
    - Chỉ lưu attempt với status = FAILED

### 2.6. Lấy lịch sử Attempts của Student

```bash
curl --location 'http://localhost:8080/api/minigames/{miniGameId}/attempts/my' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

---

## PHẦN 3: LOGIC TÍNH ĐIỂM ĐÃ CẬP NHẬT

### 3.1. Test Activity trong Series - Điểm = 0

**Flow:**
1. Tạo series với milestonePoints
2. Tạo activity trong series (maxPoints = null)
3. Student đăng ký và check-in/check-out
4. Kiểm tra:
   - `ActivityParticipation.pointsEarned = 0`
   - Series Progress được update
   - Milestone points được cộng vào StudentScore

**CURL để kiểm tra điểm sau check-out:**

```bash
# Lấy thông tin participation
curl --location 'http://localhost:8080/api/registrations/activities/{activityId}/participations' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

```bash
# Lấy điểm của student
curl --location 'http://localhost:8080/api/scores/student/{studentId}/semester/{semesterId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 3.2. Test Activity đơn lẻ - Điểm từ maxPoints

**Tạo Activity đơn lẻ (không thuộc series):**

```bash
curl --location 'http://localhost:8080/api/activities' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "name": "Sự kiện đơn lẻ",
  "description": "Sự kiện không thuộc series",
  "type": "SUKIEN",
  "scoreType": "REN_LUYEN",
  "seriesId": null,
  "maxPoints": 15.0,
  "startDate": "2025-02-01T08:00:00",
  "endDate": "2025-02-01T17:00:00",
  "requiresSubmission": false,
  "isDraft": false,
  "requiresApproval": false,
  "location": "Phòng A101",
  "ticketQuantity": 100
}'
```

**Check-in/Check-out:**

```bash
# Check-in lần 1
curl --location 'http://localhost:8080/api/registrations/checkin' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "ticketCode": "{ticketCode}"
}'

# Check-out lần 2 (gọi lại với cùng ticketCode)
curl --location 'http://localhost:8080/api/registrations/checkin' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "ticketCode": "{ticketCode}"
}'
```

**Kết quả:**
- `ActivityParticipation.pointsEarned = 15.0` (từ maxPoints)
- Điểm được cộng vào StudentScore (ScoreType = REN_LUYEN)

---

## PHẦN 4: CHUYÊN ĐỀ DOANH NGHIỆP (DUAL SCORE)

### 4.1. Tạo Activity CHUYEN_DE_DOANH_NGHIEP

```bash
curl --location 'http://localhost:8080/api/activities' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "name": "Chuyên đề doanh nghiệp - Buổi 1",
  "description": "Chuyên đề về quản trị doanh nghiệp",
  "type": "CHUYEN_DE_DOANH_NGHIEP",
  "scoreType": "CHUYEN_DE",
  "maxPoints": 5.0,
  "startDate": "2025-02-01T08:00:00",
  "endDate": "2025-02-01T17:00:00",
  "registrationStartDate": "2025-01-20T00:00:00",
  "registrationDeadline": "2025-02-01T23:59:59",
  "requiresSubmission": false,
  "isDraft": false,
  "requiresApproval": false,
  "location": "Phòng A101",
  "ticketQuantity": 100
}'
```

**Lưu ý:**
- `type`: `CHUYEN_DE_DOANH_NGHIEP` (bắt buộc)
- `scoreType`: `CHUYEN_DE` (để đếm số buổi)
- `maxPoints`: Điểm để cộng vào REN_LUYEN (ví dụ: 5.0)

### 4.2. Student đăng ký

```bash
curl --location 'http://localhost:8080/api/registrations' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "activityId": 3
}'
```

### 4.3. Student Check-in/Check-out

```bash
# Check-in lần 1
curl --location 'http://localhost:8080/api/registrations/checkin' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "ticketCode": "{ticketCode}"
}'

# Check-out lần 2
curl --location 'http://localhost:8080/api/registrations/checkin' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "ticketCode": "{ticketCode}"
}'
```

### 4.4. Kiểm tra Dual Score

**Sau khi check-out, hệ thống tự động:**

1. **CHUYEN_DE Score:**
   - Đếm số participation COMPLETED
   - Cập nhật `StudentScore.score = count` (ScoreType = CHUYEN_DE)
   - Ví dụ: 1 buổi → score = 1, 2 buổi → score = 2

2. **REN_LUYEN Score:**
   - Cộng `maxPoints` vào StudentScore (ScoreType = REN_LUYEN)
   - Ví dụ: maxPoints = 5.0 → +5 điểm REN_LUYEN

**CURL để kiểm tra:**

```bash
# Lấy điểm CHUYEN_DE
curl --location 'http://localhost:8080/api/scores/student/{studentId}/semester/{semesterId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response sẽ có:**
```json
{
  "status": true,
  "data": [
    {
      "scoreType": "CHUYEN_DE",
      "score": 1.0  // Số buổi đã tham gia
    },
    {
      "scoreType": "REN_LUYEN",
      "score": 5.0  // Điểm từ maxPoints
    }
  ]
}
```

### 4.5. Test nhiều buổi CHUYEN_DE_DOANH_NGHIEP

**Tạo thêm activity:**

```bash
curl --location 'http://localhost:8080/api/activities' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "name": "Chuyên đề doanh nghiệp - Buổi 2",
  "description": "Chuyên đề về quản trị doanh nghiệp",
  "type": "CHUYEN_DE_DOANH_NGHIEP",
  "scoreType": "CHUYEN_DE",
  "maxPoints": 5.0,
  "startDate": "2025-02-08T08:00:00",
  "endDate": "2025-02-08T17:00:00",
  "requiresSubmission": false,
  "isDraft": false,
  "requiresApproval": false,
  "location": "Phòng A101",
  "ticketQuantity": 100
}'
```

**Sau khi check-out buổi 2:**
- CHUYEN_DE score: 1 → 2 (đếm số buổi)
- REN_LUYEN score: 5.0 → 10.0 (+5.0 từ buổi 2)

---

## PHẦN 5: TEST FLOW HOÀN CHỈNH

### Flow 1: Test Series với Milestone Points

```bash
# 1. Tạo series
curl --location 'http://localhost:8080/api/series' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "name": "Chuỗi sự kiện test",
  "description": "Test milestone points",
  "milestonePoints": "{\"3\": 5, \"4\": 7, \"5\": 10}",
  "scoreType": "REN_LUYEN"
}'
# → Lưu lại seriesId từ response

# 2. Tạo 5 activities (tạo riêng, chưa thuộc series)
# (Lặp lại 5 lần, lưu lại activityId của mỗi activity)

# 3. Thêm từng activity vào series
curl --location 'http://localhost:8080/api/series/{seriesId}/activities' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "activityId": {activityId1},
  "order": 1
}'
# (Lặp lại cho 5 activities với order 1, 2, 3, 4, 5)

# 4. Student đăng ký tất cả activities

# 5. Student check-in/check-out từng activity

# 6. Kiểm tra:
#    - ActivityParticipation.pointsEarned = 0 (cho tất cả)
#    - Series Progress: completedCount tăng dần
#    - Milestone points được cộng:
#      * Sau 3 activities → +5 điểm REN_LUYEN
#      * Sau 4 activities → Cập nhật: -5 +7 = +2 điểm
#      * Sau 5 activities → Cập nhật: -7 +10 = +3 điểm
```

### Flow 2: Test Minigame Quiz

```bash
# 1. Tạo Activity với type = MINIGAME (Bước 1)
curl --location 'http://localhost:8080/api/activities' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "name": "Quiz kiến thức IT",
  "type": "MINIGAME",
  "scoreType": "REN_LUYEN",
  ...
}'
# → Lưu lại activityId từ response

# 2. Tạo minigame với quiz (Bước 2: Dùng activityId từ bước 1)
curl --location 'http://localhost:8080/api/minigames' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "activityId": {activityId},
  "title": "Quiz kiến thức IT",
  "questions": [...]
}'

# 3. Student đăng ký activity

# 4. Student start attempt

# 5. Student submit với answers đúng >= requiredCorrectAnswers

# 6. Kiểm tra:
#    - ActivityParticipation được tạo với pointsEarned = rewardPoints
#    - StudentScore được cộng điểm
```

### Flow 3: Test CHUYEN_DE_DOANH_NGHIEP Dual Score

```bash
# 1. Tạo 3 activities CHUYEN_DE_DOANH_NGHIEP (mỗi activity maxPoints = 5.0)

# 2. Student đăng ký và check-in/check-out từng activity

# 3. Kiểm tra sau mỗi buổi:
#    - CHUYEN_DE score: 1 → 2 → 3 (đếm số buổi)
#    - REN_LUYEN score: 5.0 → 10.0 → 15.0 (cộng maxPoints)
```

---

## PHẦN 6: CÁC ENDPOINT HỖ TRỢ

### 6.1. Lấy danh sách Activities

```bash
curl --location 'http://localhost:8080/api/activities' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

### 6.2. Lấy Activity theo ID

```bash
curl --location 'http://localhost:8080/api/activities/{activityId}' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

### 6.3. Lấy danh sách đăng ký của Student

```bash
curl --location 'http://localhost:8080/api/registrations/my' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

### 6.4. Kiểm tra trạng thái đăng ký

```bash
curl --location 'http://localhost:8080/api/registrations/check/{activityId}' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

### 6.5. Lấy danh sách Participations của Activity

```bash
curl --location 'http://localhost:8080/api/registrations/activities/{activityId}/participations' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 6.6. Lấy điểm của Student

```bash
# Lấy điểm theo semester
curl --location 'http://localhost:8080/api/scores/student/{studentId}/semester/{semesterId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'

# Lấy tổng điểm
curl --location 'http://localhost:8080/api/scores/student/{studentId}/semester/{semesterId}/total' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 6.7. Validate Ticket Code (trước khi check-in)

```bash
curl --location 'http://localhost:8080/api/registrations/checkin/validate?ticketCode={ticketCode}' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

---

## TÓM TẮT FLOW VÀ LOGIC TÍNH ĐIỂM

### Flow tạo Chuỗi Sự Kiện:
1. ✅ **Tạo Series** → Lưu `seriesId`
2. ✅ **Tạo Activity** (tạo riêng, chưa thuộc series) → Lưu `activityId`
3. ✅ **Thêm Activity vào Series** → Dùng `POST /api/series/{seriesId}/activities` với `activityId` và `order`

### Flow tạo Minigame:
1. ✅ **Tạo Activity** với `type = "MINIGAME"` → Lưu `activityId`
2. ✅ **Tạo Minigame với Quiz** → Dùng `POST /api/minigames` với `activityId` từ bước 1

### Logic tính điểm:

#### Activity trong Series:
- ✅ `pointsEarned = 0` (không tính từ maxPoints)
- ✅ Series Progress: `completedCount++`
- ✅ Milestone Points: Tính từ `milestonePoints` JSON, cộng vào StudentScore (scoreType từ series)

#### Activity đơn lẻ:
- ✅ `pointsEarned = maxPoints` (nếu có)
- ✅ Cộng điểm vào StudentScore (scoreType từ activity)

#### CHUYEN_DE_DOANH_NGHIEP:
- ✅ CHUYEN_DE: Đếm số participation COMPLETED → `StudentScore.score = count`
- ✅ REN_LUYEN: Cộng `maxPoints` vào StudentScore (ScoreType = REN_LUYEN)

#### Minigame:
- ✅ **KHÔNG dùng check-in/check-out** để tính điểm
- ✅ **Điểm cộng:** Từ `MiniGame.rewardPoints` (khi tạo minigame), **KHÔNG phải từ `Activity.maxPoints`**
- ✅ **KHÔNG có điểm trừ:** Khi không đạt quiz, không trừ điểm
- ✅ **Khi đạt (PASSED):**
  - Student submit quiz
  - Đạt `requiredCorrectAnswers`
  - Status = PASSED
  - Tạo ActivityParticipation với `pointsEarned = rewardPoints` (số dương)
  - `isCompleted = true`
  - Cộng điểm vào StudentScore
- ✅ **Khi không đạt (FAILED):**
  - Student submit quiz
  - Không đạt `requiredCorrectAnswers`
  - Status = FAILED
  - Không làm gì (không trừ điểm, không tạo participation)
  - Chỉ lưu attempt với status = FAILED để theo dõi lịch sử

