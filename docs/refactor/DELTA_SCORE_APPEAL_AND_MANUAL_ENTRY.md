# FE Handoff Delta — Score Appeal & Manual Score Entry

> **Phiên bản:** 1.1  
> **Phạm vi:** Khiếu nại điểm (`ScoreAppeal` + messages + evidence + decide preview) + nhập điểm thủ công đơn / bulk (`ManualScoreAdjustment` → ledger `MANUAL_ADJUSTMENT`)  
> **Stack FE:** TypeScript / React  
> **Đọc kèm:** [`FE_BACKEND_HANDOFF_SPEC.md`](./FE_BACKEND_HANDOFF_SPEC.md) (score history, ranking), [`fe-department-scope-integration-guide.md`](../fe-department-scope-integration-guide.md)

### Changelog 1.1
- Manual: bắt buộc chọn **học kỳ tích điểm** (`semesterId`); thêm **bulk** nhiều sinh viên.
- Appeal: sinh viên upload **ảnh minh chứng**; staff **preview** điểm trước khi decide.

---

## 1. Tóm tắt cho FE

| Tính năng | Ai dùng | Mục đích |
|-----------|---------|----------|
| **Manual score entry** | ADMIN, MANAGER | Nhập điểm trực tiếp — **bắt buộc chọn học kỳ**; hỗ trợ 1 SV hoặc bulk nhiều SV |
| **Score appeal** | STUDENT tạo; ADMIN/MANAGER xử lý | Khiếu nại + ảnh minh chứng → preview điểm → duyệt / từ chối |

**Quy tắc quan trọng:**
- Mọi thay đổi điểm đều ghi vào ledger `score_entries` với `sourceType = "MANUAL_ADJUSTMENT"`.
- Form nhập điểm thủ công **phải** có dropdown học kỳ (`semesterId`) — điểm ghi vào học kỳ đó.
- Sau khi nhập/duyệt điều chỉnh, FE **refresh** score history / score view của sinh viên.
- Response wrapper vẫn là `{ status, message, body }` — đọc dữ liệu ở `body`.
- `BigDecimal` từ BE thường serialize thành **number hoặc string** — FE nên type `number | string` và parse khi cần.

---

## 2. Enums & TypeScript types

Thêm vào `score.ts` (hoặc module tương đương):

```typescript
/** Loại điểm — đã có trong FE_BACKEND_HANDOFF_SPEC */
export type ScoreType = "REN_LUYEN" | "CONG_TAC_XA_HOI" | "CHUYEN_DE";

export type ScoreAppealStatus =
  | "PENDING"
  | "IN_REVIEW"
  | "APPROVED"
  | "REJECTED"
  | "CLOSED";

/** Xuất hiện trong score history khi điểm được nhập thủ công / duyệt khiếu nại */
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

export interface ApiResponse<T> {
  status: boolean;
  message: string;
  body: T;
}
```

### 2.1 Manual score

```typescript
export interface ManualScoreRequest {
  studentId: number;
  /** Học kỳ tích điểm — bắt buộc (dropdown học kỳ trên FE) */
  semesterId: number;
  scoreType: ScoreType;
  /** Có thể âm (phạt) hoặc dương (cộng). Không null. */
  points: number | string;
  reason: string;
  /** Optional — gắn với activity nếu có */
  activityId?: number | null;
}

/** Bulk: cùng học kỳ + loại điểm + lý do chung cho nhiều SV */
export interface BulkManualScoreRequest {
  semesterId: number; // bắt buộc — học kỳ tích điểm
  scoreType: ScoreType;
  reason: string;
  activityId?: number | null;
  entries: Array<{
    studentId: number;
    points: number | string;
    reason?: string | null; // override lý do từng SV
  }>; // max 200
}

export interface BulkManualScoreItemResult {
  studentId: number;
  success: boolean;
  data?: ManualScoreResponse | null;
  error?: string | null;
}

export interface BulkManualScoreResponse {
  semesterId: number;
  scoreType: ScoreType;
  total: number;
  successCount: number;
  failureCount: number;
  results: BulkManualScoreItemResult[];
}

export interface ManualScoreReverseRequest {
  reason: string;
}

export interface ManualScoreResponse {
  adjustmentId: number;
  scoreEntryId: number;
  studentId: number;
  semesterId: number;
  scoreType: ScoreType;
  points: number | string;
  reason: string;
  activityId?: number | null;
  createdByUserId?: number | null;
  createdAt?: string | null; // ISO datetime
}
```

