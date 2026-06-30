# FE Integration Plan — Preset ScoreRule Toggle Support

> **Scope:** Frontend tích hợp khả năng bật/tắt các rule đi kèm `presetCode` theo metadata `required` và `enabledByDefault` trả về từ backend.
> **Backend Source of Truth:** `ScorePresetServiceImpl.java`, `PresetRuleDescriptor`, `FieldDefinition`.
> **Affected FE Flows:** Tạo hoạt động (Standard / Minigame), Cập nhật hoạt động, Preset Preview.
>
> **⚠️ Outdated (P6.1):** Tài liệu này tham chiếu `participationFailPoints` đã bị xoá khỏi `PARTICIPATION_COMPLETED`. Xem `docs/refactor/FE_SPEC_v6_PRESET_AND_REGISTRATION.md` và `docs/refactor/FE_BACKEND_HANDOFF_SPEC.md` cho spec mới nhất.

---

## 1. Tổng Quan Thay Đổi

Backend đã hỗ trợ trả về danh sách `supportedRules` kèm theo từng `presetCode`. Mỗi rule có 2 cờ quan trọng:

| Cờ | Ý nghĩa | UI Behavior |
|----|---------|-------------|
| `required` | `true` = rule bắt buộc, không thể tắt | Toggle bị `disabled`, luôn `checked`/`on` |
| `enabledByDefault` | `true` = mặc định bật khi load preset | Toggle có thể bật/tắt tự do (nếu `required=false`) |

**Ba nhóm rule thực tế:**
1. **Bắt buộc + luôn bật** (`required=true`, `enabledByDefault=true`) — VD: `PARTICIPATION_COMPLETED`, `SUBMISSION_GRADED`.
2. **Mặc định bật + có thể tắt** (`required=false`, `enabledByDefault=true`) — VD: `NO_SHOW` (một số preset), `BONUS_POINTS`.
3. **Mặc định tắt + có thể bật** (`required=false`, `enabledByDefault=false`) — VD: `TASK_OVERDUE`, `MINIGAME_EXHAUSTED_ATTEMPTS`.

Ngoài ra, mỗi rule có `fieldDefinitions` với `visibility`: `ALWAYS` (luôn hiển thị) hoặc `rule_enabled` (chỉ hiển thị khi rule được bật).

---

## 2. API Endpoints Liên Quan

### 2.1 Lấy định nghĩa preset (metadata + supportedRules)

- **Method:** `GET`
- **Path:** `/api/activities/presets`
- **Authentication:** Required (Admin/Manager)
- **Response:** `ApiResponse<ActivityPresetDefinitionResponse[]>`

```json
{
  "status": true,
  "message": "success",
  "body": [
    {
      "code": "EVENT_BASIC",
      "displayName": "Sự kiện thường",
      "description": "...",
      "recommendedActivityTypes": ["SUKIEN", "CONG_TAC_XA_HOI"],
      "defaultRequiresSubmission": false,
      "notes": ["..."],
      "supportedRules": [
        {
          "ruleKey": "PARTICIPATION_COMPLETED",
          "label": "Cộng điểm hoàn thành",
          "description": "Tự động cộng điểm khi check-in/check-out thành công.",
          "required": true,
          "enabledByDefault": true,
          "fieldDefinitions": [
            {
              "fieldName": "primaryScoreType",
              "label": "Loại điểm chính",
              "inputType": "SELECT",
              "required": true,
              "defaultValue": "REN_LUYEN",
              "visibility": "ALWAYS",
              "options": ["REN_LUYEN", "CONG_TAC_XA_HOI", "CHUYEN_DE"]
            },
            {
              "fieldName": "participationPoints",
              "label": "Điểm hoàn thành",
              "inputType": "NUMBER",
              "required": true,
              "defaultValue": 5,
              "visibility": "ALWAYS"
            }
          ]
        },
        {
          "ruleKey": "NO_SHOW",
          "label": "Phạt vắng mặt (No-show)",
          "description": "Trừ điểm khi sinh viên đã đăng ký nhưng không đến tham gia.",
          "required": false,
          "enabledByDefault": true,
          "fieldDefinitions": [
            {
              "fieldName": "noShowPenaltyEnabled",
              "label": "Bật phạt vắng mặt",
              "inputType": "BOOLEAN",
              "required": true,
              "defaultValue": true,
              "visibility": "ALWAYS"
            },
            {
              "fieldName": "noShowPenaltyPoints",
              "label": "Số điểm phạt",
              "inputType": "NUMBER",
              "required": true,
              "defaultValue": 5,
              "visibility": "rule_enabled"
            },
            {
              "fieldName": "noShowPenaltyScoreType",
              "label": "Loại điểm phạt",
              "inputType": "SELECT",
              "required": false,
              "defaultValue": null,
              "visibility": "rule_enabled",
              "options": ["REN_LUYEN", "CONG_TAC_XA_HOI", "CHUYEN_DE"]
            }
          ]
        }
      ]
    }
  ]
}
```

### 2.2 Preview preset (trả về scoreRules đã sinh)

- **Method:** `POST`
- **Path:** `/api/activities/presets/preview`
- **Body:** `ActivityPresetPreviewRequest`
- **Response:** `ApiResponse<ActivityPresetPreviewResponse>`

```json
{
  "presetCode": "EVENT_BASIC",
  "type": "SUKIEN",
  "requiresSubmission": false,
  "presetConfig": {
    "primaryScoreType": "REN_LUYEN",
    "participationPoints": 5,
    "participationFailPoints": 0,
    "noShowPenaltyEnabled": true,
    "noShowPenaltyPoints": 5,
    "noShowPenaltyScoreType": null
  }
}
```

