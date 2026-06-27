# FE Backend Handoff Spec

> **Version:** 5.1 (Series isDraft + Per-rule audience config)  
> **Baseline:** Post v5.0 → Current HEAD (series `isDraft`, per-rule audience/semesterPolicy/departmentIds on `ActivityPresetConfig`)  
> **Source of truth:** Java backend implementation (controllers, DTOs, mappers, services, validators)

## 1. Mục Đích

Tài liệu này là bản handoff hợp nhất và mới nhất từ backend sang frontend sau các đợt refactor hệ thống tính điểm (Score Engine), chuỗi sự kiện (Series), và nhiệm vụ (Task/Submission).

Tài liệu này đóng vai trò là **Source of Truth duy nhất** cho team Frontend (TypeScript) tích hợp mà không cần đọc code Java. Mọi tài liệu delta hoặc spec cũ hơn đều được gom về đây.

---

## 2. Báo Cáo Thay Đổi Mới Nhất & Hướng Dẫn Tích Hợp (FE Action Items)

### 2.1 Bảng thay đổi nghiệp vụ chính

| Nghiệp vụ / Tính năng | Hành vi Backend mới | Thay đổi UI/UX cần làm ở Frontend | Mức độ |
| :--- | :--- | :--- | :--- |
| **Sự kiện thuộc chuỗi (Series Child Activity)** | Hoạt động con trong chuỗi **không** cộng điểm riêng lẻ. Điểm chỉ được cộng theo mốc hoàn thành chuỗi (Series Milestone) và bị phạt nếu không đạt số hoạt động tối thiểu. | - Ẩn phần hiển thị điểm riêng của hoạt động con nếu nó thuộc Series.<br>- Màn hình Tiến độ chuỗi (Series Progress) cần hiển thị: tiến độ mốc, số sự kiện tối thiểu, trạng thái đạt/chưa đạt (`minimumRequirementMet`), và số sự kiện còn thiếu để tránh bị phạt (`remainingToAvoidPenalty`). | **Cao** |
| **requiresSubmission = false** | Hoạt động không bắt buộc nộp bài vẫn có thể có task/assignment đi kèm. Tuy nhiên, các task này được coi là optional: **không tính điểm, không bị quá hạn (overdue penalty), không khóa completion**. | - Ở màn hình chi tiết hoạt động, ẩn cảnh báo quá hạn hoặc điểm phạt đối với các task optional.<br>- BE tự động chặn không cho tạo quy tắc tính điểm dạng submission cho hoạt động này. | **Trung bình** |
| **requiresSubmission = true** | Hoạt động hoàn thành (COMPLETED) khi và chỉ khi sinh viên **đã điểm danh (ATTENDED)** và **đã được chấm bài (GRADED)** (kể cả chấm trượt).<br>✅ **Order-independent finalization**: Sinh viên có thể nộp bài ngay cả khi chưa điểm danh. Hệ thống treo (defer) điểm cho đến khi **cả hai** điều kiện điểm danh & chấm bài đều đạt. Quy tắc phạt quá hạn (`TASK_OVERDUE`) sẽ lấy đúng theo `failPoints`. | - Form tạo hoạt động dạng này phải bắt buộc người dùng nhập `failPoints` (điểm khi trượt/quá hạn).<br>- Sinh viên không còn bị block nộp bài nếu chưa điểm danh (miễn là status `APPROVED` hoặc `ATTENDED`). | **Cao** |
| **Hai luồng quét QR điểm danh** | Tách biệt hoàn toàn:<br>1. **Activity QR** (`checkInCode`): Sinh viên tự quét -> lên thẳng trạng thái `ATTENDED` (điểm danh nhanh).<br>2. **Ticket QR** (`ticketCode`): Ban tổ chức quét ticket của sinh viên -> luồng transition stateful: quét lần 1 lên `CHECKED_IN`, quét lần 2 lên `ATTENDED`. | - Màn hình quét QR trên App của Sinh viên chỉ dùng để quét mã của hoạt động (Activity QR). Gọi endpoint `/api/registrations/checkin/qr`.<br>- Màn hình quét QR dành cho Ban tổ chức (Organizer) để check-in cho sinh viên. Gọi endpoint `/api/registrations/checkin`. | **Cao** |
| **Minigame & Đáp án** | Bổ sung cấu hình `showAnswers` (cho phép xem đáp án đúng sau khi nộp). MiniGame độc lập có thể cấu hình phạt khi hết lượt mà không pass (`MINIGAME_EXHAUSTED_ATTEMPTS`). | - Form tạo/sửa MiniGame: Thêm toggle `showAnswers` (Hiển thị đáp án đúng sau khi nộp).<br>- Màn hình xem lịch sử/chi tiết attempt của sinh viên: Kiểm tra cờ `showAnswers` từ backend trả về trước khi hiển thị đáp án đúng. Không tự ý render đáp án đúng nếu cờ này bằng `false`. | **Cao** |
| **Minigame Quiz Hierarchy** | Đã sửa lỗi mất dữ liệu khi lưu. Backend giờ đây lưu trọn vẹn cấu trúc `quiz -> questions -> options` khi tạo/cập nhật sự kiện Minigame. Khi cập nhật (`PATCH`), backend sẽ xóa các câu hỏi/đáp án cũ và tái tạo lại theo payload mới. Các `imageUrl` của câu hỏi cũng tự động được chuẩn hóa đường dẫn. | - Payload gọi API POST/PATCH Minigame cần truyền đầy đủ mảng `questions` và `options`.<br>- Vì backend áp dụng cơ chế xóa-tạo lại, nếu người dùng chỉ muốn sửa 1 câu hỏi, FE vẫn phải gửi lên toàn bộ danh sách câu hỏi hiện tại. | **Cao** |
| **No-show Penalty** | Preset `EVENT_BASIC` và `EVENT_WITH_SUBMISSION` mặc định bật No-show. Seminar mặc định tắt. Nếu bật No-show cho Seminar, hệ thống bắt buộc phạt sang loại điểm khác (không trừ ngược vào tích lũy chuyên đề chính). | - Form tạo hoạt động: Cho phép bật/tắt No-show và chọn loại điểm phạt phù hợp.<br>- Enforce validation loại điểm phạt của Seminar ở FE nếu bật. | **Trung bình** |
| **Xử lý quá hạn bài nộp** | Backend chuyển sang Quartz tự động quét và đánh dấu `OVERDUE` (không dùng cron hàng ngày). | - FE chỉ hiển thị trạng thái `OVERDUE` khi backend trả về trong status. Không tự viết logic so sánh ngày tháng ở FE để hiển thị trạng thái quá hạn. | **Thấp** |
| **Series `targetSemesterId`** | Admin có thể cấu hình trước học kỳ nào sẽ được dùng để cộng điểm thưởng (milestone) cho chuỗi sự kiện. Nếu gửi lên `null`, backend tự động tính toán học kỳ dựa trên thời gian diễn ra sự kiện đầu tiên của chuỗi. | - Form tạo/sửa Series: Thêm dropdown chọn học kỳ đích (`targetSemesterId`).<br>- Lưu ý: `SeriesResponse` hiện tại **đã trả về** `targetSemesterId` (đã fix backend). | **Trung bình** |
| **Series Progress List (Admin)** | Admin có thể xem danh sách tiến độ của tất cả sinh viên trong chuỗi với phân trang, tìm kiếm. | - Thêm màn hình Admin xem progress danh sách. Endpoint: `GET /api/series/{seriesId}/progress?page=&size=&keyword=`. | **Trung bình** |
| **Task Assignment Validation (P0)** | Backend **chặn** gán task cho sinh viên **không đăng ký** hoạt động sở hữu task. Trả về lỗi kèm danh sách `studentIds` chưa đăng ký. | - FE hiển thị lỗi validation rõ ràng khi gán task: "Students not registered for activity: [ids]".<br>- FE nên pre-filter danh sách sinh viên theo registration trước khi gọi API. | **Cao** |
| **Score History Pagination (P1)** | `GET /api/scores/history/student/{studentId}` dùng **DB-level pagination** (không còn load toàn bộ). Chạy nhanh hơn với dữ liệu lớn. Thêm filter `startDate`, `endDate`, `keyword`. | - FE truyền thêm query params `startDate`, `endDate`, `keyword` (tùy chọn).<br>- Response structure giữ nguyên `ScoreHistoryViewResponse`, FE không cần thay đổi logic parse. | **Trung bình** |
| **Preset Validation (P2)** | Backend **từ chối** gửi `scoreRules` tùy chỉnh kèm `presetCode` (không phải `CUSTOM`). Rules do preset sinh ra được đánh dấu `isPresetGenerated = true`. | - FE không gửi `scoreRules` khi dùng preset (chỉ gửi `presetConfig`).<br>- Hiển thị badge/tag "Preset" cho rules có `isPresetGenerated = true` để phân biệt với rules thủ công. | **Trung bình** |
| **Rich Preset Descriptors & UI Keys** | API `/presets` trả về `supportedRules` kèm chi tiết `FieldDefinition` (kiểu input, mặc định, bắt buộc,...). Backend áp dụng Single Source of Truth để xử lý fallback động (vd: tự động lấy điểm participation làm điểm phạt vắng mặt nếu để trống). | - FE dùng metadata trả về để **render dynamic form** thay vì hardcode giao diện cho từng preset.<br>- Dùng `ruleKey` (`PARTICIPATION_COMPLETED`, `NO_SHOW`, v.v.) làm key định danh ổn định trên UI. | **Cao** |
| **Async Recalculation (P3)** | Thêm API async recalculation chạy nền với batch processing, timeout protection, progress tracking. Thay thế sync cho bulk recalculation. | - FE dùng `POST /api/scores/recalculate/async` thay vì `/recalculate/all` cho bulk.<br>- Poll `GET /api/scores/recalculate/status/{jobId}` để hiển thị progress bar.<br>- Nếu job FAILED/TIMEOUT, FE cho phép retry qua `POST /api/scores/recalculate/retry/{jobId}`. | **Cao** |
| **Score Breakdown (P3)** | `GET /api/statistics/scores/breakdown` phân tích điểm theo `sourceType` (ACTIVITY_PARTICIPATION, MINIGAME_ATTEMPT, SERIES_PROGRESS, ...). | - FE thêm tab/section hiển thị breakdown biểu đồ tròn hoặc stacked bar.<br>- Student chỉ xem được của mình; Admin xem được tất cả hoặc filter theo `studentId`. | **Trung bình** |
| **Async Notifications (P3)** | Gửi notification bulk chạy async qua `@Async("notificationExecutor")`. Không block request khi gửi FCM cho nhiều sinh viên. | - FE không cần thay đổi API contract. Response time cải thiện đáng kể cho bulk notification. | **Thấp** |
| **ActivityPresetConfig mở rộng (P4)** | `ActivityPresetConfig` có thêm `audience`, `semesterPolicy`, `explicitSemesterId`, `departmentIds`. `buildRule()` đọc từ config thay vì hard-code `ALL_PARTICIPANTS`. | - Form preset: thêm section "Giới hạn đối tượng" với audience SELECT, departmentIds MULTI_SELECT, semesterPolicy SELECT, explicitSemesterId SELECT.<br>- Render từ `ACTIVITY_AUDIENCE` descriptor trong `supportedRules`.<br>- `visibility=audience_department_scoped` → chỉ hiện departmentIds khi audience != ALL_PARTICIPANTS.<br>- `visibility=semester_policy_explicit` → chỉ hiện explicitSemesterId khi semesterPolicy = EXPLICIT_SEMESTER. | **Cao** |
| **SeriesPresetConfig mở rộng (P4)** | `SeriesPresetConfig` thêm `audience`, `departmentIds`. Series scoring kiểm tra `isEligibleBySeriesAudience()`: student không thuộc khoa đã chọn sẽ KHÔNG nhận milestone/penalty điểm. | - Form tạo/sửa Series: thêm section audience + MULTI_SELECT khoa.<br>- Render từ `SERIES_AUDIENCE` descriptor trong `supportedRules`. | **Cao** |
| **supportedRules có thêm descriptors (P4)** | `GET /api/activities/presets` thêm `ACTIVITY_AUDIENCE`. `GET /api/series/presets` thêm `SERIES_AUDIENCE`. Mỗi descriptor có `FieldDefinition` với `inputType=MULTI_SELECT` và visibility mới. | - FE hỗ trợ `inputType=MULTI_SELECT` render thành multi-select dropdown (ví dụ: react-select multi).<br>- Xử lý visibility mới: `audience_department_scoped`, `semester_policy_explicit`. | **Cao** |
| **Score edit merge-by-key + rules-lock (P4)** | Khi edit score rules, BE merge theo key `(triggerType, scoreType, calculation)` thay vì delete-all+recreate → giữ nguyên rule ID. Nếu activity có ACTIVE score entries và không phải draft, BE từ chối sửa. | - FE không cần thay đổi logic gửi scoreRules.<br>- Hiển thị lỗi "Cannot modify score rules..." khi edit activity đã có entries. | **Cao** |
| **Type-lock + Standard update có type (P4)** | `StandardActivityUpdateRequest` giờ có field `type` (cho phép đổi type khi update). BE từ chối đổi type nếu activity có ACTIVE score entries và non-draft. | - Form edit Standard Activity: bỏ disable dropdown type.<br>- Hiển thị lỗi "Cannot change type..." nếu activity đã có entries. | **Cao** |
| **ActivityResponse/SeriesResponse có presetCode (P4)** | Response trả về `presetCode` (enum). `presetConfig` hiện để `null` (chưa lưu full config vào DB). | - Màn hình edit: dùng `presetCode` để pre-select preset.<br>- Dùng `scoreRules` hiện tại để reconstruct `presetConfig` nếu cần. | **Trung bình** |
| **Series update fixes (P4)** | `targetSemesterId` không còn bị kẹt trong `minimumPenaltyPoints`. `scoreType` không còn bắt buộc khi update series. | - FE không cần thay đổi logic. Partial update Series hoạt động đúng. | **Thấp** |
| **Series auto-register flags (P5)** | `ActivitySeries` có thêm `isImportant` (boolean, default `false`) và `mandatoryForFacultyStudents` (boolean, default `false`). Khi tạo/sửa Series non-draft, BE auto-register toàn bộ sinh viên active (nếu `isImportant=true`) và/或 sinh viên thuộc các khoa tổ chức (nếu `mandatoryForFacultyStudents=true`). Cờ này nằm **ở mức Series**, áp dụng cho main activity VÀ mỗi child activity mới được thêm vào chuỗi (khi non-draft). | - Form tạo/sửa Series: thêm 2 toggle "Sự kiện quan trọng" (`isImportant`) và "Bắt buộc với sinh viên khoa tổ chức" (`mandatoryForFacultyStudents`).<br>- `CreateSeriesRequest`, `UpdateSeriesRequest`, `SeriesResponse` đều có 2 field bool này.<br>- Child activity tạo qua `POST /api/series/{seriesId}/activities` **không** gửi 2 flag này (luôn `false` ở mức activity); auto-register cho child dùng flag của Series. | **Cao** |
| **Auto-register service extraction (P5)** | Auto-register logic được trích thành service chung `ActivityRegistrationAutoService`, dùng chung cho Standard, Minigame, Legacy activity create/update, và Series create/update. Auto-register **không bao giờ throw** — lỗi bị swallow nội bộ, không làm fail create/update. | - FE không có thay đổi API contract. Behavior auto-register nhất quán trên mọi type (Standard/Minigame/Legacy/Series).<br>- Nếu sinh viên đã có registration thì skip (idempotent). | **Thấp** |
| **Series isDraft (P5)** | `ActivitySeries` có thêm `isDraft` (boolean, default `true`). Auto-register chỉ chạy khi Series **non-draft** (`isDraft=false`). | - Form tạo/sửa Series: thêm toggle "Bản nháp" (`isDraft`).<br>- `CreateSeriesRequest`, `UpdateSeriesRequest`, `SeriesResponse` đều có field `isDraft`. | **Cao** |
| **Per-rule audience config (P5)** | `ActivityPresetConfig` thêm per-trigger override fields: `submissionAudience`, `noShowAudience`, `participationAudience`, `taskOverdueAudience`, `bonusAudience`, `minigamePassedAudience`, `minigameExhaustedAudience` (kèm `*SemesterPolicy`, `*ExplicitSemesterId`, `*DepartmentIds`). `buildRule()` ưu tiên per-rule override, fallback về top-level `audience`. | - Form preset config: thêm section cho phép cấu hình audience riêng theo từng trigger.<br>- Nếu không set per-rule, BE dùng `audience` top-level như cũ (backward compatible). | **Cao** |
| **Minigame dual-creation modes (P5)** | Có 2 cách tạo Minigame: (1) **All-at-once** qua `POST /api/activities/minigame` với `MinigameActivityCreateRequest` (shell + `quiz`) — backend tạo trọn Activity→MiniGame→Quiz→Questions→Options trong 1 call. (2) **Activity-first** — tạo activity trước (Standard hoặc Legacy `POST /api/activities`), rồi gắn quiz qua `POST /api/minigames` với `CreateMiniGameRequest` (chứa `activityId`). | - Flow (1): gửi `quiz` object đầy đủ; KHÔNG cần gọi thêm `POST /api/minigames`.<br>- Flow (2): tạo activity (type `MINIGAME` hoặc khác), lấy `id`, rồi gọi `POST /api/minigames` với `activityId` + quiz config.<br>- Cả 2 flow đều hợp lệ; chọn theo UX (form 1 bước vs 2 bước). | **Trung bình** |

