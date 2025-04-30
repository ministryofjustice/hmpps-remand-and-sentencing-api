ALTER TABLE recall_sentence
    ADD CONSTRAINT fk_recall_sentence_to_recall FOREIGN KEY (recall_id) REFERENCES recall (id);