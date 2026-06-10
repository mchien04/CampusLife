# Tài Liệu Tổng Hợp API — Module Article (Bài Viết Sự Kiện)

Tài liệu này tổng hợp toàn bộ các API thuộc Module Article (Bài viết Sự kiện), bao gồm các API dành cho Học viên / Công khai (Student/Public) và các API Quản trị (Admin/CMS).

Tất cả các API đều tuân thủ cấu trúc phản hồi chuẩn hóa:
- **Success (200/201):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": { ... }
  }
  ```

---

## PHẦN A. API DÀNH CHO HỌC VIÊN / CÔNG KHAI (STUDENT / PUBLIC APIS)

### 1. Lấy danh sách bài viết công khai

#### 1. Mô tả nghiệp vụ
Hiển thị danh sách các bài viết đã được xuất bản (public). Dữ liệu được phân trang và sắp xếp ưu tiên theo bài viết được ghim (`isPinned`) và thứ tự độ ưu tiên (`priority`).

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles`
- **Authentication:** Not required

#### 3. Request
- **Path Parameters:** Không có.
- **Query Parameters:**
  - `page` (number - mặc định `0`) - Số trang cần lấy.
  - `size` (number - mặc định `10`) - Kích thước trang.

#### 4. Response
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
          "seoDescription": "Sự kiện hội thao lớn nhất năm.",
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
          "tags": ["Hội thao"],
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
  - `400 BAD REQUEST` - Sai định dạng tham số.

---

### 2. Lấy danh sách bài viết nổi bật (Featured)

#### 1. Mô tả nghiệp vụ
Lấy danh sách các bài viết được đánh dấu là nổi bật (`isFeatured = true`) để hiển thị tại trang chủ hoặc banner nổi bật.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/featured`
- **Authentication:** Not required

#### 3. Request
- **Query/Path Parameters:** Không có.

#### 4. Response
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
        "seoDescription": "Sự kiện hội thao lớn nhất năm.",
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
        "tags": ["Hội thao"],
        "images": []
      }
    ]
  }
  ```

---

### 3. Lọc bài viết theo danh mục (Category)

#### 1. Mô tả nghiệp vụ
Hiển thị danh sách phân trang các bài viết đã xuất bản thuộc một danh mục cụ thể qua slug của danh mục.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/category/{categorySlug}`
- **Authentication:** Not required

#### 3. Request
- **Path Parameters:**
  - `categorySlug` (string) - Slug của danh mục (ví dụ: `the-thao`).
- **Query Parameters:**
  - `page` (number - mặc định `0`)
  - `size` (number - mặc định `10`)

#### 4. Response
- **Success (200):** Trả về đối tượng Page tương tự API `/api/articles`.
- **Error Responses:**
  - `404 NOT FOUND` - Không tìm thấy danh mục tương ứng.

---

### 4. Tìm kiếm bài viết công khai

#### 1. Mô tả nghiệp vụ
Tìm kiếm các bài viết công khai theo từ khóa xuất hiện trong tiêu đề hoặc nội dung.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/search`
- **Authentication:** Not required

#### 3. Request
- **Query Parameters:**
  - `keyword` (string - bắt buộc) - Từ khóa tìm kiếm.
  - `page` (number - mặc định `0`)
  - `size` (number - mặc định `10`)

#### 4. Response
- **Success (200):** Trả về đối tượng Page chứa danh sách bài viết khớp từ khóa.

---

### 5. Chi tiết bài viết theo Slug

#### 1. Mô tả nghiệp vụ
Tải nội dung chi tiết bài viết (HTML) cùng thông tin hoạt động đi kèm (nếu có), danh sách ảnh thư viện. Nếu người xem là học viên đã đăng nhập, API sẽ trả thêm trạng thái yêu thích (`isWishlisted`) và cảm xúc hiện tại (`myReaction`). Hỗ trợ cơ chế tự động phát hiện slug cũ và cung cấp thông tin chuyển hướng (`redirectedFrom`, `currentSlug`).

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/{slug}`
- **Authentication:** Not required (Cần Header Authorization để lấy `isWishlisted` và `myReaction`).

#### 3. Request
- **Path Parameters:**
  - `slug` (string) - Đường dẫn tĩnh của bài viết.

#### 4. Response
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
      "tags": ["Hội thao"],
      "images": [],
      "coverImages": [],
      "isWishlisted": false,
      "myReaction": "LOVE",
      "redirectedFrom": null,
      "currentSlug": null
    }
  }
  ```