### 2.2 Các thay đổi về Endpoint API

- **Endpoint Mới (`NEW`):**
  - `GET /api/activities/presets`: Lấy danh sách preset hoạt động gợi ý từ BE.
  - `POST /api/activities/presets/preview`: Preview cấu hình score rules do BE sinh trước khi tạo hoạt động.
  - `GET /api/series/presets`: Lấy danh sách preset chuỗi.
  - `POST /api/series/presets/preview`: Preview milestone/penalty của series.
  - `GET /api/series/{seriesId}/progress/my`: Student xem tiến độ chuỗi của chính mình (bao gồm thông tin phạt). **Trả về `Map<string, any>`, không có DTO typed cố định.**
  - `GET /api/series/{seriesId}/progress`: Admin xem danh sách tiến độ của tất cả SV (phân trang, tìm kiếm).
  - `GET /api/series/{seriesId}/registration/my`: Kiểm tra SV đã đăng ký chuỗi chưa.
  - `POST /api/series/{seriesId}/students/{studentId}/calculate-milestone`: Trigger tính lại milestone cho 1 SV.
  - `POST /api/registrations/checkin/qr`: Sinh viên tự quét Activity QR code.
  - `GET /api/scores/ranking`: Bảng xếp hạng điểm sinh viên (phân trang, filter theo khoa/lớp/loại điểm).
  - `POST /api/scores/recalculate/student/{studentId}`: Trigger tính lại điểm thủ công cho sinh viên.
  - `POST /api/scores/recalculate/all`: Trigger tính lại điểm cho toàn trường.
  - `POST /api/scores/recalculate/async`: Bắt đầu job tính lại điểm async (chạy nền, trả về `jobId`).
  - `GET /api/scores/recalculate/status/{jobId}`: Lấy trạng thái job recalculation (progress %, error count).
  - `POST /api/scores/recalculate/retry/{jobId}`: Retry job recalculation bị FAILED/TIMEOUT.
  - `GET /api/statistics/scores/breakdown`: Phân tích điểm theo `sourceType` (ACTIVITY_PARTICIPATION, MINIGAME_ATTEMPT, ...).
