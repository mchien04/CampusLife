# Delta: ScoreRule Preset mở rộng + Series Audience + Edit Flow fixes

> **Date:** 2026-06-28
> **Baseline:** Post `FE_BACKEND_HANDOFF_SPEC.md` v3.1
> **Source:** Java backend (controllers, DTOs, services, validators)

Tài liệu này mô tả các thay đổi mới nhất về cấu hình ScoreRule qua preset, audience cho Series, và các bug-fix quan trọng trong luồng edit.

---

## 1. Bảng thay đổi nghiệp vụ mới

| Nghiệp vụ | Hành vi Backend mới | FE cần làm | Mức độ |
|:---|:---|:---|:---|
| **ActivityPresetConfig mở rộng** | `ActivityPresetConfig` giờ có thêm `audience`, `semesterPolicy`, `explicitSemesterId`, `departmentIds`. `buildRule()` đọc từ config thay vì hard-code `ALL_PARTICIPANTS`. | Form preset: thêm section "Giới hạn đối tượng" cho phép chọn audience + khoa + học kỳ. FE render từ `ACTIVITY_AUDIENCE` descriptor trong `supportedRules`. | **Cao** |
| **SeriesPresetConfig mở rộng** | `SeriesPresetConfig` thêm `audience`, `departmentIds`. Series scoring engine (`ScoreRuleEngineImpl`) kiểm tra audience khi tính milestone/penalty: student không thuộc khoa được chọn sẽ KHÔNG nhận điểm. | Form tạo/sửa Series: thêm section chọn audience + khoa. FE render từ `SERIES_AUDIENCE` descriptor. | **Cao** |
| **supportedRules có ACTIVITY_AUDIENCE & SERIES_AUDIENCE** | `GET /api/activities/presets` và `GET /api/series/presets` giờ trả thêm rule descriptor `ACTIVITY_AUDIENCE` / `SERIES_AUDIENCE` chứa các `FieldDefinition` cho audience, departmentIds, semesterPolicy, explicitSemesterId. | FE render form động theo metadata. Dùng `inputType=MULTI_SELECT` cho `departmentIds` và `visibility=audience_department_scoped` để ẩn/hiện. | **Cao** |
| **ScoreRule edit merge-by-key** | Khi edit score rules, backend dùng merge-by-key `(triggerType, scoreType, calculation)` thay vì delete-all+recreate. Rule ID được giữ nguyên → score entries không bị gãy FK. | FE không cần thay đổi logic gửi request. Gửi toàn bộ `scoreRules` list như cũ, backend tự merge. | **Thấp** |
| **Rules-lock khi có ACTIVE entries** | Nếu activity đã có score entries ACTIVE và KHÔNG phải draft, BE từ chối sửa score rules. Trả về lỗi `IllegalStateException`. | FE hiển thị thông báo lỗi rõ ràng: "Không thể sửa scoreboard khi đã có X score entries. Unpublish activity trước." | **Cao** |
| **Type-lock khi có ACTIVE entries** | Activity không thể đổi type nếu đã có ACTIVE score entries và không phải draft. | FE disable/ẩn dropdown type khi có entries, hoặc hiển thị thông báo lỗi khi submit. | **Cao** |
| **StandardActivityUpdateRequest có type** | `type` không còn bị khóa — có thể đổi type khi update (nếu activity draft hoặc chưa có entries). | Form edit Standard Activity: bỏ disable dropdown type. Có thể đổi SU KEN → CONG_TAC_XA_HOI miễn là chưa có entries. | **Trung bình** |
| **ActivityResponse có presetCode** | Response giờ có field `presetCode`. `presetConfig` để `null` (chưa lưu vào DB). | FE dùng `presetCode` để pre-select preset khi mở form edit. Khôi phục `presetConfig` từ `scoreRules` hiện tại hoặc dùng default của preset. | **Trung bình** |
| **SeriesResponse mở rộng** | Thêm `audience`, `targetDepartmentIds`, `presetCode`, `presetConfig`. | FE hiển thị audience/department trên màn hình chi tiết Series. Dùng `presetCode` để pre-select khi edit. | **Trung bình** |
| **Series update: fix nesting bug** | `targetSemesterId` không còn bị kẹt trong `minimumPenaltyPoints`. Update target semester hoàn toàn độc lập. | FE không cần thay đổi logic. | **Thấp** |
| **Series update: scoreType không bắt buộc** | Update Series không còn bắt buộc gửi `scoreType`. Có thể partial update chỉ name/mô tả. | FE có thể gửi update chỉ các field cần thay đổi, không phải gửi full payload. | **Thấp** |
| **applySeriesPreset abstraction** | Controller dùng `scorePresetService.applySeriesPreset()` thay vì resolve thủ công. Hành vi không đổi với FE. | FE không cần thay đổi. | **Thấp** |

