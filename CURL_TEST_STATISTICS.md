# CURL Commands để Test Statistics APIs

## LƯU Ý QUAN TRỌNG

1. **Thay thế tokens:**
   - `{ADMIN_TOKEN}` - Token của user có role ADMIN
   - `{MANAGER_TOKEN}` - Token của user có role MANAGER
   - `{STUDENT_TOKEN}` - Token của user có role STUDENT

2. **Base URL:** `http://localhost:8080` (thay đổi nếu cần)

3. **Lấy Token:** Đăng nhập trước để lấy JWT token
   ```bash
   curl --location 'http://localhost:8080/api/auth/login' \
   --header 'Content-Type: application/json' \
   --data '{
     "username": "admin",
     "password": "password"
   }'
   ```

4. **Quyền truy cập:**
   - **Dashboard:** ADMIN, MANAGER, STUDENT (mỗi role xem khác nhau)
   - **Activities:** ADMIN, MANAGER only
   - **Students:** ADMIN, MANAGER only
   - **Scores:** ADMIN, MANAGER, STUDENT (STUDENT chỉ xem điểm của mình)
   - **Series:** ADMIN, MANAGER, STUDENT
   - **MiniGames:** ADMIN, MANAGER, STUDENT
   - **Timeline:** ADMIN, MANAGER only

---

## 1. DASHBOARD OVERVIEW

**API:** `GET /api/statistics/dashboard`

**Mô tả:** Dashboard tổng quan với các số liệu chính và top activities/students.

**Quyền:** ADMIN, MANAGER, STUDENT

### 1.1. Admin/Manager xem dashboard

```bash
curl --location 'http://localhost:8080/api/statistics/dashboard' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Dashboard overview retrieved successfully",
  "data": {
    "totalActivities": 150,
    "totalStudents": 5000,
    "totalSeries": 10,
    "totalMiniGames": 25,
    "monthlyRegistrations": 500,
    "monthlyParticipations": 450,
    "averageParticipationRate": 0.9,
    "topActivities": [
      {
        "activityId": 1,
        "activityName": "Hội trại mùa hè",
        "registrationCount": 200,
        "participationCount": 180
      },
      ...
    ],
    "topStudents": [
      {
        "studentId": 123,
        "studentName": "Nguyễn Văn A",
        "studentCode": "SV001",
        "participationCount": 15
      },
      ...
    ]
  }
}
```

### 1.2. Student xem dashboard (thống kê cá nhân)

```bash
curl --location 'http://localhost:8080/api/statistics/dashboard' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Lưu ý:** Student sẽ chỉ xem thống kê liên quan đến chính mình (nếu được implement trong service).

---

## 2. ACTIVITY STATISTICS

**API:** `GET /api/statistics/activities`

**Mô tả:** Thống kê chi tiết về activities theo loại, trạng thái, khoa tổ chức, etc.

**Quyền:** ADMIN, MANAGER only

**Query Parameters:**
- `activityType` (optional): SUKIEN, MINIGAME, CONG_TAC_XA_HOI, CHUYEN_DE_DOANH_NGHIEP
- `scoreType` (optional): REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE
- `departmentId` (optional): ID của khoa
- `startDate` (optional): ISO 8601 format (yyyy-MM-ddTHH:mm:ss)
- `endDate` (optional): ISO 8601 format (yyyy-MM-ddTHH:mm:ss)

### 2.1. Lấy tất cả thống kê activities

```bash
curl --location 'http://localhost:8080/api/statistics/activities' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 2.2. Lấy thống kê activities theo loại

