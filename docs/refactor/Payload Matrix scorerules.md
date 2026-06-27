### 4.3 Payload Matrix Cho Score Rules Hiện Tại

Phần này là contract FE nên dùng để render bảng lập `scoreRules`. FE có thể cho người dùng chọn preset để BE sinh rules, hoặc chuyển sang custom và gửi trực tiếp `scoreRules`.

#### 4.3.1 Quy ước điểm trừ và học kỳ ghi điểm

> [!IMPORTANT]
> FE **luôn nhập điểm phạt/fail dưới dạng số dương** trong `failPoints`. Không tự thêm dấu âm ở UI/request. Backend sẽ tự negate khi apply các nhánh failure/penalty.

- Các rule `NO_SHOW`, `TASK_OVERDUE`, `MINIGAME_EXHAUSTED_ATTEMPTS` bắt buộc có `failPoints`. Nếu FE gửi nhầm `points` mà bỏ `failPoints` cho 3 trigger này, backend hiện có fallback copy `points -> failPoints`, nhưng FE nên gửi đúng `failPoints`.
- `PENALTY_POINTS`: backend luôn lưu entry âm khi `failPoints > 0`.
- `PASS_FAIL_POINTS`: khi pass dùng `points` giữ nguyên; khi fail dùng `failPoints` và backend tự đổi sang âm nếu `failPoints > 0`.
- `FIXED_POINTS`, `COUNT_COMPLETION`, `SERIES_MILESTONE`: dùng cho nhánh cộng điểm, FE gửi `points` dương.
- `ActivityResponse.scoreRules` hiện chỉ trả các rule `enabled = true`; nếu UI muốn "tắt" một rule trong form edit, cách an toàn là bỏ row đó khỏi danh sách gửi lên hoặc dùng preset config tương ứng, không kỳ vọng row `enabled=false` sẽ round-trip trong detail.
- Chọn học kỳ/năm học:
  - Nếu ghi vào học kỳ theo thời gian sự kiện: `semesterPolicy: "ACTIVITY_SEMESTER"`, `explicitSemesterId: null`.
  - Nếu admin muốn ghi vào học kỳ/năm học cụ thể: `semesterPolicy: "EXPLICIT_SEMESTER"`, bắt buộc gửi `explicitSemesterId`.
  - FE nên lấy dropdown năm học/học kỳ bằng `GET /api/academic/years`, `GET /api/academic/years/{yearId}/semesters`, hoặc `GET /api/academic/semesters`.

#### 4.3.2 Base payload cho một rule

```json
{
  "scoreType": "REN_LUYEN",
  "triggerType": "PARTICIPATION_COMPLETED",
  "calculation": "FIXED_POINTS",
  "points": "5",
  "failPoints": "0",
  "audience": "ALL_PARTICIPANTS",
  "semesterPolicy": "ACTIVITY_SEMESTER",
  "explicitSemesterId": null,
  "departmentIds": [],
  "enabled": true
}
```

Validation hiện tại:

| Field | FE rule |
| --- | --- |
| `scoreType`, `triggerType`, `calculation`, `audience`, `semesterPolicy` | Bắt buộc |
| `explicitSemesterId` | Bắt buộc khi `semesterPolicy = EXPLICIT_SEMESTER` |
| `departmentIds` | Bắt buộc khi `audience = DEPARTMENT_ONLY` hoặc `OUTSIDE_DEPARTMENTS_ONLY` |
| `SUBMISSION_GRADED`, `TASK_OVERDUE` | Chỉ hợp lệ khi activity `requiresSubmission = true` |
| `MINIGAME_PASSED`, `MINIGAME_EXHAUSTED_ATTEMPTS` | Chỉ hợp lệ với activity `type = MINIGAME` |
| Activity trong series | Không cộng/trừ điểm rule riêng; engine skip individual scoring và chỉ update progress/milestone |

#### 4.3.3 Activity standalone: tất cả rule case đang hỗ trợ

**EVENT_BASIC / sự kiện thường không cần nộp bài**

```json
{
  "scoreType": "REN_LUYEN",
  "triggerType": "PARTICIPATION_COMPLETED",
  "calculation": "FIXED_POINTS",
  "points": "5",
  "failPoints": "0",
  "audience": "ALL_PARTICIPANTS",
  "semesterPolicy": "ACTIVITY_SEMESTER",
  "explicitSemesterId": null,
  "departmentIds": [],
  "enabled": true
}
```

