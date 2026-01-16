ALTER TABLE immigration_detention RENAME COLUMN status_id to status_id_old;
ALTER TABLE immigration_detention ADD COLUMN status_id VARCHAR;

UPDATE immigration_detention
SET status_id = CASE status_id_old
                    WHEN 0 THEN 'ACTIVE'
                    WHEN 1 THEN 'DELETED'
                    WHEN 2 THEN 'ACTIVE'
    END;

ALTER TABLE immigration_detention_history RENAME COLUMN status_id to status_id_old;
ALTER TABLE immigration_detention_history ADD COLUMN status_id VARCHAR;

UPDATE immigration_detention_history
SET status_id = CASE status_id_old
                    WHEN 0 THEN 'ACTIVE'
                    WHEN 1 THEN 'DELETED'
                    WHEN 2 THEN 'ACTIVE'
    END;

ALTER TABLE immigration_detention ALTER COLUMN status_id SET NOT NULL;
ALTER TABLE immigration_detention_history ALTER COLUMN status_id SET NOT NULL;

ALTER TABLE immigration_detention DROP COLUMN status_id_old;
ALTER TABLE immigration_detention_history DROP COLUMN status_id_old;
ALTER TABLE immigration_detention_history DROP COLUMN history_status_id;
