SET @col_exists_desc := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'allocation_adjustment_requests'
      AND COLUMN_NAME = 'description'
);

SET @sql_desc := IF(
    @col_exists_desc > 0,
    'SELECT 1',
    'ALTER TABLE allocation_adjustment_requests ADD COLUMN description VARCHAR(500) NOT NULL DEFAULT \"\"'
);

PREPARE stmt_desc FROM @sql_desc;
EXECUTE stmt_desc;
DEALLOCATE PREPARE stmt_desc;

