INSERT INTO hmcts_result_code (
    result_uuid,
    result_name,
    result_code,
    charge_outcome_id,
    status
)
SELECT
    gen_random_uuid(),
    outcome_name,
    hmcts_code,
    id,
    status
FROM charge_outcome
WHERE hmcts_code IS NOT NULL;

ALTER TABLE charge_outcome
DROP COLUMN hmcts_code;