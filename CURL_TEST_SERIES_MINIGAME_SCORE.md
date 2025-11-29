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
  "order": 1,
  "shareLink": "https://example.com/event1",
  "bannerUrl": "https://example.com/banner1.jpg",
  "benefits": "Lợi ích khi tham gia",
  "requirements": "Yêu cầu tham gia",
  "contactInfo": "Email: contact@example.com, Phone: 0123456789",
  "organizerIds": [1, 2, 3]
}'
```

**Lưu ý:**

#### ✅ Các thuộc tính CẦN NHẬP:
- `name` (bắt buộc) - Tên sự kiện
- `description` (tùy chọn) - Mô tả
- `startDate` (tùy chọn) - Thời gian bắt đầu
- `endDate` (tùy chọn) - Thời gian kết thúc
- `location` (tùy chọn) - Địa điểm
- `order` (tùy chọn) - Thứ tự trong series
- `shareLink` (tùy chọn) - Link chia sẻ
- `bannerUrl` (tùy chọn) - URL banner
- `benefits` (tùy chọn) - Lợi ích khi tham gia
- `requirements` (tùy chọn) - Yêu cầu tham gia
- `contactInfo` (tùy chọn) - Thông tin liên hệ
- `organizerIds` (tùy chọn) - Danh sách ID các khoa/ban tổ chức (mảng số)

#### ❌ Các thuộc tính KHÔNG CẦN NHẬP (tự động được set):

**1. Lấy từ Series:**
- `registrationStartDate` → lấy từ `ActivitySeries.registrationStartDate`
- `registrationDeadline` → lấy từ `ActivitySeries.registrationDeadline`
- `requiresApproval` → lấy từ `ActivitySeries.requiresApproval`
- `ticketQuantity` → lấy từ `ActivitySeries.ticketQuantity`
- `scoreType` → lấy từ `ActivitySeries.scoreType` (để tính milestone points)

**2. Tự động set giá trị mặc định:**
- `type` → `null` (không cần loại activity)
- `maxPoints` → `null` (không dùng để tính điểm, dùng milestone thay thế)
- `isImportant` → `false`
- `mandatoryForFacultyStudents` → `false`
- `penaltyPointsIncomplete` → `null` (không trừ điểm)
- `requiresSubmission` → `false`
- `isDraft` → `false` (tự động published)
- `isDeleted` → `false`
- `seriesId` → tự động set từ path variable `{seriesId}`
- `seriesOrder` → từ tham số `order` (nếu có)

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

#### 1.12.1. Student xem progress của chính mình

```bash
curl --location 'http://localhost:8080/api/series/{seriesId}/progress/my' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Student progress retrieved successfully",
  "data": {
    "studentId": 1,
    "seriesId": 1,
    "seriesName": "Chuỗi sự kiện mùa hè",
    "completedCount": 3,
    "totalActivities": 5,
    "completedActivityIds": [1, 2, 3],
    "pointsEarned": 5.0,
    "lastUpdated": "2025-02-05T10:30:00",
    "currentMilestone": "3",
    "nextMilestoneCount": 4,
    "nextMilestonePoints": 7,
    "milestonePoints": {
      "3": 5,
      "4": 7,
      "5": 10
    },
    "scoreType": "REN_LUYEN"
  }
}
```

#### 1.12.2. Admin/Manager xem progress của student khác

```bash
curl --location 'http://localhost:8080/api/series/{seriesId}/students/{studentId}/progress' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:** Tương tự như trên

**Giải thích các trường:**
- `completedCount`: Số sự kiện đã hoàn thành
- `totalActivities`: Tổng số sự kiện trong series
- `completedActivityIds`: Danh sách ID các sự kiện đã hoàn thành
- `pointsEarned`: Tổng điểm milestone đã nhận
- `currentMilestone`: Mốc hiện tại đã đạt (ví dụ: "3" nghĩa là đã đạt mốc 3 sự kiện)
- `nextMilestoneCount`: Số sự kiện cần hoàn thành để đạt mốc tiếp theo
- `nextMilestonePoints`: Điểm sẽ nhận khi đạt mốc tiếp theo
- `milestonePoints`: Map các mốc điểm (key: số sự kiện, value: điểm thưởng)

---

## PHẦN 2: MINIGAME QUIZ

### 📋 TỔNG QUAN LUỒNG TẠO MINIGAME

**Luồng tạo minigame gồm 2 bước chính:**

