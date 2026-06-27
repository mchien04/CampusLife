# FE Backend Handoff v5 Delta

> **Phiên bản:** 5.1 — Các thay đổi mới so với v4.0  
> **Phạm vi:** Series auto-register flags + isDraft, Per-rule audience config, Auto-register service extraction, Minigame dual-creation modes, Known bug  
> **Người tích hợp FE:** Đọc tài liệu này TRƯỚC, sau đó tham chiếu `FE_BACKEND_HANDOFF_SPEC.md` (v5.0) để biết chi tiết DTO đầy đủ.

---

## 1. Series: `isImportant` & `mandatoryForFacultyStudents`

### 1.1 Mô tả

Series Activity giờ có 2 cờ boolean điều khiển auto-register **ở cấp độ Series** (không phải cấp activity con):

| Field | Kiểu | Mặc định | Ý nghĩa |
|-------|------|----------|---------|
| `isImportant` | boolean | `false` | Nếu `true`, BE auto-register **mọi sinh viên active** vào main activity + mọi child activity mới (khi Series non-draft). |
| `mandatoryForFacultyStudents` | boolean | `false` | Nếu `true`, BE auto-register sinh viên thuộc **các khoa tổ chức** của activity trong Series. |

### 1.2 DTO thay đổi

**`CreateSeriesRequest`** / **`UpdateSeriesRequest`** — thêm 2 field:

```typescript
export interface CreateSeriesRequest {
  // ... các field cũ (name, description, milestonePoints, scoreType, ...)
  isImportant?: boolean | null;                    // MỚI
  mandatoryForFacultyStudents?: boolean | null;     // MỚI
}

export interface UpdateSeriesRequest {
  // ... các field cũ
  isImportant?: boolean | null;                    // MỚI
  mandatoryForFacultyStudents?: boolean | null;     // MỚI
}
```

**`SeriesResponse`** — thêm 2 field (luôn trả về, không optional):

```typescript
export interface SeriesResponse {
  // ... các field cũ
  isImportant: boolean;                  // MỚI — mặc định false
  mandatoryForFacultyStudents: boolean;  // MỚI — mặc định false
}
```

### 1.3 Hành vi

- **Khi tạo Series** (`POST /api/series`): nếu `isImportant=true` hoặc `mandatoryForFacultyStudents=true` và Series **không phải draft** (`isDraft=false` hoặc không gửi), BE tự động đăng ký sinh viên vào `mainActivity`.
- **Khi sửa Series** (`PUT /api/series/{id}`): nếu cờ thay đổi và Series non-draft, BE tự động đăng ký/bổ sung sinh viên vào `mainActivity`.
- **Khi thêm child activity** vào Series (`POST /api/series/{seriesId}/activities`): nếu Series non-draft, BE tự động đăng ký sinh viên theo cờ của Series vào child activity mới.
- **Child activity không có 2 cờ này** ở mức tạo — luôn `false`. Auto-register của child dùng cờ của Series cha.
- **Idempotent**: sinh viên đã có registration thì skip.
- **Không throw**: nếu auto-register lỗi, BE log và swallow — không làm fail API create/update.

### 1.4 Hướng dẫn FE

- Form tạo/sửa Series: thêm 2 toggle switch:
  - "Sự kiện quan trọng" (`isImportant`)
  - "Bắt buộc với sinh viên khoa tổ chức" (`mandatoryForFacultyStudents`)
- Màn hình chi tiết Series (`SeriesResponse`): hiển thị trạng thái 2 cờ này.
- Khi xem child activity trong Series: không hiển thị 2 cờ này (luôn false).

### 1.5 Cờ `isDraft` trên Series

Series giờ có cờ `isDraft` (boolean, mặc định `true`). Khi `isDraft = true`:
- **Auto-register không chạy** — dù `isImportant` hay `mandatoryForFacultyStudents` có bật.
- Series chưa được publish, FE có thể dùng để phân biệt draft/published.

**DTO thay đổi:**

| DTO | Field | Kiểu |
|-----|-------|------|
| `CreateSeriesRequest` | `isDraft` | `Boolean` (optional) |
| `UpdateSeriesRequest` | `isDraft` | `Boolean` (optional) |
| `SeriesResponse` | `isDraft` | `boolean` |

```typescript
// SeriesResponse
export interface SeriesResponse {
  // ... các field cũ
  isDraft: boolean;   // true = bản nháp, false = đã publish
}
```

**Hành vi:**
- Khi tạo: nếu `isDraft = true` (hoặc không gửi), auto-register **bị bỏ qua**.
- Khi publish: sửa `isDraft = false` qua `PUT /api/series/{id}`, auto-register chạy cho main activity + child activities.
- `createActivityInSeries` (`POST /api/series/{seriesId}/activities`): kiểm tra `series.isDraft()` trước khi auto-register.

