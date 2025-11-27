-- Đổi cột deadline từ DATE sang DATETIME để lưu cả giờ
ALTER TABLE activity_tasks
MODIFY COLUMN deadline DATETIME NULL COMMENT 'Hạn chót nộp bài (có cả giờ)';

