with appearanceOutcome AS (select id from appearance_outcome ao where ao.outcome_uuid = 'c3d9a1e6-2b74-4f5a-9d3c-7e1f0a8b6c54')
update court_appearance set appearance_outcome_id = appearanceOutcome.id from appearanceOutcome where legacy_data ->> 'nomisOutcomeCode' = '1507';

with chargeOutcome as (select id from charge_outcome co where co.outcome_uuid = '8a5c4f7d-8e9c-4c1b-9f8e-2f4a3a6d0b21')
update charge set charge_outcome_id = chargeOutcome.id from chargeOutcome where legacy_data ->> 'nomisOutcomeCode' = '1507';