update legacy_sentence_types set nomis_reference = 'FTR_56', sentencing_act = '2003' where nomis_reference = 'FTR_56ORA';

-- Sets sentence.sentence_type_id to the 'Unknown recall sentence ID' for all FTR_56 legacy sentences
UPDATE sentence s
SET sentence_type_id = st.id
    FROM sentence_type st
WHERE s.sentence_type_id IS NULL
  AND s.legacy_data ->> 'sentenceCalcType' = 'FTR_56'
  AND st.sentence_type_uuid = 'f9a1551e-86b1-425b-96f7-23465a0f05fc';
