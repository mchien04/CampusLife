# FE Backend Handoff Spec

## 1. Mục Đích

Tài liệu này là bản handoff hợp nhất và mới nhất từ backend sang frontend sau các đợt refactor hệ thống tính điểm (Score Engine), chuỗi sự kiện (Series), và nhiệm vụ (Task/Submission).

Tài liệu này đóng vai trò là **Source of Truth duy nhất** cho team Frontend (TypeScript) tích hợp mà không cần đọc code Java. Mọi tài liệu delta hoặc spec cũ hơn đều được gom về đây.

---

## 2. Báo Cáo Thay Đổi Mới Nhất & Hướng Dẫn Tích Hợp (FE Action Items)

Để giúp team Frontend dễ dàng rà soát mã nguồn hiện tại, dưới đây là tổng hợp các thay đổi nghiệp vụ và API kèm theo hành động cần thực hiện ở Frontend.

### 2.1 Bảng thay đổi nghiệp vụ chính

| Nghiệp vụ / Tính năng | Hành vi Backend mới | Thay đổi UI/UX cần làm ở Frontend | Mức độ |
| :--- | :--- | :--- | :--- |
| **Sự kiện thuộc chuỗi (Series Child Activity)** | Hoạt động con trong chuỗi **không** cộng điểm riêng lẻ. Điểm chỉ được cộng theo mốc hoàn thành chuỗi (Series Milestone) và bị phạt nếu không đạt số hoạt động tối thiểu. | - Ẩn phần hiển thị điểm riêng của hoạt động con nếu nó thuộc Series.<br>- Màn hình Tiến độ chuỗi (Series Progress) cần hiển thị: tiến độ mốc, số sự kiện tối thiểu, trạng thái đạt/chưa đạt (`minimumRequirementMet`), và số sự kiện còn thiếu để tránh bị phạt (`remainingToAvoidPenalty`). | **Cao** |
| **requiresSubmission = false** | Hoạt động không bắt buộc nộp bài vẫn có thể có task/assignment đi kèm. Tuy nhiên, các task này được coi là optional: **không tính điểm, không bị quá hạn (overdue penalty), không khóa completion**. | - Ở màn hình chi tiết hoạt động, ẩn cảnh báo quá hạn hoặc điểm phạt đối với các task optional.<br>- BE tự động chặn không cho tạo quy tắc tính điểm dạng submission cho hoạt động này. | **Trung bình** |
| **requiresSubmission = true** | Hoạt động hoàn thành (COMPLETED) khi và chỉ khi sinh viên **đã điểm danh (ATTENDED)** và **đã được chấm bài (GRADED)** (kể cả chấm trượt). Quy tắc phạt quá hạn (`TASK_OVERDUE`) sẽ lấy đúng theo `failPoints` cấu hình của bài nộp, không tự suy từ điểm cộng. | - Form tạo hoạt động dạng này phải bắt buộc người dùng nhập `failPoints` (điểm khi trượt/quá hạn).<br>- Trạng thái hoàn thành hoạt động phụ thuộc cả 2 yếu tố điểm danh & chấm điểm. | **Cao** |
| **Hai luồng quét QR điểm danh** | Tách biệt hoàn toàn:<br>1. **Activity QR** (`checkInCode`): Sinh viên tự quét -> lên thẳng trạng thái `ATTENDED` (điểm danh nhanh).<br>2. **Ticket QR** (`ticketCode`): Ban tổ chức quét ticket của sinh viên -> luồng transition stateful: quét lần 1 lên `CHECKED_IN`, quét lần 2 lên `ATTENDED`. | - Màn hình quét QR trên App của Sinh viên chỉ dùng để quét mã của hoạt động (Activity QR). Gọi endpoint `/api/registrations/checkin/qr`.<br>- Màn hình quét QR dành cho Ban tổ chức (Organizer) để check-in cho sinh viên. Gọi endpoint `/api/registrations/checkin`. | **Cao** |
| **Minigame & Đáp án** | Bổ sung cấu hình `showAnswers` (cho phép xem đáp án đúng sau khi nộp). MiniGame độc lập có thể cấu hình phạt khi hết lượt mà không pass (`MINIGAME_EXHAUSTED_ATTEMPTS`). | - Form tạo/sửa MiniGame: Thêm toggle `showAnswers` (Hiển thị đáp án đúng sau khi nộp).<br>- Màn hình xem lịch sử/chi tiết attempt của sinh viên: Kiểm tra cờ `showAnswers` từ backend trả về trước khi hiển thị đáp án đúng. Không tự ý render đáp án đúng nếu cờ này bằng `false`. | **Cao** |
| **No-show Penalty** | Preset `EVENT_BASIC` và `EVENT_WITH_SUBMISSION` mặc định bật No-show. Seminar mặc định tắt. Nếu bật No-show cho Seminar, hệ thống bắt buộc phạt sang loại điểm khác (không trừ ngược vào tích lũy chuyên đề chính). | - Form tạo hoạt động: Cho phép bật/tắt No-show và chọn loại điểm phạt phù hợp.<br>- Enforce validation loại điểm phạt của Seminar ở FE nếu bật. | **Trung bình** |
| **Xử lý quá hạn bài nộp** | Backend chuyển sang Quartz tự động quét và đánh dấu `OVERDUE` (không dùng cron hàng ngày). | - FE chỉ hiển thị trạng thái `OVERDUE` khi backend trả về trong status. Không tự viết logic so sánh ngày tháng ở FE để hiển thị trạng thái quá hạn. | **Thấp** |

