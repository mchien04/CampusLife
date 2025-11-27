-- Cho phép null cho type và scoreType khi activity thuộc series
ALTER TABLE activities
MODIFY COLUMN type VARCHAR(50) NULL COMMENT 'Loại hoạt động (null = lấy từ series)',
MODIFY COLUMN score_type VARCHAR(50) NULL COMMENT 'Kiểu tính điểm (null = lấy từ series)';

