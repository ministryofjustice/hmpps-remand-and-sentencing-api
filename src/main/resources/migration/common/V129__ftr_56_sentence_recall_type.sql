INSERT INTO recall_type(id, code, description) VALUES
   (9, 'FTR_56', '56-day fixed-term');


INSERT INTO legacy_sentence_types (
    nomis_reference, classification, sentencing_act, nomis_active, nomis_expiry_date, nomis_description, recall_type_id, recall_type_description, eligibility, nomis_terms, sentence_type_uuid
) VALUES
('FTR_56ORA','STANDARD',2020, TRUE, NULL, 'ORA 56 Day Fixed Term Recall',9,'56 Day Fixed Term', NULL, '["IMP"]'::jsonb, NULL);