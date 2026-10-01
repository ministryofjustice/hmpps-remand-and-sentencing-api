ALTER TABLE uploaded_document
    ADD CONSTRAINT uploaded_document_unique_uuid UNIQUE (document_uuid);