-- Convert date columns to datetime and add flags for Activity

-- Note: Adjust syntax per your DB (MySQL example below)

ALTER TABLE activities
    MODIFY COLUMN start_date DATETIME NULL,
    MODIFY COLUMN end_date DATETIME NULL,
    MODIFY COLUMN registration_start_date DATETIME NULL,
    MODIFY COLUMN registration_deadline DATETIME NULL;

ALTER TABLE activities
    ADD COLUMN is_draft TINYINT(1) NOT NULL DEFAULT 1 AFTER is_important,
    ADD COLUMN requires_approval TINYINT(1) NOT NULL DEFAULT 1 AFTER contact_info;

-- Backfill existing rows: published activities
UPDATE activities SET is_draft = 0 WHERE is_draft IS NULL;
UPDATE activities SET requires_approval = 1 WHERE requires_approval IS NULL;


