-- Fixing spelling mistake
UPDATE charge_outcome
SET outcome_name = 'No separate penalty'
WHERE outcome_name = 'No seperate penalty';

-- Update outcome_type to NON_CUSTODIAL for specific charge outcomes based on outcome_name
UPDATE charge_outcome
SET outcome_type = 'NON_CUSTODIAL'
WHERE outcome_name = 'No separate penalty';