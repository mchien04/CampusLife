-- Thêm các trường mới cho event_articles
ALTER TABLE event_articles ADD COLUMN category_id BIGINT;
ALTER TABLE event_articles ADD COLUMN is_featured BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE event_articles ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE event_articles ADD COLUMN priority INT NOT NULL DEFAULT 0;
ALTER TABLE event_articles ADD COLUMN wishlist_count BIGINT NOT NULL DEFAULT 0;

-- Tạo bảng article_categories
CREATE TABLE article_categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    slug VARCHAR(100),
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME,
    CONSTRAINT fk_article_category FOREIGN KEY (category_id) REFERENCES event_articles(id)
);

-- Tạo bảng article_tags
CREATE TABLE article_tags (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME
);

-- Tạo bảng article_article_tags (many-to-many)
CREATE TABLE article_article_tags (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    CONSTRAINT fk_article_tags_article FOREIGN KEY (article_id) REFERENCES event_articles(id),
    CONSTRAINT fk_article_tags_tag FOREIGN KEY (tag_id) REFERENCES article_tags(id)
);

-- Tạo bảng article_images
CREATE TABLE article_images (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    caption VARCHAR(255),
    display_order INT NOT NULL DEFAULT 0,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME,
    CONSTRAINT fk_article_images FOREIGN KEY (article_id) REFERENCES event_articles(id)
);

-- Tạo bảng article_wishlists
CREATE TABLE article_wishlists (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    created_at DATETIME,
    UNIQUE KEY uk_article_student (article_id, student_id),
    CONSTRAINT fk_wishlist_article FOREIGN KEY (article_id) REFERENCES event_articles(id),
    CONSTRAINT fk_wishlist_student FOREIGN KEY (student_id) REFERENCES students(id)
);

-- Thêm khóa ngoại cho category_id
ALTER TABLE event_articles ADD CONSTRAINT fk_event_article_category FOREIGN KEY (category_id) REFERENCES article_categories(id);
