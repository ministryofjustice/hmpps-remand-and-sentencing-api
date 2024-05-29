ALTER TABLE court_appearance ADD COLUMN overall_sentence_length_id int references period_length(id);
ALTER TABLE next_court_appearance ADD COLUMN appearance_time time;
ALTER TABLE period_length ALTER COLUMN years TYPE int,
ALTER COLUMN months TYPE int,
ALTER COLUMN weeks TYPE int,
ALTER COLUMN days TYPE int;