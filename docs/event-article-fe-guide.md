# Hướng Dẫn Tích Hợp Frontend (React TypeScript) — Module Article (Phases 1, 2, 3)

Tài liệu này hướng dẫn chi tiết cách Frontend (React TypeScript) tương tác với các API mới và thay đổi cấu trúc dữ liệu thuộc **Phases 1, 2 và 3** của Module Bài viết (Article).

---

## I. TypeScript Types & Enums

Khai báo các kiểu dữ liệu sau trên Frontend để tương tác chính xác với Backend:

```typescript
export type RegistrationCtaStatus = "UPCOMING" | "OPEN" | "WAITLIST" | "FULL" | "CLOSED";

export type ArticleType = "ANNOUNCEMENT" | "RECAP" | "BEHIND_SCENE" | "RESULT" | "UPDATE";

export type ReactionType = "LIKE" | "LOVE" | "CLAP" | "FIRE" | "SUPPORT";

export interface StudentBasicInfo {
  id: number;
  fullName: string;
  studentCode: string;
  avatarUrl: string | null;
}

export interface ArticleCommentResponse {
  id: number;
  articleId: number;
  parentCommentId: number | null;
  content: string;
  isFlagged: boolean;
  flagReason: string | null;
  isHidden: boolean;
  student: StudentBasicInfo | null;
  replies: ArticleCommentResponse[];
  createdAt: string;
  updatedAt: string;
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
  shareLink: string | null;
}

export interface ArticleCategoryInfo {
  id: number;
  name: string;
  slug: string;
}

export interface ArticleImageResponse {
  id: number;
  imageUrl: string;
  caption: string | null;
  displayOrder: number;
  isCover: boolean;
  createdAt: string;
}

export interface ArticleDetailResponse {
  id: number;
  title: string;
  slug: string;
  thumbnailUrl: string | null;
  content: string; // Nội dung HTML
  seoTitle: string | null;
  seoDescription: string | null;
  published: boolean;
  publishedAt: string | null;
  registrationStatus: RegistrationCtaStatus | null;
  viewCount: number;
  wishlistCount: number;
  isFeatured: boolean;
  isPinned: boolean;
  priority: number;
  createdAt: string;
  updatedAt: string;
  activityInfo: ArticleActivityInfo | null;
  category: ArticleCategoryInfo | null;
  tags: string[];
  images: ArticleImageResponse[];
  coverImages: ArticleImageResponse[];
  isWishlisted: boolean;
  myReaction: ReactionType | null; // Cảm xúc của user hiện tại
  redirectedFrom: string | null; // Phase 3: Slug cũ đã chuyển hướng
  currentSlug: string | null;    // Phase 3: Slug mới hiện tại
}

export interface ArticleListResponse {
  id: number;
  title: string;
  slug: string;
  thumbnailUrl: string | null;
  seoDescription: string | null;
  registrationStatus: RegistrationCtaStatus | null;
  activityId: number | null;     // Phase 1: Liên kết hoạt động
  shareLink: string | null;      // Phase 1: Link đăng ký ngoài (Google Form...)
  articleType: ArticleType;      // Phase 2: Phân loại bài viết
  isPrimary: boolean;            // Phase 2: Bài viết chính đại diện cho hoạt động
  isPublished: boolean;
  isFeatured: boolean;
  isPinned: boolean;
  publishedAt: string | null;
  viewCount: number;
  wishlistCount: number;
  categoryName: string | null;
  tags: string[];
  images: ArticleImageResponse[];
}

export interface ArticleWishlistItemResponse {
  id: number;
  articleId: number;
  title: string;
  slug: string;
  thumbnailUrl: string | null;
  seoDescription: string | null;
  isPublished: boolean;
  publishedAt: string | null;
  registrationStatus: RegistrationCtaStatus | null;
  wishlistedAt: string;
}

export interface ArticleHistoryResponse {
  id: number;
  articleId: number;
  title: string;
  slug: string;
  thumbnailUrl: string | null;
  seoDescription: string | null;
  isPublished: boolean;
  publishedAt: string | null;
  registrationStatus: RegistrationCtaStatus | null;
  viewedAt: string;
}

export interface EventArticleAdminResponse {
  id: number;
  activityId: number | null;
  title: string;
  slug: string;
  thumbnailUrl: string | null;
  content: string;
  seoTitle: string | null;
  seoDescription: string | null;
  published: boolean;
  publishedAt: string | null;
  viewCount: number;
  wishlistCount: number;
  featured: boolean;
  pinned: boolean;
  priority: number;
  categoryId: number | null;
  categoryName: string | null;
  tagNames: string[];
  createdAt: string;
  updatedAt: string;
  articleType: ArticleType;
  isPrimary: boolean;
}

export interface ArticleStatisticsResponse {
  totalArticles: number;
  publishedArticles: number;
  draftArticles: number;
  totalViews: number;
  totalWishlists: number;
  featuredArticles: number;
  pinnedArticles: number;
  topViewedArticles: any[] | null;
  recentlyPublished: any[] | null;
  articlesByCategory: Record<string, number> | null;
  articlesByMonth: Record<string, number> | null;
}
```

