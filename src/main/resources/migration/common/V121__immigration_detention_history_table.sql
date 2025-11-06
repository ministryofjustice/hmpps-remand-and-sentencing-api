CREATE TABLE immigration_detention_history
(
    id                                SERIAL PRIMARY KEY,
    original_immigration_detention_id INTEGER                  NOT NULL,
    immigration_detention_uuid        UUID                     NOT NULL,
    prisoner_id                       VARCHAR                  NOT NULL,
    immigration_detention_record_type VARCHAR                  NOT NULL,
    record_date                       DATE                     NOT NULL,
    home_office_reference_number      VARCHAR,
    no_longer_of_interest_reason      VARCHAR,
    no_longer_of_interest_comment     VARCHAR,
    status_id                         SMALLINT,
    created_at                        TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_username               VARCHAR                  NOT NULL,
    created_prison                    VARCHAR,
    updated_at                        TIMESTAMP WITH TIME ZONE,
    updated_by                        VARCHAR,
    updated_prison                    VARCHAR,
    source                            VARCHAR                  NOT NULL,
    history_status_id                 INTEGER                  NOT NULL,
    history_created_at                TIMESTAMP WITH TIME ZONE NOT NULL
);