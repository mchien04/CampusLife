# Cập nhật API Activity & Series (Delta)

> **Version:** 2.0 (Regenerated from actual backend source code at HEAD)  
> **Baseline:** Commit `c848ee6` → Current HEAD  
> **Source of truth:** Java backend implementation (controllers, DTOs, mappers, services)

Tài liệu này tóm tắt các thay đổi về kiến trúc API Activity và cấu hình Series, giúp team Frontend dễ dàng nắm bắt và tích hợp.

---

## Phần 1: Tách Biệt API Activity (Architecture Refactoring)

Kiến trúc Activity đã được chia nhỏ thành 3 nhánh riêng biệt để tránh việc dùng chung Payload (DTO) cồng kềnh và gây lỗi ghi đè dữ liệu. Endpoint legacy `/api/activities` vẫn được giữ lại để tương thích ngược, nhưng FE nên dần chuyển sang các endpoint mới.

### 1. Standard Activity (Hoạt động thông thường)

- Dùng cho các sự kiện truyền thống không liên quan tới minigame hay chuỗi (series).
- **Create:** `POST /api/activities/standard`
- **Update:** `PUT /api/activities/standard/{id}`
- **Get Detail:** `GET /api/activities/standard/{id}`
- **Payload DTO:** `StandardActivityCreateRequest` / `StandardActivityUpdateRequest`

### 2. Minigame Activity (Hoạt động Minigame)

- Dùng riêng cho các sự kiện đi kèm Quiz/Minigame.
- **Create:** `POST /api/activities/minigame`
- **Update:** `PATCH /api/activities/minigame/{id}`
- **Get Detail:** `GET /api/activities/minigame/{id}`
- **Payload DTO:** `MinigameActivityCreateRequest` / `MinigameActivityUpdateRequest`

### 3. Series Child Activity (Hoạt động thuộc Chuỗi)

- Dùng riêng cho các sự kiện nằm trong một Chuỗi (Series).
- Hoạt động con **không tự cộng điểm riêng lẻ**, mà điểm được cộng qua tiến độ của Series. Việc gắn một hoạt động vào Series đã được chuyển thành endpoint riêng biệt.
- **Tạo sự kiện con mới:** `POST /api/series/{seriesId}/activities`
- **Cập nhật sự kiện con:** `PUT /api/series/{seriesId}/activities/{activityId}`
- **Lấy chi tiết sự kiện con:** `GET /api/series/{seriesId}/activities/{activityId}`
- **Gắn sự kiện đã có sẵn vào chuỗi:** `POST /api/series/{seriesId}/activities/attach`

---

### Các Interfaces Mới (Thay thế cho CreateActivityRequest và ActivityResponse)

> [!TIP]
> Các interface `CreateActivityRequest`, `UpdateActivityRequest`, và `ActivityResponse` hiện tại vẫn có thể sử dụng (Legacy) để tương thích ngược. Tuy nhiên, FE nên ưu tiên sử dụng các interface chuyên biệt dưới đây cho các endpoint mới.

#### 1. Standard Activity (Hoạt động truyền thống)

