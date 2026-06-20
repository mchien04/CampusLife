# Frontend Integration Plan: Score Engine & Architecture Refactor

## 1. Mục Tiêu & Phạm Vi Tích Hợp

Frontend cần tích hợp lại theo backend đã refactor thực tế, với các trọng tâm sau:

- **Cấu hình điểm theo `scoreRules`**: không còn dùng điểm tĩnh kiểu `maxPoints` hoặc `rewardPoints`.
- **Lịch sử điểm theo ledger**: hiển thị theo `ScoreEntry` và `sourceType`, không parse logic từ text.
- **Nộp bài tập có attachment typed**: phân biệt `image` và `file`.
- **Series milestone theo engine**: điểm mốc series là một nguồn riêng, không suy diễn từ activity con.
- **Upload URL theo backend config**: FE chỉ dùng URL backend trả về, không hardcode prefix public.

---

## 2. Những Thay Đổi FE Phải Chấp Nhận

### 2.1 Các Trường Không Còn Là Nguồn Chuẩn

- Không dùng `maxPoints`, `penaltyPointsIncomplete` ở activity.
- Không dùng `rewardPoints` ở minigame như nguồn điểm runtime.
- Không suy score history từ `reason`.
- Không giả định task submission chỉ có `fileUrls`.

### 2.2 Những Contract FE Phải Đồng Bộ

- `ActivityResponse` có `scoreRules`.
- `ScoreHistoryViewResponse` có `scoreHistories[]` và `activityParticipations[]`.
- `TaskSubmissionResponse` có `attachments`.
- `SubmitAttemptResponse` dùng `status` và `pointsEarned`, không có `passed` hoặc `attemptId`.
- Upload image API trả `{ status, message, data }`, không dùng wrapper chuẩn `{ status, message, body }`.

---

## 3. Enum & Types Cần Đồng Bộ Với Backend

### 3.1 Enum Chuẩn

```typescript
export type ScoreType = "REN_LUYEN" | "CONG_TAC_XA_HOI" | "CHUYEN_DE";

export type ScoreRuleTrigger =
  | "PARTICIPATION_COMPLETED"
  | "SUBMISSION_GRADED"
  | "MINIGAME_PASSED"
  | "SERIES_MILESTONE_REACHED";

export type ScoreRuleCalculation =
  | "FIXED_POINTS"
  | "COUNT_COMPLETION"
  | "PASS_FAIL_POINTS"
  | "PENALTY_POINTS"
  | "SERIES_MILESTONE";

export type ScoreRuleAudience =
  | "ALL_PARTICIPANTS"
  | "DEPARTMENT_ONLY"
  | "OUTSIDE_DEPARTMENTS_ONLY";

export type ScoreSemesterPolicy =
  | "ACTIVITY_SEMESTER"
  | "CURRENT_OPEN_SEMESTER"
  | "EXPLICIT_SEMESTER";

export type ScoreEntrySourceType =
  | "ACTIVITY_PARTICIPATION"
  | "TASK_SUBMISSION"
  | "MINIGAME_ATTEMPT"
  | "SERIES_PROGRESS"
  | "MANUAL_ADJUSTMENT"
  | "RECALCULATION";

export type ActivityType =
  | "SUKIEN"
  | "MINIGAME"
  | "CONG_TAC_XA_HOI"
  | "CHUYEN_DE_DOANH_NGHIEP";

export type SubmissionStatus =
  | "SUBMITTED"
  | "GRADED"
  | "RETURNED"
  | "LATE"
  | "MISSING";

export type ParticipationType =
  | "REGISTERED"
  | "CHECKED_IN"
  | "CHECKED_OUT"
  | "ATTENDED"
  | "COMPLETED";

export type MiniGameType = "QUIZ";
```

### 3.2 Types Cốt Lõi

```typescript
export interface ActivityScoreRuleRequest {
  scoreType: ScoreType;
  triggerType: ScoreRuleTrigger;
  calculation: ScoreRuleCalculation;
  points: string;
  failPoints?: string | null;
  audience: ScoreRuleAudience;
  semesterPolicy: ScoreSemesterPolicy;
  explicitSemesterId?: number | null;
  departmentIds?: number[];
  enabled?: boolean | null;
}

export interface ActivityScoreRuleResponse {
  id: number;
  activityId: number;
  scoreType: ScoreType;
  triggerType: ScoreRuleTrigger;
  calculation: ScoreRuleCalculation;
  points: string;
  failPoints?: string | null;
  audience: ScoreRuleAudience;
  semesterPolicy: ScoreSemesterPolicy;
  explicitSemesterId?: number | null;
  targetDepartmentIds: number[];
  enabled?: boolean | null;
}

export interface SubmissionAttachment {
  url: string;
  type: "file" | "image";
}
```