---

## II. Danh Sách API Theo Chuẩn API-First

### A. API Dành Cho Học Viên / Công Khai (Student / Public APIs)

---

#### 1. Lấy danh sách bài viết công khai

##### 1. Mô tả nghiệp vụ
Hiển thị danh sách tất cả các bài viết đã được xuất bản (public) cho người dùng. Dữ liệu trả về hỗ trợ phân trang và được sắp xếp ưu tiên theo bài viết được ghim (`isPinned`) và mức độ ưu tiên (`priority`).

##### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles`
- **Authentication:** Not required

##### 3. Request
- **Path Parameters:** Không có.
- **Query Parameters:**
  - `page` (number - mặc định `0`) - Số trang cần lấy.
  - `size` (number - mặc định `10`) - Số phần tử trên một trang.

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "content": [
        {
          "id": 10,
          "title": "Hội thao Sinh viên CampusLife 2026",
          "slug": "hoi-thao-sinh-vien-campuslife-2026",
          "thumbnailUrl": "https://example.com/images/sport.jpg",
          "seoDescription": "Sự kiện hội thao lớn nhất năm dành cho sinh viên.",
          "registrationStatus": "OPEN",
          "activityId": 5,
          "shareLink": null,
          "articleType": "ANNOUNCEMENT",
          "isPrimary": true,
          "isPublished": true,
          "isFeatured": true,
          "isPinned": true,
          "publishedAt": "2026-06-01T08:00:00",
          "viewCount": 125,
          "wishlistCount": 42,
          "categoryName": "Thể Thao",
          "tags": ["Hội Thao", "Ngoại Khóa"],
          "images": []
        }
      ],
      "totalPages": 1,
      "totalElements": 1,
      "size": 10,
      "number": 0,
      "last": true
    }
  }
  ```
- **Error Responses:**
  - `400 BAD REQUEST` - Lỗi định dạng tham số.

---

#### 2. Chi tiết bài viết theo Slug

##### 1. Mô tả nghiệp vụ
Tải thông tin chi tiết một bài viết qua slug. Hỗ trợ trả về thông tin đăng ký hoạt động đi kèm (nếu có), danh sách ảnh và xác định xem học viên hiện tại đã lưu bài viết vào danh sách yêu thích hay chưa. Nếu bài viết được truy cập qua slug cũ đã bị thay đổi, hệ thống sẽ trả về thêm các trường `redirectedFrom` và `currentSlug` hỗ trợ tự động điều hướng URL trên Frontend.

##### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/{slug}`
- **Authentication:** Not required (Nếu học viên đã đăng nhập, đính kèm Header Authorization để nhận thông tin trạng thái `isWishlisted`).

##### 3. Request
- **Path Parameters:**
  - `slug` (string) - Đường dẫn tĩnh của bài viết.
- **Query Parameters:** Không có.

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 10,
      "title": "Hội thao Sinh viên CampusLife 2026",
      "slug": "hoi-thao-sinh-vien-campuslife-2026",
      "thumbnailUrl": "https://example.com/images/sport.jpg",
      "content": "<p>Nội dung chi tiết bài viết dạng HTML...</p>",
      "seoTitle": "Hội thao Sinh viên CampusLife 2026",
      "seoDescription": "Sự kiện hội thao lớn nhất năm dành cho sinh viên.",
      "published": true,
      "publishedAt": "2026-06-01T08:00:00",
      "registrationStatus": "OPEN",
      "viewCount": 126,
      "wishlistCount": 42,
      "isFeatured": true,
      "isPinned": true,
      "priority": 1,
      "createdAt": "2026-06-01T07:00:00",
      "updatedAt": "2026-06-05T12:00:00",
      "activityInfo": {
        "id": 5,
        "name": "Đăng ký Hội Thao Sinh Viên",
        "location": "Sân vận động trường",
        "startDate": "2026-06-10T08:00:00",
        "endDate": "2026-06-12T17:00:00",
        "registrationStartDate": "2026-06-01T08:00:00",
        "registrationDeadline": "2026-06-08T23:59:59",
        "scoreType": "NGOAI_KHOA",
        "shareLink": null
      },
      "category": {
        "id": 2,
        "name": "Thể Thao",
        "slug": "the-thao"
      },
      "tags": ["Hội Thao", "Ngoại Khóa"],
      "images": [],
      "coverImages": [],
      "myReaction": "LOVE",
      "isWishlisted": false,
      "redirectedFrom": null,
      "currentSlug": null
    }
  }
  ```
- **Error Responses:**
  - `404 NOT FOUND` - Không tìm thấy bài viết hoặc bài viết chưa được công bố công khai.

---

#### 3. Bình luận bài viết

##### 1. Mô tả nghiệp vụ
Cho phép sinh viên đăng nhập gửi bình luận vào bài viết hoặc trả lời (reply) một bình luận khác. Nội dung bình luận sẽ đi qua bộ lọc kiểm duyệt từ ngữ thô tục tự động (tiếng Anh và tiếng Việt). Nếu phát hiện từ thô tục, bình luận vẫn được tạo nhưng được gắn cờ `isFlagged = true` và lý do `PROFANITY` (không tự động ẩn để Admin xem xét duyệt thủ công).

##### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/articles/{slug}/comments`
- **Authentication:** Required (Sinh viên đăng nhập)

##### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết cần bình luận.
- **Query Parameters:** Không có.
- **Request Body:**
  ```json
  {
    "content": "string - Nội dung bình luận (bắt buộc, không trống)",
    "parentCommentId": "number - ID bình luận cha nếu là phản hồi (tùy chọn, có thể null)"
  }
  ```

##### 4. Response
- **Success (201):**
  ```json
  {
    "code": 201,
    "message": "success",
    "data": {
      "id": 15,
      "articleId": 10,
      "parentCommentId": null,
      "content": "Bài viết hữu ích quá, cảm ơn ban tổ chức!",
      "isFlagged": false,
      "flagReason": null,
      "isHidden": false,
      "student": {
        "id": 3,
        "fullName": "Nguyễn Văn A",
        "studentCode": "SV12345",
        "avatarUrl": "https://example.com/avatar/a.png"
      },
      "replies": [],
      "createdAt": "2026-06-05T18:00:00",
      "updatedAt": "2026-06-05T18:00:00"
    }
  }
  ```
- **Error Responses:**
  - `401 UNAUTHORIZED` - Chưa đăng nhập hoặc token không hợp lệ.
  - `400 BAD REQUEST` - Nội dung trống hoặc phản hồi bình luận không tồn tại/không cùng bài viết.
  - `404 NOT FOUND` - Không tìm thấy bài viết.

---

#### 4. Lấy cây bình luận của bài viết

##### 1. Mô tả nghiệp vụ
Hiển thị danh sách bình luận đã được duyệt (không bị ẩn) của bài viết dưới dạng phân trang. Mỗi bình luận gốc (root) sẽ chứa danh sách các câu trả lời trực tiếp đệ quy bên trong thuộc tính `replies`.

##### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/{slug}/comments`
- **Authentication:** Not required

##### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết.
- **Query Parameters:**
  - `page` (number - mặc định `0`)
  - `size` (number - mặc định `10`)

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "content": [
        {
          "id": 15,
          "articleId": 10,
          "parentCommentId": null,
          "content": "Bài viết hữu ích quá, cảm ơn ban tổ chức!",
          "isFlagged": false,
          "flagReason": null,
          "isHidden": false,
          "student": {
            "id": 3,
            "fullName": "Nguyễn Văn A",
            "studentCode": "SV12345",
            "avatarUrl": "https://example.com/avatar/a.png"
          },
          "replies": [
            {
              "id": 16,
              "articleId": 10,
              "parentCommentId": 15,
              "content": "Đồng ý với bạn!",
              "isFlagged": false,
              "flagReason": null,
              "isHidden": false,
              "student": {
                "id": 4,
                "fullName": "Trần Thị B",
                "studentCode": "SV67890",
                "avatarUrl": null
              },
              "replies": [],
              "createdAt": "2026-06-05T18:05:00",
              "updatedAt": "2026-06-05T18:05:00"
            }
          ],
          "createdAt": "2026-06-05T18:00:00",
          "updatedAt": "2026-06-05T18:00:00"
        }
      ],
      "totalPages": 1,
      "totalElements": 1,
      "size": 10,
      "number": 0,
      "last": true
    }
  }
  ```
- **Error Responses:**
  - `404 NOT FOUND` - Không tìm thấy bài viết.

---

#### 5. Xóa bình luận của chính mình

##### 1. Mô tả nghiệp vụ
Cho phép sinh viên đăng nhập có quyền tự xóa bình luận do chính mình viết.

##### 2. API Endpoint
- **Method:** `DELETE`
- **Path:** `/api/articles/comments/{commentId}`
- **Authentication:** Required (Sinh viên đăng nhập)

##### 3. Request
- **Path Parameters:**
  - `commentId` (number) - ID của bình luận cần xóa.
- **Query Parameters:** Không có.

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```
- **Error Responses:**
  - `401 UNAUTHORIZED` - Chưa đăng nhập.
  - `400 BAD REQUEST` - Bình luận không thuộc sở hữu của tài khoản hiện tại.
  - `404 NOT FOUND` - Bình luận không tồn tại.

---

#### 6. Thêm/Cập nhật tương tác Emoji (Reaction)

##### 1. Mô tả nghiệp vụ
Cho phép học viên thêm mới hoặc thay đổi loại cảm xúc đối với một bài viết cụ thể (chọn 1 trong các emoji: `LIKE`, `LOVE`, `CLAP`, `FIRE`, `SUPPORT`). Mỗi học viên chỉ có tối đa 1 tương tác trên mỗi bài viết.

##### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/articles/{slug}/reaction`
- **Authentication:** Required (Sinh viên đăng nhập)

##### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết.
- **Query Parameters:**
  - `type` (string - giá trị thuộc enum ReactionType, ví dụ `LOVE`) - Loại tương tác.

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```
- **Error Responses:**
  - `401 UNAUTHORIZED` - Chưa đăng nhập.
  - `400 BAD REQUEST` - Loại reaction không hợp lệ.
  - `404 NOT FOUND` - Không tìm thấy bài viết.

---

#### 7. Hủy tương tác Emoji (Delete Reaction)

##### 1. Mô tả nghiệp vụ
Cho phép học viên thu hồi tương tác cảm xúc đã tạo trên bài viết.

##### 2. API Endpoint
- **Method:** `DELETE`
- **Path:** `/api/articles/{slug}/reaction`
- **Authentication:** Required (Sinh viên đăng nhập)

##### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết.
- **Query Parameters:** Không có.

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```
- **Error Responses:**
  - `401 UNAUTHORIZED` - Chưa đăng nhập.
  - `404 NOT FOUND` - Không tìm thấy tương tác hoặc bài viết.

---

#### 8. Lấy thống kê số lượt tương tác của bài viết

##### 1. Mô tả nghiệp vụ
Lấy tổng số lượng các emoji tương tác của bài viết được phân loại chi tiết theo từng loại cảm xúc để hiển thị lên UI.

##### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/{slug}/reactions`
- **Authentication:** Not required

##### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết.
- **Query Parameters:** Không có.

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "LIKE": 150,
      "LOVE": 80,
      "CLAP": 45,
      "FIRE": 12,
      "SUPPORT": 9
    }
  }
  ```
- **Error Responses:**
  - `404 NOT FOUND` - Không tìm thấy bài viết.

---

#### 9. Ghi nhận số lượt chia sẻ (Track Share)

##### 1. Mô tả nghiệp vụ
Tăng bộ đếm lượt chia sẻ khi người dùng nhấn nút "Share" bài viết (trên Facebook, sao chép liên kết...). Hỗ trợ cả người dùng vãng lai không đăng nhập.

##### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/articles/{slug}/track-share`
- **Authentication:** Not required

##### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết được chia sẻ.
- **Query Parameters:** Không có.

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```
- **Error Responses:**
  - `404 NOT FOUND` - Không tìm thấy bài viết.

---

#### 10. Lấy danh sách lịch sử đọc của học viên

##### 1. Mô tả nghiệp vụ
Hiển thị danh sách các bài viết mà học viên đã truy cập xem chi tiết, sắp xếp theo thời gian xem gần đây nhất giảm dần.

##### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/history`
- **Authentication:** Required (Sinh viên đăng nhập)

##### 3. Request
- **Path Parameters:** Không có.
- **Query Parameters:**
  - `page` (number - mặc định `0`)
  - `size` (number - mặc định `10`)

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "content": [
        {
          "id": 1,
          "articleId": 10,
          "title": "Hội thao Sinh viên CampusLife 2026",
          "slug": "hoi-thao-sinh-vien-campuslife-2026",
          "thumbnailUrl": "https://example.com/images/sport.jpg",
          "seoDescription": "Sự kiện hội thao lớn nhất năm...",
          "isPublished": true,
          "publishedAt": "2026-06-01T08:00:00",
          "registrationStatus": "OPEN",
          "viewedAt": "2026-06-05T10:00:00"
        }
      ],
      "totalPages": 1,
      "totalElements": 1,
      "size": 10,
      "number": 0,
      "last": true
    }
  }
  ```
- **Error Responses:**
  - `401 UNAUTHORIZED` - Chưa đăng nhập.

---

#### 11. Xóa một bản ghi lịch sử đọc

##### 1. Mô tả nghiệp vụ
Cho phép học viên xóa bỏ một bài viết bất kỳ khỏi lịch sử đọc của mình.

##### 2. API Endpoint
- **Method:** `DELETE`
- **Path:** `/api/articles/history/{historyId}`
- **Authentication:** Required (Sinh viên đăng nhập)

##### 3. Request
- **Path Parameters:**
  - `historyId` (number) - ID của bản ghi lịch sử đọc bài viết.
- **Query Parameters:** Không có.

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```
- **Error Responses:**
  - `401 UNAUTHORIZED` - Chưa đăng nhập.
  - `400 BAD REQUEST` - Lịch sử đọc không thuộc sở hữu của học viên hiện tại.

---

#### 12. Xóa toàn bộ lịch sử đọc bài viết

##### 1. Mô tả nghiệp vụ
Cho phép học viên dọn dẹp sạch toàn bộ danh sách lịch sử đọc bài viết của mình.

##### 2. API Endpoint
- **Method:** `DELETE`
- **Path:** `/api/articles/history`
- **Authentication:** Required (Sinh viên đăng nhập)