---

## 2. Minigame: Hai luồng tạo

### 2.1 Mode 1 — All-at-once (khuyến nghị)

**Endpoint:** `POST /api/activities/minigame`
**Request:** `MinigameActivityCreateRequest` (shell + `quiz`)

```typescript
export interface MinigameActivityCreateRequest {
  // Shell fields
  name: string;
  startDate: string;
  endDate: string;
  organizerIds?: number[];
  isImportant?: boolean | null;
  mandatoryForFacultyStudents?: boolean | null;
  scoreRules?: ActivityScoreRuleRequest[];

  // Quiz fields — nếu null thì chỉ tạo shell (tương đương Mode 2 bước 1)
  quiz?: QuizConfigRequest | null;
}
```

- Backend tạo: Activity (MINIGAME) → MiniGame → Quiz → Questions → Options trong 1 transaction.
- **Không cần gọi thêm** `POST /api/minigames`.
- Dùng khi UX có form 1 bước (tạo activity + quiz cùng lúc).

### 2.2 Mode 2 — Activity-first (2 bước)

**Bước 1:** Tạo activity shell:
- `POST /api/activities/minigame` với `quiz = null`; hoặc
- `POST /api/activities/standard` với `type = MINIGAME`; hoặc
- Legacy `POST /api/activities` với activity type MINIGAME.

**Bước 2:** Gắn quiz vào activity đã tạo:
- **Endpoint:** `POST /api/minigames`
- **Request:** `CreateMiniGameRequest`

```typescript
export interface CreateMiniGameRequest {
  activityId: number;        // ID activity đã tạo ở bước 1
  title: string;
  questionCount: number;
  timeLimit: number;
  requiredCorrectAnswers: number;
  maxAttempts: number;
  showAnswers?: boolean;
  questions: QuestionRequest[];  // Mỗi QuestionRequest có options[]
}
```

- Dùng khi UX có form 2 bước (tạo activity trước, cấu hình quiz sau).

### 2.3 Lưu ý chung

- `PATCH /api/activities/minigame/{id}` với `quiz.questions[]`: backend **xóa-tạo lại** toàn bộ quiz (gồm cả answers của student). Gửi đầy đủ questions, không chỉ gửi câu cần sửa.
- `quiz = null` trong `MinigameActivityCreateRequest` là hợp lệ — tạo activity shell không kèm quiz.

---

## 3. Known Bug: `PARTICIPATION_COMPLETED` failPoints negate

### 3.1 Vấn đề

Trong `ScoreRuleEngineImpl.applyActivityCompleted()`, khi `isCompleted = false`, `failPoints` được dùng **raw (không negate)**:

```java
// Bug: không gọi applySignForFailure()
BigDecimal points = Boolean.TRUE.equals(participation.getIsCompleted())
    ? rule.getPoints()
    : rule.getFailPoints();
```

Tất cả các trigger khác (`NO_SHOW`, `SUBMISSION_GRADED`, `TASK_OVERDUE`, `MINIGAME_EXHAUSTED_ATTEMPTS`) đều gọi `applySignForFailure()` để negate khi `calculation = PASS_FAIL_POINTS` hoặc `PENALTY_POINTS`.

### 3.2 Ảnh hưởng theo từng loại calculation

| Calculation | Ảnh hưởng | Xử lý |
|-------------|-----------|-------|
| `FIXED_POINTS` | **Không ảnh hưởng.** `failPoints` là "điểm khi không hoàn thành" (VD 0 hoặc điểm giảm), không cần negate. | OK, dùng bình thường. |
| `PASS_FAIL_POINTS` | **BUG.** `failPoints` lưu dương trong `score_entries` thay vì âm (penalty). | **Workaround:** gửi `failPoints` âm sẵn (VD `-5`) nếu dùng custom rule. |
| `PENALTY_POINTS` | **BUG.** Tương tự. | **Workaround:** gửi `failPoints` âm sẵn. |

### 3.3 Ảnh hưởng đến Preset

| Preset | Trigger | Calculation | Có failPoints? | Bị bug? |
|--------|---------|-------------|----------------|---------|
| `EVENT_BASIC` | `PARTICIPATION_COMPLETED` | `FIXED_POINTS` | Có (0) | **Không** |
| `ENTERPRISE_SEMINAR_BASIC` | `PARTICIPATION_COMPLETED` | `COUNT_COMPLETION` | Có (0) | **Không** |
| `ENTERPRISE_SEMINAR_WITH_BONUS` | `PARTICIPATION_COMPLETED` | `COUNT_COMPLETION` | Có (0, rules chính) | **Không** |
| `EVENT_WITH_SUBMISSION` | `SUBMISSION_GRADED` | `PASS_FAIL_POINTS` | Có | **Không** (dùng `applySubmissionGraded`, đã negate đúng) |