> **Quan trọng:** Khi dùng preset (khác `CUSTOM`), FE **KHÔNG** gửi `scoreRules` trong request tạo/cập nhật activity. Backend tự sinh và đánh dấu `isPresetGenerated = true`.

---

## 3. TypeScript Types Cần Định Nghĩa / Cập Nhật

Các type này đã có trong `FE_BACKEND_HANDOFF_SPEC.md`, nhưng FE cần đảm bảo đủ trường và dùng đúng:

```typescript
export type ActivityPresetCode =
  | 'EVENT_BASIC'
  | 'EVENT_WITH_SUBMISSION'
  | 'ENTERPRISE_SEMINAR_BASIC'
  | 'ENTERPRISE_SEMINAR_WITH_BONUS'
  | 'MINIGAME_PASS_ONLY'
  | 'CUSTOM';

export type InputType = 'NUMBER' | 'BOOLEAN' | 'SELECT' | 'MAP';
export type VisibilityType = 'ALWAYS' | 'rule_enabled';

export interface FieldDefinition {
  fieldName: string;
  label: string;
  inputType: InputType;
  required: boolean;
  defaultValue: any;
  visibility: VisibilityType;
  options?: string[] | null;
}

export interface PresetRuleDescriptor {
  ruleKey: string;           // 'PARTICIPATION_COMPLETED' | 'NO_SHOW' | 'SUBMISSION_GRADED' | ...
  label: string;
  description: string;
  required: boolean;           // true = bắt buộc, không thể tắt
  enabledByDefault: boolean;   // trạng thái mặc định khi load preset
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

export interface ActivityPresetConfig {
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
}

export interface ActivityPresetPreviewRequest {
  presetCode: ActivityPresetCode;
  type?: ActivityType | null;
  requiresSubmission?: boolean | null;
  presetConfig?: ActivityPresetConfig | null;
}

export interface ActivityPresetPreviewResponse {
  presetCode: ActivityPresetCode;
  activityType: ActivityType;
  requiresSubmission: boolean;
  scoreRules: ActivityScoreRuleRequest[];
  notes: string[];
}
```

---

## 4. UI/UX Integration Plan

### 4.1 Preset Selector (Dropdown / Card)

1. Load danh sách preset từ `GET /api/activities/presets`.
2. Hiển thị: `displayName` + `description` + `recommendedActivityTypes`.
3. Khi user chọn preset != `CUSTOM`:
   - Render panel **"Cấu hình rule tự động"** từ `supportedRules`.
   - Ẩn hoặc disable phần nhập `scoreRules` thủ công.
   - Reset `scoreRules` trong form payload về `null` / `undefined`.
4. Khi chọn `CUSTOM`:
   - Hiển thị form nhập `scoreRules` thủ công (table / accordion).
   - Không gọi preset APIs.

### 4.2 Dynamic Rule Renderer (Theo `supportedRules`)

Với mỗi `PresetRuleDescriptor` trong `supportedRules`, render một card / row:

```
┌─────────────────────────────────────────────────────────┐
│ [Toggle] PARTICIPATION_COMPLETED — Cộng điểm hoàn thành │  ← required=true → toggle disabled, checked
│  Mô tả: Tự động cộng điểm...                              │
│  ───────────────────────────────────────────────────────  │
│  Loại điểm chính: [SELECT: REN_LUYEN ▼]                   │  ← visibility=ALWAYS
│  Điểm hoàn thành: [NUMBER: 5]                             │  ← visibility=ALWAYS
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ [Toggle] NO_SHOW — Phạt vắng mặt (No-show)               │  ← required=false, enabledByDefault=true
│  Mô tả: Trừ điểm khi...                                   │
│  ───────────────────────────────────────────────────────  │
│  Bật phạt vắng mặt: [BOOLEAN: ☑]                         │  ← visibility=ALWAYS
│  Số điểm phạt: [NUMBER: 5]                               │  ← visibility=rule_enabled → show khi toggle ON
│  Loại điểm phạt: [SELECT: null ▼]                       │  ← visibility=rule_enabled → show khi toggle ON
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ [Toggle] TASK_OVERDUE — Phạt nộp trễ                    │  ← required=false, enabledByDefault=false
│  Mô tả: Trừ điểm khi...                                   │
│  ───────────────────────────────────────────────────────  │
│  (Không hiển thị field nào khi toggle OFF)               │
│  Điểm phạt nộp trễ: [NUMBER: 0]                         │  ← visibility=rule_enabled → show khi toggle ON
└─────────────────────────────────────────────────────────┘
```

### 4.3 Rule Toggle Behavior — 3 Trạng Thái

| `required` | `enabledByDefault` | FE Toggle State | Có thể tắt? | Gửi lên BE? |
|------------|--------------------|-----------------|-------------|-------------|
| `true` | `true` | `checked`, `disabled` | **Không** | Luôn (qua `presetConfig` / `preview`) |
| `false` | `true` | `checked`, `enabled` | **Có** | Phụ thuộc vào user toggle |
| `false` | `false` | `unchecked`, `enabled` | **Có** (bật) | Phụ thuộc vào user toggle |

**Logic Toggle:**
- Khi user toggle một rule `required=false`:
  - Nếu bật → các field có `visibility: 'rule_enabled'` được hiển thị.
  - Nếu tắt → các field `visibility: 'rule_enabled'` bị ẩn. Giá trị của chúng vẫn được giữ trong state nhưng không đưa vào `presetConfig` (hoặc đưa vào nhưng backend sẽ ignore nếu rule không sinh ra).
- Khi user toggle `NO_SHOW`:
  - `noShowPenaltyEnabled` trong `presetConfig` phải đồng bộ với trạng thái toggle.

### 4.4 Field Visibility Logic (`visibility`)