##### 3. Request
- **Path/Query Parameters:** Không có.

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```
- **Error Responses:**
  - `401 UNAUTHORIZED` - Chưa đăng nhập.

---

#### 13. Lấy danh sách bài viết thịnh hành (Trending Articles)

##### 1. Mô tả nghiệp vụ
Hiển thị danh sách các bài viết có lượng truy cập nhiều nhất trong khoảng thời gian N ngày gần nhất (sắp xếp giảm dần theo lượt xem).

##### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/trending`
- **Authentication:** Not required

##### 3. Request
- **Path Parameters:** Không có.
- **Query Parameters:**
  - `days` (number - mặc định `7`) - Phạm vi thống kê số ngày gần đây.
  - `limit` (number - mặc định `5`) - Giới hạn số bài viết thịnh hành trả về.

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 10,
        "title": "Hội thao Sinh viên CampusLife 2026",
        "slug": "hoi-thao-sinh-vien-campuslife-2026",
        "thumbnailUrl": "https://example.com/images/sport.jpg",
        "seoDescription": "Sự kiện hội thao lớn nhất năm...",
        "registrationStatus": "OPEN",
        "activityId": 5,
        "shareLink": null,
        "articleType": "ANNOUNCEMENT",
        "isPrimary": true,
        "isPublished": true,
        "isFeatured": false,
        "isPinned": false,
        "publishedAt": "2026-06-01T08:00:00",
        "viewCount": 126,
        "wishlistCount": 42,
        "categoryName": "Thể Thao",
        "tags": ["Hội thao"],
        "images": []
      }
    ]
  }
  ```

---

#### 14. Đăng ký nhận thông báo danh sách chờ (Waitlist)

##### 1. Mô tả nghiệp vụ
Khi CTA bài viết hiển thị trạng thái `WAITLIST` (sự kiện đầy chỗ nội bộ), học viên nhấn đăng ký tham gia danh sách chờ để nhận thông báo tự động khi có chỗ trống.

##### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/articles/{slug}/waitlist`
- **Authentication:** Required (Sinh viên đăng nhập)

##### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết chứa hoạt động.
- **Query Parameters:** Không có.

##### 4. Response
- **Success (201):**
  ```json
  {
    "code": 201,
    "message": "success",
    "data": null
  }
  ```
- **Error Responses:**
  - `401 UNAUTHORIZED` - Chưa đăng nhập.
  - `400 BAD REQUEST` - Hoạt động không ở trạng thái WAITLIST hoặc học viên đã đăng ký tham gia/đăng ký chờ trước đó.

---

#### 15. Lưu bài viết vào danh sách yêu thích (Wishlist)

##### 1. Mô tả nghiệp vụ
Thêm bài viết vào danh sách yêu thích cá nhân của học viên.

##### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/articles/{slug}/wishlist`
- **Authentication:** Required (Sinh viên đăng nhập)

##### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết.

##### 4. Response
- **Success (201):**
  ```json
  {
    "code": 201,
    "message": "success",
    "data": null
  }
  ```

---

#### 16. Hủy lưu bài viết khỏi danh sách yêu thích

##### 1. Mô tả nghiệp vụ
Xóa bài viết khỏi danh sách yêu thích cá nhân.

##### 2. API Endpoint
- **Method:** `DELETE`
- **Path:** `/api/articles/{slug}/wishlist`
- **Authentication:** Required (Sinh viên đăng nhập)

##### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết.

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

---

#### 17. Lấy danh sách bài viết yêu thích của học viên

##### 1. Mô tả nghiệp vụ
Lấy danh sách phân trang các bài viết học viên đã đánh dấu yêu thích.

##### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/wishlist`
- **Authentication:** Required (Sinh viên đăng nhập)

##### 3. Request
- **Query Parameters:**
  - `page` (number - mặc định `0`)
  - `size` (number - mặc định `10`)

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "content": [
        {
          "id": 1,
          "articleId": 10,
          "title": "Hội thao Sinh viên CampusLife 2026",
          "slug": "hoi-thao-sinh-vien-campuslife-2026",
          "thumbnailUrl": "https://example.com/images/sport.jpg",
          "seoDescription": "Sự kiện hội thao lớn nhất năm...",
          "isPublished": true,
          "publishedAt": "2026-06-01T08:00:00",
          "registrationStatus": "OPEN",
          "wishlistedAt": "2026-06-05T10:00:00"
        }
      ],
      "totalPages": 1,
      "totalElements": 1,
      "size": 10,
      "number": 0,
      "last": true
    }
  }
  ```

---

#### 18. Tải file lịch sự kiện định dạng ICS

##### 1. Mô tả nghiệp vụ
Cho phép tải về tệp tin lịch định dạng `.ics` chứa thông tin thời gian diễn ra sự kiện/hoạt động liên quan để tích hợp vào Google Calendar, Apple Calendar, Outlook...

##### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/{slug}/calendar`
- **Authentication:** Not required

##### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết liên quan.

##### 4. Response
- **Success (200):** File nhị phân dạng `.ics` (MIME: `text/calendar`).
- **Error Responses:**
  - `404 NOT FOUND` - Không tìm thấy bài viết hoặc bài viết không đính kèm hoạt động.

---

### B. API Quản Trị - CMS (Admin & Manager APIs)

---

#### 1. Tạo bài viết mới

##### 1. Mô tả nghiệp vụ
Admin/Manager tạo mới bài viết. Có thể liên kết hoặc không liên kết với một Hoạt động (`activityId` có thể gửi `null` để tạo bài viết độc lập). Có quyền chỉ định loại bài viết (`articleType`) và gán trạng thái bài viết chính đại diện cho hoạt động (`isPrimary`).

##### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/admin/articles`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

##### 3. Request
- **Request Body:**
  ```json
  {
    "activityId": "number - ID hoạt động đính kèm (có thể null)",
    "articleType": "string - thuộc enum ArticleType (ví dụ 'ANNOUNCEMENT')",
    "isPrimary": "boolean - đánh dấu bài viết đại diện chính cho hoạt động",
    "title": "string - tiêu đề bài viết (bắt buộc)",
    "slug": "string - đường dẫn tĩnh, unique (bắt buộc)",
    "thumbnailUrl": "string - link ảnh đại diện bài viết",
    "content": "string - nội dung chi tiết bài viết (HTML - bắt buộc)",
    "seoTitle": "string - tiêu đề tối ưu SEO",
    "seoDescription": "string - mô tả tối ưu SEO",
    "categoryId": "number - ID danh mục bài viết",
    "tagIds": "number[] - danh sách ID từ khóa bài viết",
    "isFeatured": "boolean - trạng thái nổi bật",
    "isPinned": "boolean - trạng thái ghim đầu trang",
    "priority": "number - thứ tự ưu tiên hiển thị (số lớn hơn ưu tiên hơn)"
  }
  ```

##### 4. Response
- **Success (201):**
  ```json
  {
    "code": 201,
    "message": "success",
    "data": {
      "id": 12,
      "activityId": 5,
      "title": "Bài viết giới thiệu sự kiện",
      "slug": "bai-viet-gioi-thieu-su-kien",
      "thumbnailUrl": "https://example.com/thumb.jpg",
      "content": "<p>Nội dung...</p>",
      "seoTitle": "Bài viết giới thiệu",
      "seoDescription": "SEO desc",
      "published": false,
      "publishedAt": null,
      "viewCount": 0,
      "wishlistCount": 0,
      "featured": false,
      "pinned": false,
      "priority": 0,
      "categoryId": 2,
      "categoryName": "Thể Thao",
      "tagNames": ["Hội thao"],
      "createdAt": "2026-06-05T19:00:00",
      "updatedAt": "2026-06-05T19:00:00",
      "articleType": "ANNOUNCEMENT",
      "isPrimary": false
    }
  }
  ```
- **Error Responses:**
  - `401 UNAUTHORIZED` - Chưa đăng nhập hoặc token hết hạn.
  - `403 FORBIDDEN` - Không có quyền ADMIN/MANAGER.
  - `400 BAD REQUEST` - Slug trùng lặp hoặc thiếu dữ liệu bắt buộc.

---

#### 2. Cập nhật bài viết

##### 1. Mô tả nghiệp vụ
Admin/Manager cập nhật thông tin bài viết đã tồn tại. Nếu slug của bài viết được cập nhật đổi sang giá trị mới, hệ thống tự động lưu lại slug cũ vào lịch sử chuyển hướng để hỗ trợ Frontend tự động redirect người dùng truy cập link cũ.

##### 2. API Endpoint
- **Method:** `PUT`
- **Path:** `/api/admin/articles/{articleId}`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

##### 3. Request
- **Path Parameters:**
  - `articleId` (number) - ID bài viết cần sửa đổi.
- **Request Body:** Tương tự như API tạo bài viết mới.

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 12,
      "activityId": 5,
      "title": "Bài viết giới thiệu sự kiện (Đã cập nhật)",
      "slug": "bai-viet-gioi-thieu-su-kien-moi",
      "thumbnailUrl": "https://example.com/thumb.jpg",
      "content": "<p>Nội dung...</p>",
      "seoTitle": "Bài viết giới thiệu mới",
      "seoDescription": "SEO desc mới",
      "published": false,
      "publishedAt": null,
      "viewCount": 0,
      "wishlistCount": 0,
      "featured": false,
      "pinned": false,
      "priority": 0,
      "categoryId": 2,
      "categoryName": "Thể Thao",
      "tagNames": ["Hội thao"],
      "createdAt": "2026-06-05T19:00:00",
      "updatedAt": "2026-06-05T19:15:00",
      "articleType": "ANNOUNCEMENT",
      "isPrimary": false
    }
  }
  ```
- **Error Responses:**
  - `404 NOT FOUND` - Bài viết không tồn tại.
  - `400 BAD REQUEST` - Trùng lặp slug mới với bài viết khác.

---

#### 3. Thiết lập bài viết chính cho hoạt động

##### 1. Mô tả nghiệp vụ
Một hoạt động có thể có nhiều bài viết khác nhau (ví dụ: bài thông báo, bài gom ảnh hậu trường, bài công bố kết quả...). Quản trị viên sử dụng API này để thiết lập bài viết cụ thể làm bài chính đại diện hiển thị trên các trang danh sách chung.

##### 2. API Endpoint
- **Method:** `PUT`
- **Path:** `/api/admin/articles/{articleId}/set-primary`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 12,
      "activityId": 5,
      "title": "Bài viết giới thiệu sự kiện",
      "slug": "bai-viet-gioi-thieu-su-kien",
      "isPrimary": true,
      "articleType": "ANNOUNCEMENT"
    }
  }
  ```
