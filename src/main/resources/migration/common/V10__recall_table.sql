CREATE TABLE recall(
    id SERIAL PRIMARY KEY,
    recall_unique_identifier UUID NOT NULL,
    prisoner_id VARCHAR NOT NULL,
    recall_date DATE NOT NULL,
    return_to_custody_date DATE NOT NULL,
    recall_type VARCHAR NOT NULL,
    created_at timestamp with time zone not null,
    created_by_username VARCHAR NOT NULL
);

CREATE UNIQUE INDEX idx_recall_unique_identifier ON recall (recall_unique_identifier);
