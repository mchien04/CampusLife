# EventArticle (Landing Page) - Hướng dẫn cho Frontend

## 1. Mô tả nghiệp vụ

Module EventArticle cung cấp trang bài viết quảng bá (dạng blog/landing page) cho một sự kiện (Activity). Sinh viên truy cập bằng `slug` (SEO-friendly URL). Backend trả về nội dung HTML + metadata SEO + trạng thái CTA đăng ký động dựa theo thời gian đăng ký và số lượng vé.

## 2. API Endpoint

- **Method:** GET
- **Path:** `/api/articles/{slug}`
- **Authentication:** Not required

## 3. Request

- **Path Parameters:**
  - `slug` (string) - Slug SEO của bài viết, ví dụ: `workshop-react-2026`
- **Query Parameters:** Không có
- **Request Body:** Không có

## 4. Response

- **Success (200):**

  ```json
  {
    "status": true,
    "message": "Article retrieved successfully",
    "body": {
      "id": 1,
      "title": "Workshop React 2026",
      "slug": "workshop-react-2026",
      "thumbnailUrl": "http://localhost:8080/uploads/abc.jpg",
      "content": "<h1>...</h1><p>...</p>",
      "seoTitle": "Workshop React 2026 - Đăng ký ngay",
      "seoDescription": "Landing page giới thiệu workshop...",
      "published": true,
      "publishedAt": "2026-04-17T10:30:00",
      "registrationStatus": "OPEN",
      "registrationLink": "/activities/123"
    }
  }
  ```

- **Error Responses:**
  - `404 NOT_FOUND` - Không tìm thấy bài viết theo slug hoặc bài viết chưa publish.

  ```json
  {
    "status": false,
    "message": "Article not found with slug: workshop-react-2026",
    "body": null
  }
  ```

---

## FE Implementation Notes

### A. Routing & Page

- Route gợi ý: `/articles/:slug`
- Khi load page:
  - Call `GET /api/articles/{slug}`
  - Render `title`, `thumbnailUrl` (nếu có), và `content` (HTML)
  - Set SEO:
    - `<title>` = `seoTitle ?? title`
    - `<meta name="description">` = `seoDescription`

### B. Render HTML (content)

- `content` là HTML đã được backend lưu (TEXT).
- FE cần render HTML an toàn:
  - Nếu dùng React: `dangerouslySetInnerHTML` + sanitize (khuyến nghị) trước khi render.
  - Nếu dùng Vue: `v-html` + sanitize (khuyến nghị).

### C. CTA Đăng ký (Dynamic)

Field: `registrationStatus` (enum)

- `UPCOMING`: disable nút, label gợi ý: "Sắp mở đăng ký"
- `OPEN`: enable nút, label: "Đăng ký ngay"
- `FULL`: disable nút, label: "Hết chỗ"
- `CLOSED`: disable nút, label: "Đã đóng đăng ký"

Field: `registrationLink` (string)

- Khi `registrationStatus == OPEN`: click CTA navigate tới `registrationLink`
  - Nếu link là path nội bộ (`/activities/{id}`) thì dùng router push.
  - Nếu link là URL tuyệt đối (`https://...`) thì dùng `window.location.href`.

### D. Trạng thái loading/error

- Loading skeleton cho tiêu đề + thumbnail + body.
- Nếu API trả 404:
  - Hiển thị trang "Không tìm thấy bài viết" hoặc điều hướng về danh sách sự kiện.

### E. Gợi ý typing (TypeScript)

```ts
export type RegistrationCtaStatus = "UPCOMING" | "OPEN" | "FULL" | "CLOSED";

export type EventArticleDetailResponse = {
  id: number;
  title: string;
  slug: string;
  thumbnailUrl?: string | null;
  content: string;
  seoTitle?: string | null;
  seoDescription?: string | null;
  published: boolean;
  publishedAt?: string | null;
  registrationStatus: RegistrationCtaStatus;
  registrationLink: string;
};

export type ApiResponse<T> = {
  status: boolean;
  message: string;
  body: T;
};
```

### F. Ví dụ call API

```ts
export async function getArticleBySlug(slug: string) {
  const res = await fetch(`/api/articles/${encodeURIComponent(slug)}`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return (await res.json()) as ApiResponse<EventArticleDetailResponse>;
}
```

---

## Admin/Manager APIs (CMS)

## 1. Mô tả nghiệp vụ

Admin/Manager tạo và quản lý bài viết quảng bá cho Activity: tạo mới, cập nhật nội dung/SEO, publish/unpublish. Public chỉ xem được khi `published = true`.

## 2. API Endpoint

### 2.1 Tạo bài viết

- **Method:** POST
- **Path:** `/api/admin/articles`
- **Authentication:** Required + Role `ADMIN` hoặc `MANAGER`

### 2.2 Cập nhật bài viết

- **Method:** PUT
- **Path:** `/api/admin/articles/{articleId}`
- **Authentication:** Required + Role `ADMIN` hoặc `MANAGER`

### 2.3 Publish bài viết

- **Method:** PUT
- **Path:** `/api/admin/articles/{articleId}/publish`
- **Authentication:** Required + Role `ADMIN` hoặc `MANAGER`

### 2.4 Unpublish bài viết

- **Method:** PUT
- **Path:** `/api/admin/articles/{articleId}/unpublish`
- **Authentication:** Required + Role `ADMIN` hoặc `MANAGER`

### 2.5 Lấy bài viết theo id

- **Method:** GET
- **Path:** `/api/admin/articles/{articleId}`
- **Authentication:** Required + Role `ADMIN` hoặc `MANAGER`

### 2.6 Lấy bài viết theo activityId

- **Method:** GET
- **Path:** `/api/admin/articles/by-activity/{activityId}`
- **Authentication:** Required + Role `ADMIN` hoặc `MANAGER`

## 3. Request

### 3.1 Request Body (POST/PUT)

```json
{
  "activityId": "number - required khi tạo, không đổi khi update",
  "title": "string - required",
  "slug": "string - required, unique",
  "thumbnailUrl": "string - optional",
  "content": "string - required (HTML)",
  "seoTitle": "string - optional",
  "seoDescription": "string - optional"
}
```

## 4. Response

- **Success (201/200):**

```json
{
  "status": true,
  "message": "Article created/updated/published...",
  "body": {
    "id": 1,
    "activityId": 123,
    "title": "Workshop React 2026",
    "slug": "workshop-react-2026",
    "thumbnailUrl": "http://localhost:8080/uploads/abc.jpg",
    "content": "<h1>...</h1>",
    "seoTitle": "SEO title",
    "seoDescription": "SEO desc",
    "published": true,
    "publishedAt": "2026-04-17T10:30:00",
    "createdAt": "2026-04-17T09:00:00",
    "updatedAt": "2026-04-17T10:40:00"
  }
}
```

- **Error Responses:**
  - `400 BAD_REQUEST` - Thiếu field bắt buộc / slug trùng / activity đã có bài viết.
  - `404 NOT_FOUND` - Không tìm thấy activity hoặc article.
  - `403 FORBIDDEN` - Không đủ quyền (không phải ADMIN/MANAGER).
