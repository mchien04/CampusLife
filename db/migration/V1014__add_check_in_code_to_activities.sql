ALTER TABLE activities
ADD COLUMN check_in_code VARCHAR(50) NULL UNIQUE COMMENT 'Mã QR code unique để check-in nhanh';

