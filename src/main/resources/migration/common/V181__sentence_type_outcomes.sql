INSERT INTO hmcts_result_code (result_uuid,
                               result_name,
                               result_code,
                               charge_outcome_id,
                               status)
VALUES ('2b9973cb-e357-429e-ae7e-a62b0a7d24e5', 'Extended Determinate Sentence (U18)', 'EXTDVSU',
        (SELECT id
         FROM charge_outcome
         WHERE outcome_uuid = 'f17328cf-ceaa-43c2-930a-26cf74480e18'),
        'ACTIVE'),
       ('2af25e58-6e8d-4f35-88e7-5fda4465be07', 'Extended Determinate Sentence (18-20)', 'EXTDVS',
        (SELECT id
         FROM charge_outcome
         WHERE outcome_uuid = 'f17328cf-ceaa-43c2-930a-26cf74480e18'),
        'ACTIVE'),
       ('e5f5a02c-c431-430c-8c96-5c64030fac5a', 'Extended Determinate Sentence (21+)', 'EXTIVS',
        (SELECT id
         FROM charge_outcome
         WHERE outcome_uuid = 'f17328cf-ceaa-43c2-930a-26cf74480e18'),
        'ACTIVE'),
       ('5fc7498f-8394-4cc4-ab5c-39db338d5d34', 'Adult Mandatory Life Sentences', 'MLI',
        (SELECT id
         FROM charge_outcome
         WHERE outcome_uuid = 'f17328cf-ceaa-43c2-930a-26cf74480e18'),
        'ACTIVE');
