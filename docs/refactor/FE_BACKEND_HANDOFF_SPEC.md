# FE Backend Handoff Spec

## 1. Mục Đích

Tài liệu này là bản handoff trực tiếp từ backend sang frontend sau đợt refactor score engine và domain package.

Mục tiêu:

- Cung cấp contract backend thực tế để team frontend tích hợp mà không phải dò lại code Java.
- Tổng hợp endpoint, request, response và TypeScript types quan trọng.
- Chỉ ra các điểm không đồng nhất hoặc các chỗ cần adapter riêng ở frontend.

Nguồn chuẩn của tài liệu này là:

- `docs/refactor/BE_IMPLEMENTATION_REPORT.md`
- controller/service/model hiện tại trong backend

---

## 2. Quy Ước Chung

## 2.1 Wrapper response chuẩn

Phần lớn API backend dùng wrapper:

```ts
export interface ApiResponse<T> {
  status: boolean;
  message: string;
  body: T;
}
```

Ví dụ:

```json
{
  "status": true,
  "message": "success",
  "body": { "...": "..." }
}
```

## 2.2 Ngoại lệ wrapper

API upload image **không** dùng `body`, mà dùng `data`:

```ts
export interface UploadImageApiResponse {
  status: boolean;
  message: string;
  data: string;
}
```

## 2.3 Dữ liệu số kiểu điểm

Các field điểm trong backend thường là `BigDecimal`, FE nên map sang `string` để tránh mất precision:

- `points`
- `failPoints`
- `currentScore`
- `oldScore`
- `newScore`
- `pointsEarned`

---

## 3. TypeScript Types Đề Xuất

## 3.1 Enum

```ts
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

## 3.2 Score Rule Types

```ts
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
```

## 3.3 Activity Types

```ts
export interface CreateActivityRequest {
  name: string;
  type: ActivityType;
  description?: string | null;
  startDate: string;
  endDate: string;
  requiresSubmission?: boolean | null;
  scoreRules?: ActivityScoreRuleRequest[];
  registrationStartDate?: string | null;
  registrationDeadline?: string | null;
  shareLink?: string | null;
  isImportant?: boolean | null;
  isDraft?: boolean | null;
  bannerUrl?: string | null;
  location?: string | null;
  ticketQuantity?: number | null;
  benefits?: string | null;
  requirements?: string | null;
  contactInfo?: string | null;
  requiresApproval?: boolean | null;
  mandatoryForFacultyStudents?: boolean | null;
  organizerIds?: number[];
}

export interface ActivityResponse {
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
  createdAt?: string | null;
  updatedAt?: string | null;
  createdBy?: string | null;
  lastModifiedBy?: string | null;
}
```

## 3.4 Score History Types

```ts
export interface ActivityParticipationDetailResponse {
  id: number;
  activityId: number;
  activityName: string;
  activityType: ActivityType;
  seriesId?: number | null;
  seriesName?: string | null;
  pointsEarned?: string | null;
  participationType: ParticipationType;
  date: string;
  isCompleted?: boolean | null;
  sourceType?: string | null;
}

export interface ScoreHistoryDetailResponse {
  id: number;
  oldScore: string;
  newScore: string;
  changeDate: string;
  reason?: string | null;
  activityId?: number | null;
  activityName?: string | null;
  seriesId?: number | null;
  seriesName?: string | null;
  sourceType: ScoreEntrySourceType | string;
  changedByUsername?: string | null;
  changedByFullName?: string | null;
}

export interface ScoreHistoryViewResponse {
  studentId: number;
  studentCode: string;
  studentName: string;
  semesterId: number;
  semesterName: string;
  scoreType?: ScoreType | null;
  currentScore: string;
  scoreHistories: ScoreHistoryDetailResponse[];
  activityParticipations: ActivityParticipationDetailResponse[];
  totalRecords: number;
  page: number;
  size: number;
  totalPages: number;
}
```

## 3.5 Task Submission Types

```ts
export interface SubmissionAttachment {
  url: string;
  type: "file" | "image";
}

