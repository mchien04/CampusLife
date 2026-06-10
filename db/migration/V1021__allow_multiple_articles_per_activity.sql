-- Drop the existing foreign key constraint
ALTER TABLE event_articles DROP FOREIGN KEY fk_event_articles_activity;

-- Drop the unique index/constraint on activity_id
ALTER TABLE event_articles DROP INDEX activity_id;

-- Make activity_id nullable
ALTER TABLE event_articles MODIFY COLUMN activity_id BIGINT NULL;

-- Recreate foreign key constraint without UNIQUE, setting activity_id to NULL on delete
ALTER TABLE event_articles ADD CONSTRAINT fk_event_articles_activity
    FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE SET NULL;

-- Add article_type and is_primary columns
ALTER TABLE event_articles ADD COLUMN article_type VARCHAR(30) NOT NULL DEFAULT 'ANNOUNCEMENT' AFTER activity_id;
ALTER TABLE event_articles ADD COLUMN is_primary BOOLEAN NOT NULL DEFAULT FALSE AFTER article_type;

-- Create an index on activity_id for performance
CREATE INDEX idx_event_articles_activity_id ON event_articles(activity_id);