```bash
curl --location 'http://localhost:8080/api/statistics/activities?activityType=SUKIEN' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 2.3. Lấy thống kê activities theo khoảng thời gian

```bash
curl --location 'http://localhost:8080/api/statistics/activities?startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Activity statistics retrieved successfully",
  "data": {
    "totalActivities": 150,
    "countByType": {
      "SUKIEN": 80,
      "MINIGAME": 25,
      "CONG_TAC_XA_HOI": 30,
      "CHUYEN_DE_DOANH_NGHIEP": 15
    },
    "countByStatus": {
      "draft": 20,
      "published": 130,
      "deleted": 0
    },
    "topActivitiesByRegistrations": [
      {
        "activityId": 1,
        "activityName": "Hội trại mùa hè",
        "registrationCount": 200,
        "participationCount": 180
      },
      ...
    ],
    "participationRates": [
      {
        "activityId": 1,
        "activityName": "Hội trại mùa hè",
        "registrationCount": 200,
        "participationCount": 180,
        "participationRate": 0.9
      },
      ...
    ],
    "countByDepartment": {
      "1": 50,
      "2": 30,
      "3": 20
    },
    "activitiesInSeries": 40,
    "standaloneActivities": 110
  }
}
```

---

## 3. STUDENT STATISTICS

**API:** `GET /api/statistics/students`

**Mô tả:** Thống kê về sinh viên: tổng số, phân bố theo khoa/lớp, top participants, inactive students, etc.

**Quyền:** ADMIN, MANAGER only

**Query Parameters:**
- `departmentId` (optional): ID của khoa
- `classId` (optional): ID của lớp
- `semesterId` (optional): ID của học kỳ

### 3.1. Lấy tất cả thống kê students

```bash
curl --location 'http://localhost:8080/api/statistics/students' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 3.2. Lấy thống kê students theo khoa

```bash
curl --location 'http://localhost:8080/api/statistics/students?departmentId=1' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Student statistics retrieved successfully",
  "data": {
    "totalStudents": 5000,
    "countByDepartment": {
      "1": 1500,
      "2": 1200,
      "3": 1000
    },
    "countByClass": {},
    "topParticipants": [
      {
        "studentId": 123,
        "studentName": "Nguyễn Văn A",
        "studentCode": "SV001",
        "participationCount": 15
      },
      ...
    ],
    "inactiveStudents": [
      {
        "studentId": 456,
        "studentName": "Trần Thị B",
        "studentCode": "SV002",
        "departmentName": "Khoa CNTT"
      },
      ...
    ],
    "lowParticipationRateStudents": [
      {
        "studentId": 789,
        "studentName": "Lê Văn C",
        "studentCode": "SV003",
        "registrationCount": 10,
        "participationCount": 2,
        "participationRate": 0.2
      },
      ...
    ]
  }
}
```

---

## 4. SCORE STATISTICS

**API:** `GET /api/statistics/scores`

**Mô tả:** Thống kê về điểm số: phân bố điểm, top students, điểm trung bình theo khoa/lớp, histogram, etc.

**Quyền:** ADMIN, MANAGER, STUDENT (STUDENT chỉ xem điểm của mình)

**Query Parameters:**
- `scoreType` (optional): REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE
- `semesterId` (optional): ID của học kỳ
- `departmentId` (optional): ID của khoa
- `classId` (optional): ID của lớp

### 4.1. Admin/Manager xem tất cả thống kê scores

```bash
curl --location 'http://localhost:8080/api/statistics/scores' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 4.2. Xem thống kê scores theo loại điểm

```bash
curl --location 'http://localhost:8080/api/statistics/scores?scoreType=REN_LUYEN' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 4.3. Xem thống kê scores theo khoa