---

## 2. API Contract Changes

### 2.1 Enums & Types mới/cập nhật

```ts
// ScoreSemesterPolicy: KHÔNG THAY ĐỔI, chỉ có 2 giá trị
export type ScoreSemesterPolicy =
  | "ACTIVITY_SEMESTER"
  | "EXPLICIT_SEMESTER";

// FieldDefinition: inputType có thêm MULTI_SELECT, visibility có thêm các giá trị mới
export interface FieldDefinition {
  fieldName: string;
  label: string;
  inputType: 'NUMBER' | 'BOOLEAN' | 'SELECT' | 'MAP' | 'MULTI_SELECT';
  required: boolean;
  defaultValue: any;
  visibility: 'ALWAYS' | 'rule_enabled' | 'audience_department_scoped' | 'semester_policy_explicit';
  options?: string[] | null;
}
```

### 2.2 ActivityPresetConfig — thêm fields

```ts
export interface ActivityPresetConfig {
  // ... các field cũ giữ nguyên
  primaryScoreType?: ScoreType | null;
  participationPoints?: number | string | null;
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

  // === MỚI ===
  audience?: ScoreRuleAudience | null;            // mặc định ALL_PARTICIPANTS
  semesterPolicy?: ScoreSemesterPolicy | null;    // mặc định ACTIVITY_SEMESTER
  explicitSemesterId?: number | null;
  departmentIds?: number[] | null;
}
```

### 2.3 SeriesPresetConfig — thêm fields

```ts
export interface SeriesPresetConfig {
  primaryScoreType?: ScoreType | null;
  milestonePoints?: Record<number, number>;
  minimumRequirementEnabled?: boolean | null;
  minimumRequiredEvents?: number | null;
  minimumPenaltyPoints?: number | null;

  // === MỚI ===
  audience?: ScoreRuleAudience | null;
  departmentIds?: number[] | null;
}
```

### 2.4 StandardActivityUpdateRequest — thêm type

```diff
 export interface StandardActivityUpdateRequest {
   name?: string | null;
+  type?: ActivityType | null;           // CÓ THỂ đổi type khi update
   description?: string | null;
   // ... còn lại giữ nguyên
 }
```

### 2.5 CreateSeriesRequest / UpdateSeriesRequest — thêm fields

```diff
 export interface CreateSeriesRequest {
   // ... các field cũ giữ nguyên
+  audience?: ScoreRuleAudience | null;
+  departmentIds?: number[] | null;
   presetCode?: SeriesPresetCode | null;
   presetConfig?: SeriesPresetConfig | null;
 }

 export interface UpdateSeriesRequest {
   // ... các field cũ giữ nguyên
+  audience?: ScoreRuleAudience | null;
+  departmentIds?: number[] | null;
   presetCode?: SeriesPresetCode | null;
   presetConfig?: SeriesPresetConfig | null;
 }
```

### 2.6 ActivityResponse / StandardActivityResponse — thêm presetCode + activeScoreEntryCount

```diff
 export interface ActivityResponse {
   // ... các field cũ giữ nguyên
+  presetCode?: ActivityPresetCode | null;
+  presetConfig?: ActivityPresetConfig | null;     // luôn null hiện tại (chưa lưu DB)
+  activeScoreEntryCount: number;                  // số lượng score entries ACTIVE
 }

 export interface StandardActivityResponse {
   // ... các field cũ giữ nguyên
+  presetCode?: ActivityPresetCode | null;
+  presetConfig?: ActivityPresetConfig | null;     // luôn null hiện tại
+  activeScoreEntryCount: number;
 }
```

### 2.7 SeriesResponse — thêm audience + presetCode

```diff
 export interface SeriesResponse {
   // ... các field cũ giữ nguyên
+  audience?: ScoreRuleAudience | null;
+  targetDepartmentIds?: number[] | null;
+  presetCode?: SeriesPresetCode | null;
+  presetConfig?: SeriesPresetConfig | null;       // luôn null hiện tại
   createdAt?: string | null;
 }
```

---

## 3. supportedRules — Dynamic Form Render

### 3.1 Activity presets: ACTIVITY_AUDIENCE descriptor

`GET /api/activities/presets` giờ trả thêm 1 `PresetRuleDescriptor` với `ruleKey = "ACTIVITY_AUDIENCE"`:

