-- AC1: Drop the is_sub_list column
ALTER TABLE charge_outcome
DROP COLUMN is_sub_list;

-- AC2: Update outcome_type to NON_CUSTODIAL for specific charge outcomes based on outcome_name
UPDATE charge_outcome
SET outcome_type = 'NON_CUSTODIAL'
WHERE outcome_name IN (
                       'Lie on file',
                       'Not guilty',
                       'No separate penalty',
                       'Suspended imprisonment',
                       'Dismissed',
                       'Discharged',
                       'Discontinuance',
                       'Withdrawn final',
                       'Withdrawn',
                       'Proceedings stayed',
                       'Fine'
    );