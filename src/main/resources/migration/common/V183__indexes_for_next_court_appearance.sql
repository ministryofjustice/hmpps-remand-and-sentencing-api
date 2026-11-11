CREATE INDEX idx_court_case_prisoner_status_latest
    ON court_case (prisoner_id, status_id)
    INCLUDE (id, latest_court_appearance_id);

CREATE INDEX idx_court_appearance_case_status_next
    ON court_appearance (court_case_id, status_id)
    INCLUDE (id, next_court_appearance_id);

CREATE INDEX idx_next_court_appearance_date_time
    ON next_court_appearance (appearance_date, appearance_time);

CREATE INDEX idx_court_appearance_subtype_lookup
    ON court_appearance_subtype (appearance_type_id);
