ALTER TABLE sentence ADD COLUMN sentence_type_id int references sentence_type(id);
ALTER TABLE sentence DROP COLUMN sentence_type;