- **Error Responses:**
  - `404 NOT FOUND` - Không tìm thấy bài viết.
  - `400 BAD REQUEST` - Bài viết hiện không đính kèm hoạt động nào (không thể set primary).

---

#### 4. Lấy danh sách lọc nâng cao bài viết CMS (Admin Filter)

##### 1. Mô tả nghiệp vụ
Hiển thị danh sách bài viết hỗ trợ bộ lọc đa tham số nâng cao cho giao diện quản trị CMS.

##### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/admin/articles`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

##### 3. Request
- **Query Parameters:**
  - `status` (string - `PUBLISHED` / `DRAFT` / `ALL`)
  - `activityId` (number) - Lọc theo hoạt động đính kèm.
  - `categoryId` (number) - Lọc theo danh mục.
  - `articleType` (string - thuộc enum ArticleType)
  - `featured` (boolean) - Lọc bài viết nổi bật.
  - `pinned` (boolean) - Lọc bài viết được ghim.
  - `primary` (boolean) - Lọc bài viết đại diện chính.
  - `search` (string) - Tìm kiếm tự do theo tiêu đề/nội dung.
  - `dateFrom` (string - định dạng `yyyy-MM-dd`) - Bắt đầu từ ngày xuất bản.
  - `dateTo` (string - định dạng `yyyy-MM-dd`) - Đến ngày xuất bản.
  - `page` (number - mặc định `0`)
  - `size` (number - mặc định `10`)

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "content": [
        {
          "id": 12,
          "title": "Bài viết giới thiệu sự kiện",
          "slug": "bai-viet-gioi-thieu-su-kien",
          "isPublished": false,
          "isPrimary": true,
          "articleType": "ANNOUNCEMENT"
        }
      ],
      "totalPages": 1,
      "totalElements": 1,
      "size": 10,
      "number": 0
    }
  }
  ```

---

#### 5. Xuất báo cáo danh sách bài viết ra file Excel

##### 1. Mô tả nghiệp vụ
Tải xuống báo cáo danh sách bài viết dạng Excel dựa trên bộ lọc tham số hoàn toàn tương đương với trang CMS nâng cao.

##### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/admin/articles/export`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

##### 3. Request
- **Query Parameters:** Bộ lọc hoàn toàn tương tự như API Lọc nâng cao bài viết CMS.

