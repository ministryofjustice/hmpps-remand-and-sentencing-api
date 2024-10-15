ALTER TABLE period_length ADD COLUMN period_length_type VARCHAR;
ALTER TABLE period_length ADD COLUMN sentence_id int references sentence(id);
ALTER TABLE period_length ADD COLUMN appearance_id int references court_appearance(id);
ALTER TABLE court_appearance DROP COLUMN overall_sentence_length_id;
ALTER TABLE sentence DROP COLUMN custodial_length_id;
ALTER TABLE sentence DROP COLUMN extended_licence_length_id;