- **Endpoint Thay Đổi Contract (`MODIFIED`):**
  - `POST /api/series` & `PUT /api/series/{seriesId}`: Request body hỗ trợ các trường cấu hình phạt tối thiểu (`minimumRequirementEnabled`, `minimumRequiredEvents`, `minimumPenaltyPoints`) và `targetSemesterId`.
  - `POST /api/activities/standard` & `PUT /api/activities/standard/{id}`: Endpoint mới cho Standard Activity.
  - `POST /api/activities/minigame` & `PATCH /api/activities/minigame/{miniGameId}`: Endpoint mới cho Minigame Activity.
  - `GET /api/scores/history/student/{studentId}`: Thêm query params `startDate`, `endDate`, `keyword` (tùy chọn). Response structure giữ nguyên.
  - `ActivityScoreRuleRequest` & `ActivityScoreRuleResponse`: Thêm trường `isPresetGenerated` (Boolean, optional).
  - `ActivityPresetConfig`: Thêm `audience`, `semesterPolicy`, `explicitSemesterId`, `departmentIds`.
  - `SeriesPresetConfig`: Thêm `audience`, `departmentIds`.
  - `ActivityResponse`, `StandardActivityResponse`: Thêm `presetCode`, `presetConfig`.
  - `SeriesResponse`: Thêm `audience`, `targetDepartmentIds`, `presetCode`, `presetConfig`, `isImportant`, `mandatoryForFacultyStudents`, `isDraft`.
  - `StandardActivityUpdateRequest`: Thêm `type` (cho phép đổi type khi update).
  - `CreateSeriesRequest`, `UpdateSeriesRequest`: Thêm `audience`, `departmentIds`, `isImportant`, `mandatoryForFacultyStudents`, `isDraft`.
  - `ActivityPresetConfig`: Thêm per-rule override fields (`submissionAudience`, `submissionSemesterPolicy`, `submissionExplicitSemesterId`, `submissionDepartmentIds`, `participationAudience`, `noShowAudience`, `taskOverdueAudience`, `bonusAudience`, `minigamePassedAudience`, `minigameExhaustedAudience` + các field semester/department tương ứng).
  - `ScoreSemesterPolicy`: Không thay đổi (chỉ có `ACTIVITY_SEMESTER`, `EXPLICIT_SEMESTER`).
  - `FieldDefinition.inputType`: Thêm `MULTI_SELECT`.
  - `FieldDefinition.visibility`: Thêm `audience_department_scoped`, `semester_policy_explicit`.

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

> **Raw list endpoints** (không bọc `ApiResponse`): `GET /api/activities/score-type/{scoreType}`, `GET /api/activities/department/{deptId}`, `GET /api/activities/my`, `GET /api/activities/upcoming`, `GET /api/activities/month`.

### 3.3 Kiểu dữ liệu số dạng điểm số

Các trường điểm số ở backend được khai báo `BigDecimal` (Java). Jackson mặc định serialize `BigDecimal` thành JSON **number** (không phải string). Tuy nhiên, tùy cấu hình Jackson, có thể trả về string. **Frontend nên định nghĩa kiểu `number | string` cho các trường điểm số** để tránh lỗi parse.

Các trường bị ảnh hưởng:
- `points`, `failPoints`, `currentScore`, `oldScore`, `newScore`, `pointsEarned`, `score`, `totalMilestonePointsAwarded`

---

## 4. TypeScript Types & Enums Đầy Đủ

Frontend có thể copy-paste trực tiếp các định nghĩa TypeScript này vào thư mục `src/types/` hoặc `src/api/` để sử dụng.

### 4.1 Enums & Types Định Danh

```ts
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

### 4.2 Interfaces Dữ Liệu

#### Hoạt Động (Activity & Presets)

```ts
export interface ActivityScoreRuleRequest {
  scoreType: ScoreType;
  triggerType: ScoreRuleTrigger;
  calculation: ScoreRuleCalculation;
  points: number | string; // BigDecimal
  failPoints?: number | string | null;
  audience: ScoreRuleAudience;
  semesterPolicy: ScoreSemesterPolicy;
  explicitSemesterId?: number | null;
  departmentIds?: number[];
  enabled?: boolean | null;
  isPresetGenerated?: boolean | null; // true nếu rule do preset sinh ra
}

export interface ActivityScoreRuleResponse {
  id: number;
  activityId: number;
  scoreType: ScoreType;
  triggerType: ScoreRuleTrigger;
  calculation: ScoreRuleCalculation;
  points: number | string; // BigDecimal
  failPoints?: number | string | null;
  audience: ScoreRuleAudience;
  semesterPolicy: ScoreSemesterPolicy;
  explicitSemesterId?: number | null;
  targetDepartmentIds: number[];
  enabled?: boolean | null;
  isPresetGenerated?: boolean | null; // true nếu rule do preset sinh ra
}

export interface ActivityPresetConfig {
  primaryScoreType?: ScoreType | null;
  participationPoints?: number | string | null; // BigDecimal
  participationFailPoints?: number | string | null;
  noShowPenaltyEnabled?: boolean | null;
  noShowPenaltyPoints?: number | string | null;
  noShowPenaltyScoreType?: ScoreType | null;
  submissionPassPoints?: number | string | null;
  submissionFailPoints?: number | string | null;
  taskOverduePenaltyPoints?: number | string | null;
  minigameExhaustedPenaltyPoints?: number | string | null;
  bonusScoreType?: ScoreType | null;
  bonusPoints?: number | string | null;

  // Top-level (fallback cho tất cả rules)
  audience?: ScoreRuleAudience | null;
  semesterPolicy?: ScoreSemesterPolicy | null;
  explicitSemesterId?: number | null;
  departmentIds?: number[] | null;

  // Per-rule overrides (P5.1) — ưu tiên hơn top-level fields
  submissionAudience?: ScoreRuleAudience | null;
  submissionSemesterPolicy?: ScoreSemesterPolicy | null;
  submissionExplicitSemesterId?: number | null;
  submissionDepartmentIds?: number[] | null;

  participationAudience?: ScoreRuleAudience | null;
  participationSemesterPolicy?: ScoreSemesterPolicy | null;
  participationExplicitSemesterId?: number | null;
  participationDepartmentIds?: number[] | null;

  noShowAudience?: ScoreRuleAudience | null;
  noShowSemesterPolicy?: ScoreSemesterPolicy | null;
  noShowExplicitSemesterId?: number | null;
  noShowDepartmentIds?: number[] | null;

  taskOverdueAudience?: ScoreRuleAudience | null;
  taskOverdueSemesterPolicy?: ScoreSemesterPolicy | null;
  taskOverdueExplicitSemesterId?: number | null;
  taskOverdueDepartmentIds?: number[] | null;

  bonusAudience?: ScoreRuleAudience | null;
  bonusSemesterPolicy?: ScoreSemesterPolicy | null;
  bonusExplicitSemesterId?: number | null;
  bonusDepartmentIds?: number[] | null;

  minigamePassedAudience?: ScoreRuleAudience | null;
  minigamePassedSemesterPolicy?: ScoreSemesterPolicy | null;
  minigamePassedExplicitSemesterId?: number | null;
  minigamePassedDepartmentIds?: number[] | null;

  minigameExhaustedAudience?: ScoreRuleAudience | null;
  minigameExhaustedSemesterPolicy?: ScoreSemesterPolicy | null;
  minigameExhaustedExplicitSemesterId?: number | null;
  minigameExhaustedDepartmentIds?: number[] | null;
}

export interface CreateActivityRequest {
  name: string;
  type: ActivityType;
  presetCode?: ActivityPresetCode | null;
  presetConfig?: ActivityPresetConfig | null;
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
  presetCode?: ActivityPresetCode | null;
  presetConfig?: ActivityPresetConfig | null;
  activeScoreEntryCount: number;
  createdAt?: string | null;
  updatedAt?: string | null;
  createdBy?: string | null;
  lastModifiedBy?: string | null;
}
```

#### Standard Activity (Hoạt động truyền thống)

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

export interface StandardActivityUpdateRequest {
  name?: string | null;
  type?: ActivityType | null;
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
  presetCode?: ActivityPresetCode | null;
  presetConfig?: ActivityPresetConfig | null;
  activeScoreEntryCount: number;
  createdAt?: string | null;
  updatedAt?: string | null;
  createdBy?: string | null;
  lastModifiedBy?: string | null;
}
```

