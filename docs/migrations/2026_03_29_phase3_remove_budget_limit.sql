SET @col_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'preparation_tasks'
      AND COLUMN_NAME = 'budget_limit'
);

SET @sql := IF(
    @col_exists > 0,
    'ALTER TABLE preparation_tasks DROP COLUMN budget_limit',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