### 2.2 Các thay đổi về Endpoint API

- **Endpoint Mới (`NEW`):**
  - `GET /api/activities/presets`: Lấy danh sách preset hoạt động gợi ý từ BE.
  - `POST /api/activities/presets/preview`: Preview cấu hình score rules do BE sinh trước khi tạo hoạt động.
  - `GET /api/series/presets`: Lấy danh sách preset chuỗi.
  - `POST /api/series/presets/preview`: Preview milestone/penalty của series.
  - `GET /api/series/{seriesId}/progress/my`: Student xem tiến độ chuỗi của chính mình (bao gồm thông tin phạt).
  - `POST /api/registrations/checkin/qr`: Sinh viên tự quét Activity QR code.
  - `GET /api/scores/ranking`: Bảng xếp hạng điểm sinh viên (phân trang, filter theo khoa/lớp/loại điểm).
  - `POST /api/scores/recalculate/student/{studentId}`: Trigger tính lại điểm thủ công cho sinh viên.
  - `POST /api/scores/recalculate/all`: Trigger tính lại điểm cho toàn trường.
- **Endpoint Thay Đổi Contract (`MODIFIED`):**
  - `POST /api/series` & `PUT /api/series/{seriesId}`: Request body hỗ trợ các trường cấu hình phạt tối thiểu (`minimumRequirementEnabled`, `minimumRequiredEvents`, `minimumPenaltyPoints`).
  - `POST /api/minigames` & `PUT /api/minigames/{miniGameId}`: Request body hỗ trợ trường `showAnswers`.
  - `GET /api/minigames/attempts/{attemptId}`: Response trả thêm trường `showAnswers` và lọc đáp án đúng ở backend nếu `showAnswers` là `false`.

---

## 3. Quy Ước Chung

### 3.1 Wrapper response chuẩn
Hầu hết các API backend sử dụng wrapper JSON sau:
```ts
export interface ApiResponse<T> {
  status: boolean; // true = thành công, false = thất bại
  message: string; // Tin nhắn thông báo
  body: T;         // Dữ liệu trả về
}
```

### 3.2 Ngoại lệ wrapper
API upload hình ảnh **không** dùng field `body` mà trả về link ảnh qua field `data`:
```ts
export interface UploadImageApiResponse {
  status: boolean;
  message: string;
  data: string; // Link ảnh public sau khi upload
}
```

### 3.3 Kiểu dữ liệu số dạng điểm số
Các trường điểm số ở backend được lưu dưới dạng `BigDecimal` (Java) nhằm tránh sai lệch làm tròn. Ở Frontend TypeScript, hãy luôn map và hiển thị các trường này dưới dạng **`string`**.
Các trường bị ảnh hưởng bao gồm:
- `points`, `failPoints`, `currentScore`, `oldScore`, `newScore`, `pointsEarned`

---

## 4. TypeScript Types & Enums Đầy Đủ

Frontend có thể copy-paste trực tiếp các định nghĩa TypeScript này vào thư mục `src/types/` hoặc `src/api/` để sử dụng.

### 4.1 Enums & Types Định Danh
```ts
export type ScoreType = "REN_LUYEN" | "CONG_TAC_XA_HOI" | "CHUYEN_DE";

export type ScoreRuleTrigger =
  | "PARTICIPATION_COMPLETED"      // Hoàn thành tham gia (áp dụng cho Event thường)
  | "NO_SHOW"                      // Không tham gia dù đã đăng ký thành công
  | "SUBMISSION_GRADED"            // Đã nộp bài và bài nộp đã được chấm điểm
  | "MINIGAME_PASSED"              // Vượt qua minigame quiz
  | "MINIGAME_EXHAUSTED_ATTEMPTS"  // Hết lượt thử minigame nhưng không vượt qua (phạt)
  | "SERIES_MILESTONE_REACHED"     // Đạt mốc hoàn thành trong chuỗi sự kiện
  | "TASK_OVERDUE";                // Quá hạn nộp bài

export type ScoreRuleCalculation =
  | "FIXED_POINTS"       // Cộng điểm cố định
  | "COUNT_COMPLETION"   // Tính theo số lần hoàn thành
  | "PASS_FAIL_POINTS"   // Điểm đạt/trượt (cho submission)
  | "PENALTY_POINTS"     // Trừ điểm (phạt no-show hoặc overdue)
  | "SERIES_MILESTONE";  // Tính điểm mốc cho chuỗi

export type ScoreRuleAudience =
  | "ALL_PARTICIPANTS"           // Áp dụng cho mọi sinh viên
  | "DEPARTMENT_ONLY"            // Chỉ áp dụng cho sinh viên trong khoa
  | "OUTSIDE_DEPARTMENTS_ONLY";  // Chỉ áp dụng cho sinh viên ngoài khoa

export type ScoreSemesterPolicy =
  | "ACTIVITY_SEMESTER"      // Tính vào học kỳ của hoạt động
  | "CURRENT_OPEN_SEMESTER"  // Tính vào học kỳ hiện tại đang mở
  | "EXPLICIT_SEMESTER";     // Tính vào một học kỳ cụ thể chỉ định sẵn

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
  | "ATTENDED"
  | "COMPLETED";

export type MiniGameType = "QUIZ";

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
```

### 4.2 Interfaces Dữ Liệu

