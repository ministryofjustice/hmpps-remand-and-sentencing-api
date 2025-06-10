ALTER TABLE court_case ADD COLUMN merged_to_case_id int constraint fk_merged_to_case_id references court_case(id);