### 2.2 Score appeal

```typescript
export interface CreateScoreAppealRequest {
  semesterId: number;
  scoreType: ScoreType;
  /** ID dòng trong ScoreHistoryDetailResponse.id — optional */
  relatedScoreEntryId?: number | null;
  title: string;
  reason: string;
  /** Điểm đề xuất; null = chỉ yêu cầu xem xét */
  requestedPoints?: number | string | null;
  /** Public URLs từ POST /api/scores/appeals/evidence (max 5) */
  evidenceUrls?: string[] | null;
}

export interface ScoreAppealEvidenceUploadResponse {
  urls: string[];
}

export interface ScoreAppealDecisionPreviewResponse {
  appealId: number;
  studentId: number;
  studentCode?: string | null;
  studentFullName?: string | null;
  semesterId: number;
  scoreType: ScoreType;
  decision: "APPROVED" | "REJECTED";
  currentScore: number | string;
  adjustedPoints?: number | string | null;
  projectedScore: number | string;
  willCreateLedgerEntry: boolean;
  relatedScoreEntryId?: number | null;
  relatedEntryPoints?: number | string | null;
  note: string;
}

export interface ScoreAppealMessageRequest {
  content: string;
}

export interface ScoreAppealDecisionRequest {
  /** Chỉ nhận APPROVED hoặc REJECTED */
  decision: "APPROVED" | "REJECTED";
  decisionNotes?: string | null;
  /**
   * Khi APPROVED và có giá trị → BE tạo MANUAL_ADJUSTMENT ledger.
   * Khi APPROVED và null/omit → chấp nhận nhưng giữ nguyên điểm (không ghi ledger).
   */
  adjustedPoints?: number | string | null;
  /** Override loại điểm khi điều chỉnh (mặc định = appeal.scoreType) */
  scoreType?: ScoreType | null;
  /** Override học kỳ khi điều chỉnh (mặc định = appeal.semesterId) */
  semesterId?: number | null;
}

export interface ScoreAppealMessageResponse {
  id: number;
  senderId: number;
  senderUsername: string;
  content: string;
  createdAt?: string | null;
}

export interface ScoreAppealResponse {
  id: number;
  studentId: number;
  studentCode?: string | null;
  studentFullName?: string | null;
  semesterId: number;
  scoreType: ScoreType;
  relatedScoreEntryId?: number | null;
  title: string;
  reason: string;
  /** Public URLs ảnh minh chứng */
  evidenceUrls: string[];
  requestedPoints?: number | string | null;
  status: ScoreAppealStatus;
  decisionNotes?: string | null;
  decidedAt?: string | null;
  decidedById?: number | null;
  decidedByUsername?: string | null;
  resultingScoreEntryId?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  /** List endpoints thường trả `messages: []`; detail/message/decide trả đầy đủ */
  messages: ScoreAppealMessageResponse[];
}

/** Body của GET /api/scores/appeals (phân trang) */
export interface ScoreAppealPageBody {
  content: ScoreAppealResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
```

---

## 3. API — Manual Score Entry

Base: `/api/scores/manual`  
Roles: **ADMIN**, **MANAGER** only.

### 3.1 Tạo điều chỉnh điểm (1 sinh viên)

```http
POST /api/scores/manual
Authorization: Bearer <token>
Content-Type: application/json
```

**Request**

```json
{
  "studentId": 10,
  "semesterId": 200,
  "scoreType": "CONG_TAC_XA_HOI",
  "points": 5,
  "reason": "Hỗ trợ chuẩn bị sự kiện Open Day",
  "activityId": 55
}
```

| Field | Bắt buộc | Ghi chú |
|-------|----------|---------|
| `studentId` | ✅ | Phải tồn tại, chưa xóa |
| `semesterId` | ✅ | **Học kỳ tích điểm** — FE bắt buộc dropdown chọn học kỳ |
| `scoreType` | ✅ | `REN_LUYEN` \| `CONG_TAC_XA_HOI` \| `CHUYEN_DE` |
| `points` | ✅ | Số thập phân OK |
| `reason` | ✅ | Không blank |
| `activityId` | ❌ | Nếu gửi phải là activity còn active |

### 3.1b Bulk — nhiều sinh viên cùng lúc

