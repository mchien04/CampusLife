-- V1027: Backend audit improvements - indexes, preset tracking, async recalculation

-- Task 2: Score history pagination index
CREATE INDEX IF NOT EXISTS idx_score_entries_student_semester_type
    ON score_entries(student_id, semester_id, score_type, status, created_at);

-- Task 6: Statistics source-type breakdown indexes
CREATE INDEX IF NOT EXISTS idx_score_entries_source_type
    ON score_entries(source_type, status, semester_id);

CREATE INDEX IF NOT EXISTS idx_score_entries_created_at
    ON score_entries(student_id, semester_id, created_at);

-- Task 4: Track preset-generated rules
ALTER TABLE activity_score_rules
    ADD COLUMN IF NOT EXISTS is_preset_generated BOOLEAN NOT NULL DEFAULT FALSE;

-- Task 7b: Async recalculation job tracking
CREATE TABLE IF NOT EXISTS recalculation_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_students INT NOT NULL,
    processed_students INT DEFAULT 0,
    error_count INT DEFAULT 0,
    error_details TEXT,
    started_at DATETIME,
    completed_at DATETIME,
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
