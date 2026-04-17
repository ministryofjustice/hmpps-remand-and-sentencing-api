INSERT INTO charge_outcome (outcome_name, outcome_uuid, nomis_code, outcome_type, display_order, disposition_code, status)
    select 'Youth Offenders Institution', '3a1d8f72-7b2e-4f5c-8d4a-6c9e1b7f2d03', '1024', 'SENTENCING', 60, 'FINAL', 'ACTIVE'
    where not exists (select id from charge_outcome co where co.outcome_uuid='3a1d8f72-7b2e-4f5c-8d4a-6c9e1b7f2d03');

insert into charge_outcome_sentence_type(charge_outcome_id, sentence_type_id)
    select id, (select id
               from sentence_type st
               where st.sentence_type_uuid='babc059d-53aa-423c-9129-be2ba86026ad')
    from charge_outcome co
    where co.outcome_uuid='3a1d8f72-7b2e-4f5c-8d4a-6c9e1b7f2d03';
insert into charge_outcome_sentence_type(charge_outcome_id, sentence_type_id)
    select id, (select id
                from sentence_type st
                where st.sentence_type_uuid='d64283fd-7e48-4ed8-a98c-d68e938a5661')
    from charge_outcome co
    where co.outcome_uuid='3a1d8f72-7b2e-4f5c-8d4a-6c9e1b7f2d03';
insert into charge_outcome_sentence_type(charge_outcome_id, sentence_type_id)
    select id, (select id
                from sentence_type st
                where st.sentence_type_uuid='87ef5d34-5912-411a-8733-2b8cf0cfc8e5')
    from charge_outcome co
    where co.outcome_uuid='3a1d8f72-7b2e-4f5c-8d4a-6c9e1b7f2d03';
insert into charge_outcome_sentence_type(charge_outcome_id, sentence_type_id)
    select id,(select id
               from sentence_type st
               where st.sentence_type_uuid='3bfbf00d-747e-4576-8d96-aa4f8f9832ef')
    from charge_outcome co
    where co.outcome_uuid='3a1d8f72-7b2e-4f5c-8d4a-6c9e1b7f2d03';