```http
POST /api/scores/manual/bulk
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "semesterId": 200,
  "scoreType": "CONG_TAC_XA_HOI",
  "reason": "Hỗ trợ chuẩn bị sự kiện Open Day",
  "activityId": 55,
  "entries": [
    { "studentId": 10, "points": 5 },
    { "studentId": 11, "points": 3, "reason": "Trưởng nhóm chuẩn bị" }
  ]
}
```

- `semesterId` / `scoreType` / `reason` dùng chung cho cả batch (override `reason` từng dòng nếu có).
- Max **200** entries / request.
- Partial success: mỗi SV xử lý riêng; response có `successCount` / `failureCount` / `results[]`.
- Manager: từng SV vẫn check department scope — SV ngoài khoa → dòng đó `success: false`.

**Response** `ApiResponse<BulkManualScoreResponse>`

```json
{
  "status": true,
  "message": "Bulk manual score adjustments processed",
  "body": {
    "semesterId": 200,
    "scoreType": "CONG_TAC_XA_HOI",
    "total": 2,
    "successCount": 2,
    "failureCount": 0,
    "results": [
      { "studentId": 10, "success": true, "data": { "adjustmentId": 99, "scoreEntryId": 500 } },
      { "studentId": 11, "success": true, "data": { "adjustmentId": 100, "scoreEntryId": 501 } }
    ]
  }
}
```

**Gợi ý UI bulk:**
1. Chọn **học kỳ tích điểm** (required).
2. Chọn loại điểm + lý do chung + optional activity.
3. Multi-select sinh viên (Manager: list đã filter theo khoa) + nhập điểm từng người (hoặc điểm mặc định).
4. Submit → hiển thị bảng kết quả success/fail từng dòng.

**Response** `ApiResponse<ManualScoreResponse>`

```json
{
  "status": true,
  "message": "Manual score adjustment created",
  "body": {
    "adjustmentId": 99,
    "scoreEntryId": 500,
    "studentId": 10,
    "semesterId": 200,
    "scoreType": "CONG_TAC_XA_HOI",
    "points": 5,
    "reason": "Hỗ trợ chuẩn bị sự kiện Open Day",
    "activityId": 55,
    "createdByUserId": 1,
    "createdAt": "2026-07-10T00:30:00"
  }
}
```

**Side effects BE:**
- Tạo `ManualScoreAdjustment` + `ScoreEntry` (`MANUAL_ADJUSTMENT`)
- Refresh aggregate `student_scores`
- `AuditLog` action `SCORE_MANUAL_CREATE`
- Notification `SCORE_UPDATE` tới sinh viên

### 3.2 Hủy điều chỉnh (reverse)

```http
POST /api/scores/manual/{adjustmentId}/reverse
Authorization: Bearer <token>
Content-Type: application/json
```

**Request**

```json
{
  "reason": "Nhập nhầm điểm"
}
```

**Response** `ApiResponse<{ adjustmentId: number; reversedEntries: number }>`

```json
{
  "status": true,
  "message": "Manual score adjustment reversed",
  "body": {
    "adjustmentId": 99,
    "reversedEntries": 1
  }
}
```

- Path param dùng **`adjustmentId`** (không phải `scoreEntryId`).
- Chỉ reverse được khi còn entry `ACTIVE`; gọi lại lần 2 → `400`.
- Audit: `SCORE_MANUAL_REVERSE` + notify sinh viên.

### 3.3 Department scope (MANAGER)

- FE **không** gửi danh sách khoa.
- BE lấy scope từ JWT/user login (`DepartmentContextFilter`).
- Manager chỉ nhập/reverse điểm cho sinh viên thuộc khoa được gán.
- Ngoài scope → `403` với message kiểu `"Access denied"`.
- Admin: toàn trường.

### 3.4 Gợi ý UI Manager/Admin

1. Màn **Score management**: form “Nhập điểm thủ công” — **dropdown học kỳ bắt buộc**.
2. Hỗ trợ 2 mode: 1 SV (`POST /manual`) hoặc nhiều SV (`POST /manual/bulk`).
3. Fields: học kỳ, loại điểm, lý do, optional activity, chọn SV (+ điểm từng người nếu bulk).
4. Sau success: toast + reload history theo `semesterId` đã chọn.
5. History: `sourceType = MANUAL_ADJUSTMENT` → label “Điều chỉnh thủ công”.
6. Reverse: dùng `adjustmentId` từ response create/bulk (history không trả `adjustmentId`).

---

## 4. API — Score Appeals

