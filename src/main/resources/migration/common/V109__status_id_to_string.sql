ALTER TABLE court_case ADD COLUMN entity_status VARCHAR;
ALTER TABLE court_case_history ADD COLUMN entity_status VARCHAR;
ALTER TABLE court_appearance ADD COLUMN entity_status VARCHAR;
ALTER TABLE court_appearance_history ADD COLUMN entity_status VARCHAR;
ALTER TABLE charge ADD COLUMN entity_status VARCHAR;
ALTER TABLE charge_history ADD COLUMN entity_status VARCHAR;
ALTER TABLE sentence ADD COLUMN entity_status VARCHAR;
ALTER TABLE sentence_history ADD COLUMN entity_status VARCHAR;
ALTER TABLE period_length ADD COLUMN entity_status VARCHAR;
ALTER TABLE period_length_history ADD COLUMN entity_status VARCHAR;

UPDATE court_case SET entity_status = 'ACTIVE' WHERE status_id=0;
UPDATE court_case SET entity_status = 'INACTIVE' WHERE status_id=1;
UPDATE court_case SET entity_status = 'DELETED' WHERE status_id=3;
UPDATE court_case SET entity_status = 'MERGED' WHERE status_id=6;
UPDATE court_case SET entity_status = 'DUPLICATE' WHERE status_id=8;

UPDATE court_case_history SET entity_status = 'ACTIVE' WHERE status_id=0;
UPDATE court_case_history SET entity_status = 'INACTIVE' WHERE status_id=1;
UPDATE court_case_history SET entity_status = 'DELETED' WHERE status_id=3;
UPDATE court_case_history SET entity_status = 'MERGED' WHERE status_id=6;
UPDATE court_case_history SET entity_status = 'DUPLICATE' WHERE status_id=8;

UPDATE court_appearance SET entity_status = 'ACTIVE' WHERE status_id=0;
UPDATE court_appearance SET entity_status = 'DELETED' WHERE status_id=3;
UPDATE court_appearance SET entity_status = 'FUTURE' WHERE status_id=5;
UPDATE court_appearance SET entity_status = 'DUPLICATE' WHERE status_id=8;

UPDATE court_appearance_history SET entity_status = 'ACTIVE' WHERE status_id=0;
UPDATE court_appearance_history SET entity_status = 'DELETED' WHERE status_id=3;
UPDATE court_appearance_history SET entity_status = 'FUTURE' WHERE status_id=5;
UPDATE court_appearance_history SET entity_status = 'DUPLICATE' WHERE status_id=8;

UPDATE charge SET entity_status = 'ACTIVE' WHERE status_id=0;
UPDATE charge SET entity_status = 'DELETED' WHERE status_id=3;
UPDATE charge SET entity_status = 'MERGED' WHERE status_id=6;
UPDATE charge SET entity_status = 'DUPLICATE' WHERE status_id=8;

UPDATE charge_history SET entity_status = 'ACTIVE' WHERE status_id=0;
UPDATE charge_history SET entity_status = 'DELETED' WHERE status_id=3;
UPDATE charge_history SET entity_status = 'MERGED' WHERE status_id=6;
UPDATE charge_history SET entity_status = 'DUPLICATE' WHERE status_id=8;

UPDATE sentence SET entity_status = 'ACTIVE' WHERE status_id=0;
UPDATE sentence SET entity_status = 'INACTIVE' WHERE status_id=1;
UPDATE sentence SET entity_status = 'DELETED' WHERE status_id=3;
UPDATE sentence SET entity_status = 'MANY_CHARGES_DATA_FIX' WHERE status_id=7;
UPDATE sentence SET entity_status = 'DUPLICATE' WHERE status_id=8;

UPDATE sentence_history SET entity_status = 'ACTIVE' WHERE status_id=0;
UPDATE sentence_history SET entity_status = 'INACTIVE' WHERE status_id=1;
UPDATE sentence_history SET entity_status = 'DELETED' WHERE status_id=3;
UPDATE sentence_history SET entity_status = 'MANY_CHARGES_DATA_FIX' WHERE status_id=7;
UPDATE sentence_history SET entity_status = 'DUPLICATE' WHERE status_id=8;

UPDATE period_length SET entity_status = 'ACTIVE' WHERE status_id=0;
UPDATE period_length SET entity_status = 'INACTIVE' WHERE status_id=1;
UPDATE period_length SET entity_status = 'DELETED' WHERE status_id=3;
UPDATE period_length SET entity_status = 'MANY_CHARGES_DATA_FIX' WHERE status_id=7;
UPDATE period_length SET entity_status = 'DUPLICATE' WHERE status_id=8;

UPDATE period_length_history SET entity_status = 'ACTIVE' WHERE status_id=0;
UPDATE period_length_history SET entity_status = 'INACTIVE' WHERE status_id=1;
UPDATE period_length_history SET entity_status = 'DELETED' WHERE status_id=3;
UPDATE period_length_history SET entity_status = 'MANY_CHARGES_DATA_FIX' WHERE status_id=7;
UPDATE period_length_history SET entity_status = 'DUPLICATE' WHERE status_id=8;

ALTER TABLE court_case drop COLUMN status_id;
ALTER TABLE court_case_history drop COLUMN status_id;
ALTER TABLE court_appearance drop COLUMN status_id;
ALTER TABLE court_appearance_history drop COLUMN status_id;
ALTER TABLE charge drop COLUMN status_id;
ALTER TABLE charge_history drop COLUMN status_id;
ALTER TABLE sentence drop COLUMN status_id;
ALTER TABLE sentence_history drop COLUMN status_id;
ALTER TABLE period_length drop COLUMN status_id;
ALTER TABLE period_length_history drop COLUMN status_id;

ALTER TABLE court_case RENAME COLUMN entity_status to status_id;
ALTER TABLE court_case_history RENAME COLUMN entity_status to status_id;
ALTER TABLE court_appearance RENAME COLUMN entity_status to status_id;
ALTER TABLE court_appearance_history RENAME COLUMN entity_status to status_id;
ALTER TABLE charge RENAME COLUMN entity_status to status_id;
ALTER TABLE charge_history RENAME COLUMN entity_status to status_id;
ALTER TABLE sentence RENAME COLUMN entity_status to status_id;
ALTER TABLE sentence_history RENAME COLUMN entity_status to status_id;
ALTER TABLE period_length RENAME COLUMN entity_status to status_id;
ALTER TABLE period_length_history RENAME COLUMN entity_status to status_id;

CREATE INDEX idx_sentence_status_id ON sentence USING btree (status_id);
CREATE INDEX idx_court_case_status_id ON court_case USING btree (status_id);
CREATE INDEX idx_charge_status_id ON charge USING btree (status_id);
CREATE UNIQUE INDEX unique_uuid_status_sentence ON period_length USING btree (period_length_uuid, status_id, sentence_id);
CREATE INDEX idx_court_appearance_status_id_date ON court_appearance USING btree (appearance_date, status_id);
CREATE INDEX idx_court_appearance_status_id ON court_appearance USING btree (status_id);