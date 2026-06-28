# FE Backend Handoff v5 Delta

> **Phiên bản:** 5.2.1 — Các thay đổi mới so với v4.0 (Bao gồm các bản vá lỗi)  
> **Phạm vi:** Series auto-register flags + isDraft, Per-rule audience config, CUSTOM preset suggestedCombinations, Auto-register service extraction, Minigame dual-creation modes, Bug fixes  
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

## 5. Cập nhật Bug Fix (v5.3)
### 5.1. Cập nhật rule NO_SHOW cho MINIGAME_PASS_ONLY
- **Vấn đề trước đây**: ActivityPreset `MINIGAME_PASS_ONLY` thiếu form config cho `NO_SHOW` trong API get definitions mặc dù có thể hỗ trợ trigger này.
- **Thay đổi hiện tại**: 
  - BE đã bổ sung logic trả về rule descriptor `NO_SHOW` cho preset này.
  - Tuy nhiên, **giá trị mặc định `noShowPenaltyEnabled` được đổi thành `false`** (tắt mặc định).
- **Tác động tới FE**: FE khi fetch `GET /api/activities/presets` sẽ nhận được rule descriptor `NO_SHOW` cho `MINIGAME_PASS_ONLY` với `defaultValue = false`. FE sẽ render form toggle tắt theo mặc định, nhưng người dùng (admin) **hoàn toàn có thể chủ động bật lên (`true`)** và gửi về BE để lưu phạt vắng mặt cho minigame nếu muốn.

---

## 6. Per-Rule Audience Config trên Preset

### 6.1 Mô tả

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

## 6. CUSTOM Preset: `suggestedCombinations`

### 6.1 Mô tả

Trước đây, `GET /api/activities/presets` với CUSTOM preset trả về `supportedRules: []` (rỗng). Giờ trả về **tất cả** rule descriptors với field mới `suggestedCombinations` — danh sách các `ScoreRuleTrigger` có thể kết hợp.

### 6.2 `PresetRuleDescriptor` — Field mới

```typescript
export interface PresetRuleDescriptor {
  ruleKey: string;
  label: string;
  description: string;
  required: boolean;
  enabledByDefault: boolean;
  fieldDefinitions: FieldDefinition[];
  suggestedCombinations?: ScoreRuleTrigger[];  // MỚI — chỉ có ý nghĩa với CUSTOM
}
```

### 6.3 Bảng gợi ý

| Rule (ruleKey) | suggestedCombinations |
|----------------|----------------------|
| `PARTICIPATION_COMPLETED` | `[NO_SHOW]` |
| `SUBMISSION_GRADED` | `[TASK_OVERDUE, NO_SHOW]` |
| `TASK_OVERDUE` | `[SUBMISSION_GRADED, NO_SHOW]` |
| `NO_SHOW` | `[PARTICIPATION_COMPLETED, SUBMISSION_GRADED, TASK_OVERDUE, MINIGAME_PASSED]` |
| `MINIGAME_PASSED` | `[MINIGAME_EXHAUSTED_ATTEMPTS, NO_SHOW]` |
| `MINIGAME_EXHAUSTED_ATTEMPTS` | `[MINIGAME_PASSED]` |
| `BONUS_POINTS` | `[PARTICIPATION_COMPLETED]` |

### 6.4 Hướng dẫn FE

- Khi render form CUSTOM mode, duyệt `supportedRules[]`.
- Với mỗi rule user chọn, lấy `suggestedCombinations` để render chip gợi ý: "Có thể kết hợp với: Nộp trễ, Vắng mặt".
- `suggestedCombinations` chỉ là gợi ý từ BE, không block user chọn rule khác ngoài danh sách.

---

## 7. Tổng kết thay đổi API

| Endpoint | Thay đổi |
|----------|----------|
| `POST /api/series` | Request thêm `isImportant`, `mandatoryForFacultyStudents`, `isDraft` |
| `PUT /api/series/{id}` | Request thêm `isImportant`, `mandatoryForFacultyStudents`, `isDraft` |
| `GET /api/series/{id}` | Response thêm `isImportant`, `mandatoryForFacultyStudents`, `isDraft` |
| `POST /api/activities/minigame` | `quiz` có thể null (tạo shell không quiz). Giờ hỗ trợ `presetCode` + `presetConfig`. |
| `GET /api/activities/minigame/{id}` | Response thêm `presetCode`. |
| `POST /api/minigames` | Mode 2: gắn quiz vào activity đã tồn tại |
| Preset config | `ActivityPresetConfig` thêm per-rule audience/semesterPolicy/departmentIds. `MINIGAME_PASS_ONLY` giờ có `NO_SHOW` mặc định bật. |
| *Không đổi* | `POST/PUT /api/activities/standard`, Legacy, Series child |

---

## 8. MINIGAME_PASS_ONLY giờ có NO_SHOW (v5.3)

### 8.1 Mô tả

Preset `MINIGAME_PASS_ONLY` giờ **mặc định bật** rule `NO_SHOW` (phạt nếu đăng ký nhưng không hoàn thành minigame). Trước đây minigame không có NO_SHOW.

