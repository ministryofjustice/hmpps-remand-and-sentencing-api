ALTER TABLE charge DROP COLUMN charge_uuid;
ALTER TABLE charge RENAME COLUMN lifetime_charge_uuid TO charge_uuid;
ALTER TABLE charge ADD COLUMN updated_at timestamp with time zone;
ALTER TABLE charge ADD COLUMN updated_by VARCHAR;
ALTER TABLE charge ADD COLUMN updated_prison VARCHAR;
