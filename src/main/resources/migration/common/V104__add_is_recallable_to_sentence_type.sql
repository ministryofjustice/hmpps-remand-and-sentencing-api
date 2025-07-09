-- Add is_recallable column to sentence_type table
ALTER TABLE sentence_type ADD COLUMN is_recallable BOOLEAN DEFAULT TRUE;

-- Update non-recallable sentence types to false
UPDATE sentence_type SET is_recallable = FALSE WHERE sentence_type_uuid IN (
    '514eb0fc-239c-43e2-912f-139dc26d4473',  -- Civil Imprisonment (2003)
    '18826041-43f4-402b-b529-56c58fa8bc3c',  -- Civil Imprisonment (2020)
    'cab9e914-e0de-48d0-9e72-0e1fc9a19cf4',  -- Detention and Training Order (2020)
    'e8098305-f909-4cf5-9b06-f71a4e4d9df0',  -- Detention and Training Order (2003)
    'c71ceefe-932b-4a69-b87c-7c1294e37cf7',  -- Imprisonment in Default of Fine (A/FINE)
    'd74201de-2154-4096-891a-62237dcef23b',  -- ORA Breach Top Up Supervision (2003)
    'd721e4c9-6ba8-47b7-8744-c58ef2703eab',  -- ORA Breach Top Up Supervision (2020)
    'b6862370-e8f0-4680-8797-0aca9cacb302',  -- ORA Detention and Training Order (2003)
    '903ca33b-e264-4a16-883d-fee03a2a3396'   -- ORA Detention and Training Order (2020)
);

-- Add comment to explain the column
COMMENT ON COLUMN sentence_type.is_recallable IS 'Indicates whether sentences of this type are recallable';