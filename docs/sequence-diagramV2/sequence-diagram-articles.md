# Sequence Diagram — Event Articles Module (Bài viết sự kiện)

Hệ thống: **CampusLife** (Spring Boot + React)

---

## 1. Tạo / Xuất bản bài viết (H.30)

```mermaid
sequenceDiagram
    autonumber
    participant A as Admin/Manager
    participant C as Client (React)
    participant AC as AdminArticleController
    participant AS as ArticleService
    participant AR as ArticleRepository
    participant NS as NotificationService
    participant NR as NotificationRepository
    participant DB as Database
    participant FS as FileStorage

    Note over A, FS: === TẠO BÀI VIẾT (POST /api/admin/articles) ===

    A->>C: Nhập form: title, content, slug, coverImage, activityId, tags
    C->>AC: POST /api/admin/articles<br/>Body: {title, content, slug, coverImage, activityId, tags}
    AC->>AS: createArticle(dto)
    AS->>AR: existsBySlug(slug)
    AR->>DB: SELECT slug FROM articles WHERE slug = ?
    DB-->>AR: ResultSet
    AR-->>AS: true / false

    alt Slug đã tồn tại
        AS-->>AC: Throw DuplicateSlugException
        AC-->>C: 409 CONFLICT<br/>{error: "Slug already exists"}
        C-->>A: Hiển thị lỗi slug trùng
    else Slug chưa tồn tại
        AS->>FS: uploadCoverImage(coverImage) [nếu có]
        FS-->>AS: coverImageUrl
        AS->>AR: save(articleEntity)
        AR->>DB: INSERT INTO articles (...)
        DB-->>AR: articleId (generated)
        AR-->>AS: ArticleEntity
        AS-->>AC: ArticleResponseDTO
        AC-->>C: 201 CREATED<br/>{articleId, title, slug, status: "DRAFT"}
        C-->>A: Hiển thị "Tạo bài viết thành công"
    end

    Note over A, FS: === XUẤT BẢN (PUT /api/admin/articles/{id}/publish) ===

    A->>C: Nhấn "Xuất bản"
    C->>AC: PUT /api/admin/articles/{id}/publish
    AC->>AS: publishArticle(id)
    AS->>AR: findById(id)
    AR->>DB: SELECT * FROM articles WHERE id = ?
    DB-->>AR: ResultSet
    AR-->>AS: Optional<ArticleEntity>

    alt Article không tồn tại
        AS-->>AC: Throw ArticleNotFoundException
        AC-->>C: 404 NOT FOUND
        C-->>A: Hiển thị lỗi không tìm thấy bài viết
    else Article tồn tại
        AS->>AS: article.setPublished(true)<br/>article.setPublishedAt(Now)
        AS->>AR: save(article)
        AR->>DB: UPDATE articles SET published = true, published_at = ? ...
        DB-->>AR: updated
        AR-->>AS: ArticleEntity

        alt Article liên kết với Activity
            AS->>AS: activityId = article.getActivityId()
            AS->>NS: notifyActivityParticipants(activityId, article)
            NS->>NR: findParticipantsByActivityId(activityId)
            NR->>DB: SELECT student_id FROM activity_participants WHERE activity_id = ?
            DB-->>NR: List<studentId>
            NR-->>NS: List<Student>
            loop Mỗi participant
                NS->>NR: save(notification)
                NR->>DB: INSERT INTO notifications (user_id, type, content, article_id) ...
                DB-->>NR: notificationId
            end
            NR-->>NS: saved
            NS-->>AS: notificationSent
        end

        AS-->>AC: ArticleResponseDTO (published=true)
        AC-->>C: 200 OK<br/>{articleId, title, slug, published: true, publishedAt}
        C-->>A: Hiển thị "Xuất bản thành công"
    end
```

**Tóm tắt:**
- Admin/Manager tạo bài viết với slug, nếu slug trùng thì báo lỗi.
- FileStorage xử lý upload ảnh bìa nếu có.
- ArticleRepository lưu draft vào Database.
- Khi xuất bản, cập nhật `published=true` và `publishedAt=now`.
- Nếu bài viết liên kết với activity, NotificationService gửi thông báo cho tất cả participants.

---

## 2. Xem bài viết & Tìm kiếm (H.31)