- **Error Responses:**
  - `404 NOT FOUND` - Không tìm thấy bài viết hoặc bài viết chưa được công bố.

---

### 6. Lấy danh sách bài viết liên quan (Related)

#### 1. Mô tả nghiệp vụ
Lấy danh sách các bài viết có liên quan (cùng danh mục hoặc tag) để hiển thị gợi ý đọc tiếp ở cuối trang chi tiết.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/{slug}/related`
- **Authentication:** Not required

#### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết hiện tại.
- **Query Parameters:**
  - `limit` (number - mặc định `3`) - Số lượng bài viết gợi ý.

#### 4. Response
- **Success (200):** Trả về mảng danh sách bài viết tương ứng.

---

### 7. Tải file lịch sự kiện (.ics)

#### 1. Mô tả nghiệp vụ
Tải tệp lịch chuẩn `.ics` chứa thông tin thời gian, địa điểm sự kiện đính kèm bài viết để người dùng thêm vào Google Calendar, Apple Calendar...

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/{slug}/calendar`
- **Authentication:** Not required

#### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết.

#### 4. Response
- **Success (200):** Tệp tin định dạng `text/calendar`.
- **Error Responses:**
  - `404 NOT FOUND` - Không tìm thấy bài viết hoặc bài viết không có hoạt động đính kèm.

---

### 8. Ghi nhận lượt xem bài viết (Track view)

#### 1. Mô tả nghiệp vụ
Tăng bộ đếm số lượt xem của bài viết lên 1 đơn vị khi có người dùng truy cập.

#### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/articles/{slug}/track-view`
- **Authentication:** Not required

#### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết.

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

---

### 9. Đăng ký danh sách chờ (Waitlist)

#### 1. Mô tả nghiệp vụ
Khi CTA đăng ký của hoạt động nội bộ đạt trạng thái `WAITLIST` (hết chỗ), học viên đăng nhập có thể đăng ký vào danh sách chờ để nhận thông báo khi có chỗ trống.

#### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/articles/{slug}/waitlist`
- **Authentication:** Required (Quyền: `STUDENT`)

#### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết.

#### 4. Response
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
  - `400 BAD REQUEST` - Hoạt động không ở trạng thái WAITLIST hoặc học viên đã đăng ký trước đó.

---

### 10. Lưu bài viết vào danh sách yêu thích (Wishlist)

#### 1. Mô tả nghiệp vụ
Lưu bài viết để học viên có thể xem lại sau này trong trang cá nhân.

#### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/articles/{slug}/wishlist`
- **Authentication:** Required (Quyền: `STUDENT`)

#### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết.

#### 4. Response
- **Success (201):**
  ```json
  {
    "code": 201,
    "message": "success",
    "data": null
  }
  ```

---

### 11. Hủy lưu bài viết khỏi danh sách yêu thích (Wishlist)

#### 1. Mô tả nghiệp vụ
Xóa bài viết khỏi danh sách yêu thích cá nhân.

#### 2. API Endpoint
- **Method:** `DELETE`
- **Path:** `/api/articles/{slug}/wishlist`
- **Authentication:** Required (Quyền: `STUDENT`)

#### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết.

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

---

### 12. Lấy danh sách bài viết yêu thích của bản thân

#### 1. Mô tả nghiệp vụ
Lấy danh sách phân trang các bài viết mà học viên đã lưu vào mục yêu thích.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/wishlist`
- **Authentication:** Required (Quyền: `STUDENT`)

#### 3. Request
- **Query Parameters:**
  - `page` (number - mặc định `0`)
  - `size` (number - mặc định `10`)

#### 4. Response
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
          "thumbnailUrl": "https://example.com/thumb.jpg",
          "seoDescription": "...",
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

### 13. Lấy danh sách bài viết thịnh hành (Trending)

#### 1. Mô tả nghiệp vụ
Lấy danh sách các bài viết có lượt xem cao nhất trong khoảng thời gian N ngày gần đây.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/trending`
- **Authentication:** Not required

