# Đánh Giá Plan + Phân Tích Chỉnh Sửa ScoreRule Sau Tạo Sự Kiện

> **Tóm tắt:** Plan tốt cho flow CREATE nhưng thiếu hoàn toàn phần EDIT. Quan trọng hơn, backend **không persist `presetCode`/`presetConfig`** trên entity `Activity`, khiến FE không thể reconstruct form edit. Cần bổ sung backend contract và UX flow cho edit.

---

## 1. Đánh Giá Tổng Quan Plan

### 1.1 Điểm Mạnh

| # | Điểm mạnh | Đánh giá |
|---|-----------|----------|
| 1 | **Bao phủ đủ 3 module** (Activity Preset, Series Preset, AppliedScoreAward) | Tốt — đầy đủ scope nghiệp vụ |
| 2 | **Types định nghĩa chi tiết** | Tốt — FE có thể copy-paste |
| 3 | **Dynamic rendering từ `supportedRules`** | Rất tốt — không hardcode theo preset |
| 4 | **3 trạng thái toggle + visibility logic** | Rõ ràng — dễ implement |
| 5 | **MAP input type cho milestone** | Đã nghĩ đến — component nhập cặp key-value |
| 6 | **AppliedScoreAward UX** | Tốt — ưu tiên `scoreAwards` thay vì `pointsEarned` |
| 7 | **Constraints rõ ràng** | Tốt — không gửi scoreRules khi dùng preset |

### 1.2 Điểm Yếu / GAP Nghiêm Trọng

| # | Gap | Mức độ | Lý do |
|---|-----|--------|-------|
| 1 | **Thiếu hoàn toàn flow EDIT** | 🔴 **P0 — Critical** | Plan chỉ tập trung CREATE. Không có phần nào về "mở form edit activity đã tạo bằng preset". |
| 2 | **Backend không persist `presetCode`/`presetConfig`** | 🔴 **P0 — Critical** | `Activity` entity không có trường `presetCode`. `ActivityResponse` không trả về. FE không biết activity được tạo bằng preset nào. |
| 3 | **Không đề cập `replaceRules` behavior** | 🟡 **P1 — High** | Backend xóa toàn bộ rule cũ và tạo mới. FE phải gửi đủ set rule. Không thể patch từng rule. |
| 4 | **Không giải thích "Switch preset → custom"** | 🟡 **P1 — High** | Backend cho phép (gửi `presetCode=CUSTOM` + `scoreRules`), nhưng plan không đề cập UX cho transition này. |
| 5 | **Không đề cập preset config trong edit** | 🟡 **P1 — High** | Nếu admin giữ preset nhưng đổi `participationPoints` từ 5→10, FE phải biết presetCode và presetConfig cũ. Hiện tại không thể. |
| 6 | **Không liên kết với `implementation_plan.md`** | 🟢 **P2 — Medium** | Plan backend mở rộng `audience`, `semesterPolicy` vào preset config — FE plan nên đề cập tính tương thích. |

---

## 2. Câu Trả Lời: Sau Khi Tạo Sự Kiện, Có Cho Phép Chỉnh Sửa ScoreRule Không?

**CÓ — nhưng với 3 ràng buộc quan trọng.**

### 2.1 Phân Tích Backend Update Flow

```
PUT /api/activities/standard/{id}
  ↓
StandardActivityServiceImpl.updateActivity()
  ├── scorePresetService.applyActivityPreset(request)
  │   ├── presetCode == null || CUSTOM → return early (không động vào rule)
  │   ├── presetCode != null && != CUSTOM
  │   │   ├── Nếu scoreRules != null → throw 400 (reject custom rule kèm preset)
  │   │   └── Regenerate scoreRules từ preset + presetConfig → set vào request
  │   └── End
  ├── mapper.applyUpdate(existing, request)
  ├── activityRepository.save(existing)
  └── if (request.getScoreRules() != null)
      └── activityScoreRuleService.replaceRules(saved.getId(), request.getScoreRules())
          └── [XÓA TOÀN BỘ rule cũ] → [TẠO MỚI rule từ request]
```

**Quan trọng:** `replaceRules` nghĩa là **xóa tất cả rule cũ** và **tạo lại từ đầu**. Không có partial update.

### 2.2 Ba Cách Chỉnh Sửa Sau Tạo

#### Cách 1: Giữ Preset, Chỉnh Sửa Qua `presetConfig`