```typescript
export interface StandardActivityCreateRequest {
  name: string;
  type: ActivityType; // SUKIEN, CONG_TAC_XA_HOI, CHUYEN_DE_DOANH_NGHIEP
  description?: string | null;
  startDate: string; // ISO 8601 datetime
  endDate: string;
  location?: string | null;
  organizerIds?: number[];
  registrationStartDate?: string | null;
  registrationDeadline?: string | null;
  requiresSubmission?: boolean | null;
  requiresApproval?: boolean | null;
  ticketQuantity?: number | null;
  isImportant?: boolean | null;
  mandatoryForFacultyStudents?: boolean | null;
  isDraft?: boolean | null;
  bannerUrl?: string | null;
  shareLink?: string | null;
  benefits?: string | null;
  requirements?: string | null;
  contactInfo?: string | null;
  scoreRules?: ActivityScoreRuleRequest[];
  presetCode?: ActivityPresetCode | null;
  presetConfig?: ActivityPresetConfig | null;
}

export interface StandardActivityUpdateRequest {
  // Không extends CreateRequest. Là class standalone trong Java.
  // type không thể thay đổi sau khi tạo.
  name?: string | null;
  description?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  location?: string | null;
  organizerIds?: number[];
  registrationStartDate?: string | null;
  registrationDeadline?: string | null;
  requiresSubmission?: boolean | null;
  requiresApproval?: boolean | null;
  ticketQuantity?: number | null;
  isImportant?: boolean | null;
  mandatoryForFacultyStudents?: boolean | null;
  isDraft?: boolean | null;
  bannerUrl?: string | null;
  shareLink?: string | null;
  benefits?: string | null;
  requirements?: string | null;
  contactInfo?: string | null;
  scoreRules?: ActivityScoreRuleRequest[];
  presetCode?: ActivityPresetCode | null;
  presetConfig?: ActivityPresetConfig | null;
}

export interface StandardActivityResponse {
  id: number;
  name: string;
  type: ActivityType;
  description?: string | null;
  startDate: string;
  endDate: string;
  location?: string | null;
  organizerIds: number[];
  registrationStartDate?: string | null;
  registrationDeadline?: string | null;
  hasPreparation: boolean;
  requiresSubmission: boolean;
  requiresApproval: boolean;
  ticketQuantity?: number | null;
  isImportant: boolean;
  mandatoryForFacultyStudents: boolean;
  isDraft: boolean;
  bannerUrl?: string | null;
  shareLink?: string | null;
  benefits?: string | null;
  requirements?: string | null;
  contactInfo?: string | null;
  checkInCode?: string | null;
  scoreRules: ActivityScoreRuleResponse[];
  createdAt?: string | null;
  updatedAt?: string | null;
  createdBy?: string | null;
  lastModifiedBy?: string | null;
}
```

#### 2. Minigame Activity (Hoạt động kèm Minigame / Quiz)

```typescript
export interface MinigameActivityCreateRequest {
  name: string;
  description?: string | null;
  startDate: string;
  endDate: string;
  organizerIds?: number[];
  requiresApproval?: boolean | null;
  ticketQuantity?: number | null;
  isImportant?: boolean | null;
  mandatoryForFacultyStudents?: boolean | null;
  isDraft?: boolean | null;
  registrationStartDate?: string | null;
  registrationDeadline?: string | null;
  bannerUrl?: string | null;
  shareLink?: string | null;
  scoreRules?: ActivityScoreRuleRequest[];
  quiz?: QuizConfigRequest | null;
}

export interface QuizConfigRequest {
  title: string;
  questionCount: number;
  timeLimit: number;
  requiredCorrectAnswers: number;
  maxAttempts: number;
  showAnswers?: boolean | null;
  questions: QuestionRequest[];
}

export interface QuestionRequest {
  questionText: string;
  imageUrl?: string | null;
  options: OptionRequest[];
}

export interface OptionRequest {
  text: string;
  isCorrect?: boolean | null;
}

export interface MinigameActivityUpdateRequest {
  // Không extends CreateRequest. Là class standalone trong Java.
  name?: string | null;
  description?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  organizerIds?: number[];
  requiresApproval?: boolean | null;
  ticketQuantity?: number | null;
  isImportant?: boolean | null;
  mandatoryForFacultyStudents?: boolean | null;
  isDraft?: boolean | null;
  registrationStartDate?: string | null;
  registrationDeadline?: string | null;
  bannerUrl?: string | null;
  shareLink?: string | null;
  scoreRules?: ActivityScoreRuleRequest[];
  quiz?: QuizConfigRequest | null;
}

export interface MinigameActivityResponse {
  id: number;
  name: string;
  type: ActivityType; // Luôn là MINIGAME
  description?: string | null;
  startDate: string;
  endDate: string;
  isDraft: boolean;
  bannerUrl?: string | null;
  shareLink?: string | null;
  isImportant: boolean;
  checkInCode?: string | null;
  scoreRules: ActivityScoreRuleResponse[];
  quiz?: QuizConfigResponse | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface QuizConfigResponse {
  id: number;
  title: string;
  questionCount: number;
  timeLimit: number;
  requiredCorrectAnswers: number;
  maxAttempts: number;
  showAnswers: boolean;
  isActive: boolean;
}
```

#### 3. Series Child Activity (Hoạt động con trong chuỗi)