#### Hoạt Động (Activity & Presets)
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

export interface ActivityPresetConfig {
  primaryScoreType?: ScoreType | null;
  participationPoints?: string | null;
  participationFailPoints?: string | null;
  noShowPenaltyEnabled?: boolean | null;
  noShowPenaltyPoints?: string | null;
  noShowPenaltyScoreType?: ScoreType | null;
  submissionPassPoints?: string | null;
  submissionFailPoints?: string | null;
  taskOverduePenaltyPoints?: string | null;
  minigameExhaustedPenaltyPoints?: string | null;
  bonusScoreType?: ScoreType | null;
  bonusPoints?: string | null;
}

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
  presetCode?: ActivityPresetCode | null;
  presetConfig?: ActivityPresetConfig | null;
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

export interface ActivityPresetDefinition {
  code: ActivityPresetCode;
  displayName: string;
  description: string;
  defaultRequiresSubmission: boolean;
  recommendedActivityTypes: ActivityType[];
  notes: string[];
}

export interface ActivityPresetPreviewResponse {
  presetCode: ActivityPresetCode;
  activityType: ActivityType;
  requiresSubmission: boolean;
  scoreRules: ActivityScoreRuleRequest[];
  notes: string[];
}
```

#### Chuỗi Sự Kiện (Series & Progress)
```ts
export interface SeriesPresetConfig {
  primaryScoreType?: ScoreType | null;
  milestonePoints?: Record<number, number>; // key: số hoạt động cần hoàn thành, value: điểm thưởng
  minimumRequirementEnabled?: boolean | null;
  minimumRequiredEvents?: number | null;
  minimumPenaltyPoints?: number | null;
}

export interface CreateSeriesRequest {
  name: string;
  description?: string | null;
  milestonePoints?: Record<number, number>;
  scoreType?: ScoreType | null;
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

export interface UpdateSeriesRequest extends CreateSeriesRequest {}

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
  minimumPenaltyPoints?: number | null;
  createdAt?: string | null;
}

export interface SeriesPresetDefinition {
  code: SeriesPresetCode;
  displayName: string;
  description: string;
  notes: string[];
}

export interface SeriesPresetPreviewResponse {
  presetCode: SeriesPresetCode;
  scoreType: ScoreType;
  milestonePoints: Record<number, number>;
  minimumRequirementEnabled?: boolean | null;
  minimumRequiredEvents?: number | null;
  minimumPenaltyPoints?: number | null;
  notes: string[];
}

export interface SeriesStudentProgressView {
  studentId: number;
  seriesId: number;
  seriesName: string;
  completedCount: number;
  totalActivities: number;
  completedActivityIds: number[];
  pointsEarned: string; // milestone points hiện tại
  lastUpdated?: string | null;
  currentMilestone?: string | null;
  nextMilestoneCount?: number | null;
  nextMilestonePoints?: number | null;
  milestonePoints?: Record<string, number> | null;
  scoreType: ScoreType;
  minimumRequirementEnabled: boolean;
  minimumRequiredEvents?: number | null;
  minimumPenaltyPoints?: number | null;
  minimumRequirementMet: boolean;
  remainingToAvoidPenalty: number;
}
```

#### Đăng Ký & Điểm Danh (Registration & Attendance)
```ts
export interface ActivityRegistrationRequest {
  activityId: number;
}

export interface ActivityParticipationRequest {
  ticketCode: string;
  studentId: number;
  participationType?: ParticipationType | null; // BE tự transition nếu bỏ trống
  pointsEarned?: string | null;
}

export interface RegistrationResponse {
  id: number;
  activityId: number;
  studentId: number;
  status: RegistrationStatus;
  ticketCode: string;
  registeredAt: string;
}

export type RegistrationStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED" | "ATTENDED" | "WAITLIST";
```

#### Điểm Số & Lịch Sử Điểm (Score & History)
```ts
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
  sourceType: ScoreEntrySourceType;
  changedByUsername?: string | null;
  changedByFullName?: string | null;
}

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

export interface StudentRankResponse {
  rank: number;
  studentId: number;
  studentCode: string;
  studentName: string;
  departmentName: string;
  className: string;
  score: string;
}
```

#### Nhiệm Vụ & Bài Nộp (Task & Submission)
```ts
export interface CreateActivityTaskRequest {
  activityId: number;
  title: string;
  description?: string | null;
  deadline: string;
  isMandatory?: boolean | null;
}

