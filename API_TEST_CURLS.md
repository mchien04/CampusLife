# API Test CURLs cho Postman

## 1. ACTIVITY SERIES (Chuỗi Sự Kiện)

### 1.1. Tạo chuỗi sự kiện mới
```bash
POST http://localhost:8080/api/series
Authorization: Bearer {MANAGER_TOKEN}
Content-Type: application/json

{
  "name": "Chuỗi sự kiện mùa hè 2024",
  "description": "Các sự kiện trong mùa hè",
  "milestonePoints": "{\"3\": 5, \"4\": 7, \"5\": 10}",
  "mainActivityId": null
}
```

**CURL:**
```bash
curl --location 'http://localhost:8080/api/series' \
--header 'Authorization: Bearer {MANAGER_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "name": "Chuỗi sự kiện mùa hè 2024",
  "description": "Các sự kiện trong mùa hè",
  "milestonePoints": "{\"3\": 5, \"4\": 7, \"5\": 10}",
  "mainActivityId": null
}'
```

### 1.2. Thêm activity vào chuỗi
```bash
POST http://localhost:8080/api/series/{seriesId}/activities
Authorization: Bearer {MANAGER_TOKEN}
Content-Type: application/json

{
  "activityId": 1,
  "order": 1
}
```

**CURL:**
```bash
curl --location 'http://localhost:8080/api/series/1/activities' \
--header 'Authorization: Bearer {MANAGER_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "activityId": 1,
  "order": 1
}'
```

### 1.3. Tính điểm milestone cho student
```bash
POST http://localhost:8080/api/series/{seriesId}/students/{studentId}/calculate-milestone
Authorization: Bearer {MANAGER_TOKEN}
```

**CURL:**
```bash
curl --location 'http://localhost:8080/api/series/1/students/1/calculate-milestone' \
--header 'Authorization: Bearer {MANAGER_TOKEN}'
```

---

## 2. MINIGAME QUIZ

### 2.1. Tạo minigame với quiz
```bash
POST http://localhost:8080/api/minigames
Authorization: Bearer {MANAGER_TOKEN}
Content-Type: application/json

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
}
```

**CURL:**
```bash
curl --location 'http://localhost:8080/api/minigames' \
--header 'Authorization: Bearer {MANAGER_TOKEN}' \
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

### 2.2. Lấy minigame theo activity ID
```bash
GET http://localhost:8080/api/minigames/activity/{activityId}
Authorization: Bearer {STUDENT_TOKEN}
```

**CURL:**
```bash
curl --location 'http://localhost:8080/api/minigames/activity/2' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

### 2.3. Student bắt đầu làm quiz
```bash
POST http://localhost:8080/api/minigames/{miniGameId}/start
Authorization: Bearer {STUDENT_TOKEN}
```

**CURL:**
```bash
curl --location 'http://localhost:8080/api/minigames/1/start' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

### 2.4. Student nộp bài quiz
```bash
POST http://localhost:8080/api/minigames/attempts/{attemptId}/submit
Authorization: Bearer {STUDENT_TOKEN}
Content-Type: application/json

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

**CURL:**
```bash
curl --location 'http://localhost:8080/api/minigames/attempts/1/submit' \
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
- Key trong `answers` là `questionId` (Long)
- Value là `optionId` (Long) - ID của option đã chọn

### 2.5. Lấy lịch sử attempts của student
```bash
GET http://localhost:8080/api/minigames/{miniGameId}/attempts/my
Authorization: Bearer {STUDENT_TOKEN}
```

**CURL:**
```bash
curl --location 'http://localhost:8080/api/minigames/1/attempts/my' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

---

## 3. DUAL SCORE CALCULATION (CHUYEN_DE_DOANH_NGHIEP)

### 3.1. Check-in activity CHUYEN_DE_DOANH_NGHIEP
```bash
POST http://localhost:8080/api/registrations/checkin
Authorization: Bearer {STUDENT_TOKEN}
Content-Type: application/json

{
  "ticketCode": "ABC12345"
}
```

**CURL:**
```bash
curl --location 'http://localhost:8080/api/registrations/checkin' \
--header 'Authorization: Bearer {STUDENT_TOKEN}' \
--header 'Content-Type: application/json' \
--data '{
  "ticketCode": "ABC12345"
}'
```

**Lưu ý:** 
- Activity phải có `type = CHUYEN_DE_DOANH_NGHIEP`
- Activity phải có `maxPoints` để tính điểm RL
- Sau khi check-out, hệ thống sẽ tự động:
  - Cộng điểm CHUYEN_DE (đếm số buổi)
  - Cộng điểm REN_LUYEN (nếu có maxPoints)

---

## 4. ACTIVITY PHOTOS (Đã implement trước đó)

### 4.1. Upload ảnh cho activity
```bash
POST http://localhost:8080/api/activities/{activityId}/photos
Authorization: Bearer {MANAGER_TOKEN}
Content-Type: multipart/form-data

files: [file1.jpg, file2.jpg]
captions: ["Caption 1", "Caption 2"]
```

**CURL:**
```bash
curl --location 'http://localhost:8080/api/activities/1/photos' \
--header 'Authorization: Bearer {MANAGER_TOKEN}' \
--form 'files=@"/path/to/image1.jpg"' \
--form 'files=@"/path/to/image2.jpg"' \
--form 'captions="Caption 1"' \
--form 'captions="Caption 2"'
```

### 4.2. Lấy danh sách ảnh của activity
```bash
GET http://localhost:8080/api/activities/{activityId}/photos
Authorization: Bearer {STUDENT_TOKEN}
```

**CURL:**
```bash
curl --location 'http://localhost:8080/api/activities/1/photos' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

### 4.3. Xóa ảnh
```bash
DELETE http://localhost:8080/api/activities/{activityId}/photos/{photoId}
Authorization: Bearer {MANAGER_TOKEN}
```

**CURL:**
```bash
curl --location --request DELETE 'http://localhost:8080/api/activities/1/photos/1' \
--header 'Authorization: Bearer {MANAGER_TOKEN}'
```

---

## 5. REMINDER SYSTEM

Reminder system chạy tự động mỗi giờ qua scheduled task. Không có API endpoint để test trực tiếp.

**Để test reminder:**
1. Tạo activity với `startDate` trong khoảng 1 ngày hoặc 1 giờ tới
2. Student đăng ký và được approve
3. Đợi scheduled task chạy (hoặc trigger thủ công trong code)
4. Kiểm tra Notification của student

---

## LƯU Ý QUAN TRỌNG:

1. **Thay thế tokens:**
   - `{MANAGER_TOKEN}` - Token của user có role MANAGER hoặc ADMIN
   - `{STUDENT_TOKEN}` - Token của user có role STUDENT

2. **Thay thế IDs:**
   - `{activityId}` - ID của activity
   - `{seriesId}` - ID của series
   - `{studentId}` - ID của student
   - `{miniGameId}` - ID của minigame
   - `{attemptId}` - ID của attempt

3. **Base URL:**
   - Thay `http://localhost:8080` bằng URL thực tế của server

4. **Test flow đề xuất:**
   - **Series:** Tạo series → Thêm activities → Student check-in → Tính milestone
   - **Quiz:** Tạo minigame → Student start → Student submit → Kiểm tra điểm
   - **Dual Score:** Tạo activity CHUYEN_DE_DOANH_NGHIEP với maxPoints → Student check-in → Kiểm tra cả 2 loại điểm