No-show mặc định bật cho preset `EVENT_BASIC`:

```json
{
  "scoreType": "REN_LUYEN",
  "triggerType": "NO_SHOW",
  "calculation": "PENALTY_POINTS",
  "points": "0",
  "failPoints": "5",
  "audience": "ALL_PARTICIPANTS",
  "semesterPolicy": "ACTIVITY_SEMESTER",
  "explicitSemesterId": null,
  "departmentIds": [],
  "enabled": true
}
```

**EVENT_WITH_SUBMISSION / sự kiện có bài nộp**

```json
{
  "scoreType": "REN_LUYEN",
  "triggerType": "SUBMISSION_GRADED",
  "calculation": "PASS_FAIL_POINTS",
  "points": "5",
  "failPoints": "0",
  "audience": "ALL_PARTICIPANTS",
  "semesterPolicy": "ACTIVITY_SEMESTER",
  "explicitSemesterId": null,
  "departmentIds": [],
  "enabled": true
}
```

Overdue nếu admin cấu hình phạt quá hạn:

```json
{
  "scoreType": "REN_LUYEN",
  "triggerType": "TASK_OVERDUE",
  "calculation": "PENALTY_POINTS",
  "points": "0",
  "failPoints": "2",
  "audience": "ALL_PARTICIPANTS",
  "semesterPolicy": "ACTIVITY_SEMESTER",
  "explicitSemesterId": null,
  "departmentIds": [],
  "enabled": true
}
```

No-show mặc định bật cho preset `EVENT_WITH_SUBMISSION`:

```json
{
  "scoreType": "REN_LUYEN",
  "triggerType": "NO_SHOW",
  "calculation": "PENALTY_POINTS",
  "points": "0",
  "failPoints": "5",
  "audience": "ALL_PARTICIPANTS",
  "semesterPolicy": "ACTIVITY_SEMESTER",
  "explicitSemesterId": null,
  "departmentIds": [],
  "enabled": true
}
```

**ENTERPRISE_SEMINAR_BASIC / chuyên đề doanh nghiệp**

```json
{
  "scoreType": "CHUYEN_DE",
  "triggerType": "PARTICIPATION_COMPLETED",
  "calculation": "COUNT_COMPLETION",
  "points": "1",
  "failPoints": "0",
  "audience": "ALL_PARTICIPANTS",
  "semesterPolicy": "ACTIVITY_SEMESTER",
  "explicitSemesterId": null,
  "departmentIds": [],
  "enabled": true
}
```

No-show của seminar mặc định **tắt**. Nếu admin bật, nên trừ sang `REN_LUYEN`, không trừ ngược vào `CHUYEN_DE` của chính buổi chuyên đề:

```json
{
  "scoreType": "REN_LUYEN",
  "triggerType": "NO_SHOW",
  "calculation": "PENALTY_POINTS",
  "points": "0",
  "failPoints": "2",
  "audience": "ALL_PARTICIPANTS",
  "semesterPolicy": "ACTIVITY_SEMESTER",
  "explicitSemesterId": null,
  "departmentIds": [],
  "enabled": true
}
```

**ENTERPRISE_SEMINAR_WITH_BONUS**

Rule chính giống seminar basic, cộng thêm bonus rule:

```json
{
  "scoreType": "REN_LUYEN",
  "triggerType": "PARTICIPATION_COMPLETED",
  "calculation": "FIXED_POINTS",
  "points": "2",
  "failPoints": "0",
  "audience": "ALL_PARTICIPANTS",
  "semesterPolicy": "ACTIVITY_SEMESTER",
  "explicitSemesterId": null,
  "departmentIds": [],
  "enabled": true
}
```

**MINIGAME_PASS_ONLY**

```json
{
  "scoreType": "REN_LUYEN",
  "triggerType": "MINIGAME_PASSED",
  "calculation": "FIXED_POINTS",
  "points": "5",
  "failPoints": "0",
  "audience": "ALL_PARTICIPANTS",
  "semesterPolicy": "ACTIVITY_SEMESTER",
  "explicitSemesterId": null,
  "departmentIds": [],
  "enabled": true
}
```

