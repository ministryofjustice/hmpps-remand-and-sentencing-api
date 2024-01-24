CREATE TABLE period_length(
    id SERIAL PRIMARY KEY,
    years NUMERIC(7,2),
    months NUMERIC(7,2),
    weeks NUMERIC(7,2),
    days NUMERIC(7,2),
    period_order VARCHAR
);

CREATE TABLE sentence(
    id SERIAL PRIMARY KEY,
    lifetime_sentence_uuid UUID NOT NULL,
    sentence_uuid UUID NOT NULL,
    charge_number VARCHAR NOT NULL,
    custodial_length_id int references period_length(id),
    extended_licence_length_id int references period_length(id),
    status_id smallint NOT NULL,
    created_at timestamp with time zone not null,
    created_by_username VARCHAR NOT NULL,
    created_prison VARCHAR,
    superseding_sentence_id int references sentence(id),
    charge_id int references charge(id)
);