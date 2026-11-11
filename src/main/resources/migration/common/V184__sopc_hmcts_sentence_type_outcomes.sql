INSERT INTO hmcts_result_code (result_uuid,
                               result_name,
                               result_code,
                               charge_outcome_id,
                               status)
VALUES ('8afc8129-813e-4c2b-ab12-f35daaf41d2a', 'SOPC 18-20', 'SPECCC',
        (SELECT id
         FROM charge_outcome
         WHERE outcome_uuid = 'f17328cf-ceaa-43c2-930a-26cf74480e18'),
        'ACTIVE'),
       ('1bb94801-d4ac-4bee-892e-d690ec7a9e03', 'SOPC 21+', 'SPECC',
        (SELECT id
         FROM charge_outcome
         WHERE outcome_uuid = 'f17328cf-ceaa-43c2-930a-26cf74480e18'),
        'ACTIVE'),
       ('5059fe13-a6b8-4495-aa22-ce1f6b517e28', 'SOPC U18', 'SPECCD',
        (SELECT id
         FROM charge_outcome
         WHERE outcome_uuid = 'f17328cf-ceaa-43c2-930a-26cf74480e18'),
        'ACTIVE');
