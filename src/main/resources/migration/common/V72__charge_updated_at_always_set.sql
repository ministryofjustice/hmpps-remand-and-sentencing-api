update charge set updated_at = created_at where updated_at = null;
alter table charge alter column updated_at SET not null;