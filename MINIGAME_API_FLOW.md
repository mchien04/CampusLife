# Luồng Tạo và Sử Dụng Minigame - Tổng Hợp API

## 📋 TỔNG QUAN

Minigame là một loại Activity đặc biệt cho phép student làm quiz để nhận điểm. Luồng tạo minigame gồm 2 bước chính và có 6 API chính.

---

## 🔄 LUỒNG TẠO MINIGAME (Admin/Manager)

### Bước 1: Tạo Activity với type = MINIGAME

**API:** `POST /api/activities`

**Yêu cầu:**
- Role: `ADMIN` hoặc `MANAGER`
- `type`: **BẮT BUỘC** phải là `"MINIGAME"`

**Request Body:**
```json
{
  "name": "Quiz kiến thức IT",
  "description": "Bài quiz về kiến thức IT cơ bản",
  "type": "MINIGAME",
  "scoreType": "REN_LUYEN",
  "startDate": "2025-02-01T08:00:00",
  "endDate": "2025-02-01T23:59:59",
  "registrationStartDate": "2025-01-20T00:00:00",
  "registrationDeadline": "2025-02-01T23:59:59",
  "requiresSubmission": false,
  "maxPoints": null,
  "isDraft": false,
  "requiresApproval": false,
  "location": "Online",
  "ticketQuantity": 1000
}
```

**Lưu ý:**
- `maxPoints`: Không dùng để tính điểm (có thể null)
- `penaltyPointsIncomplete`: Không dùng cho minigame
- Lưu lại `activityId` từ response

---

### Bước 2: Tạo Minigame với Quiz

**API:** `POST /api/minigames`

**Yêu cầu:**
- Role: `ADMIN` hoặc `MANAGER`
- `activityId`: ID của Activity đã tạo ở bước 1

**Request Body:**
```json
{
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
    }
    // ... thêm các câu hỏi khác
  ]
}
```

**Giải thích các trường:**
- `activityId` (bắt buộc): ID của Activity (type = MINIGAME)
- `title` (bắt buộc): Tiêu đề minigame
- `questionCount` (bắt buộc): Số lượng câu hỏi
- `timeLimit` (tùy chọn): Thời gian giới hạn (giây), null = không giới hạn
- `requiredCorrectAnswers` (tùy chọn): Số câu đúng tối thiểu để đạt, null = phải đúng tất cả
- `rewardPoints` (tùy chọn): Điểm thưởng khi đạt, null = 0 điểm
- `questions` (bắt buộc): Mảng câu hỏi, mỗi câu có:
  - `questionText`: Nội dung câu hỏi
  - `options`: Mảng lựa chọn, mỗi option có:
    - `text`: Nội dung lựa chọn
    - `isCorrect`: true/false

**Entities được tạo tự động:**
- 1 `MiniGame`
- 1 `MiniGameQuiz`
- N `MiniGameQuizQuestion` (N = số câu hỏi)
- M `MiniGameQuizOption` (M = tổng số options)

---

## 🎮 LUỒNG STUDENT SỬ DỤNG MINIGAME

### Bước 1: Lấy thông tin Minigame

**API:** `GET /api/minigames/activity/{activityId}`

**Yêu cầu:**
- Role: `STUDENT`, `ADMIN`, hoặc `MANAGER`
- `activityId`: ID của Activity (type = MINIGAME)

**Response:**
```json
{
  "status": true,
  "data": {
    "id": 1,
    "title": "Quiz kiến thức IT",
    "questionCount": 5,
    "timeLimit": 300,
    "requiredCorrectAnswers": 3,
    "rewardPoints": 10.0
  }
}
```

---

### Bước 2: Lấy danh sách câu hỏi

**API:** `GET /api/minigames/{miniGameId}/questions`

**Yêu cầu:**
- Role: `STUDENT`, `ADMIN`, hoặc `MANAGER`
- `miniGameId`: ID của MiniGame

**Response:**
```json
{
  "status": true,
  "data": {
    "miniGameId": 1,
    "title": "Quiz kiến thức IT",
    "questionCount": 5,
    "timeLimit": 300,
    "questions": [
      {
        "id": 1,
        "questionText": "HTML là viết tắt của gì?",
        "displayOrder": 0,
        "options": [
          {"id": 1, "text": "HyperText Markup Language"},
          {"id": 2, "text": "High Tech Modern Language"},
          {"id": 3, "text": "Home Tool Markup Language"},
          {"id": 4, "text": "Hyperlink and Text Markup Language"}
        ]
      }
      // ... các câu hỏi khác
    ]
  }
}
```

