-- V1030: Performance indexes for department scope queries (Phase 4)
-- MySQL-compatible DDL (Flyway runs each script once).

CREATE INDEX idx_activity_departments_dept_activity
    ON activity_departments(department_id, activity_id);

CREATE INDEX idx_students_department
    ON students(department_id);