#### Minigame Activity (Hoạt động kèm Minigame / Quiz)

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
  // Không extends MinigameActivityCreateRequest trong Java. Là class standalone.
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

#### Series Child Activity (Hoạt động con trong chuỗi)

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
  // Không extends SeriesChildActivityCreateRequest trong Java. Là class standalone.
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

#### Activity Summary (Dùng cho API danh sách để tối ưu performance)

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

#### Preset Definitions

```ts
export interface FieldDefinition {
  fieldName: string;
  label: string;
  inputType: 'NUMBER' | 'BOOLEAN' | 'SELECT' | 'MAP' | 'MULTI_SELECT';
  required: boolean;
  defaultValue: any;
  visibility: 'ALWAYS' | 'rule_enabled' | 'audience_department_scoped' | 'semester_policy_explicit';
  options?: string[] | null;
}

export interface PresetRuleDescriptor {
  ruleKey: string;
  label: string;
  description: string;
  required: boolean;
  enabledByDefault: boolean;
  fieldDefinitions: FieldDefinition[];
}

export interface ActivityPresetDefinitionResponse {
  code: ActivityPresetCode;
  displayName: string;
  description: string;
  recommendedActivityTypes: ActivityType[];
  defaultRequiresSubmission?: boolean | null;
  notes: string[];
  supportedRules: PresetRuleDescriptor[];
}

export interface ActivityPresetPreviewResponse {
  presetCode: ActivityPresetCode;
  activityType: ActivityType;
  requiresSubmission: boolean;
  scoreRules: ActivityScoreRuleRequest[];
  notes: string[];
}

export interface SeriesPresetDefinitionResponse {
  code: SeriesPresetCode;
  displayName: string;
  description: string;
  notes: string[];
  supportedRules: PresetRuleDescriptor[];
}

export interface SeriesPresetPreviewResponse {
  presetCode: SeriesPresetCode;
  scoreType: ScoreType;
  milestonePoints: Record<number, number>;
  minimumRequirementEnabled?: boolean | null;
  minimumRequiredEvents?: number | null;
  minimumPenaltyPoints?: number | null;
  notes: string[];
  // Không có targetSemesterId trong DTO này
}
```

#### Chuỗi Sự Kiện (Series & Progress)

```ts
export interface SeriesPresetConfig {
  primaryScoreType?: ScoreType | null;
  milestonePoints?: Record<number, number>;
  minimumRequirementEnabled?: boolean | null;
  minimumRequiredEvents?: number | null;
  minimumPenaltyPoints?: number | null;
  audience?: ScoreRuleAudience | null;
  departmentIds?: number[] | null;
}

export interface CreateSeriesRequest {
  name: string;
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
  audience?: ScoreRuleAudience | null;
  departmentIds?: number[] | null;
  isImportant?: boolean | null;                    // P5: auto-register toàn bộ SV active
  mandatoryForFacultyStudents?: boolean | null;    // P5: auto-register SV các khoa tổ chức
  isDraft?: boolean | null;                        // P5.1: bản nháp
  presetCode?: SeriesPresetCode | null;
  presetConfig?: SeriesPresetConfig | null;
}

export interface UpdateSeriesRequest {
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
  audience?: ScoreRuleAudience | null;
  departmentIds?: number[] | null;
  isImportant?: boolean | null;
  mandatoryForFacultyStudents?: boolean | null;
  isDraft?: boolean | null;                        // P5.1
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
  minimumPenaltyPoints?: number | null;
  targetSemesterId?: number | null;
  audience?: ScoreRuleAudience | null;
  targetDepartmentIds?: number[] | null;
  isImportant: boolean;                 // P5
  mandatoryForFacultyStudents: boolean; // P5
  isDraft: boolean;                     // P5.1 — mặc định true
  presetCode?: SeriesPresetCode | null;
  presetConfig?: SeriesPresetConfig | null;
  createdAt?: string | null;
  // ✅ targetSemesterId đã có trong SeriesResponse (đã fix backend)
}

export interface SeriesOverviewResponse {
  seriesId: number;
  seriesName: string;
  description?: string | null;
  scoreType: ScoreType;
  targetSemesterId?: number | null;
  milestonePoints: string; // JSON string
  milestonePointsMap: Record<string, number>; // Parsed map
  registrationStartDate?: string | null;
  registrationDeadline?: string | null;
  requiresApproval?: boolean | null;
  ticketQuantity?: number | null;
  minimumRequirementEnabled?: boolean | null;
  minimumRequiredEvents?: number | null;
  minimumPenaltyPoints?: number | null;
  createdAt?: string | null;

  totalActivities: number;
  totalRegisteredStudents: number;
  totalCompletedStudents: number; // Hoàn thành TẤT CẢ activities
  completionRate: number;
  totalMilestonePointsAwarded: number | string; // BigDecimal
  minimumRequirementMetCount: number;

  milestoneProgress: MilestoneProgressItem[];
  activityStats: ActivityStatItem[];
}

export interface MilestoneProgressItem {
  milestoneKey: string;
  milestoneCount: number;
  milestonePoints: number;
  studentCount: number;
  percentage: number;
}

export interface ActivityStatItem {
  activityId: number;
  activityName: string;
  order: number;
  registrationCount: number;
  participationCount: number;
  participationRate: number;
}

export interface SeriesProgressListResponse {
  seriesId: number;
  seriesName: string;
  totalActivities: number;
  totalRegistered: number;
  progressList: SeriesProgressItemResponse[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

export interface SeriesProgressItemResponse {
  studentId: number;
  studentCode: string;
  studentName: string;
  className?: string | null;
  departmentName?: string | null;
  completedCount: number;
  totalActivities: number;
  pointsEarned: number | string; // BigDecimal
  currentMilestone?: string | null;
  completedActivityIds: number[];
  lastUpdated?: string | null;
  isRegistered?: boolean | null;
}

// ⚠️ KHÔNG CÓ DTO SeriesStudentProgressView trong backend.
// GET /api/series/{seriesId}/progress/my trả về Map<string, any>.
// Các key của Map:
export interface SeriesStudentProgressMap {
  studentId: number;
  seriesId: number;
  seriesName: string;
  completedCount: number;
  totalActivities: number;
  completedActivityIds: number[];
  pointsEarned: number | string; // BigDecimal
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

#### Đăng Ký & Điểm Danh (Registration & Check-in)

```ts
export interface ActivityRegistrationRequest {
  activityId: number;
}

export interface ActivityParticipationRequest {
  ticketCode: string;
  studentId: number;
  participationType?: ParticipationType | null;
  pointsEarned?: number | string | null;
}

export interface RegistrationResponse {
  id: number;
  activityId: number;
  studentId: number;
  status: RegistrationStatus;
  ticketCode: string;
  registeredAt: string;
}

export interface AppliedScoreAward {
  ruleId?: number | null;
  scoreType: ScoreType;
  scoreTypeLabel: string;
  points: number | string; // BigDecimal
  displayUnit: string;
  displayText: string; // e.g., "+5 điểm rèn luyện"
  triggerType?: ScoreRuleTrigger | null;
  scoreEntryId?: number | null;
}

export interface ActivityParticipationResponse {
  id: number;
  registrationId: number;
  activityId: number;
  activityName: string;
  studentId: number;
  studentName: string;
  participationType: ParticipationType;
  pointsEarned: number | string; // Tổng điểm cộng gộp (tương thích ngược)
  scoreAwards: AppliedScoreAward[]; // Danh sách điểm chi tiết hiển thị UI
  date: string;
  isCompleted?: boolean | null;
  notes?: string | null;
}
```

#### Điểm Số & Lịch Sử Điểm (Score & History)

```ts
export interface ScoreHistoryDetailResponse {
  id: number;
  oldScore: number | string; // BigDecimal
  newScore: number | string; // BigDecimal
  changeDate: string;
  reason?: string | null;
  activityId?: number | null;
  activityName?: string | null;
  seriesId?: number | null;
  seriesName?: string | null;
  sourceType: string; // enum name
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
  pointsEarned?: number | string | null; // BigDecimal
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
  currentScore: number | string; // BigDecimal
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
  score: number | string; // BigDecimal
}

export interface ScoreBreakdownItem {
  sourceType: ScoreEntrySourceType; // enum name
  totalPoints: number | string; // BigDecimal
  entryCount: number;
  percentage: number; // 0-100
}

export interface ScoreBreakdownResponse {
  studentId: number;
  semesterId: number;
  scoreType?: ScoreType | null;
  totalScore: number | string; // BigDecimal
  breakdown: ScoreBreakdownItem[];
}

export type RecalculationJobStatus = "PENDING" | "RUNNING" | "COMPLETED" | "FAILED" | "TIMEOUT";