1. **Bước 1:** Tạo Activity với `type = MINIGAME` (bắt buộc)
2. **Bước 2:** Tạo Minigame với Quiz (sau khi có Activity)

**Các entity được tạo tự động:**
- `MiniGame` (1 entity)
- `MiniGameQuiz` (1 entity)
- `MiniGameQuizQuestion` (nhiều câu hỏi)
- `MiniGameQuizOption` (nhiều lựa chọn cho mỗi câu hỏi)

---

### 🔄 CÁC API LIÊN QUAN ĐẾN MINIGAME

#### **API Tạo và Quản lý:**
1. `POST /api/activities` - Tạo Activity (type = MINIGAME) - **Bước 1**
2. `POST /api/minigames` - Tạo Minigame với Quiz - **Bước 2**
3. `GET /api/minigames/activity/{activityId}` - Lấy Minigame theo Activity ID

#### **API Student sử dụng:**
4. `POST /api/minigames/{miniGameId}/start` - Bắt đầu làm quiz (tạo attempt)
5. `POST /api/minigames/attempts/{attemptId}/submit` - Nộp bài quiz
6. `GET /api/minigames/{miniGameId}/attempts/my` - Xem lịch sử attempts của mình

---

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

**API:** `POST /api/minigames`

**Yêu cầu:**
- Role: `ADMIN` hoặc `MANAGER`
- `activityId`: ID của Activity đã tạo ở bước 1 (type = MINIGAME)

**Request Body:**
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

**Giải thích các trường:**
- `activityId` (bắt buộc): ID của Activity đã tạo ở bước 1 (phải có `type = MINIGAME`)
- `title` (bắt buộc): Tiêu đề minigame
- `description` (tùy chọn): Mô tả minigame
- `questionCount` (bắt buộc): Số lượng câu hỏi (phải khớp với số câu hỏi trong mảng `questions`)
- `timeLimit` (tùy chọn): Thời gian giới hạn làm bài (giây), null = không giới hạn
- `requiredCorrectAnswers` (tùy chọn): Số câu đúng tối thiểu để đạt (PASSED), null = phải đúng tất cả
- `rewardPoints` (tùy chọn): Điểm thưởng khi đạt quiz (số dương), null = 0 điểm
- `questions` (bắt buộc): Mảng các câu hỏi, mỗi câu hỏi có:
  - `questionText` (bắt buộc): Nội dung câu hỏi
  - `options` (bắt buộc): Mảng các lựa chọn, mỗi option có:
    - `text` (bắt buộc): Nội dung lựa chọn
    - `isCorrect` (bắt buộc): `true` nếu là đáp án đúng, `false` nếu sai

**Response:**
```json
{
  "status": true,
  "message": "MiniGame created successfully",
  "data": {
    "id": 1,
    "title": "Quiz kiến thức IT",
    "description": "Bài quiz về kiến thức IT cơ bản",
    "questionCount": 5,
    "timeLimit": 300,
    "requiredCorrectAnswers": 3,
    "rewardPoints": 10.0,
    "isActive": true,
    "type": "QUIZ",
    "activity": {
      "id": 2,
      "name": "Quiz kiến thức IT",
      ...
    }
  }
}
```

**Lưu ý:**
- Sau khi tạo thành công, hệ thống tự động tạo:
  - 1 `MiniGame` entity
  - 1 `MiniGameQuiz` entity
  - N `MiniGameQuizQuestion` entities (N = số câu hỏi)
  - M `MiniGameQuizOption` entities (M = tổng số options của tất cả câu hỏi)
- Lưu lại `miniGameId` từ response để dùng cho các API tiếp theo

---

### 2.3. Lấy Minigame theo Activity ID

**API:** `GET /api/minigames/activity/{activityId}`

**Yêu cầu:**
- Role: `STUDENT`, `ADMIN`, hoặc `MANAGER`
- `activityId`: ID của Activity (type = MINIGAME)

```bash
curl --location 'http://localhost:8080/api/minigames/activity/{activityId}' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "MiniGame retrieved successfully",
  "data": {
    "id": 1,
    "title": "Quiz kiến thức IT",
    "description": "Bài quiz về kiến thức IT cơ bản",
    "questionCount": 5,
    "timeLimit": 300,
    "requiredCorrectAnswers": 3,
    "rewardPoints": 10.0,
    "isActive": true,
    "type": "QUIZ",
    "activity": {
      "id": 2,
      "name": "Quiz kiến thức IT",
      ...
    }
  }
}
```

**Lưu ý:** API này dùng để lấy thông tin minigame trước khi student bắt đầu làm quiz.

