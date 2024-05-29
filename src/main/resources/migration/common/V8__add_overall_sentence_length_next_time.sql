ALTER TABLE court_appearance ADD COLUMN overall_sentence_length_id int references period_length(id);
ALTER TABLE next_court_appearance ADD COLUMN appearance_time time;