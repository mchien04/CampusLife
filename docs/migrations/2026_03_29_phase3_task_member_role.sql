SET @col_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'preparation_task_members'
      AND COLUMN_NAME = 'role'
);

SET @sql := IF(
    @col_exists > 0,
    'SELECT 1',
    'ALTER TABLE preparation_task_members ADD COLUMN role ENUM(''LEADER'',''MEMBER'') NOT NULL DEFAULT ''MEMBER'''
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO preparation_task_members (task_id, student_id, role, created_at)
SELECT t.id, t.assignee_id, 'LEADER', NOW()
FROM preparation_tasks t
WHERE NOT EXISTS (
    SELECT 1
    FROM preparation_task_members m
    WHERE m.task_id = t.id
      AND m.student_id = t.assignee_id
);