export interface TaskAssignmentRequest {
  taskId: number;
  studentId: number;
}

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
  fileUrls: string[]; // urls cũ (tương thích ngược)
  attachments: SubmissionAttachment[]; // Khuyên dùng cho UI mới
  score?: number | null;
  isCompleted?: boolean | null; // Cờ chấm điểm đạt/không đạt
  feedback?: string | null;
  graderId?: number | null;
  graderUsername?: string | null;
  status: SubmissionStatus;
  submittedAt?: string | null;
  updatedAt?: string | null;
  gradedAt?: string | null;
}
```

#### MiniGame Quiz
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
  showAnswers?: boolean | null;
  questions: CreateMiniGameQuestionRequest[];
}

export interface UpdateMiniGameRequest {
  title: string;
  description?: string | null;
  questionCount: number;
  timeLimit: number;
  requiredCorrectAnswers: number;
  maxAttempts: number;
  showAnswers?: boolean | null;
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
  showAnswers: boolean;
  type: MiniGameType;
  activityId?: number | null;
}

export interface StartAttemptResponse {
  id: number;
  miniGameId: number;
  studentId: number;
  status: string; // e.g., "IN_PROGRESS"
  startedAt: string;
  timeLimit: number;
}

export interface SubmitAttemptResponse {
  id: number;
  status: string; // e.g., "PASSED", "FAILED"
  correctCount: number;
  totalQuestions: number;
  pointsEarned: string; // Điểm nhận được (cộng hoặc trừ)
  startedAt: string;
  submittedAt: string;
  requiredCorrectAnswers: number;
  participation: unknown | null;
}

export interface QuizQuestionOptionDetailResponse {
  id: number;
  text: string;
  isCorrect?: boolean | null; // Chỉ có giá trị nếu showAnswers=true
}

export interface QuizQuestionDetailResponse {
  id: number;
  questionText: string;
  imageUrl?: string | null;
  options: QuizQuestionOptionDetailResponse[];
  selectedOptionId?: number | null;
}

export interface AttemptDetailResponse {
  id: number;
  status: string;
  correctCount: number;
  totalQuestions: number;
  pointsEarned: string;
  startedAt: string;
  submittedAt?: string | null;
  requiredCorrectAnswers: number;
  showAnswers: boolean;
  questions: QuizQuestionDetailResponse[];
}
```

---

## 5. API-First Endpoint Specification

### 5.1 Nhóm API Preset & Preview Cấu Hình

#### 1. Lấy danh sách preset hoạt động
- **Mô tả nghiệp vụ:** Lấy danh sách preset gợi ý cấu hình hoạt động để tạo dropdown trong form tạo hoạt động của Admin.
- **API Endpoint:**
  - **Method:** `GET`
  - **Path:** `/api/activities/presets`
  - **Authentication:** Required (Admin / Manager)
- **Request:** Không có request body hay parameters.
- **Response:**
  - **Success (200):**
    ```json
    {
      "status": true,
      "message": "Activity presets retrieved successfully",
      "body": [
        {
          "code": "EVENT_WITH_SUBMISSION",
          "displayName": "Sự kiện có bài nộp",
          "description": "Sự kiện yêu cầu điểm danh và nộp bài tập chấm điểm để hoàn thành",
          "defaultRequiresSubmission": true,
          "recommendedActivityTypes": ["SUKIEN"],
          "notes": ["Tự động sinh Quartz overdue rule cho task", "Cần cấu hình failPoints"]
        }
      ]
    }
    ```

#### 2. Preview cấu hình Score Rules cho hoạt động
- **Mô tả nghiệp vụ:** Preview danh sách score rules sẽ được backend sinh tự động khi chọn preset cụ thể, giúp hiển thị trước cho Admin xem trước khi tạo chính thức.
- **API Endpoint:**
  - **Method:** `POST`
  - **Path:** `/api/activities/presets/preview`
  - **Authentication:** Required (Admin / Manager)
- **Request:**
  - **Request Body:**
    ```json
    {
      "presetCode": "EVENT_WITH_SUBMISSION",
      "type": "SUKIEN",
      "requiresSubmission": true,
      "presetConfig": {
        "primaryScoreType": "REN_LUYEN",
        "noShowPenaltyEnabled": true,
        "noShowPenaltyPoints": "5",
        "noShowPenaltyScoreType": "REN_LUYEN",
        "submissionPassPoints": "10",
        "submissionFailPoints": "0",
        "taskOverduePenaltyPoints": "5"
      }
    }
    ```
- **Response:**
  - **Success (200):**
    ```json
    {
      "status": true,
      "message": "Activity preset preview generated successfully",
      "body": {
        "presetCode": "EVENT_WITH_SUBMISSION",
        "activityType": "SUKIEN",
        "requiresSubmission": true,
        "scoreRules": [
          {
            "scoreType": "REN_LUYEN",
            "triggerType": "NO_SHOW",
            "calculation": "PENALTY_POINTS",
            "points": "0",
            "failPoints": "5"
          },
          {
            "scoreType": "REN_LUYEN",
            "triggerType": "SUBMISSION_GRADED",
            "calculation": "PASS_FAIL_POINTS",
            "points": "10",
            "failPoints": "0"
          },
          {
            "scoreType": "REN_LUYEN",
            "triggerType": "TASK_OVERDUE",
            "calculation": "PENALTY_POINTS",
            "points": "0",
            "failPoints": "5"
          }
        ],
        "notes": ["Tất cả các bài nộp quá hạn sẽ bị trừ 5 điểm rèn luyện"]
      }
    }
    ```

#### 3. Lấy danh sách preset chuỗi sự kiện
- **Mô tả nghiệp vụ:** Lấy danh sách preset cho chuỗi sự kiện.
- **API Endpoint:**
  - **Method:** `GET`
  - **Path:** `/api/series/presets`
  - **Authentication:** Required (Admin / Manager)
- **Response:**
  - **Success (200):**
    ```json
    {
      "status": true,
      "message": "Series presets retrieved successfully",
      "body": [
        {
          "code": "ENTERPRISE_SERIES",
          "displayName": "Chuỗi sự kiện doanh nghiệp",
          "description": "Chuỗi workshop có tính mốc và phạt nếu không tham gia đủ số buổi",
          "notes": ["Mặc định bật kiểm tra số buổi tối thiểu"]
        }
      ]
    }
    ```