| Điều kiện | Mô tả |
|------------|-------|
| FE gửi | `presetCode` (cùng hoặc khác preset) + `presetConfig` mới |
| Backend | Regenerate toàn bộ `scoreRules` từ preset mới → `replaceRules` |
| Ưu điểm | Admin không cần hiểu cấu trúc rule, chỉ cần điều chỉnh config |
| Nhược điểm | Không edit từng rule riêng lẻ; mất rule cũ |
| **Vấn đề hiện tại** | 🔴 **FE không biết `presetCode` gốc** vì backend không persist trên `Activity` entity và không trả về trong `ActivityResponse` |

**Ví dụ:**
```json
// Request update
{
  "presetCode": "EVENT_BASIC",
  "presetConfig": {
    "primaryScoreType": "REN_LUYEN",
    "participationPoints": 10,        // ← đổi từ 5 lên 10
    "participationFailPoints": 0,
    "noShowPenaltyEnabled": false     // ← tắt no-show
  }
  // KHÔNG gửi scoreRules
}
```

Backend sẽ:
1. Regenerate rule `PARTICIPATION_COMPLETED` với `points = 10`
2. Không sinh rule `NO_SHOW` (vì `noShowPenaltyEnabled = false`)
3. `replaceRules` → xóa rule cũ, tạo rule mới

---

#### Cách 2: Chuyển Sang CUSTOM, Chỉnh Sửa Thủ Công

| Điều kiện | Mô tả |
|------------|-------|
| FE gửi | `presetCode = "CUSTOM"` + `scoreRules` thủ công |
| Backend | `applyActivityPreset` return early (vì CUSTOM) → `replaceRules` với rule thủ công |
| Ưu điểm | Edit từng rule riêng lẻ, full control |
| Nhược điểm | Mất preset convenience; phải hiểu rule structure |
| **Vấn đề hiện tại** | 🟡 Cần UX cảnh báo: "Chuyển sang custom sẽ xóa preset config và không thể quay lại preset mode" |

**Ví dụ:**
```json
// Request update
{
  "presetCode": "CUSTOM",
  "scoreRules": [
    {
      "scoreType": "REN_LUYEN",
      "triggerType": "PARTICIPATION_COMPLETED",
      "calculation": "FIXED_POINTS",
      "points": 10,
      "failPoints": 0,
      "audience": "ALL_PARTICIPANTS",
      "semesterPolicy": "ACTIVITY_SEMESTER",
      "enabled": true
    }
  ]
}
```

---

#### Cách 3: Gửi `presetCode = null` (Giữ Nguyên Rule Cũ)

| Điều kiện | Mô tả |
|------------|-------|
| FE gửi | `presetCode = null` + `scoreRules = null` |
| Backend | `applyActivityPreset` return early; `replaceRules` không được gọi (vì scoreRules null) |
| Kết quả | Rule cũ được giữ nguyên, không thay đổi |
| Ưu điểm | An toàn, không vô tình xóa rule |
| Nhược điểm | Không thể chỉnh sửa rule trong cùng request |

---

### 2.3 Kết Luận

| Hành động | Có thể? | Cách làm | Ràng buộc |
|-----------|---------|----------|-----------|
| **Đổi preset config** (VD: điểm từ 5→10) | ✅ Có | Gửi `presetCode` + `presetConfig` mới | FE phải biết `presetCode` gốc (hiện tại không có trong response) |
| **Bật/tắt optional rule** (VD: bật TASK_OVERDUE) | ✅ Có | Gửi `presetCode` + `presetConfig` với `taskOverduePenaltyPoints > 0` | Tương tự trên |
| **Edit từng rule riêng lẻ** | ✅ Có | Chuyển `presetCode = "CUSTOM"` + gửi `scoreRules` thủ công | Mất preset mode; phải gửi đủ set rule |
| **Patch 1 rule, giữ các rule khác** | ❌ Không | Backend dùng `replaceRules` — luôn xóa toàn bộ và tạo mới | FE phải gửi đủ set rule mới |
| **Giữ nguyên rule, chỉ sửa thông tin activity khác** | ✅ Có | Gửi `presetCode = null` + `scoreRules = null` | An toàn nhất |

---

## 3. Vấn Đề Nghiêm Trọng: Backend Không Persist `presetCode` / `presetConfig`

### 3.1 Bằng Chứng

**Entity `Activity`:**
```bash
# Grep trên entity folder — không có presetCode/presetConfig
src/main/java/vn/campuslife/entity/Activity.java
# → Không chứa presetCode hay presetConfig
```

