CREATE TABLE uploaded_document(
    id SERIAL PRIMARY KEY,
    appearance_id int references court_appearance(id) NULL,
    document_uuid UUID NOT NULL,
    document_type VARCHAR NOT NULL,
    warrant_type VARCHAR NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by VARCHAR NOT NULL,
    updated_at timestamp with time zone NULL,
    updated_by VARCHAR NULL
);