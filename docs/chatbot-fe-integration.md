# Chatbot FE Integration (API-First)

### 1. Mô tả nghiệp vụ
Chatbot hỗ trợ sinh viên hỏi đáp thông tin sự kiện dựa trên dữ liệu hệ thống (Activity/Registration). FE có thể dùng ở:
- **Global** (ngoài trang chi tiết): sinh viên nêu tên sự kiện hoặc hỏi “đang mở đăng ký…”, bot tự tìm sự kiện.
- **Activity detail** (trong trang sự kiện): FE truyền `contextActivityId` để bot trả lời bám đúng sự kiện.
- **Article detail** (trong trang bài viết): FE truyền `contextArticleSlug` để bot trả lời dựa trên bài viết và mapping sang sự kiện (nếu có).

Chatbot hỗ trợ hội thoại nhiều lượt bằng `conversationId`, và hỗ trợ “chọn sự kiện” khi bot đưa danh sách gợi ý.

---

### 2. API Endpoint
- **Method:** POST
- **Path:** `/api/chatbot`
- **Authentication:** Required (JWT Bearer)

---

### 2.1 API Endpoint (Status)
- **Method:** GET
- **Path:** `/api/chatbot/status`
- **Authentication:** Required (JWT Bearer)

---

### 3. Request
- **Path Parameters:** Không có
- **Query Parameters:** Không có
- **Request Body:**
  ```json
  {
    "conversationId": "number|null - id hội thoại (null để tạo mới)",
    "contextActivityId": "number|null - id Activity nếu đang ở trang chi tiết sự kiện",
    "contextArticleSlug": "string|null - slug bài viết nếu đang ở trang bài viết",
    "pageContext": "string - GLOBAL | ACTIVITY_DETAIL | ARTICLE_DETAIL",
    "message": "string - nội dung câu hỏi của sinh viên"
  }
  ```

#### TypeScript types (khuyến nghị)
```ts
export type ChatbotPageContext = "GLOBAL" | "ACTIVITY_DETAIL" | "ARTICLE_DETAIL";

export interface ChatbotMessageRequest {
  conversationId?: number | null;
  contextActivityId?: number | null;
  contextArticleSlug?: string | null;
  pageContext?: ChatbotPageContext; // default: "GLOBAL"
  message: string;
}
```

**Header bắt buộc**
- `Authorization: Bearer <JWT>`
- `Content-Type: application/json`

**Gợi ý FE usage**
- Lần đầu mở widget: gọi với `conversationId=null`.
- Các lần sau: giữ lại `conversationId` từ response và gửi lại cho mọi message tiếp theo.
- Nếu đang ở trang chi tiết sự kiện: luôn gửi `contextActivityId=<activityId>` và `pageContext=ACTIVITY_DETAIL`.
- Nếu đang ở trang bài viết: gửi `contextArticleSlug=<slug>` và `pageContext=ARTICLE_DETAIL`.
- Nếu ở global: gửi `pageContext=GLOBAL`, `contextActivityId=null`.

**Ví dụ request (global)**
```json
{
  "conversationId": null,
  "contextActivityId": null,
  "contextArticleSlug": null,
  "pageContext": "GLOBAL",
  "message": "Sự kiện nào đang mở đăng ký?"
}
```

**Ví dụ request (trang chi tiết)**
```json
{
  "conversationId": 10,
  "contextActivityId": 123,
  "contextArticleSlug": null,
  "pageContext": "ACTIVITY_DETAIL",
  "message": "Hạn đăng ký khi nào?"
}
```

**Ví dụ request (trang bài viết)**
```json
{
  "conversationId": 10,
  "contextActivityId": null,
  "contextArticleSlug": "su-kien-chao-tan-sv-2026",
  "pageContext": "ARTICLE_DETAIL",
  "message": "Bài viết này của sự kiện nào?"
}
```

**Ví dụ request (chọn option sau khi bot gợi ý danh sách)**
```json
{
  "conversationId": 10,
  "contextActivityId": null,
  "contextArticleSlug": null,
  "pageContext": "GLOBAL",
  "message": "chọn số 2"
}
```

**Kiểm tra Gemini đã bật chưa (status)**
```bash
curl --request GET "http://localhost:8080/api/chatbot/status" \
  --header "Authorization: Bearer <JWT>"
```

---

### 4. Response
Lưu ý: API hiện tại trả về DTO trực tiếp, chưa bọc theo format `{code,message,data}`.

- **Success (200):**

  ```json
  {
    "conversationId": 10,
    "answer": "string - câu trả lời",
    "resolvedActivity": {
      "id": 123,
      "name": "Workshop ABC"
    },
    "needsClarification": false,
    "activityOptions": [
      {
        "id": 123,
        "name": "Workshop ABC",
        "startDate": "2026-05-20T09:00:00",
        "location": "Hội trường A"
      }
    ]
  }
  ```

#### TypeScript types (khuyến nghị)
```ts
export interface ChatbotResolvedActivityResponse {
  id: number;
  name: string;
}

export interface ChatbotActivityOptionResponse {
  id: number;
  name: string;
  startDate: string | null; // ISO LocalDateTime (không có timezone)
  location: string | null;
}

export interface ChatbotMessageResponse {
  conversationId: number;
  answer: string;
  resolvedActivity: ChatbotResolvedActivityResponse | null;
  needsClarification: boolean;
  activityOptions: ChatbotActivityOptionResponse[];
}

export interface ChatbotStatusResponse {
  geminiEnabled: boolean;
  geminiModel: string;
}
```