#### 4. Preview cấu hình chuỗi sự kiện
- **Mô tả nghiệp vụ:** Xem trước milestone points và penalty config cho series.
- **API Endpoint:**
  - **Method:** `POST`
  - **Path:** `/api/series/presets/preview`
  - **Authentication:** Required (Admin / Manager)
- **Request:**
  - **Request Body:**
    ```json
    {
      "presetCode": "ENTERPRISE_SERIES",
      "presetConfig": {
        "primaryScoreType": "CHUYEN_DE",
        "milestonePoints": {
          "3": 5,
          "5": 10
        },
        "minimumRequirementEnabled": true,
        "minimumRequiredEvents": 3,
        "minimumPenaltyPoints": 2
      }
    }
    ```
- **Response:**
  - **Success (200):**
    ```json
    {
      "status": true,
      "message": "Series preset preview generated successfully",
      "body": {
        "presetCode": "ENTERPRISE_SERIES",
        "scoreType": "CHUYEN_DE",
        "milestonePoints": {
          "3": 5,
          "5": 10
        },
        "minimumRequirementEnabled": true,
        "minimumRequiredEvents": 3,
        "minimumPenaltyPoints": 2,
        "notes": ["Sinh viên sẽ bị trừ 2 điểm chuyên đề nếu tham gia dưới 3 buổi"]
      }
    }
    ```

---

### 5.2 Nhóm API Quản Lý Hoạt Động (Activity Management)

#### 1. Tạo hoạt động mới
- **Mô tả nghiệp vụ:** Tạo hoạt động mới kèm theo rules tính điểm tự động từ presets hoặc custom rules.
- **API Endpoint:**
  - **Method:** `POST`
  - **Path:** `/api/activities`
  - **Authentication:** Required (Admin / Manager)
- **Request:**
  - **Request Body:** Gửi DTO `CreateActivityRequest` (xem định nghĩa ở mục 4).
- **Response:**
  - **Success (200):** Trả về `ApiResponse<ActivityResponse>`.
  - **Error (400):** `{"status": false, "message": "Thông tin cấu hình không hợp lệ", "body": null}`

#### 2. Cập nhật hoạt động
- **Mô tả nghiệp vụ:** Cập nhật thông tin hoạt động có sẵn.
- **API Endpoint:**
  - **Method:** `PUT`
  - **Path:** `/api/activities/{id}`
  - **Authentication:** Required (Admin / Manager)
- **Request:**
  - **Path Parameters:** `id` (number) - ID của hoạt động
  - **Request Body:** Gửi DTO `CreateActivityRequest`.
- **Response:**
  - **Success (200):** Trả về `ApiResponse<ActivityResponse>`.

#### 3. Danh sách hoạt động trả về dạng bọc Response
- **Mô tả nghiệp vụ:** Lấy toàn bộ danh sách hoạt động có bọc wrapper Response.
- **API Endpoints:**
  - `GET /api/activities` (Lấy tất cả hoạt động)
  - `GET /api/activities/{id}` (Lấy chi tiết hoạt động)
- **Response:**
  - **Success (200):** Trả về `ApiResponse<ActivityResponse>` hoặc `ApiResponse<ActivityResponse[]>`.

#### 4. Danh sách hoạt động dạng Raw List (Không bọc Response)
> [!IMPORTANT]
> Các endpoint dưới đây backend trả thẳng danh sách `ActivityResponse[]`, không bọc qua `ApiResponse`. Frontend cần parse trực tiếp data nhận được từ Axios/Fetch.
- **API Endpoints:**
  - `GET /api/activities/score-type/{scoreType}` (Lấy theo loại điểm)
  - `GET /api/activities/department/{deptId}` (Lấy theo khoa)
  - `GET /api/activities/my` (Hoạt động của sinh viên hiện tại đăng nhập)
  - `GET /api/activities/upcoming` (Các hoạt động sắp diễn ra)
  - `GET /api/activities/month` (Lịch hoạt động trong tháng)

---

### 5.3 Nhóm API Chuỗi Sự Kiện (Series Management)

#### 1. Tạo chuỗi sự kiện mới
- **Mô tả nghiệp vụ:** Tạo chuỗi sự kiện mới với cấu hình mốc thưởng (milestone) và phạt tối thiểu.
- **API Endpoint:**
  - **Method:** `POST`
  - **Path:** `/api/series`
  - **Authentication:** Required (Admin / Manager)
- **Request:**
  - **Request Body:** Gửi DTO `CreateSeriesRequest`.
- **Response:**
  - **Success (200):** Trả về `ApiResponse<SeriesResponse>`.
  - **Error (400):** Trả về lỗi nếu `minimumRequiredEvents` hoặc `minimumPenaltyPoints` không hợp lệ khi bật phạt.

#### 2. Cập nhật chuỗi sự kiện
- **Mô tả nghiệp vụ:** Cập nhật thông tin và cấu hình phạt cho chuỗi.
- **API Endpoint:**
  - **Method:** `PUT`
  - **Path:** `/api/series/{seriesId}`
  - **Authentication:** Required (Admin / Manager)
- **Request:**
  - **Path Parameters:** `seriesId` (number)
  - **Request Body:** Gửi DTO `UpdateSeriesRequest`.
- **Response:**
  - **Success (200):** Trả về `ApiResponse<SeriesResponse>`.

