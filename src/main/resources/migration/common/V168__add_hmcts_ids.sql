ALTER TABLE court_appearance ADD COLUMN hmcts_court_hearing_id UUID NULL;
ALTER TABLE charge ADD COLUMN hmcts_charge_id UUID NULL;
ALTER TABLE next_court_appearance ADD COLUMN hmcts_court_hearing_id UUID NULL;