Base: `/api/scores/appeals`

### 4.1 Ma trận endpoint

| Method | Path | Role | Mô tả |
|--------|------|------|-------|
| `POST` | `/api/scores/appeals/evidence` | STUDENT | Upload ảnh minh chứng (multipart) |
| `POST` | `/api/scores/appeals` | STUDENT | Tạo khiếu nại (`PENDING`) |
| `GET` | `/api/scores/appeals/my` | STUDENT | Danh sách của tôi |
| `GET` | `/api/scores/appeals` | ADMIN, MANAGER | Queue + filter + phân trang |
| `GET` | `/api/scores/appeals/{id}` | STUDENT (owner) / ADMIN / MANAGER | Chi tiết + messages + evidence |
| `POST` | `/api/scores/appeals/{id}/messages` | Owner / ADMIN / MANAGER | Thêm tin nhắn |
| `POST` | `/api/scores/appeals/{id}/decide/preview` | ADMIN, MANAGER | Xem trước điểm (không ghi DB) |
| `PUT` | `/api/scores/appeals/{id}/decide` | ADMIN, MANAGER | Duyệt / từ chối |
| `PUT` | `/api/scores/appeals/{id}/close` | ADMIN, MANAGER | Đóng hồ sơ |
| `PUT` | `/api/scores/appeals/{id}/withdraw` | STUDENT | Rút khi `PENDING` → `CLOSED` |

### 4.1b Student — upload ảnh minh chứng

```http
POST /api/scores/appeals/evidence
Content-Type: multipart/form-data
```

| Part | Ghi chú |
|------|---------|
| `files` | 1–5 ảnh; mỗi file ≤ 5MB; chỉ image |

**Response**

```json
{
  "status": true,
  "message": "Evidence uploaded",
  "body": {
    "urls": [
      "http://localhost:8080/uploads/score-appeals/10/uuid.jpg"
    ]
  }
}
```

**Luồng FE (2 bước):**
1. User chọn ảnh → `POST .../evidence` → nhận `urls`.
2. `POST .../appeals` với `evidenceUrls: urls`.

Không dùng `/api/upload/image` (chỉ ADMIN/MANAGER).

### 4.2 Student — tạo khiếu nại

```http
POST /api/scores/appeals
```

```json
{
  "semesterId": 200,
  "scoreType": "REN_LUYEN",
  "relatedScoreEntryId": 501,
  "title": "Sai điểm check-in",
  "reason": "Tôi đã điểm danh nhưng bị trừ điểm",
  "requestedPoints": 10,
  "evidenceUrls": [
    "http://localhost:8080/uploads/score-appeals/10/uuid.jpg"
  ]
}
```

- `relatedScoreEntryId` = `ScoreHistoryDetailResponse.id` của dòng đang khiếu nại.
- Entry phải thuộc chính sinh viên đang login; sai → `403`.
- `evidenceUrls` optional; max 5; lấy từ bước upload evidence.
- Response: `ApiResponse<ScoreAppealResponse>` (`status: PENDING`, `evidenceUrls`, `messages: []`).

**Gợi ý UX:** Trên trang Score History, mỗi dòng có nút “Khiếu nại” → prefill `relatedScoreEntryId`, `scoreType`, `semesterId` + upload ảnh.

### 4.3 Student — danh sách của tôi

```http
GET /api/scores/appeals/my
```

Response: `ApiResponse<ScoreAppealResponse[]>` (không phân trang; `messages` thường rỗng — gọi detail để lấy thread).

### 4.4 Staff — danh sách / queue

```http
GET /api/scores/appeals?status=PENDING&semesterId=200&studentId=10&page=0&size=20
```

| Query | Optional | Ghi chú |
|-------|----------|---------|
| `status` | ✅ | `PENDING` \| `IN_REVIEW` \| `APPROVED` \| `REJECTED` \| `CLOSED` |
| `semesterId` | ✅ | |
| `studentId` | ✅ | Manager: student phải trong khoa |
| `page` | ✅ | default `0` |
| `size` | ✅ | default `20`, max 100 |

Response: `ApiResponse<ScoreAppealPageBody>`

**MANAGER:** chỉ thấy appeal của sinh viên thuộc khoa mình.  
**ADMIN:** toàn trường.

### 4.5 Chi tiết + messages

```http
GET /api/scores/appeals/{id}
```

