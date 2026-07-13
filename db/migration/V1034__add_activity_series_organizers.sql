-- Series-level organizers (khoa tổ chức). Child activities inherit and sync from series.

CREATE TABLE IF NOT EXISTS activity_series_organizers (
    series_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    PRIMARY KEY (series_id, department_id),
    CONSTRAINT fk_activity_series_organizers_series
        FOREIGN KEY (series_id) REFERENCES activity_series(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_series_organizers_department
        FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Khoa tổ chức của chuỗi sự kiện';

CREATE INDEX idx_activity_series_organizers_department
    ON activity_series_organizers(department_id);

-- Backfill: union of child activity organizers for existing series
INSERT IGNORE INTO activity_series_organizers (series_id, department_id)
SELECT DISTINCT a.series_id, ad.department_id
FROM activities a
JOIN activity_departments ad ON ad.activity_id = a.id
WHERE a.series_id IS NOT NULL
  AND a.is_deleted = false;
