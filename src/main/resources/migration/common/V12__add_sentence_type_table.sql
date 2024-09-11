CREATE TABLE sentence_type(
    id SERIAL PRIMARY KEY,
    sentence_type_uuid UUID NOT NULL,
    description VARCHAR NOT NULL,
    min_age_inclusive int,
    max_age_exclusive int,
    min_date_inclusive date,
    max_date_exclusive date,
    classification VARCHAR NOT NULL
);

INSERT INTO sentence_type(sentence_type_uuid, description, min_age_inclusive, max_age_exclusive, min_date_inclusive, max_date_exclusive, classification) values
('100f4921-38dd-45be-846f-edfb1fba5343', 'Legacy (1967 Act)',null,null,null,'2020-12-01','LEGACY'),
('dd82452a-e973-4783-b72f-cb45a89511f6', 'Legacy (1991 Act)',null,null,null,'2020-12-01','LEGACY'),
('cbc9685c-7d79-401a-9252-c70033ad184b', 'Violent Offender Order',18,null,'2020-12-01',null,'NON_CUSTODIAL'),
('1ae747ca-6341-4dd9-9b7e-36122539f606', 'Youth Rehabilitation Order',null,18,'2020-12-01',null,'NON_CUSTODIAL'),
('18d5af6d-2fa7-4166-a4c9-8381a1e3c7e0', 'EDS (Extended Determinate Sentence)',18,21,'2020-12-01',null,'EXTENDED'),
('0da58738-2db6-4fba-8f65-438284019756', 'EDS (Extended Determinate Sentence)',21,null,'2020-12-01',null,'EXTENDED'),
('f1ddc9d6-3c2e-419a-b54d-4e05c92a0f55', 'EDS (Extended Determinate Sentence)',null,18,'2020-12-01',null,'EXTENDED'),
('71eeafd6-c39d-496e-8e50-87363bf71cf0', 'Extended Sentence for Public Protection',null,null,null,'2020-12-01','EXTENDED'),
('cecc102c-424f-4a92-a789-915c07d38d93', 'EDS LASPO Automatic Release',18,null,null,'2020-12-01','EXTENDED'),
('0cebfbdf-accd-42bf-8f90-acb00bc31f4d', 'EDS LASPO Discretionary Release',18,null,null,'2020-12-01','EXTENDED'),
('b14be497-e9fe-4b67-913b-91b6c27a57c4', 'Serious Terrorism Sentence',18,21,'2020-12-01',null,'EXTENDED'),
('0e822fe7-80dd-4c5d-b477-33b7109058c9', 'Serious Terrorism Sentence',21,null,'2020-12-01',null,'EXTENDED'),
('8d04557c-8e54-4e2a-844f-272163fca833', 'SDS (Standard Determinate Sentence)',18,null,null,'2020-12-01','STANDARD'),
('02fe3513-40a6-47e9-a72d-9dafdd936a0e', 'SDS (Standard Determinate Sentence)',21,null,'2020-12-01',null,'STANDARD'),
('6d0948c1-243b-4a2d-8b23-0721a5a1a949', 'ORA SDS (Offender rehabilitation act standard determinate sentence)',18,null,null,'2020-12-01','STANDARD'),
('e138374d-810f-4718-a81a-1c9d4745031e', 'ORA SDS (Offender rehabilitation act standard determinate sentence)',21,null,'2020-12-01',null,'STANDARD'),
('1104e683-5467-4340-b961-ff53672c4f39', 'SDS (Standard Determinate Sentence)',null,18,'2020-12-01',null,'STANDARD'),
('f1fb11de-aa51-4a74-8c5c-9c08ada6db37', 'ORA (Offender rehabilitation act)',null,18,'2020-12-01',null,'STANDARD'),
('6ad464cc-08d9-438c-85c4-3352c25756c4', 'Serious Offence -18 CJA03 POCCA 2000',null,18,null,'2020-12-01','STANDARD'),
('d34c90fe-cc49-4ac0-8ead-ca3fcc261b24', 'ORA (Offender rehabilitation act)',null,18,null,'2020-12-01','STANDARD'),
('87ef5d34-5912-411a-8733-2b8cf0cfc8e5', 'YOI (Young offender institution)',null,18,null,'2020-12-01','STANDARD'),
('3bfbf00d-747e-4576-8d96-aa4f8f9832ef', 'YOI (Young offender institution)',18,21,'2020-12-01',null,'STANDARD'),
('babc059d-53aa-423c-9129-be2ba86026ad', 'YOI ORA (Young offender Institution offender rehabilitation act)',null,18,null,'2020-12-01','STANDARD'),
('d64283fd-7e48-4ed8-a98c-d68e938a5661', 'YOI ORA (Young offender Institution offender rehabilitation act)',18,21,'2020-12-01',null,'STANDARD'),
('5fc63cba-510f-4122-8fdd-649fbcd52efb', 'SDOPC (Special sentence of detention for terrorist offenders of particular concern)',null,18,'2020-12-01',null,'SOPC'),
('b4769983-fd23-4c95-9f47-5e5eb3aef491', 'Section 236A SOPC CJA03',18,null,null,'2020-12-01','SOPC'),
('4d434819-4aff-442a-8c78-64dfcfba61cc', 'SOPC (offenders of a particular concern)',18,21,'2020-12-01',null,'SOPC'),
('deb22e75-2335-44de-8a9f-1047d58c9b04', 'SOPC (offenders of a particular concern)',21,null,'2020-12-01',null,'SOPC'),
('4e745d45-2c42-48b3-998d-0c7d1a19a8fc', 'Automatic Life',18,null,null,'2020-12-01','INDETERMINATE'),
('496cdc0f-0136-413f-ab6b-c24ce53b8f1e', 'Automatic Life',21,null,'2020-12-01',null,'INDETERMINATE'),
('4eaf630e-1629-43a0-aaef-31dfd805e54a', 'Automatic Life Sec 273 Sentencing Code (18 - 20)',18,21,'2020-12-01',null,'INDETERMINATE'),
('a501bc80-2d82-44c4-98af-17467620463c', 'Automatic Life Sec 224A 03',18,null,null,'2020-12-01','INDETERMINATE'),
('143c501e-b4dd-4709-8cef-7e06168fc6db', 'Detention For Life',null,21,null,'2020-12-01','INDETERMINATE'),
('e5a4cfc1-7b7d-4cbe-9e7f-863decbf3787', 'Detention For Life',null,21,'2020-12-01',null,'INDETERMINATE'),
('a119bf52-e2d1-45bd-9fcc-34ecd76cc2fc', 'Adult Discretionary Life',18,null,null,'2020-12-01','INDETERMINATE'),
('80a95619-7477-44c6-8bc6-29d5486fefe3', 'Adult Discretionary Life',18,null,'2020-12-01',null,'INDETERMINATE'),
('a12c98e1-7321-4c41-9684-6a79cd789af8', 'Detention For Public Protection',null,18,null,'2020-12-01','INDETERMINATE'),
('8f23e11b-dfa6-4d97-81ca-07ccd5f9983a', 'Detention During His Majesty''s Pleasure',null,21,null,'2020-12-01','INDETERMINATE'),
('69811a64-c18a-4e18-9f9a-87a3fa7ee0bd', 'Detention During His Majesty''s Pleasure',null,21,'2020-12-01',null,'INDETERMINATE'),
('ba057d8b-07ba-4779-a704-5b54e6135dcf', 'Indeterminate Sentence for the Public Protection',18,null,null,'2020-12-01','INDETERMINATE'),
('e3b69eb7-6362-4d8b-affe-480abcfd35f7', 'Adult Mandatory Life',18,null,null,'2020-12-01','INDETERMINATE'),
('61ae9cdf-ae37-4a91-8216-deb9fef7330e', 'Adult Mandatory Life',21,null,'2020-12-01',null,'INDETERMINATE'),
('d8d38763-bee6-474e-8988-7dfa3d02f3ae', 'Custody For Life Sec 272 Sentencing Code (18 - 20)',18,21,'2020-12-01',null,'INDETERMINATE'),
('3725c368-fbfb-4e46-babf-12aabc2a7f91', 'Custody For Life Sec 275 Sentencing Code (Murder) (U21)',null,21,'2020-12-01',null,'INDETERMINATE'),
('80584016-4bab-4408-a8f2-b142830819e5', 'Custody For Life - Under 21 CJA03',null,21,null,'2020-12-01','INDETERMINATE'),
('b7b6a775-6146-484f-a8a6-92f2ed8fa6ae', 'Custody Life (18-21 Years Old)',18,21,null,'2020-12-01','INDETERMINATE'),
('d74201de-2154-4096-891a-62237dcef23b', 'ORA Breach Top Up Supervision',18,null,null,'2020-12-01','BOTUS'),
('d721e4c9-6ba8-47b7-8744-c58ef2703eab', 'ORA Breach Top Up Supervision',18,null,'2020-12-01',null,'BOTUS'),
('514eb0fc-239c-43e2-912f-139dc26d4473', 'Civil Imprisonment',18,null,null,'2020-12-01','CIVIL'),
('18826041-43f4-402b-b529-56c58fa8bc3c', 'Civil Imprisonment',18,null,'2020-12-01',null,'CIVIL'),
('e8098305-f909-4cf5-9b06-f71a4e4d9df0', 'Detention and Training Order',null,18,null,'2020-12-01','DTO'),
('cab9e914-e0de-48d0-9e72-0e1fc9a19cf4', 'Detention and Training Order',null,18,'2020-12-01',null,'DTO'),
('b6862370-e8f0-4680-8797-0aca9cacb302', 'ORA Detention and Training Order',null,18,null,'2020-12-01','DTO'),
('903ca33b-e264-4a16-883d-fee03a2a3396', 'ORA Detention and Training Order',null,18,'2020-12-01',null,'DTO'),
('cd92d39b-64f3-484c-b5b2-fbe0a7dc3a50', 'Section 104',null,18,null,'2020-12-01','DTO'),
('115f1ebb-2ea2-4de6-8434-d6a957ab3aa5', 'Section 105',null,18,null,'2020-12-01','DTO'),
('c71ceefe-932b-4a69-b87c-7c1294e37cf7', 'Imprisonment in Default of Fine',null,null,'2020-12-01',null,'FINE');