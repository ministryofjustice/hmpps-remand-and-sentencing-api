drop index idx_sentence_status_id;
drop index idx_court_case_status_id;
drop index idx_charge_status_id;
alter table period_length drop constraint unique_uuid_status_sentence;
drop index idx_court_appearance_status_id_date;
drop index idx_court_appearance_status_id;

ALTER TABLE court_case RENAME COLUMN status_id to status_id_old;
ALTER TABLE court_case_history RENAME COLUMN status_id to status_id_old;
ALTER TABLE court_appearance RENAME COLUMN status_id to status_id_old;
ALTER TABLE court_appearance_history RENAME COLUMN status_id to status_id_old;
ALTER TABLE charge RENAME COLUMN status_id to status_id_old;
ALTER TABLE charge_history RENAME COLUMN status_id to status_id_old;
ALTER TABLE sentence RENAME COLUMN status_id to status_id_old;
ALTER TABLE sentence_history RENAME COLUMN status_id to status_id_old;
ALTER TABLE period_length RENAME COLUMN status_id to status_id_old;
ALTER TABLE period_length_history RENAME COLUMN status_id to status_id_old;

ALTER TABLE court_case ALTER COLUMN status_id_old SET DEFAULT 100;
ALTER TABLE court_case_history ALTER COLUMN status_id_old SET DEFAULT 100;
ALTER TABLE court_appearance ALTER COLUMN status_id_old SET DEFAULT 100;
ALTER TABLE court_appearance_history ALTER COLUMN status_id_old SET DEFAULT 100;
ALTER TABLE charge ALTER COLUMN status_id_old SET DEFAULT 100;
ALTER TABLE charge_history ALTER COLUMN status_id_old SET DEFAULT 100;
ALTER TABLE sentence ALTER COLUMN status_id_old SET DEFAULT 100;
ALTER TABLE sentence_history ALTER COLUMN status_id_old SET DEFAULT 100;
ALTER TABLE period_length ALTER COLUMN status_id_old SET DEFAULT 100;
ALTER TABLE period_length_history ALTER COLUMN status_id_old SET DEFAULT 100;


ALTER TABLE court_case ADD COLUMN status_id VARCHAR;
ALTER TABLE court_case_history ADD COLUMN status_id VARCHAR;
ALTER TABLE court_appearance ADD COLUMN status_id VARCHAR;
ALTER TABLE court_appearance_history ADD COLUMN status_id VARCHAR;
ALTER TABLE charge ADD COLUMN status_id VARCHAR;
ALTER TABLE charge_history ADD COLUMN status_id VARCHAR;
ALTER TABLE sentence ADD COLUMN status_id VARCHAR;
ALTER TABLE sentence_history ADD COLUMN status_id VARCHAR;
ALTER TABLE period_length ADD COLUMN status_id VARCHAR;
ALTER TABLE period_length_history ADD COLUMN status_id VARCHAR;

CREATE INDEX idx_sentence_status_id ON sentence USING btree (status_id);
CREATE INDEX idx_court_case_status_id ON court_case USING btree (status_id);
CREATE INDEX idx_charge_status_id ON charge USING btree (status_id);
CREATE INDEX idx_court_appearance_status_id_date ON court_appearance USING btree (appearance_date, status_id);
CREATE INDEX idx_court_appearance_status_id ON court_appearance USING btree (status_id);
