# EventArticle APIs + TypeScript DTO (Frontend Contract)

Tài liệu này mô tả API theo hướng API-first và cung cấp TypeScript DTO để Frontend dùng trực tiếp.

Lưu ý: Dự án hiện có 2 kiểu response:
- Một số API trả thẳng DTO (vd: `Page<ArticleListResponse>`, `ArticleDetailResponse`).
- Một số API trả theo wrapper `Response { status, message, body }` (vd: `track-view`, `wishlist`, `waitlist`).

---

## A) TypeScript DTO (khuyến nghị đặt trong `src/types/article.ts`)

```ts
export type ISODateTime = string; // ví dụ: "2026-05-03T02:26:44"

export type RegistrationCtaStatus =
  | "UPCOMING"
  | "OPEN"
  | "WAITLIST"
  | "FULL"
  | "CLOSED";

export type ResponseWrapper<T = unknown> = {
  status: boolean;
  message: string;
  body: T;
};

export type SpringPage<T> = {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: { empty: boolean; sorted: boolean; unsorted: boolean };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort: { empty: boolean; sorted: boolean; unsorted: boolean };
  first: boolean;
  numberOfElements: number;
  empty: boolean;
};

export type ArticleImageResponse = {
  id: number;
  imageUrl: string;
  caption?: string | null;
  displayOrder: number;
  isCover: boolean;
  createdAt?: ISODateTime | null;
};

export type ArticleListResponse = {
  id: number;
  title: string;
  slug: string;
  thumbnailUrl?: string | null;
  seoDescription?: string | null;
  registrationStatus?: string | null; // backend đang trả string
  registrationLink?: string | null;
  isPublished: boolean;
  isFeatured: boolean;
  isPinned: boolean;
  publishedAt?: ISODateTime | null;
  viewCount: number;
  wishlistCount: number;
  categoryName?: string | null;
  tags?: string[] | null;
  images?: ArticleImageResponse[] | null;
};

export type ArticleDetailResponse = {
  id: number;
  title: string;
  slug: string;
  thumbnailUrl?: string | null;
  content: string; // HTML
  seoTitle?: string | null;
  seoDescription?: string | null;
  published: boolean;
  publishedAt?: ISODateTime | null;
  registrationStatus?: string | null; // backend đang trả string
  registrationLink?: string | null;
  viewCount: number;
  wishlistCount: number;
  isFeatured: boolean;
  isPinned: boolean;
  priority: number;
  createdAt?: ISODateTime | null;
  updatedAt?: ISODateTime | null;
  activityInfo?: {
    id: number;
    name: string;
    location?: string | null;
    startDate?: ISODateTime | null;
    endDate?: ISODateTime | null;
    registrationStartDate?: ISODateTime | null;
    registrationDeadline?: ISODateTime | null;
  } | null;
  category?: { id: number; name: string; slug?: string | null } | null;
  tags?: string[] | null;
  images?: ArticleImageResponse[] | null;
  coverImages?: ArticleImageResponse[] | null;
  isWishlisted: boolean;
};

export type ArticleWishlistItemResponse = {
  id: number;
  articleId: number;
  title: string;
  slug: string;
  thumbnailUrl?: string | null;
  seoDescription?: string | null;
  isPublished: boolean;
  publishedAt?: ISODateTime | null;
  registrationStatus?: string | null;
  wishlistedAt?: ISODateTime | null;
};

export type EventArticleUpsertRequest = {
  activityId?: number; // required khi tạo
  title: string;
  slug: string;
  thumbnailUrl?: string | null;
  content: string; // HTML
  seoTitle?: string | null;
  seoDescription?: string | null;
  categoryId?: number | null;
  tagIds?: number[] | null;
  isFeatured: boolean;
  isPinned: boolean;
  priority: number;
};

export type EventArticleAdminResponse = {
  id: number;
  activityId: number;
  title: string;
  slug: string;
  thumbnailUrl?: string | null;
  content: string;
  seoTitle?: string | null;
  seoDescription?: string | null;
  published: boolean;
  publishedAt?: ISODateTime | null;
  viewCount: number;
  wishlistCount: number;
  featured: boolean;
  pinned: boolean;
  priority: number;
  categoryId?: number | null;
  categoryName?: string | null;
  tagNames?: string[] | null;
  createdAt?: ISODateTime | null;
  updatedAt?: ISODateTime | null;
};

export type ArticleCategoryRequest = {
  name: string;
  description?: string | null;
  slug?: string | null;
  displayOrder: number;
  isActive: boolean;
};

export type ArticleCategoryResponse = {
  id: number;
  name: string;
  description?: string | null;
  slug?: string | null;
  displayOrder: number;
  isActive: boolean;
  createdAt?: ISODateTime | null;
};

export type ArticleTagRequest = {
  name: string;
  slug?: string | null;
  isActive: boolean;
};

export type ArticleTagResponse = {
  id: number;
  name: string;
  slug?: string | null;
  isActive: boolean;
  createdAt?: ISODateTime | null;
};

export type ArticleImageRequest = {
  imageUrl: string;
  caption?: string | null;
  displayOrder: number;
  isCover: boolean;
};

export type ArticleStatisticsResponse = {
  totalArticles: number;
  publishedArticles: number;
  draftArticles: number;
  totalViews: number;
  totalWishlists: number;
  featuredArticles: number;
  pinnedArticles: number;
  topViewedArticles?: Record<string, unknown>[] | null;
  recentlyPublished?: Record<string, unknown>[] | null;
  articlesByCategory?: Record<string, number> | null;
  articlesByMonth?: Record<string, number> | null;
};
```

