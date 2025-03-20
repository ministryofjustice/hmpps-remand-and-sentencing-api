ALTER TABLE appearance_charge DROP CONSTRAINT appearance_charge_pkey;

ALTER TABLE appearance_charge ADD CONSTRAINT appearance_charge_pkey PRIMARY KEY (appearance_id, charge_id);

ALTER TABLE appearance_charge DROP COLUMN id;