```bash
curl --location 'http://localhost:8080/api/statistics/scores?departmentId=1&scoreType=REN_LUYEN' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 4.4. Student xem điểm của mình

```bash
curl --location 'http://localhost:8080/api/statistics/scores' \
--header 'Authorization: Bearer {STUDENT_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Score statistics retrieved successfully",
  "data": {
    "statisticsByType": {
      "REN_LUYEN": {
        "scoreType": "REN_LUYEN",
        "averageScore": 75.5,
        "maxScore": 100.0,
        "minScore": 0.0,
        "totalStudents": 5000
      },
      "CONG_TAC_XA_HOI": {
        "scoreType": "CONG_TAC_XA_HOI",
        "averageScore": 60.0,
        "maxScore": 100.0,
        "minScore": 0.0,
        "totalStudents": 5000
      },
      "CHUYEN_DE": {
        "scoreType": "CHUYEN_DE",
        "averageScore": 5.0,
        "maxScore": 10.0,
        "minScore": 0.0,
        "totalStudents": 5000
      }
    },
    "topStudents": [
      {
        "studentId": 123,
        "studentName": "Nguyễn Văn A",
        "studentCode": "SV001",
        "scoreType": "REN_LUYEN",
        "score": 95.0,
        "semesterId": 1,
        "semesterName": "Học kỳ 1 - 2025"
      },
      ...
    ],
    "averageByDepartment": {
      "1": 78.5,
      "2": 72.0,
      "3": 70.0
    },
    "averageByClass": {},
    "averageBySemester": {
      "1": 75.5
    },
    "scoreDistribution": {
      "0-10": 50,
      "10-20": 100,
      "20-30": 200,
      "30-40": 300,
      "40-50": 400,
      "50-60": 500,
      "60-70": 600,
      "70-80": 800,
      "80-90": 1000,
      "90-100": 1050
    }
  }
}
```

---

## 5. SERIES STATISTICS

**API:** `GET /api/statistics/series`

**Mô tả:** Thống kê về chuỗi sự kiện: tổng số series, số sinh viên tham gia, tỷ lệ hoàn thành, milestone points, etc.

**Quyền:** ADMIN, MANAGER, STUDENT

**Query Parameters:**
- `seriesId` (optional): ID của series cụ thể
- `semesterId` (optional): ID của học kỳ

### 5.1. Lấy tất cả thống kê series

```bash
curl --location 'http://localhost:8080/api/statistics/series' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 5.2. Lấy thống kê series cụ thể

```bash
curl --location 'http://localhost:8080/api/statistics/series?seriesId=1' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Series statistics retrieved successfully",
  "data": {
    "totalSeries": 10,
    "seriesDetails": [
      {
        "seriesId": 1,
        "seriesName": "Chuỗi sự kiện mùa hè",
        "totalActivities": 5,
        "registeredStudents": 200,
        "completedStudents": 150,
        "completionRate": 0.75
      },
      ...
    ],
    "studentsPerSeries": {
      "1": 200,
      "2": 150,
      "3": 100
    },
    "milestonePointsAwarded": {
      "1": 0.0,
      "2": 0.0,
      "3": 0.0
    },
    "popularSeries": [
      {
        "seriesId": 1,
        "seriesName": "Chuỗi sự kiện mùa hè",
        "studentCount": 200,
        "totalActivities": 5
      },
      ...
    ]
  }
}
```

---

## 6. MINIGAME STATISTICS

**API:** `GET /api/statistics/minigames`

**Mô tả:** Thống kê về minigames: tổng số, tỷ lệ đạt/không đạt, điểm trung bình, minigame phổ biến nhất, etc.

**Quyền:** ADMIN, MANAGER, STUDENT

**Query Parameters:**
- `miniGameId` (optional): ID của minigame cụ thể
- `startDate` (optional): ISO 8601 format (yyyy-MM-ddTHH:mm:ss)
- `endDate` (optional): ISO 8601 format (yyyy-MM-ddTHH:mm:ss)

### 6.1. Lấy tất cả thống kê minigames

```bash
curl --location 'http://localhost:8080/api/statistics/minigames' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 6.2. Lấy thống kê minigame cụ thể

```bash
curl --location 'http://localhost:8080/api/statistics/minigames?miniGameId=1' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 6.3. Lấy thống kê minigames theo khoảng thời gian

```bash
curl --location 'http://localhost:8080/api/statistics/minigames?startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "MiniGame statistics retrieved successfully",
  "data": {
    "totalMiniGames": 25,
    "totalAttempts": 1000,
    "passedAttempts": 750,
    "failedAttempts": 250,
    "passRate": 0.75,
    "miniGameDetails": {
      "1": {
        "miniGameId": 1,
        "title": "Quiz kiến thức IT",
        "totalAttempts": 200,
        "passedAttempts": 150,
        "failedAttempts": 50,
        "passRate": 0.75,
        "averageScore": 8.5
      },
      ...
    },
    "popularMiniGames": [
      {
        "miniGameId": 1,
        "title": "Quiz kiến thức IT",
        "attemptCount": 200,
        "uniqueStudentCount": 150
      },
      ...
    ],
    "averageScoreByMiniGame": {
      "1": 8.5,
      "2": 7.0,
      "3": 9.0
    },
    "averageCorrectAnswersByMiniGame": {
      "1": 4.5,
      "2": 3.5,
      "3": 4.8
    }
  }
}
```