```typescript
function shouldShowField(field: FieldDefinition, isRuleEnabled: boolean): boolean {
  if (field.visibility === 'ALWAYS') return true;
  if (field.visibility === 'rule_enabled') return isRuleEnabled;
  return true;
}
```

- `ALWAYS`: Input field luôn render, bất kể rule bật/tắt.
- `rule_enabled`: Input field chỉ render khi rule đang bật (toggle = ON).
- Lưu ý: Một số field có `fieldName` trùng với toggle flag (ví dụ `noShowPenaltyEnabled`). Field này thường có `visibility: 'ALWAYS'` để user có thể bật/tắt penalty trước khi các field phạt hiện ra.

### 4.5 Mapping `ruleKey` ↔ `presetConfig` Fields

FE cần biết cách map từng `ruleKey` sang các field trong `ActivityPresetConfig` để build request body cho `preview` và create/update:

| Rule Key |presetConfig Fields Liên Quan |
|----------|------------------------------|
| `PARTICIPATION_COMPLETED` | `primaryScoreType`, `participationPoints`, `participationFailPoints` |
| `SUBMISSION_GRADED` | `primaryScoreType`, `submissionPassPoints`, `submissionFailPoints` |
| `TASK_OVERDUE` | `taskOverduePenaltyPoints` |
| `NO_SHOW` | `noShowPenaltyEnabled`, `noShowPenaltyPoints`, `noShowPenaltyScoreType` |
| `BONUS_POINTS` | `bonusScoreType`, `bonusPoints` |
| `MINIGAME_PASSED` | `primaryScoreType`, `participationPoints` |
| `MINIGAME_EXHAUSTED_ATTEMPTS` | `minigameExhaustedPenaltyPoints` |
| `MILESTONE_POINTS` | `primaryScoreType`, `milestonePoints` |
| `MINIMUM_REQUIREMENT` | `minimumRequirementEnabled`, `minimumRequiredEvents`, `minimumPenaltyPoints` |

**Cách xử lý:** Không hardcode mapping trong component. Thay vào đó, đọc `fieldName` trực tiếp từ `fieldDefinitions` của rule và set vào object `presetConfig` theo key tương ứng.

### 4.6 Form Submission Logic (Create / Update)

Khi user nhấn **Tạo / Cập nhật**:

```typescript
function buildActivityPayload(formState: FormState): CreateActivityRequest {
  const base = { /* ... các trường cơ bản ... */ };

  if (formState.presetCode && formState.presetCode !== 'CUSTOM') {
    // Dùng preset → chỉ gửi presetCode + presetConfig
    return {
      ...base,
      presetCode: formState.presetCode,
      presetConfig: formState.presetConfig, // object đã build từ các field input
      scoreRules: undefined, // KHÔNG gửi scoreRules
    };
  } else {
    // CUSTOM → gửi scoreRules thủ công
    return {
      ...base,
      presetCode: 'CUSTOM',
      presetConfig: undefined,
      scoreRules: formState.manualScoreRules,
    };
  }
}
```

> **Validation:** Nếu `presetCode !== 'CUSTOM'` và `scoreRules` có phần tử, backend sẽ throw `400 IllegalArgumentException`. FE nên tự strip `scoreRules` trước khi gửi.

---

## 5. State Management Flow (React / Vue — Pseudocode)

```typescript
interface PresetFormState {
  selectedPresetCode: ActivityPresetCode | null;
  presetDefinitions: ActivityPresetDefinitionResponse[] | null;
  
  // presetConfig đang được edit trong form
  presetConfig: ActivityPresetConfig;
  
  // Map<ruleKey, boolean> — trạng thái bật/tắt của từng rule (do user toggle)
  // Với rule required=true, luôn true và không cho phép edit
  enabledRules: Record<string, boolean>;
  
  // Preview response từ BE (dùng để hiển thị scoreRules sẽ sinh ra)
  previewResponse: ActivityPresetPreviewResponse | null;
}

// Khởi tạo khi chọn preset
function initPresetState(preset: ActivityPresetDefinitionResponse): PresetFormState {
  const enabledRules: Record<string, boolean> = {};
  const presetConfig: ActivityPresetConfig = {};

  for (const rule of preset.supportedRules) {
    // Rule bắt buộc luôn bật
    enabledRules[rule.ruleKey] = rule.required ? true : rule.enabledByDefault;

    for (const field of rule.fieldDefinitions) {
      // Set default values vào presetConfig
      if (field.defaultValue !== undefined && field.defaultValue !== null) {
        (presetConfig as any)[field.fieldName] = field.defaultValue;
      }
    }
  }

  return { selectedPresetCode: preset.code, presetDefinitions: [preset], presetConfig, enabledRules, previewResponse: null };
}

// Khi user toggle rule hoặc thay đổi field → gọi preview
async function refreshPreview(state: PresetFormState): Promise<void> {
  if (!state.selectedPresetCode || state.selectedPresetCode === 'CUSTOM') return;

  const request: ActivityPresetPreviewRequest = {
    presetCode: state.selectedPresetCode,
    type: /* form activity type */,
    requiresSubmission: /* form requiresSubmission */,
    presetConfig: state.presetConfig,
  };

  const response = await api.post('/api/activities/presets/preview', request);
  state.previewResponse = response.body;
  
  // Có thể dùng previewResponse.scoreRules để hiển thị bản xem trước rule sẽ sinh ra
}
```

---

## 6. Validation Rules (FE-side)

