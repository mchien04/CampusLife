-- V1028: Department scope foundation for manager multi-tenancy
-- Expand-only migration: add nullable scope/snapshot structures first, then backfill.
-- MySQL-compatible DDL (Flyway runs each script once).

CREATE TABLE IF NOT EXISTS user_departments (
    user_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by_user_id BIGINT NULL,
    PRIMARY KEY (user_id, department_id),
    CONSTRAINT fk_user_departments_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_departments_department
        FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_departments_assigned_by
        FOREIGN KEY (assigned_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

DROP INDEX IF EXISTS idx_user_departments_user ON user_departments;
CREATE INDEX idx_user_departments_user
    ON user_departments(user_id);

DROP INDEX IF EXISTS idx_user_departments_dept ON user_departments;
CREATE INDEX idx_user_departments_dept
    ON user_departments(department_id);

ALTER TABLE activity_registrations
    ADD COLUMN student_department_id_at_registration BIGINT NULL;

ALTER TABLE activity_participations
    ADD COLUMN student_department_id_at_participation BIGINT NULL;

ALTER TABLE score_entries
    ADD COLUMN student_department_id_at_award BIGINT NULL;

ALTER TABLE student_scores
    ADD COLUMN student_department_id_at_award BIGINT NULL;

DROP INDEX IF EXISTS idx_activity_registrations_student_dept_snapshot
    ON activity_registrations;
CREATE INDEX idx_activity_registrations_student_dept_snapshot
    ON activity_registrations(student_department_id_at_registration);

DROP INDEX IF EXISTS idx_activity_participations_student_dept_snapshot
    ON activity_participations;
CREATE INDEX idx_activity_participations_student_dept_snapshot
    ON activity_participations(student_department_id_at_participation);

DROP INDEX IF EXISTS idx_score_entries_student_dept_snapshot
    ON score_entries;
CREATE INDEX idx_score_entries_student_dept_snapshot
    ON score_entries(student_department_id_at_award);

DROP INDEX IF EXISTS idx_student_scores_student_dept_snapshot
    ON student_scores;
CREATE INDEX idx_student_scores_student_dept_snapshot
    ON student_scores(student_department_id_at_award);

SET SQL_SAFE_UPDATES = 0;

UPDATE activity_registrations ar
INNER JOIN students s ON ar.student_id = s.id
SET ar.student_department_id_at_registration = s.department_id
WHERE ar.student_department_id_at_registration IS NULL
  AND s.department_id IS NOT NULL;

UPDATE activity_participations ap
INNER JOIN activity_registrations ar ON ap.registration_id = ar.id
LEFT JOIN students s ON ar.student_id = s.id
SET ap.student_department_id_at_participation = COALESCE(
    ar.student_department_id_at_registration,
    s.department_id
)
WHERE ap.student_department_id_at_participation IS NULL
  AND COALESCE(ar.student_department_id_at_registration, s.department_id) IS NOT NULL;

UPDATE score_entries se
INNER JOIN students s ON se.student_id = s.id
SET se.student_department_id_at_award = s.department_id
WHERE se.student_department_id_at_award IS NULL
  AND s.department_id IS NOT NULL;

UPDATE student_scores ss
INNER JOIN students s ON ss.student_id = s.id
SET ss.student_department_id_at_award = s.department_id
WHERE ss.student_department_id_at_award IS NULL
  AND s.department_id IS NOT NULL;
