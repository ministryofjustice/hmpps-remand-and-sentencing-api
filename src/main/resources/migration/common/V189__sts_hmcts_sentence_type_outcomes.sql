INSERT INTO hmcts_result_code (result_uuid,
                               result_name,
                               result_code,
                               charge_outcome_id,
                               status)
VALUES ('8f7de4cf-2974-43d6-8d89-c24e9f57018c', 'Serious Terrorism Sentence Sentence (18-20)', 'STSDY',
        (SELECT id
         FROM charge_outcome
         WHERE outcome_uuid = '8a5c4f7d-8e9c-4c1b-9f8e-2f4a3a6d0b21'),
        'ACTIVE'),
       ('a195f655-ab2c-421c-9b2a-479dfa1a33a4', 'Serious Terrorism Sentence Sentence (21+)', 'STSI',
        (SELECT id
         FROM charge_outcome
         WHERE outcome_uuid = '8a5c4f7d-8e9c-4c1b-9f8e-2f4a3a6d0b21'),
        'ACTIVE')
    ON CONFLICT (result_code) DO NOTHING;
