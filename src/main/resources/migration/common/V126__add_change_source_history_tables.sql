alter table appearance_charge_history ADD COLUMN change_source varchar NOT NULL DEFAULT 'NOMIS';
alter table charge_history ADD COLUMN change_source varchar NOT NULL DEFAULT 'NOMIS';
alter table court_appearance_history ADD COLUMN change_source varchar NOT NULL DEFAULT 'NOMIS';
alter table court_case_history ADD COLUMN change_source varchar NOT NULL DEFAULT 'NOMIS';
alter table period_length_history ADD COLUMN change_source varchar NOT NULL DEFAULT 'NOMIS';
alter table recall_history ADD COLUMN change_source varchar NOT NULL DEFAULT 'NOMIS';
alter table recall_sentence_history ADD COLUMN change_source varchar NOT NULL DEFAULT 'NOMIS';
alter table sentence_history ADD COLUMN change_source varchar NOT NULL DEFAULT 'NOMIS';