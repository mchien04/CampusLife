# Hướng dẫn Frontend (TypeScript) - Module Chuẩn bị Sự kiện (Preparation)

## 1. Mục tiêu UI/UX theo role

### 1.1. Sinh viên BTC (Organizer) - Web
Đề xuất layout trong trang chi tiết Activity:
- Tab "Nhiệm vụ"
  - Hiển thị list `PreparationTask` của Activity
  - Click vào item -> mở modal/inline action đổi trạng thái `PENDING/ACCEPTED/COMPLETED`
- Tab "Tài chính"
  - Chỉ hiển thị nếu activity có Budget (dashboard trả `budget != null`)
  - Hiển thị:
    - Tổng ngân sách
    - Đã chi (chỉ APPROVED)
    - Còn lại
  - Nút "+ Thêm chi phí"
    - Form: số tiền, nội dung, nút "Chụp ảnh hóa đơn"
    - Sau khi chụp/chọn ảnh:
      - Nén ảnh (client) trước khi upload
      - Hiển thị thumbnail để xác nhận
    - Flow submit:
      1) upload ảnh -> nhận `evidenceUrl`
      2) createExpense với amount/description/evidenceUrl
    - UI hiển thị trạng thái: `WAITING_APPROVAL` sau khi gửi

### 1.2. Admin/Manager - Web Dashboard
Đề xuất 1 màn hình "Preparation Management":
- Bảng Activity có Preparation bật:
  - Cột: tên activity, số task pending, số expense waiting approval, remaining budget
  - Click row -> mở trang chi tiết preparation của activity
- Trang chi tiết:
  - Panel "Task"
    - Bảng: task title, assignee, status, deadline
  - Panel "Chi phí"
    - Filter trạng thái: ALL / WAITING_APPROVAL / APPROVED / REJECTED
    - Table: amount, description, reportedBy, createdAt, status, evidence
    - Cột "Minh chứng":
      - icon kính lúp/ảnh -> mở modal phóng to (img src = evidenceUrl)
      - Nút hành động: "Duyệt" / "Từ chối" (chỉ hiện khi WAITING_APPROVAL)

## 2. Quy ước trạng thái chi phí (FE)
Backend lưu `Expense.approved: Boolean | null`:
- `null` => `WAITING_APPROVAL`
- `true` => `APPROVED`
- `false` => `REJECTED`

FE nên map thành enum để UI rõ ràng:

```ts
export type ExpenseApprovalState = 'WAITING_APPROVAL' | 'APPROVED' | 'REJECTED';

export function mapApproval(approved: boolean | null | undefined): ExpenseApprovalState {
  if (approved === true) return 'APPROVED';
  if (approved === false) return 'REJECTED';
  return 'WAITING_APPROVAL';
}
```

Lưu ý nghiệp vụ: `remaining` được tính dựa trên `APPROVED` בלבד, nên các khoản `WAITING_APPROVAL` chưa trừ ngân sách.

## 3. API contract & types TypeScript
Backend trả chuẩn:

```ts
export type ApiResponse<T> = {
  status: boolean;
  message: string;
  body: T;
};
```

### 3.1. DTO types

```ts
export type PreparationTaskStatus = 'PENDING' | 'ACCEPTED' | 'COMPLETED';

export type PreparationTaskDto = {
  id: number;
  activityId: number;
  assigneeId: number;
  assigneeName: string | null;
  title: string;
  description: string | null;
  deadline: string | null;
  status: PreparationTaskStatus;
};

export type BudgetDto = {
  id: number;
  activityId: number | null;
  totalAmount: string;
  spentAmount: string;
  remainingAmount: string;
  description: string | null;
};

export type PreparationDashboardDto = {
  activityId: number;
  hasPreparation: boolean;
  tasks: PreparationTaskDto[];
  budget: BudgetDto | null;
  financeMessage: string | null;
};

export type ExpenseDto = {
  id: number;
  activityId: number;
  budgetId: number;
  amount: string;
  description: string | null;
  evidenceUrl: string | null;
  reportedById: number;
  reportedByName: string | null;
  approved: boolean | null;
  createdAt: string;
};

export type UploadResultDto = { url: string };
```

Ghi chú: `amount/totalAmount/...` là `BigDecimal` nên backend trả thường là string. FE nên dùng formatter (Intl) và chỉ convert number khi thật cần thiết.

## 4. Endpoint usage theo role (gợi ý)

### 4.1. Organizer
- Dashboard:
  - `GET /api/preparation/activities/{activityId}/dashboard`
- Update task status của mình:
  - `PUT /api/preparation/tasks/{taskId}/status`
  - body: `{ status: 'ACCEPTED' }`
- Upload hóa đơn:
  - `POST /api/preparation/activities/{activityId}/expenses/evidence` (multipart)
