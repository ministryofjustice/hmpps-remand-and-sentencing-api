INSERT INTO court_case
(id, prisoner_id, case_unique_identifier, latest_court_appearance_id, created_at, created_by, status_id, legacy_data, created_prison)
VALUES(1354340, 'A4544EC', 'b12210d6-bdfd-46b0-a455-07eb3a730f53', NULL, '2025-05-19 16:40:52.000', 'YMUSTAFA_GEN', 0, '{"caseReferences": [{"updatedDate": "2025-05-19T16:40:52.154663", "offenderCaseReference": "RASS-YM-6"}]}'::jsonb, 'KMI');

INSERT INTO court_appearance
(id, appearance_outcome_id, court_case_id, court_code, court_case_reference, appearance_date, status_id, next_court_appearance_id, previous_appearance_id, warrant_id, created_at, created_by, created_prison, warrant_type, overall_conviction_date, appearance_uuid, legacy_data, updated_at, updated_by, updated_prison)
VALUES(3246145, 26, 1354340, 'KNGHMC', 'RASS-YM-6', '2025-01-10', 0, NULL, NULL, NULL, '2025-05-19 16:40:52.154', 'YMUSTAFA_GEN', 'KMI', 'SENTENCING', '2025-01-11', '4a5d8632-dd77-4fc8-8341-ec5fde0475fc'::uuid, NULL, NULL, NULL, NULL);

UPDATE court_case SET latest_court_appearance_id = 3246145 WHERE id = 1354340;

INSERT INTO charge
(id, charge_uuid, offence_code, offence_start_date, offence_end_date, status_id, charge_outcome_id, superseding_charge_id, terror_related, created_at, legacy_data, created_by, merged_from_case_id, created_prison, updated_at, updated_by, updated_prison, merged_from_date)
VALUES(7371944, 'f6f1c966-1c92-4b13-849b-5bdf016195ec'::uuid, 'COML020', '2025-01-09', NULL, 0, 19, NULL, NULL, '2025-05-19 16:40:52.163', NULL, 'YMUSTAFA_GEN', NULL, 'KMI', '2025-05-19 16:40:52.163', NULL, NULL, NULL);

INSERT INTO charge
(id, charge_uuid, offence_code, offence_start_date, offence_end_date, status_id, charge_outcome_id, superseding_charge_id, terror_related, created_at, legacy_data, created_by, merged_from_case_id, created_prison, updated_at, updated_by, updated_prison, merged_from_date)
VALUES(7371945, '99392e91-ec34-4e44-82ed-ff00faeabad6'::uuid, 'SX03163A', '2025-01-08', NULL, 0, 19, NULL, NULL, '2025-05-19 16:43:15.611', '{"postedDate": "2025-05-19", "nomisOutcomeCode": null, "outcomeDescription": null, "outcomeConvictionFlag": true, "outcomeDispositionCode": null}'::jsonb, 'hmpps-prisoner-from-nomis-migration-court-sentencing-1', NULL, NULL, '2025-05-19 16:43:15.611', NULL, NULL, NULL);

INSERT INTO appearance_charge
(appearance_id, charge_id, created_at, created_by, created_prison)
VALUES(3246145, 7371944, '2025-05-19 16:40:52.185', 'YMUSTAFA_GEN', 'KMI');

INSERT INTO appearance_charge
(appearance_id, charge_id, created_at, created_by, created_prison)
VALUES(3246145, 7371945, '2025-05-19 16:43:15.619', 'hmpps-prisoner-from-nomis-migration-court-sentencing-1', NULL);

INSERT INTO sentence
(id, sentence_uuid, charge_number, status_id, created_at, created_by, created_prison, superseding_sentence_id, charge_id, sentence_serve_type, consecutive_to_id, conviction_date, sentence_type_id, legacy_data, updated_at, updated_by, updated_prison, fine_amount)
VALUES(2363717, '8ba46b2d-ee44-48fe-a57f-ac51004e516e'::uuid, '1', 7, '2025-05-19 16:40:52.000', 'YMUSTAFA_GEN', 'KMI', 2363717, 7371944, 'UNKNOWN', NULL, '2025-01-11', 16, '{"active": true, "postedDate": "2025-05-19T16:40:54.997606", "sentenceCalcType": null, "sentenceCategory": null, "sentenceTypeDesc": null}'::jsonb, '2025-05-19 16:46:16.415', 'hmpps-prisoner-from-nomis-migration-court-sentencing-1', NULL, NULL);

INSERT INTO period_length
(id, years, months, weeks, days, period_order, period_length_type, sentence_id, appearance_id, legacy_data, period_length_uuid, status_id, created_at, created_by, created_prison, updated_at, updated_by, updated_prison)
VALUES(2482106, NULL, NULL, NULL, 99, 'years,months,weeks,days', 'SENTENCE_LENGTH', 2363717, NULL, NULL, 'ead43284-11dc-4105-afc6-b1f8f2c6fa1d'::uuid, 7, '2025-05-19 16:40:52.179', 'YMUSTAFA_GEN', 'KMI', '2025-05-19 16:46:16.264', 'hmpps-prisoner-from-nomis-migration-court-sentencing-1', NULL);
