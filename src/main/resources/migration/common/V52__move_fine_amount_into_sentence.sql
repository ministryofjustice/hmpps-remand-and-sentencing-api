ALTER TABLE sentence ADD COLUMN fine_amount numeric(11, 2) NULL;
ALTER TABLE sentence_history ADD COLUMN fine_amount numeric(11, 2) NULL;
DROP TABLE fine_amount_history;
DROP TABLE fine_amount;
