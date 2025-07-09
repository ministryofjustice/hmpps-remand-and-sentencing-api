-- Adding source column to recall and recall_history
ALTER TABLE recall ADD COLUMN source VARCHAR NOT NULL DEFAULT 'DPS';
UPDATE recall SET source = 'NOMIS' WHERE created_by_username = 'hmpps-prisoner-from-nomis-migration-court-sentencing-1';

ALTER TABLE recall_history ADD COLUMN source VARCHAR NOT NULL DEFAULT 'DPS';
UPDATE recall_history SET source = 'NOMIS' WHERE created_by_username = 'hmpps-prisoner-from-nomis-migration-court-sentencing-1';