| # | Rule | Mức độ |
|---|------|--------|
| 1 | Khi `presetCode !== 'CUSTOM'`, `scoreRules` phải bị xóa khỏi payload trước khi gửi. | P0 |
| 2 | Các field có `inputType: 'NUMBER'` và `required: true` không được để trống. | P0 |
| 3 | Các field có `inputType: 'SELECT'` và `required: true` phải chọn giá trị hợp lệ thuộc `options`. | P0 |
| 4 | Toggle `NO_SHOW` cho Seminar preset: nếu bật, `noShowPenaltyScoreType` không được trùng với `primaryScoreType` nếu `primaryScoreType === 'CHUYEN_DE'` (hoặc để backend validate). | P1 |
| 5 | `participationPoints` / `submissionPassPoints` phải >= 0 (hoặc theo business rule). | P1 |
| 6 | `taskOverduePenaltyPoints` / `noShowPenaltyPoints` / `minigameExhaustedPenaltyPoints` nên >= 0 (dù backend dùng làm `failPoints`, FE nên validate số dương). | P1 |

---

## 7. UI Components Đề Xuất

### 7.1 `PresetRuleCard`
Props:
- `rule: PresetRuleDescriptor`
- `enabled: boolean` (trạng thái toggle hiện tại)
- `onToggle: (ruleKey: string, enabled: boolean) => void`
- `fieldValues: Record<string, any>` (giá trị hiện tại của các field)
- `onFieldChange: (fieldName: string, value: any) => void`

Behavior:
- Render header: Toggle + `label` + Badge `required ? "Bắt buộc" : "Tùy chọn"`
- Render description text nhỏ.
- Render fields theo `visibility`.

### 7.2 `PresetConfigPanel`
Props:
- `presetDefinition: ActivityPresetDefinitionResponse`
- `config: ActivityPresetConfig`
- `onConfigChange: (config: ActivityPresetConfig) => void`
- `onPreview: () => void`

Behavior:
- Render danh sách `PresetRuleCard`.
- Có nút "Xem trước rule" để gọi preview API.
- Hiển thị `previewResponse.scoreRules` dạng bảng tóm tắt.

### 7.3 `ScoreRuleBadge`
Hiển thị trong màn chi tiết / danh sách hoạt động:
- `isPresetGenerated === true` → Badge màu xanh: "Preset"
- `isPresetGenerated === false || null` → Badge màu cam: "Tùy chỉnh"

---

## 8. Các Preset Cụ Thể & Lưu Ý FE

### EVENT_BASIC
- `PARTICIPATION_COMPLETED`: required=true, enabledByDefault=true → luôn bật.
- `NO_SHOW`: required=false, enabledByDefault=true → mặc định bật, có thể tắt. Nếu tắt, `noShowPenaltyEnabled` trong config phải là `false`.

### EVENT_WITH_SUBMISSION
- `SUBMISSION_GRADED`: required=true, enabledByDefault=true → luôn bật.
- `TASK_OVERDUE`: required=false, enabledByDefault=false → mặc định tắt. Nếu bật, `taskOverduePenaltyPoints` phải > 0 để có ý nghĩa.
- `NO_SHOW`: required=false, enabledByDefault=true → mặc định bật (có thể tắt).

### ENTERPRISE_SEMINAR_BASIC
- `PARTICIPATION_COMPLETED`: required=true, enabledByDefault=true.
- `NO_SHOW`: required=false, enabledByDefault=false → mặc định tắt. Nếu bật, validate `noShowPenaltyScoreType` != `CHUYEN_DE` (để tránh trừ ngược tích lũy).

### ENTERPRISE_SEMINAR_WITH_BONUS
- `PARTICIPATION_COMPLETED`: required=true, enabledByDefault=true.
- `BONUS_POINTS`: required=false, enabledByDefault=true → mặc định bật, có thể tắt.
- `NO_SHOW`: required=false, enabledByDefault=false → mặc định tắt.

### MINIGAME_PASS_ONLY
- `MINIGAME_PASSED`: required=true, enabledByDefault=true.
- `NO_SHOW`: required=false, enabledByDefault=true → mặc định bật. `noShowPenaltyPoints` mặc định = `participationPoints` (5). FE nhập dương, BE tự negate.
- `MINIGAME_EXHAUSTED_ATTEMPTS`: required=false, enabledByDefault=false → mặc định tắt.

### CUSTOM
- Không có `supportedRules`. Hiển thị form nhập `scoreRules` thủ công.

---

## 9. Acceptance Criteria (Definition of Done)

- [ ] FE load đúng danh sách preset từ `GET /api/activities/presets`.
- [ ] FE render đúng 3 loại rule: bắt buộc (disabled toggle), mặc định bật (toggleable), mặc định tắt (toggleable).
- [ ] FE ẩn/hiện field có `visibility: 'rule_enabled'` đúng theo trạng thái toggle.
- [ ] FE gọi `POST /api/activities/presets/preview` để preview scoreRules khi user thay đổi config.
- [ ] FE gửi đúng `presetConfig` (không gửi `scoreRules`) khi dùng preset.
- [ ] FE hiển thị badge "Preset" cho rules có `isPresetGenerated = true` trong màn chi tiết / danh sách hoạt động.
- [ ] FE không bị lỗi khi backend thay đổi default values hoặc thêm rule mới (dynamic rendering từ `supportedRules`).
- [ ] FE validate cơ bản (required field, number range) trước khi submit.

---

## 10. Tệp Tin FE Dự Kiến Cần Thay Đổi (Giả Định Cấu Trúc)

> Đây là ví dụ cho React/TypeScript. Điều chỉnh tên đường dẫn theo cấu trúc thực tế của dự án FE.