---

## B) Student APIs

### 1) Danh sách bài viết (phân trang)

#### 1. Mô tả nghiệp vụ
Hiển thị danh sách bài viết public (đã publish), ưu tiên bài ghim và độ ưu tiên.

#### 2. API Endpoint
- **Method:** GET
- **Path:** `/api/articles`
- **Authentication:** Not required

#### 3. Request
- **Query Parameters:**
  - `page` (number, default `0`) - Trang
  - `size` (number, default `10`) - Kích thước trang

#### 4. Response
- **Success (200):** `SpringPage<ArticleListResponse>`
- **Error Responses:** `400 BAD_REQUEST` (tham số không hợp lệ)

---

### 2) Danh sách featured

#### 1. Mô tả nghiệp vụ
Hiển thị các bài viết được đánh dấu nổi bật.

#### 2. API Endpoint
- **Method:** GET
- **Path:** `/api/articles/featured`
- **Authentication:** Not required

#### 3. Request
Không có

#### 4. Response
- **Success (200):** `ArticleListResponse[]`

---

### 3) Lọc theo danh mục

#### 1. Mô tả nghiệp vụ
Hiển thị bài viết theo danh mục.

#### 2. API Endpoint
- **Method:** GET
- **Path:** `/api/articles/category/{categorySlug}`
- **Authentication:** Not required

#### 3. Request
- **Path Parameters:**
  - `categorySlug` (string) - slug của danh mục
- **Query Parameters:**
  - `page` (number, default `0`)
  - `size` (number, default `10`)

#### 4. Response
- **Success (200):** `SpringPage<ArticleListResponse>`
- **Error Responses:** `404 NOT_FOUND` (không tìm thấy danh mục)

---

### 4) Tìm kiếm bài viết

#### 1. Mô tả nghiệp vụ
Tìm bài viết public theo từ khóa (title/content).

#### 2. API Endpoint
- **Method:** GET
- **Path:** `/api/articles/search`
- **Authentication:** Not required

#### 3. Request
- **Query Parameters:**
  - `keyword` (string, required) - từ khóa
  - `page` (number, default `0`)
  - `size` (number, default `10`)

#### 4. Response
- **Success (200):** `SpringPage<ArticleListResponse>`

---

### 5) Chi tiết bài viết theo slug

