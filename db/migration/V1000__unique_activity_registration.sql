-- Add unique constraint on activity_registrations(activity_id, student_id)
-- to prevent duplicate registrations

-- First, remove any existing duplicates (keep the most recent one)
-- This is a safety measure before adding the unique constraint
DELETE ar1 FROM activity_registrations ar1
INNER JOIN activity_registrations ar2
WHERE ar1.id > ar2.id
AND ar1.activity_id = ar2.activity_id
AND ar1.student_id = ar2.student_id;

-- Add unique index to prevent duplicates
ALTER TABLE activity_registrations
ADD UNIQUE INDEX uk_activity_registration (activity_id, student_id);

