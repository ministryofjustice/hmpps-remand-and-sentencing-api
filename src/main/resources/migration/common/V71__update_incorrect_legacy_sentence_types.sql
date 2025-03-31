
UPDATE legacy_sentence_types
SET nomis_expiry_date = '2012-12-03'
WHERE nomis_reference = '59';

UPDATE legacy_sentence_types
SET nomis_expiry_date = '2014-05-10'
WHERE nomis_reference = '60';

UPDATE legacy_sentence_types
SET nomis_expiry_date = '2012-12-03'
WHERE nomis_reference = 'PPEXT_SENT';

UPDATE legacy_sentence_types
SET recall_type = 'STANDARD_RECALL'
WHERE nomis_reference = 'LR_SEC236A';