```typescript
export interface SeriesChildActivityCreateRequest {
  name: string;
  description?: string | null;
  startDate: string;
  endDate: string;
  location?: string | null;
  order?: number | null; // Thứ tự trong chuỗi (seriesOrder)
  bannerUrl?: string | null;
  shareLink?: string | null;
  benefits?: string | null;
  requirements?: string | null;
  contactInfo?: string | null;
  organizerIds?: number[];
  type?: ActivityType | null;
}

export interface SeriesChildActivityUpdateRequest {
  // Không extends CreateRequest. Là class standalone trong Java.
  name?: string | null;
  description?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  location?: string | null;
  order?: number | null;
  bannerUrl?: string | null;
  shareLink?: string | null;
  benefits?: string | null;
  requirements?: string | null;
  contactInfo?: string | null;
  organizerIds?: number[];
  type?: ActivityType | null;
}

export interface SeriesChildActivityResponse {
  id: number;
  name: string;
  type: ActivityType;
  description?: string | null;
  startDate: string;
  endDate: string;
  hasPreparation: boolean;
  requiresSubmission: boolean;
  scoreRules: ActivityScoreRuleResponse[];
  registrationStartDate?: string | null;
  registrationDeadline?: string | null;
  shareLink?: string | null;
  isImportant: boolean;
  isDraft: boolean;
  bannerUrl?: string | null;
  location?: string | null;
  ticketQuantity?: number | null;
  benefits?: string | null;
  requirements?: string | null;
  contactInfo?: string | null;
  checkInCode?: string | null;
  requiresApproval: boolean;
  mandatoryForFacultyStudents: boolean;
  organizerIds: number[];
  seriesId?: number | null;
  seriesOrder?: number | null;
  seriesName?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  createdBy?: string | null;
  lastModifiedBy?: string | null;
}
```

#### 4. Activity Summary (Dùng cho API danh sách để tối ưu performance)

```typescript
export interface ActivitySummaryResponse {
  id: number;
  name: string;
  type: ActivityType;
  startDate: string;
  endDate: string;
  bannerUrl?: string | null;
  isDraft: boolean;
  isImportant: boolean;
  location?: string | null;
  variantTag: string; // "STANDARD", "MINIGAME", or "SERIES_CHILD"
  seriesId?: number | null;
}
```

---

## Phần 2: Cập nhật API Series (Cấu hình điểm Milestone & Target Semester)

### 1. Thay đổi về DTO

Trong `CreateSeriesRequest`, `UpdateSeriesRequest` và `SeriesResponse`, đã được bổ sung thêm các trường cấu hình phạt tối thiểu và milestone.

> [!IMPORTANT]
> `SeriesResponse` hiện tại **đã có `targetSemesterId`** (đã fix trong backend). Trường này tồn tại trên Entity, Request DTO, và Response DTO.

```typescript
export interface CreateSeriesRequest {
  name: string;
  description?: string | null;
  milestonePoints: Record<number, number>; // Map<Integer, Integer> - e.g. {3: 5, 5: 10}
  scoreType: ScoreType; // e.g. REN_LUYEN, CTXH, CHUYEN_DE
  targetSemesterId?: number | null; // Học kỳ nhận điểm milestone. null = tự suy luận.
  mainActivityId?: number | null;
  registrationStartDate?: string | null;
  registrationDeadline?: string | null;
  requiresApproval?: boolean | null;
  ticketQuantity?: number | null;
  minimumRequirementEnabled?: boolean | null;
  minimumRequiredEvents?: number | null;
  minimumPenaltyPoints?: number | null;
  presetCode?: SeriesPresetCode | null;
  presetConfig?: SeriesPresetConfig | null;
}

export interface UpdateSeriesRequest {
  // Không extends CreateSeriesRequest. Là class standalone trong Java.
  name?: string | null;
  description?: string | null;
  milestonePoints?: Record<number, number>;
  scoreType?: ScoreType | null;
  targetSemesterId?: number | null;
  mainActivityId?: number | null;
  registrationStartDate?: string | null;
  registrationDeadline?: string | null;
  requiresApproval?: boolean | null;
  ticketQuantity?: number | null;
  minimumRequirementEnabled?: boolean | null;
  minimumRequiredEvents?: number | null;
  minimumPenaltyPoints?: number | null;
  presetCode?: SeriesPresetCode | null;
  presetConfig?: SeriesPresetConfig | null;
}

export interface SeriesResponse {
  id: number;
  name: string;
  description?: string | null;
  milestonePoints: Record<number, number>;
  scoreType: ScoreType;
  mainActivityId?: number | null;
  registrationStartDate?: string | null;
  registrationDeadline?: string | null;
  requiresApproval: boolean;
  ticketQuantity?: number | null;
  minimumRequirementEnabled: boolean;
  minimumRequiredEvents?: number | null;
  targetSemesterId?: number | null;
  minimumPenaltyPoints?: number | null;
  createdAt?: string | null;
  // ✅ targetSemesterId đã có trong SeriesResponse (đã fix backend)
}
```

