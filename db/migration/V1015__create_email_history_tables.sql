-- Tạo bảng email_history
CREATE TABLE IF NOT EXISTS email_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    is_html BOOLEAN NOT NULL DEFAULT FALSE,
    recipient_type VARCHAR(50) NOT NULL,
    recipient_filter TEXT NULL COMMENT 'JSON string chứa thông tin filter',
    attachment_count INT DEFAULT 0,
    sent_at DATETIME NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT NULL,
    notification_created BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (recipient_id) REFERENCES users(id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_recipient_id (recipient_id),
    INDEX idx_sent_at (sent_at),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo bảng email_attachments
CREATE TABLE IF NOT EXISTS email_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email_history_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (email_history_id) REFERENCES email_history(id) ON DELETE CASCADE,
    INDEX idx_email_history_id (email_history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