#### 3. Student đăng ký tham gia toàn bộ chuỗi
- **Mô tả nghiệp vụ:** Sinh viên đăng ký tham gia chuỗi sự kiện, backend sẽ tự động tạo đăng ký tham gia (registration) cho mọi hoạt động hiện có trong chuỗi.
- **API Endpoint:**
  - **Method:** `POST`
  - **Path:** `/api/series/{seriesId}/register`
  - **Authentication:** Required (Student)
- **Response:**
  - **Success (200):**
    ```json
    {
      "status": true,
      "message": "Registered for series successfully. 5 activities registered.",
      "body": [] // Danh sách Registration Entity
    }
    ```

#### 4. Student tự xem tiến độ trong chuỗi
- **Mô tả nghiệp vụ:** Xem chi tiết số hoạt động đã hoàn thành, điểm mốc tích lũy, cảnh báo phạt tối thiểu.
- **API Endpoint:**
  - **Method:** `GET`
  - **Path:** `/api/series/{seriesId}/progress/my`
  - **Authentication:** Required (Student)
- **Response:**
  - **Success (200):**
    ```json
    {
      "status": true,
      "message": "Student progress retrieved successfully",
      "body": {
        "studentId": 15,
        "seriesId": 10,
        "seriesName": "Workshop Doanh Nghiệp 2026",
        "completedCount": 2,
        "totalActivities": 5,
        "completedActivityIds": [101, 102],
        "pointsEarned": "5",
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
    }
    ```

#### 5. Admin xem tiến độ chuỗi của một sinh viên
- **API Endpoint:**
  - **Method:** `GET`
  - **Path:** `/api/series/{seriesId}/students/{studentId}/progress`
  - **Authentication:** Required (Admin / Manager)
- **Response:** Giống endpoint progress/my ở trên.

---

### 5.4 Nhóm API Đăng Ký & Quét QR Điểm Danh (Registration & Check-in)

#### 1. Sinh viên điểm danh nhanh qua Activity QR Code (Student quét)
- **Mô tả nghiệp vụ:** Sinh viên tự mở camera quét QR Code của Hoạt động (được hiển thị trên màn hình lớn của ban tổ chức). Backend sẽ cập nhật ngay trạng thái tham gia thành `ATTENDED`.
- **API Endpoint:**
  - **Method:** `POST`
  - **Path:** `/api/registrations/checkin/qr`
  - **Authentication:** Required (Student)
- **Request:**
  - **Request Body:**
    ```json
    {
      "checkInCode": "QR_CODE_STRING_FROM_ACTIVITY"
    }
    ```
- **Response:**
  - **Success (200):**
    ```json
    {
      "status": true,
      "message": "Check-in successful. Status updated to ATTENDED.",
      "body": null
    }
    ```

#### 2. Ban tổ chức điểm danh qua Ticket QR Code của Sinh viên (Organizer quét)
- **Mô tả nghiệp vụ:** Ban tổ chức quét mã ticket cá nhân của từng sinh viên. Hỗ trợ thay đổi trạng thái theo bước quét: Lần 1 quét chuyển từ `REGISTERED -> CHECKED_IN`, Lần 2 quét chuyển từ `CHECKED_IN -> ATTENDED`.
- **API Endpoint:**
  - **Method:** `POST`
  - **Path:** `/api/registrations/checkin`
  - **Authentication:** Required (Admin / Manager / Organizer)
- **Request:**
  - **Request Body:**
    ```json
    {
      "ticketCode": "TICKET_CODE_FROM_STUDENT",
      "studentId": 15,
      "participationType": null // Có thể truyền CHECKED_IN hoặc ATTENDED, hoặc để null BE tự transition
    }
    ```
- **Response:**
  - **Success (201):** Trả về participation status sau khi check-in thành công.

---

### 5.5 Nhóm API Nhiệm Vụ & Bài Nộp (Task & Submission)

#### 1. Nộp bài cho nhiệm vụ
- **Mô tả nghiệp vụ:** Sinh viên nộp bài làm (văn bản và file đính kèm/ảnh) cho một task cụ thể.
- **API Endpoint:**
  - **Method:** `POST`
  - **Path:** `/api/submissions/task/{taskId}`
  - **Authentication:** Required (Student)
  - **Content-Type:** `multipart/form-data`
- **Request:**
  - **Path Parameters:** `taskId` (number)
  - **FormData Fields:**
    - `content` (string, optional)
    - `files` (List<MultipartFile>, optional)
    - `images` (List<MultipartFile>, optional)
- **Response:**
  - **Success (200):** Trả về `ApiResponse<TaskSubmissionResponse>`.

#### 2. Chấm điểm bài nộp (Đạt / Không đạt)
- **Mô tả nghiệp vụ:** Admin/Manager chấm điểm bài nộp. Đạt (`isCompleted=true`) sẽ kích hoạt cộng điểm pass. Không đạt (`isCompleted=false`) sẽ tính điểm fail.
- **API Endpoint:**
  - **Method:** `PUT`
  - **Path:** `/api/submissions/{submissionId}/grade`
  - **Authentication:** Required (Admin / Manager)
- **Request:**
  - **Path Parameters:** `submissionId` (number)
  - **Query Parameters:**
    - `isCompleted` (boolean, required): `true` = Đạt, `false` = Không đạt.
    - `feedback` (string, optional): Nhận xét từ người chấm.
- **Response:**
  - **Success (200):** Trả về `ApiResponse<TaskSubmissionResponse>`.

---

### 5.6 Nhóm API MiniGame Quiz