---

### 2.4. Student bắt đầu làm Quiz

**API:** `POST /api/minigames/{miniGameId}/start`

**Yêu cầu:**
- Role: `STUDENT`
- `miniGameId`: ID của MiniGame (lấy từ bước 2.2 hoặc 2.3)

```bash
curl --location --request POST 'http://localhost:8080/api/minigames/{miniGameId}/start' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Attempt started successfully",
  "data": {
    "id": 1,
    "miniGameId": 1,
    "studentId": 123,
    "status": "IN_PROGRESS",
    "startedAt": "2025-02-05T10:00:00",
    "timeLimit": 300
  }
}
```

**Lưu ý:**
- Tạo một `MiniGameAttempt` với status = `IN_PROGRESS`
- Lưu lại `attemptId` từ response để dùng ở bước 2.5
- Nếu đã có attempt `IN_PROGRESS`, sẽ trả về lỗi (phải submit attempt cũ trước)

---

### 2.5. Student nộp bài Quiz

**API:** `POST /api/minigames/attempts/{attemptId}/submit`

**Yêu cầu:**
- Role: `STUDENT`
- `attemptId`: ID của attempt đã tạo ở bước 2.4

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

**Request Body:**
- `answers`: Map với:
  - **Key**: `questionId` (String, nhưng sẽ được parse thành Long) - ID của câu hỏi
  - **Value**: `optionId` (Number) - ID của option đã chọn

**Response khi đạt (PASSED):**
```json
{
  "status": true,
  "message": "Attempt submitted successfully",
  "data": {
    "id": 1,
    "status": "PASSED",
    "correctCount": 4,
    "totalQuestions": 5,
    "pointsEarned": 10.0,
    "participation": {
      "id": 100,
      "pointsEarned": 10.0,
      "isCompleted": true,
      "participationType": "COMPLETED"
    }
  }
}
```

**Response khi không đạt (FAILED):**
```json
{
  "status": true,
  "message": "Attempt submitted successfully",
  "data": {
    "id": 1,
    "status": "FAILED",
    "correctCount": 2,
    "totalQuestions": 5,
    "requiredCorrectAnswers": 3,
    "pointsEarned": 0.0
  }
}
```

**Logic xử lý sau khi submit:**
1. Tính số câu đúng (`correctCount`)
2. So sánh với `requiredCorrectAnswers`:
   - **Nếu đạt (PASSED):**
     - Cập nhật attempt: `status = PASSED`, `correctCount = X`
     - Tạo `ActivityParticipation` với:
       - `pointsEarned = rewardPoints` (từ MiniGame)
       - `isCompleted = true`
       - `participationType = COMPLETED`
     - Cộng điểm vào `StudentScore` (scoreType từ Activity)
   - **Nếu không đạt (FAILED):**
     - Cập nhật attempt: `status = FAILED`, `correctCount = X`
     - **KHÔNG** tạo ActivityParticipation
     - **KHÔNG** trừ điểm
     - Chỉ lưu attempt để theo dõi lịch sử

---

### 2.6. Lấy lịch sử Attempts của Student

**API:** `GET /api/minigames/{miniGameId}/attempts/my`

**Yêu cầu:**
- Role: `STUDENT`
- `miniGameId`: ID của MiniGame

```bash
curl --location 'http://localhost:8080/api/minigames/{miniGameId}/attempts/my' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Attempts retrieved successfully",
  "data": [
    {
      "id": 1,
      "status": "PASSED",
      "correctCount": 4,
      "totalQuestions": 5,
      "pointsEarned": 10.0,
      "startedAt": "2025-02-05T10:00:00",
      "submittedAt": "2025-02-05T10:05:00"
    },
    {
      "id": 2,
      "status": "FAILED",
      "correctCount": 2,
      "totalQuestions": 5,
      "pointsEarned": 0.0,
      "startedAt": "2025-02-05T11:00:00",
      "submittedAt": "2025-02-05T11:03:00"
    }
  ]
}
```

**Lưu ý:** API này trả về tất cả attempts của student cho minigame này, bao gồm cả PASSED và FAILED.

---

### 2.7. Lấy danh sách câu hỏi và options (KHÔNG có đáp án đúng)

**API:** `GET /api/minigames/{miniGameId}/questions`

**Yêu cầu:**
- Role: `STUDENT`, `ADMIN`, hoặc `MANAGER`
- `miniGameId`: ID của MiniGame

