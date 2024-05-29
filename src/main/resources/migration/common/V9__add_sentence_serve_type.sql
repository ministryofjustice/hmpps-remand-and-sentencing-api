ALTER TABLE sentence ADD COLUMN sentence_serve_type varchar NOT NULL,
ADD COLUMN consecutive_to_id int references sentence(id),
ADD COLUMN sentence_type varchar NOT NULL;