```
src/
  types/
    activity.ts           ← Thêm/cập nhật các type preset (PresetRuleDescriptor, FieldDefinition, ActivityPresetConfig, ...)
  api/
    activityApi.ts          ← Thêm hàm getActivityPresets(), previewActivityPreset()
  components/
    PresetSelector.tsx      ← Dropdown chọn preset, hiển thị metadata
    PresetRuleCard.tsx      ← Card render 1 rule với toggle + fields
    PresetConfigPanel.tsx   ← Panel chứa danh sách PresetRuleCard + nút Preview
    ScoreRuleBadge.tsx      ← Badge hiển thị Preset / Custom
  hooks/
    useActivityPresets.ts   ← Hook load preset definitions + cache
    usePresetPreview.ts     ← Hook gọi preview API + quản lý state config
  pages/
    CreateActivityPage.tsx  ← Tích hợp PresetConfigPanel vào form tạo hoạt động
    EditActivityPage.tsx    ← Tích hợp PresetConfigPanel vào form sửa hoạt động
    ActivityDetailPage.tsx  ← Hiển thị badge Preset cho scoreRules
```

---

## 11. Series Preset Integration — Chi Tiết Tích Hợp

> **Scope:** FE tích hợp form tạo/cập nhật **Chuỗi sự kiện (Series)** với `SeriesPresetCode` và `SeriesPresetConfig`, sử dụng cùng cơ chế dynamic rendering từ `supportedRules` như Activity preset.
> **Backend Source:** `ScorePresetServiceImpl.getSeriesPresetDefinitions()`, `SeriesPresetDefinitionResponse`, `SeriesPresetConfig`.

### 11.1 API Endpoints Series Preset

#### Lấy định nghĩa preset chuỗi
- **Method:** `GET`
- **Path:** `/api/series/presets`
- **Authentication:** Required (Admin/Manager)
- **Response:** `ApiResponse<SeriesPresetDefinitionResponse[]>`

```json
{
  "status": true,
  "message": "success",
  "body": [
    {
      "code": "SERIES_MILESTONE_BASIC",
      "displayName": "Series milestone cơ bản",
      "description": "Định nghĩa các mốc hoàn thành và điểm thưởng tương ứng cho chuỗi sự kiện.",
      "notes": [
        "Mặc định scoreType là REN_LUYEN.",
        "Phù hợp cho chuỗi sự kiện thông thường cần mốc 3/5/7."
      ],
      "supportedRules": [
        {
          "ruleKey": "MILESTONE_POINTS",
          "label": "Điểm mốc tích lũy (Milestones)",
          "description": "Cộng điểm thưởng khi sinh viên đạt các mốc số lượng sự kiện con đã hoàn thành.",
          "required": true,
          "enabledByDefault": true,
          "fieldDefinitions": [
            {
              "fieldName": "primaryScoreType",
              "label": "Loại điểm mốc",
              "inputType": "SELECT",
              "required": true,
              "defaultValue": "REN_LUYEN",
              "visibility": "ALWAYS",
              "options": ["REN_LUYEN", "CONG_TAC_XA_HOI", "CHUYEN_DE"]
            },
            {
              "fieldName": "milestonePoints",
              "label": "Cấu hình mốc điểm",
              "inputType": "MAP",
              "required": true,
              "defaultValue": { "3": 5, "5": 10, "7": 15 },
              "visibility": "ALWAYS"
            }
          ]
        },
        {
          "ruleKey": "MINIMUM_REQUIREMENT",
          "label": "Yêu cầu tối thiểu",
          "description": "Phạt điểm nếu đăng ký tham gia chuỗi nhưng không đạt số sự kiện tối thiểu.",
          "required": false,
          "enabledByDefault": false,
          "fieldDefinitions": [
            {
              "fieldName": "minimumRequirementEnabled",
              "label": "Bật yêu cầu tối thiểu",
              "inputType": "BOOLEAN",
              "required": true,
              "defaultValue": false,
              "visibility": "ALWAYS"
            },
            {
              "fieldName": "minimumRequiredEvents",
              "label": "Số lượng sự kiện tối thiểu",
              "inputType": "NUMBER",
              "required": true,
              "defaultValue": 3,
              "visibility": "rule_enabled"
            },
            {
              "fieldName": "minimumPenaltyPoints",
              "label": "Số điểm phạt",
              "inputType": "NUMBER",
              "required": true,
              "defaultValue": 5,
              "visibility": "rule_enabled"
            }
          ]
        }
      ]
    },
    {
      "code": "ENTERPRISE_SERIES",
      "displayName": "Series chuyên đề doanh nghiệp",
      "description": "Chuỗi chuyên đề doanh nghiệp tích lũy theo số buổi hoàn thành.",
      "notes": [
        "Mặc định scoreType là CHUYEN_DE.",
        "Mốc điểm mặc định tăng dần theo số buổi đã tham gia."
      ],
      "supportedRules": [
        {
          "ruleKey": "MILESTONE_POINTS",
          "label": "Điểm mốc tích lũy (Milestones)",
          "description": "...",
          "required": true,
          "enabledByDefault": true,
          "fieldDefinitions": [
            {
              "fieldName": "primaryScoreType",
              "label": "Loại điểm mốc",
              "inputType": "SELECT",
              "required": true,
              "defaultValue": "CHUYEN_DE",
              "visibility": "ALWAYS",
              "options": ["REN_LUYEN", "CONG_TAC_XA_HOI", "CHUYEN_DE"]
            },
            {
              "fieldName": "milestonePoints",
              "label": "Cấu hình mốc điểm",
              "inputType": "MAP",
              "required": true,
              "defaultValue": { "1": 1, "3": 3, "5": 5 },
              "visibility": "ALWAYS"
            }
          ]
        },
        {
          "ruleKey": "MINIMUM_REQUIREMENT",
          "label": "Yêu cầu tối thiểu",
          "description": "...",
          "required": false,
          "enabledByDefault": false,
          "fieldDefinitions": [
            {
              "fieldName": "minimumRequirementEnabled",
              "label": "Bật yêu cầu tối thiểu",
              "inputType": "BOOLEAN",
              "required": true,
              "defaultValue": false,
              "visibility": "ALWAYS"
            },
            {
              "fieldName": "minimumRequiredEvents",
              "label": "Số lượng sự kiện tối thiểu",
              "inputType": "NUMBER",
              "required": true,
              "defaultValue": 3,
              "visibility": "rule_enabled"
            },
            {
              "fieldName": "minimumPenaltyPoints",
              "label": "Số điểm phạt",
              "inputType": "NUMBER",
              "required": true,
              "defaultValue": 5,
              "visibility": "rule_enabled"
            }
          ]
        }
      ]
    },
    {
      "code": "CUSTOM",
      "displayName": "Series tùy biến",
      "description": "Tự cấu hình scoreType và milestonePoints.",
      "notes": ["Không tự động sinh mốc milestone."],
      "supportedRules": []
    }
  ]
}
```