⇒ **Preset mặc định không bị ảnh hưởng.** Bug chỉ xảy ra với custom rule `PARTICIPATION_COMPLETED` + `PASS_FAIL_POINTS`/`PENALTY_POINTS`.

### 3.4 Hướng dẫn FE

- **Form tạo rule `PARTICIPATION_COMPLETED`**:
  - Với `EVENT_BASIC`: ẩn `failPoints` input (vì `FIXED_POINTS` không cần negate, failPoints mặc định = 0).
  - Với `EVENT_WITH_SUBMISSION`: hiện `failPoints` input (dùng trigger `SUBMISSION_GRADED`, đã negate đúng).
  - Với custom rule: nếu chọn `calculation = PASS_FAIL_POINTS` hoặc `PENALTY_POINTS` + `trigger = PARTICIPATION_COMPLETED`, hiển thị cảnh báo: "failPoints sẽ không được tự động negate, vui lòng nhập số âm."
- **Kế hoạch sửa BE**: sẽ gọi `applySignForFailure()` trong `applyActivityCompleted()` ở phiên bản sau.

---

## 4. Auto-register Service Extraction

### 4.1 Mô tả

Logic auto-register được trích từ `ActivityServiceImpl` thành service chung `ActivityRegistrationAutoService`. Dùng chung cho:

- `POST /api/activities/standard` (StandardActivityServiceImpl)
- `PATCH /api/activities/standard/{id}` (StandardActivityServiceImpl)
- `POST /api/activities/minigame` (MinigameActivityServiceImpl)
- `PATCH /api/activities/minigame/{id}` (MinigameActivityServiceImpl)
- `POST /api/activities` Legacy (ActivityServiceImpl)
- `PUT /api/activities/{id}` Legacy (ActivityServiceImpl)
- `POST /api/series` (ActivitySeriesServiceImpl — với cờ của Series)
- `PUT /api/series/{id}` (ActivitySeriesServiceImpl — với cờ của Series)
- `POST /api/series/{seriesId}/activities` (ActivitySeriesServiceImpl — với cờ của Series)

### 4.2 Hành vi

- Auto-register chạy **sau khi** activity/series được save (có ID).
- Chỉ chạy khi activity/series **non-draft**.
- **Idempotent**: sinh viên đã có registration `APPROVED` thì skip.
- **Không throw**: mọi exception bị log + swallow, không fail API.
- Gửi notification FCM cho sinh viên được auto-register.

### 4.3 Hướng dẫn FE

- **Không có thay đổi API contract.** Behaviour auto-register nhất quán trên mọi activity type.
- FE có thể yên tâm: auto-register chạy ngầm, không ảnh hưởng response API.
- Nếu FE muốn hiển thị danh sách sinh viên vừa được auto-register, cần gọi API riêng (`GET /api/activities/{id}/registrations`) sau khi tạo.

---

## 5. Per-Rule Audience Config trên Preset

### 5.1 Mô tả

Cho phép FE cấu hình `audience`, `semesterPolicy`, `explicitSemesterId`, `departmentIds` riêng cho **từng trigger** trong preset, thay vì dùng chung 1 giá trị cho tất cả rules. Nếu không set per-rule, BE fallback về top-level `audience`/`semesterPolicy`/`departmentIds` như cũ.

### 5.2 `ActivityPresetConfig` — Thêm per-rule fields