```mermaid
sequenceDiagram
    autonumber
    participant U as Student/Admin
    participant C as Client (React)
    participant GC as GuestArticleController
    participant AS as ArticleService
    participant AR as ArticleRepository
    participant UR as UserRepository
    participant DB as Database

    Note over U, DB: === XEM CHI TIẾT BÀI VIẾT (GET /api/articles/{slug}) ===

    U->>C: Truy cập URL /articles/{slug}
    C->>GC: GET /api/articles/{slug}
    GC->>AS: getArticleBySlug(slug)
    AS->>AR: findBySlugAndPublishedTrue(slug)
    AR->>DB: SELECT * FROM articles WHERE slug = ? AND published = true
    DB-->>AR: ResultSet
    AR-->>AS: Optional<ArticleEntity>

    alt Article không tồn tại hoặc chưa publish
        AS-->>GC: Throw ArticleNotFoundException
        GC-->>C: 404 NOT FOUND<br/>{error: "Article not found"}
        C-->>U: Hiển thị trang 404 hoặc "Bài viết không tồn tại"
    else Article tồn tại
        AS->>AS: article.setViewCount(article.getViewCount() + 1)
        AS->>AR: save(article) [async/batch]
        AR->>DB: UPDATE articles SET view_count = view_count + 1 WHERE id = ?
        DB-->>AR: updated
        AS->>UR: findById(article.getAuthorId())
        UR->>DB: SELECT * FROM users WHERE id = ?
        DB-->>UR: ResultSet
        UR-->>AS: UserEntity
        AS-->>GC: ArticleDetailDTO (article + author info)
        GC-->>C: 200 OK<br/>{articleId, title, content, viewCount, author: {name, avatar}, ...}
        C-->>U: Hiển thị bài viết chi tiết
    end

    Note over U, DB: === TÌM KIẾM BÀI VIẾT (GET /api/articles?search=&tag=&page=) ===

    U->>C: Nhập từ khóa / chọn tag / chuyển trang
    C->>GC: GET /api/articles?search={keyword}&tag={tag}&page={page}&size={size}
    GC->>AS: searchArticles(search, tag, pageable)
    AS->>AR: findByTitleContainingOrContentContainingOrTagsContaining(search, search, search, pageable)<br/>[nếu có tag thì thêm AND tags LIKE %tag%]
    AR->>DB: SELECT * FROM articles WHERE published = true AND (title LIKE ? OR content LIKE ? OR tags LIKE ?) LIMIT ? OFFSET ?
    DB-->>AR: ResultSet
    AR-->>AS: Page<ArticleEntity>
    AS-->>GC: Page<ArticleSummaryDTO>
    GC-->>C: 200 OK<br/>{content: [...], totalElements, totalPages, currentPage}
    C-->>U: Hiển thị danh sách bài viết kèm phân trang
```

**Tóm tắt:**
- Xem chi tiết: Tìm article theo slug với `published=true`, tăng `viewCount` (async/batch), lấy thông tin author và trả về chi tiết.
- Tìm kiếm: Tìm theo title/content/tags với pagination, chỉ trả về các bài viết đã xuất bản.

---

## 3. Bình luận bài viết (H.32)

```mermaid
sequenceDiagram
    autonumber
    participant S as Student
    participant C as Client (React)
    participant GC as GuestArticleController
    participant CS as CommentService
    participant AS as ArticleService
    participant AR as ArticleRepository
    participant CR as CommentRepository
    participant FS as FilterService
    participant NS as NotificationService
    participant NR as NotificationRepository
    participant DB as Database

    Note over S, DB: === BÌNH LUẬN BÀI VIẾT (POST /api/articles/{slug}/comments) ===

    S->>C: Nhập nội dung comment (hoặc reply với parentId)
    C->>GC: POST /api/articles/{slug}/comments<br/>Body: {content, parentId?}
    GC->>CS: createComment(slug, studentId, dto)
    CS->>AS: findBySlugAndPublishedTrue(slug)
    AS->>AR: findBySlugAndPublishedTrue(slug)
    AR->>DB: SELECT * FROM articles WHERE slug = ? AND published = true
    DB-->>AR: ResultSet
    AR-->>AS: Optional<ArticleEntity>
    AS-->>CS: ArticleEntity

    alt Article không tồn tại hoặc chưa publish
        CS-->>GC: Throw ArticleNotFoundException
        GC-->>C: 404 NOT FOUND
        C-->>S: Hiển thị lỗi không tìm thấy bài viết
    else Article tồn tại
        CS->>FS: filterContent(content) [Spam/Bad words check]
        FS-->>CS: FilterResult (isClean / blockedWords)

        alt Nội dung vi phạm
            CS-->>GC: Throw ContentNotAllowedException
            GC-->>C: 400 BAD REQUEST<br/>{error: "Content contains inappropriate words"}
            C-->>S: Hiển thị lỗi nội dung không hợp lệ
        else Nội dung hợp lệ
            CS->>CS: Tạo CommentEntity<br/>(articleId, studentId, content, parentId, createdAt)
            CS->>CR: save(comment)
            CR->>DB: INSERT INTO comments (article_id, student_id, content, parent_id, created_at) ...
            DB-->>CR: commentId
            CR-->>CS: CommentEntity
            CS->>CR: findByArticleIdOrderByCreatedAtDesc(articleId)
            CR->>DB: SELECT * FROM comments WHERE article_id = ? ORDER BY created_at DESC
            DB-->>CR: List<CommentEntity>
            CR-->>CS: List<CommentEntity>
            CS-->>GC: CommentResponseDTO (comment + list)

            alt Comment không phải của author (gửi notification)
                CS->>NS: notifyAuthor(article.getAuthorId(), comment)
                NS->>NR: save(notification)
                NR->>DB: INSERT INTO notifications (user_id, type, content, article_id, comment_id) ...
                DB-->>NR: notificationId
                NR-->>NS: NotificationEntity
                NS-->>CS: notificationSent
            end

            GC-->>C: 201 CREATED<br/>{commentId, content, studentId, createdAt, parentId}
            C-->>S: Hiển thị comment mới trong danh sách
        end
    end
```