#### 1. Tạo mới MiniGame cho hoạt động
- **API Endpoint:**
  - **Method:** `POST`
  - **Path:** `/api/minigames`
  - **Authentication:** Required (Admin / Manager)
- **Request:**
  - **Request Body:** Gửi DTO `CreateMiniGameRequest` (chú ý cờ `showAnswers`).
- **Response:**
  - **Success (200):** Trả về `ApiResponse<unknown>`. Sau khi tạo, FE nên reload lại chi tiết.

#### 2. Lấy thông tin MiniGame của hoạt động
- **API Endpoint:**
  - **Method:** `GET`
  - **Path:** `/api/minigames/activity/{activityId}`
  - **Authentication:** Required
- **Response:**
  - **Success (200):** Trả về `ApiResponse<MiniGameResponse>`.

#### 3. Bắt đầu lượt chơi MiniGame
- **Mô tả nghiệp vụ:** Tạo lượt thử (attempt) mới cho sinh viên chơi game.
- **API Endpoint:**
  - **Method:** `POST`
  - **Path:** `/api/minigames/{miniGameId}/start`
  - **Authentication:** Required (Student)
- **Response:**
  - **Success (200):** Trả về `ApiResponse<StartAttemptResponse>`.

#### 4. Nộp kết quả MiniGame
- **Mô tả nghiệp vụ:** Sinh viên nộp bài chọn phương án trả lời.
- **API Endpoint:**
  - **Method:** `POST`
  - **Path:** `/api/minigames/attempts/{attemptId}/submit`
  - **Authentication:** Required (Student)
- **Request:**
  - **Request Body:**
    ```json
    {
      "answers": {
        "1": 25, // questionId: optionId
        "2": 28
      }
    }
    ```
- **Response:**
  - **Success (200):** Trả về `ApiResponse<SubmitAttemptResponse>`.

#### 5. Xem chi tiết lượt thử (Quiz Details)
- **Mô tả nghiệp vụ:** Xem lại danh sách câu hỏi, câu trả lời đã chọn và đáp án đúng.
- **API Endpoint:**
  - **Method:** `GET`
  - **Path:** `/api/minigames/attempts/{attemptId}`
  - **Authentication:** Required (Student sở hữu hoặc Admin)
- **Response:**
  - **Success (200):** Trả về `ApiResponse<AttemptDetailResponse>`.
  - **Chú ý:** Nếu cờ `showAnswers` của minigame là `false`, backend sẽ **không** trả về cờ `isCorrect` hoặc đáp án đúng trong list options câu hỏi. FE chỉ hiển thị phương án sinh viên đã chọn, không hiển thị đáp án đúng.

---

### 5.7 Nhóm API Điểm Số & Xếp Hạng (Score & Ranking)

#### 1. Lấy bảng xếp hạng điểm sinh viên (Ranking)
- **Mô tả nghiệp vụ:** Xem xếp hạng điểm rèn luyện, chuyên đề, công tác xã hội toàn trường, theo khoa hoặc theo lớp.
- **API Endpoint:**
  - **Method:** `GET`
  - **Path:** `/api/scores/ranking`
  - **Authentication:** Required
- **Request:**
  - **Query Parameters:**
    - `semesterId` (number, required): ID học kỳ.
    - `scoreType` (string, optional): `"REN_LUYEN"`, `"CONG_TAC_XA_HOI"`, `"CHUYEN_DE"`. Nếu không truyền, tính tổng tất cả loại điểm.
    - `departmentId` (number, optional): ID khoa.
    - `classId` (number, optional): ID lớp.
    - `sortOrder` (string, optional, default: `"DESC"`): `"ASC"` hoặc `"DESC"`.
- **Response:**
  - **Success (200):**
    ```json
    {
      "status": true,
      "message": "Bảng xếp hạng điểm sinh viên",
      "body": {
        "semesterId": 1,
        "semesterName": "Học kỳ I 2025-2026",
        "scoreType": "REN_LUYEN",
        "departmentId": null,
        "classId": null,
        "sortOrder": "DESC",
        "totalStudents": 1,
        "rankings": [
          {
            "rank": 1,
            "studentId": 12,
            "studentCode": "SV0012",
            "studentName": "Nguyễn Văn A",
            "departmentId": 2,
            "departmentName": "Công nghệ thông tin",
            "classId": 5,
            "className": "D19-CNTT1",
            "semesterId": 1,
            "semesterName": "Học kỳ I 2025-2026",
            "scoreType": "REN_LUYEN",
            "score": "95.5",
            "scoreTypeLabel": "Điểm rèn luyện"
          }
        ]
      }
    }
    ```

#### 2. Trigger tính toán lại điểm thủ công cho một sinh viên (Recalculate)
- **Mô tả nghiệp vụ:** Yêu cầu backend quét và tính toán lại điểm toàn bộ hoạt động & chuỗi sự kiện của 1 sinh viên trong học kỳ (thường dùng khi có khiếu nại hoặc dữ liệu đồng bộ chậm).
- **API Endpoint:**
  - **Method:** `POST`
  - **Path:** `/api/scores/recalculate/student/{studentId}`
  - **Authentication:** Required (Admin / Manager)
- **Request:**
  - **Path Parameters:** `studentId` (number)
  - **Query Parameters:**
    - `semesterId` (number, optional): Nếu bỏ trống, backend tự động lấy học kỳ hiện tại đang mở.