```json
{
  "ruleKey": "ACTIVITY_AUDIENCE",
  "label": "Gioi han doi tuong nhan diem",
  "description": "Kiem soat viec student thuoc khoa nao se duoc cong/tru diem tu su kien nay.",
  "required": false,
  "enabledByDefault": false,
  "fieldDefinitions": [
    {
      "fieldName": "audience",
      "label": "Doi tuong ap dung",
      "inputType": "SELECT",
      "required": true,
      "defaultValue": "ALL_PARTICIPANTS",
      "visibility": "ALWAYS",
      "options": ["ALL_PARTICIPANTS", "DEPARTMENT_ONLY", "OUTSIDE_DEPARTMENTS_ONLY"]
    },
    {
      "fieldName": "departmentIds",
      "label": "Danh sach Khoa",
      "inputType": "MULTI_SELECT",
      "required": false,
      "defaultValue": [],
      "visibility": "audience_department_scoped"
    },
    {
      "fieldName": "semesterPolicy",
      "label": "Hoc ky cong diem",
      "inputType": "SELECT",
      "required": true,
      "defaultValue": "ACTIVITY_SEMESTER",
      "visibility": "ALWAYS",
      "options": ["ACTIVITY_SEMESTER", "EXPLICIT_SEMESTER"]
    },
    {
      "fieldName": "explicitSemesterId",
      "label": "Hoc ky chi dinh",
      "inputType": "SELECT",
      "required": false,
      "defaultValue": null,
      "visibility": "semester_policy_explicit"
    }
  ]
}
```

### 3.2 Series presets: SERIES_AUDIENCE descriptor

`GET /api/series/presets` giờ trả thêm 1 `PresetRuleDescriptor` với `ruleKey = "SERIES_AUDIENCE"`:

```json
{
  "ruleKey": "SERIES_AUDIENCE",
  "label": "Gioi han doi tuong nhan diem",
  "description": "Kiem soat viec student thuoc khoa nao se duoc cong/tru diem tu chuoi nay.",
  "required": false,
  "enabledByDefault": false,
  "fieldDefinitions": [
    {
      "fieldName": "audience",
      "label": "Doi tuong ap dung",
      "inputType": "SELECT",
      "required": true,
      "defaultValue": "ALL_PARTICIPANTS",
      "visibility": "ALWAYS",
      "options": ["ALL_PARTICIPANTS", "DEPARTMENT_ONLY", "OUTSIDE_DEPARTMENTS_ONLY"]
    },
    {
      "fieldName": "departmentIds",
      "label": "Danh sach Khoa",
      "inputType": "MULTI_SELECT",
      "required": false,
      "defaultValue": [],
      "visibility": "audience_department_scoped"
    }
  ]
}
```

### 3.3 FE render logic cho MULTI_SELECT và visibility

```ts
// Pseudocode render logic cho FieldDefinition mới:
function shouldShowField(field: FieldDefinition, config: Record<string, any>): boolean {
  switch (field.visibility) {
    case 'ALWAYS': return true;
    case 'rule_enabled': return true; // rule được bật
    case 'audience_department_scoped':
      // Hiện khi audience != ALL_PARTICIPANTS
      return config.audience !== 'ALL_PARTICIPANTS' && config.audience != null;
    case 'semester_policy_explicit':
      // Hiện khi semesterPolicy == EXPLICIT_SEMESTER
      return config.semesterPolicy === 'EXPLICIT_SEMESTER';
    default: return true;
  }
}

function renderField(field: FieldDefinition): JSX.Element {
  switch (field.inputType) {
    case 'MULTI_SELECT':
      return <DepartmentMultiSelect
               value={config.departmentIds}
               onChange={(ids) => setConfig({ ...config, departmentIds: ids })}
             />;
    case 'SELECT':
      return <Select options={field.options} ... />;
    // ... existing cases
  }
}
```

---

## 4. Rule-key mapping cho supportedRules (đầy đủ)

