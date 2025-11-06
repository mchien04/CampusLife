# ActivityController - Postman Endpoints

Base URL: `http://localhost:8080/api/activities`

**Lưu ý:** 
- Tất cả endpoint đều cần JWT token trong header (trừ GET public)
- Format: `Authorization: Bearer <token>`
- Content-Type: `application/json`

---

## 1. Tạo Activity (POST)
**Method:** `POST`  
**URL:** `/api/activities`  
**Auth:** ADMIN hoặc MANAGER  
**Body:**
```json
{
  "name": "Tên hoạt động",
  "type": "WORKSHOP",
  "scoreType": "REN_LUYEN",
  "description": "Mô tả hoạt động",
  "startDate": "2025-01-15T08:00:00",
  "endDate": "2025-01-15T17:00:00",
  "requiresSubmission": true,
  "maxPoints": 10.0,
  "penaltyPointsIncomplete": 2.0,
  "registrationStartDate": "2025-01-01T00:00:00",
  "registrationDeadline": "2025-01-10T23:59:59",
  "shareLink": "https://example.com",
  "isImportant": false,
  "isDraft": true,
  "bannerUrl": "https://example.com/banner.jpg",
  "location": "Phòng A101",
  "ticketQuantity": 100,
  "benefits": "Lợi ích tham gia",
  "requirements": "Yêu cầu tham gia",
  "contactInfo": "Email: contact@example.com",
  "requiresApproval": true,
  "mandatoryForFacultyStudents": false,
  "organizerIds": [1, 2, 3]
}
```

**Enum Values:**
- `type`: `WORKSHOP`, `SEMINAR`, `CONTEST`, `VOLUNTEER`, `OTHER`
- `scoreType`: `REN_LUYEN`, `CONG_TAC_XA_HOI`, `CHUYEN_DE`

---

## 2. Lấy tất cả Activities (GET)
**Method:** `GET`  
**URL:** `/api/activities`  
**Auth:** Public (nhưng có filter draft nếu không phải admin)  
**Query Params:** Không có  
**Response:** Danh sách activities (sinh viên không thấy draft)

---

## 3. Lấy Activity theo ID (GET)
**Method:** `GET`  
**URL:** `/api/activities/{id}`  
**Auth:** Public (nhưng có filter draft nếu không phải admin)  
**Path Variables:**
- `id`: Long (ví dụ: 1)

**Example:** `/api/activities/1`

---

## 4. Cập nhật Activity (PUT)
**Method:** `PUT`  
**URL:** `/api/activities/{id}`  
**Auth:** ADMIN hoặc MANAGER  
**Path Variables:**
- `id`: Long

**Body:** (Giống như POST, tất cả fields optional)
```json
{
  "name": "Tên hoạt động đã cập nhật",
  "isDraft": false,
  "requiresApproval": false
}
```

---

## 5. Xóa Activity (DELETE)
**Method:** `DELETE`  
**URL:** `/api/activities/{id}`  
**Auth:** ADMIN hoặc MANAGER  
**Path Variables:**
- `id`: Long

**Example:** `/api/activities/1`

---

## 6. Publish Activity (PUT)
**Method:** `PUT`  
**URL:** `/api/activities/{id}/publish`  
**Auth:** ADMIN hoặc MANAGER  
**Path Variables:**
- `id`: Long

**Description:** Chuyển activity từ draft sang published (isDraft = false)

**Example:** `/api/activities/1/publish`

---

## 7. Unpublish Activity (PUT)
**Method:** `PUT`  
**URL:** `/api/activities/{id}/unpublish`  
**Auth:** ADMIN hoặc MANAGER  
**Path Variables:**
- `id`: Long

**Description:** Chuyển activity từ published sang draft (isDraft = true)

**Example:** `/api/activities/1/unpublish`

---

## 8. Copy Activity (POST)
**Method:** `POST`  
**URL:** `/api/activities/{id}/copy`  
**Auth:** ADMIN hoặc MANAGER  
**Path Variables:**
- `id`: Long

**Query Params:**
- `offsetDays`: Integer (optional) - Số ngày offset cho dates mới

**Example:** `/api/activities/1/copy?offsetDays=30`

**Description:** Copy activity cùng với organizers và tasks, có thể offset dates

---