- **Response:**
  - **Success (200):**
    ```json
    {
      "status": true,
      "message": "Score recalculated successfully",
      "body": null
    }
    ```
- **Documentation Notes:**
  - > [!IMPORTANT]
    > Đây là API chạy **Đồng bộ (Synchronous)**. Tác vụ tính toán lại điểm cho sinh viên sẽ được thực hiện trực tiếp trên luồng HTTP request này. FE nên hiển thị loading indicator cho đến khi nhận được response.

#### 3. Trigger tính toán lại điểm cho tất cả sinh viên (Recalculate All)
- **Mô tả nghiệp vụ:** Yêu cầu backend quét và tính toán lại điểm cho toàn bộ sinh viên trong một học kỳ.
- **API Endpoint:**
  - **Method:** `POST`
  - **Path:** `/api/scores/recalculate/all`
  - **Authentication:** Required (Admin / Manager)
- **Request:**
  - **Query Parameters:**
    - `semesterId` (number, optional): Nếu bỏ trống, backend tự động lấy học kỳ hiện tại đang mở.
- **Response:**
  - **Success (200):**
    ```json
    {
      "status": true,
      "message": "Recalculated all student scores",
      "body": {
        "semesterId": 1,
        "semesterName": "Học kỳ I 2025-2026",
        "totalStudents": 1500,
        "successCount": 1498,
        "errorCount": 2,
        "errors": [
          {
            "studentId": 45,
            "studentCode": "SV0045",
            "error": "Lỗi kết nối cơ sở dữ liệu khi cập nhật điểm"
          }
        ]
      }
    }
    ```
- **Documentation Notes:**
  - > [!IMPORTANT]
    > Đây là API chạy **Đồng bộ (Synchronous)**. Backend sẽ duyệt qua toàn bộ danh sách sinh viên để tính lại điểm. Thời gian phản hồi có thể kéo dài đáng kể (vài chục giây đến vài phút tùy theo quy mô dữ liệu). FE **bắt buộc** hiển thị màn hình loading chờ/khóa UI cho Admin và cảnh báo tác vụ đang xử lý.


---

### 5.8 Nhóm API Upload Hình Ảnh (File Upload Exception)

#### 1. Upload hình ảnh (Dạng Raw Data - Ngoại lệ Wrapper)
- **Mô tả nghiệp vụ:** Gửi ảnh lưu trữ lên server để lấy URL ảnh public.
- **API Endpoint:**
  - **Method:** `POST`
  - **Path:** `/api/upload/image`
  - **Authentication:** Required
  - **Content-Type:** `multipart/form-data`
- **Request:**
  - **FormData Fields:**
    - `file` (MultipartFile, required): File ảnh cần upload (tối đa 5MB, định dạng image/*).
- **Response:**
  - **Success (200):**
    ```json
    {
      "status": true,
      "message": "File uploaded successfully",
      "data": "https://server.domain/uploads/images/abc-123.jpg"
    }
    ```
  - **Chú ý:** Trường chứa URL trả về là **`data`**, không phải `body`.

#### 2. Xóa hình ảnh
- **API Endpoint:**
  - **Method:** `DELETE`
  - **Path:** `/api/upload/image`
  - **Authentication:** Required
- **Request:**
  - **Query Parameters:**
    - `fileUrl` (string, required): URL đầy đủ của ảnh cần xóa.
- **Response:**
  - **Success (200):** `{"status": true, "message": "File deleted successfully"}`

---

## 6. Lời Khuyên Cấu Trúc Code Frontend

Team Frontend nên phân chia các file TypeScript theo hướng module hóa để dễ quản lý và cập nhật:

1. **Thư mục `src/types/`**:
   - `presets.ts`: Khai báo các loại Preset, Preset Config cho Activity và Series.
   - `activity.ts`: Định nghĩa Create/Update Request và Activity Response.
   - `series.ts`: Định nghĩa Create/Update Request, Series Response và SeriesStudentProgressView.
   - `score.ts`: Định nghĩa ScoreHistory, ActivityParticipation và StudentRank.
   - `submission.ts`: Định nghĩa các cấu trúc bài nộp.
   - `minigame.ts`: Định nghĩa các request/response cho Quiz.

2. **Thư mục `src/api/`**:
   Tạo các hàm fetch/post bọc Axios, ví dụ:
   ```ts
   import axios from 'axios';
   import { ApiResponse } from '@/types/common';
   import { SeriesStudentProgressView } from '@/types/series';

   export const getMySeriesProgress = async (seriesId: number): Promise<SeriesStudentProgressView> => {
     const response = await axios.get<ApiResponse<SeriesStudentProgressView>>(`/api/series/${seriesId}/progress/my`);
     return response.data.body; // Unwraps ApiResponse wrapper
   };
   ```

3. **Cảnh giác (Common Pitfalls) ở FE:**
   - Không tự động thêm prefix `/uploads` vào ảnh; backend trả về full link ảnh public.
   - Các API dạng raw list (`GET /api/activities/my`, `/upcoming`, `/month`, `/score-type/*`, `/department/*`) **không** dùng `ApiResponse` wrapper, hãy xử lý trực tiếp payload danh sách.
   - Luôn sử dụng kiểu dữ liệu `string` ở FE cho các trường chứa điểm (ví dụ: `pointsEarned: string`) để tương thích với `BigDecimal` phía backend, tránh bị làm tròn số không mong muốn ở trình duyệt.
