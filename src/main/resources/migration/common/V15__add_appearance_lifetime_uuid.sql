ALTER TABLE court_appearance ADD COLUMN lifetime_uuid UUID NOT NULL DEFAULT gen_random_uuid();