#### 3. Request
- **Query Parameters:**
  - `days` (number - mặc định `7`) - Khoảng số ngày cần thống kê.
  - `limit` (number - mặc định `5`) - Giới hạn số bài viết trả về.

#### 4. Response
- **Success (200):** Trả về mảng danh sách bài viết thịnh hành.

---

### 14. Gửi bình luận / Phản hồi bình luận

#### 1. Mô tả nghiệp vụ
Sinh viên gửi bình luận trực tiếp cho bài viết, hoặc phản hồi (reply) bình luận khác bằng cách truyền `parentCommentId`. Hệ thống kiểm duyệt từ ngữ tục tĩu tự động (tiếng Anh + Việt). Nếu dính từ thô tục, bình luận sẽ được lưu kèm cờ `isFlagged = true` và `flagReason = "PROFANITY"` để chờ Admin xử trị.

#### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/articles/{slug}/comments`
- **Authentication:** Required (Quyền: `STUDENT`)

#### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết.
- **Request Body:**
  ```json
  {
    "content": "string - Nội dung bình luận (không trống)",
    "parentCommentId": "number - ID bình luận cha (tùy chọn, có thể null)"
  }
  ```

#### 4. Response
- **Success (201):**
  ```json
  {
    "code": 201,
    "message": "success",
    "data": {
      "id": 15,
      "articleId": 10,
      "parentCommentId": null,
      "content": "Bài viết này rất hay!",
      "isFlagged": false,
      "flagReason": null,
      "isHidden": false,
      "student": {
        "id": 3,
        "fullName": "Nguyễn Văn A",
        "studentCode": "SV12345",
        "avatarUrl": null
      },
      "replies": [],
      "createdAt": "2026-06-05T18:00:00",
      "updatedAt": "2026-06-05T18:00:00"
    }
  }
  ```

---

### 15. Lấy danh sách bình luận (Public)

#### 1. Mô tả nghiệp vụ
Lấy cây bình luận phân trang của bài viết (bình luận bị Admin ẩn sẽ tự động không hiển thị). Các bình luận phản hồi được lồng đệ quy trong danh sách `replies` của bình luận cha.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/{slug}/comments`
- **Authentication:** Not required

#### 3. Request
- **Path Parameters:**
  - `slug` (string) - Slug bài viết.
- **Query Parameters:**
  - `page` (number - mặc định `0`)
  - `size` (number - mặc định `10`)

#### 4. Response
- **Success (200):** Trả về trang chứa cấu trúc cây bình luận.

---

### 16. Xóa bình luận của bản thân

#### 1. Mô tả nghiệp vụ
Học viên xóa bình luận do chính mình viết. Hệ thống sẽ tự động thực hiện **Cascade Delete** để xóa toàn bộ các phản hồi con bên trong nó.

#### 2. API Endpoint
- **Method:** `DELETE`
- **Path:** `/api/articles/comments/{commentId}`
- **Authentication:** Required (Quyền: `STUDENT`)

#### 3. Request
- **Path Parameters:**
  - `commentId` (number) - ID bình luận cần xóa.

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

---

### 17. Thêm/Cập nhật tương tác Emoji (Reaction)

#### 1. Mô tả nghiệp vụ
Học viên thể hiện cảm xúc đối với bài viết bằng cách chọn 1 trong các loại emoji: `LIKE`, `LOVE`, `CLAP`, `FIRE`, `SUPPORT`. Mỗi học viên chỉ được tương tác tối đa 1 emoji mỗi bài viết (gửi yêu cầu mới sẽ cập nhật loại emoji cũ).

#### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/articles/{slug}/reaction`
- **Authentication:** Required (Quyền: `STUDENT`)

