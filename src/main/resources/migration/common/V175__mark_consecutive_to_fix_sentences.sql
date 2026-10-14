alter table court_case drop column has_unconfirmed_consecutive_to_fix;
alter table court_case_history drop column has_unconfirmed_consecutive_to_fix;

alter table sentence add column has_unconfirmed_consecutive_to_fix bool NULL;
alter table sentence_history add column has_unconfirmed_consecutive_to_fix bool NULL;
