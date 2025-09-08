ALTER TABLE period_length ADD CONSTRAINT unique_uuid_status_sentence UNIQUE (period_length_uuid, status_id, sentence_id);

ALTER TABLE court_case drop COLUMN status_id_old;
ALTER TABLE court_case_history drop COLUMN status_id_old;
ALTER TABLE court_appearance drop COLUMN status_id_old;
ALTER TABLE court_appearance_history drop COLUMN status_id_old;
ALTER TABLE charge drop COLUMN status_id_old;
ALTER TABLE charge_history drop COLUMN status_id_old;
ALTER TABLE sentence drop COLUMN status_id_old;
ALTER TABLE sentence_history drop COLUMN status_id_old;
ALTER TABLE period_length drop COLUMN status_id_old;
ALTER TABLE period_length_history drop COLUMN status_id_old;