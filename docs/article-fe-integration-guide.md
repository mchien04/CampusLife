# Hướng Dẫn Tích Hợp Module Article — Frontend (TypeScript)

> Tài liệu này dành cho team Frontend tích hợp đầy đủ module **Bài viết Sự kiện (EventArticle)** với Backend CampusLife (Spring Boot). Bao gồm toàn bộ TypeScript DTO, API mapping, cách render nội dung HTML và hướng dẫn tích hợp trình soạn thảo.

> **Delta cập nhật gần đây (view count, flags, stats, lọc cate/tag, personal-calendar):** xem [fe-article-and-calendar-update-guide.md](./fe-article-and-calendar-update-guide.md).

---

## Mục Lục

1. [Enums](#1-enums)
2. [TypeScript DTOs](#2-typescript-dtos)
3. [Cấu Trúc Phân Trang (Spring Page)](#3-cấu-trúc-phân-trang-spring-page)
4. [API Endpoints & Service Functions](#4-api-endpoints--service-functions)
5. [Render Nội Dung HTML An Toàn](#5-render-nội-dung-html-an-toàn)
6. [Tích Hợp Trình Soạn Thảo (Tiptap)](#6-tích-hợp-trình-soạn-thảo-tiptap)
7. [Xử Lý Bình Luận (Comment Tree)](#7-xử-lý-bình-luận-comment-tree)
8. [Lưu Ý Bảo Mật & XSS](#8-lưu-ý-bảo-mật--xss)

---

## 1. Enums

```typescript
export enum ArticleType {
  ANNOUNCEMENT = 'ANNOUNCEMENT',   // Thông báo / Bài đăng chính
  RECAP = 'RECAP',                 // Tổng kết sau sự kiện
  BEHIND_SCENE = 'BEHIND_SCENE',   // Hậu trường, chuẩn bị
  RESULT = 'RESULT',               // Kết quả, vinh danh, giải thưởng
  UPDATE = 'UPDATE',               // Cập nhật thông tin
}

export enum ReactionType {
  LIKE = 'LIKE',       // 👍
  LOVE = 'LOVE',       // ❤️
  CLAP = 'CLAP',       // 👏
  FIRE = 'FIRE',       // 🔥
  SUPPORT = 'SUPPORT', // 💪
}

export enum RegistrationCtaStatus {
  UPCOMING = 'UPCOMING',   // Sắp mở đăng ký
  OPEN = 'OPEN',           // Đang mở đăng ký
  WAITLIST = 'WAITLIST',   // Hết chỗ (chờ)
  FULL = 'FULL',           // Đã đầy
  CLOSED = 'CLOSED',       // Đã đóng đăng ký
}
```

---

## 2. TypeScript DTOs

### 2.1. Article (Core)

```typescript
export interface ArticleListResponse {
  id: number;
  title: string;
  slug: string;
  thumbnailUrl?: string;
  seoDescription?: string;
  registrationStatus?: RegistrationCtaStatus;
  activityId?: number;
  shareLink?: string;
  articleType: ArticleType;
  isPrimary: boolean;
  isPublished: boolean;
  isFeatured: boolean;
  isPinned: boolean;
  publishedAt?: string; // ISO 8601
  viewCount?: number;
  wishlistCount?: number;
  categoryName?: string;
  tags?: string[];
  images?: ArticleImageResponse[];
  commentCount?: number;
}

export interface ArticleDetailResponse {
  myReaction?: ReactionType;
  id: number;
  title: string;
  slug: string;
  thumbnailUrl?: string;
  content: string; // HTML string
  seoTitle?: string;
  seoDescription?: string;
  published: boolean;
  publishedAt?: string;
  registrationStatus?: RegistrationCtaStatus;
  viewCount?: number;
  wishlistCount?: number;
  isFeatured: boolean;
  isPinned: boolean;
  priority: number;
  createdAt: string;
  updatedAt: string;
  activityInfo?: ArticleActivityInfo;
  category?: ArticleCategoryInfo;
  tags?: string[];
  images?: ArticleImageResponse[];
  coverImages?: ArticleImageResponse[];
  isWishlisted?: boolean;
  redirectedFrom?: string;
  currentSlug?: string;
  commentCount?: number;
}

export interface ArticleActivityInfo {
  id: number;
  name: string;
  location: string;
  startDate: string;
  endDate: string;
  registrationStartDate: string;
  registrationDeadline: string;
  scoreType: string;
  shareLink?: string;
}

export interface ArticleCategoryInfo {
  id: number;
  name: string;
  slug: string;
}

export interface ArticleImageResponse {
  id: number;
  imageUrl: string;
  caption?: string;
  displayOrder: number;
  isCover: boolean;
  createdAt?: string;
}
```

### 2.2. CMS / Admin DTOs

```typescript
export interface EventArticleAdminResponse {
  id: number;
  activityId?: number;
  activityName?: string;
  articleType: ArticleType;
  isPrimary: boolean;
  title: string;
  slug: string;
  thumbnailUrl?: string;
  content: string;
  seoTitle?: string;
  seoDescription?: string;
  published: boolean;
  publishedAt?: string;
  viewCount?: number;
  wishlistCount?: number;
  featured: boolean;
  pinned: boolean;
  priority: number;
  categoryId?: number;
  categoryName?: string;
  tagNames?: string[];
  createdAt: string;
  updatedAt: string;
}

export interface EventArticleUpsertRequest {
  activityId?: number | null;
  articleType: ArticleType;
  isPrimary: boolean;
  title: string;
  slug: string;
  thumbnailUrl?: string;
  content: string; // HTML
  seoTitle?: string;
  seoDescription?: string;
  categoryId?: number | null;
  tagIds?: number[];
  isFeatured: boolean;
  isPinned: boolean;
  priority: number;
}
```

### 2.3. Category & Tag

```typescript
export interface ArticleCategoryResponse {
  id: number;
  name: string;
  description?: string;
  slug: string;
  displayOrder: number;
  isActive: boolean;
  createdAt: string;
}

export interface ArticleCategoryRequest {
  name: string;
  description?: string;
  slug: string;
  displayOrder: number;
  isActive: boolean;
}

export interface ArticleTagResponse {
  id: number;
  name: string;
  slug: string;
  isActive: boolean;
  createdAt: string;
}

export interface ArticleTagRequest {
  name: string;
  slug: string;
  isActive: boolean;
}
```

### 2.4. Image Gallery

```typescript
export interface ArticleImageRequest {
  imageUrl: string;
  caption?: string;
  displayOrder: number;
  isCover: boolean;
}

// ArticleImageResponse đã định nghĩa ở 2.1
```

### 2.5. Interaction (Reaction / Wishlist / History)

```typescript
export interface ArticleWishlistItemResponse {
  id: number;
  articleId: number;
  title: string;
  slug: string;
  thumbnailUrl?: string;
  seoDescription?: string;
  isPublished: boolean;
  publishedAt?: string;
  registrationStatus?: RegistrationCtaStatus;
  wishlistedAt: string;
}

export interface ArticleHistoryResponse {
  id: number;
  articleId: number;
  title: string;
  slug: string;
  thumbnailUrl?: string;
  seoDescription?: string;
  isPublished: boolean;
  publishedAt?: string;
  registrationStatus?: RegistrationCtaStatus;
  viewedAt: string;
}

export type ReactionCounts = Record<ReactionType, number>;
```

### 2.6. Comment

```typescript
export interface ArticleCommentRequest {
  content: string;
  parentCommentId?: number | null;
}

export interface ArticleCommentResponse {
  id: number;
  articleId: number;
  parentCommentId?: number;
  content: string;
  isFlagged: boolean;
  flagReason?: string;
  isHidden: boolean;
  isAutoHidden: boolean;
  isEdited: boolean;
  editedAt?: string;
  student: StudentBasicInfo;
  replies: ArticleCommentResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface StudentBasicInfo {
  id: number;
  fullName: string;
  studentCode: string;
  avatarUrl?: string;
  departmentName?: string;
  className?: string;
}
```

### 2.7. Statistics

```typescript
export interface ArticleStatisticsResponse {
  totalArticles: number;
  publishedArticles: number;
  draftArticles: number;
  totalViews: number;
  totalWishlists: number;
  featuredArticles: number;
  pinnedArticles: number;
  topViewedArticles: Array<Record<string, unknown>>;
  recentlyPublished: Array<Record<string, unknown>>;
  articlesByCategory: Record<string, number>;
  articlesByMonth: Record<string, number>;
}
```

### 2.8. Global Response Wrapper

```typescript
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// Hoặc dạng đơn giản hơn dùng cho mutation
export interface SimpleResponse {
  status: boolean;
  message: string;
  body?: unknown;
}
```

---

## 3. Cấu Trúc Phân Trang (Spring Page)

Backend dùng `org.springframework.data.domain.Page<T>`. FE cần định nghĩa wrapper:

```typescript
export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number; // trang hiện tại (0-based)
  sort: {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
  };
  numberOfElements: number;
  empty: boolean;
}

// Helper kiểm tra còn trang tiếp theo
export const hasNextPage = <T>(page: PageResponse<T>): boolean =>
  !page.last && page.number + 1 < page.totalPages;
```

---

## 4. API Endpoints & Service Functions

> Giả định sử dụng **Axios** với base URL `/api`. Nếu dùng `fetch`, chỉ cần thay đổi cú pháp gọi network.

```typescript
import axios, { AxiosResponse } from 'axios';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Gắn token nếu có
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

### 4.1. Public APIs — Không cần Auth

```typescript
// GET /api/articles?page={page}&size={size}
export const getPublishedArticles = (
  page = 0,
  size = 10
): Promise<AxiosResponse<ApiResponse<PageResponse<ArticleListResponse>>>> =>
  api.get('/articles', { params: { page, size } });

// GET /api/articles/featured?limit={limit}
export const getFeaturedArticles = (
  limit = 5
): Promise<AxiosResponse<ApiResponse<ArticleListResponse[]>>> =>
  api.get('/articles/featured', { params: { limit } });

// GET /api/articles/category/{categorySlug}
export const getArticlesByCategory = (
  categorySlug: string,
  page = 0,
  size = 10
): Promise<AxiosResponse<ApiResponse<PageResponse<ArticleListResponse>>>> =>
  api.get(`/articles/category/${categorySlug}`, { params: { page, size } });

// GET /api/articles/search?keyword={keyword}&page={page}&size={size}
export const searchArticles = (
  keyword: string,
  page = 0,
  size = 10
): Promise<AxiosResponse<ApiResponse<PageResponse<ArticleListResponse>>>> =>
  api.get('/articles/search', { params: { keyword, page, size } });

// GET /api/articles/tag/{tagSlug}
export const getArticlesByTag = (
  tagSlug: string,
  page = 0,
  size = 10
): Promise<AxiosResponse<ApiResponse<PageResponse<ArticleListResponse>>>> =>
  api.get(`/articles/tag/${tagSlug}`, { params: { page, size } });

// GET /api/articles/{slug}
export const getArticleDetail = (
  slug: string
): Promise<AxiosResponse<ApiResponse<ArticleDetailResponse>>> =>
  api.get(`/articles/${slug}`);

// GET /api/articles/{slug}/related?limit={limit}
export const getRelatedArticles = (
  slug: string,
  limit = 3
): Promise<AxiosResponse<ApiResponse<ArticleListResponse[]>>> =>
  api.get(`/articles/${slug}/related`, { params: { limit } });

// GET /api/articles/{slug}/calendar
export const getCalendarFile = (slug: string): Promise<AxiosResponse<Blob>> =>
  api.get(`/articles/${slug}/calendar`, { responseType: 'blob' });

// POST /api/articles/{slug}/track-view
export const trackArticleView = (
  slug: string
): Promise<AxiosResponse<ApiResponse<null>>> =>
  api.post(`/articles/${slug}/track-view`);

// GET /api/articles/series/{seriesId}
export const getArticlesBySeries = (
  seriesId: number
): Promise<AxiosResponse<ApiResponse<ArticleListResponse[]>>> =>
  api.get(`/articles/series/${seriesId}`);

// GET /api/articles/trending?days={days}&limit={limit}
export const getTrendingArticles = (
  days = 7,
  limit = 5
): Promise<AxiosResponse<ApiResponse<ArticleListResponse[]>>> =>
  api.get('/articles/trending', { params: { days, limit } });
```

### 4.2. Authenticated APIs — STUDENT

```typescript
// POST /api/articles/{slug}/wishlist
export const addToWishlist = (
  slug: string
): Promise<AxiosResponse<ApiResponse<null>>> =>
  api.post(`/articles/${slug}/wishlist`);

// DELETE /api/articles/{slug}/wishlist
export const removeFromWishlist = (
  slug: string
): Promise<AxiosResponse<ApiResponse<null>>> =>
  api.delete(`/articles/${slug}/wishlist`);

// GET /api/articles/wishlist
export const getMyWishlist = (
  page = 0,
  size = 10
): Promise<AxiosResponse<ApiResponse<PageResponse<ArticleWishlistItemResponse>>>> =>
  api.get('/articles/wishlist', { params: { page, size } });

// POST /api/articles/{slug}/waitlist
export const registerWaitlist = (
  slug: string
): Promise<AxiosResponse<ApiResponse<null>>> =>
  api.post(`/articles/${slug}/waitlist`);

// POST /api/articles/{slug}/reaction?type={type}
export const addReaction = (
  slug: string,
  type: ReactionType
): Promise<AxiosResponse<ApiResponse<null>>> =>
  api.post(`/articles/${slug}/reaction`, null, { params: { type } });

// DELETE /api/articles/{slug}/reaction
export const removeReaction = (
  slug: string
): Promise<AxiosResponse<ApiResponse<null>>> =>
  api.delete(`/articles/${slug}/reaction`);

// GET /api/articles/{slug}/reactions
export const getReactionCounts = (
  slug: string
): Promise<AxiosResponse<ApiResponse<ReactionCounts>>> =>
  api.get(`/articles/${slug}/reactions`);

// POST /api/articles/{slug}/track-share
export const trackShare = (
  slug: string
): Promise<AxiosResponse<ApiResponse<null>>> =>
  api.post(`/articles/${slug}/track-share`);

// GET /api/articles/history
export const getReadingHistory = (
  page = 0,
  size = 10
): Promise<AxiosResponse<ApiResponse<PageResponse<ArticleHistoryResponse>>>> =>
  api.get('/articles/history', { params: { page, size } });

// DELETE /api/articles/history/{historyId}
export const deleteReadingHistoryItem = (
  historyId: number
): Promise<AxiosResponse<ApiResponse<null>>> =>
  api.delete(`/articles/history/${historyId}`);

// DELETE /api/articles/history
export const clearAllReadingHistory = (): Promise<AxiosResponse<ApiResponse<null>>> =>
  api.delete('/articles/history');
```

### 4.3. Comment APIs

```typescript
// GET /api/articles/{slug}/comments?page={page}&size={size}
export const getArticleComments = (
  slug: string,
  page = 0,
  size = 10
): Promise<AxiosResponse<ApiResponse<PageResponse<ArticleCommentResponse>>>> =>
  api.get(`/articles/${slug}/comments`, { params: { page, size } });

// POST /api/articles/{slug}/comments
export const postComment = (
  slug: string,
  payload: ArticleCommentRequest
): Promise<AxiosResponse<ApiResponse<ArticleCommentResponse>>> =>
  api.post(`/articles/${slug}/comments`, payload);

// DELETE /api/articles/comments/{commentId}
export const deleteMyComment = (
  commentId: number
): Promise<AxiosResponse<ApiResponse<null>>> =>
  api.delete(`/articles/comments/${commentId}`);
```

### 4.4. Admin / Manager APIs

```typescript
// GET /api/admin/articles?status=&activityId=&categoryId=&articleType=&featured=&pinned=&primary=&search=&dateFrom=&dateTo=&page=&size=
export const getAdminArticles = (params: {
  status?: 'PUBLISHED' | 'DRAFT' | 'ALL';
  activityId?: number;
  categoryId?: number;
  articleType?: ArticleType;
  featured?: boolean;
  pinned?: boolean;
  primary?: boolean;
  search?: string;
  dateFrom?: string; // yyyy-MM-dd
  dateTo?: string;
  page?: number;
  size?: number;
}): Promise<AxiosResponse<ApiResponse<PageResponse<EventArticleAdminResponse>>>> =>
  api.get('/admin/articles', { params });

// GET /api/admin/articles/{articleId}
export const getAdminArticleById = (
  articleId: number
): Promise<AxiosResponse<ApiResponse<EventArticleAdminResponse>>> =>
  api.get(`/admin/articles/${articleId}`);

// GET /api/admin/articles/by-activity/{activityId}
export const getArticlesByActivityId = (
  activityId: number
): Promise<AxiosResponse<ApiResponse<EventArticleAdminResponse[]>>> =>
  api.get(`/admin/articles/by-activity/${activityId}`);

// POST /api/admin/articles
export const createArticle = (
  payload: EventArticleUpsertRequest
): Promise<AxiosResponse<ApiResponse<EventArticleAdminResponse>>> =>
  api.post('/admin/articles', payload);

// PUT /api/admin/articles/{articleId}
export const updateArticle = (
  articleId: number,
  payload: EventArticleUpsertRequest
): Promise<AxiosResponse<ApiResponse<EventArticleAdminResponse>>> =>
  api.put(`/admin/articles/${articleId}`, payload);

// PUT /api/admin/articles/{articleId}/publish
export const publishArticle = (
  articleId: number
): Promise<AxiosResponse<ApiResponse<EventArticleAdminResponse>>> =>
  api.put(`/admin/articles/${articleId}/publish`);

// PUT /api/admin/articles/{articleId}/unpublish
export const unpublishArticle = (
  articleId: number
): Promise<AxiosResponse<ApiResponse<EventArticleAdminResponse>>> =>
  api.put(`/admin/articles/${articleId}/unpublish`);

// PUT /api/admin/articles/{articleId}/set-primary
export const setPrimaryArticle = (
  articleId: number
): Promise<AxiosResponse<ApiResponse<EventArticleAdminResponse>>> =>
  api.put(`/admin/articles/${articleId}/set-primary`);

// GET /api/admin/articles/statistics
export const getArticleStatistics = (): Promise<
  AxiosResponse<ApiResponse<ArticleStatisticsResponse>>
> => api.get('/admin/articles/statistics');

// GET /api/admin/articles/export?... (tương tự filter params)
export const exportArticlesExcel = (params: {
  status?: string;
  activityId?: number;
  categoryId?: number;
  articleType?: ArticleType;
  featured?: boolean;
  pinned?: boolean;
  primary?: boolean;
  search?: string;
  dateFrom?: string;
  dateTo?: string;
}): Promise<AxiosResponse<Blob>> =>
  api.get('/admin/articles/export', { params, responseType: 'blob' });
```

### 4.5. Admin — Category & Tag

```typescript
// GET /api/admin/articles/categories
export const getAllCategories = (): Promise<
  AxiosResponse<ApiResponse<ArticleCategoryResponse[]>>
> => api.get('/admin/articles/categories');

// POST /api/admin/articles/categories
export const createCategory = (
  payload: ArticleCategoryRequest
): Promise<AxiosResponse<ApiResponse<ArticleCategoryResponse>>> =>
  api.post('/admin/articles/categories', payload);

// PUT /api/admin/articles/categories/{categoryId}
export const updateCategory = (
  categoryId: number,
  payload: ArticleCategoryRequest
): Promise<AxiosResponse<ApiResponse<ArticleCategoryResponse>>> =>
  api.put(`/admin/articles/categories/${categoryId}`, payload);

// DELETE /api/admin/articles/categories/{categoryId}
export const deleteCategory = (
  categoryId: number
): Promise<AxiosResponse<ApiResponse<null>>> =>
  api.delete(`/admin/articles/categories/${categoryId}`);

// GET /api/admin/articles/tags
export const getAllTags = (): Promise<
  AxiosResponse<ApiResponse<ArticleTagResponse[]>>
> => api.get('/admin/articles/tags');

// POST /api/admin/articles/tags
export const createTag = (
  payload: ArticleTagRequest
): Promise<AxiosResponse<ApiResponse<ArticleTagResponse>>> =>
  api.post('/admin/articles/tags', payload);

// PUT /api/admin/articles/tags/{tagId}
export const updateTag = (
  tagId: number,
  payload: ArticleTagRequest
): Promise<AxiosResponse<ApiResponse<ArticleTagResponse>>> =>
  api.put(`/admin/articles/tags/${tagId}`, payload);

// DELETE /api/admin/articles/tags/{tagId}
export const deleteTag = (
  tagId: number
): Promise<AxiosResponse<ApiResponse<null>>> =>
  api.delete(`/admin/articles/tags/${tagId}`);
```

### 4.6. Admin — Image Gallery

```typescript
// POST /api/admin/articles/{articleId}/images
export const addArticleImage = (
  articleId: number,
  payload: ArticleImageRequest
): Promise<AxiosResponse<ApiResponse<ArticleImageResponse>>> =>
  api.post(`/admin/articles/${articleId}/images`, payload);

// DELETE /api/admin/articles/{articleId}/images/{imageId}
export const removeArticleImage = (
  articleId: number,
  imageId: number
): Promise<AxiosResponse<ApiResponse<null>>> =>
  api.delete(`/admin/articles/${articleId}/images/${imageId}`);
```

### 4.7. Admin — Comment Moderation

```typescript
// GET /api/admin/articles/{articleId}/comments
export const getAdminArticleComments = (
  articleId: number,
  page = 0,
  size = 10
): Promise<AxiosResponse<ApiResponse<PageResponse<ArticleCommentResponse>>>> =>
  api.get(`/admin/articles/${articleId}/comments`, { params: { page, size } });

// PUT /api/admin/articles/comments/{commentId}/hide
export const hideComment = (
  commentId: number
): Promise<AxiosResponse<ApiResponse<ArticleCommentResponse>>> =>
  api.put(`/admin/articles/comments/${commentId}/hide`);

// PUT /api/admin/articles/comments/{commentId}/unhide
export const unhideComment = (
  commentId: number
): Promise<AxiosResponse<ApiResponse<ArticleCommentResponse>>> =>
  api.put(`/admin/articles/comments/${commentId}/unhide`);

// DELETE /api/admin/articles/comments/{commentId}
export const adminDeleteComment = (
  commentId: number
): Promise<AxiosResponse<ApiResponse<null>>> =>
  api.delete(`/admin/articles/comments/${commentId}`);
```

---

## 5. Render Nội Dung HTML An Toàn

Backend trả `content` là chuỗi HTML đầy đủ. **Tuyệt đối không** dùng `dangerouslySetInnerHTML` trực tiếp nếu không qua bước sanitize.

### 5.1. React + DOMPurify (Khuyến nghị)

```bash
npm install dompurify
npm install -D @types/dompurify
```

```typescript
import DOMPurify from 'dompurify';

interface ArticleContentProps {
  html: string;
}

export const ArticleContent: React.FC<ArticleContentProps> = ({ html }) => {
  const clean = DOMPurify.sanitize(html, {
    USE_PROFILES: { html: true },
    ALLOWED_TAGS: [
      'p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'strike',
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'ul', 'ol', 'li',
      'blockquote',
      'a', 'img',
      'table', 'thead', 'tbody', 'tr', 'th', 'td',
      'figure', 'figcaption', 'iframe', 'div', 'span',
    ],
    ALLOWED_ATTR: [
      'href', 'target', 'rel', 'src', 'alt', 'title',
      'width', 'height', 'class', 'style',
      'allow', 'allowfullscreen', 'frameborder', 'scrolling',
    ],
  });

  return <div className="article-body" dangerouslySetInnerHTML={{ __html: clean }} />;
};
```

### 5.2. Vue 3

```vue
<template>
  <div class="article-body" v-html="sanitizedContent" />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import DOMPurify from 'dompurify';

const props = defineProps<{ html: string }>();

const sanitizedContent = computed(() =>
  DOMPurify.sanitize(props.html, {
    ALLOWED_TAGS: ['p', 'br', 'strong', 'b', 'em', 'i', 'u', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'ul', 'ol', 'li', 'blockquote', 'a', 'img', 'table', 'thead', 'tbody', 'tr', 'th', 'td', 'figure', 'figcaption', 'iframe', 'div', 'span'],
    ALLOWED_ATTR: ['href', 'target', 'rel', 'src', 'alt', 'title', 'width', 'height', 'class', 'style', 'allow', 'allowfullscreen', 'frameborder', 'scrolling'],
  })
);
</script>
```

### 5.3. Tailwind / CSS gợi ý cho nội dung HTML

```css
.article-body h2 { @apply text-2xl font-bold mt-6 mb-3; }
.article-body h3 { @apply text-xl font-semibold mt-5 mb-2; }
.article-body p  { @apply mb-4 leading-relaxed; }
.article-body ul { @apply list-disc pl-6 mb-4; }
.article-body ol { @apply list-decimal pl-6 mb-4; }
.article-body blockquote { @apply border-l-4 border-gray-300 pl-4 italic text-gray-600; }
.article-body table { @apply w-full border-collapse mb-4; }
.article-body th, .article-body td { @apply border px-3 py-2; }
.article-body iframe { @apply w-full aspect-video rounded-lg; }
```

---

## 6. Tích Hợp Trình Soạn Thảo (Tiptap)

Backend lưu `content` dạng HTML. Khuyến nghị dùng **Tiptap** để soạn thảo.

### 6.1. Cài đặt dependencies

```bash
npm install @tiptap/react @tiptap/pm @tiptap/starter-kit
npm install @tiptap/extension-table @tiptap/extension-table-row @tiptap/extension-table-cell @tiptap/extension-table-header
npm install @tiptap/extension-image @tiptap/extension-link @tiptap/extension-youtube @tiptap/extension-placeholder
npm install @tiptap/extension-underline @tiptap/extension-text-align @tiptap/extension-highlight
```

### 6.2. Editor Component (React)

```typescript
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Table from '@tiptap/extension-table';
import TableRow from '@tiptap/extension-table-row';
import TableCell from '@tiptap/extension-table-cell';
import TableHeader from '@tiptap/extension-table-header';
import Image from '@tiptap/extension-image';
import Link from '@tiptap/extension-link';
import Youtube from '@tiptap/extension-youtube';
import Underline from '@tiptap/extension-underline';
import TextAlign from '@tiptap/extension-text-align';
import Placeholder from '@tiptap/extension-placeholder';

interface ArticleEditorProps {
  initialContent?: string; // HTML
  onChange: (html: string) => void;
}

export const ArticleEditor: React.FC<ArticleEditorProps> = ({
  initialContent = '',
  onChange,
}) => {
  const editor = useEditor({
    extensions: [
      StarterKit,
      Table.configure({ resizable: true }),
      TableRow,
      TableHeader,
      TableCell,
      Image.configure({ allowBase64: false }),
      Link.configure({ openOnClick: false }),
      Youtube.configure({
        width: 640,
        height: 360,
        HTMLAttributes: { class: 'rounded-lg aspect-video w-full' },
      }),
      Underline,
      TextAlign.configure({ types: ['heading', 'paragraph'] }),
      Placeholder.configure({ placeholder: 'Viết nội dung bài viết…' }),
    ],
    content: initialContent,
    onUpdate: ({ editor }) => {
      onChange(editor.getHTML());
    },
  });

  if (!editor) return null;

  return (
    <div className="article-editor border rounded-lg p-4">
      {/* Toolbar */}
      <div className="toolbar flex flex-wrap gap-2 mb-3 pb-3 border-b">
        <button
          onClick={() => editor.chain().focus().toggleBold().run()}
          className={editor.isActive('bold') ? 'active' : ''}
        >
          Bold
        </button>
        <button
          onClick={() => editor.chain().focus().toggleItalic().run()}
          className={editor.isActive('italic') ? 'active' : ''}
        >
          Italic
        </button>
        <button
          onClick={() => editor.chain().focus().toggleUnderline().run()}
          className={editor.isActive('underline') ? 'active' : ''}
        >
          Underline
        </button>
        <button
          onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
          className={editor.isActive('heading', { level: 2 }) ? 'active' : ''}
        >
          H2
        </button>
        <button
          onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}
          className={editor.isActive('heading', { level: 3 }) ? 'active' : ''}
        >
          H3
        </button>
        <button
          onClick={() => editor.chain().focus().toggleBulletList().run()}
          className={editor.isActive('bulletList') ? 'active' : ''}
        >
          Bullet
        </button>
        <button
          onClick={() => editor.chain().focus().toggleOrderedList().run()}
          className={editor.isActive('orderedList') ? 'active' : ''}
        >
          Number
        </button>
        <button
          onClick={() => {
            const url = window.prompt('Nhập URL ảnh');
            if (url) editor.chain().focus().setImage({ src: url }).run();
          }}
        >
          Image
        </button>
        <button
          onClick={() => {
            const url = window.prompt('Nhập URL');
            if (url) editor.chain().focus().setLink({ href: url }).run();
          }}
        >
          Link
        </button>
        <button
          onClick={() => {
            const url = window.prompt('Nhập ID video YouTube');
            if (url) editor.chain().focus().setYoutubeVideo({ src: url }).run();
          }}
        >
          YouTube
        </button>
        <button
          onClick={() =>
            editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
          }
        >
          Table
        </button>
      </div>

      <EditorContent editor={editor} className="min-h-[300px] prose max-w-none" />
    </div>
  );
};
```

### 6.3. Form tạo bài viết

```typescript
import { useState } from 'react';
import { ArticleEditor } from './ArticleEditor';
import { createArticle } from './articleService';
import type { EventArticleUpsertRequest, ArticleType } from './articleTypes';

export const ArticleCreateForm: React.FC = () => {
  const [form, setForm] = useState<EventArticleUpsertRequest>({
    title: '',
    slug: '',
    content: '',
    articleType: ArticleType.ANNOUNCEMENT,
    isPrimary: false,
    isFeatured: false,
    isPinned: false,
    priority: 0,
  });

  const handleSubmit = async () => {
    const res = await createArticle(form);
    console.log('Created:', res.data.data);
  };

  return (
    <form onSubmit={(e) => { e.preventDefault(); handleSubmit(); }}>
      <input
        value={form.title}
        onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
        placeholder="Tiêu đề"
      />
      <input
        value={form.slug}
        onChange={(e) => setForm((f) => ({ ...f, slug: e.target.value }))}
        placeholder="Slug"
      />
      <ArticleEditor
        initialContent={form.content}
        onChange={(html) => setForm((f) => ({ ...f, content: html }))}
      />
      <button type="submit">Tạo bài viết</button>
    </form>
  );
};
```

---

## 7. Xử Lý Bình Luận (Comment Tree)

Backend trả cây bình luận lồng nhau qua trường `replies: ArticleCommentResponse[]`.

```typescript
interface CommentTreeProps {
  comments: ArticleCommentResponse[];
  onReply: (parentId: number, content: string) => void;
  onDelete: (commentId: number) => void;
}

export const CommentTree: React.FC<CommentTreeProps> = ({
  comments,
  onReply,
  onDelete,
}) => {
  return (
    <div className="space-y-4">
      {comments.map((comment) => (
        <CommentNode
          key={comment.id}
          comment={comment}
          onReply={onReply}
          onDelete={onDelete}
        />
      ))}
    </div>
  );
};

const CommentNode: React.FC<{
  comment: ArticleCommentResponse;
  onReply: (parentId: number, content: string) => void;
  onDelete: (commentId: number) => void;
}> = ({ comment, onReply, onDelete }) => {
  return (
    <div className="border-l-2 pl-4 py-2">
      <div className="flex items-center gap-2 mb-1">
        <img src={comment.student.avatarUrl} className="w-8 h-8 rounded-full" />
        <span className="font-semibold">{comment.student.fullName}</span>
        <span className="text-xs text-gray-400">{comment.createdAt}</span>
        {comment.isFlagged && (
          <span className="text-xs text-red-500">🚩 {comment.flagReason}</span>
        )}
      </div>
      <p className="text-gray-800">{comment.content}</p>

      <div className="flex gap-3 mt-2 text-sm text-blue-600">
        <button onClick={() => onReply(comment.id, prompt('Trả lời:') || '')}>
          Trả lời
        </button>
        <button onClick={() => onDelete(comment.id)}>Xóa</button>
      </div>

      {comment.replies && comment.replies.length > 0 && (
        <div className="mt-3">
          <CommentTree
            comments={comment.replies}
            onReply={onReply}
            onDelete={onDelete}
          />
        </div>
      )}
    </div>
  );
};
```

---

## 8. Lưu Ý Bảo Mật & XSS

| Vấn đề | Khuyến nghị |
|--------|-------------|
| **XSS qua `content` HTML** | Luôn sanitize HTML bằng **DOMPurify** trước khi render. Không tin tưởng HTML từ backend tuyệt đối. |
| **Iframe / YouTube** | DOMPurify cần cho phép `iframe` với các attribute `allow`, `allowfullscreen`, `src`. Không cho phép `javascript:` protocol. |
| **Link tự do** | DOMPurify sẽ tự động loại bỏ `javascript:` trong `href`. Kiểm tra thêm `target="_blank"` + `rel="noopener noreferrer"`. |
| **Upload ảnh** | Không nên dùng `allowBase64: true` trong Tiptap vì DB sẽ phình to. Upload ảnh lên server riêng rồi chèn URL. |
| **Slug redirect** | Khi gọi `getArticleDetail(slug)`, nếu API trả `redirectedFrom` != null, FE nên cập nhật URL browser thành `currentSlug` (dùng `history.replaceState` hoặc router navigate). |

---

## Phụ Lục: Tóm Tắt HTTP Status Code Thường Gặp

| Status | Ý nghĩa |
|--------|---------|
| `200` | Thành công (GET, PUT, DELETE) |
| `201` | Tạo thành công (POST) |
| `400` | Bad Request — thiếu trường bắt buộc, dữ liệu không hợp lệ |
| `401` | Unauthorized — chưa đăng nhập / token hết hạn |
| `403` | Forbidden — không đủ quyền (ví dụ: STUDENT gọi API Admin) |
| `404` | Not Found — slug không tồn tại, article chưa publish |

---

> **Phiên bản**: 1.0 | **Cập nhật**: 2026-07-08 | **Backend**: Spring Boot 3.5.x | **FE Stack**: TypeScript / React / Vue (khuyến nghị)
