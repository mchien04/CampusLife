CREATE TABLE article_comments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    parent_comment_id BIGINT NULL,          -- null = root comment, not null = reply
    content TEXT NOT NULL,
    is_flagged BOOLEAN NOT NULL DEFAULT FALSE,   -- contains profanity
    flag_reason VARCHAR(255) NULL,               -- reason e.g. "PROFANITY"
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,    -- hidden by admin
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_comment_article   FOREIGN KEY (article_id)       REFERENCES event_articles(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_student   FOREIGN KEY (student_id)       REFERENCES students(id),
    CONSTRAINT fk_comment_parent    FOREIGN KEY (parent_comment_id) REFERENCES article_comments(id) ON DELETE CASCADE,
    INDEX idx_comment_article (article_id),
    INDEX idx_comment_student (student_id)
);