##### 4. Response
- **Success (200):** File nhị phân `.xlsx` (MIME: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`).

---

#### 6. Ẩn bình luận (Moderation - Hide)

##### 1. Mô tả nghiệp vụ
Admin/Manager duyệt và ẩn một bình luận có nội dung không phù hợp (bao gồm bình luận bị gắn cờ profanity). Bình luận bị ẩn sẽ không hiển thị trên giao diện công khai của sinh viên.

##### 2. API Endpoint
- **Method:** `PUT`
- **Path:** `/api/admin/articles/comments/{commentId}/hide`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

##### 3. Request
- **Path Parameters:**
  - `commentId` (number) - ID bình luận cần ẩn.

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 15,
      "content": "Bình luận chứa từ thô tục",
      "isFlagged": true,
      "isHidden": true
    }
  }
  ```

---

#### 7. Hiện bình luận (Moderation - Unhide)

##### 1. Mô tả nghiệp vụ
Admin/Manager khôi phục lại bình luận bị ẩn cho hiển thị lại công khai.

##### 2. API Endpoint
- **Method:** `PUT`
- **Path:** `/api/admin/articles/comments/{commentId}/unhide`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

##### 3. Request
- **Path Parameters:**
  - `commentId` (number) - ID bình luận cần hiện lại.

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 15,
      "content": "Bình luận chứa từ thô tục",
      "isFlagged": true,
      "isHidden": false
    }
  }
  ```

---

#### 8. Xóa vĩnh viễn bình luận (Admin Delete)

##### 1. Mô tả nghiệp vụ
Admin/Manager xóa hoàn toàn bình luận khỏi cơ sở dữ liệu.

##### 2. API Endpoint
- **Method:** `DELETE`
- **Path:** `/api/admin/articles/comments/{commentId}`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

##### 3. Request
- **Path Parameters:**
  - `commentId` (number) - ID bình luận cần xóa.

##### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

---

## III. Hướng Dẫn Xử Lý Điều Hướng React (Frontend Router & Redirect)

### 1. Phân tích Logic Đăng ký Hoạt động (CTA Button)
Từ Phase 1, Backend loại bỏ hoàn toàn trường `registrationLink` tính sẵn để Frontend chủ động xử lý các kịch bản CTA động. 
Học viên xem bài viết, nhấn nút đăng ký thì Frontend xử lý điều hướng dựa trên 3 trường: `activityId`, `shareLink`, và `registrationStatus`:

- **Trường hợp 1:** Bài viết độc lập (`activityInfo` / `activityId` bằng `null`).
  - *Hành vi:* Không hiển thị bất kỳ nút đăng ký hoạt động nào trên giao diện bài viết.
- **Trường hợp 2:** Có `shareLink` khác rỗng (Link đăng ký Google Form, Typeform...).
  - *Hành vi:* Bỏ qua trạng thái nội bộ, mở `shareLink` trong một tab mới (`window.open`).
- **Trường hợp 3:** Không có `shareLink` nhưng có liên kết hoạt động nội bộ (`activityId`).
  - *Hành vi:* Dựa vào trạng thái `registrationStatus` để chuyển hướng trong ứng dụng:
    - Nếu trạng thái là `OPEN`: Điều hướng tới trang đăng ký tham gia nội bộ: `/activities/:activityId/register`
    - Nếu trạng thái là `WAITLIST`: Điều hướng tới trang đăng ký danh sách chờ nội bộ: `/activities/:activityId/waitlist`
    - Các trạng thái khác (`UPCOMING`, `FULL`, `CLOSED`): Vô hiệu hóa nút bấm (Disabled).

#### React Hook gợi ý xử lý điều hướng:
```typescript
import { useNavigate } from "react-router-dom";
import { RegistrationCtaStatus } from "./types";

export function useArticleRegistration() {
  const navigate = useNavigate();

  const handleRegister = (
    activityId: number | null,
    shareLink: string | null,
    status: RegistrationCtaStatus | null
  ) => {
    // 1. Không có hoạt động hoặc không có trạng thái đăng ký -> không hành động
    if (!status || status === "CLOSED" || status === "FULL" || status === "UPCOMING") return;

    // 2. Nếu có link chia sẻ bên ngoài
    if (shareLink && shareLink.trim() !== "") {
      window.open(shareLink, "_blank", "noopener,noreferrer");
      return;
    }

    // 3. Nếu là hoạt động đăng ký nội bộ hệ thống CampusLife
    if (activityId) {
      if (status === "WAITLIST") {
        // Điều hướng đến form danh sách chờ nội bộ
        navigate(`/activities/${activityId}/waitlist`);
        return;
      }
      // Điều hướng đến form đăng ký hoạt động nội bộ
      navigate(`/activities/${activityId}/register`);
      return;
    }
  };

  return { handleRegister };
}
```

---

### 2. Xử lý Chuyển hướng Slug cũ tự động (Auto Slug Redirect)
Khi quản trị viên đổi tên hoặc thay đổi slug của bài viết trong CMS, các liên kết cũ đã chia sẻ trước đây trên mạng xã hội sẽ bị gãy nếu không có cơ chế chuyển hướng.
Backend hỗ trợ trả về bài viết kèm dữ liệu kiểm tra chuyển hướng (`redirectedFrom` và `currentSlug`) ở chi tiết bài viết `GET /api/articles/{slug}`. 
Frontend cần xử lý chuyển hướng URL mượt mà trên trình duyệt mà không làm gián đoạn trải nghiệm của người dùng:

#### Component React xử lý tự động chuyển hướng:
```typescript
import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArticleDetailResponse } from "./types";
import { api } from "./apiService";

export const ArticleDetailContainer: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const [article, setArticle] = useState<ArticleDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!slug) return;
    setLoading(true);

    api.getArticleDetail(slug)
      .then((res) => {
        const data = res.data; // Ở đây res.data đại diện cho body của API-First Success response

        // Phát hiện chuyển hướng slug cũ sang slug mới
        if (data.redirectedFrom && data.currentSlug && data.currentSlug !== slug) {
          // Thực hiện replace URL trên Browser Router để tránh ghi đè history stack
          navigate(`/articles/${data.currentSlug}`, { replace: true });
          return;
        }

        setArticle(data);
      })
      .catch((err) => {
        console.error("Không thể tải bài viết:", err);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [slug, navigate]);

  if (loading) return <div>Đang tải nội dung bài viết...</div>;
  if (!article) return <div>Không tìm thấy bài viết yêu cầu.</div>;

  return (
    <article>
      <h1>{article.title}</h1>
      <div dangerouslySetInnerHTML={{ __html: article.content }} />
    </article>
  );
};
```
