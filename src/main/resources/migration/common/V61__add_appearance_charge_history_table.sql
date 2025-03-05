ALTER TABLE appearance_charge
    ADD COLUMN created_at timestamp with time zone NOT NULL default NOW(),
    ADD COLUMN created_by varchar NOT NULL default 'UNKNOWN',
    ADD COLUMN created_prison varchar;

CREATE TABLE appearance_charge_history
(
    id             SERIAL PRIMARY KEY,
    appearance_id  int4 NOT NULL,
    charge_id      int4 NOT NULL,
    created_at     timestamp with time zone NOT NULL,
    created_by     varchar NOT NULL,
    created_prison varchar,
    removed_at     timestamp with time zone,
    removed_by     varchar,
    removed_prison varchar
);