#### 1. Mô tả nghiệp vụ
Trả chi tiết bài viết (HTML content), CTA đăng ký động, gallery ảnh, trạng thái wishlist (nếu đã đăng nhập).

#### 2. API Endpoint
- **Method:** GET
- **Path:** `/api/articles/{slug}`
- **Authentication:** Not required (nếu có login sẽ có `isWishlisted`)

#### 3. Request
- **Path Parameters:**
  - `slug` (string) - slug bài viết

#### 4. Response
- **Success (200):** `ArticleDetailResponse`
- **Error Responses:** `404 NOT_FOUND` (không tìm thấy hoặc chưa publish)

---

### 6) Related articles

#### 1. Mô tả nghiệp vụ
Gợi ý bài viết liên quan để giữ chân người dùng.

#### 2. API Endpoint
- **Method:** GET
- **Path:** `/api/articles/{slug}/related`
- **Authentication:** Not required

#### 3. Request
- **Path Parameters:**
  - `slug` (string)
- **Query Parameters:**
  - `limit` (number, default `3`)

#### 4. Response
- **Success (200):** `ArticleListResponse[]`

---

### 7) Add to Calendar (ICS)

#### 1. Mô tả nghiệp vụ
Tải file `.ics` để sinh viên thêm sự kiện vào lịch cá nhân.

#### 2. API Endpoint
- **Method:** GET
- **Path:** `/api/articles/{slug}/calendar`
- **Authentication:** Not required

#### 3. Request
- **Path Parameters:** `slug` (string)

#### 4. Response
- **Success (200):** file bytes, header `Content-Type: text/calendar`

---

### 8) Track view

#### 1. Mô tả nghiệp vụ
Ghi nhận 1 lượt xem bài viết.

#### 2. API Endpoint
- **Method:** POST
- **Path:** `/api/articles/{slug}/track-view`
- **Authentication:** Not required

#### 3. Request
- **Path Parameters:** `slug` (string)

#### 4. Response
- **Success (200):** `ResponseWrapper<null>`

---

### 9) Waitlist

#### 1. Mô tả nghiệp vụ
Khi CTA trả `WAITLIST`, sinh viên có thể đăng ký danh sách chờ.

#### 2. API Endpoint
- **Method:** POST
- **Path:** `/api/articles/{slug}/waitlist`
- **Authentication:** Required

#### 3. Request
- **Path Parameters:** `slug` (string)

#### 4. Response
- **Success (201):** `ResponseWrapper<null>`
- **Error Responses:** `401 UNAUTHORIZED`, `400 BAD_REQUEST`

---

### 10) Wishlist

#### 1. Mô tả nghiệp vụ
Sinh viên lưu/bỏ lưu bài viết để xem lại.

#### 2. API Endpoint
- **Add**
  - **Method:** POST
  - **Path:** `/api/articles/{slug}/wishlist`
  - **Authentication:** Required
- **Remove**
  - **Method:** DELETE
  - **Path:** `/api/articles/{slug}/wishlist`
  - **Authentication:** Required
- **List**
  - **Method:** GET
  - **Path:** `/api/articles/wishlist`
  - **Authentication:** Required

#### 3. Request
- **Path Parameters:** `slug` (string) cho add/remove
- **Query Parameters:** `page`, `size` cho list

#### 4. Response
- **Add/Remove:**
  - **Success (201/200):** `ResponseWrapper<null>`
  - **Error:** `401`, `400`
- **List:**
  - **Success (200):** `SpringPage<ArticleWishlistItemResponse>`

---

## C) Admin/Manager APIs (CMS)

### 1) Thống kê Dashboard

#### 1. Mô tả nghiệp vụ
Admin/Manager theo dõi tổng quan hiệu quả bài viết (views, wishlist, top viewed...).

#### 2. API Endpoint
- **Method:** GET
- **Path:** `/api/admin/articles/statistics`
- **Authentication:** Required + Role `ADMIN` hoặc `MANAGER`

#### 3. Request
Không có

