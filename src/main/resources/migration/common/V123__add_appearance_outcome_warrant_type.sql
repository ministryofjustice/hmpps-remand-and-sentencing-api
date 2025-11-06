ALTER TABLE appearance_outcome ADD COLUMN warrant_type VARCHAR;

update appearance_outcome set warrant_type = outcome_type;
