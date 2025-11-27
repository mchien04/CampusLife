-- Tạo bảng activity_series để quản lý chuỗi sự kiện
CREATE TABLE activity_series (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Khóa chính',
    name VARCHAR(255) NOT NULL COMMENT 'Tên chuỗi sự kiện',
    description TEXT NULL COMMENT 'Mô tả chuỗi sự kiện',
    milestone_points TEXT NULL COMMENT 'JSON: {"3": 5, "4": 7, "5": 10} - Mốc điểm thưởng',
    score_type VARCHAR(50) NOT NULL COMMENT 'Loại điểm để cộng milestone (REN_LUYEN, CONG_TAC_XA_HOI, etc.)',
    main_activity_id BIGINT NULL COMMENT 'Activity chính (có thể null)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Ngày tạo',
    FOREIGN KEY (main_activity_id) REFERENCES activities(id) ON DELETE SET NULL,
    INDEX idx_main_activity (main_activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chuỗi sự kiện';

-- Tạo bảng student_series_progress để theo dõi tiến độ sinh viên
CREATE TABLE student_series_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Khóa chính',
    student_id BIGINT NOT NULL COMMENT 'ID sinh viên',
    series_id BIGINT NOT NULL COMMENT 'ID chuỗi sự kiện',
    completed_activity_ids TEXT NULL COMMENT 'JSON array: [1,3,5] - Danh sách activityId đã tham gia',
    completed_count INT NOT NULL DEFAULT 0 COMMENT 'Số sự kiện đã tham gia',
    points_earned DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT 'Điểm đã nhận từ milestone',
    last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Ngày cập nhật',
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (series_id) REFERENCES activity_series(id) ON DELETE CASCADE,
    UNIQUE KEY uk_student_series (student_id, series_id),
    INDEX idx_student_series (student_id, series_id),
    INDEX idx_completed_count (completed_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tiến độ sinh viên trong chuỗi sự kiện';

-- Thêm cột series_id và series_order vào bảng activities
ALTER TABLE activities
ADD COLUMN series_id BIGINT NULL COMMENT 'ID chuỗi sự kiện (null = sự kiện đơn lẻ)' AFTER is_deleted,
ADD COLUMN series_order INT NULL COMMENT 'Thứ tự trong chuỗi (1, 2, 3...)' AFTER series_id,
ADD INDEX idx_series_id (series_id),
ADD FOREIGN KEY (series_id) REFERENCES activity_series(id) ON DELETE SET NULL;