- Tạo expense:
  - `POST /api/preparation/activities/{activityId}/expenses`
  - body: `{ amount, description, evidenceUrl }`
- Xem danh sách expense:
  - `GET /api/preparation/activities/{activityId}/expenses?status=ALL|PENDING|APPROVED|REJECTED`

### 4.2. Admin/Manager
- Toggle:
  - `PUT /api/preparation/activities/{activityId}/toggle?enabled=true`
- Add/Remove organizer:
  - `POST/DELETE /api/preparation/activities/{activityId}/organizers/{studentId}`
- Upsert budget:
  - `PUT /api/preparation/activities/{activityId}/budget`
  - body: `{ totalAmount, description }`
- Assign task:
  - `POST /api/preparation/activities/{activityId}/tasks`
  - body: `{ assigneeId, title, description, deadline }`
- List expense + approve/reject:
  - `GET /api/preparation/activities/{activityId}/expenses?...`
  - `PUT /api/preparation/expenses/{expenseId}/approval`
    - body: `{ approved: true }` hoặc `{ approved: false }`

## 5. Gợi ý triển khai client (TypeScript)

### 5.1. API client tối thiểu (fetch)

```ts
async function apiFetch<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    ...init,
    headers: {
      ...(init?.headers ?? {}),
      'Content-Type': init?.body instanceof FormData ? undefined : 'application/json',
    },
  });

  const data = (await res.json()) as ApiResponse<T>;
  if (!data.status) throw new Error(data.message || 'Request failed');
  return data.body;
}
```

Khi upload `FormData`, không set `Content-Type` thủ công (browser sẽ tự set boundary).

### 5.2. Upload ảnh hóa đơn + tạo expense

```ts
export async function uploadEvidence(activityId: number, file: File): Promise<string> {
  const fd = new FormData();
  fd.append('file', file);
  const body = await apiFetch<UploadResultDto>(`/api/preparation/activities/${activityId}/expenses/evidence`, {
    method: 'POST',
    body: fd,
  });
  return body.url;
}

export async function createExpense(activityId: number, payload: { amount: string; description?: string; evidenceUrl?: string; }) {
  return apiFetch<ExpenseDto>(`/api/preparation/activities/${activityId}/expenses`, {
    method: 'POST',
    body: JSON.stringify({
      amount: payload.amount,
      description: payload.description ?? null,
      evidenceUrl: payload.evidenceUrl ?? null,
    }),
  });
}
```

## 6. Nén ảnh hóa đơn (khuyến nghị)

### 6.1. Nén ảnh phía Client (không dùng thư viện)
Mục tiêu:
- Giới hạn chiều rộng tối đa (vd 1280px)
- Chất lượng JPEG ~0.7–0.8
- Output ~200KB–800KB (tùy hóa đơn)

```ts
export async function compressImage(file: File, opts?: { maxWidth?: number; quality?: number }) {
  const maxWidth = opts?.maxWidth ?? 1280;
  const quality = opts?.quality ?? 0.75;

  const imageBitmap = await createImageBitmap(file);
  const scale = Math.min(1, maxWidth / imageBitmap.width);
  const targetW = Math.round(imageBitmap.width * scale);
  const targetH = Math.round(imageBitmap.height * scale);

  const canvas = document.createElement('canvas');
  canvas.width = targetW;
  canvas.height = targetH;
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('Canvas not supported');
  ctx.drawImage(imageBitmap, 0, 0, targetW, targetH);

  const blob: Blob = await new Promise((resolve, reject) => {
    canvas.toBlob(
      b => (b ? resolve(b) : reject(new Error('Failed to compress image'))),
      'image/jpeg',
      quality
    );
  });

  return new File([blob], file.name.replace(/\.\w+$/, '.jpg'), { type: 'image/jpeg' });
}
```

Flow đề xuất trong UI:
- User chọn/chụp ảnh -> `compressImage` -> hiển thị thumbnail -> upload.

### 6.2. Nén ảnh phía Server
Hiện backend đang lưu local theo FileUploadService, chưa có bước nén server-side. Nếu muốn đồng nhất và đảm bảo tiết kiệm dung lượng ngay cả khi client không nén, có thể bổ sung thêm nén server-side (đề xuất làm sau).

## 7. Gợi ý đảm bảo “cùng theme”
- Giữ nguyên kiểu response `{status,message,body}` và toast theo `message`.
- Quy ước màu trạng thái dùng chung:
  - WAITING_APPROVAL: warning
  - APPROVED: success
  - REJECTED: danger
  - Task COMPLETED: success
- Dùng cùng format tiền tệ và ngày giờ trong toàn dự án (Intl.NumberFormat + Intl.DateTimeFormat).