**Lưu ý:** KHÔNG có field `isCorrect` trong options để student không biết đáp án đúng.

---

### Bước 3: Bắt đầu làm Quiz

**API:** `POST /api/minigames/{miniGameId}/start`

**Yêu cầu:**
- Role: `STUDENT`
- `miniGameId`: ID của MiniGame

**Response:**
```json
{
  "status": true,
  "data": {
    "id": 1,
    "status": "IN_PROGRESS",
    "startedAt": "2025-02-05T10:00:00",
    "timeLimit": 300
  }
}
```

**Lưu ý:**
- Tạo `MiniGameAttempt` với status = `IN_PROGRESS`
- Lưu lại `attemptId` từ response
- Nếu đã có attempt `IN_PROGRESS`, sẽ trả về lỗi

---

### Bước 4: Nộp bài Quiz

**API:** `POST /api/minigames/attempts/{attemptId}/submit`

**Yêu cầu:**
- Role: `STUDENT`
- `attemptId`: ID của attempt đã tạo ở bước 2

**Request Body:**
```json
{
  "answers": {
    "1": 1,
    "2": 2,
    "3": 2,
    "4": 2,
    "5": 1
  }
}
```

**Giải thích:**
- Key: `questionId` (String, parse thành Long)
- Value: `optionId` (Number)

**Response khi đạt (PASSED):**
```json
{
  "status": true,
  "data": {
    "id": 1,
    "status": "PASSED",
    "correctCount": 4,
    "totalQuestions": 5,
    "pointsEarned": 10.0,
    "participation": {
      "id": 100,
      "pointsEarned": 10.0,
      "isCompleted": true
    }
  }
}
```

**Response khi không đạt (FAILED):**
```json
{
  "status": true,
  "data": {
    "id": 1,
    "status": "FAILED",
    "correctCount": 2,
    "totalQuestions": 5,
    "pointsEarned": 0.0
  }
}
```

**Logic xử lý:**
1. Tính số câu đúng
2. So sánh với `requiredCorrectAnswers`:
   - **PASSED:** Tạo ActivityParticipation, cộng điểm vào StudentScore
   - **FAILED:** Không tạo participation, không trừ điểm, chỉ lưu attempt

---

### Bước 5: Xem chi tiết Attempt (sau khi submit)

**API:** `GET /api/minigames/attempts/{attemptId}`

**Yêu cầu:**
- Role: `STUDENT` (chỉ xem được attempt của chính mình)
- `attemptId`: ID của attempt

**Response:**
```json
{
  "status": true,
  "data": {
    "id": 1,
    "status": "PASSED",
    "correctCount": 4,
    "totalQuestions": 5,
    "pointsEarned": 10.0,
    "questions": [
      {
        "id": 1,
        "questionText": "HTML là viết tắt của gì?",
        "options": [
          {"id": 1, "text": "...", "isCorrect": true, "isSelected": true},
          {"id": 2, "text": "...", "isCorrect": false, "isSelected": false}
        ],
        "correctOptionId": 1,
        "selectedOptionId": 1,
        "isCorrect": true
      }
    ]
  }
}
```

**Lưu ý:** Chỉ trả về đáp án đúng sau khi đã submit (status != IN_PROGRESS).

---

### Bước 6: Xem lịch sử Attempts

**API:** `GET /api/minigames/{miniGameId}/attempts/my`

**Yêu cầu:**
- Role: `STUDENT`
- `miniGameId`: ID của MiniGame

**Response:**
```json
{
  "status": true,
  "data": [
    {
      "id": 1,
      "status": "PASSED",
      "correctCount": 4,
      "pointsEarned": 10.0,
      "startedAt": "2025-02-05T10:00:00",
      "submittedAt": "2025-02-05T10:05:00"
    },
    {
      "id": 2,
      "status": "FAILED",
      "correctCount": 2,
      "pointsEarned": 0.0,
      "startedAt": "2025-02-05T11:00:00",
      "submittedAt": "2025-02-05T11:03:00"
    }
  ]
}
```

---

## 📊 TÓM TẮT CÁC API

