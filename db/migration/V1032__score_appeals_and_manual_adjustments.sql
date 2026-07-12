-- V1032: Score appeals workflow + manual score adjustments metadata

CREATE TABLE IF NOT EXISTS manual_score_adjustments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    score_type VARCHAR(50) NOT NULL,
    points DECIMAL(19, 2) NOT NULL,
    reason TEXT NOT NULL,
    activity_id BIGINT NULL,
    created_by_user_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_msa_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_msa_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
    CONSTRAINT fk_msa_activity FOREIGN KEY (activity_id) REFERENCES activities(id),
    CONSTRAINT fk_msa_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_msa_student_semester
    ON manual_score_adjustments(student_id, semester_id);

CREATE TABLE IF NOT EXISTS score_appeals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    score_type VARCHAR(50) NOT NULL,
    related_score_entry_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    reason TEXT NOT NULL,
    requested_points DECIMAL(19, 2) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decision_notes TEXT NULL,
    decided_at DATETIME NULL,
    decided_by_id BIGINT NULL,
    resulting_score_entry_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sa_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_sa_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
    CONSTRAINT fk_sa_related_entry FOREIGN KEY (related_score_entry_id) REFERENCES score_entries(id),
    CONSTRAINT fk_sa_resulting_entry FOREIGN KEY (resulting_score_entry_id) REFERENCES score_entries(id),
    CONSTRAINT fk_sa_decided_by FOREIGN KEY (decided_by_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_sa_student_status
    ON score_appeals(student_id, status);

CREATE INDEX IF NOT EXISTS idx_sa_status_created
    ON score_appeals(status, created_at);

CREATE INDEX IF NOT EXISTS idx_sa_semester_status
    ON score_appeals(semester_id, status);

CREATE TABLE IF NOT EXISTS score_appeal_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    appeal_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sam_appeal FOREIGN KEY (appeal_id) REFERENCES score_appeals(id),
    CONSTRAINT fk_sam_sender FOREIGN KEY (sender_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_sam_appeal_created
    ON score_appeal_messages(appeal_id, created_at);
