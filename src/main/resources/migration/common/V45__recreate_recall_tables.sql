DROP TABLE recall CASCADE ;

CREATE TABLE recall(
    id SERIAL PRIMARY KEY,
    recall_uuid UUID NOT NULL,
    prisoner_id VARCHAR NOT NULL,
    revocation_date DATE NULL,
    return_to_custody_date DATE NULL,
    recall_type_id INTEGER NOT NULL,
    created_at timestamp with time zone not null,
    created_by_username VARCHAR NOT NULL,
    created_by_prison VARCHAR NOT NULL
);

CREATE UNIQUE INDEX idx_recall_uuid ON recall (recall_uuid);

CREATE TABLE recall_sentence(
   id SERIAL PRIMARY KEY,
   recall_sentence_uuid UUID NOT NULL,
   recall_id INTEGER NOT NULL,
   sentence_id INTEGER NOT NULL,
   legacy_data JSONB NULL,
   created_at timestamp with time zone not null,
   created_by_username VARCHAR NOT NULL,
   created_by_prison VARCHAR NOT NULL
);

CREATE TABLE recall_type(
    recall_type_id SERIAL PRIMARY KEY,
    code VARCHAR NOT NULL,
    description VARCHAR NOT NULL
);

INSERT INTO recall_type(id, code, description) VALUES
   (1, 'LR', 'Standard recall'),
   (2, 'FTR_14', 'Fixed-term recall (14 days)'),
   (3, 'FTR_28', 'Fixed-term recall (28 days)'),
   (4, 'LR_HDC', 'Standard recall from HDC'),
   (5, 'FTR_HDC_14', 'Fixed-term recall from HDC (14 days)'),
   (6, 'FTR_HDC_28', 'Fixed-term recall from HDC (28 days)'),
   (7, 'CUR_HDC', 'HDC recall from curfew conditions'),
   (8, 'IN_HDC', 'HDC recall from inability to monitor');

ALTER TABLE recall ADD CONSTRAINT recall_type_id_fk FOREIGN KEY (recall_type_id) references recall_type(id);