**Tóm tắt:**
- Student nhập comment → kiểm tra article tồn tại và đã publish.
- FilterService kiểm tra spam/bad words.
- Tạo CommentEntity (có hỗ trợ reply qua parentId) và lưu vào Database.
- Gửi notification cho author của bài viết khi có comment mới (nếu comment không phải của author).

---

## 4. Thêm / Xóa yêu thích (Wishlist) (H.33)

```mermaid
sequenceDiagram
    autonumber
    participant S as Student
    participant C as Client (React)
    participant GC as GuestArticleController
    participant WS as WishlistService
    participant AS as ArticleService
    participant AR as ArticleRepository
    participant WR as ArticleWishlistRepository
    participant DB as Database

    Note over S, DB: === THÊM YÊU THÍCH (POST /api/articles/{slug}/wishlist) ===

    S->>C: Click "Yêu thích" (heart icon)
    C->>GC: POST /api/articles/{slug}/wishlist
    GC->>WS: addToWishlist(slug, studentId)
    WS->>AS: findBySlug(slug)
    AS->>AR: findBySlug(slug)
    AR->>DB: SELECT * FROM articles WHERE slug = ?
    DB-->>AR: ResultSet
    AR-->>AS: Optional<ArticleEntity>
    AS-->>WS: ArticleEntity

    alt Article không tồn tại
        WS-->>GC: Throw ArticleNotFoundException
        GC-->>C: 404 NOT FOUND
        C-->>S: Hiển thị lỗi không tìm thấy bài viết
    else Article tồn tại
        WS->>WR: existsByArticleIdAndStudentId(articleId, studentId)
        WR->>DB: SELECT * FROM article_wishlists WHERE article_id = ? AND student_id = ?
        DB-->>WR: ResultSet
        WR-->>WS: true / false

        alt Đã có trong wishlist
            WS-->>GC: Throw AlreadyWishlistedException
            GC-->>C: 409 CONFLICT<br/>{error: "Already in wishlist"}
            C-->>S: Icon heart đã active, không thay đổi
        else Chưa có trong wishlist
            WS->>WS: Tạo ArticleWishlistEntity<br/>(articleId, studentId, createdAt)
            WS->>WR: save(wishlist)
            WR->>DB: INSERT INTO article_wishlists (article_id, student_id, created_at) ...
            DB-->>WR: wishlistId
            WR-->>WS: ArticleWishlistEntity
            WS-->>GC: WishlistResponseDTO
            GC-->>C: 201 CREATED<br/>{wishlistId, articleId, studentId, createdAt}
            C-->>S: Icon heart active (đỏ), hiển thị "Đã thêm vào yêu thích"
        end
    end

    Note over S, DB: === XÓA YÊU THÍCH (DELETE /api/articles/{slug}/wishlist) ===

    S->>C: Click "Bỏ yêu thích" (heart icon active)
    C->>GC: DELETE /api/articles/{slug}/wishlist
    GC->>WS: removeFromWishlist(slug, studentId)
    WS->>AS: findBySlug(slug)
    AS->>AR: findBySlug(slug)
    AR->>DB: SELECT * FROM articles WHERE slug = ?
    DB-->>AR: ResultSet
    AR-->>AS: Optional<ArticleEntity>
    AS-->>WS: ArticleEntity

    alt Article không tồn tại
        WS-->>GC: Throw ArticleNotFoundException
        GC-->>C: 404 NOT FOUND
        C-->>S: Hiển thị lỗi
    else Article tồn tại
        WS->>WR: findByArticleIdAndStudentId(articleId, studentId)
        WR->>DB: SELECT * FROM article_wishlists WHERE article_id = ? AND student_id = ?
        DB-->>WR: ResultSet
        WR-->>WS: Optional<ArticleWishlistEntity>

        alt Không tồn tại trong wishlist
            WS-->>GC: Throw WishlistNotFoundException
            GC-->>C: 404 NOT FOUND<br/>{error: "Not in wishlist"}
            C-->>S: Icon heart inactive, không thay đổi
        else Tồn tại trong wishlist
            WS->>WR: delete(wishlist)
            WR->>DB: DELETE FROM article_wishlists WHERE id = ?
            DB-->>WR: deleted
            WR-->>WS: void
            WS-->>GC: void
            GC-->>C: 204 NO CONTENT
            C-->>S: Icon heart inactive, hiển thị "Đã bỏ yêu thích"
        end
    end
```