---

## 7. TIME-BASED STATISTICS

**API:** `GET /api/statistics/timeline`

**Mô tả:** Thống kê theo thời gian: xu hướng đăng ký, tham gia, điểm số theo thời gian, peak hours, etc.

**Quyền:** ADMIN, MANAGER only

**Query Parameters:**
- `startDate` (optional): ISO 8601 format (yyyy-MM-ddTHH:mm:ss)
- `endDate` (optional): ISO 8601 format (yyyy-MM-ddTHH:mm:ss)
- `groupBy` (optional): "day", "week", "month", "quarter", "year" (default: "day")

### 7.1. Lấy thống kê theo thời gian (mặc định: tháng hiện tại)

```bash
curl --location 'http://localhost:8080/api/statistics/timeline' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 7.2. Lấy thống kê theo khoảng thời gian cụ thể

```bash
curl --location 'http://localhost:8080/api/statistics/timeline?startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

### 7.3. Lấy thống kê theo tháng (groupBy=month)

```bash
curl --location 'http://localhost:8080/api/statistics/timeline?startDate=2025-01-01T00:00:00&endDate=2025-12-31T23:59:59&groupBy=month' \
--header 'Authorization: Bearer {ADMIN_TOKEN}'
```

**Response:**
```json
{
  "status": true,
  "message": "Time-based statistics retrieved successfully",
  "data": {
    "registrationsOverTime": [
      {
        "period": "2025-01",
        "count": 500,
        "value": null
      },
      ...
    ],
    "participationsOverTime": [
      {
        "period": "2025-01",
        "count": 450,
        "value": null
      },
      ...
    ],
    "scoresOverTime": [
      {
        "period": "2025-01",
        "count": null,
        "value": 75.5
      },
      ...
    ],
    "peakHours": {
      "8": 50,
      "9": 100,
      "10": 150,
      "14": 120,
      "15": 80
    },
    "groupBy": "month"
  }
}
```

---

## TÓM TẮT CÁC ENDPOINTS

| Endpoint | Method | Quyền | Mô tả |
|----------|--------|-------|-------|
| `/api/statistics/dashboard` | GET | ADMIN, MANAGER, STUDENT | Dashboard tổng quan |
| `/api/statistics/activities` | GET | ADMIN, MANAGER | Thống kê activities |
| `/api/statistics/students` | GET | ADMIN, MANAGER | Thống kê students |
| `/api/statistics/scores` | GET | ADMIN, MANAGER, STUDENT | Thống kê scores |
| `/api/statistics/series` | GET | ADMIN, MANAGER, STUDENT | Thống kê series |
| `/api/statistics/minigames` | GET | ADMIN, MANAGER, STUDENT | Thống kê minigames |
| `/api/statistics/timeline` | GET | ADMIN, MANAGER | Thống kê theo thời gian |

---

## LƯU Ý

1. **Role-based filtering:** Một số endpoints (dashboard, scores) có logic lọc dữ liệu dựa trên role trong controller. STUDENT chỉ xem được dữ liệu của chính mình.

2. **Date format:** Sử dụng ISO 8601 format: `yyyy-MM-ddTHH:mm:ss` (ví dụ: `2025-01-15T10:30:00`)

3. **Pagination:** Hiện tại các endpoints trả về top 5-10 items. Có thể mở rộng thêm pagination nếu cần.

4. **Performance:** Các queries thống kê có thể tốn thời gian với dữ liệu lớn. Có thể cân nhắc thêm caching hoặc background jobs để tính toán trước.

