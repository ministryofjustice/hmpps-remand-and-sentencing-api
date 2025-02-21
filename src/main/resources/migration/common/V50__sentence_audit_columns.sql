ALTER TABLE sentence DROP COLUMN sentence_uuid;
ALTER TABLE sentence RENAME COLUMN lifetime_sentence_uuid TO sentence_uuid;
ALTER TABLE sentence ADD COLUMN updated_at timestamp with time zone;
ALTER TABLE sentence ADD COLUMN updated_by VARCHAR;
ALTER TABLE sentence ADD COLUMN updated_prison VARCHAR;
