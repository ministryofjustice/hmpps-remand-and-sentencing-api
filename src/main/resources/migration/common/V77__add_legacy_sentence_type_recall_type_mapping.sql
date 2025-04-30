ALTER TABLE legacy_sentence_types RENAME COLUMN recall_type TO recall_type_description;
ALTER TABLE legacy_sentence_types ADD COLUMN recall_type_id int references recall_type(id);

-- LR
UPDATE legacy_sentence_types SET recall_type_id = 1 WHERE nomis_reference in (
'LR',
'LR_ORA',
'LR_DPP',
'LR_DLP',
'LR_ALP',
'LR_ALP_LASPO',
'LR_ALP_CDE18',
'LR_ALP_CDE21',
'LR_LIFE',
'LR_EPP',
'LR_IPP',
'LR_MLP',
'LR_SEC236A',
'LR_SEC91_ORA',
'LRSEC250_ORA',
'LR_ES',
'LR_EDS18',
'LR_EDS21',
'LR_EDSU18',
'LR_LASPO_AR',
'LR_LASPO_DR',
'LR_SOPC18',
'LR_SOPC21',
'LR_YOI_ORA'
);

-- FTR 14
UPDATE legacy_sentence_types SET recall_type_id = 2 WHERE nomis_reference in (
'14FTR_ORA'
);

--FTR 28
UPDATE legacy_sentence_types SET recall_type_id = 3 WHERE nomis_reference in (
'FTR',
'FTR_ORA',
'FTR_SCH15',
'FTRSCH15_ORA',
'FTRSCH18',
'FTRSCH18_ORA'
);

-- No legacy type for LR_HDC

--FTR_HDC_14
UPDATE legacy_sentence_types SET recall_type_id = 5 WHERE nomis_reference in (
'14FTRHDC_ORA'
);

-- FTR_HDC_28
UPDATE legacy_sentence_types SET recall_type_id = 6 WHERE nomis_reference in (
'FTR_HDC',
'FTR_HDC_ORA'
);

-- CUR_HDC
UPDATE legacy_sentence_types SET recall_type_id = 7 WHERE nomis_reference in (
'CUR',
'CUR_ORA'
);
-- IN_HDC
UPDATE legacy_sentence_types SET recall_type_id = 8 WHERE nomis_reference in (
'HDR',
'HDR_ORA'
);