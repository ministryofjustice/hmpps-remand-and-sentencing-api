CREATE INDEX IF NOT EXISTS idx_recall_prisoner_id ON recall(prisoner_id);

CREATE INDEX IF NOT EXISTS idx_recall_sentence_recall_id ON recall_sentence(recall_id);
