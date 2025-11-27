
INSERT INTO appearance_outcome(outcome_name,outcome_uuid,nomis_code,outcome_type,display_order,related_charge_outcome_uuid,is_sub_list,status,warrant_type) values
('Remand on conditional bail', 'c1e6c5fc-0f0b-4b42-8e7b-6b1e8ce51f36', '4530', 'NON_CUSTODIAL', 135, '8c2f8c29-3b3d-4c8e-9ad4-a0e3df2de6c0', true, 'ACTIVE', 'REMAND');

INSERT INTO charge_outcome (outcome_name, outcome_uuid, nomis_code, outcome_type, display_order, disposition_code, status) VALUES
('Remand on conditional bail', '8c2f8c29-3b3d-4c8e-9ad4-a0e3df2de6c0', '4530', 'NON_CUSTODIAL', 15, 'INTERIM', 'ACTIVE');