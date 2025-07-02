
CREATE TABLE recall_history(
                       id SERIAL PRIMARY KEY,
                       recall_id INTEGER NOT NULL,
                       recall_uuid UUID NOT NULL,
                       prisoner_id VARCHAR NOT NULL,
                       revocation_date DATE NULL,
                       return_to_custody_date DATE NULL,
                       recall_type_id INTEGER NOT NULL,
                       status_id smallint NOT NULL default 0,
                       in_prison_on_revocation_date BOOLEAN,
                       created_at timestamp with time zone not null,
                       created_by_username VARCHAR NOT NULL,
                       created_prison VARCHAR,
                       updated_at timestamp with time zone,
                       updated_by VARCHAR,
                       updated_prison VARCHAR,
                       history_status_id smallint NOT NULL default 0,
                       history_created_at timestamp with time zone
);

CREATE TABLE recall_sentence_history(
                                id SERIAL PRIMARY KEY,
                                recall_sentence_id INTEGER NOT NULL,
                                recall_sentence_uuid UUID NOT NULL,
                                recall_history_id INTEGER NOT NULL,
                                sentence_id INTEGER NOT NULL,
                                legacy_data JSONB NULL,
                                created_at timestamp with time zone not null,
                                created_by_username VARCHAR NOT NULL,
                                created_prison VARCHAR,
                                history_created_at timestamp with time zone
);

ALTER TABLE recall_sentence_history ADD CONSTRAINT fk_recall_sentence_history_to_recall_history FOREIGN KEY (recall_history_id) REFERENCES recall_history(id);
