-- Thêm cột series_id vào bảng activity_registrations để đánh dấu đăng ký theo chuỗi
ALTER TABLE activity_registrations
ADD COLUMN series_id BIGINT NULL AFTER student_id;

-- (Tuỳ chọn) Thêm foreign key nếu cần, có thể bỏ comment nếu DB đã sẵn sàng
-- ALTER TABLE activity_registrations
-- ADD CONSTRAINT fk_activity_registrations_series
--     FOREIGN KEY (series_id) REFERENCES activity_series(id);