#### Preview preset chuỗi
- **Method:** `POST`
- **Path:** `/api/series/presets/preview`
- **Body:** `SeriesPresetPreviewRequest`
- **Response:** `ApiResponse<SeriesPresetPreviewResponse>`

```typescript
export interface SeriesPresetPreviewRequest {
  presetCode: SeriesPresetCode;
  presetConfig?: SeriesPresetConfig | null;
}

export interface SeriesPresetPreviewResponse {
  presetCode: SeriesPresetCode;
  scoreType: ScoreType;
  milestonePoints: Record<number, number>;
  minimumRequirementEnabled?: boolean | null;
  minimumRequiredEvents?: number | null;
  minimumPenaltyPoints?: number | string | null;
  notes: string[];
}
```

### 11.2 TypeScript Types Cần Có Cho Series

```typescript
export type SeriesPresetCode = 'SERIES_MILESTONE_BASIC' | 'ENTERPRISE_SERIES' | 'CUSTOM';

export interface SeriesPresetConfig {
  primaryScoreType?: ScoreType | null;
  milestonePoints?: Record<number, number> | null;
  minimumRequirementEnabled?: boolean | null;
  minimumRequiredEvents?: number | null;
  minimumPenaltyPoints?: number | string | null;
}

export interface SeriesPresetDefinitionResponse {
  code: SeriesPresetCode;
  displayName: string;
  description: string;
  notes: string[];
  supportedRules: PresetRuleDescriptor[];  // reuse cùng type với Activity
}

export interface CreateSeriesRequest {
  name: string;
  description?: string | null;
  presetCode?: SeriesPresetCode | null;
  presetConfig?: SeriesPresetConfig | null;
  // ... các trường khác
  targetSemesterId?: number | null;
  registrationStartDate?: string | null;
  registrationDeadline?: string | null;
  requiresApproval?: boolean | null;
  ticketQuantity?: number | null;
  mainActivityId?: number | null;
}

export interface UpdateSeriesRequest {
  name?: string | null;
  description?: string | null;
  presetCode?: SeriesPresetCode | null;
  presetConfig?: SeriesPresetConfig | null;
  // ... các trường khác (type không extends CreateSeriesRequest)
  targetSemesterId?: number | null;
  registrationStartDate?: string | null;
  registrationDeadline?: string | null;
  requiresApproval?: boolean | null;
  ticketQuantity?: number | null;
  mainActivityId?: number | null;
}
```

### 11.3 Map Rule Key → SeriesPresetConfig Fields

| Rule Key | `SeriesPresetConfig` Fields Liên Quan | Input Type | Mô Tả |
|----------|----------------------------------------|------------|-------|
| `MILESTONE_POINTS` | `primaryScoreType`, `milestonePoints` | SELECT, MAP | Loại điểm + cặp key-value `(số buổi: điểm)` |
| `MINIMUM_REQUIREMENT` | `minimumRequirementEnabled`, `minimumRequiredEvents`, `minimumPenaltyPoints` | BOOLEAN, NUMBER, NUMBER | Bật/tắt yêu cầu tối thiểu + số buổi tối thiểu + điểm phạt |

**Lưu ý `MAP` input type:**
- `milestonePoints` là `Record<number, number>` (số buổi → điểm).
- FE cần component riêng để nhập cặp key-value dạng bảng: cột "Số buổi đã hoàn thành" (number), cột "Điểm thưởng" (number).
- Cho phép thêm/xóa dòng. Ví dụ mặc định SERIES_MILESTONE_BASIC: `{3:5, 5:10, 7:15}`.
- Validate: key phải là số nguyên dương tăng dần, value phải >= 0.

### 11.4 UI/UX Cho Series Form

1. **Preset Selector** — Dropdown chọn `SERIES_MILESTONE_BASIC`, `ENTERPRISE_SERIES`, `CUSTOM`.
2. **Dynamic Panel** — Render từ `supportedRules`:
   - `MILESTONE_POINTS`: Card hiển thị SELECT `primaryScoreType` + bảng nhập `milestonePoints`.
   - `MINIMUM_REQUIREMENT`: Toggle (mặc định tắt). Khi bật → hiện `minimumRequiredEvents` + `minimumPenaltyPoints`.
3. **Target Semester** — Dropdown chọn học kỳ đích (`targetSemesterId`). Nếu để null, backend tự tính từ thời gian sự kiện đầu tiên của chuỗi.
4. **Preview Button** — Gọi `POST /api/series/presets/preview` để xem trước `milestonePoints` đã merge với default.

### 11.5 Form Submission Logic (Create / Update Series)

