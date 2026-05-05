-- Thêm cột view_count vào event_articles
ALTER TABLE event_articles ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;

-- Tạo bảng lưu lịch sử slug để redirect
CREATE TABLE event_article_slug_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    old_slug VARCHAR(255) NOT NULL UNIQUE,
    created_at DATETIME,
    CONSTRAINT fk_slug_history_article FOREIGN KEY (article_id) REFERENCES event_articles(id)
);

-- Cập nhật RegistrationStatus enum trong DB (nếu là MySQL/MariaDB thì thường không cần, nhưng nếu là PostgreSQL thì cần)
-- Ở đây giả định MySQL/MariaDB dùng VARCHAR cho enum nên không cần alter type.