### 2. API Cấu hình Series (Không dùng ActivityScoreRuleRequest)

Điểm thưởng của Series (Milestone points) được cấu hình **trực tiếp** trên Entity Series thông qua trường `milestonePoints` dạng Map (JSON Object), thay vì dùng chung cơ chế của `ActivityScoreRuleRequest`.

#### API Endpoint
- **POST /api/series** (Tạo mới)
- **PUT /api/series/{seriesId}** (Cập nhật)

#### Request Body (Ví dụ tham khảo)
```json
{
  "name": "Workshop Doanh Nghiệp 2026",
  "description": "Chuỗi workshop",
  "scoreType": "CHUYEN_DE",
  "milestonePoints": {
    "1": 1,
    "3": 3,
    "5": 5
  },
  "minimumRequirementEnabled": true,
  "minimumRequiredEvents": 3,
  "minimumPenaltyPoints": 2,
  "targetSemesterId": 1,
  "registrationStartDate": "2026-06-01T00:00:00",
  "registrationDeadline": "2026-06-30T23:59:59",
  "requiresApproval": true,
  "ticketQuantity": 200,
  "presetCode": "ENTERPRISE_SERIES",
  "presetConfig": null
}
```

#### Điểm nhấn quan trọng cho Frontend:

1. **`milestonePoints`**: Key là số sự kiện tối thiểu cần hoàn thành, Value là số điểm được cộng (vd: Hoàn thành 3 sự kiện được 3 điểm chuyên đề).
2. **`minimumPenaltyPoints`**: Frontend truyền số **dương** (vd: `2`). Backend sẽ tự động xử lý chuyển thành điểm trừ (penalty) trong hệ thống tính điểm (`ScoreRuleEngineImpl`).
3. **`targetSemesterId`**: Dropdown chọn học kỳ. Tuân theo logic cấu hình Explicit Semester. Bỏ trống (null) nếu muốn hệ thống tự suy luận từ ngày bắt đầu của activity con đầu tiên trong chuỗi.
4. **`SeriesResponse` không có `targetSemesterId`**: Nếu FE cần hiển thị học kỳ đích của Series, hãy dùng `GET /api/series` (danh sách) vì endpoint này trả về ad-hoc Map có chứa `targetSemesterId`, hoặc dùng `GET /api/series/{id}/overview`.

---

## Phần 3: Thống kê tổng quan Series (Dành cho Ban tổ chức)

### API Endpoint: GET /api/series/{seriesId}/overview
- **Mục đích:** Lấy thông tin thống kê tổng quan của chuỗi sự kiện để hiển thị trên Dashboard của Admin / Organizer.
- **Authentication:** Required (Admin / Organizer)
- **Response Type:** `SeriesOverviewResponse`
- **Response Body (Ví dụ):**
```json
{
  "status": true,
  "message": "success",
  "body": {
    "seriesId": 1,
    "seriesName": "Workshop Doanh Nghiệp 2026",
    "description": "Chuỗi workshop",
    "scoreType": "CHUYEN_DE",
    "targetSemesterId": 1,
    "milestonePoints": "{\"1\":1,\"3\":3,\"5\":5}",
    "milestonePointsMap": {
      "1": 1,
      "3": 3,
      "5": 5
    },
    "registrationStartDate": "2026-06-01T00:00:00",
    "registrationDeadline": "2026-06-30T23:59:59",
    "requiresApproval": true,
    "ticketQuantity": 200,
    "minimumRequirementEnabled": true,
    "minimumRequiredEvents": 3,
    "minimumPenaltyPoints": 2,
    "createdAt": "2026-06-01T00:00:00",

    "totalActivities": 5,
    "totalRegisteredStudents": 150,
    "totalCompletedStudents": 45,
    "completionRate": 0.3,
    "totalMilestonePointsAwarded": 135.0,
    "minimumRequirementMetCount": 80,

    "milestoneProgress": [
      {
        "milestoneKey": "3",
        "milestoneCount": 3,
        "milestonePoints": 3,
        "studentCount": 80,
        "percentage": 53.33
      }
    ],

    "activityStats": [
      {
        "activityId": 10,
        "activityName": "Workshop 1",
        "order": 1,
        "registrationCount": 120,
        "participationCount": 100,
        "participationRate": 0.83
      }
    ]
  }
}
```