**Response `ActivityResponse`:**
```typescript
export interface ActivityResponse {
  id: number;
  name: string;
  type: ActivityType;
  // ... nhiều trường ...
  scoreRules: ActivityScoreRuleResponse[];
  // ... nhưng KHÔNG CÓ presetCode hay presetConfig!
}
```

**Request `CreateActivityRequest` / `StandardActivityUpdateRequest`:**
```java
// CÓ presetCode và presetConfig
private ActivityPresetCode presetCode;
private ActivityPresetConfig presetConfig;
```

### 3.2 Hậu Quả

Khi admin mở form **edit** activity đã tạo bằng preset:

```
GET /api/activities/standard/{id}
  ↓
Response: ActivityResponse
  ├── scoreRules: [
  │   { id: 101, triggerType: "PARTICIPATION_COMPLETED", points: 5, isPresetGenerated: true },
  │   { id: 102, triggerType: "NO_SHOW", points: 5, isPresetGenerated: true }
  │ ]
  ├── type: "SUKIEN"
  ├── requiresSubmission: false
  └── ...
  └── KHÔNG CÓ presetCode → FE không biết đây là EVENT_BASIC
  └── KHÔNG CÓ presetConfig → FE không biết participationPoints = 5, noShowPenaltyEnabled = true
```

**FE chỉ thấy:**
- `scoreRules` với `isPresetGenerated = true` → biết đây là preset-generated
- Nhưng **không biết** preset nào, và **không biết** config values

→ FE không thể:
1. Hiển thị dropdown preset đã chọn
2. Pre-fill form với giá trị cũ (VD: participationPoints = 5)
3. Cho phép admin chỉnh sửa preset config mà không đoán mò

### 3.3 Khuyến Nghị Backend

Cần **một trong các thay đổi sau** (đề xuất cách 1):

**Cách 1: Persist trên Entity (Khuyến nghị)**
```java
// Activity.java
@Entity
public class Activity {
    // ... existing fields ...
    
    @Enumerated(EnumType.STRING)
    @Column(name = "preset_code", length = 50)
    private ActivityPresetCode presetCode;  // Nullable — null nghĩa là custom
    
    @Column(name = "preset_config", columnDefinition = "TEXT")
    private String presetConfigJson;  // JSON string của ActivityPresetConfig
}
```

**Cách 2: Derive từ `isPresetGenerated`** (Workaround, không khuyến nghị)
- Khi `isPresetGenerated = true` trên tất cả scoreRules, FE đoán là "preset mode"
- Nhưng vẫn không biết presetCode và presetConfig → không pre-fill form

**Cách 3: Trả về trong Response (ít tốn kém nhất)**
```typescript
// ActivityResponse.ts
export interface ActivityResponse {
  // ... existing fields ...
  presetCode?: ActivityPresetCode | null;     // ← THÊM
  presetConfig?: ActivityPresetConfig | null;  // ← THÊM
  scoreRules: ActivityScoreRuleResponse[];
}
```
- Backend đọc từ entity (nếu đã persist) hoặc derive từ rule (nếu chưa persist)

---

## 4. Khuyến Nghị Cập Nhật Plan

### 4.1 Thêm Phần "12. Edit Flow" (P0)

