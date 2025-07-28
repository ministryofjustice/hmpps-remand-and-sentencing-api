-- Add unique constraint to prevent duplicate active recalls per prisoner
-- This enforces the find-or-create pattern at the database level
CREATE UNIQUE INDEX recall_unique_active_prisoner_idx 
ON recall (prisoner_id) 
WHERE status_id = 0; -- 0 represents EntityStatus.ACTIVE in the enum