ALTER TABLE court_appearance DROP COLUMN appearance_uuid;
ALTER TABLE court_appearance RENAME COLUMN lifetime_uuid TO appearance_uuid;
ALTER TABLE court_appearance ADD COLUMN updated_at timestamp with time zone;
ALTER TABLE court_appearance ADD COLUMN updated_by VARCHAR;
ALTER TABLE court_appearance ADD COLUMN updated_prison VARCHAR;
