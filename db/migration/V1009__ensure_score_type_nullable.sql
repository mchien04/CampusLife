-- Đảm bảo cột score_type và type cho phép null cho activities trong series
-- Migration này đảm bảo rằng các cột này có thể null
-- KHÔNG CẦN DROP BẢNG - chỉ thay đổi constraint, không xóa dữ liệu

-- Sửa cột score_type để cho phép NULL
-- Nếu cột đã cho phép NULL, lệnh này vẫn chạy được (idempotent)
ALTER TABLE activities
MODIFY COLUMN score_type VARCHAR(50) NULL COMMENT 'Kiểu tính điểm (null = lấy từ series)';

-- Sửa cột type để cho phép NULL
-- Nếu cột đã cho phép NULL, lệnh này vẫn chạy được (idempotent)
ALTER TABLE activities
MODIFY COLUMN type VARCHAR(50) NULL COMMENT 'Loại hoạt động (null = lấy từ series)';