```markdown
## 12. Edit Flow — Chỉnh Sửa Activity Đã Tạo

### 12.1 Load Activity để Edit

Khi mở form edit:
1. Gọi `GET /api/activities/standard/{id}` (hoặc endpoint tương ứng)
2. Kiểm tra `presetCode` trong response:
   - Nếu `presetCode != null && != CUSTOM` → Mode "Preset"
   - Nếu `presetCode == null || CUSTOM` → Mode "Custom"

> ⚠️ **Hiện tại:** `ActivityResponse` chưa có `presetCode`. 
> Nếu backend chưa fix, FE có thể dùng heuristic:
> - Nếu TẤT CẢ `scoreRules` có `isPresetGenerated = true` → đoán là "Preset mode"
> - Nhưng không biết presetCode cụ thể → dropdown preset hiển thị "Preset (không xác định)"
> - Không thể pre-fill presetConfig → form preset config trống, admin phải chọn lại từ đầu

### 12.2 Mode "Preset" (Edit Preset Config)

- Hiển thị preset selector với `presetCode` đã chọn (nếu có)
- Render `PresetConfigPanel` với giá trị từ `presetConfig` (nếu backend trả về)
- Cho phép admin thay đổi `presetConfig` fields
- Gọi preview API để xem rule sẽ thay đổi
- Khi submit: gửi `presetCode` + `presetConfig` mới
- Backend sẽ `replaceRules` (xóa rule cũ, tạo rule mới từ preset)

**UX Warning:**
> "Thay đổi preset config sẽ xóa toàn bộ score rules hiện tại và tạo lại từ đầu. Các rule tùy chỉnh (nếu có) sẽ bị mất."

### 12.3 Mode "Custom" (Edit Manual ScoreRules)

- Hiển thị bảng/accordion danh sách `scoreRules` hiện tại
- Cho phép thêm/xóa/sửa từng rule
- Khi submit: gửi `presetCode = "CUSTOM"` + `scoreRules` đầy đủ
- Backend `replaceRules` với rule thủ công

### 12.4 Switch Preset → Custom

1. Admin click "Chuyển sang tùy chỉnh thủ công"
2. FE hiển thị **Confirm Dialog**:
   > "Chuyển sang chế độ tùy chỉnh sẽ xóa cấu hình preset. Bạn sẽ cần tự quản lý từng score rule. Bạn có chắc không?"
3. Nếu đồng ý:
   - FE chuyển sang mode "Custom"
   - Nếu có `scoreRules` hiện tại (từ preset), có thể giữ nguyên để admin edit từng rule
   - Hoặc xóa trắng và cho admin tạo rule mới
4. Khi submit: gửi `presetCode = "CUSTOM"` + `scoreRules` (đầy đủ)

### 12.5 Switch Custom → Preset

1. Admin click "Dùng preset thay thế"
2. FE hiển thị **Confirm Dialog**:
   > "Chuyển sang preset sẽ xóa toàn bộ rule tùy chỉnh hiện tại và thay bằng rule tự động từ preset. Bạn có chắc không?"
3. Admin chọn preset từ dropdown
4. Render `PresetConfigPanel` với default values
5. Khi submit: gửi `presetCode` + `presetConfig`

### 12.6 Giữ Nguyên Rule (Không Sửa)

- Nếu admin chỉ sửa thông tin activity khác (name, date, location...) mà không đụng đến score:
- FE gửi `presetCode = null` và `scoreRules = null`
- Backend giữ nguyên rule cũ
```

### 4.2 Thêm Lưu Ý `replaceRules` Vào Plan

Thêm vào phần "Constraints":
> **Backend dùng `replaceRules` (xóa toàn bộ, tạo mới)** — không phải patch. FE phải gửi đầy đủ set rule trong mọi request có `scoreRules != null`.

### 4.3 Cập Nhật `ActivityResponse` Type

```typescript
export interface ActivityResponse {
  // ... existing fields ...
  
  // ← THÊM: Cần backend trả về để hỗ trợ edit
  presetCode?: ActivityPresetCode | null;
  presetConfig?: ActivityPresetConfig | null;
  
  scoreRules: ActivityScoreRuleResponse[];
}
```

### 4.4 Liên Kết Với `implementation_plan.md`

Thêm vào plan:
> Khi backend thực hiện `implementation_plan.md` (thêm `audience`, `semesterPolicy`, `explicitSemesterId`, `departmentIds` vào `ActivityPresetConfig` và `supportedRules`), FE dynamic form sẽ **tự động render thêm các field mới** mà không cần sửa code, vì FE đọc `FieldDefinition` từ `supportedRules` trả về bởi API.
>
> Tuy nhiên, FE cần hỗ trợ input type `MULTI_SELECT` (cho `departmentIds`) và `SELECT` với conditional visibility (cho `explicitSemesterId` khi `semesterPolicy = EXPLICIT`).

---

## 5. Checklist Cập Nhật Plan (Giao Cho Agent)

| # | Task | Priority |
|---|------|----------|
| 1 | Thêm phần "12. Edit Flow" vào plan | P0 |
| 2 | Cập nhật `ActivityResponse` type thêm `presetCode` + `presetConfig` | P0 |
| 3 | Thêm lưu ý `replaceRules` (xóa toàn bộ, tạo mới) | P0 |
| 4 | Thêm UX flow Switch Preset ↔ Custom với confirm dialog | P1 |
| 5 | Thêm khuyến nghị backend persist `presetCode`/`presetConfig` | P0 |
| 6 | Thêm note về tương thích `implementation_plan.md` (audience, semesterPolicy) | P2 |
| 7 | Cập nhật prompt gửi agent để include edit flow | P1 |

---

*Đánh giá này dựa trên: `StandardActivityServiceImpl.updateActivity()`, `ScorePresetServiceImpl.applyActivityPreset()`, `StandardActivityUpdateRequest`, `ActivityResponse` type, và `implementation_plan.md`.*
