SET @col_type := (
    SELECT COLUMN_TYPE
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'preparation_tasks'
      AND COLUMN_NAME = 'status'
    LIMIT 1
);

SET @needs_alter := (
    SELECT CASE
        WHEN @col_type IS NULL THEN 0
        WHEN LOCATE('COMPLETION_REQUESTED', @col_type) > 0 THEN 0
        ELSE 1
    END
);

SET @sql := IF(
    @needs_alter = 1,
    'ALTER TABLE preparation_tasks MODIFY COLUMN status ENUM(''PENDING'',''ACCEPTED'',''COMPLETION_REQUESTED'',''COMPLETED'') NOT NULL',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