#### 3. Request
- **Path Parameters:**
  - `slug` (string)
- **Query Parameters:**
  - `type` (string - thuộc enum ReactionType, ví dụ `LOVE`) - Loại tương tác.

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

---

### 18. Xóa tương tác Emoji (Delete Reaction)

#### 1. Mô tả nghiệp vụ
Học viên thu hồi tương tác cảm xúc đã bày tỏ trên bài viết.

#### 2. API Endpoint
- **Method:** `DELETE`
- **Path:** `/api/articles/{slug}/reaction`
- **Authentication:** Required (Quyền: `STUDENT`)

#### 3. Request
- **Path Parameters:**
  - `slug` (string)

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

---

### 19. Lấy thống kê số lượt tương tác của bài viết

#### 1. Mô tả nghiệp vụ
Lấy tổng số lượng tương tác phân loại theo từng loại emoji trên bài viết.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/{slug}/reactions`
- **Authentication:** Not required

#### 3. Request
- **Path Parameters:**
  - `slug` (string)

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "LIKE": 15,
      "LOVE": 42,
      "CLAP": 8,
      "FIRE": 1,
      "SUPPORT": 2
    }
  }
  ```

---

### 20. Ghi nhận số lượt chia sẻ (Track share)

#### 1. Mô tả nghiệp vụ
Tăng bộ đếm lượt chia sẻ khi người dùng nhấp chọn nút share bài viết.

#### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/articles/{slug}/track-share`
- **Authentication:** Not required

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

---

### 21. Lấy danh sách lịch sử đọc của bản thân

#### 1. Mô tả nghiệp vụ
Hiển thị danh sách phân trang các bài viết học viên đã xem, được sắp xếp giảm dần theo thời gian xem gần đây nhất.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/history`
- **Authentication:** Required (Quyền: `STUDENT`)

#### 3. Request
- **Query Parameters:**
  - `page` (number - mặc định `0`)
  - `size` (number - mặc định `10`)

#### 4. Response
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
          "thumbnailUrl": "https://example.com/thumb.jpg",
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

---

### 22. Xóa một lịch sử đọc bài viết

#### 1. Mô tả nghiệp vụ
Xóa bỏ một bài viết khỏi lịch sử xem cá nhân của học viên qua ID lịch sử.

#### 2. API Endpoint
- **Method:** `DELETE`
- **Path:** `/api/articles/history/{historyId}`
- **Authentication:** Required (Quyền: `STUDENT`)

#### 3. Request
- **Path Parameters:**
  - `historyId` (number) - ID bản ghi lịch sử đọc.

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

---

### 23. Xóa toàn bộ lịch sử đọc bài viết

#### 1. Mô tả nghiệp vụ
Xóa sạch toàn bộ danh sách lịch sử đọc của học viên.

#### 2. API Endpoint
- **Method:** `DELETE`
- **Path:** `/api/articles/history`
- **Authentication:** Required (Quyền: `STUDENT`)

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

---

### 24. Lấy danh sách bài viết theo Chuỗi sự kiện (Series)

#### 1. Mô tả nghiệp vụ
Lấy danh sách các bài viết thuộc một chuỗi hoạt động/sự kiện cụ thể.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/articles/series/{seriesId}`
- **Authentication:** Not required

#### 3. Request
- **Path Parameters:**
  - `seriesId` (number) - ID của chuỗi sự kiện.

#### 4. Response
- **Success (200):** Trả về mảng danh sách bài viết thuộc chuỗi sự kiện.

---

## PHẦN B. API QUẢN TRỊ CMS (ADMIN & MANAGER APIS)

### 1. Lọc nâng cao danh sách bài viết CMS

