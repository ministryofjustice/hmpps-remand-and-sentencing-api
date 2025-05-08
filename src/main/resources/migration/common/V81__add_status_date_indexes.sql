create index idx_court_case_status_id on court_case(status_id);
create index idx_court_appearance_status_id on court_appearance(status_id);
create index idx_charge_status_id on charge(status_id);
create index idx_sentence_status_id on sentence(status_id);
create index idx_court_appearance_status_id_date on court_appearance(appearance_date, status_id);