## 9. Lấy Activities theo ScoreType (GET)
**Method:** `GET`  
**URL:** `/api/activities/score-type/{scoreType}`  
**Auth:** Public  
**Path Variables:**
- `scoreType`: `REN_LUYEN`, `CONG_TAC_XA_HOI`, hoặc `CHUYEN_DE`

**Example:** `/api/activities/score-type/REN_LUYEN`

---

## 10. Lấy Activities theo Tháng (GET)
**Method:** `GET`  
**URL:** `/api/activities/month`  
**Auth:** Public  
**Query Params:**
- `year`: Integer (optional) - Mặc định: năm hiện tại
- `month`: Integer (optional) - Mặc định: tháng hiện tại

**Examples:**
- `/api/activities/month` (tháng hiện tại)
- `/api/activities/month?year=2025&month=1` (tháng 1/2025)

---

## 11. Lấy Activities theo Department (GET)
**Method:** `GET`  
**URL:** `/api/activities/department/{deptId}`  
**Auth:** Public  
**Path Variables:**
- `deptId`: Long

**Example:** `/api/activities/department/1`

---

## 12. Lấy Activities của User hiện tại (GET)
**Method:** `GET`  
**URL:** `/api/activities/my`  
**Auth:** Authenticated (bất kỳ role nào)  
**Description:** Lấy activities mà user hiện tại là organizer hoặc đã đăng ký

---

## 13. Kiểm tra Activity có yêu cầu nộp bài (GET)
**Method:** `GET`  
**URL:** `/api/activities/{activityId}/requires-submission`  
**Auth:** Public  
**Path Variables:**
- `activityId`: Long

**Example:** `/api/activities/1/requires-submission`

**Response:**
```json
{
  "status": true,
  "message": "Success",
  "data": {
    "requiresSubmission": true
  }
}
```

---

## 14. Kiểm tra trạng thái đăng ký của Student (GET)
**Method:** `GET`  
**URL:** `/api/activities/{activityId}/registration-status`  
**Auth:** Authenticated (bất kỳ role nào)  
**Path Variables:**
- `activityId`: Long

**Example:** `/api/activities/1/registration-status`

**Description:** Kiểm tra student hiện tại đã đăng ký activity chưa và trạng thái

---

## 15. Debug User Info (GET)
**Method:** `GET`  
**URL:** `/api/activities/debug/user-info`  
**Auth:** Authenticated (bất kỳ role nào)  
**Description:** Debug endpoint để xem thông tin user hiện tại

**Response:**
```json
{
  "status": true,
  "message": "User info retrieved",
  "data": {
    "username": "student123",
    "authorities": ["ROLE_STUDENT"],
    "isAuthenticated": true,
    "principal": "UsernamePasswordAuthenticationToken"
  }
}
```

---

## Postman Collection Setup

### Headers mặc định:
```
Authorization: Bearer <your-jwt-token>
Content-Type: application/json
```

### Environment Variables:
- `base_url`: `http://localhost:8080`
- `jwt_token`: `<your-jwt-token>`

### Ví dụ Request trong Postman:

**1. Tạo Activity:**
```
POST {{base_url}}/api/activities
Authorization: Bearer {{jwt_token}}
Content-Type: application/json

{
  "name": "Workshop Spring Boot",
  "type": "WORKSHOP",
  "scoreType": "REN_LUYEN",
  "startDate": "2025-02-01T09:00:00",
  "endDate": "2025-02-01T17:00:00",
  "isDraft": false,
  "requiresApproval": true
}
```

**2. Publish Activity:**
```
PUT {{base_url}}/api/activities/1/publish
Authorization: Bearer {{jwt_token}}
```

**3. Copy Activity:**
```
POST {{base_url}}/api/activities/1/copy?offsetDays=30
Authorization: Bearer {{jwt_token}}
```

**4. Lấy Activities theo tháng:**
```
GET {{base_url}}/api/activities/month?year=2025&month=2
```

---

## Lưu ý về Security:

- **Public (GET):** Không cần token, nhưng draft activities sẽ bị filter
- **Authenticated:** Cần token, bất kỳ role nào
- **ADMIN/MANAGER:** Cần token với role ADMIN hoặc MANAGER

## Lưu ý về Draft Activities:

- Draft activities (`isDraft = true`) chỉ visible cho ADMIN/MANAGER
- Sinh viên không thể đăng ký hoặc check-in draft activities
- Auto-registration sẽ skip draft activities

