-- 1. Updates Detention and Training Order (DTO) to SENTENCING (from REMAND) in charge_outcome table
UPDATE charge_outcome
SET outcome_type = 'SENTENCING'
WHERE outcome_name = 'Detention and training order' AND outcome_type = 'REMAND';

-- 2. Updates DTO to SENTENCING in appearance_outcome table
UPDATE appearance_outcome
SET
    outcome_type = 'SENTENCING',
    display_order = 30
WHERE outcome_name = 'Detention and training order' AND outcome_type = 'REMAND';

-- 3. Adds 'Imprisonment in default of a fine' to appearance_outcome table
INSERT INTO appearance_outcome (
    outcome_name,
    outcome_uuid,
    nomis_code,
    outcome_type,
    display_order,
    related_charge_outcome_uuid,
    is_sub_list,
    status
) VALUES (
   'Imprisonment in default of a fine',
   '04f21679-268f-44c5-9a58-00aaa00c24e0',
   '1510',
   'SENTENCING',
   20,
    (SELECT outcome_uuid FROM charge_outcome WHERE outcome_name = 'Imprisonment in default of a fine' LIMIT 1),
    false,
    'ACTIVE'
    );
