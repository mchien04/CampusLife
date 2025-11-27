-- Thêm các cột quy định chung cho chuỗi sự kiện
ALTER TABLE activity_series
ADD COLUMN registration_start_date DATETIME NULL COMMENT 'Ngày mở đăng ký tham gia chuỗi' AFTER score_type,
ADD COLUMN registration_deadline DATETIME NULL COMMENT 'Hạn chót đăng ký tham gia chuỗi' AFTER registration_start_date,
ADD COLUMN requires_approval BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Đăng ký có cần duyệt hay không' AFTER registration_deadline,
ADD COLUMN ticket_quantity INT NULL COMMENT 'Số lượng vé/slot có thể đăng ký (null = không giới hạn)' AFTER requires_approval;