Phạt khi hết lượt mà vẫn không pass, chỉ áp dụng cho minigame standalone:

```json
{
  "scoreType": "REN_LUYEN",
  "triggerType": "MINIGAME_EXHAUSTED_ATTEMPTS",
  "calculation": "PASS_FAIL_POINTS",
  "points": "0",
  "failPoints": "2",
  "audience": "ALL_PARTICIPANTS",
  "semesterPolicy": "ACTIVITY_SEMESTER",
  "explicitSemesterId": null,
  "departmentIds": [],
  "enabled": true
}
```

**CUSTOM**

FE render bảng custom bằng cách cho chọn các cột: `scoreType`, `triggerType`, `calculation`, `points`, `failPoints`, `audience`, `semesterPolicy`, `explicitSemesterId`, `departmentIds`, `enabled`. Backend vẫn enforce compatibility như bảng validation ở trên.

#### 4.3.4 Series: payload và scoring hiện tại

Series không dùng `ActivityScoreRuleRequest` cho milestone. FE gửi trực tiếp config series:

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
  "audience": "ALL_PARTICIPANTS",
  "departmentIds": [],
  "registrationStartDate": "2026-06-01T00:00:00",
  "registrationDeadline": "2026-06-30T23:59:59",
  "requiresApproval": true,
  "ticketQuantity": 200,
  "presetCode": "ENTERPRISE_SERIES",
  "presetConfig": null
}
```

**Tính năng mới: Series audience**

| audience | Hành vi |
|----------|---------|
| `ALL_PARTICIPANTS` (default) | Tất cả student có progress đều nhận milestone/penalty điểm |
| `DEPARTMENT_ONLY` | Chỉ student thuộc khoa trong `departmentIds` mới nhận điểm |
| `OUTSIDE_DEPARTMENTS_ONLY` | Chỉ student KHÔNG thuộc khoa trong `departmentIds` mới nhận điểm |

- Audience chỉ ảnh hưởng scoring (milestone + penalty), KHÔNG ảnh hưởng enrollment (student vẫn đăng ký được).
- FE gửi `departmentIds` khi `audience != ALL_PARTICIPANTS`.
- GET response có `audience` và `targetDepartmentIds`.

Preset default:

| Preset | `scoreType` thường dùng | `milestonePoints` default |
| --- | --- | --- |
| `SERIES_MILESTONE_BASIC` | `REN_LUYEN` | `{ "3": 5, "5": 10, "7": 15 }` |
| `ENTERPRISE_SERIES` | `CHUYEN_DE` | `{ "1": 1, "3": 3, "5": 5 }` |
| `CUSTOM` | FE tự gửi | `{}` |

Series engine hiện tại:

- Khi student đạt milestone, backend ghi `ScoreEntrySourceType = SERIES_PROGRESS`, `points = milestone cao nhất đã đạt`.
- Khi bật minimum requirement và student chưa đủ số buổi, backend ghi `ScoreEntrySourceType = SERIES_MINIMUM_REQUIREMENT`, `points = -minimumPenaltyPoints`.
- Activity con trong series không ghi điểm riêng cho `PARTICIPATION_COMPLETED`, `SUBMISSION_GRADED`, `TASK_OVERDUE`, `NO_SHOW`, `MINIGAME_PASSED`, `MINIGAME_EXHAUSTED_ATTEMPTS`.
- Học kỳ của series hiện resolve theo activity đầu tiên trong series; nếu không có activity thì fallback học kỳ đang mở/đầu tiên. Series request hiện **chưa có** `explicitSemesterId`, nên UI chọn học kỳ cụ thể chỉ áp dụng cho `scoreRules` của activity standalone.

#### 4.3.6 Preset Form: Dynamic Render từ supportedRules

FE gọi `GET /api/activities/presets` và `GET /api/series/presets` để lấy metadata `supportedRules`. Mỗi descriptor có `ruleKey` và danh sách `FieldDefinition`. FE render form động theo metadata này.

**Rule-key mới (P4):**

| ruleKey | Áp dụng | fieldName cần render |
|---------|---------|---------------------|
| `ACTIVITY_AUDIENCE` | Activity | `audience` (SELECT), `departmentIds` (MULTI_SELECT), `semesterPolicy` (SELECT), `explicitSemesterId` (SELECT) |
| `SERIES_AUDIENCE` | Series | `audience` (SELECT), `departmentIds` (MULTI_SELECT) |

**Visibility rules cho FE:**

| visibility | Điều kiện hiện |
|------------|---------------|
| `ALWAYS` | Luôn hiện |
| `rule_enabled` | Hiện khi rule descriptor được bật |
| `audience_department_scoped` | Hiện khi `audience != ALL_PARTICIPANTS` |
| `semester_policy_explicit` | Hiện khi `semesterPolicy == EXPLICIT_SEMESTER` |

**inputType mới:**
- `MULTI_SELECT` — render thành multi-select dropdown (VD: react-select) để chọn danh sách khoa. Gửi `presetConfig.departmentIds: [1, 2, 3]`.

**Flow render form Activity:**
1. User chọn preset → FE load `supportedRules` từ GET /presets
2. Với mỗi `PresetRuleDescriptor`, FE render 1 section tương ứng `ruleKey`
3. Mỗi section chứa các input theo `fieldDefinitions`, dùng `inputType`, `visibility`, `defaultValue` để render đúng
4. Khi user thay đổi giá trị → cập nhật `presetConfig` tương ứng (map `fieldName` → value)
5. Gửi `presetCode` + `presetConfig` khi create/update. BE sinh rules từ config.

| Case | Trigger/Source | Điểm FE gửi | Điểm BE ghi | Ghi chú UI |
| --- | --- | --- | --- | --- |
| Event thường attended/completed | `PARTICIPATION_COMPLETED` | `points: "5"` | `+5` | Hoàn thành khi `ATTENDED` |
| Event thường no-show | `NO_SHOW` | `failPoints: "5"` | `-5` | FE không gửi `-5` |
| Event có submission pass | `SUBMISSION_GRADED` | `points: "5"` | `+5` | Cần `ATTENDED + GRADED` |
| Event có submission fail | `SUBMISSION_GRADED` | `failPoints: "2"` | `-2` | Fail vẫn considered graded/completed nghiệp vụ |
| Event có submission overdue | `TASK_OVERDUE` | `failPoints: "2"` | `-2` | Chỉ khi `requiresSubmission=true`; dùng failPoints, không suy từ points |
| Seminar attended | `PARTICIPATION_COMPLETED` | `points: "1"` | `+1` | Thường là `CHUYEN_DE` |
| Seminar no-show tùy chọn | `NO_SHOW` | `failPoints: "2"` | `-2` | Nên chọn `REN_LUYEN` |
| Minigame pass | `MINIGAME_PASSED` | `points: "5"` | `+5` | Standalone only |
| Minigame hết lượt fail | `MINIGAME_EXHAUSTED_ATTEMPTS` | `failPoints: "2"` | `-2` | Standalone only |
| Series milestone | `SERIES_PROGRESS` | `milestonePoints` | `+highestMilestone` | Không phải activity score rule |
| Series thiếu minimum | `SERIES_MINIMUM_REQUIREMENT` | `minimumPenaltyPoints: 2` | `-2` | FE gửi số dương |

---
#### 5. CRUD coverage và edit behavior của Activity

| Nhu cầu FE/Admin | Endpoint hiện có | Coverage |
| --- | --- | --- |
| Tạo activity kèm preset/custom score rules | `POST /api/activities` | Đã có |
| Xem list/detail activity | `GET /api/activities`, `GET /api/activities/{id}` | Đã có |
| Sửa activity đã tạo | `PUT /api/activities/{id}` | Đã có |
| Xóa activity | `DELETE /api/activities/{id}` | Đã có soft delete |
| Publish/unpublish draft | `PUT /api/activities/{id}/publish`, `PUT /api/activities/{id}/unpublish` | Đã có |
| Copy activity | `POST /api/activities/{id}/copy?offsetDays=...` | Đã có; score rules copy sang activity mới với `ACTIVITY_SEMESTER` |

> [!IMPORTANT]
> **Edit activity behavior (updated):**
> - `PUT /api/activities/{id}` và `PUT /api/activities/standard/{id}`: gọi preset resolver nếu có `presetCode/presetConfig`, sau đó merge-by-key thay vì delete-all+recreate. Rule ID được giữ nguyên → score entries hiện có không bị gãy FK.
> - Nếu activity đã có ACTIVE score entries và không phải draft → **backend từ chối sửa score rules** (IllegalStateException). Admin phải unpublish trước khi sửa.
> - Nếu activity đã có ACTIVE score entries và không phải draft → **backend từ chối đổi type** (chỉ Standard path vì có field `type`).
> - Nếu gửi update KHÔNG có `scoreRules`, rules hiện tại được giữ nguyên (null guard fix).
> - Nếu FE muốn lưu bảng rule custom, gửi `presetCode: "CUSTOM"` hoặc không gửi `presetCode`, đồng thời gửi toàn bộ `scoreRules` mong muốn.
> - ActivityResponse giờ có `presetCode` để FE biết preset nào đã dùng.

Read-back để hiển thị lại form:

- `POST /api/activities`, `PUT /api/activities/{id}`, `POST /api/activities/{id}/copy`, `PUT /publish`, `PUT /unpublish` đều trả `ApiResponse<ActivityResponse>`.
- `ActivityResponse` hiện trả lại các field cần dựng form: `requiresSubmission`, `requiresApproval`, `mandatoryForFacultyStudents`, `requirements`, `benefits`, `contactInfo`, `registrationStartDate`, `registrationDeadline`, `ticketQuantity`, `location`, `bannerUrl`, `organizerIds`, `seriesId`, `seriesOrder`, `isDraft`, `isImportant`, `scoreRules`, **`presetCode`**, **`activeScoreEntryCount`**.
- `activeScoreEntryCount` > 0 và `isDraft = false` → FE disable dropdown type và ẩn nút sửa score rules, hiển thị thông báo "Sự kiện đã có X lượt tính điểm, không thể sửa cấu hình điểm".
- `GET /api/activities/{id}` cũng trả cùng `ActivityResponse`, nên FE có thể reload detail sau mỗi CRUD/copy để đồng bộ UI.
- `presetCode` cho phép FE pre-select preset khi mở form edit. `presetConfig` hiện để `null`, FE nên reconstruct từ `scoreRules` hiện có hoặc dùng default của preset.
- Caveat: `copyActivity` copy thông tin và score rules, activity copy được set `isDraft=true`, clear `explicitSemesterId` của copied rules về `ACTIVITY_SEMESTER`, và hiện chưa auto-generate `checkInCode` mới trong copy flow. Nếu FE cần QR ngay cho bản copy, publish/backfill hoặc BE nên bổ sung generate code trong copy flow.

---
#### 6. CRUD coverage của Series

| Nhu cầu FE/Admin | Endpoint hiện có | Coverage |
| --- | --- | --- |
| Tạo series | `POST /api/series` | Đã có |
| Xem list/detail series | `GET /api/series`, `GET /api/series/{seriesId}` | Đã có |
| Sửa series | `PUT /api/series/{seriesId}` | Đã có; hỗ trợ preset/milestone/minimum requirement |
| Xóa series | `DELETE /api/series/{seriesId}` | Đã có soft delete |
| Tạo activity con trong series | `POST /api/series/{seriesId}/activities/create` | Đã có payload tối giản |
| Gắn activity có sẵn vào series | `POST /api/series/{seriesId}/activities` | Đã có |
| Xem activity trong series | `GET /api/series/{seriesId}/activities` | Đã có |
| Student đăng ký series | `POST /api/series/{seriesId}/register` | Đã có |
| Progress student/admin | `GET /api/series/{seriesId}/progress/my`, `GET /api/series/{seriesId}/students/{studentId}/progress`, `GET /api/series/{seriesId}/progress` | Đã có |

Lưu ý cho FE: activity con tạo qua `/activities/create` là payload tối giản, mặc định `requiresSubmission=false`, published, lấy registration config từ series. Nếu cần chỉnh chi tiết activity con sâu hơn, dùng `PUT /api/activities/{id}` sau khi tạo.

Read-back để hiển thị lại form series:

- `POST /api/series`, `PUT /api/series/{seriesId}`, `GET /api/series/{seriesId}` trả `ApiResponse<SeriesResponse>`.
- `SeriesResponse` hiện trả lại `name`, `description`, `milestonePoints`, `scoreType`, `mainActivityId`, `registrationStartDate`, `registrationDeadline`, `requiresApproval`, `ticketQuantity`, `minimumRequirementEnabled`, `minimumRequiredEvents`, `minimumPenaltyPoints`, **`audience`**, **`targetDepartmentIds`**, **`presetCode`**, `createdAt`.
- `presetCode` cho phép FE pre-select preset khi mở form edit series. `presetConfig` hiện để `null`.
- `audience` và `targetDepartmentIds` hiển thị trên màn hình chi tiết series: đối tượng nào được nhận milestone/penalty điểm.

---
#### 6. CRUD coverage của MiniGame

| Nhu cầu FE/Admin | Endpoint hiện có | Coverage |
| --- | --- | --- |
| Tạo minigame/quiz | `POST /api/minigames` | Đã có |
| Xem minigame theo activity | `GET /api/minigames/activity/{activityId}` | Đã có |
| Xem tất cả minigames | `GET /api/minigames` | Đã có |
| Sửa minigame/quiz/questions | `PUT /api/minigames/{miniGameId}` | Đã có |
| Xóa minigame | `DELETE /api/minigames/{miniGameId}` | Đã có deactivate/soft delete |
| Check activity đã có quiz chưa | `GET /api/minigames/activity/{activityId}/check` | Đã có |
| Lấy questions cho student | `GET /api/minigames/{miniGameId}/questions` | Đã có, không trả đáp án đúng |
| Lấy questions cho edit | `GET /api/minigames/{miniGameId}/questions/edit` | Đã có, trả đáp án đúng cho admin/manager |
| Start/submit attempt | `POST /api/minigames/{miniGameId}/start`, `POST /api/minigames/attempts/{attemptId}/submit` | Đã có |
| Xem attempts/detail | `GET /api/minigames/{miniGameId}/attempts/my`, `GET /api/minigames/attempts/{attemptId}` | Đã có |

Read-back để hiển thị lại form minigame:

- `GET /api/minigames/activity/{activityId}` và `GET /api/minigames` trả `MiniGameResponse` với `title`, `description`, `questionCount`, `timeLimit`, `requiredCorrectAnswers`, `maxAttempts`, `isActive`, `showAnswers`, `type`, `activityId`.
- Để edit câu hỏi/options và đáp án đúng, FE phải gọi thêm `GET /api/minigames/{miniGameId}/questions/edit`; `MiniGameResponse` không chứa danh sách questions.
- `PUT /api/minigames/{miniGameId}` nhận lại toàn bộ questions trong `UpdateMiniGameRequest`, nên FE nên gửi danh sách questions/options hoàn chỉnh sau chỉnh sửa.

---
### 5.8 Nhóm API Năm Học / Học Kỳ Cho Dropdown Ghi Điểm

FE cần cho admin chọn rõ học kỳ nào, năm học nào khi rule dùng `semesterPolicy = EXPLICIT_SEMESTER`.

| Nhu cầu FE | Endpoint | Response |
| --- | --- | --- |
| Lấy tất cả năm học | `GET /api/academic/years` | `ApiResponse<AcademicYear[]>` |
| Lấy học kỳ theo năm học | `GET /api/academic/years/{yearId}/semesters` | `ApiResponse<Semester[]>` |
| Lấy tất cả học kỳ | `GET /api/academic/semesters` | `ApiResponse<Semester[]>` |
| Admin quản trị năm học | `GET/POST/PUT/DELETE /api/admin/academics/years...` | `ApiResponse` |
| Admin quản trị học kỳ | `GET/POST/PUT/DELETE /api/admin/academics/semesters...` | `ApiResponse` |
| Mở/đóng học kỳ | `POST /api/admin/academics/semesters/{id}/toggle?open=true|false` | `ApiResponse` |
| Khởi tạo score theo học kỳ | `POST /api/admin/academics/semesters/{id}/initialize-scores` | `ApiResponse` |

Type gợi ý:

```ts
export interface AcademicYear {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
}

export interface Semester {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  open: boolean;
  year?: AcademicYear | null;
}
```

UI đề xuất:

- Bảng score rules có cột `semesterPolicy`.
- Nếu chọn `ACTIVITY_SEMESTER`, disable dropdown học kỳ và gửi `explicitSemesterId: null`.
- Nếu chọn `EXPLICIT_SEMESTER`, bắt buộc chọn `academicYearId` trước, sau đó chọn `semesterId`; request chỉ cần gửi `explicitSemesterId = semester.id`.

---