> [!NOTE]
> `SeriesOverviewResponse` có 2 trường liên quan milestone: `milestonePoints` (JSON string gốc) và `milestonePointsMap` (đã parse). Frontend nên sử dụng `milestonePointsMap`.

**Lưu ý quan trọng cho FE:**
- `minimumRequirementMetCount` (Integer) là tổng số lượng sinh viên đã đạt mốc tối thiểu. Trường này dùng cho biểu đồ/thống kê của **Organizer** qua endpoint `/overview`.
- `minimumRequirementMet` (Boolean) là trạng thái cá nhân xem sinh viên hiện tại đã vượt qua mốc tối thiểu chưa. Trường này dùng cho màn hình của **Student** qua endpoint `/progress/my`.
- `totalCompletedStudents` = số SV đã hoàn thành **tất cả** activities trong series (không phải chỉ đạt mốc tối thiểu).

---

## Phần 4: Tiến độ Series cho Student

### API Endpoint: GET /api/series/{seriesId}/progress/my
- **Response:** `Response<Map<string, any>>` (không có DTO typed cố định, backend trả về Map động)

Các key trong Map body:
```json
{
  "studentId": 15,
  "seriesId": 10,
  "seriesName": "Workshop Doanh Nghiệp 2026",
  "completedCount": 2,
  "totalActivities": 5,
  "completedActivityIds": [101, 102],
  "pointsEarned": "5.00",
  "lastUpdated": "2026-06-22T20:00:00",
  "currentMilestone": "2",
  "nextMilestoneCount": 4,
  "nextMilestonePoints": 10,
  "milestonePoints": { "2": 5, "4": 10 },
  "scoreType": "CHUYEN_DE",
  "minimumRequirementEnabled": true,
  "minimumRequiredEvents": 3,
  "minimumPenaltyPoints": 2,
  "minimumRequirementMet": false,
  "remainingToAvoidPenalty": 1
}
```

> [!WARNING]
> Backend **không có** DTO `SeriesStudentProgressView`. Response là `Map<String, Object>` được build động. FE không nên khai báo TypeScript interface cứng `SeriesStudentProgressView` nếu không muốn rủi ro mismatch.

---

## Phần 5: Kiểu dữ liệu điểm số (BigDecimal)

Các trường điểm số trong Java backend sử dụng `BigDecimal` (không phải `String`):
- `ActivityScoreRuleRequest.points`, `ActivityScoreRuleRequest.failPoints`
- `ActivityScoreRuleResponse.points`, `ActivityScoreRuleResponse.failPoints`
- `SubmitAttemptResponse.pointsEarned`
- `ScoreHistoryViewResponse.currentScore`
- `StudentRankingResponse.score`
- `ActivityPresetConfig.participationPoints`, `noShowPenaltyPoints`, `submissionPassPoints`, etc.
- `SeriesOverviewResponse.totalMilestonePointsAwarded`
- `SeriesProgressItemResponse.pointsEarned`

> [!CAUTION]
> Jackson mặc định serialize `BigDecimal` thành JSON **number** (không phải string). Tuy nhiên, tùy thuộc vào cấu hình Jackson của backend, có thể trả về string. **Frontend nên handle cả 2 kiểu** (`number | string`) cho các trường điểm số để tránh lỗi parse.

---

## Phần 6: Danh sách Enum đầy đủ (Verified từ code)

