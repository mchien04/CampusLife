SET @col_exists_req := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fund_advances'
      AND COLUMN_NAME = 'requested_by_id'
);

SET @sql_req := IF(
    @col_exists_req > 0,
    'SELECT 1',
    'ALTER TABLE fund_advances ADD COLUMN requested_by_id BIGINT NULL'
);

PREPARE stmt_req FROM @sql_req;
EXECUTE stmt_req;
DEALLOCATE PREPARE stmt_req;

SET @col_exists_decided := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fund_advances'
      AND COLUMN_NAME = 'decided_by_id'
);

SET @sql_decided := IF(
    @col_exists_decided > 0,
    'SELECT 1',
    'ALTER TABLE fund_advances ADD COLUMN decided_by_id BIGINT NULL'
);

PREPARE stmt_decided FROM @sql_decided;
EXECUTE stmt_decided;
DEALLOCATE PREPARE stmt_decided;

SET @col_exists_decided_at := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fund_advances'
      AND COLUMN_NAME = 'decided_at'
);

SET @sql_decided_at := IF(
    @col_exists_decided_at > 0,
    'SELECT 1',
    'ALTER TABLE fund_advances ADD COLUMN decided_at DATETIME NULL'
);

PREPARE stmt_decided_at FROM @sql_decided_at;
EXECUTE stmt_decided_at;
DEALLOCATE PREPARE stmt_decided_at;

SET @col_exists_cat := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fund_advances'
      AND COLUMN_NAME = 'category_id'
);

SET @sql_cat := IF(
    @col_exists_cat > 0,
    'SELECT 1',
    'ALTER TABLE fund_advances ADD COLUMN category_id BIGINT NULL'
);

PREPARE stmt_cat FROM @sql_cat;
EXECUTE stmt_cat;
DEALLOCATE PREPARE stmt_cat;

ALTER TABLE fund_advances
    MODIFY COLUMN status ENUM('REQUESTED','HOLDING','SETTLED','REJECTED') NOT NULL;

UPDATE fund_advances fa
JOIN preparation_tasks t ON t.id = fa.task_id
JOIN activity_budgets ab ON ab.activity_id = t.activity_id
JOIN budget_categories bc ON bc.activity_budget_id = ab.id AND bc.name = 'Khác'
SET fa.category_id = bc.id
WHERE fa.category_id IS NULL;

SET @fk_exists_req := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fund_advances'
      AND CONSTRAINT_NAME = 'fk_fund_advances_requested_by'
);

SET @sql_fk_req := IF(
    @fk_exists_req > 0,
    'SELECT 1',
    'ALTER TABLE fund_advances ADD CONSTRAINT fk_fund_advances_requested_by FOREIGN KEY (requested_by_id) REFERENCES students(id)'
);

PREPARE stmt_fk_req FROM @sql_fk_req;
EXECUTE stmt_fk_req;
DEALLOCATE PREPARE stmt_fk_req;

SET @fk_exists_decided := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fund_advances'
      AND CONSTRAINT_NAME = 'fk_fund_advances_decided_by'
);

SET @sql_fk_decided := IF(
    @fk_exists_decided > 0,
    'SELECT 1',
    'ALTER TABLE fund_advances ADD CONSTRAINT fk_fund_advances_decided_by FOREIGN KEY (decided_by_id) REFERENCES users(id)'
);

PREPARE stmt_fk_decided FROM @sql_fk_decided;
EXECUTE stmt_fk_decided;
DEALLOCATE PREPARE stmt_fk_decided;

SET @fk_exists_cat := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fund_advances'
      AND CONSTRAINT_NAME = 'fk_fund_advances_category'
);

SET @sql_fk_cat := IF(
    @fk_exists_cat > 0,
    'SELECT 1',
    'ALTER TABLE fund_advances ADD CONSTRAINT fk_fund_advances_category FOREIGN KEY (category_id) REFERENCES budget_categories(id)'
);

PREPARE stmt_fk_cat FROM @sql_fk_cat;
EXECUTE stmt_fk_cat;
DEALLOCATE PREPARE stmt_fk_cat;
