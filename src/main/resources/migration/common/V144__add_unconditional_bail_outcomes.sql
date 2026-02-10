INSERT INTO appearance_outcome (outcome_name, outcome_uuid, nomis_code, outcome_type, display_order, related_charge_outcome_uuid, is_sub_list, status, warrant_type, disposition_code)
VALUES('Remand on unconditional bail', 'c2f1d8a4-6e3b-4f9a-9a7b-1c6d2f0e8b53', '4542', 'NON_CUSTODIAL', 135, '9f7b2c2e-7a9c-4d8c-9b3a-8b9f4d3e2c1a', true, 'ACTIVE', 'NON_SENTENCING', 'INTERIM');

INSERT INTO charge_outcome (outcome_name, outcome_uuid, nomis_code, outcome_type, display_order, disposition_code, status)
VALUES('Remand on unconditional bail', '9f7b2c2e-7a9c-4d8c-9b3a-8b9f4d3e2c1a', '4542', 'NON_CUSTODIAL', 18, 'INTERIM', 'ACTIVE');