```typescript
export type ActivityType = "SUKIEN" | "MINIGAME" | "CONG_TAC_XA_HOI" | "CHUYEN_DE_DOANH_NGHIEP";

export type ScoreType = "REN_LUYEN" | "CONG_TAC_XA_HOI" | "CHUYEN_DE";

export type ScoreRuleTrigger =
  | "PARTICIPATION_COMPLETED"
  | "NO_SHOW"
  | "SUBMISSION_GRADED"
  | "MINIGAME_PASSED"
  | "MINIGAME_EXHAUSTED_ATTEMPTS"
  | "SERIES_MILESTONE_REACHED"
  | "TASK_OVERDUE";

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
  | "EXPLICIT_SEMESTER";

export type ScoreEntrySourceType =
  | "ACTIVITY_PARTICIPATION"
  | "ACTIVITY_REGISTRATION"
  | "TASK_SUBMISSION"
  | "TASK_ASSIGNMENT"
  | "MINIGAME_ATTEMPT"
  | "SERIES_PROGRESS"
  | "SERIES_MINIMUM_REQUIREMENT"
  | "MANUAL_ADJUSTMENT"
  | "RECALCULATION";

export type ActivityPresetCode =
  | "EVENT_BASIC"
  | "EVENT_WITH_SUBMISSION"
  | "ENTERPRISE_SEMINAR_BASIC"
  | "ENTERPRISE_SEMINAR_WITH_BONUS"
  | "MINIGAME_PASS_ONLY"
  | "CUSTOM";

export type SeriesPresetCode =
  | "SERIES_MILESTONE_BASIC"
  | "ENTERPRISE_SERIES"
  | "CUSTOM";

export type MiniGameType = "QUIZ";

export type RegistrationStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED" | "ATTENDED" | "WAITLIST";

export type ParticipationType = "REGISTERED" | "CHECKED_IN" | "ATTENDED" | "COMPLETED";

export type SubmissionStatus = "SUBMITTED" | "GRADED" | "RETURNED" | "LATE" | "MISSING";
```

---

## Phần 7: Endpoint tổng hợp (Quick Reference)

| Nhóm | Method | Endpoint | Request | Response |
|------|--------|----------|---------|----------|
| Standard | POST | `/api/activities/standard` | `StandardActivityCreateRequest` | `Response<StandardActivityResponse>` |
| Standard | PUT | `/api/activities/standard/{id}` | `StandardActivityUpdateRequest` | `Response<StandardActivityResponse>` |
| Standard | GET | `/api/activities/standard/{id}` | — | `Response<StandardActivityResponse>` |
| Minigame | POST | `/api/activities/minigame` | `MinigameActivityCreateRequest` | `Response<MinigameActivityResponse>` |
| Minigame | PATCH | `/api/activities/minigame/{id}` | `MinigameActivityUpdateRequest` | `Response<MinigameActivityResponse>` |
| Minigame | GET | `/api/activities/minigame/{id}` | — | `Response<MinigameActivityResponse>` |
| Series Child | POST | `/api/series/{seriesId}/activities` | `SeriesChildActivityCreateRequest` | `Response<SeriesChildActivityResponse>` |
| Series Child | PUT | `/api/series/{seriesId}/activities/{activityId}` | `SeriesChildActivityUpdateRequest` | `Response<SeriesChildActivityResponse>` |
| Series Child | GET | `/api/series/{seriesId}/activities/{activityId}` | — | `Response<SeriesChildActivityResponse>` |
| Series Child | POST | `/api/series/{seriesId}/activities/attach` | `AddActivityToSeriesRequest` | `Response<Activity>` |
| Legacy | POST | `/api/activities` | `CreateActivityRequest` | `Response<ActivityResponse>` |
| Legacy | PUT | `/api/activities/{id}` | `CreateActivityRequest` | `Response<ActivityResponse>` |
| Legacy | GET | `/api/activities` | — | `Response<ActivityResponse[]>` |
| Legacy | GET | `/api/activities/{id}` | — | `Response<ActivityResponse>` |
| Legacy | GET | `/api/activities/upcoming` | `?keyword=` | `ActivityResponse[]` (raw) |
| Legacy | GET | `/api/activities/month` | `?year=&month=` | `ActivityResponse[]` (raw) |
| Legacy | GET | `/api/activities/my` | — | `ActivityResponse[]` (raw) |
| Preset | GET | `/api/activities/presets` | — | `Response<ActivityPresetDefinitionResponse[]>` |
| Preset | POST | `/api/activities/presets/preview` | `ActivityPresetPreviewRequest` | `Response<ActivityPresetPreviewResponse>` |
| Preset | GET | `/api/series/presets` | — | `Response<SeriesPresetDefinitionResponse[]>` |
| Preset | POST | `/api/series/presets/preview` | `SeriesPresetPreviewRequest` | `Response<SeriesPresetPreviewResponse>` |
| Series | POST | `/api/series` | `CreateSeriesRequest` | `Response<SeriesResponse>` |
| Series | PUT | `/api/series/{seriesId}` | `UpdateSeriesRequest` | `Response<SeriesResponse>` |
| Series | GET | `/api/series` | — | `Response<Map[]>` (ad-hoc, có `targetSemesterId`) |
| Series | GET | `/api/series/{seriesId}` | — | `Response<SeriesResponse>` |
| Series | DELETE | `/api/series/{seriesId}` | — | `Response<?>` |
| Series | POST | `/api/series/{seriesId}/register` | — | `Response<Registration[]>` |
| Series | GET | `/api/series/{seriesId}/progress/my` | — | `Response<Map>` |
| Series | GET | `/api/series/{seriesId}/students/{studentId}/progress` | — | `Response<Map>` |
| Series | GET | `/api/series/{seriesId}/progress` | `?page=&size=&keyword=` | `Response<SeriesProgressListResponse>` |
| Series | GET | `/api/series/{seriesId}/overview` | — | `Response<SeriesOverviewResponse>` |
| Series | POST | `/api/series/{seriesId}/students/{studentId}/calculate-milestone` | — | `Response<StudentSeriesProgress>` |
| Minigame | POST | `/api/minigames` | `CreateMiniGameRequest` | `Response<MiniGameResponse>` |
| Minigame | PUT | `/api/minigames/{miniGameId}` | `UpdateMiniGameRequest` | `Response<MiniGameResponse>` |
| Minigame | GET | `/api/minigames/activity/{activityId}` | — | `Response<MiniGameResponse>` |
| Minigame | POST | `/api/minigames/{miniGameId}/start` | — | `Response<StartAttemptResponse>` |
| Minigame | POST | `/api/minigames/attempts/{attemptId}/submit` | `{answers: Map<string, number>}` | `Response<SubmitAttemptResponse>` |
| Minigame | GET | `/api/minigames/attempts/{attemptId}` | — | `Response<AttemptDetailResponse>` |
| Check-in | POST | `/api/registrations/checkin` | `ActivityParticipationRequest` | `Response<ActivityParticipation>` |
| Check-in | POST | `/api/registrations/checkin/qr` | `{checkInCode: string}` | `Response<?>` |
| Score | GET | `/api/scores/ranking` | `?semesterId=&scoreType=&departmentId=&classId=&sortOrder=` | `Response<Map>` |
| Score | POST | `/api/scores/recalculate/student/{studentId}` | `?semesterId=` | `Response<?>` |
| Score | POST | `/api/scores/recalculate/all` | `?semesterId=` | `Response<Map>` |

