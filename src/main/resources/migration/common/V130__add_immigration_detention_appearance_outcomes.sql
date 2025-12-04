insert into appearance_outcome (
    outcome_name,
    outcome_uuid,
    nomis_code,
    outcome_type,
    display_order,
    related_charge_outcome_uuid,
    warrant_type,
    disposition_code
) values
      ('Immigration Detainee', '5c670576-ffbf-4005-8d54-4aeba7bf1a22', '5500', 'IMMIGRATION', 300, '0896fac8-663f-4f4f-a55e-166c005f0a52', 'IMMIGRATION', 'INTERIM'),
      ('Immigration Decision to Deport', 'b28afb19-dd94-4970-8071-e616b33274cb', '5502', 'IMMIGRATION', 310, 'e536e07a-18c4-4ade-80f0-3e8bc369019a', 'IMMIGRATION', 'INTERIM'),
      ('Immigration No Longer of Interest', '15524814-3238-4e4b-86a7-cda31b0221ec', '5503', 'IMMIGRATION', 320, '3b3781ae-b1ad-4db6-9889-c464f48738e5', 'IMMIGRATION', 'FINAL');

insert into charge_outcome (
    outcome_name,
    outcome_uuid,
    nomis_code,
    outcome_type,
    display_order,
    disposition_code
) values
    ('Immigration Detainee', '0896fac8-663f-4f4f-a55e-166c005f0a52', '5500', 'IMMIGRATION', 300, 'INTERIM'),
    ('Immigration Decision to Deport', 'e536e07a-18c4-4ade-80f0-3e8bc369019a', '5502', 'IMMIGRATION', 310, 'INTERIM'),
    ('Immigration No Longer of Interest', '3b3781ae-b1ad-4db6-9889-c464f48738e5', '5503', 'IMMIGRATION', 320, 'FINAL');

