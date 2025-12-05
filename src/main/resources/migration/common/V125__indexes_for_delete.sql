CREATE INDEX IF NOT EXISTS idx_rsh_sentence_id ON recall_sentence_history (sentence_id);
CREATE INDEX IF NOT EXISTS idx_rsh_recall_history_id ON recall_sentence_history (recall_history_id);

CREATE INDEX IF NOT EXISTS idx_rh_prisoner_id ON recall_history (prisoner_id);

CREATE INDEX IF NOT EXISTS idx_court_case_history_original_court_case_id ON court_case_history (original_court_case_id);