**Tóm tắt:**
- Thêm: Kiểm tra article tồn tại, kiểm tra chưa wishlist → tạo ArticleWishlistEntity và save.
- Xóa: Kiểm tra article tồn tại, kiểm tra đã wishlist → delete khỏi Database.

---

## 5. Theo dõi lượt xem & Phản ứng (H.34)

```mermaid
sequenceDiagram
    autonumber
    participant U as Student/Admin
    participant C as Client (React)
    participant GC as GuestArticleController
    participant AS as ArticleService
    participant RS as ReactionService
    participant AR as ArticleRepository
    participant RR as ArticleReactionRepository
    participant DB as Database

    Note over U, DB: === THEO DÕI LƯỢT XEM (GET /api/articles/{slug}) ===

    U->>C: Truy cập bài viết /articles/{slug}
    C->>GC: GET /api/articles/{slug}
    GC->>AS: getArticleBySlug(slug)
    AS->>AR: findBySlugAndPublishedTrue(slug)
    AR->>DB: SELECT * FROM articles WHERE slug = ? AND published = true
    DB-->>AR: ResultSet
    AR-->>AS: Optional<ArticleEntity>

    alt Article không tồn tại
        AS-->>GC: Throw ArticleNotFoundException
        GC-->>C: 404 NOT FOUND
        C-->>U: Hiển thị lỗi
    else Article tồn tại
        AS->>AS: article.setViewCount(viewCount + 1)
        AS->>AR: save(article) [Async hoặc Batch]
        AR->>DB: UPDATE articles SET view_count = view_count + 1 WHERE id = ?
        DB-->>AR: updated
        AS-->>GC: ArticleDetailDTO
        GC-->>C: 200 OK<br/>{article + viewCount}
        C-->>U: Hiển thị bài viết + viewCount cập nhật
    end

    Note over U, DB: === PHẢN ỨNG (REACTION) (POST /api/articles/{slug}/reactions) ===

    U->>C: Chọn reaction (LIKE, LOVE, HAHA, WOW, SAD, ANGRY)
    C->>GC: POST /api/articles/{slug}/reactions<br/>Body: {type: "LIKE"}
    GC->>RS: reactToArticle(slug, studentId, type)
    RS->>AS: findBySlug(slug)
    AS->>AR: findBySlug(slug)
    AR->>DB: SELECT * FROM articles WHERE slug = ?
    DB-->>AR: ResultSet
    AR-->>AS: Optional<ArticleEntity>
    AS-->>RS: ArticleEntity

    alt Article không tồn tại
        RS-->>GC: Throw ArticleNotFoundException
        GC-->>C: 404 NOT FOUND
        C-->>U: Hiển thị lỗi
    else Article tồn tại
        RS->>RR: findByArticleIdAndStudentIdAndType(articleId, studentId, type)
        RR->>DB: SELECT * FROM article_reactions WHERE article_id = ? AND student_id = ? AND type = ?
        DB-->>RR: ResultSet
        RR-->>RS: Optional<ArticleReactionEntity>

        alt Đã react cùng type
            RS-->>GC: Throw DuplicateReactionException
            GC-->>C: 409 CONFLICT<br/>{error: "Already reacted with this type"}
            C-->>U: Giữ nguyên trạng thái reaction
        else Chưa react cùng type
            RS->>RR: findByArticleIdAndStudentId(articleId, studentId) [Tìm reaction khác type]
            RR->>DB: SELECT * FROM article_reactions WHERE article_id = ? AND student_id = ?
            DB-->>RR: ResultSet
            RR-->>RS: Optional<ArticleReactionEntity>

            alt Đã react type khác
                RS->>RS: reaction.setType(newType)
                RS->>RR: save(reaction)
                RR->>DB: UPDATE article_reactions SET type = ? WHERE id = ?
                DB-->>RR: updated
            else Chưa react bao giờ
                RS->>RS: Tạo ArticleReactionEntity<br/>(articleId, studentId, type, createdAt)
                RS->>RR: save(reaction)
                RR->>DB: INSERT INTO article_reactions (article_id, student_id, type, created_at) ...
                DB-->>RR: reactionId
            end
            RR-->>RS: ArticleReactionEntity
            RS->>RR: countByArticleIdAndType(articleId, type)
            RR->>DB: SELECT COUNT(*) FROM article_reactions WHERE article_id = ? AND type = ?
            DB-->>RR: count
            RR-->>RS: count
            RS-->>GC: ReactionResponseDTO<br/>{reactionId, type, count}
            GC-->>C: 200 OK / 201 CREATED<br/>{reactionId, type, count, articleId}
            C-->>U: Hiển thị reaction đã chọn + count cập nhật
        end
    end
```

