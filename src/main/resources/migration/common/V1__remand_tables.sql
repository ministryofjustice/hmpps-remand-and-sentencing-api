CREATE TABLE court_case( -- OFFENDER_CASES, a booking would also need to be created in NOMIS
    id SERIAL PRIMARY KEY,
    prisoner_id VARCHAR NOT NULL,
    case_unique_identifier VARCHAR NOT NULL,
    latest_court_appearance_id int,
    created_at timestamp with time zone not null,
    created_by_username VARCHAR NOT NULL,
    status_id smallint NOT NULL -- active/inactive, deleted and any other states an offence could be in represented by number
);

CREATE TABLE appearance_outcome(
    id SERIAL PRIMARY KEY,
    outcome_name VARCHAR NOT NULL
);

CREATE TABLE next_court_appearance(
    id SERIAL PRIMARY KEY,
    appearance_date date NOT NULL,
    court_code VARCHAR,
    appearance_type VARCHAR
);

CREATE TABLE court_appearance( -- COURT_EVENTS
    id SERIAL PRIMARY KEY,
    appearance_uuid UUID NOT NULL,
    appearance_outcome_id int references appearance_outcome(id) NOT NULL,
    court_case_id int references court_case(id) NOT NULL,
    court_code VARCHAR NOT NULL, -- the identifier for the court the appearance took place at so that metadata can be looked up in view journey
    court_case_reference VARCHAR,
    appearance_date date NOT NULL,
    status_id smallint NOT NULL, -- active/inactive, deleted and any other states an offence could be in represented by number
    next_court_appearance_id int references next_court_appearance(id),
    previous_appearance_id int references court_appearance(id),
    warrant_id VARCHAR,
    created_at timestamp with time zone not null,
    created_by_username VARCHAR NOT NULL,
    created_prison VARCHAR
);

ALTER TABLE court_case ADD CONSTRAINT latest_court_appearance_fk FOREIGN KEY (latest_court_appearance_id) references court_appearance(id);

CREATE TABLE charge_outcome(
    id SERIAL PRIMARY KEY,
    outcome_name VARCHAR NOT NULL
);

CREATE TABLE charge(
    id SERIAL PRIMARY KEY,
    lifetime_charge_uuid UUID NOT NULL,
    charge_uuid UUID NOT NULL,
    offence_code VARCHAR NOT NULL,
    offence_start_date DATE NOT NULL,
    offence_end_date DATE,
    status_id smallint NOT NULL,
    charge_outcome_id int references charge_outcome(id) NOT NULL,
    superseding_charge_id int references charge(id)
);

CREATE TABLE appearance_charge(
    id SERIAL PRIMARY KEY,
    appearance_id int references court_appearance(id) NOT NULL,
    charge_id int references charge(id) NOT NULL
);