Response: `ApiResponse<ScoreAppealResponse>` với `messages` đầy đủ, sort theo `createdAt` ASC.

### 4.6 Thêm message

```http
POST /api/scores/appeals/{id}/messages
```

```json
{
  "content": "Vui lòng gửi ảnh check-in"
}
```

- Chỉ khi status `PENDING` hoặc `IN_REVIEW`.
- **Staff** (ADMIN/MANAGER) gửi message lần đầu khi đang `PENDING` → BE tự chuyển `IN_REVIEW`.
- Student gửi message **không** đổi status.

### 4.7 Preview điểm trước khi decide (ADMIN/MANAGER)

```http
POST /api/scores/appeals/{id}/decide/preview
```

Body giống `ScoreAppealDecisionRequest` (cùng payload sẽ gửi decide).

**Response** `ApiResponse<ScoreAppealDecisionPreviewResponse>`

```json
{
  "status": true,
  "message": "Score appeal decision preview",
  "body": {
    "appealId": 50,
    "studentId": 10,
    "studentCode": "SV001",
    "studentFullName": "Nguyen Van A",
    "semesterId": 200,
    "scoreType": "REN_LUYEN",
    "decision": "APPROVED",
    "currentScore": 80,
    "adjustedPoints": 5,
    "projectedScore": 85,
    "willCreateLedgerEntry": true,
    "relatedScoreEntryId": 501,
    "relatedEntryPoints": -10,
    "note": "Chấp nhận và điều chỉnh điểm — sẽ tạo bản ghi MANUAL_ADJUSTMENT."
  }
}
```

- **Không ghi DB** — chỉ tính `projectedScore = currentScore + adjustedPoints` khi `APPROVED` + có `adjustedPoints`.
- FE nên gọi preview khi staff đổi `adjustedPoints` / `scoreType` / `semesterId` (debounce), hiển thị: `80 → 85`.
- Confirm mới gọi `PUT .../decide` với cùng body.

### 4.8 Quyết định (decide)

```http
PUT /api/scores/appeals/{id}/decide
```

**Duyệt + điều chỉnh điểm**

```json
{
  "decision": "APPROVED",
  "decisionNotes": "Bổ sung 5 điểm CTXH",
  "adjustedPoints": 5,
  "scoreType": "CONG_TAC_XA_HOI"
}
```

**Duyệt giữ nguyên điểm**

```json
{
  "decision": "APPROVED",
  "decisionNotes": "Đã kiểm tra, điểm hiện tại đúng"
}
```

**Từ chối**

```json
{
  "decision": "REJECTED",
  "decisionNotes": "Không đủ căn cứ"
}
```

| Trường hợp | Ledger |
|------------|--------|
| `APPROVED` + có `adjustedPoints` | Tạo `ManualScoreAdjustment` + `ScoreEntry` (`MANUAL_ADJUSTMENT`); set `resultingScoreEntryId` |
| `APPROVED` không `adjustedPoints` | Không ghi ledger |
| `REJECTED` | Không ghi ledger |

- Chỉ decide từ `PENDING` hoặc `IN_REVIEW`.
- `decision` khác `APPROVED`/`REJECTED` → `400`.
- Notify sinh viên (`SCORE_UPDATE`).
- **UX khuyến nghị:** luôn gọi preview trước khi confirm decide.

### 4.9 Close / Withdraw

```http
PUT /api/scores/appeals/{id}/close      # staff
PUT /api/scores/appeals/{id}/withdraw   # student, chỉ PENDING → CLOSED
```

Không body.

### 4.10 State machine (FE hiển thị)

```text
PENDING ──staff message──► IN_REVIEW
PENDING ──decide──────────► APPROVED | REJECTED
PENDING ──student withdraw─► CLOSED
IN_REVIEW ──decide────────► APPROVED | REJECTED
IN_REVIEW ──close─────────► CLOSED
APPROVED | REJECTED ──close─► CLOSED
```

Gợi ý badge màu:

| Status | Label VI gợi ý |
|--------|----------------|
| `PENDING` | Chờ xử lý |
| `IN_REVIEW` | Đang xem xét |
| `APPROVED` | Đã chấp nhận |
| `REJECTED` | Từ chối |
| `CLOSED` | Đã đóng |

---

## 5. Notification

Type: `SCORE_UPDATE` (đã có sẵn).

