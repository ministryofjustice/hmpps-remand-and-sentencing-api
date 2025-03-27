
CREATE TABLE legacy_sentence_types (
                                       id SERIAL PRIMARY KEY,
                                       classification VARCHAR NOT NULL,
                                       sentencing_act INTEGER NOT NULL,
                                       nomis_reference VARCHAR NOT NULL,
                                       nomis_active BOOLEAN NOT NULL,
                                       nomis_expiry_date DATE,
                                       nomis_terms JSONB NOT NULL,
                                       nomis_description VARCHAR NOT NULL,
                                       eligibility JSONB NOT NULL,
                                       recall_type VARCHAR,
                                       sentence_type_uuid UUID,
                                       CONSTRAINT fk_sentence_type_uuid
                                           FOREIGN KEY (sentence_type_uuid) REFERENCES sentence_type(sentence_type_uuid)
);

INSERT INTO legacy_sentence_types (
    nomis_reference, classification, sentencing_act, nomis_active, nomis_expiry_date, nomis_description, recall_type, eligibility, nomis_terms, sentence_type_uuid
) VALUES

      ('1', '', 1991, FALSE, '2009-05-23', 'Community Rehabilitation Order', '', '{}'::jsonb, '["LIC","SUP","DET"]'::jsonb, NULL),
      ('2', '', 1991, FALSE, '2009-05-23', 'Suspended Sentence Supervision Order', '', '{}'::jsonb, '["SUP","IMP","SUSP"]'::jsonb, NULL),
      ('3', '', 1991, FALSE, '2009-05-23', 'Money Payment Supervision Order', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('4', '', 1991, FALSE, '2009-05-23', 'Curfew Order', '', '{}'::jsonb, '["CUR"]'::jsonb, NULL),
      ('5', '', 1991, FALSE, '2009-05-23', 'C&YP Act 1969 Supervision Order', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('11', '', 1991, FALSE, '2009-05-23', 'Family Assistance Order', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('12', '', 1991, FALSE, '2009-05-23', 'Care and Supervision Order', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('15', '', 1991, FALSE, '2009-05-23', 'Drug Treatment and Testing Order - Low', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('20', 'INDETERMINATE', 1991, FALSE, '2009-07-27', 'Life Imprisonment', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('21', '', 1991, FALSE, '2009-07-27', 'Detention under s53(2) C&YP Act 1933', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('22', 'EXTENDED', 1991, FALSE, '2009-07-27', 'Extended Sent sentenced pre October 1992', '', '{}'::jsonb, '["IMP","SCUS"]'::jsonb, NULL),
      ('23', 'EXTENDED', 1991, FALSE, '2009-05-23', 'Extended Sent sentenced pre October 1992', '', '{}'::jsonb, '["LIC"]'::jsonb, NULL),
      ('25', '', 1991, FALSE, '2006-06-29', 'Detention Centre Order', '', '{}'::jsonb, '[]'::jsonb, NULL),
      ('28', '', 1991, FALSE, '2009-05-23', 'Psychiatric Hospital on Cond Dis', '', '{}'::jsonb, '["PSYCH"]'::jsonb, NULL),
      ('29', '', 1991, FALSE, '2009-05-23', 'Young Offender Institution', '', '{}'::jsonb, '["SUP","IMP","LIC"]'::jsonb, NULL),
      ('29', '', 2003, FALSE, '2009-05-23', 'Young Offender Institution', '', '{}'::jsonb, '["SUP","IMP","LIC"]'::jsonb, NULL),
      ('30', '', 1991, TRUE, NULL, 'Voluntary Supervision', '', '{}'::jsonb, '["IMP","LIC"]'::jsonb, NULL),
      ('31', '', 1991, FALSE, '2006-06-29', 'Adult Custody 12 months to 4 years (Automatic Conditional Release)', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('32', '', 1991, FALSE, '2006-06-29', 'Young Offender Institution for those sentences to more than 1 year', '', '{}'::jsonb, '[]'::jsonb, NULL),
      ('33', 'EXTENDED', 1991, TRUE, NULL, 'Sex Offender Sent Extended pre Oct 1998', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('34', 'EXTENDED', 1991, FALSE, '2009-05-23', 'Sex Offender Sent Extended post Oct 1998', '', '{}'::jsonb, '["LIC"]'::jsonb, NULL),
      ('35', '', 1991, FALSE, '2009-07-27', 'Custody with Extended Supervision for violent offenders', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('36', 'DTO', 1991, FALSE, '2009-11-18', 'Detention and Training Order', '', '{}'::jsonb, '[]'::jsonb, NULL),
      ('37', '', 1991, TRUE, NULL, 'Pre/Post Release s105 C&D Act 1998', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('38', '', 2003, FALSE, '2006-06-29', 'Intermittent Custody - weekday', '', '{}'::jsonb, '[]'::jsonb, NULL),
      ('39', '', 2003, FALSE, '2006-06-29', 'Intermittent Custody - weekends', '', '{}'::jsonb, '[]'::jsonb, NULL),
      ('41', '', 1991, FALSE, '2006-06-29', 'Adult Custody of 4 years or more (Discretionary Conditional Release)', '', '{}'::jsonb, '[]'::jsonb, NULL),
      ('42', '', 1991, FALSE, '2009-05-23', 'Drug Abstinence Order', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('43', '', 1991, FALSE, '2009-05-23', 'Drug Treatment and Testing Order - High', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('44', '', 1991, FALSE, '2009-05-23', 'Action Plan Order', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('45', '', 1991, FALSE, '2009-05-23', 'Reparation Order', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('46', '', 1991, FALSE, '2009-05-23', 'Parenting Order', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('47', '', 1991, FALSE, '2009-05-23', 'CPOrder for Persistent Petty Offenders', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('48', '', 1991, FALSE, '2009-05-23', 'CPOrder for Fine Defaulters', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('49', '', 1991, FALSE, '2009-05-23', 'Added CPO hours reflecting breach code 5', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('50', '', 1991, FALSE, '2009-05-23', 'Community Punishment Order', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('51', '', 1991, FALSE, '2009-05-23', 'Community Punishment component of CPRO', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('52', '', 1991, FALSE, '2009-05-23', 'Community Rehabilitation component of CPRO', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('54', '', 2003, FALSE, '2009-05-23', 'Community Order', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('55', '', 2003, FALSE, '2009-05-23', 'Deferred Sentence', '', '{}'::jsonb, '["DEF"]'::jsonb, NULL),
      ('56', '', 2003, FALSE, '2009-05-23', 'Suspended Sentence Order -Custody Minus', '', '{}'::jsonb, '["SUP","IMP","SUSP"]'::jsonb, NULL),
      ('57', '', 2003, FALSE, '2006-06-29', 'Custody Plus (Less than 12 months)', '', '{}'::jsonb, '[]'::jsonb, NULL),
      ('58', '', 2003, FALSE, '2006-06-29', 'Adult Custody', '', '{}'::jsonb, '[]'::jsonb, NULL),
      ('59', '', 2003, FALSE, NULL, 'Extended Public Protection Sentence <10 years (Sexual and Violent Offences)', '', '{}'::jsonb, '[]'::jsonb, NULL),
      ('60', 'INDETERMINATE', 2003, FALSE, NULL, 'Indeterminate Sentence >10 years (Sexual and Violent Offences)', '', '{}'::jsonb, '[]'::jsonb, NULL),
      ('61', '', 2003, FALSE, '2009-05-23', 'Interim Custody Plus Adult', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('62', '', 2003, FALSE, '2009-05-23', 'Interim Custody Plus Adult', '', '{}'::jsonb, '["SUP","IMP","LIC"]'::jsonb, NULL),
      ('72', '', 1991, FALSE, '2006-06-29', 'Attendance Centre Order', '', '{}'::jsonb, '[]'::jsonb, NULL),
      ('74', '', 1991, FALSE, '2006-06-29', 'Secure Training Order', '', '{}'::jsonb, '[]'::jsonb, NULL),
      ('77', '', 1991, FALSE, '2006-06-29', 'Restriction Order under the MHA 83 s44/41', '', '{}'::jsonb, '[]'::jsonb, NULL),
      ('78', 'STANDARD', 2003, TRUE, NULL, 'Violent Offender Order', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('79', 'STANDARD', 2003, TRUE, NULL, 'Youth Rehabilitation Order', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('14FTR_ORA', 'STANDARD', 2003, TRUE, NULL, 'ORA 14 Day Fixed Term Recall', 'FIXED_TERM_RECALL_14', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('14FTR_ORA', 'STANDARD', 2020, TRUE, NULL, 'ORA 14 Day Fixed Term Recall', 'FIXED_TERM_RECALL_14', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('14FTRHDC_ORA', 'STANDARD', 2003, TRUE, NULL, '14 Day Fixed Term Recall from HDC', 'FIXED_TERM_RECALL_14', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('14FTRHDC_ORA', 'STANDARD', 2020, TRUE, NULL, '14 Day Fixed Term Recall from HDC', 'FIXED_TERM_RECALL_14', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('4E', '', 1991, FALSE, '2006-06-29', 'Curfew Order with Electronic Monitoring', '', '{}'::jsonb, '[]'::jsonb, NULL),
      ('52V', '', 1991, FALSE, '2006-06-29', 'ICCP', '', '{}'::jsonb, '[]'::jsonb, NULL),
      ('A/FINE', 'AFINE', 1991, TRUE, NULL, 'Imprisoned in Default of a fine', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('A/FINE', 'AFINE', 2003, TRUE, NULL, 'Imprisonment in Default of Fine', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('A/FINE', 'AFINE', 2020, TRUE, NULL, 'Imprisonment in Default of Fine', '', '{}'::jsonb, '["IMP"]'::jsonb, 'c71ceefe-932b-4a69-b87c-7c1294e37cf7'),
      ('ADIMP', 'STANDARD', 2003, TRUE, NULL, 'CJA03 Standard Determinate Sentence', '', '{"toreraEligibilityType": "SDS", "sdsPlusEligibilityType": "SDS"}'::jsonb, '["LIC","IMP"]'::jsonb, '8d04557c-8e54-4e2a-844f-272163fca833'),
      ('ADIMP', 'STANDARD', 2020, TRUE, NULL, 'Sentencing Code Standard Determinate Sentence', '', '{"toreraEligibilityType": "SDS", "sdsPlusEligibilityType": "SDS"}'::jsonb, '["IMP"]'::jsonb, '02fe3513-40a6-47e9-a72d-9dafdd936a0e'),
      ('ADIMP_ORA', 'STANDARD', 2003, TRUE, NULL, 'ORA CJA03 Standard Determinate Sentence', '', '{"toreraEligibilityType": "SDS", "sdsPlusEligibilityType": "SDS"}'::jsonb, '["IMP"]'::jsonb, '6d0948c1-243b-4a2d-8b23-0721a5a1a949'),
      ('ADIMP_ORA', 'STANDARD', 2020, TRUE, NULL, 'ORA Sentencing Code Standard Determinate Sentence', '', '{"toreraEligibilityType": "SDS", "sdsPlusEligibilityType": "SDS"}'::jsonb, '["IMP"]'::jsonb, 'e138374d-810f-4718-a81a-1c9d4745031e'),
      ('ALP', 'INDETERMINATE', 1991, TRUE, NULL, 'Automatic Life', '', '{}'::jsonb, '["IMP"]'::jsonb, '4e745d45-2c42-48b3-998d-0c7d1a19a8fc'),
      ('ALP', 'INDETERMINATE', 2003, TRUE, NULL, 'Automatic LIfe', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('ALP', 'INDETERMINATE', 2020, TRUE, NULL, 'Automatic Life', '', '{}'::jsonb, '["IMP"]'::jsonb, '496cdc0f-0136-413f-ab6b-c24ce53b8f1e'),
      ('ALP_CODE18', 'INDETERMINATE', 2020, TRUE, NULL, 'Automatic Life Sec 273 Sentencing Code (18 - 20)', '', '{}'::jsonb, '["IMP"]'::jsonb, '4eaf630e-1629-43a0-aaef-31dfd805e54a'),
      ('ALP_CODE21', 'INDETERMINATE', 2020, FALSE, '2021-04-20', 'Automatic Life Sec 283 Sentencing Code (21+)', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('ALP_LASPO', 'INDETERMINATE', 2003, TRUE, NULL, 'Automatic Life Sec 224A 03', '', '{}'::jsonb, '["IMP"]'::jsonb, 'a501bc80-2d82-44c4-98af-17467620463c'),
      ('AR', 'STANDARD', 1991, TRUE, NULL, 'Adult Imprisonment less than 12 months', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('AR', 'STANDARD', 2003, FALSE, '2009-05-23', 'IMPRISONED FOR LESS THAN 12 MONTHS', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('BOTUS', 'BOTUS', 2003, TRUE, NULL, 'ORA Breach Top Up Supervision', '', '{}'::jsonb, '["IMP"]'::jsonb, 'd74201de-2154-4096-891a-62237dcef23b'),
      ('BOTUS', 'BOTUS', 2020, TRUE, NULL, 'ORA Breach Top Up Supervision', '', '{}'::jsonb, '["IMP"]'::jsonb, 'd721e4c9-6ba8-47b7-8744-c58ef2703eab'),
      ('CIVIL', 'CIVIL', 1991, TRUE, NULL, 'Civil Imprisonment', '', '{}'::jsonb, '["DET"]'::jsonb, NULL),
      ('CIVIL', 'CIVIL', 2003, TRUE, NULL, 'Civil Imprisonment', '', '{}'::jsonb, '["DET"]'::jsonb, '514eb0fc-239c-43e2-912f-139dc26d4473'),
      ('CIVIL', 'CIVIL', 2020, TRUE, NULL, 'Civil Imprisonment', '', '{}'::jsonb, '["DET"]'::jsonb, '18826041-43f4-402b-b529-56c58fa8bc3c'),
      ('CIVILLT', 'CIVIL', 1991, TRUE, NULL, 'CIVIL IMPRISONMENT OVER 12 MONTHS SENTENCE', '', '{}'::jsonb, '["DET"]'::jsonb, NULL),
      ('CP', '', 2003, FALSE, '2009-05-23', 'Custody Plus', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('CR', 'STANDARD', 1991, TRUE, NULL, 'Adult Imprison above 12 mths below 4 yrs', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('CUR', 'STANDARD', 1991, TRUE, NULL, 'Recalled from Curfew Conditions', 'STANDARD_RECALL_255', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('CUR', 'STANDARD', 2003, TRUE, NULL, 'Breach of Curfew', 'STANDARD_RECALL_255', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('CUR', 'STANDARD', 2020, TRUE, NULL, 'Breach of Curfew', 'STANDARD_RECALL_255', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('CUR_ORA', 'STANDARD', 2003, TRUE, NULL, 'ORA Recalled from Curfew Conditions', 'STANDARD_RECALL_255', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('CUR_ORA', 'STANDARD', 2020, TRUE, NULL, 'ORA Recalled from Curfew Conditions', 'STANDARD_RECALL_255', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('DFL', 'INDETERMINATE', 1991, TRUE, NULL, 'Detention For Life', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('DFL', 'INDETERMINATE', 2003, TRUE, NULL, 'Detention For Life', '', '{}'::jsonb, '["IMP"]'::jsonb, '143c501e-b4dd-4709-8cef-7e06168fc6db'),
      ('DFL', 'INDETERMINATE', 2020, TRUE, NULL, 'Detention For Life', '', '{}'::jsonb, '["IMP"]'::jsonb, 'e5a4cfc1-7b7d-4cbe-9e7f-863decbf3787'),
      ('DLP', 'INDETERMINATE', 1991, TRUE, NULL, 'Adult Discretionary Life', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('DLP', 'INDETERMINATE', 2003, TRUE, NULL, 'Adult Discretionary Life', '', '{}'::jsonb, '["IMP"]'::jsonb, 'a119bf52-e2d1-45bd-9fcc-34ecd76cc2fc'),
      ('DLP', 'INDETERMINATE', 2020, TRUE, NULL, 'Adult Discretionary Life', '', '{}'::jsonb, '["IMP"]'::jsonb, '80a95619-7477-44c6-8bc6-29d5486fefe3'),
      ('DPP', 'INDETERMINATE', 1991, TRUE, NULL, 'Detention For Public Protection', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('DPP', 'INDETERMINATE', 2003, TRUE, NULL, 'Detention For Public Protection', '', '{}'::jsonb, '["IMP"]'::jsonb, 'a12c98e1-7321-4c41-9684-6a79cd789af8'),
      ('DTO', 'DTO', 1991, TRUE, NULL, 'Detention and Training Order', '', '{}'::jsonb, '["SEC104","SEC105","IMP"]'::jsonb, NULL),
      ('DTO', 'DTO', 2003, TRUE, NULL, 'Detention and Training Order', '', '{}'::jsonb, '["SEC104","SEC105","IMP"]'::jsonb, 'e8098305-f909-4cf5-9b06-f71a4e4d9df0'),
      ('DTO', 'DTO', 2020, TRUE, NULL, 'Detention and Training Order', '', '{}'::jsonb, '["SEC104","SEC105","IMP"]'::jsonb, 'cab9e914-e0de-48d0-9e72-0e1fc9a19cf4'),
      ('DTO_ORA', 'DTO', 2003, TRUE, NULL, 'ORA Detention and Training Order', '', '{}'::jsonb, '["SEC104","SEC105","IMP"]'::jsonb, 'b6862370-e8f0-4680-8797-0aca9cacb302'),
      ('DTO_ORA', 'DTO', 2020, TRUE, NULL, 'ORA Detention and Training Order', '', '{}'::jsonb, '["SEC104","SEC105","IMP"]'::jsonb, '903ca33b-e264-4a16-883d-fee03a2a3396'),
      ('EDS18', 'EXTENDED', 2020, TRUE, NULL, 'EDS Sec 266 Sentencing Code (18 - 20)', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, '18d5af6d-2fa7-4166-a4c9-8381a1e3c7e0'),
      ('EDS21', 'EXTENDED', 2020, TRUE, NULL, 'EDS Sec 279 Sentencing Code (21+)', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, '0da58738-2db6-4fba-8f65-438284019756'),
      ('EDSU18', 'EXTENDED', 2020, TRUE, NULL, 'EDS Sec 254 Sentencing Code (U18)', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, 'f1ddc9d6-3c2e-419a-b54d-4e05c92a0f55'),
      ('EPP', 'EXTENDED', 2003, TRUE, NULL, 'Extended Sent Public Protection CJA 03', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, '71eeafd6-c39d-496e-8e50-87363bf71cf0'),
      ('EXT', 'EXTENDED', 1991, TRUE, NULL, 'Sent Extended Sec 86 of PCC(S) Act 2000', '', '{}'::jsonb, '["LIC","IMP","SEC86"]'::jsonb, NULL),
      ('FTR', 'STANDARD', 2003, TRUE, NULL, 'Fixed Term Recall Pre ORA Sentence', 'FIXED_TERM_RECALL_28', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('FTR', 'STANDARD', 2020, TRUE, NULL, 'Fixed Term Recall Pre ORA Sentence', 'FIXED_TERM_RECALL_28', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('FTR_HDC', 'STANDARD', 2003, TRUE, NULL, 'Fixed Term Recall while on HDC', 'FIXED_TERM_RECALL_14', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('FTR_HDC', 'STANDARD', 2020, TRUE, NULL, 'Fixed Term Recall while on HDC', 'FIXED_TERM_RECALL_14', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('FTR_HDC_ORA', 'STANDARD', 2003, TRUE, NULL, 'ORA Fixed Term Recall while on HDC', 'FIXED_TERM_RECALL_28', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('FTR_HDC_ORA', 'STANDARD', 2020, TRUE, NULL, 'ORA Fixed Term Recall while on HDC', 'FIXED_TERM_RECALL_28', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('FTR_ORA', 'STANDARD', 2003, TRUE, NULL, 'ORA 28 Day Fixed Term Recall', 'FIXED_TERM_RECALL_28', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('FTR_ORA', 'STANDARD', 2020, TRUE, NULL, 'ORA 28 Day Fixed Term Recall', 'FIXED_TERM_RECALL_28', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('FTR_SCH15', 'STANDARD', 2003, TRUE, NULL, 'FTR Schedule 15 Offender', 'FIXED_TERM_RECALL_28', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('FTRSCH15_ORA', 'STANDARD', 2003, TRUE, NULL, 'ORA FTR Schedule 15 Offender', 'FIXED_TERM_RECALL_28', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('FTRSCH18', 'STANDARD', 2020, TRUE, NULL, 'FTR Sch 18 Sentencing Code Offender', 'FIXED_TERM_RECALL_28', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('FTRSCH18_ORA', 'STANDARD', 2020, TRUE, NULL, 'ORA FTR Sch 18 Sentencing Code Offender', 'FIXED_TERM_RECALL_28', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('HDR', 'STANDARD', 1991, TRUE, NULL, 'Recalled from HDC (not for curfew violation)', 'STANDARD_RECALL_255', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('HDR', 'STANDARD', 2003, TRUE, NULL, 'ORA FTR Sch 18 Sentencing Code Offender', 'STANDARD_RECALL_255', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('HDR', 'STANDARD', 2020, TRUE, NULL, 'Inability to Monitor', 'STANDARD_RECALL_255', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('HDR_ORA', 'STANDARD', 2003, TRUE, NULL, 'ORA HDC Recall (not curfew violation)', 'STANDARD_RECALL_255', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('HDR_ORA', 'STANDARD', 2020, TRUE, NULL, 'ORA HDC Recall (not curfew violation)', 'STANDARD_RECALL_255', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('HMPL', 'INDETERMINATE', 1991, TRUE, NULL, 'Detention During Her Majesty''s Pleasure', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('HMPL', 'INDETERMINATE', 2003, TRUE, NULL, 'Detention During Her Majesty''s Pleasure', '', '{}'::jsonb, '["IMP"]'::jsonb, '8f23e11b-dfa6-4d97-81ca-07ccd5f9983a'),
      ('HMPL', 'INDETERMINATE', 2020, TRUE, NULL, 'Detention During Her Majesty''s Pleasure', '', '{}'::jsonb, '["IMP"]'::jsonb, '69811a64-c18a-4e18-9f9a-87a3fa7ee0bd'),
      ('IC', '', 2003, FALSE, '2015-08-20', 'Intermittent Custody', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('IPP', 'INDETERMINATE', 2003, TRUE, NULL, 'Indeterminate Sentence for the Public Protection', '', '{}'::jsonb, '["IMP"]'::jsonb, 'ba057d8b-07ba-4779-a704-5b54e6135dcf'),
      ('LASPO_AR', 'EXTENDED', 2003, TRUE, NULL, 'EDS LASPO Automatic Release', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, 'cecc102c-424f-4a92-a789-915c07d38d93'),
      ('LASPO_DR', 'EXTENDED', 2003, TRUE, NULL, 'EDS LASPO Discretionary Release', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, '0cebfbdf-accd-42bf-8f90-acb00bc31f4d'),
      ('LEGACY', '', 1967, TRUE, NULL, 'Legacy (pre 1991 Act)', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, '100f4921-38dd-45be-846f-edfb1fba5343'),
      ('LIFE', 'INDETERMINATE', 1991, TRUE, NULL, 'Life Imprisonment or Detention S.53(1) CYPA 1933', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LIFE/IPP', 'INDETERMINATE', 2003, FALSE, '2012-09-25', 'Life or Indeterminate Sentence for Public Protection', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR', 'STANDARD', 1991, TRUE, NULL, 'Licence Recall', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR', 'STANDARD', 2003, TRUE, NULL, 'Licence Recall', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR', 'STANDARD', 2020, TRUE, NULL, 'Licence Recall', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_ALP', 'INDETERMINATE', 2003, TRUE, NULL, 'Recall from Automatic Life', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_ALP', 'INDETERMINATE', 2020, TRUE, NULL, 'Recall from Automatic Life', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_ALP_CDE18', 'INDETERMINATE', 2020, TRUE, NULL, 'Recall from Automatic Life Sec 273 Sentencing Code (18 - 20)', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_ALP_CDE21', 'INDETERMINATE', 2020, TRUE, NULL, 'Recall from Automatic Life Sec 283 Sentencing Code (21+)', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_ALP_LASPO', 'INDETERMINATE', 2003, TRUE, NULL, 'Recall from Automatic Life Sec 224A 03', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_DLP', 'INDETERMINATE', 2003, TRUE, NULL, 'Recall from Discretionary Life', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_DLP', 'INDETERMINATE', 2020, TRUE, NULL, 'Recall from Discretionary Life', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_DPP', 'INDETERMINATE', 1991, TRUE, NULL, 'Licence recall from DPP Sentence', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_DPP', 'INDETERMINATE', 2003, TRUE, NULL, 'Licence recall from DPP Sentence', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_EDS18', 'EXTENDED', 2020, TRUE, NULL, 'LR - EDS Sec 266 Sentencing Code (18 - 20)', 'STANDARD_RECALL', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('LR_EDS21', 'EXTENDED', 2020, TRUE, NULL, 'LR - EDS Sec 279 Sentencing Code (21+)', 'STANDARD_RECALL', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('LR_EDSU18', 'EXTENDED', 2020, TRUE, NULL, 'LR - EDS Sec 254 Sentencing Code (U18)', 'STANDARD_RECALL', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('LR_EPP', 'EXTENDED', 2003, TRUE, NULL, 'Licence recall from Extended Sentence for Public Protection', 'STANDARD_RECALL', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('LR_ES', 'EXTENDED', 1991, TRUE, NULL, 'Licence recall from Extended Sentence', 'STANDARD_RECALL', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('LR_IPP', 'INDETERMINATE', 2003, TRUE, NULL, 'Licence recall from IPP Sentence', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_LASPO_AR', 'EXTENDED', 2003, TRUE, NULL, 'LR - EDS LASPO Automatic Release', 'STANDARD_RECALL', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('LR_LASPO_DR', 'EXTENDED', 2003, TRUE, NULL, 'LR - EDS LASPO Discretionary Release', 'STANDARD_RECALL', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('LR_LIFE', 'INDETERMINATE', 1991, TRUE, NULL, 'Recall to Custody Indeterminate Sentence', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_LIFE', 'INDETERMINATE', 2003, TRUE, NULL, 'Recall to Custody Indeterminate Sentence', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_LIFE', 'INDETERMINATE', 2020, TRUE, NULL, 'Recall to Custody Indeterminate Sentence', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_MLP', 'INDETERMINATE', 2003, TRUE, NULL, 'Recall to Custody Mandatory Life', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_MLP', 'INDETERMINATE', 2020, TRUE, NULL, 'Recall to Custody Mandatory Life', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_ORA', 'STANDARD', 2003, TRUE, NULL, 'ORA Licence Recall', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_ORA', 'STANDARD', 2020, TRUE, NULL, 'ORA Licence Recall', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_SEC236A', 'SOPC', 2003, TRUE, NULL, 'LR - Section 236A SOPC CJA03', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('LR_SEC91_ORA', 'STANDARD', 2003, TRUE, NULL, 'Recall Serious Off - 18 CJA03 POCCA 2000', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_SOPC18', 'SOPC', 2020, TRUE, NULL, 'LR - SOPC Sec 265 Sentencing Code (18 - 20)', 'STANDARD_RECALL', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('LR_SOPC21', 'SOPC', 2020, TRUE, NULL, 'LR - SOPC Sec 278 Sentencing Code (21+)', 'STANDARD_RECALL', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('LR_YOI_ORA', 'STANDARD', 2003, TRUE, NULL, 'Recall from YOI', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LR_YOI_ORA', 'STANDARD', 2020, TRUE, NULL, 'Recall from YOI', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('LRSEC250_ORA', 'STANDARD', 2020, TRUE, NULL, 'Recall Serious Offence Sec 250 Sentencing Code (U18)', 'STANDARD_RECALL', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('MLP', 'INDETERMINATE', 1991, TRUE, NULL, 'Adult Mandatory Life', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('MLP', 'INDETERMINATE', 2003, TRUE, NULL, 'Adult Mandatory Life', '', '{}'::jsonb, '["IMP"]'::jsonb, 'e3b69eb7-6362-4d8b-affe-480abcfd35f7'),
      ('MLP', 'INDETERMINATE', 2020, TRUE, NULL, 'Adult Mandatory Life', '', '{}'::jsonb, '["IMP"]'::jsonb, '61ae9cdf-ae37-4a91-8216-deb9fef7330e'),
      ('NP', 'STANDARD', 1991, TRUE, NULL, 'Adult Imprison above 4 years (not Life)', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('PPEXT_SENT', 'EXTENDED', 2003, FALSE, NULL, 'Extended Sentence for the Public Protection', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, NULL),
      ('SDOPCU18', 'SOPC', 2020, TRUE, NULL, 'Special sentence of detention for terrorist offenders of particular concern Sec 252A', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, '5fc63cba-510f-4122-8fdd-649fbcd52efb'),
      ('SEC236A', 'SOPC', 2003, TRUE, NULL, 'Section 236A SOPC CJA03', '', '{"toreraEligibilityType": "SOPC", "sdsPlusEligibilityType": ""}'::jsonb, '["LIC","IMP"]'::jsonb, 'b4769983-fd23-4c95-9f47-5e5eb3aef491'),
      ('SEC250', 'STANDARD', 2020, TRUE, NULL, 'Serious Offence Sec 250 Sentencing Code (U18)', '', '{"toreraEligibilityType": "SDS", "sdsPlusEligibilityType": "SECTION250"}'::jsonb, '["IMP"]'::jsonb, '1104e683-5467-4340-b961-ff53672c4f39'),
      ('SEC250_ORA', 'STANDARD', 2020, TRUE, NULL, 'ORA Serious Offence Sec 250 Sentencing Code (U18)', '', '{"toreraEligibilityType": "SDS", "sdsPlusEligibilityType": "SECTION250"}'::jsonb, '["IMP"]'::jsonb, 'f1fb11de-aa51-4a74-8c5c-9c08ada6db37'),
      ('SEC272', 'INDETERMINATE', 2020, TRUE, NULL, 'Custody For Life Sec 272 Sentencing Code (18 - 20)', '', '{}'::jsonb, '["IMP"]'::jsonb, 'd8d38763-bee6-474e-8988-7dfa3d02f3ae'),
      ('SEC275', 'INDETERMINATE', 2020, TRUE, NULL, 'Custody For Life Sec 275 Sentencing Code (Murder) (U21)', '', '{}'::jsonb, '["IMP"]'::jsonb, '3725c368-fbfb-4e46-babf-12aabc2a7f91'),
      ('SEC91', 'DTO', 1991, TRUE, NULL, 'Serious Offence -18 POCCA 2000', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('SEC91_03', 'DTO', 2003, TRUE, NULL, 'Serious Offence -18 CJA03 POCCA 2000', '', '{}'::jsonb, '["IMP"]'::jsonb, '6ad464cc-08d9-438c-85c4-3352c25756c4'),
      ('SEC91_03_ORA', 'DTO', 2003, TRUE, NULL, 'ORA Serious Offence -18 CJA03 POCCA 2000', '', '{}'::jsonb, '["IMP"]'::jsonb, 'd34c90fe-cc49-4ac0-8ead-ca3fcc261b24'),
      ('SEC93', 'INDETERMINATE', 1991, TRUE, NULL, 'Custody For Life - Under 21', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('SEC93_03', 'INDETERMINATE', 2003, TRUE, NULL, 'Custody For Life - Under 21 CJA03', '', '{}'::jsonb, '["IMP"]'::jsonb, '80584016-4bab-4408-a8f2-b142830819e5'),
      ('SEC94', 'INDETERMINATE', 1991, TRUE, NULL, 'Custody Life (18-21 Years Old)', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('SEC94', 'INDETERMINATE', 2003, TRUE, NULL, 'Custody Life (18-21 Years Old)', '', '{}'::jsonb, '["IMP"]'::jsonb, 'b7b6a775-6146-484f-a8a6-92f2ed8fa6ae'),
      ('SOPC18', 'SOPC', 2020, TRUE, NULL, 'SOPC Sec 265 Sentencing Code (18 - 20)', '', '{"toreraEligibilityType": "SOPC", "sdsPlusEligibilityType": ""}'::jsonb, '["LIC","IMP"]'::jsonb, '4d434819-4aff-442a-8c78-64dfcfba61cc'),
      ('SOPC21', 'SOPC', 2020, TRUE, NULL, 'SOPC Sec 278 Sentencing Code (21+)', '', '{"toreraEligibilityType": "SOPC", "sdsPlusEligibilityType": ""}'::jsonb, '["LIC","IMP"]'::jsonb, 'deb22e75-2335-44de-8a9f-1047d58c9b04'),
      ('STS18', '  ', 2020, TRUE, NULL, 'Serious Terrorism Sentence Sec 268A (18 - 20)', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, 'b14be497-e9fe-4b67-913b-91b6c27a57c4'),
      ('STS21', 'EXTENDED', 2020, TRUE, NULL, 'Serious Terrorism Sentence Sec 282A (21+)', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, '0e822fe7-80dd-4c5d-b477-33b7109058c9'),
      ('VOO', 'STANDARD', 2020, TRUE, NULL, 'Violent Offender Order', '', '{}'::jsonb, '["COMM"]'::jsonb, NULL),
      ('YOI', 'STANDARD', 1991, TRUE, NULL, 'Young Offender Institution', '', '{"toreraEligibilityType": "SDS", "sdsPlusEligibilityType": "SDS"}'::jsonb, '["LIC","SUP","IMP"]'::jsonb, NULL),
      ('YOI', 'STANDARD', 2003, TRUE, NULL, 'Young Offender Institution', '', '{"toreraEligibilityType": "SDS", "sdsPlusEligibilityType": "SDS"}'::jsonb, '["LIC","IMP"]'::jsonb, '87ef5d34-5912-411a-8733-2b8cf0cfc8e5'),
      ('YOI', 'STANDARD', 2020, TRUE, NULL, 'Young Offender Institution', '', '{"toreraEligibilityType": "SDS", "sdsPlusEligibilityType": "SDS"}'::jsonb, '["LIC","IMP"]'::jsonb, '3bfbf00d-747e-4576-8d96-aa4f8f9832ef'),
      ('YOI_ORA', 'STANDARD', 2003, TRUE, NULL, 'ORA Young Offender Institution', '', '{"toreraEligibilityType": "SDS", "sdsPlusEligibilityType": "SDS"}'::jsonb, '["IMP"]'::jsonb, 'babc059d-53aa-423c-9129-be2ba86026ad'),
      ('YOI_ORA', 'STANDARD', 2020, TRUE, NULL, 'ORA Young Offender Institution', '', '{"toreraEligibilityType": "SDS", "sdsPlusEligibilityType": "SDS"}'::jsonb, '["IMP"]'::jsonb, 'd64283fd-7e48-4ed8-a98c-d68e938a5661'),
      ('YRO', 'NONCUSTODIAL', 2020, TRUE, NULL, 'Youth Rehabilitation Order', '', '{}'::jsonb, '["SUP"]'::jsonb, NULL),
      ('ZMD', 'INDETERMINATE', 1991, TRUE, NULL, 'Migrated Sentence Data', '', '{}'::jsonb, '["LIC","IMP"]'::jsonb, 'dd82452a-e973-4783-b72f-cb45a89511f6'),
      ('ZMD', 'INDETERMINATE', 2003, TRUE, NULL, 'Migrated Sentence Data', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL),
      ('ZMD', 'INDETERMINATE', 2020, TRUE, NULL, 'Migrated Sentence Data', '', '{}'::jsonb, '["IMP"]'::jsonb, NULL)

