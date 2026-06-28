# FE Spec v6.0 — Score Preset Adjustments + Registration Cancel Policies

> **Focus:** Tất cả thay đổi BE mới từ P6 (Preset) và P7 (Registration)  
> **Dành cho:** Frontend TypeScript team  
> **Ngày:** 2026-06-29

---

## 1. PresetRuleDescriptor — Field mới

### `conflictsWith: string[]`

Mảng `ruleKey` của các rule **xung khắc** (mutual exclusion). Khi FE toggle 1 rule **ON**, phải **tự tắt** tất cả rule trong `conflictsWith`.

```ts
export interface PresetRuleDescriptor {
  ruleKey: string;
  label: string;
  description: string;
  required: boolean;
  enabledByDefault: boolean;
  fieldDefinitions: FieldDefinition[];
  suggestedCombinations?: ScoreRuleTrigger[];
  conflictsWith?: string[];  // <-- MỚI
}
```

### Response thực tế cho Enterprise Seminar

```json
GET /api/activities/presets
// ENTERPRISE_SEMINAR_BASIC.supportedRules:

[
  {
    "ruleKey": "PARTICIPATION_COMPLETED",
    "enabledByDefault": true,
    "suggestedCombinations": ["NO_SHOW"],
    "conflictsWith": ["SUBMISSION_GRADED"]     // <-- khi bật rule này → tắt SUBMISSION_GRADED
  },
  {
    "ruleKey": "SUBMISSION_GRADED",
    "enabledByDefault": false,
    "suggestedCombinations": ["TASK_OVERDUE", "NO_SHOW"],
    "conflictsWith": ["PARTICIPATION_COMPLETED"] // <-- khi bật rule này → tắt PARTICIPATION_COMPLETED
  },
  {
    "ruleKey": "TASK_OVERDUE",
    "enabledByDefault": false,
    "suggestedCombinations": ["SUBMISSION_GRADED", "NO_SHOW"]
  },
  {
    "ruleKey": "NO_SHOW",
    "enabledByDefault": false
  }
]
```

### FE Logic

```ts
function toggleRule(ruleKey: string, enabled: boolean, supportedRules: PresetRuleDescriptor[]) {
  if (!enabled) return; // tắt rule thì không cần xử lý conflict

  const rule = supportedRules.find(r => r.ruleKey === ruleKey);
  if (!rule?.conflictsWith?.length) return;

  // Tự tắt các rule conflict
  const conflicts = rule.conflictsWith;
  return supportedRules.map(r => ({
    ...r,
    enabledByDefault: conflicts.includes(r.ruleKey) ? false : r.enabledByDefault
  }));
}
```

---

## 2. ActivityPresetConfig — Field mới

### `submissionEnabled: boolean`

Dùng cho Enterprise Seminar để toggle mode:
- `false` (default) → Participation mode: sinh `PARTICIPATION_COMPLETED`
- `true` → Submission mode: sinh `SUBMISSION_GRADED`, **không** sinh `PARTICIPATION_COMPLETED`

```ts
export interface ActivityPresetConfig {
  // ... existing fields ...
  submissionEnabled?: boolean | null;  // <-- MỚI
}
```

### Cách dùng

```ts
// Khi admin bật SUBMISSION_GRADED cho Enterprise Seminar:
const presetConfig: ActivityPresetConfig = {
  submissionEnabled: true,
  submissionPassPoints: 5,
  submissionFailPoints: 1,
  taskOverduePenaltyPoints: 2,  // optional
  noShowPenaltyEnabled: false
};

// Gọi preview để xem kết quả
POST /api/activities/presets/preview
{
  "presetCode": "ENTERPRISE_SEMINAR_BASIC",
  "type": "CHUYEN_DE_DOANH_NGHIEP",
  "presetConfig": { "submissionEnabled": true }
}

// Response: scoreRules chỉ có SUBMISSION_GRADED (+ TASK_OVERDUE nếu penalty != 0)
//           requiresSubmission = true
//           KHÔNG có PARTICIPATION_COMPLETED
```

