ALTER TABLE charge_outcome ADD COLUMN hmcts_code varchar null;

UPDATE charge_outcome set hmcts_code = 'RI' where outcome_uuid = '315280e5-d53e-43b3-8ba6-44da25676ce2';
UPDATE charge_outcome set hmcts_code = 'CCII' where outcome_uuid = '8976a19b-ab84-4881-b8c7-cf7b1978a262';
UPDATE charge_outcome set hmcts_code = 'RC' where outcome_uuid = '8c2f8c29-3b3d-4c8e-9ad4-a0e3df2de6c0';
UPDATE charge_outcome set hmcts_code = 'CCSI' where outcome_uuid = 'daed8a56-a5e7-4bba-807a-ede788f2f078';
UPDATE charge_outcome set hmcts_code = 'NSP' where outcome_uuid = 'f9042ecc-6bed-4872-bbed-1b0938bcecd7';
UPDATE charge_outcome set hmcts_code = 'FCOMP' where outcome_uuid = '9f3c6b7e-2a91-4c4b-8d9a-6f2e1c5a7b3d';
UPDATE charge_outcome set hmcts_code = 'CTROF' where outcome_uuid = '76ec0de0-da17-44a8-9184-790770d16d46';
UPDATE charge_outcome set hmcts_code = 'SUSPS' where outcome_uuid = 'fa6c0ada-c6e8-4f7a-ab57-ec46a51c41f7';
UPDATE charge_outcome set hmcts_code = 'DISCH' where outcome_uuid = '0e9437fa-d9bc-4588-8105-dc932163c624';
UPDATE charge_outcome set hmcts_code = 'WDRN' where outcome_uuid = '6d2eb21d-ec02-48fa-9fcd-02e73b8e45ca';
UPDATE charge_outcome set hmcts_code = 'REMUB' where outcome_uuid = '9f7b2c2e-7a9c-4d8c-9b3a-8b9f4d3e2c1a';
UPDATE charge_outcome set hmcts_code = 'A' where outcome_uuid = '23e9e49c-96ec-4493-8768-6fbc0247ace3';
UPDATE charge_outcome set hmcts_code = 'COEW' where outcome_uuid = 'c3a0e1a2-5a7c-4a56-8f3f-2a4d8c78d611';
UPDATE charge_outcome set hmcts_code = 'DISC' where outcome_uuid = '6ff42a7a-6e88-42ef-8671-5438bd221324';
UPDATE charge_outcome set hmcts_code = 'DISM' where outcome_uuid = 'e4c69c8a-9320-4126-9101-5674191ff37e';
