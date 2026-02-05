INSERT INTO appearance_outcome (outcome_name, outcome_uuid, nomis_code, outcome_type, display_order, related_charge_outcome_uuid, is_sub_list, status, warrant_type, disposition_code)
VALUES('Suspended sentence implemented', 'c3d9a1e6-2b74-4f5a-9d3c-7e1f0a8b6c54', '1507', 'SENTENCING', 25, '8a5c4f7d-8e9c-4c1b-9f8e-2f4a3a6d0b21', false, 'ACTIVE', 'SENTENCING', 'FINAL');

INSERT INTO charge_outcome (outcome_name, outcome_uuid, nomis_code, outcome_type, display_order, disposition_code, status)
VALUES('Suspended sentence implemented', '8a5c4f7d-8e9c-4c1b-9f8e-2f4a3a6d0b21', '1507', 'SENTENCING', 40, 'FINAL', 'ACTIVE');