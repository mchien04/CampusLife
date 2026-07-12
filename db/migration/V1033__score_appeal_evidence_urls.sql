-- V1033: Evidence image URLs on score appeals (comma-separated relative paths)

ALTER TABLE score_appeals
    ADD COLUMN IF NOT EXISTS evidence_urls TEXT NULL;