**Mục đích:** Student lấy danh sách câu hỏi để làm quiz (không có đáp án đúng để tránh gian lận)

```bash
curl --location 'http://localhost:8080/api/minigames/{miniGameId}/questions' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Questions retrieved successfully",
  "data": {
    "miniGameId": 1,
    "title": "Quiz kiến thức IT",
    "description": "Bài quiz về kiến thức IT cơ bản",
    "questionCount": 5,
    "timeLimit": 300,
    "questions": [
      {
        "id": 1,
        "questionText": "HTML là viết tắt của gì?",
        "displayOrder": 0,
        "options": [
          {
            "id": 1,
            "text": "HyperText Markup Language"
          },
          {
            "id": 2,
            "text": "High Tech Modern Language"
          },
          {
            "id": 3,
            "text": "Home Tool Markup Language"
          },
          {
            "id": 4,
            "text": "Hyperlink and Text Markup Language"
          }
        ]
      },
      {
        "id": 2,
        "questionText": "CSS được dùng để làm gì?",
        "displayOrder": 1,
        "options": [
          {
            "id": 5,
            "text": "Tạo cấu trúc trang web"
          },
          {
            "id": 6,
            "text": "Tạo style cho trang web"
          },
          {
            "id": 7,
            "text": "Xử lý logic"
          },
          {
            "id": 8,
            "text": "Lưu trữ dữ liệu"
          }
        ]
      }
      // ... các câu hỏi khác
    ]
  }
}
```

**Lưu ý:**
- ✅ **KHÔNG có** field `isCorrect` trong options (để student không biết đáp án đúng)
- ✅ Questions được sắp xếp theo `displayOrder`
- ✅ Options được trả về đầy đủ để student chọn

---

### 2.8. Xem chi tiết Attempt (sau khi submit)

**API:** `GET /api/minigames/attempts/{attemptId}`

**Yêu cầu:**
- Role: `STUDENT` (chỉ xem được attempt của chính mình)
- `attemptId`: ID của attempt

**Mục đích:** Student xem kết quả chi tiết sau khi submit, bao gồm đáp án đúng và câu trả lời của mình

```bash
curl --location 'http://localhost:8080/api/minigames/attempts/{attemptId}' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Attempt detail retrieved successfully",
  "data": {
    "id": 1,
    "status": "PASSED",
    "correctCount": 4,
    "totalQuestions": 5,
    "pointsEarned": 10.0,
    "startedAt": "2025-02-05T10:00:00",
    "submittedAt": "2025-02-05T10:05:00",
    "requiredCorrectAnswers": 3,
    "questions": [
      {
        "id": 1,
        "questionText": "HTML là viết tắt của gì?",
        "displayOrder": 0,
        "options": [
          {
            "id": 1,
            "text": "HyperText Markup Language",
            "isCorrect": true,
            "isSelected": true
          },
          {
            "id": 2,
            "text": "High Tech Modern Language",
            "isCorrect": false,
            "isSelected": false
          },
          {
            "id": 3,
            "text": "Home Tool Markup Language",
            "isCorrect": false,
            "isSelected": false
          },
          {
            "id": 4,
            "text": "Hyperlink and Text Markup Language",
            "isCorrect": false,
            "isSelected": false
          }
        ],
        "correctOptionId": 1,
        "selectedOptionId": 1,
        "isCorrect": true
      },
      {
        "id": 2,
        "questionText": "CSS được dùng để làm gì?",
        "displayOrder": 1,
        "options": [
          {
            "id": 5,
            "text": "Tạo cấu trúc trang web",
            "isCorrect": false,
            "isSelected": true
          },
          {
            "id": 6,
            "text": "Tạo style cho trang web",
            "isCorrect": true,
            "isSelected": false
          },
          {
            "id": 7,
            "text": "Xử lý logic",
            "isCorrect": false,
            "isSelected": false
          },
          {
            "id": 8,
            "text": "Lưu trữ dữ liệu",
            "isCorrect": false,
            "isSelected": false
          }
        ],
        "correctOptionId": 6,
        "selectedOptionId": 5,
        "isCorrect": false
      }
      // ... các câu hỏi khác
    ]
  }
}
```

**Giải thích các trường:**
- `status`: PASSED hoặc FAILED
- `correctCount`: Số câu đúng
- `pointsEarned`: Điểm đã nhận (chỉ khi PASSED)
- `questions`: Danh sách câu hỏi với:
  - `isCorrect`: true/false cho mỗi option
  - `isSelected`: true nếu student đã chọn option này
  - `correctOptionId`: ID của đáp án đúng
  - `selectedOptionId`: ID của option student đã chọn
  - `isCorrect`: true nếu student chọn đúng

