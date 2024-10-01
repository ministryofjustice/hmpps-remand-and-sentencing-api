ALTER TABLE charge_outcome ADD COLUMN outcome_uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE charge_outcome ADD COLUMN nomis_code varchar NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE charge_outcome ADD COLUMN outcome_type varchar NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE charge_outcome ADD COLUMN display_order int NOT NULL DEFAULT 0;
ALTER TABLE charge ALTER COLUMN charge_outcome_id DROP NOT NULL;

INSERT INTO charge_outcome (outcome_name, outcome_uuid, nomis_code, outcome_type, display_order) VALUES
('Imprisonment', 'f17328cf-ceaa-43c2-930a-26cf74480e18', '1002', 'SENTENCING', 10),
('Remand on conditional bail', '70bf3a77-9f66-46cb-ba27-0f9b25682b44', '4530', 'REMAND', 10),
('Lie on file', '76ec0de0-da17-44a8-9184-790770d16d46', '2008', 'REMAND', 20),
('Not guilty', '86776327-7e1f-4830-bd4e-69168b3b0197', '2004', 'REMAND', 30),
('Commit to Crown Court for trial in custody', 'dd912c55-ca0d-4a68-8b0d-ba0a5e73b471', '4560', 'REMAND', 40),
('No seperate penalty', 'f9042ecc-6bed-4872-bbed-1b0938bcecd7', '1057', 'REMAND', 50),
('Suspended imprisonment', 'fa6c0ada-c6e8-4f7a-ab57-ec46a51c41f7', '1115', 'SENTENCING', 20),
('Send to Crown Court for trial ', '8976a19b-ab84-4881-b8c7-cf7b1978a262', '4565', 'REMAND', 60),
('Remand in custody', '315280e5-d53e-43b3-8ba6-44da25676ce2', '4531', 'REMAND', 70),
('Community order', '28cc6399-9849-48b5-b8e0-59f0163bbea6', '1116', 'REMAND', 80),
('Dismissed', 'e4c69c8a-9320-4126-9101-5674191ff37e', '2006', 'REMAND', 90),
('No evidence offered - dismissed', '80f2af70-dcf5-4f40-834c-8063bea47ce7', '2050', 'REMAND', 100),
('Discharged', '0e9437fa-d9bc-4588-8105-dc932163c624', '1019', 'REMAND', 110),
('Convicted awaiting sentence', 'ec2499bb-4413-4601-86f3-3fe70fe6f8a8', '4004', 'REMAND', 120),
('Discontinuance', '6ff42a7a-6e88-42ef-8671-5438bd221324', '2053', 'REMAND', 130),
('Guilty', '0828e8a8-c734-4c7f-8f66-a65302170f5c', 'G', 'REMAND', 140),
('Commit to Crown Court for sentence in custody', 'daed8a56-a5e7-4bba-807a-ede788f2f078', '4016', 'REMAND', 150),
('Withdrawn final', '11acbed7-58d9-4efb-9609-ff4a52fc61e1', '2063', 'REMAND', 160),
('Withdrawn', '6d2eb21d-ec02-48fa-9fcd-02e73b8e45ca', '2051', 'REMAND', 170),
('Imprisonment in default of a fine', 'cc7efcbd-88a1-4cfd-99aa-8e8eacc8e8cb', '1510', 'SENTENCING', 30),
('Proceedings stayed', '8fc275c5-dbfc-45b7-b18c-efb9e2bb22f1', '2061', 'REMAND', 180),
('Fine', '16a76712-0312-4069-a595-ce2d0698bcd5', '1015', 'REMAND', 190),
('Detention and training order', '0460ad51-04ea-402a-a249-b152b052a385', '1081', 'REMAND', 200),
('Adjourned', '23e9e49c-96ec-4493-8768-6fbc0247ace3', '4506', 'REMAND', 210),
('Commit to Crown Court for sentence', '6cb25a56-56df-4e9e-a8e9-a430d8b5822a', '4001', 'REMAND', 175),
('Immigration detainee', 'ebe76f25-3371-4789-9b19-c6b6048cd6b0', '5500', 'REMAND', 178),
('Remittal for sentence in custody', 'fda77ebe-2218-4401-815f-4feba11abea8', '4012', 'REMAND', 195),
('Remittal for trial in custody', 'a41c4f3a-6de8-486b-ad6f-c3a26bb9e840', '4588', 'REMAND', 198);

alter table appearance_outcome ADD COLUMN related_charge_outcome_uuid UUID;