---

## 4. API Contracts Cần Sửa Theo Backend Thực Tế

### 4.1 Quản lý Activity

#### Nghiệp vụ

Tạo và cập nhật activity bằng `scoreRules`. Backend trả lại `ActivityResponse` trong `body`.

#### Endpoint chính

- `POST /api/activities`
- `PUT /api/activities/{id}`
- `GET /api/activities`
- `GET /api/activities/{id}`

#### Request tạo/cập nhật

```json
{
  "name": "Workshop A",
  "type": "SUKIEN",
  "description": "Mo ta",
  "startDate": "2026-06-21T08:00:00",
  "endDate": "2026-06-21T11:00:00",
  "requiresSubmission": false,
  "scoreRules": [
    {
      "scoreType": "REN_LUYEN",
      "triggerType": "PARTICIPATION_COMPLETED",
      "calculation": "FIXED_POINTS",
      "points": "5",
      "failPoints": "0",
      "audience": "ALL_PARTICIPANTS",
      "semesterPolicy": "CURRENT_OPEN_SEMESTER",
      "departmentIds": [],
      "enabled": true
    }
  ],
  "registrationStartDate": "2026-06-01T00:00:00",
  "registrationDeadline": "2026-06-20T23:59:59",
  "shareLink": "https://...",
  "isImportant": false,
  "isDraft": false,
  "bannerUrl": "https://api.example.com/uploads/banner.jpg",
  "location": "Hoi truong A",
  "ticketQuantity": 200,
  "benefits": "Cong diem",
  "requirements": "Mac dong phuc",
  "contactInfo": "Phong CTSV",
  "requiresApproval": false,
  "mandatoryForFacultyStudents": false,
  "organizerIds": [1, 2]
}
```

#### Notes

- `scoreRules: []` là activity không tính điểm.
- FE không dùng root field kiểu `maxPoints`.
- Một số endpoint activity phụ trả raw `List<ActivityResponse>`, không bọc `Response`.

### 4.2 Xem lịch sử điểm

#### Endpoint

- `GET /api/scores/history/student/{studentId}`

#### Query

- `semesterId`: **bắt buộc**
- `scoreType`: optional
- `page`: optional, default `0`
- `size`: optional, default `20`

#### Response thực tế

```json
{
  "status": true,
  "message": "success",
  "body": {
    "studentId": 1,
    "studentCode": "SV001",
    "studentName": "Nguyen Van A",
    "semesterId": 10,
    "semesterName": "HK1 2026",
    "scoreType": "REN_LUYEN",
    "currentScore": "15",
    "scoreHistories": [
      {
        "id": 101,
        "oldScore": "10",
        "newScore": "15",
        "changeDate": "2026-06-21T00:00:00",
        "reason": "Hoan thanh su kien",
        "activityId": 1,
        "activityName": "Su kien A",
        "seriesId": null,
        "seriesName": null,
        "sourceType": "ACTIVITY_PARTICIPATION",
        "changedByUsername": "system",
        "changedByFullName": "System"
      }
    ],
    "activityParticipations": [],
    "totalRecords": 1,
    "page": 0,
    "size": 20,
    "totalPages": 1
  }
}
```

#### Notes

- FE plan cũ mô tả `body.content[]` là không khớp.
- FE phải đọc `body.scoreHistories[]`.
- `sourceType` hiện đang là string enum thực tế từ ledger.

### 4.3 Task submission

#### Endpoint đúng

- `POST /api/submissions/task/{taskId}`
- `PUT /api/submissions/{submissionId}`
- `GET /api/submissions/task/{taskId}/my`
- `GET /api/submissions/{submissionId}`
- `GET /api/submissions/{submissionId}/files`
- `PUT /api/submissions/{submissionId}/grade`

#### Request đúng

- Dùng `multipart/form-data`
- Fields:
  - `content`
  - `files`
  - `images`

#### Response đúng

```json
{
  "status": true,
  "message": "success",
  "body": {
    "id": 1,
    "taskId": 11,
    "taskTitle": "Bao cao tong ket",
    "studentId": 100,
    "studentCode": "SV001",
    "studentName": "Nguyen Van A",
    "content": "Em nop bai",
    "fileUrls": [
      "https://api.example.com/uploads/submissions/a.pdf"
    ],
    "attachments": [
      {
        "url": "https://api.example.com/uploads/submissions/a.pdf",
        "type": "file"
      },
      {
        "url": "https://api.example.com/uploads/submissions/b.jpg",
        "type": "image"
      }
    ],
    "score": null,
    "isCompleted": null,
    "feedback": null,
    "graderId": null,
    "graderUsername": null,
    "status": "SUBMITTED",
    "submittedAt": "2026-06-21T10:00:00",
    "updatedAt": "2026-06-21T10:00:00",
    "gradedAt": null
  }
}
```