---

## 3. ACTIVITY_AUDIENCE — Đã xoá

`GET /api/activities/presets` **không còn** trả về ruleKey `"ACTIVITY_AUDIENCE"` ở bất kỳ preset nào (kể cả `CUSTOM`).

Audience giờ được cấu hình **per-rule** trong từng descriptor, qua các field `*Audience`, `*DepartmentIds`, `*SemesterPolicy`, `*ExplicitSemesterId`.

### FE action
- Xoá UI section render từ `ruleKey === "ACTIVITY_AUDIENCE"`
- Per-rule audience fields đã có sẵn trong `fieldDefinitions` của từng rule descriptor (prefix `submission`, `participation`, `noShow`, `taskOverdue`, `bonus`, `minigamePassed`, `minigameExhausted`)

---

## 4. participationFailPoints — Giờ là optional

Trong `PARTICIPATION_COMPLETED` descriptor:
```json
{
  "fieldName": "participationFailPoints",
  "required": false   // <-- trước là true, giờ false
}
```

Áp dụng cho: `EVENT_BASIC`, `ENTERPRISE_SEMINAR_BASIC`, `ENTERPRISE_SEMINAR_WITH_BONUS`, `CUSTOM`.

### FE action
Dynamic form dựa trên `FieldDefinition.required` — không bắt buộc input cho field này nữa.

---

## 5. `submissionFailPoints` vẫn required

Trong `SUBMISSION_GRADED` descriptor:
```json
{
  "fieldName": "submissionFailPoints",
  "required": true    // <-- vẫn required
}
```

Khi admin bật `SUBMISSION_GRADED` (kể cả trong Enterprise), field này vẫn bắt buộc.

---

## 6. Lock presetCode khi edit activity

`PUT /api/activities/standard/{id}` từ chối đổi `presetCode`.

### Lỗi
```json
{
  "status": false,
  "message": "Cannot change preset code from ENTERPRISE_SEMINAR_BASIC to EVENT_BASIC on update. You can only customize score rules within the current preset.",
  "body": null
}
```

### FE action
- **Form edit**: disable/tắt dropdown `presetCode`, chỉ cho phép sửa các field trong `presetConfig`.
- Nếu user đang ở preset `ENTERPRISE_SEMINAR_BASIC` → dropdown bị lock, user chỉ có thể toggle `submissionEnabled`, chỉnh `participationPoints`, v.v.

---

## 7. Enterprise Seminar — Thêm SUBMISSION_GRADED + TASK_OVERDUE

`ENTERPRISE_SEMINAR_BASIC` và `ENTERPRISE_SEMINAR_WITH_BONUS` giờ có **4 rule** (BASIC) hoặc **5 rule** (WITH_BONUS):

| Rule | enabledByDefault | required |
|---|---|---|
| PARTICIPATION_COMPLETED | true | true |
| SUBMISSION_GRADED | **false** | **false** |
| TASK_OVERDUE | **false** | **false** |
| NO_SHOW | false | false |
| BONUS_POINTS (WITH_BONUS) | true | false |

### FE action
- Form enterprise seminar: hiển thị toggle cho `SUBMISSION_GRADED` và `TASK_OVERDUE`
- Khi toggle `SUBMISSION_GRADED` ON → dùng `conflictsWith` để tắt `PARTICIPATION_COMPLETED`
- Gửi `presetConfig.submissionEnabled = true` để BE biết đang ở submission mode
- `submissionFailPoints` field vẫn `required: true` — bắt buộc nhập khi bật rule

---

## 8. Cancel Policy mới (Activity)

### Luồng huỷ đăng ký activity

```
DELETE /api/registrations/activity/{activityId}
```

