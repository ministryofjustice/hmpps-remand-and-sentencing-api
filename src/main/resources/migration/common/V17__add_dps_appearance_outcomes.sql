ALTER TABLE appearance_outcome ADD COLUMN outcome_uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE appearance_outcome ADD COLUMN nomis_code varchar NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE appearance_outcome ADD COLUMN outcome_type varchar NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE appearance_outcome ADD COLUMN display_order int NOT NULL DEFAULT 0;
ALTER TABLE court_appearance ALTER COLUMN appearance_outcome_id DROP NOT NULL;

INSERT INTO appearance_outcome (outcome_name, outcome_uuid, nomis_code, outcome_type, display_order) VALUES
('Imprisonment', '62412083-9892-48c9-bf01-7864af4a8b3c', '1002', 'SENTENCING', 10),
('Imprisonment in default of a fine', '63e74d53-2e12-44d7-b35d-1f8179ff8544', '1510', 'SENTENCING', 20),
('Suspended imprisonment', 'e9a23672-2390-460e-bd2c-294efc2c307f', '1115', 'SENTENCING', 30),
('Adjourned', '301a878c-44b2-4474-b51e-0d9ea2ac765f', '4506', 'REMAND', 110),
('Commit to Crown Court for sentence in custody', 'fe3ec254-f28d-41a0-b76c-5fa7901b14dc', '4016', 'REMAND', 50),
('Send to Crown Court for trial ', '03bc2546-13dc-470e-8dfe-abde9711455e', '4565', 'REMAND', 30),
('Commit to Crown Court for trial in custody', 'fb966699-7adf-4c58-8852-395951e77846', '4560', 'REMAND', 10),
('Commit to Crown Court for sentence', '5595c7b9-2075-4431-be5c-0e0fcf6e2c9f', '4001', 'REMAND', 90),
('Community order', 'ba6b09fc-9abd-4e9d-a128-357c31508118', '1116', 'REMAND', 120),
('Detention and training order', 'bcc438da-b3b4-4ca8-a870-9d17543e4317', '1081', 'REMAND', 200),
('Discharged', '788fa741-4597-477a-9eec-2301456104d0', '1019', 'REMAND', 190),
('Discontinuance', '138b7f83-93d0-4e5a-bc4a-5caf06558820', '2053', 'REMAND', 140),
('Dismissed', '05591ff8-36ee-4f14-af08-a3c940b51fa9', '2006', 'REMAND', 160),
('Fine', '1709089f-0cf8-4544-8229-a2251e671241', '1015', 'REMAND', 180),
('Guilty', '5dbd94e5-70f2-43ea-ace5-d4c1f27b7ebb', 'G', 'REMAND', 60),
('Immigration detainee', 'b6bdf776-809b-41f1-bc20-9efb04c1c8d7', '5500', 'REMAND', 100),
('Lie on file', '876e9469-d8e3-412c-8f5a-6e8ae190bd2f', '2008', 'REMAND', 220),
('No evidence offered - dismissed', '1f9cdf32-8e28-498e-8e6a-065df21657dd', '2050', 'REMAND', 170),
('Not guilty', '6f929f8a-6a35-47b8-99f7-584ca61d1690', '2004', 'REMAND', 150),
('Proceedings stayed', '379f3c62-0c85-4492-8c29-765671c7ae87', '2061', 'REMAND', 230),
('Remand in custody', '2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8', '4531', 'REMAND', 20),
('Remand on conditional bail', '31c6974b-fb06-4dc4-95a0-9612df39ae12', '4530', 'REMAND', 80),
('Remittal for sentence in custody', '4d2d03cf-eb04-4928-b96a-700e19651415', '4012', 'REMAND', 70),
('Remittal for trial in custody', '29efebd6-eccb-4bf7-acaa-7ab0d9a4ffb3', '4588', 'REMAND', 130),
('Convicted awaiting sentence', '5b012deb-75c6-4ac2-8403-647f42b22ab8', '4004', 'REMAND', 40),
('Withdrawn final', '80dc6ca8-9be5-44f3-add6-2b725041ca80', '2063', 'REMAND', 210);
