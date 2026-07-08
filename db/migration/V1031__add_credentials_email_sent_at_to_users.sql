-- Track when student account credentials email was successfully sent
ALTER TABLE users
    ADD COLUMN credentials_email_sent_at TIMESTAMP NULL;

CREATE INDEX idx_users_credentials_email_sent_at
    ON users (credentials_email_sent_at);