#### Notes

- FE plan cũ ghi `/api/tasks/submissions` là sai.
- FE plan cũ ghi `isApproved` là sai, field đúng là `isCompleted`.
- FE nên ưu tiên `attachments`, `fileUrls` chỉ dùng cho giai đoạn chuyển tiếp.

### 4.4 Submit minigame attempt

#### Endpoint

- `POST /api/minigames/attempts/{attemptId}/submit`

#### Request đúng

Backend hiện nhận `answers` dưới dạng object map:

```json
{
  "answers": {
    "1": 2,
    "2": 5,
    "3": 9
  }
}
```

Không dùng mảng:

```json
{
  "answers": [
    { "questionId": 1, "selectedOptionId": 2 }
  ]
}
```

#### Response đúng

```json
{
  "status": true,
  "message": "success",
  "body": {
    "id": 1,
    "status": "PASSED",
    "correctCount": 9,
    "totalQuestions": 10,
    "pointsEarned": "5",
    "startedAt": "2026-06-21T10:00:00",
    "submittedAt": "2026-06-21T10:03:00",
    "requiredCorrectAnswers": 8,
    "participation": {
      "id": 10
    }
  }
}
```

#### Notes

- Không có field `attemptId` trong response.
- Không có field `passed`; FE nên suy từ `status === "PASSED"`.
- `pointsEarned` là kết quả từ engine/ledger, không phải từ `rewardPoints`.

### 4.5 Upload image

#### Endpoint

- `POST /api/upload/image`
- `DELETE /api/upload/image?fileUrl=<url>`

#### Request

- `multipart/form-data`
- field duy nhất: `file`

#### Response thực tế

```json
{
  "status": true,
  "message": "File uploaded successfully",
  "data": "https://api.example.com/uploads/abc.jpg"
}
```

#### Notes

- Upload API không dùng wrapper `{ status, message, body }`.
- FE phải đọc `response.data`.
- Không hardcode prefix `/uploads`; chỉ dùng URL BE trả về.

---

## 5. Những Chỗ FE Plan Cũ Đã Được Sửa

- Sửa `Task Submission` path từ `/api/tasks/submissions` thành `/api/submissions/task/{taskId}`.
- Sửa `Task Submission` field từ `isApproved` thành `isCompleted`.
- Chốt `Task Submission` dùng `multipart/form-data`, không mô tả mơ hồ “json hoặc form-data”.
- Sửa `Score History` để phản ánh `body.scoreHistories[]` thay vì `body.content[]`.
- Sửa `Score History` để `semesterId` là bắt buộc.
- Sửa `Minigame submit` request từ array sang object map.
- Sửa `Minigame submit` response để bỏ `attemptId`, `passed`.
- Bổ sung `SERIES_MILESTONE_REACHED` trong `ScoreRuleTrigger`.
- Bổ sung đầy đủ `ScoreRuleCalculation` theo backend thực tế.

---

## 6. UI/UX Changes & Checklist

- [ ] **Màn Tạo Sự Kiện:** Dùng form array cho `scoreRules`, bỏ ô nhập điểm tĩnh.
- [ ] **Màn Danh sách/Chi tiết Sự Kiện:** Hiển thị `scoreRules` nếu cần giải thích cách cộng điểm.
- [ ] **Màn Tạo Minigame:** Không cho nhập `rewardPoints`.
- [ ] **Màn Submit Minigame:** Build payload `answers` dạng object map.
- [ ] **Màn Lịch sử điểm:** Đọc `scoreHistories` và map `sourceType` sang label UI.
- [ ] **Màn Nộp bài / Chi tiết bài nộp:** Ưu tiên `attachments`, render `image` bằng thumbnail và `file` bằng nút tải.
- [ ] **Màn Upload ảnh:** Lưu trực tiếp public URL backend trả về, không xử lý prefix local.

---

## 7. Khuyến Nghị Triển Khai

- Tách `api clients` theo domain: `activity`, `score`, `submission`, `minigame`, `upload`.
- Tạo `types` dùng chung cho enum score engine.
- Tách adapter cho các API không đồng nhất wrapper, ví dụ upload image trả `data` thay vì `body`.
- Trong giai đoạn chuyển tiếp, FE có thể giữ `fileUrls` làm fallback nhưng UI mới nên dùng `attachments`.