```typescript
function buildSeriesPayload(formState: SeriesFormState): CreateSeriesRequest {
  const base = { /* ... các trường cơ bản ... */ };

  if (formState.presetCode && formState.presetCode !== 'CUSTOM') {
    return {
      ...base,
      presetCode: formState.presetCode,
      presetConfig: formState.presetConfig,
      // KHÔNG gửi milestonePoints trực tiếp nếu preset sinh ra
      // (backend sẽ merge config từ preset + default)
    };
  } else {
    return {
      ...base,
      presetCode: 'CUSTOM',
      presetConfig: formState.presetConfig, // hoặc gửi trực tiếp các trường cấu hình
    };
  }
}
```

### 11.6 Series Child Activity (Hoạt động con trong chuỗi)

- **Quan trọng:** Hoạt động con thuộc chuỗi **không cộng điểm riêng lẻ**. Điểm chỉ được cộng theo mốc hoàn thành chuỗi (Series Milestone) và bị phạt nếu không đạt số hoạt động tối thiểu.
- **FE cần:**
  - Ẩn phần hiển thị điểm riêng của hoạt động con nếu nó thuộc Series.
  - Màn hình Tiến độ chuỗi (Series Progress) hiển thị: tiến độ mốc, số sự kiện tối thiểu, trạng thái đạt/chưa đạt (`minimumRequirementMet`), và số sự kiện còn thiếu để tránh bị phạt (`remainingToAvoidPenalty`).
  - Series Child Activity không có `presetCode` / `scoreRules` riêng. Chỉ Series cha mới có.

---

## 12. AppliedScoreAward — Xử Lý Kết Quả Sau QR Check-in

> **Scope:** FE xử lý phần thưởng điểm chi tiết (`AppliedScoreAward`) trả về từ `POST /api/registrations/checkin/qr` để hiển thị cho sinh viên.
> **Backend Source:** `Checkin_QR_ScoreRule_Submission_Window_Plan.md`, `ActivityParticipationResponse`, `AppliedScoreAward`.

### 12.1 API Endpoint

- **Method:** `POST`
- **Path:** `/api/registrations/checkin/qr`
- **Body:** `{ "checkInCode": "string" }` (hoặc tương tự — tuỳ contract QR payload)
- **Response:** `ApiResponse<ActivityParticipationResponse>`

```json
{
  "status": true,
  "message": "success",
  "body": {
    "id": 123,
    "registrationId": 456,
    "activityId": 10,
    "activityName": "Hội thao Sinh viên CampusLife 2026",
    "studentId": 3,
    "studentName": "Nguyễn Văn A",
    "participationType": "ATTENDED",
    "pointsEarned": 6,
    "scoreAwards": [
      {
        "ruleId": 101,
        "scoreType": "REN_LUYEN",
        "scoreTypeLabel": "Điểm rèn luyện",
        "points": 5,
        "displayUnit": "điểm",
        "displayText": "+5 điểm rèn luyện",
        "triggerType": "PARTICIPATION_COMPLETED",
        "scoreEntryId": 1001
      },
      {
        "ruleId": 102,
        "scoreType": "CHUYEN_DE",
        "scoreTypeLabel": "Buổi chuyên đề",
        "points": 1,
        "displayUnit": "buổi",
        "displayText": "+1 buổi chuyên đề",
        "triggerType": "PARTICIPATION_COMPLETED",
        "scoreEntryId": 1002
      }
    ],
    "date": "2026-06-10T09:30:00",
    "isCompleted": true,
    "notes": null
  }
}
```

### 12.2 TypeScript Type

```typescript
export interface AppliedScoreAward {
  ruleId?: number | null;
  scoreType: ScoreType;           // 'REN_LUYEN' | 'CONG_TAC_XA_HOI' | 'CHUYEN_DE'
  scoreTypeLabel: string;          // 'Điểm rèn luyện', 'Buổi chuyên đề', ...
  points: number | string;         // BigDecimal
  displayUnit: string;             // 'điểm', 'buổi', ...
  displayText: string;             // '+5 điểm rèn luyện', '+1 buổi chuyên đề', ...
  triggerType?: ScoreRuleTrigger | null; // 'PARTICIPATION_COMPLETED' | 'SUBMISSION_GRADED' | ...
  scoreEntryId?: number | null;
}

export interface ActivityParticipationResponse {
  id: number;
  registrationId: number;
  activityId: number;
  activityName: string;
  studentId: number;
  studentName: string;
  participationType: ParticipationType;  // 'ATTENDED' | 'COMPLETED' | ...
  pointsEarned: number | string;          // Tổng điểm cộng gộp (backward compatible)
  scoreAwards: AppliedScoreAward[];       // Danh sách điểm chi tiết hiển thị UI
  date: string;
  isCompleted?: boolean | null;
  notes?: string | null;
}
```

### 12.3 UI/UX Hiển Thị Award

**Màn hình quét QR thành công (Success Screen):**

```
┌────────────────────────────────────────┐
│  ✅ Điểm danh thành công!              │
│                                        │
│  Hội thao Sinh viên CampusLife 2026    │
│                                        │
│  ┌─────────────────────────────────┐  │
│  │  +5 điểm rèn luyện             │  │  ← scoreAwards[0].displayText
│  │  +1 buổi chuyên đề             │  │  ← scoreAwards[1].displayText
│  └─────────────────────────────────┘  │
│                                        │
│  [Tiếp tục]                            │
└────────────────────────────────────────┘
```

**Quy tắc hiển thị:**
1. **Ưu tiên dùng `scoreAwards`** — Không dùng `pointsEarned` làm giá trị chính để hiển thị thành tích.
   - `pointsEarned` là tổng cộng gộp và không thể diễn tả đúng "+5 điểm rèn luyện + 1 buổi chuyên đề" (2 loại điểm khác nhau).
   - Giữ `pointsEarned` cho báo cáo tổng hợp / lịch sử cũ nếu cần.