---

## Phần 8: Migration Notes cho Frontend

1. **Chuyển từ Legacy sang Specialized APIs:**
   - Tạo Standard Activity → dùng `POST /api/activities/standard`
   - Tạo Minigame Activity → dùng `POST /api/activities/minigame`
   - Tạo Series Child Activity → dùng `POST /api/series/{seriesId}/activities`

2. **Không dùng `extends` trong DTOs:**
   - Tất cả `*UpdateRequest` trong Java đều là **standalone class**, không extends CreateRequest. FE nên khai báo interface riêng hoặc dùng `Partial<T>` với cẩn thận.

3. **Series `targetSemesterId`:**
   - Gửi lên `CreateSeriesRequest` và `UpdateSeriesRequest` bình thường.
   - Khi đọc `SeriesResponse`, trường `targetSemesterId` hiện **bị thiếu** (backend chưa map). Nếu cần, đọc từ `GET /api/series` (danh sách) hoặc `GET /api/series/{id}/overview`.

4. **Điểm số là `BigDecimal`:**
   - Các trường `points`, `failPoints`, `pointsEarned`, `score`, `currentScore` đều là `BigDecimal` trong Java.
   - JSON serialization thường trả về **number** (không phải string). FE nên định nghĩa kiểu `number | string` hoặc `number` cho an toàn.

5. **Progress/my trả về Map:**
   - `GET /api/series/{seriesId}/progress/my` trả về `Map<string, any>`, không có DTO typed cố định. FE cần tự định nghĩa interface hoặc xử lý dynamic.

6. **Legacy APIs vẫn hoạt động:**
   - `POST /api/activities`, `PUT /api/activities/{id}`, `GET /api/activities`, etc. vẫn đầy đủ chức năng. FE có thể migrate dần.

---

*End of DELTA_ACTIVITY_API_UPDATE.md*