| Tình huống | Được huỷ? | Message |
|---|---|---|
| `APPROVED` + `requiresApproval=true` | **NO** | `"Cannot cancel approved registration. Admin has approved this registration."` |
| `APPROVED` + `requiresApproval=false` + đã huỷ 1 lần | **NO** | `"Bạn đã huỷ 1 lần trước đó, không thể huỷ lại."` |
| `APPROVED` + `requiresApproval=false` + sau deadline-1day | **NO** | `"Chỉ được huỷ trước hạn đăng ký 1 ngày."` |
| `APPROVED` + `requiresApproval=false` + trước deadline-1day + chưa huỷ | **YES** | `"Registration cancelled successfully"` |
| `PENDING` | **YES** | `"Registration cancelled successfully"` |
| `WAITLIST` | **YES** | `"Registration cancelled successfully"` |
| `CANCELLED` | **NO** | `"Registration already cancelled"` |

### FE hiển thị canCancel

`GET /api/registrations/activity/{activityId}/status` trả về:

```json
{
  "status": true,
  "message": "...",
  "body": {
    "registrationId": 123,
    "status": "APPROVED",
    "registeredDate": "2026-06-25T10:00:00",
    "canCancel": true   // <-- BE đã tính sẵn
  }
}
```

Chỉ hiển thị nút **"Huỷ đăng ký"** khi `canCancel === true`.

---

## 9. Chặn đăng ký lại sau khi huỷ

`POST /api/registrations/activity` — lỗi mới:

```json
{
  "status": false,
  "message": "Bạn đã huỷ đăng ký trước đó, không thể đăng ký lại.",
  "body": null
}
```

### FE action
- Nếu student đã từng huỷ → ẩn nút "Đăng ký", hiển thị text "Bạn đã huỷ đăng ký sự kiện này".
- Lưu ý: `existsByActivityIdAndStudentId` giờ exclude `CANCELLED` status → SV đã huỷ sẽ KHÔNG bị chặn bởi "Already registered" check, nhưng sẽ bị chặn bởi re-register block ở bước sau.

---

## 10. Huỷ đăng ký Series

```
DELETE /api/series/{seriesId}/register
```

### Điều kiện huỷ
- Series **không** `isImportant`
- Series **không** `mandatoryForFacultyStudents`
- **Chưa** có activity con nào `ATTENDED`

### Các lỗi

| Điều kiện | Message |
|---|---|
| `isImportant=true` | `"Không thể huỷ đăng ký chuỗi sự kiện quan trọng."` |
| `mandatoryForFacultyStudents=true` | `"Không thể huỷ đăng ký chuỗi bắt buộc cho sinh viên khoa."` |
| Có activity ATTENDED | `"Không thể huỷ vì bạn đã tham gia sự kiện 'Tên sự kiện'."` |
| Chưa đăng ký | `"Bạn chưa đăng ký chuỗi sự kiện này."` |

### FE action
- Nút "Huỷ đăng ký series" ở màn hình series detail
- Confirm dialog: "Bạn có chắc muốn huỷ? Tất cả đăng ký sự kiện con cũng sẽ bị huỷ."
- Khi huỷ thành công → BE tự huỷ tất cả activity con (trừ ATTENDED) + trigger waitlist promote

---

## 11. Đăng ký chờ Series

```
POST /api/series/{seriesId}/waitlist
```

### Điều kiện
- Series **đã full** (APPROVED count >= ticketQuantity)
- Registration deadline chưa qua
- Chưa có registration nào trong series

### Response

```json
{
  "status": true,
  "message": "Successfully joined series waitlist",
  "body": [ /* RegistrationResponse[] cho từng activity con */ ]
}
```

### Lỗi

| Điều kiện | Message |
|---|---|
| Còn slot | `"Series still has slots. Please register normally."` |
| Không giới hạn vé | `"Series has unlimited slots. Please register normally."` |
| Đã đăng ký/waitlist | `"Already registered or in waitlist for this series"` |
| Quá hạn | `"Registration deadline has passed"` |

### FE action
- Khi `approvedCount >= ticketQuantity` → đổi nút "Đăng ký" thành "Đăng ký chờ"
- Gọi `POST /api/series/{seriesId}/waitlist`

---

## 12. Waitlist Auto-Promote (FIFO)

