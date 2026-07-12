# Hướng dẫn FE cập nhật — Article + Personal Calendar

Tài liệu gom các thay đổi / quy ước FE cần chỉnh theo feedback hiện tại:

1. Article: lượt xem bị nhân 3, flag ghim/nổi bật/primary, tag slug, thống kê thích, lọc cate/tag student
2. Lịch: ICS vs lịch trong web (`personal-calendar` breaking change)

Chi tiết API article đầy đủ vẫn ở [article-fe-integration-guide.md](./article-fe-integration-guide.md).  
File này là **delta / checklist cập nhật**.

---

## Mục lục

1. [Article — Lượt xem (fix nhân 3)](#1-article--lượt-xem-fix-nhân-3)
2. [Article — Ghim / Nổi bật / Đại diện chính](#2-article--ghim--nổi-bật--đại-diện-chính)
3. [Article — Tag tự tạo slug](#3-article--tag-tự-tạo-slug)
4. [Article — Thống kê & lượt thích](#4-article--thống-kê--lượt-thích)
5. [Article — Student lọc category / tag](#5-article--student-lọc-category--tag)
6. [Lịch trong web — personal-calendar](#6-lịch-trong-web--personal-calendar)
7. [ICS vẫn giữ](#7-ics-vẫn-giữ)
8. [Checklist tổng](#8-checklist-tổng)

---

## 1. Article — Lượt xem (fix nhân 3)

### Nguyên nhân

BE đếm view ở **hai chỗ**:

| API | Hành vi |
|-----|---------|
| `GET /api/articles/{slug}` | Đã `+1 view` (student: tối đa 1 lần/ngày; guest: mỗi request) |
| `POST /api/articles/{slug}/track-view` | `+1 view` thêm, **không dedupe** |

Nếu FE mở trang chi tiết vừa gọi `getArticleBySlug` vừa gọi `trackArticleView` (và React Strict Mode gọi 2 lần) → dễ ra ~3 view / 1 lần mở.

### Việc FE cần làm

- **Chỉ dùng 1 nguồn đếm.** Khuyến nghị: chỉ `GET /api/articles/{slug}`, **bỏ** `track-view` trên trang detail.
- Không gọi `track-view` trong `useEffect` nếu đã fetch detail.
- Nếu giữ `track-view` cho trường hợp đặc biệt (preview không qua detail) → **không** gọi kèm `GET detail`.

```ts
// ❌ Tránh
await getArticleBySlug(slug);
await trackArticleView(slug);

// ✅ Đúng
const article = await getArticleBySlug(slug);
// viewCount đã được BE cập nhật trong response
```

Có thể deprecate / xóa helper:

```ts
// POST /api/articles/{slug}/track-view  — không dùng trên trang detail
export const trackArticleView = (slug: string) =>
  api.post(`/articles/${slug}/track-view`);
```

---

## 2. Article — Ghim / Nổi bật / Đại diện chính

Ba flag **độc lập**, ý nghĩa khác nhau:

| UI label | Field | Dùng để |
|----------|--------|---------|
| **Ghim** | `isPinned` | Đẩy lên đầu list (`ORDER BY isPinned DESC, priority DESC, …`) |
| **Nổi bật** | `isFeatured` | Section/banner riêng: `GET /api/articles/featured` |
| **Đại diện chính** | `isPrimary` | Chỉ khi bài gắn `activityId`. Mỗi activity **1** bài primary (announcement chính). Recap / behind-scene không thay primary. Set bằng `PUT /api/admin/articles/{id}/set-primary` |

### Gợi ý CMS form

- Checkbox **Ghim**, **Nổi bật** luôn hiện.
- **Đại diện chính**: chỉ enable khi đã chọn activity; hoặc dùng action “Đặt làm bài chính” sau khi tạo.
- List admin: badge riêng cho từng flag, không gộp một label.

```ts
// List / detail đều có
isPinned: boolean;
isFeatured: boolean;
isPrimary: boolean;
```

---

## 3. Article — Tag tự tạo slug

Giống category: tạo tag **không bắt buộc** gửi `slug`.

```ts
// POST /api/admin/articles/tags
{ name: "Học thuật", isActive: true }
// BE: slug = normalize(name) → "học-thuật" sau normalize ASCII → thường là dạng slug từ name
```

FE CMS:

- Input `name` bắt buộc.
- `slug` optional (placeholder: “Để trống = tự tạo từ tên”).
- Student filter dùng **slug** trên URL: `/articles/tag/{tagSlug}`.

---

## 4. Article — Thống kê & lượt thích

### Dashboard CMS hiện có

`GET /api/admin/articles/statistics` →

```ts
interface ArticleStatisticsResponse {
  totalArticles: number;
  publishedArticles: number;
  draftArticles: number;
  totalViews: number;
  totalWishlists: number; // yêu thích (wishlist), KHÔNG phải like/reaction
  featuredArticles: number;
  pinnedArticles: number;
  topViewedArticles: Array<Record<string, unknown>>;
  recentlyPublished: Array<Record<string, unknown>>;
  articlesByCategory: Record<string, number>;
  articlesByMonth: Record<string, number>;
}
```

**Lưu ý UI:** label phải là **“Yêu thích / Wishlist”**, không ghi “Lượt thích”. Hiện BE **chưa** có `totalLikes` / `totalReactions` trong statistics.

### Lượt thích (reaction) trên trang bài

Dùng API reaction riêng:

```ts
// Đếm theo type
GET /api/articles/{slug}/reactions
// → { LIKE: 12, LOVE: 3, ... }

// Student react
POST /api/articles/{slug}/reaction?type=LIKE
DELETE /api/articles/{slug}/reaction

// Detail đã có
myReaction?: ReactionType
```

```ts
export type ReactionType = 'LIKE' | 'LOVE' | 'CLAP' | 'FIRE' | 'SUPPORT';
```

CMS dashboard muốn hiện tổng like → tạm thời **không có field sẵn**; cần BE bổ sung sau. FE đừng map `totalWishlists` thành “likes”.

---

## 5. Article — Student lọc category / tag

Admin/Manager CRUD cate/tag. Student **chỉ đọc + lọc** qua API public:

| Mục đích | API |
|----------|-----|
| List category + số bài | `GET /api/articles/categories` |
| Lọc theo category | `GET /api/articles/category/{categorySlug}?page=&size=` |
| List tag + số bài | `GET /api/articles/tags` |
| Lọc theo tag | `GET /api/articles/tag/{tagSlug}?page=&size=` |
| Search | `GET /api/articles/search?keyword=` |
| Featured | `GET /api/articles/featured?limit=` |
| List published | `GET /api/articles?page=&size=` |

### Types public

```ts
interface ArticleCategoryPublicResponse {
  id: number;
  name: string;
  slug: string;
  articleCount: number;
}

interface ArticleTagPublicResponse {
  id: number;
  name: string;
  slug: string;
  articleCount: number;
}
```

### Gợi ý UI student

- Chip categories (horizontal scroll)
- Tag cloud / filter chips từ `/tags`
- URL mirror slug: `/news?category=...` hoặc `/news/tag/...`
- Kết hợp search box → `/articles/search`

```ts
export const getPublicCategories = () => api.get('/articles/categories');
export const getPublicTags = () => api.get('/articles/tags');
export const getArticlesByCategory = (slug: string, page = 0, size = 10) =>
  api.get(`/articles/category/${slug}`, { params: { page, size } });
export const getArticlesByTag = (slug: string, page = 0, size = 10) =>
  api.get(`/articles/tag/${slug}`, { params: { page, size } });
```

---

## 6. Lịch trong web — personal-calendar

### Breaking change

| Trước | Sau |
|-------|-----|
| `body: Array<{ activityId, title, startTime, endTime, location }>` | `body: PersonalCalendarResponse` |
| Không filter | Query optional: `from`, `to`, `date` |

Nếu FE đang `body.map(...)` như list cũ → **vỡ**. Đổi sang `body.markedDates` / `body.events`.

### API

- **Method:** `GET`
- **Path:** `/api/registrations/personal-calendar`
- **Auth:** JWT, role `STUDENT`
- **Query (optional, `YYYY-MM-DD`):**

| Param | Ý nghĩa |
|-------|---------|
| `from` | Đầu khoảng (thường đầu tháng) |
| `to` | Cuối khoảng |
| `date` | Ngày chọn — chỉ thu hẹp `events`; `markedDates` vẫn theo `from`/`to` |

```http
GET /api/registrations/personal-calendar?from=2026-07-01&to=2026-07-31
Authorization: Bearer <JWT>
```

```http
GET /api/registrations/personal-calendar?from=2026-07-01&to=2026-07-31&date=2026-07-12
Authorization: Bearer <JWT>
```

### Types

```ts
export type RegistrationStatus =
  | 'APPROVED'
  | 'ATTENDED'
  | 'PENDING'
  | 'REJECTED'
  | 'CANCELLED'
  | 'WAITLIST';

export type EventTimeStatus = 'UPCOMING' | 'ONGOING' | 'PAST';

export type ActivityType =
  | 'SUKIEN'
  | 'MINIGAME'
  | 'CONG_TAC_XA_HOI'
  | 'CHUYEN_DE_DOANH_NGHIEP';

export interface CalendarMarkedDate {
  date: string; // "YYYY-MM-DD"
  eventCount: number;
}

export interface PersonalCalendarEventItem {
  registrationId: number;
  activityId: number;
  title: string;
  startTime: string; // "2026-07-12T08:00:00"
  endTime: string | null;
  location?: string;
  status: RegistrationStatus; // thực tế APPROVED | ATTENDED
  eventTimeStatus: EventTimeStatus;
  activityType?: ActivityType | null;
  bannerUrl?: string | null;
  shareLink?: string | null;
  ticketCode?: string | null;
  seriesId?: number | null;
  important: boolean;
}

export interface PersonalCalendarResponse {
  from: string | null;
  to: string | null;
  markedDates: CalendarMarkedDate[];
  events: PersonalCalendarEventItem[];
}

export interface SimpleApiResponse<T> {
  status: boolean;
  message: string;
  body: T;
}
```

### Response mẫu

```json
{
  "status": true,
  "message": "Event dates retrieved",
  "body": {
    "from": "2026-07-01",
    "to": "2026-07-31",
    "markedDates": [
      { "date": "2026-07-12", "eventCount": 2 },
      { "date": "2026-07-13", "eventCount": 1 }
    ],
    "events": [
      {
        "registrationId": 1,
        "activityId": 10,
        "title": "Hackathon",
        "startTime": "2026-07-12T08:00:00",
        "endTime": "2026-07-13T17:00:00",
        "location": "A1",
        "status": "APPROVED",
        "eventTimeStatus": "UPCOMING",
        "activityType": "SUKIEN",
        "bannerUrl": "https://cdn.example.com/uploads/banner.png",
        "shareLink": "https://...",
        "ticketCode": "ABC",
        "seriesId": null,
        "important": false
      }
    ]
  }
}
```

### Service

```ts
export const getPersonalCalendar = (params?: {
  from?: string;
  to?: string;
  date?: string;
}): Promise<AxiosResponse<SimpleApiResponse<PersonalCalendarResponse>>> =>
  api.get('/registrations/personal-calendar', { params });
```

### UI lịch (kiểu iPhone notes)

**Month view**

1. Đổi tháng → `from` = ngày 1, `to` = cuối tháng.
2. Map `markedDates` → chấm:

```ts
const markers = Object.fromEntries(
  calendar.markedDates.map((d) => [
    d.date,
    { marked: true, dotColor: '#2563eb', count: d.eventCount },
  ])
);
```

3. Multi-day: BE đã expand mọi ngày `start→end`.

**Day detail**

- Cách A: gọi lại với `date=` (khuyến nghị).
- Cách B: filter client từ `events` tháng:

```ts
const dayEvents = calendar.events.filter((e) => {
  const start = e.startTime.slice(0, 10);
  const end = (e.endTime ?? e.startTime).slice(0, 10);
  return selectedDate >= start && selectedDate <= end;
});
```

**Hook mẫu**

```ts
function usePersonalCalendar(month: Date, selectedDate?: string) {
  const from = format(startOfMonth(month), 'yyyy-MM-dd');
  const to = format(endOfMonth(month), 'yyyy-MM-dd');

  return useQuery({
    queryKey: ['personal-calendar', from, to, selectedDate ?? null],
    queryFn: async () => {
      const res = await getPersonalCalendar({ from, to, date: selectedDate });
      if (!res.data.status) throw new Error(res.data.message);
      return res.data.body;
    },
  });
}
```

### Nghiệp vụ lịch

- Chỉ `APPROVED` / `ATTENDED`.
- Overlap theo khoảng ngày.
- Luôn truyền `from`/`to` theo tháng đang xem (tránh tải full lịch).

---

## 7. ICS vẫn giữ

Hai khái niệm **không thay thế nhau**:

| Mục đích | API |
|----------|-----|
| Lịch **trong web** (chấm ngày) | `GET /api/registrations/personal-calendar` |
| Xuất file ra Google/Apple Calendar | `GET /api/articles/{slug}/calendar` → blob `.ics` |

```ts
// Giữ nút "Thêm vào lịch ngoài" trên trang article (có activity)
export const getCalendarFile = (slug: string) =>
  api.get(`/articles/${slug}/calendar`, { responseType: 'blob' });
```

---

## 8. Checklist tổng

### Article

- [ ] Trang detail: **không** gọi `track-view` kèm `GET /{slug}`
- [ ] CMS: tách rõ UI Ghim / Nổi bật / Đại diện chính
- [ ] CMS tag: `slug` optional, hint tự tạo từ name
- [ ] Dashboard: label `totalWishlists` = Yêu thích (không phải Like)
- [ ] Detail: like dùng `/reactions` + `myReaction`
- [ ] Student home/news: filter bằng `/categories`, `/category/{slug}`, `/tags`, `/tag/{slug}`

### Calendar

- [ ] Đổi parser `personal-calendar` → `PersonalCalendarResponse`
- [ ] Month: `markedDates` + query `from`/`to`
- [ ] Day: `date` hoặc filter client
- [ ] Deep link `activityId` / `shareLink`
- [ ] Giữ ICS cho “thêm lịch ngoài” nếu cần

---

## Liên quan

- Article full guide: [article-fe-integration-guide.md](./article-fe-integration-guide.md)
- Handoff personal-calendar: [FE_BACKEND_HANDOFF_SPEC.md](./refactor/FE_BACKEND_HANDOFF_SPEC.md) § 14
- Advanced article notes: [event-article-advanced-report.md](./event-article-advanced-report.md)
