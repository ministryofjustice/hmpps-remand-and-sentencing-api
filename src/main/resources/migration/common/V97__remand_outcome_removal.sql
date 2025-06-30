
update court_appearance set appearance_outcome_id = null where appearance_outcome_id in (select ao.id from appearance_outcome ao where ao.outcome_uuid in ('31c6974b-fb06-4dc4-95a0-9612df39ae12', 'b6bdf776-809b-41f1-bc20-9efb04c1c8d7', 'ba6b09fc-9abd-4e9d-a128-357c31508118'));

delete from appearance_outcome where outcome_uuid in ('31c6974b-fb06-4dc4-95a0-9612df39ae12', 'b6bdf776-809b-41f1-bc20-9efb04c1c8d7', 'ba6b09fc-9abd-4e9d-a128-357c31508118');

update charge set charge_outcome_id = null where charge_outcome_id in (select co.id from charge_outcome co where co.outcome_uuid in ('70bf3a77-9f66-46cb-ba27-0f9b25682b44', 'ebe76f25-3371-4789-9b19-c6b6048cd6b0', '28cc6399-9849-48b5-b8e0-59f0163bbea6'));

delete from charge_outcome where outcome_uuid in ('70bf3a77-9f66-46cb-ba27-0f9b25682b44', 'ebe76f25-3371-4789-9b19-c6b6048cd6b0', '28cc6399-9849-48b5-b8e0-59f0163bbea6');