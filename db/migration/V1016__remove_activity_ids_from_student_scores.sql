-- Remove activity_ids column from student_scores table
-- This field was not being properly updated and is not needed for statistics

ALTER TABLE student_scores DROP COLUMN IF EXISTS activity_ids;