2. **Render từng dòng** — Duyệt `scoreAwards` array, mỗi item render một dòng với `displayText`.
3. **Icon / màu sắc theo `scoreType`:**
   - `REN_LUYEN` → icon xanh lá, label "Điểm rèn luyện"
   - `CHUYEN_DE` → icon xanh dương, label "Buổi chuyên đề"
   - `CONG_TAC_XA_HOI` → icon cam, label "Điểm công tác xã hội"
4. **Xử lý trường hợp đặc biệt:**
   - `scoreAwards` rỗng / null → Hiển thị "Điểm danh thành công. Không có điểm thưởng cho hoạt động này."
   - `pointsEarned` = 0 và `scoreAwards` rỗng → Không hiển thị phần thưởng (hoặc chỉ hiển thị điểm danh thành công).
   - `scoreAwards` có 1 item → Hiển thị 1 dòng.
   - `scoreAwards` có nhiều item → Hiển thị danh sách, có thể nhóm theo `scoreType` nếu cần.

### 12.4 Score Award Component

```typescript
// React / Vue pseudocode
interface ScoreAwardListProps {
  awards: AppliedScoreAward[];
  emptyText?: string;
}

function ScoreAwardList({ awards, emptyText = "Không có điểm thưởng" }: ScoreAwardListProps) {
  if (!awards || awards.length === 0) {
    return <div className="text-muted">{emptyText}</div>;
  }

  return (
    <div className="score-award-list">
      {awards.map((award, index) => (
        <div key={index} className={`award-item award-${award.scoreType.toLowerCase()}`}>
          <span className="award-icon">{getScoreTypeIcon(award.scoreType)}</span>
          <span className="award-text">{award.displayText}</span>
          {award.triggerType && (
            <span className="award-trigger-tag">{award.triggerType}</span>
          )}
        </div>
      ))}
    </div>
  );
}
```

### 12.5 Các Trường Hợp Nghiệp Vụ Cần Xử Lý

| Trường hợp | `scoreAwards` | `pointsEarned` | FE Hiển Thị |
|------------|---------------|----------------|-------------|
| Sự kiện thường, có rule cộng điểm | `[{REN_LUYEN: 5}]` | `5` | "+5 điểm rèn luyện" |
| Chuyên đề + bonus | `[{CHUYEN_DE: 1}, {REN_LUYEN: 2}]` | `3` | "+1 buổi chuyên đề", "+2 điểm rèn luyện" |
| Không có rule cộng điểm | `[]` | `0` | "Điểm danh thành công. Không có điểm thưởng." |
| No-show penalty (không phải QR) | `[{REN_LUYEN: -5}]` | `-5` | "-5 điểm rèn luyện" (màu đỏ) |
| Submission-required, chưa chấm bài | `[]` | `0` | "Điểm danh thành công. Điểm thưởng sẽ được cộng sau khi bài nộp được chấm." |
| Series child activity | `[]` | `0` | "Điểm danh thành công. Điểm chuỗi sẽ được cộng khi đạt mốc." |

**Lưu ý về submission-required activities:**
- Nếu `requiresSubmission = true`, QR check-in chỉ lên `ATTENDED`. 
- Score awards chỉ được cộng khi **cả hai** điều kiện đạt: điểm danh + bài nộp được chấm.
- FE nên hiển thị message thông báo cho sinh viên: "Điểm danh thành công. Hãy nộp bài trước deadline để được cộng điểm."

### 12.6 Không Dùng `pointsEarned` Làm Primary Display

```typescript
// ❌ SAI — dùng pointsEarned hiển thị chính
<div>Điểm thưởng: +{response.body.pointsEarned}</div>

// ✅ ĐÚNG — duyệt scoreAwards
<div>
  {response.body.scoreAwards.map(award => (
    <div key={award.ruleId}>{award.displayText}</div>
  ))}
</div>
```

- `pointsEarned` có thể bị lỗi logic cũ (ví dụ vẫn = 0 do `markParticipationCompleted` set về 0).
- `scoreAwards` là nguồn sự thật (source of truth) cho hiển thị thành tích.

### 12.7 Re-scan QR (Duplicate Check-in)

- Backend đảm bảo không cộng điểm 2 lần khi sinh viên quét lại.
- `scoreAwards` có thể trả về rỗng hoặc trả về cùng list nhưng backend đã upsert (không duplicate).
- FE hiển thị: "Bạn đã điểm danh cho hoạt động này rồi." + có thể show lại awards đã cộng trước đó (nếu backend trả về).

### 12.8 Tích Hợp Với Các Flow Khác

| Flow | Endpoint | Sử Dụng `scoreAwards`? |
|------|----------|------------------------|
| QR Check-in (student tự quét) | `POST /api/registrations/checkin/qr` | ✅ Có — hiển thị awards ngay |
| Organizer check-in (quét ticket) | `POST /api/registrations/checkin` | ✅ Có — nếu response trả về participation |
| Submission graded | `POST /api/task-submissions/{id}/grade` | ✅ Có — khi finalization hoàn tất |
| View score history | `GET /api/scores/history/student/{studentId}` | Dùng `ScoreHistoryDetailResponse` |
| Series milestone reached | `POST /api/series/{seriesId}/students/{studentId}/calculate-milestone` | Dùng `SeriesProgress` |

---

*Plan version: 1.1*
*Written for: Frontend Team tích hợp CampusLife Score Preset Engine*
*Based on: Backend commit `c848ee6` trở đi — `ScorePresetServiceImpl`, `PresetRuleDescriptor`, `FieldDefinition`, `Checkin_QR_ScoreRule_Submission_Window_Plan`*
