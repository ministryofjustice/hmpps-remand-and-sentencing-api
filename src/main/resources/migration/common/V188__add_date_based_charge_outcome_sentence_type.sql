alter table charge_outcome_sentence_type add column max_date_exclusive date;

insert into charge_outcome_sentence_type(charge_outcome_id, sentence_type_id, max_date_exclusive)
select id, (select id
            from sentence_type st
            where st.sentence_type_uuid='75d642f1-a1ed-4dab-a630-ccecd7de93c7'), '2012-12-04'
from charge_outcome co
where co.outcome_uuid='f17328cf-ceaa-43c2-930a-26cf74480e18'; /* imprisonment status */

insert into charge_outcome_sentence_type(charge_outcome_id, sentence_type_id, max_date_exclusive)
select id, (select id
            from sentence_type st
            where st.sentence_type_uuid='01a1ee53-dbb4-770e-9815-6e93c32ae47d'), '2012-12-04'
from charge_outcome co
where co.outcome_uuid='f17328cf-ceaa-43c2-930a-26cf74480e18'; /* imprisonment status */




insert into charge_outcome_sentence_type(charge_outcome_id, sentence_type_id, max_date_exclusive)
select id, (select id
            from sentence_type st
            where st.sentence_type_uuid='75d642f1-a1ed-4dab-a630-ccecd7de93c7'), '2012-12-04'
from charge_outcome co
where co.outcome_uuid='8a5c4f7d-8e9c-4c1b-9f8e-2f4a3a6d0b21'; /* suspended imprisonment status */

insert into charge_outcome_sentence_type(charge_outcome_id, sentence_type_id, max_date_exclusive)
select id, (select id
            from sentence_type st
            where st.sentence_type_uuid='01a1ee53-dbb4-770e-9815-6e93c32ae47d'), '2012-12-04'
from charge_outcome co
where co.outcome_uuid='8a5c4f7d-8e9c-4c1b-9f8e-2f4a3a6d0b21'; /* suspended imprisonment status */