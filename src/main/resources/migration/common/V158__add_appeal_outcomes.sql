INSERT INTO charge_outcome (outcome_name, outcome_uuid, nomis_code, outcome_type, display_order, disposition_code, status)
VALUES
    ('Appeal dismissed','019dab54-5e29-717a-ba48-1d6017b69af0','3045','APPEAL',30,'FINAL','ACTIVE'),
    ('Appeal pending','019dabb4-c632-7789-b18a-63915157510c','3044', 'APPEAL', 20, 'INTERIM', 'ACTIVE'),
    ('Sentence varied','019daf9e-0d4d-7afb-b2ac-2af9d378f223','1046','APPEAL',10,'INTERIM','ACTIVE'),
    ('Sentence quashed','019daf9f-942d-7be3-b3f0-f4620bb7a8c8','2068','APPEAL',40,'FINAL','ACTIVE');

INSERT INTO appearance_outcome (outcome_name, outcome_uuid, nomis_code, outcome_type, display_order, related_charge_outcome_uuid, is_sub_list, status, warrant_type, disposition_code)
VALUES
    ('Appeal dismissed', '019dab54-5e29-7bbc-a463-43e6df05b651', '3045', 'APPEAL', 30, '019dab54-5e29-717a-ba48-1d6017b69af0', false, 'ACTIVE', 'APPEAL', 'FINAL'),
    (  'Appeal pending',  '019dabb4-c632-7835-a299-034cdd9e49da',  '3044',  'APPEAL',  20,  '019dabb4-c632-7789-b18a-63915157510c',  false,  'ACTIVE',  'APPEAL',  'INTERIM'),  
    (  'Sentence varied',  '019daf9e-0d4d-7282-8293-d929687afe5b',  '1046',  'APPEAL',  10,  '019daf9e-0d4d-7afb-b2ac-2af9d378f223',  false,  'ACTIVE',  'APPEAL',  'FINAL'),  
    (  'Sentence quashed',  '019daf9f-942d-72fe-bc0a-2d504d86a942',  '2068',  'APPEAL',  40,  '019daf9f-942d-7be3-b3f0-f4620bb7a8c8',  false,  'ACTIVE',  'APPEAL',  'FINAL');