ALTER TABLE period_length ADD COLUMN period_length_uuid UUID NOT NULL DEFAULT gen_random_uuid(),
ADD COLUMN status_id smallint NOT NULL default 0,
ADD COLUMN created_at timestamp with time zone NOT NULL DEFAULT NOW(),
ADD COLUMN created_by VARCHAR NOT NULL default 'UNKNOWN',
ADD COLUMN created_prison VARCHAR,
ADD COLUMN updated_at timestamp with time zone,
ADD COLUMN updated_by VARCHAR,
ADD COLUMN updated_prison VARCHAR;

create index idx_period_length_uuid on period_length(period_length_uuid);