#### 1. Mô tả nghiệp vụ
Quản trị viên lọc danh sách tất cả các bài viết (bao gồm bản nháp) qua bộ lọc đa điều kiện nâng cao (tiêu đề, trạng thái, danh mục, chuỗi sự kiện, loại bài viết, cờ đặc biệt, khoảng ngày).

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/admin/articles`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Query Parameters:**
  - `status` (string - `PUBLISHED` / `DRAFT` / `ALL`)
  - `activityId` (number)
  - `categoryId` (number)
  - `articleType` (string - enum ArticleType)
  - `featured` (boolean)
  - `pinned` (boolean)
  - `primary` (boolean)
  - `search` (string) - Tìm kiếm theo tiêu đề/nội dung.
  - `dateFrom` (string - `yyyy-MM-dd`)
  - `dateTo` (string - `yyyy-MM-dd`)
  - `page` (number - mặc định `0`)
  - `size` (number - mặc định `10`)

#### 4. Response
- **Success (200):** Trả về trang chứa danh sách bài viết dành cho Admin.

---

### 2. Chi tiết bài viết CMS

#### 1. Mô tả nghiệp vụ
Xem chi tiết đầy đủ thông tin bài viết ở tầng quản trị phục vụ cập nhật.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/admin/articles/{articleId}`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Path Parameters:**
  - `articleId` (number) - ID bài viết.

#### 4. Response
- **Success (200):** Trả về chi tiết bài viết CMS dạng `EventArticleAdminResponse`.

---

### 3. Lấy bài viết theo Activity ID

#### 1. Mô tả nghiệp vụ
Lấy danh sách các bài viết CMS đang liên kết với một Hoạt động cụ thể.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/admin/articles/by-activity/{activityId}`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Path Parameters:**
  - `activityId` (number)

#### 4. Response
- **Success (200):** Trả về mảng danh sách bài viết.

---

### 4. Tạo bài viết mới

#### 1. Mô tả nghiệp vụ
Tạo mới một bài viết. Cho phép bài viết độc lập không gắn hoạt động (`activityId` gửi `null`). Hỗ trợ chọn loại bài viết `articleType` và cài đặt cờ bài viết chính `isPrimary`.

#### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/admin/articles`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Request Body:**
  ```json
  {
    "activityId": "number - ID hoạt động liên kết (có thể null)",
    "articleType": "string - thuộc enum ArticleType",
    "isPrimary": "boolean - đánh dấu bài viết chính",
    "title": "string - tiêu đề bài viết",
    "slug": "string - đường dẫn tĩnh unique",
    "thumbnailUrl": "string - link ảnh đại diện",
    "content": "string - nội dung HTML",
    "seoTitle": "string",
    "seoDescription": "string",
    "categoryId": "number",
    "tagIds": "number[]",
    "isFeatured": "boolean",
    "isPinned": "boolean",
    "priority": "number"
  }
  ```

#### 4. Response
- **Success (201):** Trả về thông tin chi tiết bài viết mới tạo.

---

### 5. Cập nhật bài viết

#### 1. Mô tả nghiệp vụ
Cập nhật thông tin bài viết. Khi slug được cập nhật đổi sang giá trị mới, hệ thống tự động ghi nhận slug cũ vào lịch sử để thực hiện redirect cho người dùng ở FE.

#### 2. API Endpoint
- **Method:** `PUT`
- **Path:** `/api/admin/articles/{articleId}`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Path Parameters:**
  - `articleId` (number)
- **Request Body:** Tương tự API tạo bài viết mới.

#### 4. Response
- **Success (200):** Trả về thông tin bài viết đã chỉnh sửa thành công.

---

### 6. Publish bài viết

#### 1. Mô tả nghiệp vụ
Chuyển trạng thái bài viết từ bản nháp (Draft) sang xuất bản (Published) để hiển thị công khai. Đồng thời kích hoạt gửi thông báo tự động cho những học viên có bài viết này trong danh sách ước nguyện (Wishlist).

#### 2. API Endpoint
- **Method:** `PUT`
- **Path:** `/api/admin/articles/{articleId}/publish`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 4. Response
- **Success (200):** Trả về thông tin bài viết đã xuất bản.

---

### 7. Unpublish bài viết

#### 1. Mô tả nghiệp vụ
Hạ bài viết đã xuất bản về trạng thái bản nháp (Draft), ẩn khỏi giao diện học viên.

