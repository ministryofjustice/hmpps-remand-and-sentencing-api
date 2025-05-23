CREATE TABLE uploaded_document(
    id SERIAL PRIMARY KEY,
    appearance_id int references court_appearance(id) NULL,
    document_uuid UUID NOT NULL,
    document_type VARCHAR NOT NULL,
    warrant_type VARCHAR NOT NULL
);