FE nên:
- Hiển thị trong notification center khi sinh viên nhận cập nhật điểm / quyết định khiếu nại.
- Optional deep-link: `/scores/appeals/{appealId}` hoặc `/scores/history` (tùy router FE).
- Metadata có thể chứa `appealId`, `adjustmentId`, `scoreEntryId`, `status` — không phụ thuộc cứng nếu thiếu field.

---

## 6. Liên kết với Score History hiện có

API cũ không đổi:

```http
GET /api/scores/history/student/{studentId}?semesterId=...&scoreType=...&page=0&size=20
```

Sau manual entry / approve-with-adjust:
- History xuất hiện dòng mới; `sourceType` = `"MANUAL_ADJUSTMENT"` (hoặc string tương đương trong mapping FE hiện tại).
- `ScoreHistoryDetailResponse.id` = `scoreEntryId` — dùng làm `relatedScoreEntryId` khi tạo appeal.

**Lưu ý:** History **không** trả `adjustmentId`. Nếu FE cần nút reverse từ history:
- Lưu `adjustmentId` từ response `POST /api/scores/manual`, hoặc
- Chỉ reverse ngay sau khi tạo / từ màn quản lý điều chỉnh riêng (product quyết định).

---

## 7. Error handling (FE)

| HTTP | Khi nào | FE nên làm |
|------|---------|------------|
| `400` | Validation, transition sai, reverse khi không còn ACTIVE | Toast `message` từ body |
| `403` | Ngoài khoa Manager; student xem appeal người khác; related entry không thuộc mình | Toast “Không có quyền” |
| `404` | Student/semester/appeal/entry không tồn tại | Toast + redirect list |

Luôn đọc `response.message` (wrapper `ApiResponse`).

---

## 8. Checklist tích hợp FE

### Shared
- [ ] Thêm types vào `score.ts` (bulk, evidence, preview)
- [ ] API client: `createManualScore`, `createBulkManualScore`, `reverseManualScore`, `uploadAppealEvidence`, `createAppeal`, `listMyAppeals`, `listAppeals`, `getAppeal`, `addAppealMessage`, `previewAppealDecision`, `decideAppeal`, `closeAppeal`, `withdrawAppeal`
- [ ] Map `MANUAL_ADJUSTMENT` trong UI source-type label của Score History

### Student
- [ ] Nút “Khiếu nại” trên Score History → form + upload ảnh minh chứng (2 bước)
- [ ] Trang “Khiếu nại của tôi” (`/appeals/my`)
- [ ] Chi tiết appeal: gallery evidence + thread messages + withdraw (chỉ `PENDING`)
- [ ] Listen notification `SCORE_UPDATE`

### Manager / Admin
- [ ] Form nhập điểm: **dropdown học kỳ bắt buộc** + mode 1 SV / bulk nhiều SV
- [ ] Queue khiếu nại: filter status/semester, phân trang; xem ảnh minh chứng
- [ ] Detail decide: nhập `adjustedPoints` → gọi **preview** hiển thị `currentScore → projectedScore` → confirm decide
- [ ] Sau decide có adjust: refresh history sinh viên

### Không cần làm
- Không gửi `departmentIds` trong request score/appeal
- Không gọi API duyệt riêng cho manual entry (đã nhập trực tiếp)
- Không dùng `/api/upload/image` cho student evidence
- Không sửa contract score history cũ ngoài nhận thêm `MANUAL_ADJUSTMENT`

---

## 9. Ví dụ client TypeScript (sketch)