#### 2. API Endpoint
- **Method:** `PUT`
- **Path:** `/api/admin/articles/{articleId}/unpublish`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 4. Response
- **Success (200):** Trả về thông tin bài viết đã hạ trạng thái.

---

### 8. Thiết lập bài viết chính (Set Primary)

#### 1. Mô tả nghiệp vụ
Đặt một bài viết làm bài viết đại diện chính cho hoạt động được đính kèm. Bài viết cũ đang là primary sẽ tự động bị bỏ cờ.

#### 2. API Endpoint
- **Method:** `PUT`
- **Path:** `/api/admin/articles/{articleId}/set-primary`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 4. Response
- **Success (200):** Trả về thông tin bài viết đã cập nhật cờ `isPrimary: true`.

---

### 9. Thống kê Dashboard bài viết

#### 1. Mô tả nghiệp vụ
Lấy dữ liệu thống kê tổng số lượng bài viết, lượt xem, lượt yêu thích, bài viết nổi bật, biểu đồ bài viết theo danh mục và theo tháng phục vụ Dashboard CMS.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/admin/articles/statistics`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "totalArticles": 50,
      "publishedArticles": 42,
      "draftArticles": 8,
      "totalViews": 15400,
      "totalWishlists": 1200,
      "featuredArticles": 5,
      "pinnedArticles": 3,
      "topViewedArticles": [],
      "recentlyPublished": [],
      "articlesByCategory": {
        "Thể Thao": 15,
        "Học Thuật": 10
      },
      "articlesByMonth": {
        "2026-05": 12,
        "2026-06": 8
      }
    }
  }
  ```

---

### 10. Xuất báo cáo Excel bài viết

#### 1. Mô tả nghiệp vụ
Tải xuống báo cáo danh sách bài viết định dạng Excel (.xlsx) dựa theo bộ lọc CMS.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/admin/articles/export`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Query Parameters:** (Tương tự như API Lọc nâng cao bài viết CMS).

#### 4. Response
- **Success (200):** File Excel nhị phân.

---

### 11. Thêm ảnh vào Gallery của bài viết

#### 1. Mô tả nghiệp vụ
Thêm ảnh đính kèm vào thư viện ảnh chi tiết của bài viết.

#### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/admin/articles/{articleId}/images`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Path Parameters:**
  - `articleId` (number)
- **Request Body:**
  ```json
  {
    "imageUrl": "string - link ảnh (bắt buộc)",
    "caption": "string - chú thích ảnh",
    "displayOrder": "number - thứ tự hiển thị",
    "isCover": "boolean - ảnh làm ảnh cover"
  }
  ```

#### 4. Response
- **Success (201):** Trả về thông tin ảnh đã lưu thành công.

---

### 12. Xóa ảnh khỏi Gallery của bài viết

#### 1. Mô tả nghiệp vụ
Xóa bỏ một ảnh thuộc thư viện ảnh bài viết.

#### 2. API Endpoint
- **Method:** `DELETE`
- **Path:** `/api/admin/articles/{articleId}/images/{imageId}`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Path Parameters:**
  - `articleId` (number)
  - `imageId` (number)

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

---

### 13. Lấy danh sách danh mục (Categories)

#### 1. Mô tả nghiệp vụ
Lấy danh sách các danh mục phân loại bài viết phục vụ trang CMS.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/admin/articles/categories`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 4. Response
- **Success (200):** Trả về mảng danh mục bài viết.

---

### 14. Tạo danh mục mới

#### 1. Mô tả nghiệp vụ
Tạo mới một danh mục phân loại bài viết.

#### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/admin/articles/categories`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Request Body:**
  ```json
  {
    "name": "string - tên danh mục",
    "description": "string",
    "slug": "string",
    "displayOrder": "number",
    "isActive": "boolean"
  }
  ```

#### 4. Response
- **Success (201):** Trả về danh mục đã tạo.

---

### 15. Cập nhật danh mục

#### 1. Mô tả nghiệp vụ
Cập nhật thông tin danh mục bài viết.

#### 2. API Endpoint
- **Method:** `PUT`
- **Path:** `/api/admin/articles/categories/{categoryId}`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Path Parameters:**
  - `categoryId` (number)
