-- Migration SQL for Phase 7: PrepSupervisor and Task Completion Proof Photos

-- 1. Add is_prep_supervisor column to activity_organizers table
ALTER TABLE activity_organizers
ADD COLUMN is_prep_supervisor BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Add completion_proof_urls column to preparation_tasks table
ALTER TABLE preparation_tasks
ADD COLUMN completion_proof_urls TEXT NULL;