#### 4. Response
- **Success (200):** `ArticleStatisticsResponse`

---

### 2) Tạo / Cập nhật / Publish / Unpublish bài viết

#### 1. Mô tả nghiệp vụ
Quản trị bài viết tập trung: tạo mới, chỉnh sửa SEO/nội dung, bật/tắt publish.

#### 2. API Endpoint
- **Create**
  - **Method:** POST
  - **Path:** `/api/admin/articles`
  - **Authentication:** Required + Role `ADMIN` hoặc `MANAGER`
- **Update**
  - **Method:** PUT
  - **Path:** `/api/admin/articles/{articleId}`
  - **Authentication:** Required + Role `ADMIN` hoặc `MANAGER`
- **Publish**
  - **Method:** PUT
  - **Path:** `/api/admin/articles/{articleId}/publish`
  - **Authentication:** Required + Role `ADMIN` hoặc `MANAGER`
- **Unpublish**
  - **Method:** PUT
  - **Path:** `/api/admin/articles/{articleId}/unpublish`
  - **Authentication:** Required + Role `ADMIN` hoặc `MANAGER`

#### 3. Request
- **Path Parameters:**
  - `articleId` (number) - id bài viết (update/publish/unpublish)
- **Request Body (create/update):**
  ```json
  {
    "activityId": "number - required khi create",
    "title": "string - required",
    "slug": "string - required, unique",
    "thumbnailUrl": "string - optional",
    "content": "string - required (HTML)",
    "seoTitle": "string - optional",
    "seoDescription": "string - optional",
    "categoryId": "number - optional",
    "tagIds": "number[] - optional",
    "isFeatured": "boolean",
    "isPinned": "boolean",
    "priority": "number"
  }
  ```

#### 4. Response
- **Success (200/201):** `EventArticleAdminResponse`
- **Error Responses:** `400`, `401`, `403`, `404`

---

### 3) Quản lý ảnh bài viết (Gallery)

#### 1. Mô tả nghiệp vụ
Admin/Manager gắn nhiều ảnh vào bài viết; ảnh có caption, thứ tự, cover.

#### 2. API Endpoint
- **Add image**
  - **Method:** POST
  - **Path:** `/api/admin/articles/{articleId}/images`
  - **Authentication:** Required + Role `ADMIN` hoặc `MANAGER`
- **Remove image**
  - **Method:** DELETE
  - **Path:** `/api/admin/articles/{articleId}/images/{imageId}`
  - **Authentication:** Required + Role `ADMIN` hoặc `MANAGER`

#### 3. Request
- **Path Parameters:** `articleId` (number), `imageId` (number)
- **Request Body (add):**
  ```json
  {
    "imageUrl": "string - required",
    "caption": "string - optional",
    "displayOrder": "number - default 0",
    "isCover": "boolean"
  }
  ```

#### 4. Response
- **Add success (201):** `ArticleImageResponse`
- **Remove success (204):** no content

---

### 4) Categories/Tags (CMS)

#### 1. Mô tả nghiệp vụ
Admin/Manager quản trị taxonomy để FE filter & gợi ý nội dung.

#### 2. API Endpoint
- **Categories**
  - GET `/api/admin/articles/categories`
  - POST `/api/admin/articles/categories`
  - PUT `/api/admin/articles/categories/{categoryId}`
  - DELETE `/api/admin/articles/categories/{categoryId}`
- **Tags**
  - GET `/api/admin/articles/tags`
  - POST `/api/admin/articles/tags`
  - DELETE `/api/admin/articles/tags/{tagId}`

#### 3. Request
- **Category body (POST/PUT):** `ArticleCategoryRequest`
- **Tag body (POST):** `ArticleTagRequest`

#### 4. Response
- **Categories**
  - GET: `ArticleCategoryResponse[]`
  - POST/PUT: `ArticleCategoryResponse`
  - DELETE: 204
- **Tags**
  - GET: `ArticleTagResponse[]`
  - POST: `ArticleTagResponse`
  - DELETE: 204