export interface RecalculationJobResponse {
  id: number;
  semesterId: number;
  status: RecalculationJobStatus;
  totalStudents: number;
  processedStudents: number;
  errorCount: number;
  progressPercent: number; // 0-100
  startedAt?: string | null;
  completedAt?: string | null;
  createdAt: string;
  errorDetails?: string | null;
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

#### MiniGame Quiz (Standalone API)

```ts
export interface CreateMiniGameRequest {
  activityId?: number | null;
  title: string;
  description?: string | null;
  questionCount: number;
  timeLimit: number;
  requiredCorrectAnswers: number;
  maxAttempts: number;
  showAnswers?: boolean | null;
  questions: QuestionRequest[];
}

export interface UpdateMiniGameRequest {
  title: string;
  description?: string | null;
  questionCount: number;
  timeLimit: number;
  requiredCorrectAnswers: number;
  maxAttempts: number;
  showAnswers?: boolean | null;
  questions: QuestionRequest[];
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
  status: string; // e.g. "IN_PROGRESS"
  startedAt: string;
  timeLimit: number;
}

export interface SubmitAttemptResponse {
  id: number;
  status: string; // "PASSED", "FAILED"
  correctCount: number;
  totalQuestions: number;
  pointsEarned: number | string; // BigDecimal
  startedAt: string;
  submittedAt: string;
  requiredCorrectAnswers: number;
  participation: unknown | null;
}

export interface QuizOptionDetailResponse {
  id: number;
  text: string;
  isCorrect?: boolean | null; // null nếu showAnswers=false
  isSelected: boolean;
}

export interface QuizQuestionDetailResponse {
  id: number;
  questionText: string;
  imageUrl?: string | null;
  displayOrder?: number | null;
  options: QuizOptionDetailResponse[];
  correctOptionId?: number | null; // null nếu showAnswers=false
  selectedOptionId?: number | null;
  isCorrect?: boolean | null;
}

export interface AttemptDetailResponse {
  id: number;
  status: string;
  correctCount: number;
  totalQuestions: number;
  pointsEarned: number | string; // BigDecimal
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
- **Method:** `GET`
- **Path:** `/api/activities/presets`
- **Response:** `ApiResponse<ActivityPresetDefinitionResponse[]>`

#### 2. Preview cấu hình Score Rules cho hoạt động
- **Method:** `POST`
- **Path:** `/api/activities/presets/preview`
- **Request:** `ActivityPresetPreviewRequest`
- **Response:** `ApiResponse<ActivityPresetPreviewResponse>`

#### 3. Lấy danh sách preset chuỗi sự kiện
- **Method:** `GET`
- **Path:** `/api/series/presets`
- **Response:** `ApiResponse<SeriesPresetDefinitionResponse[]>`

#### 4. Preview cấu hình chuỗi sự kiện
- **Method:** `POST`
- **Path:** `/api/series/presets/preview`
- **Request:** `SeriesPresetPreviewRequest`
- **Response:** `ApiResponse<SeriesPresetPreviewResponse>`

---

### 5.2 Nhóm API Quản Lý Hoạt Động (Activity Management)

#### 1. Tạo hoạt động mới (Legacy)
- **Method:** `POST`
- **Path:** `/api/activities`
- **Request:** `CreateActivityRequest`
- **Response:** `ApiResponse<ActivityResponse>`

#### 2. Cập nhật hoạt động (Legacy)
- **Method:** `PUT`
- **Path:** `/api/activities/{id}`
- **Request:** `CreateActivityRequest`
- **Response:** `ApiResponse<ActivityResponse>`

#### 3. Danh sách hoạt động trả về dạng bọc Response
- **Endpoints:** `GET /api/activities`, `GET /api/activities/{id}`
- **Response:** `ApiResponse<ActivityResponse>` hoặc `ApiResponse<ActivityResponse[]>`

#### 4. Danh sách hoạt động dạng Raw List (Không bọc Response)
> **IMPORTANT:** Các endpoint dưới đây backend trả thẳng danh sách `ActivityResponse[]`, không bọc qua `ApiResponse`.
- `GET /api/activities/score-type/{scoreType}`
- `GET /api/activities/department/{deptId}`
- `GET /api/activities/my`
- `GET /api/activities/upcoming?keyword=` (opt)
- `GET /api/activities/month?year=&month=` (opt)

#### 5. Tạo Standard Activity (New)
- **Method:** `POST`
- **Path:** `/api/activities/standard`
- **Request:** `StandardActivityCreateRequest`
- **Response:** `ApiResponse<StandardActivityResponse>`

#### 6. Cập nhật Standard Activity (New)
- **Method:** `PUT`
- **Path:** `/api/activities/standard/{id}`
- **Request:** `StandardActivityUpdateRequest`
- **Response:** `ApiResponse<StandardActivityResponse>`

#### 7. Tạo Minigame Activity (New) ✅ Fixed: now persists quiz
- **Method:** `POST`
- **Path:** `/api/activities/minigame`
- **Request:** `MinigameActivityCreateRequest`
- **Response:** `ApiResponse<MinigameActivityResponse>`
- **Lưu ý:** Request **phải** có `quiz.questions[]` với đầy đủ `options[]` cho mỗi câu hỏi. Backend tạo: `MiniGameQuiz` → `MiniGameQuizQuestion[]` → `MiniGameQuizOption[]`. Validator chặn nếu không có quiz hoặc thiếu options.

#### 8. Cập nhật Minigame Activity (New) ✅ Fixed: rebuilds quiz
- **Method:** `PATCH`
- **Path:** `/api/activities/minigame/{id}`
- **Request:** `MinigameActivityUpdateRequest`
- **Response:** `ApiResponse<MinigameActivityResponse>`
- **Lưu ý:** Nếu payload có `quiz.questions[]`, backend **xóa toàn bộ** questions/options cũ (kèm answers của student) và tái tạo theo payload mới. FE phải gửi đầy đủ danh sách questions, không chỉ gửi questions cần sửa.

---

### 5.3 Nhóm API Chuỗi Sự Kiện (Series Management)

#### 1. Tạo chuỗi sự kiện mới
- **Method:** `POST`
- **Path:** `/api/series`
- **Request:** `CreateSeriesRequest`
- **Response:** `ApiResponse<SeriesResponse>`

#### 2. Cập nhật chuỗi sự kiện
- **Method:** `PUT`
- **Path:** `/api/series/{seriesId}`
- **Request:** `UpdateSeriesRequest`
- **Response:** `ApiResponse<SeriesResponse>`

#### 3. Student đăng ký tham gia toàn bộ chuỗi
- **Method:** `POST`
- **Path:** `/api/series/{seriesId}/register`
- **Response:** `ApiResponse<RegistrationResponse[]>`

#### 4. Student tự xem tiến độ trong chuỗi
- **Method:** `GET`
- **Path:** `/api/series/{seriesId}/progress/my`
- **Response:** `ApiResponse<Map<string, any>>` (không có DTO typed cố định)

#### 5. Admin xem tiến độ chuỗi của một sinh viên
- **Method:** `GET`
- **Path:** `/api/series/{seriesId}/students/{studentId}/progress`
- **Response:** `ApiResponse<Map<string, any>>` (giống progress/my)

#### 6. Admin xem danh sách tiến độ tất cả sinh viên (phân trang)
- **Method:** `GET`
- **Path:** `/api/series/{seriesId}/progress`
- **Query:** `page`, `size`, `keyword` (opt)
- **Response:** `ApiResponse<SeriesProgressListResponse>`

#### 7. Admin xem tổng quan thống kê của chuỗi
- **Method:** `GET`
- **Path:** `/api/series/{seriesId}/overview`
- **Response:** `ApiResponse<SeriesOverviewResponse>`

#### 8. Tạo activity con trong chuỗi
- **Method:** `POST`
- **Path:** `/api/series/{seriesId}/activities`
- **Request:** `SeriesChildActivityCreateRequest`
- **Response:** `ApiResponse<SeriesChildActivityResponse>`

#### 9. Cập nhật activity con trong chuỗi
- **Method:** `PUT`
- **Path:** `/api/series/{seriesId}/activities/{activityId}`
- **Request:** `SeriesChildActivityUpdateRequest`
- **Response:** `ApiResponse<SeriesChildActivityResponse>`

#### 10. Gắn activity đã có sẵn vào chuỗi
- **Method:** `POST`
- **Path:** `/api/series/{seriesId}/activities/attach`
- **Request:** `AddActivityToSeriesRequest`
- **Response:** `ApiResponse<ActivityResponse>`

---

### 5.4 Nhóm API Đăng Ký & Quét QR Điểm Danh (Registration & Check-in)

#### 1. Sinh viên điểm danh nhanh qua Activity QR Code (Student quét)
- **Method:** `POST`
- **Path:** `/api/registrations/checkin/qr`
- **Request:** `{ checkInCode: string }`
- **Response:** `ApiResponse<null>` (hoặc thông tin participation)

#### 2. Ban tổ chức điểm danh qua Ticket QR Code của Sinh viên (Organizer quét)
- **Method:** `POST`
- **Path:** `/api/registrations/checkin`
- **Request:** `ActivityParticipationRequest`
- **Response:** `ApiResponse<ActivityParticipationResponse>`

#### 3. Validate ticketCode trước khi check-in
- **Method:** `GET`
- **Path:** `/api/registrations/checkin/validate`
- **Query:** `ticketCode`
- **Response:** `ApiResponse<Map>`

#### 4. Lấy danh sách đăng ký của sinh viên
- **Method:** `GET`
- **Path:** `/api/registrations/my`
- **Response:** `ApiResponse<RegistrationResponse[]>`

#### 5. Lấy danh sách đăng ký theo sự kiện (Admin/Manager)
- **Method:** `GET`
- **Path:** `/api/registrations/activity/{activityId}`
- **Response:** `ApiResponse<RegistrationResponse[]>`

#### 6. Lấy danh sách đăng ký theo chuỗi sự kiện (Admin/Manager)
- **Method:** `GET`
- **Path:** `/api/registrations/series/{seriesId}`
- **Response:** `ApiResponse<RegistrationResponse[]>`

#### 7. Cập nhật trạng thái đăng ký (Admin/Manager)
- **Method:** `PUT`
- **Path:** `/api/registrations/{registrationId}/status`
- **Query:** `status`
- **Response:** `ApiResponse<RegistrationResponse>`

#### 8. Chấm điểm completion (đạt/không đạt)
- **Method:** `PUT`
- **Path:** `/api/registrations/participations/{participationId}/grade`
- **Query:** `isCompleted`, `notes` (opt)
- **Response:** `ApiResponse<ActivityParticipationResponse>`

#### 9. Lấy báo cáo tham gia / chưa tham gia (Admin/Manager)
- **Method:** `GET`
- **Path:** `/api/registrations/activities/{activityId}/report`
- **Response:** `ApiResponse<Map>`

#### 10. Lấy danh sách participations theo activityId
- **Method:** `GET`
- **Path:** `/api/registrations/activities/{activityId}/participations`
- **Response:** `ApiResponse<ActivityParticipationResponse[]>`

#### 11. Lấy danh sách đăng ký theo status của 1 sinh viên
- **Method:** `GET`
- **Path:** `/api/registrations/my/{status}`
- **Response:** `ApiResponse<RegistrationResponse[]>`

#### 12. Tìm kiếm đăng ký
- **Method:** `GET`
- **Path:** `/api/registrations/search`
- **Query:** `keyword`, `status` (opt)
- **Response:** `ApiResponse<RegistrationResponse[]>`

#### 13. Backfill participations còn thiếu
- **Method:** `POST`
- **Path:** `/api/registrations/backfill/participations`
- **Response:** `ApiResponse<Map>`

#### 14. Lịch cá nhân (sự kiện đã tham gia)
- **Method:** `GET`
- **Path:** `/api/registrations/personal-calendar`
- **Response:** `ApiResponse<Map>`

---

### 5.5 Nhóm API MiniGame Quiz

#### 1. Tạo mới MiniGame cho hoạt động
- **Method:** `POST`
- **Path:** `/api/minigames`
- **Request:** `CreateMiniGameRequest`
- **Response:** `ApiResponse<MiniGameResponse>`

#### 2. Lấy thông tin MiniGame của hoạt động
- **Method:** `GET`
- **Path:** `/api/minigames/activity/{activityId}`
- **Response:** `ApiResponse<MiniGameResponse>`

#### 3. Kiểm tra xem activity đã có minigame/quiz chưa
- **Method:** `GET`
- **Path:** `/api/minigames/activity/{activityId}/check`
- **Response:** `ApiResponse<boolean>`

#### 4. Bắt đầu lượt chơi MiniGame
- **Method:** `POST`
- **Path:** `/api/minigames/{miniGameId}/start`
- **Response:** `ApiResponse<StartAttemptResponse>`

#### 5. Nộp kết quả MiniGame
- **Method:** `POST`
- **Path:** `/api/minigames/attempts/{attemptId}/submit`
- **Request:** `{ answers: { [questionId: string]: optionId } }`
- **Response:** `ApiResponse<SubmitAttemptResponse>`

#### 6. Xem chi tiết lượt thử (Quiz Details)
- **Method:** `GET`
- **Path:** `/api/minigames/attempts/{attemptId}`
- **Response:** `ApiResponse<AttemptDetailResponse>`
- **Chú ý:** Nếu cờ `showAnswers` của minigame là `false`, backend sẽ **không** trả về cờ `isCorrect` hoặc đáp án đúng trong list options câu hỏi. FE chỉ hiển thị phương án sinh viên đã chọn, không hiển thị đáp án đúng.

#### 7. Lấy danh sách câu hỏi (không có đáp án đúng)
- **Method:** `GET`
- **Path:** `/api/minigames/{miniGameId}/questions`
- **Response:** `ApiResponse<QuizQuestionDetailResponse[]>`

#### 8. Lấy danh sách câu hỏi cho admin chỉnh sửa (có đáp án đúng)
- **Method:** `GET`
- **Path:** `/api/minigames/{miniGameId}/questions/edit`
- **Response:** `ApiResponse<QuizQuestionEditResponse[]>`

#### 9. Lấy lịch sử attempts của student
- **Method:** `GET`
- **Path:** `/api/minigames/{miniGameId}/attempts/my`
- **Response:** `ApiResponse<AttemptDetailResponse[]>`

#### 10. Cập nhật MiniGame
- **Method:** `PUT`
- **Path:** `/api/minigames/{miniGameId}`
- **Request:** `UpdateMiniGameRequest`
- **Response:** `ApiResponse<MiniGameResponse>`

#### 11. Xóa MiniGame (soft delete)
- **Method:** `DELETE`
- **Path:** `/api/minigames/{miniGameId}`
- **Response:** `ApiResponse<null>`

#### 12. Lấy tất cả minigames (Admin/Manager)
- **Method:** `GET`
- **Path:** `/api/minigames`
- **Response:** `ApiResponse<MiniGameResponse[]>`

---

### 5.6 Nhóm API Điểm Số & Xếp Hạng (Score & Ranking)

#### 1. Lấy bảng xếp hạng điểm sinh viên (Ranking)
- **Method:** `GET`
- **Path:** `/api/scores/ranking`
- **Query:** `semesterId` (req), `scoreType` (opt), `departmentId` (opt), `classId` (opt), `sortOrder` (opt, default: "DESC")
- **Response:** `ApiResponse<Map>` (với `rankings: StudentRankResponse[]`)

#### 2. Trigger tính toán lại điểm thủ công cho một sinh viên
- **Method:** `POST`
- **Path:** `/api/scores/recalculate/student/{studentId}`
- **Query:** `semesterId` (opt)
- **Response:** `ApiResponse<null>`
- **Lưu ý:** API chạy **đồng bộ (Synchronous)**.

#### 3. Trigger tính toán lại điểm cho tất cả sinh viên
- **Method:** `POST`
- **Path:** `/api/scores/recalculate/all`
- **Query:** `semesterId` (opt)
- **Response:** `ApiResponse<Map>` (với `successCount`, `errorCount`, `errors`)
- **Lưu ý:** API chạy **đồng bộ (Synchronous)**. Thời gian phản hồi có thể kéo dài. **Khuyến nghị dùng async endpoint bên dưới thay thế.**

#### 4. Bắt đầu job tính lại điểm async (New)
- **Method:** `POST`
- **Path:** `/api/scores/recalculate/async`
- **Query:** `semesterId` (opt)
- **Response:** `ApiResponse<Map>` (với `jobId`, `semesterId`, `totalStudents`, `status`)
- **Lưu ý:** Job chạy nền, FE poll status endpoint để theo dõi tiến độ.

#### 5. Lấy trạng thái job recalculation (New)
- **Method:** `GET`
- **Path:** `/api/scores/recalculate/status/{jobId}`
- **Response:** `ApiResponse<RecalculationJobResponse>`
- **Lưu ý:** Response chứa `progressPercent` (0-100), `processedStudents`, `errorCount`, `status`.

#### 6. Retry job recalculation bị FAILED/TIMEOUT (New)
- **Method:** `POST`
- **Path:** `/api/scores/recalculate/retry/{jobId}`
- **Response:** `ApiResponse<Map>` (job info mới, giống async start)

#### 7. Xem lịch sử điểm của student
- **Method:** `GET`
- **Path:** `/api/scores/history/student/{studentId}`
- **Query:** `semesterId` (req), `scoreType` (opt), `page`, `size`, `startDate` (opt, ISO datetime), `endDate` (opt, ISO datetime), `keyword` (opt)
- **Response:** `ApiResponse<ScoreHistoryViewResponse>`
- **Lưu ý:** Student chỉ có thể xem lịch sử của chính mình. Backend dùng DB-level pagination, không load toàn bộ dữ liệu.

#### 8. Xem điểm theo loại (Score View)
- **Method:** `GET`
- **Path:** `/api/scores/student/{studentId}/semester/{semesterId}`
- **Response:** `ApiResponse<ScoreViewResponse>`

#### 9. Xem tổng điểm (Total Score)
- **Method:** `GET`
- **Path:** `/api/scores/student/{studentId}/semester/{semesterId}/total`
- **Response:** `ApiResponse<Map>` (với `grandTotal`, `totalsByType`)

#### 10. Phân tích điểm theo nguồn (Score Breakdown) (New)
- **Method:** `GET`
- **Path:** `/api/statistics/scores/breakdown`
- **Query:** `semesterId` (opt), `studentId` (opt), `departmentId` (opt)
- **Response:** `ApiResponse<ScoreBreakdownResponse>`
- **Lưu ý:** Student chỉ xem được của mình; Admin xem tất cả hoặc filter theo `studentId`.

---

## 6. Lời Khuyên Cấu Trúc Code Frontend

Team Frontend nên phân chia các file TypeScript theo hướng module hóa để dễ quản lý và cập nhật:

1. **Thư mục `src/types/`**:
   - `presets.ts`: Khai báo các loại Preset, Preset Config cho Activity và Series.
   - `activity.ts`: Định nghĩa Create/Update Request và Activity Response (Standard, Minigame, SeriesChild, Legacy, Summary).
   - `series.ts`: Định nghĩa Create/Update Request, SeriesResponse, SeriesOverviewResponse, SeriesProgressListResponse, SeriesProgressItemResponse.
   - `score.ts`: Định nghĩa ScoreHistory, ActivityParticipation, StudentRank, ScoreView, ScoreBreakdownResponse, RecalculationJobResponse.
   - `submission.ts`: Định nghĩa các cấu trúc bài nộp.
   - `minigame.ts`: Định nghĩa các request/response cho Quiz (standalone API).

2. **Thư mục `src/api/`**:
   Tạo các hàm fetch/post bọc Axios, ví dụ:
   ```ts
   import axios from 'axios';
   import { ApiResponse } from '@/types/common';
   import { SeriesProgressListResponse } from '@/types/series';