```typescript
export interface ActivityPresetConfig {
  // Top-level (fallback cho tất cả rules)
  audience?: ScoreRuleAudience | null;
  semesterPolicy?: ScoreSemesterPolicy | null;
  explicitSemesterId?: number | null;
  departmentIds?: number[] | null;

  // Per-rule: SUBMISSION_GRADED (EVENT_WITH_SUBMISSION)
  submissionAudience?: ScoreRuleAudience | null;
  submissionSemesterPolicy?: ScoreSemesterPolicy | null;
  submissionExplicitSemesterId?: number | null;
  submissionDepartmentIds?: number[] | null;

  // Per-rule: PARTICIPATION_COMPLETED (EVENT_BASIC, ENTERPRISE_SEMINAR_BASIC, ENTERPRISE_SEMINAR_WITH_BONUS)
  participationAudience?: ScoreRuleAudience | null;
  participationSemesterPolicy?: ScoreSemesterPolicy | null;
  participationExplicitSemesterId?: number | null;
  participationDepartmentIds?: number[] | null;

  // Per-rule: NO_SHOW (tất cả presets)
  noShowAudience?: ScoreRuleAudience | null;
  noShowSemesterPolicy?: ScoreSemesterPolicy | null;
  noShowExplicitSemesterId?: number | null;
  noShowDepartmentIds?: number[] | null;

  // Per-rule: TASK_OVERDUE (EVENT_WITH_SUBMISSION)
  taskOverdueAudience?: ScoreRuleAudience | null;
  taskOverdueSemesterPolicy?: ScoreSemesterPolicy | null;
  taskOverdueExplicitSemesterId?: number | null;
  taskOverdueDepartmentIds?: number[] | null;

  // Per-rule: Bonus PARTICIPATION_COMPLETED (ENTERPRISE_SEMINAR_WITH_BONUS)
  bonusAudience?: ScoreRuleAudience | null;
  bonusSemesterPolicy?: ScoreSemesterPolicy | null;
  bonusExplicitSemesterId?: number | null;
  bonusDepartmentIds?: number[] | null;

  // Per-rule: MINIGAME_PASSED (MINIGAME_PASS_ONLY)
  minigamePassedAudience?: ScoreRuleAudience | null;
  minigamePassedSemesterPolicy?: ScoreSemesterPolicy | null;
  minigamePassedExplicitSemesterId?: number | null;
  minigamePassedDepartmentIds?: number[] | null;

  // Per-rule: MINIGAME_EXHAUSTED_ATTEMPTS (MINIGAME_PASS_ONLY)
  minigameExhaustedAudience?: ScoreRuleAudience | null;
  minigameExhaustedSemesterPolicy?: ScoreSemesterPolicy | null;
  minigameExhaustedExplicitSemesterId?: number | null;
  minigameExhaustedDepartmentIds?: number[] | null;
}
```

### 5.3 Ví dụ

**EVENT_WITH_SUBMISSION** — muốn `SUBMISSION_GRADED` chỉ áp dụng cho khoa 1, nhưng `NO_SHOW` cho tất cả:

```json
{
  "presetCode": "EVENT_WITH_SUBMISSION",
  "presetConfig": {
    "primaryScoreType": "CONG_TAC_XA_HOI",
    "submissionPassPoints": 5,
    "submissionFailPoints": 4,
    "noShowPenaltyEnabled": true,
    "noShowPenaltyPoints": 6,

    "audience": "ALL_PARTICIPANTS",
    "submissionAudience": "DEPARTMENT_ONLY",
    "submissionDepartmentIds": [1]
  }
}
```

Kết quả preview:
| Rule | Audience | Department |
|------|----------|------------|
| `SUBMISSION_GRADED` | `DEPARTMENT_ONLY` | `[1]` |
| `TASK_OVERDUE` | `ALL_PARTICIPANTS` (fallback) | — |
| `NO_SHOW` | `ALL_PARTICIPANTS` (fallback) | — |

### 5.4 Hướng dẫn FE

- Render form `presetConfig`: thêm section "Cấu hình đối tượng theo từng rule".
- Mỗi rule (trigger) có thể override: `submissionAudience`, `noShowAudience`,...
- Nếu không điền per-rule, BE dùng top-level `audience`.
- Các per-rule field tương ứng với trigger:
  | Trigger | Override prefix |
  |---------|----------------|
  | `SUBMISSION_GRADED` | `submission*` |
  | `PARTICIPATION_COMPLETED` (main) | `participation*` |
  | `NO_SHOW` | `noShow*` |
  | `TASK_OVERDUE` | `taskOverdue*` |
  | `PARTICIPATION_COMPLETED` (bonus) | `bonus*` |
  | `MINIGAME_PASSED` | `minigamePassed*` |
  | `MINIGAME_EXHAUSTED_ATTEMPTS` | `minigameExhausted*` |

---

## 6. Tổng kết thay đổi API

| Endpoint | Thay đổi |
|----------|----------|
| `POST /api/series` | Request thêm `isImportant`, `mandatoryForFacultyStudents`, `isDraft` |
| `PUT /api/series/{id}` | Request thêm `isImportant`, `mandatoryForFacultyStudents`, `isDraft` |
| `GET /api/series/{id}` | Response thêm `isImportant`, `mandatoryForFacultyStudents`, `isDraft` |
| `POST /api/activities/minigame` | `quiz` có thể null (tạo shell không quiz) |
| `POST /api/minigames` | Mode 2: gắn quiz vào activity đã tồn tại |
| Preset config | `ActivityPresetConfig` thêm per-rule audience/semesterPolicy/departmentIds |
| *Không đổi* | `POST/PUT /api/activities/standard`, Legacy, Series child |

---

*End of v5.1 Delta Spec*