**Ý nghĩa field**
- `conversationId`: FE lưu lại để chat nhiều lượt.
- `answer`: text hiển thị trong UI.
- `resolvedActivity`: nếu bot xác định được đang nói về sự kiện nào (có thể null).
- `needsClarification`:
  - `true`: bot chưa chắc sự kiện nào → FE nên render danh sách `activityOptions` cho người dùng chọn.
  - `false`: trả lời bình thường.
- `activityOptions`: danh sách lựa chọn (thường 0..5 phần tử).

**Error Responses**
- **401 Unauthorized:** thiếu/invalid JWT.
- **500 Internal Server Error:** lỗi server (DB, hoặc lỗi gọi Gemini khi cấu hình; hiện backend có fallback nhưng vẫn có thể phát sinh lỗi khác).

---

## Luồng UI khuyến nghị cho FE
### 1) Khởi tạo widget
- Khi mở widget lần đầu, gọi `/api/chatbot` với `conversationId: null` để nhận `conversationId`.
- Lưu `conversationId` ở state (React state/store). Không cần localStorage nếu không muốn persist, nhưng nên persist để refresh trang không mất hội thoại.

### Global widget
- Input text → POST `/api/chatbot` với `pageContext=GLOBAL`.
- Nếu `needsClarification=true`:
  - Render list `activityOptions` dạng nút.
  - Khi click:
    - Cách 1: gửi message “chọn số X”.
    - Cách 2: điều hướng sang trang chi tiết event và bắt đầu hỏi với `contextActivityId`.

### Widget trong trang Activity detail
- Mỗi message đều gửi `contextActivityId` hiện tại + `pageContext=ACTIVITY_DETAIL`.
- FE vẫn lưu `conversationId` để giữ hội thoại.

### Widget trong trang Article detail
- Mỗi message đều gửi `contextArticleSlug` hiện tại + `pageContext=ARTICLE_DETAIL`.
- Các câu hỏi hỗ trợ tốt: “bài viết này của sự kiện nào”, “tóm tắt bài viết”, “sự kiện này có bài viết không”.

### Auth
- Reuse JWT đang dùng cho các API khác, gắn vào header `Authorization: Bearer ...`.

---

## 5. Tích hợp Score Engine mới (v6.1+)

Chatbot đã được cập nhật để giải thích quy tắc tính điểm theo Score Engine mới của hệ thống. Khi sinh viên hỏi về điểm ("sự kiện này cộng bao nhiêu điểm", "có bị trừ điểm không", "no-show", "quá hạn nộp bài", "milestone"...), bot sẽ:

- Đọc các `scoreRules` đang bật của activity theo trigger: `PARTICIPATION_COMPLETED`, `NO_SHOW`, `SUBMISSION_GRADED`, `TASK_OVERDUE`, `MINIGAME_PASSED`, `MINIGAME_EXHAUSTED_ATTEMPTS`, `SERIES_MILESTONE_REACHED`.
- Hiển thị loại điểm (`REN_LUYEN`, `CONG_TAC_XA_HOI`, `CHUYEN_DE`), số điểm cộng/trừ, đối tượng áp dụng và preset code (nếu có).
- Nếu activity thuộc Series (`seriesId != null`), bot thông báo sự kiện con không cộng điểm riêng lẻ mà tính theo mốc chuỗi.
- Nếu `requiresSubmission = true`, bot lưu ý sinh viên phải điểm danh **và** được chấm bài thì điểm mới được công nhận đầy đủ.

FE không cần thay đổi API contract; nội dung trả về nằm trong field `answer` như bình thường.

---

## Gợi ý triển khai FE (TypeScript)
### API client (ví dụ)
```ts
export async function postChatbot(
  baseUrl: string,
  token: string,
  payload: ChatbotMessageRequest
): Promise<ChatbotMessageResponse> {
  const res = await fetch(`${baseUrl}/api/chatbot`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      conversationId: payload.conversationId ?? null,
      contextActivityId: payload.contextActivityId ?? null,
      contextArticleSlug: payload.contextArticleSlug ?? null,
      pageContext: payload.pageContext ?? "GLOBAL",
      message: payload.message,
    }),
  });

  if (!res.ok) {
    throw new Error(`Chatbot API failed: ${res.status}`);
  }

  return (await res.json()) as ChatbotMessageResponse;
}

export async function getChatbotStatus(
  baseUrl: string,
  token: string
): Promise<ChatbotStatusResponse> {
  const res = await fetch(`${baseUrl}/api/chatbot/status`, {
    method: "GET",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error(`Chatbot status failed: ${res.status}`);
  return (await res.json()) as ChatbotStatusResponse;
}
```

### Xử lý options (needsClarification)
- Nếu `needsClarification=true`:
  - Render `activityOptions` dạng list button.
  - Khi user click option i, có 2 cách:
    - gửi tin nhắn `"chọn số ${i + 1}"`
    - hoặc gửi trực tiếp tin nhắn có kèm `contextActivityId = option.id` (khuyến nghị UX tốt hơn)