- **Request Body:** Tương tự API tạo danh mục.

#### 4. Response
- **Success (200):** Trả về danh mục sau cập nhật.

---

### 16. Xóa danh mục bài viết

#### 1. Mô tả nghiệp vụ
Xóa bỏ một danh mục bài viết.

#### 2. API Endpoint
- **Method:** `DELETE`
- **Path:** `/api/admin/articles/categories/{categoryId}`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Path Parameters:**
  - `categoryId` (number)

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

---

### 17. Lấy danh sách thẻ (Tags)

#### 1. Mô tả nghiệp vụ
Lấy toàn bộ danh sách thẻ (Tags) bài viết hiện có.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/admin/articles/tags`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 4. Response
- **Success (200):** Trả về mảng danh sách thẻ.

---

### 18. Tạo thẻ (Tag) mới

#### 1. Mô tả nghiệp vụ
Tạo một thẻ từ khóa mới để gắn vào bài viết.

#### 2. API Endpoint
- **Method:** `POST`
- **Path:** `/api/admin/articles/tags`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Request Body:**
  ```json
  {
    "name": "string - tên thẻ",
    "slug": "string",
    "isActive": "boolean"
  }
  ```

#### 4. Response
- **Success (201):** Trả về thông tin thẻ đã tạo.

---

### 19. Xóa thẻ (Tag)

#### 1. Mô tả nghiệp vụ
Xóa thẻ từ khóa khỏi hệ thống.

#### 2. API Endpoint
- **Method:** `DELETE`
- **Path:** `/api/admin/articles/tags/{tagId}`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Path Parameters:**
  - `tagId` (number)

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

---

### 20. Lấy bình luận quản trị của bài viết

#### 1. Mô tả nghiệp vụ
Lấy cây bình luận đầy đủ của bài viết (bao gồm cả các bình luận bị ẩn `isHidden = true`) để Admin dễ kiểm duyệt và theo dõi.

#### 2. API Endpoint
- **Method:** `GET`
- **Path:** `/api/admin/articles/{articleId}/comments`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Path Parameters:**
  - `articleId` (number) - ID bài viết.
- **Query Parameters:**
  - `page` (number - mặc định `0`)
  - `size` (number - mặc định `10`)

#### 4. Response
- **Success (200):** Trả về đối tượng Page chứa cây bình luận của bài viết.

---

### 21. Ẩn bình luận (Moderation - Hide)

#### 1. Mô tả nghiệp vụ
Admin thực hiện ẩn một bình luận vi phạm khỏi hiển thị công khai.

#### 2. API Endpoint
- **Method:** `PUT`
- **Path:** `/api/admin/articles/comments/{commentId}/hide`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Path Parameters:**
  - `commentId` (number) - ID bình luận.

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 15,
      "content": "Nội dung vi phạm",
      "isHidden": true
    }
  }
  ```

---

### 22. Hiện bình luận (Moderation - Unhide)

#### 1. Mô tả nghiệp vụ
Khôi phục hiển thị bình luận bị ẩn trở lại công khai.

#### 2. API Endpoint
- **Method:** `PUT`
- **Path:** `/api/admin/articles/comments/{commentId}/unhide`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Path Parameters:**
  - `commentId` (number) - ID bình luận.

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 15,
      "content": "Nội dung bình luận",
      "isHidden": false
    }
  }
  ```

---

### 23. Xóa bình luận vĩnh viễn (Admin Delete)

#### 1. Mô tả nghiệp vụ
Admin thực hiện xóa hoàn toàn bình luận khỏi DB. Xóa bình luận cha sẽ kích hoạt Cascade Delete toàn bộ bình luận con liên quan.

#### 2. API Endpoint
- **Method:** `DELETE`
- **Path:** `/api/admin/articles/comments/{commentId}`
- **Authentication:** Required (Quyền: `ADMIN` hoặc `MANAGER`)

#### 3. Request
- **Path Parameters:**
  - `commentId` (number) - ID bình luận.

#### 4. Response
- **Success (200):**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```
