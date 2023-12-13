CREATE INDEX idx_appearance_outcome_name ON appearance_outcome (outcome_name);
CREATE INDEX idx_charge_outcome_name ON charge_outcome (outcome_name);
CREATE UNIQUE INDEX idx_charge_charge_uuid ON charge (charge_uuid);
CREATE UNIQUE INDEX idx_court_appearance_appearance_uuid ON court_appearance (appearance_uuid);