export interface TaskSubmissionResponse {
  id: number;
  taskId: number;
  taskTitle: string;
  studentId: number;
  studentCode: string;
  studentName: string;
  content?: string | null;
  fileUrls: string[];
  attachments: SubmissionAttachment[];
  score?: number | null;
  isCompleted?: boolean | null;
  feedback?: string | null;
  graderId?: number | null;
  graderUsername?: string | null;
  status: SubmissionStatus;
  submittedAt?: string | null;
  updatedAt?: string | null;
  gradedAt?: string | null;
}
```

## 3.6 MiniGame Types

```ts
export interface CreateMiniGameQuestionOptionRequest {
  text: string;
  isCorrect: boolean;
}

export interface CreateMiniGameQuestionRequest {
  questionText: string;
  imageUrl?: string | null;
  options: CreateMiniGameQuestionOptionRequest[];
}

export interface CreateMiniGameRequest {
  activityId: number;
  title: string;
  description?: string | null;
  questionCount: number;
  timeLimit: number;
  requiredCorrectAnswers: number;
  maxAttempts: number;
  questions: CreateMiniGameQuestionRequest[];
}

export interface UpdateMiniGameRequest {
  title: string;
  description?: string | null;
  questionCount: number;
  timeLimit: number;
  requiredCorrectAnswers: number;
  maxAttempts: number;
  questions: CreateMiniGameQuestionRequest[];
}

export interface MiniGameResponse {
  id: number;
  title: string;
  description?: string | null;
  questionCount: number;
  timeLimit: number;
  requiredCorrectAnswers: number;
  maxAttempts: number;
  isActive: boolean;
  type: MiniGameType;
  activityId?: number | null;
}

export interface StartAttemptResponse {
  id: number;
  miniGameId: number;
  studentId: number;
  status: string;
  startedAt: string;
  timeLimit: number;
}

export interface SubmitAttemptResponse {
  id: number;
  status: string;
  correctCount: number;
  totalQuestions: number;
  pointsEarned: string;
  startedAt: string;
  submittedAt: string;
  requiredCorrectAnswers: number;
  participation: unknown | null;
}

