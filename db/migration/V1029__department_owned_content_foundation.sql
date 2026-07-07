-- V1029: Add department ownership metadata for content/campaign scope.
-- Expand-only: nullable columns and join tables so existing data keeps working.
-- MySQL-compatible DDL (Flyway runs each script once).

ALTER TABLE event_articles
    ADD COLUMN owner_department_id BIGINT NULL;

CREATE INDEX idx_event_articles_owner_department
    ON event_articles(owner_department_id);

ALTER TABLE email_history
    ADD COLUMN sender_department_id BIGINT NULL,
    ADD COLUMN recipient_department_id_at_send BIGINT NULL;

CREATE INDEX idx_email_history_sender_department
    ON email_history(sender_department_id);

CREATE INDEX idx_email_history_recipient_department
    ON email_history(recipient_department_id_at_send);

CREATE TABLE IF NOT EXISTS email_history_target_departments (
    email_history_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    PRIMARY KEY (email_history_id, department_id),
    CONSTRAINT fk_email_history_target_departments_email
        FOREIGN KEY (email_history_id) REFERENCES email_history(id) ON DELETE CASCADE,
    CONSTRAINT fk_email_history_target_departments_department
        FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE RESTRICT
);

CREATE INDEX idx_email_history_target_dept
    ON email_history_target_departments(department_id);

ALTER TABLE notifications
    ADD COLUMN sender_department_id BIGINT NULL;

CREATE INDEX idx_notifications_sender_department
    ON notifications(sender_department_id);

CREATE TABLE IF NOT EXISTS notification_target_departments (
    notification_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    PRIMARY KEY (notification_id, department_id),
    CONSTRAINT fk_notification_target_departments_notification
        FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_target_departments_department
        FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE RESTRICT
);

CREATE INDEX idx_notification_target_dept
    ON notification_target_departments(department_id);
