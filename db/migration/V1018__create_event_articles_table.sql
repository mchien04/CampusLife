CREATE TABLE event_articles (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    thumbnail_url VARCHAR(500),
    content TEXT NOT NULL,
    seo_title VARCHAR(255),
    seo_description VARCHAR(500),
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at DATETIME,
    activity_id BIGINT NOT NULL UNIQUE,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_event_articles_activity
        FOREIGN KEY (activity_id) REFERENCES activities(id)
);
