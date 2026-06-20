-- Create activity_score_rules table
CREATE TABLE IF NOT EXISTS activity_score_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    score_type VARCHAR(50) NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,
    calculation VARCHAR(50) NOT NULL,
    points DECIMAL(10, 2) NOT NULL DEFAULT 0,
    fail_points DECIMAL(10, 2) NOT NULL DEFAULT 0,
    audience VARCHAR(50) NOT NULL,
    semester_policy VARCHAR(50) NOT NULL,
    explicit_semester_id BIGINT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (activity_id) REFERENCES activities(id),
    FOREIGN KEY (explicit_semester_id) REFERENCES semesters(id)
);

-- Create activity_score_rule_departments table
CREATE TABLE IF NOT EXISTS activity_score_rule_departments (
    rule_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    PRIMARY KEY (rule_id, department_id),
    FOREIGN KEY (rule_id) REFERENCES activity_score_rules(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
);

-- Create score_entries ledger
CREATE TABLE IF NOT EXISTS score_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    score_type VARCHAR(50) NOT NULL,
    activity_id BIGINT,
    rule_id BIGINT,
    source_type VARCHAR(50) NOT NULL,
    source_id BIGINT NOT NULL,
    points DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason TEXT,
    created_by_user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (semester_id) REFERENCES semesters(id),
    FOREIGN KEY (activity_id) REFERENCES activities(id),
    FOREIGN KEY (rule_id) REFERENCES activity_score_rules(id),
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

-- Add indexes for score_entries for performance
CREATE INDEX idx_score_entries_student_semester ON score_entries(student_id, semester_id, score_type, status);
CREATE INDEX idx_score_entries_source ON score_entries(source_type, source_id);
CREATE INDEX idx_score_entries_activity_rule ON score_entries(activity_id, rule_id);