update appearance_outcome set related_charge_outcome_uuid='f17328cf-ceaa-43c2-930a-26cf74480e18' where outcome_uuid='62412083-9892-48c9-bf01-7864af4a8b3c';
update appearance_outcome set related_charge_outcome_uuid='cc7efcbd-88a1-4cfd-99aa-8e8eacc8e8cb' where outcome_uuid='63e74d53-2e12-44d7-b35d-1f8179ff8544';
update appearance_outcome set related_charge_outcome_uuid='fa6c0ada-c6e8-4f7a-ab57-ec46a51c41f7' where outcome_uuid='e9a23672-2390-460e-bd2c-294efc2c307f';
update appearance_outcome set related_charge_outcome_uuid='23e9e49c-96ec-4493-8768-6fbc0247ace3' where outcome_uuid='301a878c-44b2-4474-b51e-0d9ea2ac765f';
update appearance_outcome set related_charge_outcome_uuid='daed8a56-a5e7-4bba-807a-ede788f2f078' where outcome_uuid='fe3ec254-f28d-41a0-b76c-5fa7901b14dc';
update appearance_outcome set related_charge_outcome_uuid='8976a19b-ab84-4881-b8c7-cf7b1978a262' where outcome_uuid='03bc2546-13dc-470e-8dfe-abde9711455e';
update appearance_outcome set related_charge_outcome_uuid='dd912c55-ca0d-4a68-8b0d-ba0a5e73b471' where outcome_uuid='fb966699-7adf-4c58-8852-395951e77846';
update appearance_outcome set related_charge_outcome_uuid='6cb25a56-56df-4e9e-a8e9-a430d8b5822a' where outcome_uuid='5595c7b9-2075-4431-be5c-0e0fcf6e2c9f';
update appearance_outcome set related_charge_outcome_uuid='28cc6399-9849-48b5-b8e0-59f0163bbea6' where outcome_uuid='ba6b09fc-9abd-4e9d-a128-357c31508118';
update appearance_outcome set related_charge_outcome_uuid='0460ad51-04ea-402a-a249-b152b052a385' where outcome_uuid='bcc438da-b3b4-4ca8-a870-9d17543e4317';
update appearance_outcome set related_charge_outcome_uuid='0e9437fa-d9bc-4588-8105-dc932163c624' where outcome_uuid='788fa741-4597-477a-9eec-2301456104d0';
update appearance_outcome set related_charge_outcome_uuid='6ff42a7a-6e88-42ef-8671-5438bd221324' where outcome_uuid='138b7f83-93d0-4e5a-bc4a-5caf06558820';
update appearance_outcome set related_charge_outcome_uuid='e4c69c8a-9320-4126-9101-5674191ff37e' where outcome_uuid='05591ff8-36ee-4f14-af08-a3c940b51fa9';
update appearance_outcome set related_charge_outcome_uuid='16a76712-0312-4069-a595-ce2d0698bcd5' where outcome_uuid='1709089f-0cf8-4544-8229-a2251e671241';
update appearance_outcome set related_charge_outcome_uuid='0828e8a8-c734-4c7f-8f66-a65302170f5c' where outcome_uuid='5dbd94e5-70f2-43ea-ace5-d4c1f27b7ebb';
update appearance_outcome set related_charge_outcome_uuid='ebe76f25-3371-4789-9b19-c6b6048cd6b0' where outcome_uuid='b6bdf776-809b-41f1-bc20-9efb04c1c8d7';
update appearance_outcome set related_charge_outcome_uuid='76ec0de0-da17-44a8-9184-790770d16d46' where outcome_uuid='876e9469-d8e3-412c-8f5a-6e8ae190bd2f';
update appearance_outcome set related_charge_outcome_uuid='80f2af70-dcf5-4f40-834c-8063bea47ce7' where outcome_uuid='1f9cdf32-8e28-498e-8e6a-065df21657dd';
update appearance_outcome set related_charge_outcome_uuid='86776327-7e1f-4830-bd4e-69168b3b0197' where outcome_uuid='6f929f8a-6a35-47b8-99f7-584ca61d1690';
update appearance_outcome set related_charge_outcome_uuid='8fc275c5-dbfc-45b7-b18c-efb9e2bb22f1' where outcome_uuid='379f3c62-0c85-4492-8c29-765671c7ae87';
update appearance_outcome set related_charge_outcome_uuid='315280e5-d53e-43b3-8ba6-44da25676ce2' where outcome_uuid='2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8';
update appearance_outcome set related_charge_outcome_uuid='70bf3a77-9f66-46cb-ba27-0f9b25682b44' where outcome_uuid='31c6974b-fb06-4dc4-95a0-9612df39ae12';
update appearance_outcome set related_charge_outcome_uuid='fda77ebe-2218-4401-815f-4feba11abea8' where outcome_uuid='4d2d03cf-eb04-4928-b96a-700e19651415';
update appearance_outcome set related_charge_outcome_uuid='a41c4f3a-6de8-486b-ad6f-c3a26bb9e840' where outcome_uuid='29efebd6-eccb-4bf7-acaa-7ab0d9a4ffb3';
update appearance_outcome set related_charge_outcome_uuid='ec2499bb-4413-4601-86f3-3fe70fe6f8a8' where outcome_uuid='5b012deb-75c6-4ac2-8403-647f42b22ab8';
update appearance_outcome set related_charge_outcome_uuid='11acbed7-58d9-4efb-9609-ff4a52fc61e1' where outcome_uuid='80dc6ca8-9be5-44f3-add6-2b725041ca80';

