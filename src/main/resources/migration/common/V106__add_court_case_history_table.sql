CREATE TABLE court_case_history (
	id SERIAL PRIMARY KEY,
    prisoner_id varchar NOT NULL,
    case_unique_identifier varchar NOT NULL,
    latest_court_appearance_id int NULL,
    created_at timestamp with time zone NOT NULL,
    created_by varchar NOT NULL,
    updated_at timestamp with time zone NULL,
    updated_by varchar NULL,
    status_id smallint NOT NULL,
    legacy_data jsonb NULL,
    created_prison varchar NULL,
    merged_to_case_id int NULL,
    merged_to_date date NULL,
	original_court_case_id int references court_case(id)
);