| ruleKey | Mô tả | Preset áp dụng |
|---------|-------|---------------|
| `PARTICIPATION_COMPLETED` | Cộng điểm hoàn thành | EVENT_BASIC, ENTERPRISE_SEMINAR_BASIC, ENTERPRISE_SEMINAR_WITH_BONUS |
| `SUBMISSION_GRADED` | Điểm chấm bài nộp | EVENT_WITH_SUBMISSION |
| `TASK_OVERDUE` | Phạt nộp trễ | EVENT_WITH_SUBMISSION |
| `BONUS_POINTS` | Cộng điểm thưởng | ENTERPRISE_SEMINAR_WITH_BONUS |
| `MINIGAME_PASSED` | Điểm hoàn thành minigame | MINIGAME_PASS_ONLY |
| `MINIGAME_EXHAUSTED_ATTEMPTS` | Phạt hết lượt chơi | MINIGAME_PASS_ONLY |
| `NO_SHOW` | Phạt vắng mặt | EVENT_BASIC, EVENT_WITH_SUBMISSION, ENTERPRISE_SEMINAR_BASIC, ENTERPRISE_SEMINAR_WITH_BONUS, MINIGAME_PASS_ONLY |
| **`ACTIVITY_AUDIENCE`** ✨ | **Giới hạn đối tượng (activity)** | **Tất cả preset** |
| `MILESTONE_POINTS` | Điểm mốc tích lũy | SERIES_MILESTONE_BASIC, ENTERPRISE_SERIES |
| `MINIMUM_REQUIREMENT` | Yêu cầu tối thiểu | SERIES_MILESTONE_BASIC, ENTERPRISE_SERIES |
| **`SERIES_AUDIENCE`** ✨ | **Giới hạn đối tượng (series)** | **Tất cả series preset** |

---

## 5. Lỗi có thể gặp từ backend (Error handling)

| HTTP Status | Type | Message mẫu | Khi nào |
|-------------|------|------------|---------|
| 400 | `Response.error` | `"Cannot send custom scoreRules with preset EVENT_BASIC. Use CUSTOM preset for manual rules."` | Gửi `scoreRules` kèm preset không phải CUSTOM |
| 400 | `Response.error` | `"Cannot change type when activity has 5 active score entries and is not draft."` | Đổi type của activity đã có entries |
| 400 | `Response.error` | `"Cannot modify score rules when activity has 3 active score entries and is not in draft."` | Sửa rules của activity đã có entries |
| 400 | `Response.error` | `"departmentIds are required for department-scoped rules"` | audience=DEPARTMENT_ONLY nhưng không gửi departmentIds |
| 400 | `Response.error` | `"explicitSemesterId is required when semesterPolicy is EXPLICIT_SEMESTER"` | Chọn EXPLICIT_SEMESTER nhưng không chọn học kỳ |

---

## 6. DB Schema Changes (migration)

```sql
ALTER TABLE activities ADD COLUMN preset_code VARCHAR(50);
ALTER TABLE activity_series ADD COLUMN preset_code VARCHAR(50);
ALTER TABLE activity_series ADD COLUMN audience VARCHAR(50) DEFAULT 'ALL_PARTICIPANTS';
CREATE TABLE IF NOT EXISTS activity_series_departments (
    series_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    PRIMARY KEY (series_id, department_id)
);
```

---

## 7. Test Cases cho FE

| # | Mô tả | Verify |
|---|-------|--------|
| 1 | Gọi `GET /api/activities/presets` | `supportedRules` có descriptor `ACTIVITY_AUDIENCE` với 4 FieldDefinition |
| 2 | Gọi `GET /api/series/presets` | `supportedRules` có descriptor `SERIES_AUDIENCE` với 2 FieldDefinition |
| 3 | Tạo activity `EVENT_BASIC` + `presetConfig.audience=DEPARTMENT_ONLY` + `presetConfig.departmentIds=[1,2]` | `GET /api/activities/{id}` → `scoreRules[0].audience = "DEPARTMENT_ONLY"`, `targetDepartmentIds = [1,2]` |
| 4 | Tạo activity `EVENT_BASIC` không gửi audience | `GET /api/activities/{id}` → `scoreRules[0].audience = "ALL_PARTICIPANTS"` (default) |
| 5 | Tạo series `SERIES_MILESTONE_BASIC` + `audience=DEPARTMENT_ONLY` + `departmentIds=[1]` | `GET /api/series/{id}` → `audience = "DEPARTMENT_ONLY"`, `targetDepartmentIds = [1]` |
| 6 | Edit activity qua `/api/activities/standard/{id}` với `type = "CONG_TAC_XA_HOI"` (draft) | Update thành công, type đổi |
| 7 | Edit activity qua `/api/activities/standard/{id}` với `type` khác, activity đã publish + có entries | Lỗi "Cannot change type..." |
| 8 | Edit activity qua `/api/activities/{id}` không gửi `scoreRules` | Rules hiện tại KHÔNG bị mất (null guard fix) |
| 9 | `GET /api/activities/{id}` | Response có `presetCode` | 
| 10 | `GET /api/series/{id}` | Response có `audience`, `targetDepartmentIds`, `presetCode` |