export interface SubmitAttemptRequest {
  answers: Record<string, number>;
}
```

## 3.7 Upload Types

```ts
export interface UploadImageApiResponse {
  status: boolean;
  message: string;
  data: string;
}
```

---

## 4. Endpoint Spec

## 4.1 Activity

### Tạo activity

- Method: `POST`
- Path: `/api/activities`
- Wrapper: `ApiResponse<ActivityResponse>`
- Auth: yêu cầu quyền manager/admin

### Cập nhật activity

- Method: `PUT`
- Path: `/api/activities/{id}`
- Wrapper: `ApiResponse<ActivityResponse>`

### Lấy toàn bộ activity

- Method: `GET`
- Path: `/api/activities`
- Wrapper: `ApiResponse<ActivityResponse[]>`

### Lấy activity theo id

- Method: `GET`
- Path: `/api/activities/{id}`
- Wrapper: `ApiResponse<ActivityResponse>`

### Các endpoint trả raw list

Các endpoint sau **không** bọc `Response`, FE cần parse trực tiếp `ActivityResponse[]`:

- `GET /api/activities/score-type/{scoreType}`
- `GET /api/activities/department/{deptId}`
- `GET /api/activities/my`
- `GET /api/activities/upcoming`
- `GET /api/activities/month`

### Ghi chú tích hợp

- `scoreRules` là nguồn cấu hình điểm duy nhất ở tầng activity.
- Không dựng UI dựa trên `maxPoints`, `rewardPoints`, `penaltyPointsIncomplete`.

## 4.2 Score History

### Xem lịch sử điểm

- Method: `GET`
- Path: `/api/scores/history/student/{studentId}`
- Wrapper: `ApiResponse<ScoreHistoryViewResponse>`

### Query params

- `semesterId`: bắt buộc
- `scoreType`: tùy chọn
- `page`: tùy chọn
- `size`: tùy chọn

### Ghi chú tích hợp

- FE plan cũ mô tả `body.content[]` là không đúng.
- Dữ liệu chính nằm ở `body.scoreHistories[]`.
- `body.activityParticipations[]` là nhóm dữ liệu phụ để hiển thị activity participation detail.

## 4.3 Task Submission

### Submit task

- Method: `POST`
- Path: `/api/submissions/task/{taskId}`
- Content-Type: `multipart/form-data`
- Wrapper: `ApiResponse<TaskSubmissionResponse>`

### Update submission

- Method: `PUT`
- Path: `/api/submissions/{submissionId}`
- Content-Type: `multipart/form-data`
- Wrapper: `ApiResponse<TaskSubmissionResponse>`

### Form fields

- `content`: string
- `files`: repeated file field
- `images`: repeated file field

### Các endpoint liên quan

- `GET /api/submissions/task/{taskId}/my`
- `GET /api/submissions/task/{taskId}`
- `GET /api/submissions/{submissionId}`
- `GET /api/submissions/{submissionId}/files`
- `PUT /api/submissions/{submissionId}/grade`
- `DELETE /api/submissions/{submissionId}`

### Ghi chú tích hợp

- `attachments` là field nên dùng cho UI mới.
- `fileUrls` chỉ là fallback tương thích ngược.
- Field đúng là `isCompleted`, không phải `isApproved`.

## 4.4 MiniGame

### Tạo minigame

- Method: `POST`
- Path: `/api/minigames`
- Wrapper: `ApiResponse<unknown>`

Ghi chú:

- Hiện create/update minigame chưa hoàn toàn đồng nhất DTO response với các endpoint get/list.
- FE nên ưu tiên dùng `getMiniGameByActivity` hoặc `getAllMiniGames` để đồng bộ dữ liệu sau khi create/update nếu cần shape chuẩn.

### Lấy minigame theo activity

- Method: `GET`
- Path: `/api/minigames/activity/{activityId}`
- Wrapper: `ApiResponse<MiniGameResponse>`

### Start attempt

- Method: `POST`
- Path: `/api/minigames/{miniGameId}/start`
- Wrapper: `ApiResponse<StartAttemptResponse>`

### Submit attempt

- Method: `POST`
- Path: `/api/minigames/attempts/{attemptId}/submit`
- Wrapper: `ApiResponse<SubmitAttemptResponse>`

### Payload submit đúng

```json
{
  "answers": {
    "1": 2,
    "2": 5
  }
}
```

### Ghi chú tích hợp

- Không gửi `answers` dưới dạng array object.
- Không dựa vào `rewardPoints`.
- Không dùng field `passed`; FE nên kiểm `status === "PASSED"`.

## 4.5 Upload Image

### Upload image

- Method: `POST`
- Path: `/api/upload/image`
- Content-Type: `multipart/form-data`
- Field: `file`
- Response: `UploadImageApiResponse`

### Delete image

- Method: `DELETE`
- Path: `/api/upload/image`
- Query: `fileUrl`

### Ghi chú tích hợp

- FE chỉ giữ URL string backend trả về.
- Không tự cắt hay ghép prefix `/uploads`.
- URL public phụ thuộc `app.upload.public-url`.

---

## 5. Những Mismatch So Với Tài Liệu FE Cũ

- Task submission path cũ `/api/tasks/submissions` là sai.
- Task submission field `isApproved` là sai, field đúng là `isCompleted`.
- Score history không trả `body.content[]`, mà trả `body.scoreHistories[]`.
- `semesterId` của score history là bắt buộc.
- Minigame submit request không dùng array answers.
- Minigame submit response không có `attemptId`, `passed`.
- Upload image response không dùng `body`, mà dùng `data`.

---

## 6. Khuyến Nghị Cấu Trúc FE

Nên tách theo domain:

- `src/api/activity.ts`
- `src/api/score.ts`
- `src/api/submission.ts`
- `src/api/minigame.ts`
- `src/api/upload.ts`
- `src/types/score.ts`
- `src/types/activity.ts`
- `src/types/submission.ts`
- `src/types/minigame.ts`

Nên tách 2 helper response:

```ts
export function unwrapApiResponse<T>(resp: ApiResponse<T>): T {
  return resp.body;
}

export function unwrapUploadResponse(resp: UploadImageApiResponse): string {
  return resp.data;
}
```

---

## 7. Checklist Cho Frontend

- [ ] Bỏ dùng các field điểm tĩnh cũ.
- [ ] Chuyển form activity sang `scoreRules`.
- [ ] Chuyển score history UI sang `scoreHistories[]`.
- [ ] Chuyển task submission sang multipart `content/files/images`.
- [ ] Dùng `attachments` để render submission attachments.
- [ ] Chuyển minigame submit payload sang object map.
- [ ] Dùng upload API trả URL string qua `data`.
- [ ] Không hardcode prefix public cho file/image.
