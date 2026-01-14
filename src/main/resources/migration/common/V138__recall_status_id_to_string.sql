ALTER TABLE recall ADD COLUMN status VARCHAR;

UPDATE recall
SET status = CASE status_id
                    WHEN 0 THEN 'ACTIVE'
                    WHEN 1 THEN 'INACTIVE'
                    WHEN 2 THEN 'EDITED'
                    WHEN 3 THEN 'DELETED'
                    WHEN 4 THEN 'FUTURE'
                    WHEN 5 THEN 'MERGED'
                    WHEN 6 THEN 'MANY_CHARGES_DATA_FIX'
                    WHEN 7 THEN 'DUPLICATE'
                    WHEN 8 THEN 'RECALL_APPEARANCE'
                    WHEN 9 THEN 'IMMIGRATION_APPEARANCE'
END;

ALTER TABLE recall_history ADD COLUMN status VARCHAR;

UPDATE recall_history
SET status = CASE status_id
                 WHEN 0 THEN 'ACTIVE'
                 WHEN 1 THEN 'INACTIVE'
                 WHEN 2 THEN 'EDITED'
                 WHEN 3 THEN 'DELETED'
                 WHEN 4 THEN 'FUTURE'
                 WHEN 5 THEN 'MERGED'
                 WHEN 6 THEN 'MANY_CHARGES_DATA_FIX'
                 WHEN 7 THEN 'DUPLICATE'
                 WHEN 8 THEN 'RECALL_APPEARANCE'
                 WHEN 9 THEN 'IMMIGRATION_APPEARANCE'
    END;


ALTER TABLE recall ALTER COLUMN status SET NOT NULL;

ALTER TABLE recall_history ALTER COLUMN status SET NOT NULL;

ALTER TABLE recall DROP COLUMN status_id;

ALTER TABLE recall_history DROP COLUMN status_id;
ALTER TABLE recall_history DROP COLUMN history_status_id;
