ALTER TABLE court_case ADD COLUMN updated_at timestamp with time zone;
ALTER TABLE court_case ADD COLUMN updated_by VARCHAR;