# Báo cáo Triển khai Module EventArticle Nâng cao

## 1. Tổng quan Nghiệp vụ
Module **EventArticle** được thiết kế để cung cấp một hệ thống Landing Page chuyên nghiệp cho các sự kiện của sinh viên. Hệ thống không chỉ dừng lại ở việc hiển thị thông tin mà còn tối ưu hóa trải nghiệm người dùng (UX) và hiệu quả quản trị (CMS).

### Các thành phần chính:
- **Landing Page (Public):** Giao diện bài viết theo phong cách báo chí, hỗ trợ SEO, Rich Media (Ảnh, Video YouTube, Link nhúng).
- **CMS (Admin/Manager):** Quản lý tập trung bài viết, danh mục, thẻ, hình ảnh và theo dõi hiệu quả qua Dashboard thống kê.
- **Tính năng chuyển đổi:** Đăng ký danh sách chờ (Waitlist), Thêm vào lịch (.ics), Wishlist bài viết.

---

## 2. Các thay đổi về Database
Đã bổ sung các bảng và quan hệ mới (Xem chi tiết tại [V1020__enhance_event_articles_advanced.sql](file:///d:/2025-2026%20HKI/TLCN/campuslife/db/migration/V1020__enhance_event_articles_advanced.sql)):
- `event_articles`: Mở rộng thêm các flag `is_featured`, `is_pinned`, `priority`, `wishlist_count`, `category_id`.
- `article_categories`: Quản lý danh mục bài viết.
- `article_tags`: Quản lý thẻ tag.
- `article_images`: Quản lý bộ sưu tập ảnh cho bài viết (có caption và cover).
- `article_wishlists`: Lưu vết các bài viết yêu thích của sinh viên.
- `event_article_slug_history`: Lưu lịch sử slug để thực hiện redirect 301 tự động.

---

## 3. Hệ thống API

### A. Dành cho Sinh viên (Public/Authenticated)
| Method | Endpoint | Auth | Ghi chú |
| :--- | :--- | :--- | :--- |
| GET | `/api/articles` | No | Danh sách bài viết (Phân trang + Pinned + Priority) |
| GET | `/api/articles/{slug}` | Optional | Chi tiết bài viết (Kèm wishlist status nếu login) |
| GET | `/api/articles/featured` | No | Top các bài viết nổi bật |
| GET | `/api/articles/category/{slug}` | No | Lọc bài viết theo danh mục |
| GET | `/api/articles/search` | No | Tìm kiếm theo tiêu đề/nội dung |
| POST | `/api/articles/{slug}/wishlist` | Yes | Thêm vào danh sách yêu thích |
| GET | `/api/articles/wishlist` | Yes | Xem danh sách bài viết đã lưu |
| POST | `/api/articles/{slug}/track-view` | No | Tự động tăng lượt xem |
| GET | `/api/articles/{slug}/calendar` | No | Tải file .ics cho sự kiện |

### B. Dành cho Admin/Manager (CMS)
| Method | Endpoint | Quyền | Ghi chú |
| :--- | :--- | :--- | :--- |
| GET | `/api/admin/articles/statistics` | ADMIN | Dashboard: Tổng view, top bài viết, tỷ lệ category |
| POST | `/api/admin/articles` | ADMIN | Tạo bài viết mới (gắn với Activity) |
| PUT | `/api/admin/articles/{id}/publish` | ADMIN | Công bố bài viết ra public |
| POST | `/api/admin/articles/{id}/images` | ADMIN | Thêm ảnh vào gallery bài viết |
| CRUD | `/api/admin/articles/categories` | ADMIN | Quản lý danh mục |

---

## 4. Logic Nghiệp vụ Nâng cao

### Dynamic CTA (Nút Đăng ký động)
Backend trả về `registrationStatus` với các trạng thái:
- `UPCOMING`: "Sắp mở đăng ký" (Vô hiệu hóa nút).
- `OPEN`: "Đăng ký ngay" (Nút nổi bật, dẫn tới link đăng ký).
- `WAITLIST`: "Đăng ký danh sách chờ" (Gọi API Waitlist).
- `CLOSED`: "Đã đóng đăng ký" (Ẩn hoặc mờ nút).

### Rich Content Support
Nội dung bài viết (`content`) hỗ trợ HTML. FE nên sử dụng một trình soạn thảo tiptap để:
- Nhúng ảnh với mô tả (Caption).
- Nhúng Video YouTube (Iframe).
- Nhúng link văn bản thay thế (Google Doc style).

---

## 5. Hướng dẫn Triển khai Frontend

### Giao diện Sinh viên
1. **Trang chủ/Danh sách:** Hiển thị dạng Grid/Card với thumbnail sinh động. Ưu tiên hiển thị các bài `isPinned` lên đầu.
2. **Trang chi tiết:** 
   - Render HTML từ `content`.
   - Hiển thị Sidebar với "Sự kiện liên quan" (API `/related`).
   - Nút Wishlist (Trái tim) để lưu bài viết.
   - Nút "Thêm vào lịch" cạnh nút "Đăng ký".
3. **Lịch cá nhân:** Tích hợp API `/api/registrations/personal-calendar?from=&to=` — dùng `markedDates` để chấm ngày, `events` (+ `date=`) khi chọn một ngày. Hướng dẫn FE gộp: [fe-article-and-calendar-update-guide.md](./fe-article-and-calendar-update-guide.md).

### Giao diện Admin (CMS)
1. **Dashboard:** Vẽ biểu đồ từ dữ liệu API `/statistics` (Sử dụng Chart.js hoặc Recharts).
2. **Editor:** Trang soạn thảo bài viết tập trung. Cho phép xem trước (Preview) bài viết trước khi Publish.
3. **Gallery:** Quản lý ảnh bài viết, cho phép chọn ảnh Cover.

---