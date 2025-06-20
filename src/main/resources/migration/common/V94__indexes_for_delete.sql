CREATE INDEX IF NOT EXISTS idx_sentence_consecutive_to_id ON sentence(consecutive_to_id);
CREATE INDEX IF NOT EXISTS idx_sentence_superseding_sentence_id ON sentence(superseding_sentence_id);
