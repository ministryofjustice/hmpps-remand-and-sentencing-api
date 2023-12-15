CREATE INDEX idx_court_case_prisoner_id ON court_case (prisoner_id);
CREATE UNIQUE INDEX idx_court_case_case_unique_identifier ON court_case (case_unique_identifier);