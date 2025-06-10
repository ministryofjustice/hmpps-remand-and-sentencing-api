ALTER TABLE court_appearance ADD COLUMN source varchar NOT NULL DEFAULT 'NOMIS';
ALTER TABLE court_appearance_history ADD COLUMN source varchar NOT NULL DEFAULT 'NOMIS';
