-- Add is_edited and edited_at to article_comments
ALTER TABLE article_comments
ADD COLUMN is_edited BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN edited_at TIMESTAMP;
