CREATE TABLE hmcts_result_code
(
    id                       SERIAL PRIMARY KEY,
    result_uuid              UUID                     NOT NULL,
    result_name              VARCHAR                  NOT NULL,
    result_code              VARCHAR                  NOT NULL,
    charge_outcome_id        SERIAL                   NOT NULL,
    status                   VARCHAR,

    CONSTRAINT fk_hmcts_result_code_charge_outcome
        FOREIGN KEY (charge_outcome_id)
            REFERENCES charge_outcome (id)
);

CREATE UNIQUE INDEX idx_hmcts_result_code_identifier ON hmcts_result_code (result_uuid);
CREATE UNIQUE INDEX idx_hmcts_result_code_code ON hmcts_result_code (result_code);
create index idx_hmcts_result_code_status on hmcts_result_code(status);
create index idx_hmcts_result_code_charge_outcome_id on hmcts_result_code(charge_outcome_id);