   export const getSeriesProgressList = async (
     seriesId: number, page = 0, size = 20, keyword?: string
   ): Promise<SeriesProgressListResponse> => {
     const response = await axios.get<ApiResponse<SeriesProgressListResponse>>(
       `/api/series/${seriesId}/progress`, { params: { page, size, keyword } }
     );
     return response.data.body;
   };
   ```

3. **Cảnh giác (Common Pitfalls) ở FE:**
   - Không tự động thêm prefix `/uploads` vào ảnh; backend trả về full link ảnh public.
   - Các API dạng raw list (`GET /api/activities/my`, `/upcoming`, `/month`, `/score-type/*`, `/department/*`) **không** dùng `ApiResponse` wrapper, hãy xử lý trực tiếp payload danh sách.
   - Luôn sử dụng kiểu dữ liệu `number | string` ở FE cho các trường chứa điểm (ví dụ: `pointsEarned: number | string`) để tương thích với `BigDecimal` phía backend, tránh bị lỗi parse khi Jackson trả về number hoặc string.
   - `GET /api/series/{seriesId}/progress/my` và `GET /api/series/{seriesId}/students/{studentId}/progress` trả về `Map<string, any>`, không có DTO typed cố định. FE nên khai báo interface `SeriesStudentProgressMap` để type-safe.
   - `SeriesResponse` hiện tại **đã có `targetSemesterId`** (đã fix backend).
   - Tất cả `*UpdateRequest` trong Java đều là **standalone class**, không extends CreateRequest. FE có thể dùng `Partial<CreateRequest>` nhưng cần aware rằng backend không có inheritance.

---

## 7. Kiến trúc Activity & Hướng dẫn Tích hợp

### 7.1 Activity Architecture

Hệ thống hiện có 3 nhánh Activity riêng biệt:

1. **Standard Activities**: Sự kiện thông thường (SUKIEN, CONG_TAC_XA_HOI, CHUYEN_DE_DOANH_NGHIEP). Có điểm riêng lẻ qua `ActivityScoreRule`.
2. **Minigames**: Sự kiện kèm Quiz. Điểm cộng qua `MINIGAME_PASSED` hoặc phạt qua `MINIGAME_EXHAUSTED_ATTEMPTS`.
3. **Series Child Activities**: Sự kiện con trong chuỗi. **Không cộng điểm riêng lẻ**. Điểm chỉ cộng qua `SERIES_MILESTONE_REACHED` và phạt qua `SERIES_MINIMUM_REQUIREMENT`.

### 7.2 Frontend Integration Guidance

| Tác vụ | Endpoint nên dùng | DTO nên dùng |
|--------|-------------------|--------------|
| Tạo sự kiện thường | `POST /api/activities/standard` | `StandardActivityCreateRequest` |
| Tạo sự kiện minigame | `POST /api/activities/minigame` | `MinigameActivityCreateRequest` |
| Tạo sự kiện con trong chuỗi | `POST /api/series/{seriesId}/activities` | `SeriesChildActivityCreateRequest` |
| Lấy chi tiết sự kiện thường | `GET /api/activities/standard/{id}` | `StandardActivityResponse` |
| Lấy chi tiết sự kiện minigame | `GET /api/activities/minigame/{id}` | `MinigameActivityResponse` |
| Lấy chi tiết sự kiện con | `GET /api/series/{seriesId}/activities/{activityId}` | `SeriesChildActivityResponse` |
| Legacy (vẫn hoạt động) | `POST /api/activities`, `PUT /api/activities/{id}` | `CreateActivityRequest`, `ActivityResponse` |

### 7.3 Score Rules

- **Cấu trúc payload:** `ActivityScoreRuleRequest` / `ActivityScoreRuleResponse`
- **Validation rules:**
  - `points` và `failPoints` là `BigDecimal` (nên dùng `number | string` ở FE).
  - `calculation = PASS_FAIL_POINTS` hoặc `PENALTY_POINTS` → backend tự động negate `failPoints`.
  - `semesterPolicy = EXPLICIT_SEMESTER` → bắt buộc `explicitSemesterId`.
- **Semester support:** `ACTIVITY_SEMESTER` (tự suy ra từ ngày activity) hoặc `EXPLICIT_SEMESTER` (chỉ định cụ thể).
- **Department targeting:** `ALL_PARTICIPANTS`, `DEPARTMENT_ONLY`, `OUTSIDE_DEPARTMENTS_ONLY`.

### 7.4 Series Features

- **Overview:** `GET /api/series/{seriesId}/overview` → `SeriesOverviewResponse`
  - `totalCompletedStudents`: số SV hoàn thành **tất cả** activities.
  - `minimumRequirementMetCount`: số SV đạt mốc tối thiểu.
  - `milestoneProgress`: phân bố SV theo từng milestone.
  - `activityStats`: thống kê từng activity con.
- **Progress (Student):** `GET /api/series/{seriesId}/progress/my` → `Map<string, any>`
  - `completedCount`, `totalActivities`, `pointsEarned`, `currentMilestone`, `nextMilestoneCount`, `nextMilestonePoints`.
  - `minimumRequirementMet`: boolean.
  - `remainingToAvoidPenalty`: số activity còn thiếu để tránh phạt.
- **Progress (Admin list):** `GET /api/series/{seriesId}/progress` → `SeriesProgressListResponse`
- **Milestones:** `milestonePoints` là `Map<number, number>`. VD: `{3: 5, 5: 10}` nghĩa là hoàn thành 3 activity được 5 điểm, hoàn thành 5 activity được 10 điểm.
- **Minimum requirements:**
  - `minimumRequirementEnabled`: bật/tắt kiểm tra số buổi tối thiểu.
  - `minimumRequiredEvents`: số buổi tối thiểu.
  - `minimumPenaltyPoints`: số điểm bị trừ nếu không đạt (frontend gửi số dương, backend tự negate).
- **Auto-register flags (P5):** Series có `isImportant` và `mandatoryForFacultyStudents`:
  - `isImportant=true`: BE auto-register **mọi sinh viên active** vào main activity VÀ mỗi child activity mới khi Series ở trạng thái non-draft.
  - `mandatoryForFacultyStudents=true`: BE auto-register sinh viên thuộc các khoa tổ chức của các activity trong Series.
  - Hai flag có thể kết hợp. Auto-register là idempotent (SV đã đăng ký thì skip) và không bao giờ throw (lỗi bị swallow).
  - Child activity tạo qua `POST /api/series/{seriesId}/activities` luôn có flag `false` ở mức activity; BE dùng flag của Series để auto-register.

### 7.5 Async Recalculation Flow

Luồng tích hợp tính lại điểm async cho Admin/Manager:

1. **Khởi tạo job:** Gọi `POST /api/scores/recalculate/async?semesterId=...`
2. **Polling tiến độ:** Gọi `GET /api/scores/recalculate/status/{jobId}` mỗi 3-5 giây.
   - Hiển thị progress bar dựa trên `progressPercent`.
   - Hiển thị `processedStudents / totalStudents`.
   - Nếu `status = RUNNING`: tiếp tục poll.
   - Nếu `status = COMPLETED`: hiển thị thành công, dừng poll.
   - Nếu `status = FAILED` hoặc `TIMEOUT`: hiển thị lỗi, cho phép retry.
3. **Retry:** Gọi `POST /api/scores/recalculate/retry/{jobId}` để tạo job mới.
4. **Lưu ý:**
   - Chỉ có 1 job active per semester (concurrency lock).
   - Timeout tối đa 30 phút.
   - Job xử lý theo batch 100 SV/lần.

---

## 8. Migration Recommendations

### 8.1 Từ Legacy sang Specialized APIs

| Legacy | Thay thế bằng | Lý do |
|--------|---------------|-------|
| `POST /api/activities` | `POST /api/activities/standard` hoặc `POST /api/activities/minigame` | Tránh dùng chung DTO cồng kềnh, validation rõ ràng hơn. |
| `PUT /api/activities/{id}` | `PUT /api/activities/standard/{id}` hoặc `PATCH /api/activities/minigame/{id}` | Tương tự. |
| `GET /api/activities/{id}` | `GET /api/activities/standard/{id}` hoặc `GET /api/activities/minigame/{id}` | Response DTO chuyên biệt, đầy đủ field hơn. |

### 8.2 Series Integration

1. **Form tạo Series:**
   - Sử dụng `CreateSeriesRequest`.
   - `milestonePoints` là `Record<number, number>`.
   - `targetSemesterId` optional.
   - Có thể dùng `presetCode` + `presetConfig` để auto-fill.

2. **Form tạo Series Child Activity:**
   - Sử dụng `SeriesChildActivityCreateRequest`.
   - Không cần gửi `scoreRules` (sẽ bị bỏ qua hoặc empty).
   - `order` = `seriesOrder`.

3. **Màn hình tiến độ Student:**
   - Gọi `GET /api/series/{seriesId}/progress/my`.
   - Parse response như `SeriesStudentProgressMap`.
   - Hiển thị `completedCount / totalActivities`, `pointsEarned`, `currentMilestone`, `nextMilestoneCount`.
   - Hiển thị cảnh báo `remainingToAvoidPenalty` nếu `minimumRequirementEnabled = true` và `minimumRequirementMet = false`.

4. **Màn hình Overview Admin:**
   - Gọi `GET /api/series/{seriesId}/overview`.
   - Dùng `milestoneProgress` cho biểu đồ phân bố milestone.
   - Dùng `activityStats` cho bảng thống kê từng activity.
   - Dùng `minimumRequirementMetCount` cho số liệu "đạt mốc tối thiểu".

### 8.3 MiniGame Integration

Có **2 luồng tạo Minigame** (cả 2 đều hợp lệ, chọn theo UX):

**Mode 1 — All-at-once (Unified flow, khuyến nghị cho form 1 bước):**
1. Gọi `POST /api/activities/minigame` với `MinigameActivityCreateRequest`.
   - Body gồm: shell fields (`name`, `startDate`, `endDate`, `organizerIds`, `scoreRules`,...) + `quiz: QuizConfigRequest`.
   - `quiz.questions[]` phải có đầy đủ `options[]` (kể cả `isCorrect`).
   - Backend tự tạo: Activity (MINIGAME) → MiniGame → MiniGameQuiz → MiniGameQuizQuestion[] → MiniGameQuizOption[] trong 1 transaction.
2. **Không cần** gọi thêm `POST /api/minigames`.
3. `quiz` Có THỂ để `null` nếu muốn chỉ tạo activity shell trước (khi đó flow trở thành Mode 2 partial).

**Mode 2 — Activity-first (2 bước, linh hoạt hơn):**
1. Tạo activity MINIGAME trước:
   - Hoặc `POST /api/activities/minigame` với `quiz = null` (chỉ shell); hoặc
   - `POST /api/activities/standard` (sau khi tạo có thể đổi type) hoặc Legacy `POST /api/activities` với `type=MINIGAME`.
2. Lấy `activity.id` trả về.
3. Gọi `POST /api/minigames` với `CreateMiniGameRequest` (chứa `activityId` + `title`, `questions[]`, `options[]`,...).
   - Backend tạo MiniGame → Quiz → Questions → Options và gắn vào activity đã có.

**Cập nhật minigame:**
- Dùng `PATCH /api/activities/minigame/{id}` với `MinigameActivityUpdateRequest`.
- Nếu gửi `quiz.questions[]`, backend **xóa-tạo lại** toàn bộ quiz (cả answers của student).
- FE phải gửi **đầy đủ** danh sách questions, không chỉ gửi questions cần sửa.

**Xem lịch sử attempt:**
- Dùng `GET /api/minigames/attempts/{attemptId}`.
- Kiểm tra `showAnswers` trước khi render đáp án đúng.
- `QuizOptionDetailResponse.isCorrect` sẽ là `null` nếu `showAnswers = false`.

**`CreateMiniGameRequest` reference (Mode 2):**
```typescript
export interface CreateMiniGameRequest {
  activityId: number;       // Bắt buộc — activity đã tồn tại
  title: string;
  description?: string;
  questionCount: number;
  timeLimit: number;
  requiredCorrectAnswers: number;
  maxAttempts: number;
  showAnswers?: boolean;
  questions: QuestionRequest[];
}
```

---

## 9. Lưu ý đặc biệt (Gotchas)

1. **`SeriesResponse` đã có `targetSemesterId`** (đã fix backend): `GET /api/series/{id}` và `POST/PUT /api/series` giờ trả về đầy đủ field.
2. **`SeriesPresetPreviewResponse` không có `targetSemesterId`**: Khi preview preset, không có trường này.
3. **Điểm số là `BigDecimal`**: Jackson serialize thành JSON **number** (có thể là string tùy config). FE nên dùng `number | string`.
4. **`GET /api/series/{seriesId}/progress/my` trả về `Map<string, any>`**: Không có DTO typed cố định. FE nên tự định nghĩa interface `SeriesStudentProgressMap`.
5. **Tất cả `*UpdateRequest` là standalone**: Không extends CreateRequest trong Java. FE có thể dùng `Partial<T>` nhưng cần aware.
6. **Legacy endpoints vẫn hoạt động**: `POST /api/activities`, `PUT /api/activities/{id}`, etc. vẫn đầy đủ chức năng. FE có thể migrate dần.
7. **Raw list endpoints**: `GET /api/activities/my`, `/upcoming`, `/month`, `/score-type/*`, `/department/*` không bọc `ApiResponse`.
8. **Activity QR vs Ticket QR**: Sinh viên quét `checkInCode` (Activity QR) → `/api/registrations/checkin/qr`. Ban tổ chức quét `ticketCode` (Ticket QR của SV) → `/api/registrations/checkin`.
9. **Auto-generated `checkInCode`**: Format `ACT-{000000}-{8 random chars}`. Không cần FE tạo.
10. **Score recalculation synchronous cũ vẫn tồn tại**: `POST /api/scores/recalculate/student/{studentId}` và `POST /api/scores/recalculate/all` vẫn chạy đồng bộ. FE nên chuyển sang dùng `POST /api/scores/recalculate/async` cho bulk để tránh timeout.
11. **Task assignment validation**: Backend chặn gán task cho SV không đăng ký hoạt động. FE nên pre-filter danh sách SV theo registration trước khi gọi API.
12. **Preset + custom rules conflict**: Backend từ chối gửi `scoreRules` tùy chỉnh kèm `presetCode` (không phải `CUSTOM`). FE chỉ gửi `presetConfig` khi dùng preset.
13. **`isPresetGenerated` flag**: `ActivityScoreRuleResponse` có thêm trường `isPresetGenerated` (boolean). FE có thể dùng để hiển thị badge/tag phân biệt rule preset vs thủ công.
14. **Score history filter params**: `GET /api/scores/history/student/{studentId}` hỗ trợ thêm `startDate`, `endDate` (ISO datetime string), `keyword` (tìm kiếm theo tên hoạt động).
15. **Minigame Quiz delete-recreate**: Khi update minigame (`PATCH /api/activities/minigame/{id}`) với `quiz.questions[]`, backend **xóa-tạo lại** toàn bộ quiz (gồm cả answers của student). FE phải gửi đầy đủ danh sách questions, không chỉ questions cần sửa.
16. **Unified minigame creation**: `POST /api/activities/minigame` giờ tạo đầy đủ quiz hierarchy (Activity → MiniGame → MiniGameQuiz → Questions → Options). Không cần gọi thêm `POST /api/minigames`. Nếu `quiz = null`, chỉ tạo activity shell (Mode 2 partial) — FE cần gọi `POST /api/minigames` sau để gắn quiz.
17. **⚠️ KNOWN BUG — `PARTICIPATION_COMPLETED` failPoints negate (P5)**: Khi trigger `PARTICIPATION_COMPLETED` và `isCompleted = false`, `ScoreRuleEngineImpl.applyActivityCompleted` dùng `failPoints` **raw (không negate)**, khác với `applyNoShowPenalty` / `applySubmissionGraded` / `applyMinigameExhausted` đều gọi `applySignForFailure()` (negate khi `calculation = PASS_FAIL_POINTS` hoặc `PENALTY_POINTS`). Hệ quả:
    - `calculation = FIXED_POINTS`: **OK** — `failPoints` là "điểm khi không hoàn thành" (VD 0 hoặc điểm giảm), giữ dương là đúng.
    - `calculation = PASS_FAIL_POINTS`: **BUG** — `failPoints` được lưu **dương** trong `score_entries` thay vì âm (penalty). Mọi method khác đều negate cho `PASS_FAIL_POINTS`.
    - **Tạm thời**: FE nếu muốn `PARTICIPATION_COMPLETED` + `PASS_FAIL_POINTS` phạt khi `isCompleted=false`, cân nhắc truyền `failPoints` **âm sẵn** (VD `-5`) cho preset/rule thủ công, vì BE chưa negate. Plan fix BE: gọi `applySignForFailure` ở line applied.

---

*End of FE_BACKEND_HANDOFF_SPEC.md*
