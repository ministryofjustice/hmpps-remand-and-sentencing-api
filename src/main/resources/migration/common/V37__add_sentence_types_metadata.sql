ALTER TABLE sentence_type
ADD COLUMN nomis_cja_code VARCHAR NOT NULL DEFAULT 'UNKNOWN',
ADD COLUMN nomis_sentence_calc_type VARCHAR NOT NULL DEFAULT 'UNKNOWN',
ADD COLUMN display_order int NOT NULL DEFAULT 0;



