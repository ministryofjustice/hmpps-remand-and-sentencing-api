
CREATE TABLE court_case( -- OFFENDER_CASES, a booking would also need to be created in NOMIS
    id SERIAL PRIMARY KEY,
    case_reference VARCHAR NOT NULL,
    prisoner_id VARCHAR NOT NULL,
    created_at timestamp with time zone not null,
    created_by_username VARCHAR NOT NULL,
    status VARCHAR NOT NULL, -- active/inactive, deleted and any other states an offence could be in
    case_type VARCHAR NOT NULL -- default to adult for now but can be others in the future when we get more journeys, this service might need to own court case types?
);

CREATE TABLE court_appearance( -- COURT_EVENTS
    id SERIAL PRIMARY KEY,
    court_case_id int references court_case(id) NOT NULL,
    court_code VARCHAR NOT NULL, -- the identifier for the court the appearance took place at so that metadata can be looked up in view journey
    appearance_reference VARCHAR,
    warrant_date date NOT NULL,
    overall_outcome VARCHAR NOT NULL, -- have a think about this, should it exist here or be a front end construct?
    outcome_apply_all_offences bool NOT NULL,
    created_at timestamp with time zone not null,
    created_by VARCHAR NOT NULL,
    status VARCHAR NOT NULL, -- active/inactive, deleted and any other states an offence could be in
    appearance_type VARCHAR, -- video link, court appearance. Only appears in the next questions after adding all offences to the appearance.. this is a thinker
    court_date timestamp with time zone NOT NULL -- this is assumed we would create a future dated court appearance from the next questions in the journey? might affect a lot of constraints.. this is a thinker

);

CREATE TABLE offence( -- not sure on this name
   id SERIAL PRIMARY KEY,
   offence_code VARCHAR NOT NULL,
   start_date date NOT NULL,
   end_date date,
   status VARCHAR NOT NULL, -- active/inactive, deleted and any other states an offence could be in
   created_at timestamp with time zone not null,
   created_by VARCHAR NOT NULL

);

CREATE TABLE appearance_offence( -- not sure on this table, offences go across appearances so when we create a new appearance with the same offence list do we copy all entries in offence table having an appearance_id column and reference the new appearance or add an entry to this table keeping the existing offence entry???
    id SERIAL PRIMARY KEY,
    appearance_id int references court_appearance(id) NOT NULL,
    offence_id int references offence(id) NOT NULL
);