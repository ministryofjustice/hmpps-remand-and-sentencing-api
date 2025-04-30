TRUNCATE TABLE recall_sentence CASCADE;
TRUNCATE TABLE recall CASCADE;
INSERT INTO recall (recall_uuid, prisoner_id, revocation_date, return_to_custody_date, recall_type_id, created_at, created_by_username, created_prison) VALUES ('550e8400-e29b-41d4-a716-446655440000', 'A12345B', '2024-07-01', '2024-07-01', 4, '2024-07-01 10:00:00+00', 'admin_user', 'HMI');
INSERT INTO recall (recall_uuid, prisoner_id, revocation_date, return_to_custody_date, recall_type_id, created_at, created_by_username, created_prison) VALUES ('550e8400-e29b-41d4-a716-446655440001', 'A12345B', '2024-07-02', '2024-07-02', 4, '2024-07-02 10:00:00+00', 'admin_user', 'HMI');
