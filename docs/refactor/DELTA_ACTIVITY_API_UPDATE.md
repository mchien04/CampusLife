
# Cập nhật API Activity & Series (Delta)

Tài liệu này tóm tắt các thay đổi mới nhất về kiến trúc API Activity và cấu hình Series, giúp team Frontend dễ dàng nắm bắt và tích hợp mà không cần đọc lại toàn bộ spec.

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
  startDate: string;
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

export interface StandardActivityUpdateRequest extends Omit<StandardActivityCreateRequest, 'type'> {
  // type is omitted because it cannot be changed after creation
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
  quiz?: {
    title: string;
    questionCount: number;
    timeLimit: number;
    requiredCorrectAnswers: number;
    maxAttempts: number;
    showAnswers: boolean;
    questions: QuestionRequest[]; // Reuses existing Quiz Question Request
  } | null;
}

export interface MinigameActivityUpdateRequest extends MinigameActivityCreateRequest {}

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
  quiz?: {
    id: number;
    title: string;
    questionCount: number;
    timeLimit: number;
    requiredCorrectAnswers: number;
    maxAttempts: number;
    showAnswers: boolean;
    isActive: boolean;
  } | null;
  createdAt?: string | null;
  updatedAt?: string | null;
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
  type: ActivityType;
}

export interface SeriesChildActivityUpdateRequest extends SeriesChildActivityCreateRequest {}

export interface SeriesChildActivityResponse {
  id: number;
  name: string;
  type: ActivityType;
  description?: string | null;
  startDate: string;
  endDate: string;
  hasPreparation: boolean;
  requiresSubmission: boolean; // Mặc định là false cho Series Child
  scoreRules: ActivityScoreRuleResponse[]; // Rỗng hoặc kế thừa
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
  seriesName?: string | null; // Thêm tên của series để dễ bề render
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


## Phần 2: Cập nhật API Series (Cấu hình điểm Milestone & Target Semester)

### 1. Thay đổi về DTO

Trong `CreateSeriesRequest`, `UpdateSeriesRequest` và `SeriesResponse`, đã được bổ sung thêm một trường mới:
- `targetSemesterId?: number | null;`

**Mục đích:** Admin có thể cấu hình trước học kỳ nào sẽ được dùng để cộng điểm thưởng (milestone) cho chuỗi sự kiện. Nếu gửi lên `null`, backend sẽ tự động tính toán học kỳ dựa trên thời gian diễn ra sự kiện đầu tiên của chuỗi.

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
3. **`targetSemesterId`**: Dropdown chọn học kỳ. Tuân theo logic cấu hình Explicit Semester. Bỏ trống (null) nếu muốn hệ thống tự suy luận.


---

## Phần 3: Thống kê tổng quan Series (Dành cho Ban tổ chức)

FE đang thắc mắc về trường `minimumRequirementMetCount` (Not in BE spec or FE). Đây là field thuộc về API Thống kê **dành cho Ban tổ chức (Organizer)**, chưa được ghi nhận trong spec. Trong khi đó, `minimumRequirementMet` (boolean), `completedCount` và `remainingToAvoidPenalty` thuộc về API Tiến độ **dành cho Sinh viên (Student)** (`GET /api/series/{seriesId}/progress/my`).

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
    "minimumRequirementEnabled": true,
    "minimumRequiredEvents": 3,
    "minimumPenaltyPoints": 2,

    // Statistics (Thống kê tổng quan)
    "totalActivities": 5,
    "totalRegisteredStudents": 150,
    "totalCompletedStudents": 45,
    "completionRate": 0.3,
    "totalMilestonePointsAwarded": 135.0,
    "minimumRequirementMetCount": 80, // TỔNG SỐ LƯỢNG SINH VIÊN ĐÃ ĐẠT SỐ SỰ KIỆN TỐI THIỂU

    // Phân bố tiến độ theo milestone
    "milestoneProgress": [
      {
        "milestoneKey": "3",
        "milestoneCount": 3,
        "milestonePoints": 3,
        "studentCount": 80,
        "percentage": 0.53
      }
    ],

    // Thống kê từng hoạt động con
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

**Lưu ý quan trọng cho FE:**
- `minimumRequirementMetCount` (Integer) là tổng số lượng sinh viên đã đạt mốc tối thiểu. Trường này dùng cho biểu đồ/thống kê của **Organizer** qua endpoint `/overview`.
- `minimumRequirementMet` (Boolean) là trạng thái cá nhân xem sinh viên hiện tại đã vượt qua mốc tối thiểu chưa. Trường này dùng cho màn hình của **Student** qua endpoint `/progress/my`.
- FE có thể đã nhầm lẫn khi áp dụng góc nhìn của Student cho UI của Organizer. Nếu màn hình của Organizer cần hiển thị tổng số người đạt chuẩn thì **phải sử dụng** `minimumRequirementMetCount` từ endpoint `/overview` này.



### Các Interfaces Mới Của Series

```typescript
export interface CreateSeriesRequest {
  name: string;
  description?: string | null;
  milestonePoints: Record<number, number>; // Map<Integer, Integer> - e.g. {3: 5, 5: 10}
  scoreType: ScoreType; // e.g. REN_LUYEN, CTXH
  targetSemesterId?: number | null;
  mainActivityId?: number | null;
  registrationStartDate?: string | null;
  registrationDeadline?: string | null;
  requiresApproval?: boolean | null;
  ticketQuantity?: number | null;
  minimumRequirementEnabled?: boolean | null;
  minimumRequiredEvents?: number | null;
  minimumPenaltyPoints?: number | null;
  presetCode?: string | null;
  presetConfig?: any | null;
}

export interface UpdateSeriesRequest extends CreateSeriesRequest {}

export interface SeriesOverviewResponse {
  id: number;
  name: string;
  description?: string | null;
  targetSemesterId?: number | null;
  targetSemesterName?: string | null;
  scoreType: ScoreType;
  mainActivityId?: number | null;
  
  activities: any[]; // SeriesChildActivityResponse[]
  milestonePoints: Record<number, number>;
  
  // Organizer stats
  totalRegistered: number;
  totalCompletedAtLeastOne: number;
  minimumRequirementMetCount: number; // Number of students meeting minimum required events
}
```