| Thay đổi | Cũ | Mới |
|-----------|-----|-----|
| `hasDefaultNoShowEnabled` | `false` (cho mọi MINIGAME type) | `true` cho `MINIGAME_PASS_ONLY` |
| `noShowPenaltyEnabled` (default) | `false` | `true` |
| `noShowPenaltyPoints` (default) | 5 (nhưng k dùng vì disable) | = `participationPoints` (5) |
| Fallback noShowPoints | `defaults.getNoShowPenaltyPoints()` | `merged.getParticipationPoints()` (giống EVENT_BASIC) |

### 8.2 Luồng hoạt động

1. **FE chọn preset `MINIGAME_PASS_ONLY`**: BE tự sinh 3 rule:
   - `MINIGAME_PASSED` + `FIXED_POINTS` + `points = 5`
   - `NO_SHOW` + `PENALTY_POINTS` + `failPoints = 5`
   - `MINIGAME_EXHAUSTED_ATTEMPTS` + `PASS_FAIL_POINTS` + `failPoints = 0` (mặc định tắt)

2. **NO_SHOW penalty**: Engine `applyNoShowPenalty()` dùng `rule.getFailPoints()` → `applySignForFailure` auto negate (5 → -5).

3. **Có thể tắt**: FE gửi `presetConfig.noShowPenaltyEnabled = false` → NO_SHOW rule bị xóa khỏi preset.

4. **Preview**: `POST /api/activities/presets/preview` với `MINIGAME_PASS_ONLY` giờ trả về 3 rule (có thêm NO_SHOW).

### 8.3 Cấu hình FE

```
Chọn preset: MINIGAME_PASS_ONLY
├── [ON] MINIGAME_PASSED: 5 điểm (bắt buộc)
├── [ON] NO_SHOW: Phạt 5 điểm nếu không hoàn thành (có thể tắt)
│   └── Input: Điểm phạt (dương, BE tự negate)
└── [OFF] MINIGAME_EXHAUSTED_ATTEMPTS: Phạt hết lượt (mặc định tắt)
    └── Input: Điểm phạt (dương, BE tự negate)
```

### 8.4 Code thay đổi

| File | Hàm | Thay đổi |
|------|-----|----------|
| `ScorePresetServiceImpl.java` | `hasDefaultNoShowEnabled()` | `MINIGAME` type giờ check thêm `presetCode == MINIGAME_PASS_ONLY`, trả `true` |
| `ScorePresetServiceImpl.java` | `getDefaultActivityConfig()` | Xóa override `setNoShowPenaltyEnabled(false)` cho MINIGAME_PASS_ONLY |
| `ScorePresetServiceImpl.java` | `mergeActivityConfig()` | Thêm `MINIGAME_PASS_ONLY` vào fallback `noShowPoints = participationPoints` |

---

## 9. Cập nhật Bug Fixes (v5.2.1)

Sau quá trình kiểm thử, các lỗi sau đã được fix trên backend để đảm bảo đúng với spec v5.2:

1. **Minigame Mode 1 (Chỉ tạo shell):**
   - Đã fix lỗi `NullPointerException` khi gửi `quiz = null` trong `MinigameActivityCreateRequest`. Việc gửi `quiz = null` giờ đây hoạt động ổn định.
2. **`createActivityInSeries` (Thêm child activity vào Series):**
   - **Response Type:** Đã đổi kiểu trả về thành `SeriesChildActivityResponse` DTO thay vì Entity raw, nhất quán với spec hiện tại.
   - **isDraft Guard:** Đã bổ sung logic kiểm tra. Nếu Series đang ở trạng thái nháp (`isDraft = true`), việc thêm một activity mới vào chuỗi sẽ **không** kích hoạt quá trình auto-register sinh viên.
3. **`getAllSeries` (Danh sách Series):**
   - Đã bổ sung 3 trường bị thiếu vào từng item trong danh sách trả về: `isImportant`, `mandatoryForFacultyStudents`, và `isDraft`.

---

*End of v5.2.1 Delta Spec*

---

## 10. Cập nhật Bug Fixes (v5.2.2)

1. **Preset Per-rule Audience Field Definitions (`ScorePresetServiceImpl`)**:
   - **Vấn đề**: API `GET /api/activities/presets` trả về cấu trúc mới có chứa `audience`, `semesterPolicy`... theo từng rule (ví dụ: `submissionAudience`, `participationSemesterPolicy`). Tuy nhiên, `fieldDefinitions` của các rules (như `SUBMISSION_GRADED`, `PARTICIPATION_COMPLETED`) chưa trả về thông tin cấu hình này để FE render form động theo `visibility`.
   - **Giải pháp**:
     - Bổ sung helper `buildPerRuleAudienceFields` để sinh ra 4 fields: `[prefix]Audience`, `[prefix]DepartmentIds`, `[prefix]SemesterPolicy`, và `[prefix]ExplicitSemesterId` cho mỗi trigger rule.
     - Cập nhật các rule descriptors (`participationCompletedDescriptor`, `submissionGradedDescriptor`, `taskOverdueDescriptor`, `bonusPointsDescriptor`, `minigamePassedDescriptor`, `minigameExhaustedDescriptor`, và `buildNoShowDescriptor`) để include thêm 4 fields này.
     - Đồng bộ logic hiển thị (`visibility`) theo chuẩn: `audience_department_scoped` và `semester_policy_explicit` như spec frontend mong đợi.

---

*End of v5.2.2 Delta Spec*
