INSERT INTO hmcts_result_code (result_uuid,
                               result_name,
                               result_code,
                               charge_outcome_id,
                               status)
VALUES ('7fc0d760-4c16-4c38-aff0-8c51d5f75758', 'SSIV Result Code - Activated with Lesser Term', 'SSIV',
        (SELECT id
         FROM charge_outcome
         WHERE outcome_uuid = '8a5c4f7d-8e9c-4c1b-9f8e-2f4a3a6d0b21'),
        'ACTIVE'),
       ('0ca5797e-82be-4cba-bb8f-49102d0bef2e', 'SSID Result Code - YOI Activated Unaltered', 'SSID',
        (SELECT id
         FROM charge_outcome
         WHERE outcome_uuid = '8a5c4f7d-8e9c-4c1b-9f8e-2f4a3a6d0b21'),
        'ACTIVE'),
       ('5ed15a79-f416-483c-ad0e-3b98f7efc81a', 'SSIDV Result Code - YOI Activated with Lesser Term', 'SSIDV',
        (SELECT id
         FROM charge_outcome
         WHERE outcome_uuid = '8a5c4f7d-8e9c-4c1b-9f8e-2f4a3a6d0b21'),
        'ACTIVE')
    ON CONFLICT (result_code) DO NOTHING;
