-- Thêm cột is_completed vào bảng task_submissions để hỗ trợ chấm điểm đạt/không đạt
ALTER TABLE task_submissions
ADD COLUMN is_completed TINYINT(1) NULL COMMENT 'Đạt/Không đạt';

-- Cập nhật các bản ghi đã có: nếu score > 0 thì is_completed = 1, nếu score < 0 thì is_completed = 0
UPDATE task_submissions
SET is_completed = CASE 
    WHEN score > 0 THEN 1
    WHEN score < 0 THEN 0
    ELSE NULL
END
WHERE score IS NOT NULL AND status = 'GRADED';