```typescript
// api/scores.ts
import type {
  ApiResponse,
  BulkManualScoreRequest,
  BulkManualScoreResponse,
  CreateScoreAppealRequest,
  ManualScoreRequest,
  ManualScoreResponse,
  ManualScoreReverseRequest,
  ScoreAppealDecisionPreviewResponse,
  ScoreAppealDecisionRequest,
  ScoreAppealMessageRequest,
  ScoreAppealPageBody,
  ScoreAppealResponse,
  ScoreAppealStatus,
} from "@/types/score";

const jsonHeaders = { "Content-Type": "application/json" };

export async function createManualScore(
  token: string,
  body: ManualScoreRequest
): Promise<ApiResponse<ManualScoreResponse>> {
  const res = await fetch("/api/scores/manual", {
    method: "POST",
    headers: { ...jsonHeaders, Authorization: `Bearer ${token}` },
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function createBulkManualScore(
  token: string,
  body: BulkManualScoreRequest
): Promise<ApiResponse<BulkManualScoreResponse>> {
  const res = await fetch("/api/scores/manual/bulk", {
    method: "POST",
    headers: { ...jsonHeaders, Authorization: `Bearer ${token}` },
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function uploadAppealEvidence(
  token: string,
  files: File[]
): Promise<ApiResponse<{ urls: string[] }>> {
  const form = new FormData();
  files.forEach((f) => form.append("files", f));
  const res = await fetch("/api/scores/appeals/evidence", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  });
  return res.json();
}

export async function previewAppealDecision(
  token: string,
  id: number,
  body: ScoreAppealDecisionRequest
): Promise<ApiResponse<ScoreAppealDecisionPreviewResponse>> {
  const res = await fetch(`/api/scores/appeals/${id}/decide/preview`, {
    method: "POST",
    headers: { ...jsonHeaders, Authorization: `Bearer ${token}` },
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function reverseManualScore(
  token: string,
  adjustmentId: number,
  body: ManualScoreReverseRequest
): Promise<ApiResponse<{ adjustmentId: number; reversedEntries: number }>> {
  const res = await fetch(`/api/scores/manual/${adjustmentId}/reverse`, {
    method: "POST",
    headers: { ...jsonHeaders, Authorization: `Bearer ${token}` },
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function createScoreAppeal(
  token: string,
  body: CreateScoreAppealRequest
): Promise<ApiResponse<ScoreAppealResponse>> {
  const res = await fetch("/api/scores/appeals", {
    method: "POST",
    headers: { ...jsonHeaders, Authorization: `Bearer ${token}` },
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function listMyScoreAppeals(
  token: string
): Promise<ApiResponse<ScoreAppealResponse[]>> {
  const res = await fetch("/api/scores/appeals/my", {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.json();
}

export async function listScoreAppeals(
  token: string,
  params: {
    status?: ScoreAppealStatus;
    semesterId?: number;
    studentId?: number;
    page?: number;
    size?: number;
  }
): Promise<ApiResponse<ScoreAppealPageBody>> {
  const q = new URLSearchParams();
  if (params.status) q.set("status", params.status);
  if (params.semesterId != null) q.set("semesterId", String(params.semesterId));
  if (params.studentId != null) q.set("studentId", String(params.studentId));
  q.set("page", String(params.page ?? 0));
  q.set("size", String(params.size ?? 20));
  const res = await fetch(`/api/scores/appeals?${q}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.json();
}

export async function getScoreAppeal(
  token: string,
  id: number
): Promise<ApiResponse<ScoreAppealResponse>> {
  const res = await fetch(`/api/scores/appeals/${id}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.json();
}

export async function addScoreAppealMessage(
  token: string,
  id: number,
  body: ScoreAppealMessageRequest
): Promise<ApiResponse<ScoreAppealResponse>> {
  const res = await fetch(`/api/scores/appeals/${id}/messages`, {
    method: "POST",
    headers: { ...jsonHeaders, Authorization: `Bearer ${token}` },
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function decideScoreAppeal(
  token: string,
  id: number,
  body: ScoreAppealDecisionRequest
): Promise<ApiResponse<ScoreAppealResponse>> {
  const res = await fetch(`/api/scores/appeals/${id}/decide`, {
    method: "PUT",
    headers: { ...jsonHeaders, Authorization: `Bearer ${token}` },
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function closeScoreAppeal(
  token: string,
  id: number
): Promise<ApiResponse<ScoreAppealResponse>> {
  const res = await fetch(`/api/scores/appeals/${id}/close`, {
    method: "PUT",
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.json();
}

export async function withdrawScoreAppeal(
  token: string,
  id: number
): Promise<ApiResponse<ScoreAppealResponse>> {
  const res = await fetch(`/api/scores/appeals/${id}/withdraw`, {
    method: "PUT",
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.json();
}
```

---

## 10. Backend files tham chiếu

| Layer | Path |
|-------|------|
| Controllers | `controller/score/ManualScoreController.java`, `ScoreAppealController.java` |
| Services | `service/impl/ManualScoreServiceImpl.java`, `ScoreAppealServiceImpl.java` |
| DTOs | `model/score/ManualScore*.java`, `CreateScoreAppealRequest.java`, `ScoreAppeal*.java` |
| Migrations | `db/migration/V1032__score_appeals_and_manual_adjustments.sql`, `V1033__score_appeal_evidence_urls.sql` |
| Security | `config/SecurityConfig.java` (khối Score Management) |
