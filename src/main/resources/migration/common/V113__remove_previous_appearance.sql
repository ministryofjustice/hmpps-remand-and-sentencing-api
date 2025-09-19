DROP INDEX IF EXISTS idx_court_appearance_previous_appearance_id;

ALTER TABLE court_appearance DROP COLUMN IF EXISTS previous_appearance_id;

ALTER TABLE court_appearance_history DROP COLUMN IF EXISTS previous_appearance_id;
