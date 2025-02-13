TRUNCATE TABLE recall_sentence;
TRUNCATE TABLE sentence CASCADE;
INSERT INTO sentence (lifetime_sentence_uuid, sentence_uuid, charge_number, status_id, created_by_username, created_prison, created_at, sentence_serve_type)
VALUES ('550e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440000', '1', '0', 'testuser', 'HMI', now(), 'FORTHWITH'),
('550e8400-e29b-41d4-a716-446655449999', '550e8400-e29b-41d4-a716-446655449999', '2', '0', 'testuser2', 'PRI', now(), 'CONCURRENT');