**Tóm tắt:**
- View: Khi GET article, tăng `viewCount` (+1) và save async/batch.
- Reaction: POST reaction type, kiểm tra article tồn tại, kiểm tra chưa react cùng type.
- Nếu đã react type khác → update type; nếu chưa react → tạo mới.
- Trả về reaction count theo type.

---

## Tóm tắt Thành phần và Chức năng

| Thành phần | Vai trò | Chức năng chính |
|------------|---------|-----------------|
| **Admin/Manager** | Actor | Quản lý bài viết: tạo, xuất bản, chỉnh sửa. |
| **Student** | Actor | Đọc bài viết, bình luận, yêu thích, phản ứng. |
| **Client (React)** | Frontend | Hiển thị UI, gọi API, xử lý trạng thái. |
| **AdminArticleController** | Controller | Xử lý các endpoint quản trị: tạo, xuất bản bài viết. |
| **GuestArticleController** | Controller | Xử lý các endpoint công khai: xem, tìm kiếm, bình luận, yêu thích, reaction. |
| **ArticleService** | Service | Business logic cho article: CRUD, publish, view count, tìm kiếm. |
| **CommentService** | Service | Business logic cho comment: tạo, kiểm tra spam, reply. |
| **WishlistService** | Service | Business logic cho wishlist: thêm, xóa, kiểm tra tồn tại. |
| **ReactionService** | Service | Business logic cho reaction: tạo, update, kiểm tra duplicate, đếm. |
| **NotificationService** | Service | Gửi thông báo: khi publish article liên kết activity, khi có comment mới. |
| **FilterService** | Service | Kiểm tra nội dung comment: spam, bad words. |
| **ArticleRepository** | Repository | Truy vấn Database cho bảng `articles`. |
| **CommentRepository** | Repository | Truy vấn Database cho bảng `comments`. |
| **ArticleWishlistRepository** | Repository | Truy vấn Database cho bảng `article_wishlists`. |
| **ArticleReactionRepository** | Repository | Truy vấn Database cho bảng `article_reactions`. |
| **NotificationRepository** | Repository | Truy vấn Database cho bảng `notifications`. |
| **UserRepository** | Repository | Truy vấn Database cho bảng `users` (lấy author info). |
| **Database** | Database | Lưu trữ dữ liệu: articles, comments, wishlists, reactions, notifications. |
| **FileStorage** | External Service | Lưu trữ file upload: ảnh bìa bài viết. |

---

## Mối quan hệ giữa các Sequence

| Sequence | Tương tác với | Mô tả |
|----------|---------------|-------|
| Tạo / Xuất bản (H.30) | Database, FileStorage, NotificationService | Tạo draft → publish → gửi notification. |
| Xem & Tìm kiếm (H.31) | Database | Đọc article published, tăng viewCount. |
| Bình luận (H.32) | Database, FilterService, NotificationService | Comment hợp lệ → notify author. |
| Wishlist (H.33) | Database | Thêm / xóa wishlist. |
| View & Reaction (H.34) | Database | Increment viewCount + CRUD reaction. |