Khi có slot trống (do ai đó huỷ), BE tự động:
1. Lấy WAITLIST đầu tiên (theo `registeredDate`)
2. Nếu `requiresApproval=false` → set `APPROVED`, gửi notification
3. Nếu `requiresApproval=true` → set `PENDING`, admin phải duyệt
4. Loop đến khi hết slot hoặc hết waitlist

### FE action
- **Không cần code gì thêm.** Student sẽ nhận notification `"Đăng ký từ danh sách chờ"` khi được promote.
- Nếu muốn hiển thị vị trí trong waitlist, FE có thể gọi `GET /api/registrations/activity/{id}` và sắp xếp theo `registeredDate`.

---

## 13. Series Quantity Check — APPROVED only

Cũ: đếm tất cả registration (mọi status) → PENDING cũng chiếm slot.  
Mới: chỉ đếm `APPROVED` distinct student — **đồng bộ với activity**.

### Hệ quả
- Series `requiresApproval=true`: PENDING registrations không chiếm slot → admin có thể duyệt quá số lượng, BE sẽ chặn lúc duyệt (`"Series is full. Cannot approve more registrations."` — tương tự activity)
- SV bị REJECTED/CANCELLED không còn chiếm slot ảo

---

## 14. Tổng kết FE Checklist

### Preset (P6)

- [ ] Xoá UI section `ACTIVITY_AUDIENCE`
- [ ] `participationFailPoints` render với `required: false`
- [ ] Enterprise Seminar: thêm toggle `SUBMISSION_GRADED` + `TASK_OVERDUE`
- [ ] Dùng `conflictsWith` để tự tắt rule xung khắc khi toggle
- [ ] Gửi `presetConfig.submissionEnabled` khi toggle submission mode
- [ ] Form edit: lock `presetCode` dropdown
- [ ] Hiển thị `submissionFailPoints` required khi `SUBMISSION_GRADED` được bật

### Registration (P7)

- [ ] Nút "Huỷ đăng ký" hiển thị theo `canCancel` flag mới
- [ ] Hiển thị thông báo phù hợp cho từng trường hợp bị chặn huỷ
- [ ] Ẩn nút đăng ký nếu đã từng huỷ (re-register block)
- [ ] Series: thêm nút "Huỷ đăng ký series" với confirm dialog
- [ ] Series: đổi nút "Đăng ký" → "Đăng ký chờ" khi full
- [ ] Series: dùng APPROVED count để hiển thị slot còn lại

### New Endpoints

- [ ] `DELETE /api/series/{seriesId}/register` — huỷ series
- [ ] `POST /api/series/{seriesId}/waitlist` — đăng ký chờ series

---

## 15. Chống Double Penalty: NO_SHOW + TASK_OVERDUE (P7.1)

### Vấn đề cũ

Task assignment được tạo cho **tất cả** SV đã đăng ký (không filter theo attendance). SV không tham gia (no-show) vẫn có TaskAssignment → khi deadline qua, `TASK_OVERDUE` vẫn bị apply → SV bị phạt cả `NO_SHOW` lẫn `TASK_OVERDUE` (double penalty).

### Fix (BE-only, FE không cần thay đổi)

BE thêm attendance guard tại 3 vị trí:

| Vị trí | Guard |
|---|---|
| `ScoreRuleEngineImpl.applyTaskOverdue` | `hasAttended()` — nếu registration != ATTENDED → skip, log |
| `ReminderDispatchService.isTaskOverdueReminderCancelled` | Nếu student không ATTENDED → cancel reminder |
| `ActivityTaskServiceImpl.checkAndUpdateOverdueAssignments` | Skip assignment nếu student không ATTENDED |

### Behavior mới

| Kịch bản | NO_SHOW | TASK_OVERDUE |
|---|---|---|
| Không tham gia, không nộp bài | Có (nếu bật) | **Không** |
| Có tham gia, không nộp bài | Không | Có (nếu bật) |
| Có tham gia, có nộp bài | Không | Không |
| Không tham gia, không có task | Có (nếu bật) | Không |

### FE action

Không cần thay đổi gì. BE tự guard ở engine level.
