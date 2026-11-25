INSERT INTO sentence_type (sentence_type_uuid, description, min_age_inclusive, max_age_exclusive, min_date_inclusive, max_date_exclusive, classification, hint_text, nomis_cja_code, nomis_sentence_calc_type, display_order, status, min_offence_date_inclusive, max_offence_date_exclusive, is_recallable)
select '01a1ee53-dbb4-770e-9815-6e93c32ae47d', 'Legacy sentence - Schedule 20B (1967)', NULL, NULL, NULL, '2012-12-04', 'LEGACY', NULL, '1967', 'SCHED20B', 185, 'INACTIVE', NULL, NULL, true
    where not exists (select id from sentence_type st where st.sentence_type_uuid='01a1ee53-dbb4-770e-9815-6e93c32ae47d');

update sentence_type set description = 'Legacy sentence - Schedule 20B (1967)' where sentence_type_uuid = '01a1ee53-dbb4-770e-9815-6e93c32ae47d'; /* covers existing entry inserted in preprod and prod */

INSERT INTO sentence_type (sentence_type_uuid, description, min_age_inclusive, max_age_exclusive, min_date_inclusive, max_date_exclusive, classification, hint_text, nomis_cja_code, nomis_sentence_calc_type, display_order, status, min_offence_date_inclusive, max_offence_date_exclusive, is_recallable)
select '75d642f1-a1ed-4dab-a630-ccecd7de93c7', 'Legacy sentence - Schedule 20B (1991)', NULL, NULL, NULL, '2012-12-04', 'LEGACY', NULL, '1991', 'SCHED20B', 186, 'INACTIVE', NULL, NULL, true
    where not exists (select id from sentence_type st where st.sentence_type_uuid='75d642f1-a1ed-4dab-a630-ccecd7de93c7'); /* not currently in any environment so no need to update */



insert into charge_outcome_sentence_type(charge_outcome_id, sentence_type_id)
select id, (select id
            from sentence_type st
            where st.sentence_type_uuid='01a1ee53-dbb4-770e-9815-6e93c32ae47d')
from charge_outcome co
where co.outcome_uuid='3a1d8f72-7b2e-4f5c-8d4a-6c9e1b7f2d03';

insert into charge_outcome_sentence_type(charge_outcome_id, sentence_type_id)
select id, (select id
            from sentence_type st
            where st.sentence_type_uuid='01a1ee53-dbb4-770e-9815-6e93c32ae47d')
from charge_outcome co
where co.outcome_uuid='0460ad51-04ea-402a-a249-b152b052a385';

insert into charge_outcome_sentence_type(charge_outcome_id, sentence_type_id)
select id, (select id
            from sentence_type st
            where st.sentence_type_uuid='75d642f1-a1ed-4dab-a630-ccecd7de93c7')
from charge_outcome co
where co.outcome_uuid='3a1d8f72-7b2e-4f5c-8d4a-6c9e1b7f2d03';

insert into charge_outcome_sentence_type(charge_outcome_id, sentence_type_id)
select id, (select id
            from sentence_type st
            where st.sentence_type_uuid='75d642f1-a1ed-4dab-a630-ccecd7de93c7')
from charge_outcome co
where co.outcome_uuid='0460ad51-04ea-402a-a249-b152b052a385';