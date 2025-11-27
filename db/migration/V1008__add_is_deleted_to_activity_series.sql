-- Thêm hoặc sửa cột is_deleted vào bảng activity_series
-- Migration này xử lý cả 2 trường hợp: cột đã tồn tại hoặc chưa tồn tại

-- Từ lỗi ban đầu, cột is_deleted đã tồn tại nhưng không có default value
-- Nên migration này sẽ MODIFY cột để thêm default value

-- Nếu cột chưa tồn tại, migration này sẽ fail
-- Trong trường hợp đó, cần chạy migration V1008b để ADD cột

ALTER TABLE activity_series
MODIFY COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Cờ xóa mềm';