| API | Method | Role | Mô tả |
|-----|--------|------|-------|
| **Tạo và Quản lý** |
| `/api/activities` | POST | ADMIN/MANAGER | Tạo Activity (type = MINIGAME) |
| `/api/minigames` | POST | ADMIN/MANAGER | Tạo Minigame với Quiz |
| `/api/minigames` | GET | ADMIN/MANAGER | Lấy tất cả Minigames |
| `/api/minigames/{miniGameId}` | PUT | ADMIN/MANAGER | Cập nhật Minigame |
| `/api/minigames/{miniGameId}` | DELETE | ADMIN/MANAGER | Xóa Minigame (soft delete) |
| **Xem thông tin** |
| `/api/minigames/activity/{activityId}` | GET | STUDENT/ADMIN/MANAGER | Lấy Minigame theo Activity ID |
| `/api/minigames/{miniGameId}/questions` | GET | STUDENT/ADMIN/MANAGER | Lấy danh sách câu hỏi (KHÔNG có đáp án đúng) |
| **Làm Quiz** |
| `/api/minigames/{miniGameId}/start` | POST | STUDENT | Bắt đầu làm quiz |
| `/api/minigames/attempts/{attemptId}/submit` | POST | STUDENT | Nộp bài quiz |
| **Xem kết quả** |
| `/api/minigames/attempts/{attemptId}` | GET | STUDENT | Xem chi tiết attempt (có đáp án đúng) |
| `/api/minigames/{miniGameId}/attempts/my` | GET | STUDENT | Xem lịch sử attempts |

---

## 🔑 CÁC ĐIỂM QUAN TRỌNG

### 1. Tính điểm
- ✅ **PASSED:** Tạo ActivityParticipation với `pointsEarned = rewardPoints`, cộng vào StudentScore
- ✅ **FAILED:** Không tạo participation, không trừ điểm

### 2. Activity.maxPoints
- ❌ **KHÔNG dùng** để tính điểm cho minigame
- ✅ Điểm thực tế lấy từ `MiniGame.rewardPoints`

### 3. Penalty Points
- ❌ **KHÔNG có** penalty points cho minigame
- ✅ Nếu không đạt, chỉ lưu attempt với status = FAILED

### 4. Multiple Attempts
- ✅ Student có thể làm nhiều lần
- ✅ Mỗi lần tạo một attempt mới
- ✅ Chỉ attempts PASSED mới tạo ActivityParticipation và cộng điểm

---

## 📝 VÍ DỤ HOÀN CHỈNH

### Admin tạo Minigame:
```bash
# 1. Tạo Activity
curl -X POST 'http://localhost:8080/api/activities' \
  -H 'Authorization: Bearer {ADMIN_TOKEN}' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Quiz IT",
    "type": "MINIGAME",
    "scoreType": "REN_LUYEN"
  }'
# → Lưu activityId = 2

# 2. Tạo Minigame
curl -X POST 'http://localhost:8080/api/minigames' \
  -H 'Authorization: Bearer {ADMIN_TOKEN}' \
  -H 'Content-Type: application/json' \
  -d '{
    "activityId": 2,
    "title": "Quiz IT",
    "questionCount": 5,
    "requiredCorrectAnswers": 3,
    "rewardPoints": 10.0,
    "questions": [...]
  }'
# → Lưu miniGameId = 1
```

### Student làm Quiz:
```bash
# 1. Lấy thông tin
curl 'http://localhost:8080/api/minigames/activity/2' \
  -H 'Authorization: Bearer {STUDENT_TOKEN}'

# 2. Lấy danh sách câu hỏi (KHÔNG có đáp án đúng)
curl 'http://localhost:8080/api/minigames/1/questions' \
  -H 'Authorization: Bearer {STUDENT_TOKEN}'

# 3. Bắt đầu
curl -X POST 'http://localhost:8080/api/minigames/1/start' \
  -H 'Authorization: Bearer {STUDENT_TOKEN}'
# → Lưu attemptId = 1

# 4. Nộp bài
curl -X POST 'http://localhost:8080/api/minigames/attempts/1/submit' \
  -H 'Authorization: Bearer {STUDENT_TOKEN}' \
  -H 'Content-Type: application/json' \
  -d '{"answers": {"1": 1, "2": 2, ...}}'

# 5. Xem chi tiết attempt (có đáp án đúng)
curl 'http://localhost:8080/api/minigames/attempts/1' \
  -H 'Authorization: Bearer {STUDENT_TOKEN}'

# 6. Xem lịch sử
curl 'http://localhost:8080/api/minigames/1/attempts/my' \
  -H 'Authorization: Bearer {STUDENT_TOKEN}'
```

