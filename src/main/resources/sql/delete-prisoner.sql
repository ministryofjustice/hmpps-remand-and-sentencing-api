-- Native sql script to delete all data for a prisoner - Only to be used for migration
-- Delete recall sentences
DELETE FROM recall_sentence rs
WHERE rs.recall_id IN (
    SELECT id FROM recall r WHERE r.prisoner_id = :prisonerId
);

-- Delete recalls
DELETE FROM recall r WHERE r.prisoner_id = :prisonerId;

-- Delete period length history
DELETE FROM period_length_history
WHERE original_period_length_id IN (
    SELECT DISTINCT pl.id
    FROM period_length pl
             JOIN sentence s ON pl.sentence_id = s.id
             JOIN charge c ON s.charge_id = c.id
             JOIN appearance_charge ac ON c.id = ac.charge_id
             JOIN court_appearance a ON ac.appearance_id = a.id
    WHERE a.court_case_id IN (
        SELECT id FROM court_case cc WHERE cc.prisoner_id = :prisonerId
    )
);

-- Delete period lengths
DELETE FROM period_length
WHERE sentence_id IN (
    SELECT s.id
    FROM sentence s
             JOIN charge c ON s.charge_id = c.id
             JOIN appearance_charge ac ON c.id = ac.charge_id
             JOIN court_appearance a ON ac.appearance_id = a.id
    WHERE a.court_case_id IN (
        SELECT id FROM court_case cc WHERE cc.prisoner_id = :prisonerId
    )
);

-- Delete sentence history
DELETE FROM sentence_history
WHERE original_sentence_id IN (
    SELECT s.id
    FROM sentence s
             JOIN charge c ON s.charge_id = c.id
             JOIN appearance_charge ac ON c.id = ac.charge_id
             JOIN court_appearance a ON ac.appearance_id = a.id
    WHERE a.court_case_id IN (
        SELECT id FROM court_case cc WHERE cc.prisoner_id = :prisonerId
    )
);

-- Delete sentences
DELETE FROM sentence s
    USING (
      SELECT DISTINCT c.id
        FROM charge c
          JOIN appearance_charge ac ON c.id = ac.charge_id
          JOIN court_appearance a ON ac.appearance_id = a.id
          JOIN court_case cc ON a.court_case_id = cc.id
        WHERE cc.prisoner_id = :prisonerId
    ) del
WHERE s.charge_id = del.id;

-- Delete charge history
DELETE FROM charge_history ch
WHERE original_charge_id IN (
    SELECT c.id
    FROM charge c
             JOIN appearance_charge ac ON ac.charge_id = c.id
             JOIN court_appearance a ON ac.appearance_id = a.id
    WHERE a.court_case_id IN (
        SELECT id FROM court_case cc WHERE cc.prisoner_id = :prisonerId
    )
);

-- Delete appearance charges
DELETE FROM appearance_charge ac
WHERE ac.appearance_id IN (
    SELECT a.id
    FROM court_appearance a
    WHERE a.court_case_id IN (
        SELECT id FROM court_case cc WHERE cc.prisoner_id = :prisonerId
    )
);

-- Delete charges
DELETE FROM charge c
WHERE EXISTS (
    SELECT 1
    FROM appearance_charge ac
             JOIN court_appearance a ON ac.appearance_id = a.id
    WHERE ac.charge_id = c.id
      AND a.court_case_id IN (
        SELECT id FROM court_case cc WHERE cc.prisoner_id = :prisonerId
    )
);

-- Delete court appearance history
DELETE FROM court_appearance_history
WHERE original_appearance_id IN (
    SELECT a.id
    FROM court_appearance a
    WHERE a.court_case_id IN (
        SELECT id FROM court_case cc WHERE cc.prisoner_id = :prisonerId
    )
);

-- Nullify latest court appearance reference
UPDATE court_case
SET latest_court_appearance_id = NULL
WHERE latest_court_appearance_id IN (
    SELECT id FROM court_appearance
    WHERE court_case_id IN (
        SELECT id FROM court_case WHERE prisoner_id = :prisonerId
    )
);

-- Delete next court appearances
DELETE FROM next_court_appearance
WHERE future_skeleton_appearance_id IN (
    SELECT id FROM court_appearance
    WHERE court_case_id IN (
        SELECT id FROM court_case cc WHERE cc.prisoner_id = :prisonerId
    )
);

-- Delete court appearances
DELETE FROM court_appearance
WHERE court_case_id IN (
    SELECT id FROM court_case cc WHERE cc.prisoner_id = :prisonerId
);

-- Delete court cases
DELETE FROM court_case
WHERE prisoner_id = :prisonerId;