**Lưu ý:**
- ✅ Chỉ trả về đáp án đúng sau khi đã submit (status != IN_PROGRESS)
- ✅ Student chỉ xem được attempt của chính mình

---

### 2.9. Cập nhật Minigame (Admin/Manager)

**API:** `PUT /api/minigames/{miniGameId}`

**Yêu cầu:**
- Role: `ADMIN` hoặc `MANAGER`
- `miniGameId`: ID của MiniGame cần cập nhật

```bash
curl --location --request PUT 'http://localhost:8080/api/minigames/{miniGameId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "title": "Quiz kiến thức IT (Cập nhật)",
  "description": "Mô tả mới",
  "questionCount": 6,
  "timeLimit": 360,
  "requiredCorrectAnswers": 4,
  "rewardPoints": 15.0,
  "questions": [
    {
      "questionText": "Câu hỏi mới?",
      "options": [
        {"text": "Đáp án A", "isCorrect": true},
        {"text": "Đáp án B", "isCorrect": false}
      ]
    }
    // ... các câu hỏi khác
  ]
}'
```

**Lưu ý:**
- Nếu có `questions` mới, hệ thống sẽ xóa tất cả questions và options cũ, tạo lại từ đầu
- Các trường khác có thể cập nhật riêng lẻ (không bắt buộc phải có tất cả)

---

### 2.10. Xóa Minigame (Admin/Manager)

**API:** `DELETE /api/minigames/{miniGameId}`

**Yêu cầu:**
- Role: `ADMIN` hoặc `MANAGER`
- `miniGameId`: ID của MiniGame cần xóa

```bash
curl --location --request DELETE 'http://localhost:8080/api/minigames/{miniGameId}' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Lưu ý:**
- Xóa mềm (soft delete): Chỉ set `isActive = false`
- Minigame vẫn tồn tại trong database nhưng không còn active

---

### 2.11. Lấy tất cả Minigames (Admin/Manager)

**API:** `GET /api/minigames`

**Yêu cầu:**
- Role: `ADMIN` hoặc `MANAGER`

```bash
curl --location 'http://localhost:8080/api/minigames' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "MiniGames retrieved successfully",
  "data": [
    {
      "id": 1,
      "title": "Quiz kiến thức IT",
      "description": "Bài quiz về kiến thức IT cơ bản",
      "questionCount": 5,
      "timeLimit": 300,
      "requiredCorrectAnswers": 3,
      "rewardPoints": 10.0,
      "isActive": true,
      "type": "QUIZ",
      "activity": {
        "id": 2,
        "name": "Quiz kiến thức IT",
        ...
      }
    },
    ...
  ]
}
```

---

### 📝 TÓM TẮT LUỒNG TẠO VÀ SỬ DỤNG MINIGAME

#### **Luồng Admin/Manager tạo Minigame:**
1. ✅ Tạo Activity với `type = "MINIGAME"` → Lưu `activityId`
2. ✅ Tạo Minigame với Quiz → Lưu `miniGameId`

#### **Luồng Student làm Quiz:**
1. ✅ Đăng ký Activity (nếu cần)
2. ✅ Lấy thông tin Minigame (`GET /api/minigames/activity/{activityId}`)
3. ✅ Lấy danh sách câu hỏi (`GET /api/minigames/{miniGameId}/questions`) - KHÔNG có đáp án đúng
4. ✅ Bắt đầu attempt (`POST /api/minigames/{miniGameId}/start`) → Lưu `attemptId`
5. ✅ Nộp bài (`POST /api/minigames/attempts/{attemptId}/submit`)
6. ✅ Xem chi tiết attempt (`GET /api/minigames/attempts/{attemptId}`) - Có đáp án đúng
7. ✅ Xem lịch sử attempts (`GET /api/minigames/{miniGameId}/attempts/my`)

#### **Logic tính điểm:**
- ✅ **PASSED:** Tạo ActivityParticipation, cộng điểm vào StudentScore
- ✅ **FAILED:** Không tạo participation, không trừ điểm, chỉ lưu attempt

#### **Các API quản lý (Admin/Manager):**
- ✅ `PUT /api/minigames/{miniGameId}` - Cập nhật minigame
- ✅ `DELETE /api/minigames/{miniGameId}` - Xóa minigame (soft delete)
- ✅ `GET /api/minigames` - Lấy tất cả minigames

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

