TRUNCATE TABLE recall_sentence;
TRUNCATE TABLE sentence CASCADE;
TRUNCATE TABLE charge CASCADE;
TRUNCATE TABLE court_case CASCADE;
TRUNCATE TABLE court_appearance CASCADE;
TRUNCATE TABLE appearance_charge CASCADE;

INSERT INTO charge(
     id, charge_uuid, offence_code, offence_start_date, status_id, charge_outcome_id, terror_related, created_at, created_by, created_prison, updated_at)
VALUES (1, 'c923b281-b64d-435d-bec8-da6c1cdcb7d0', 'ABC' , now(), 0, 1, false, now(), 'testuser', 'HMI', now()),
       (2, '11ce3609-c16b-4f23-947a-30ae75dafd7b', 'ABC' , now(), 0, 1, false, now(), 'testuser', 'HMI', now());

INSERT INTO court_case(
    id, prisoner_id, case_unique_identifier, created_at, created_by, status_id, created_prison)
VALUES (1,'A12345B', '5725bfeb-23db-439f-ab4b-2ea4e74cd2b5', now(), 'testuser', 0, 'HMI'),
       (2,'A12345B', '846799d8-ce70-4a29-a630-382c904349ae', now(), 'testuser', 0, 'HMI');


INSERT INTO court_appearance(
    id, appearance_uuid, appearance_outcome_id, court_case_id, court_code, court_case_reference, appearance_date, status_id, created_at, created_by, created_prison, warrant_type, overall_conviction_date)
VALUES (1, '11158e7c-fbf1-4b1b-b62a-af82f7b5c1ab', 1, 1, 'ABC', 'CASEREF1', now(), 0, now(), 'testuser', 'HMI', 'SENTENCING', now() ),
       (2, '5459233d-6ba2-4795-8d61-251fa0a3802d', 1, 2, 'ABC', 'CASEREF1', now(), 0, now(), 'testuser', 'HMI', 'SENTENCING', now() );


INSERT INTO appearance_charge(
    appearance_id, charge_id, created_at, created_by)
VALUES (1, 1, now(),'testuser2'), (2, 2, now(),'testuser2');

INSERT INTO sentence (sentence_uuid, charge_number, charge_id, status_id, created_by, created_prison, created_at, sentence_serve_type)
VALUES ('550e8400-e29b-41d4-a716-446655440000', '1', 1,  '0', 'testuser', 'HMI', now(), 'FORTHWITH'),
('550e8400-e29b-41d4-a716-446655449999', '2', 2, '0', 'testuser2', 'HMI', now(), 'CONCURRENT');
