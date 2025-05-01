
-- created_prison
ALTER TABLE recall RENAME COLUMN created_by_prison TO created_prison;
ALTER TABLE recall_sentence RENAME COLUMN created_by_prison TO created_prison;
ALTER TABLE recall ALTER COLUMN created_prison DROP NOT NULL;
ALTER TABLE recall_sentence ALTER COLUMN created_prison DROP NOT NULL;

-- status_id
ALTER TABLE recall ADD COLUMN status_id smallint NOT NULL default 0;

-- updated_at
ALTER TABLE recall ADD COLUMN updated_at timestamp with time zone;

-- updated_by
ALTER TABLE recall ADD COLUMN updated_by VARCHAR;

-- updated_prison
ALTER TABLE recall ADD COLUMN updated_prison VARCHAR;