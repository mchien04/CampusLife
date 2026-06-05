CREATE TABLE article_reactions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    reaction_type VARCHAR(20) NOT NULL,   -- LIKE, LOVE, CLAP, FIRE, SUPPORT
    created_at DATETIME,
    UNIQUE KEY uk_article_student_reaction (article_id, student_id),
    CONSTRAINT fk_reaction_article FOREIGN KEY (article_id) REFERENCES event_articles(id) ON DELETE CASCADE,
    CONSTRAINT fk_reaction_student FOREIGN KEY (student_id) REFERENCES students(id)
);
