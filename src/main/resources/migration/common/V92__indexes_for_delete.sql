CREATE INDEX IF NOT EXISTS idx_plh_original_period_length_id ON period_length_history (original_period_length_id);

CREATE INDEX IF NOT EXISTS idx_sh_original_sentence_id ON sentence_history (original_sentence_id);

CREATE INDEX IF NOT EXISTS idx_charge_history_original_charge_id ON charge_history (original_charge_id);

CREATE INDEX IF NOT EXISTS idx_appearance_charge_history_appearance_id ON appearance_charge_history(appearance_id);

CREATE INDEX IF NOT EXISTS idx_cah_original_appearance_id ON court_appearance_history(original_appearance_id);

CREATE INDEX IF NOT EXISTS idx_uploaded_document_appearance_id
    ON uploaded_document(appearance_id);

CREATE INDEX IF NOT EXISTS idx_court_appearance_previous_appearance_id
    ON court_appearance(previous_appearance_id);

CREATE INDEX IF NOT EXISTS idx_next_court_appearance_future_skeleton_id
    ON next_court_appearance(future_skeleton_appearance_id);

CREATE INDEX IF NOT EXISTS idx_charge_merged_from_case_id
    ON charge(merged_from_case_id);

CREATE INDEX IF NOT EXISTS idx_draft_appearance_court_case_id
    ON draft_appearance(court_case_id);

CREATE INDEX IF NOT EXISTS idx_court_case_merged_to_case_id
    ON court_case(merged_to_case_id);
