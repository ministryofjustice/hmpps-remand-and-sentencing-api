TRUNCATE TABLE court_case,
court_appearance,
appearance_charge,
charge,
next_court_appearance,
sentence,
period_length,
recall_sentence,
recall,
appearance_charge_history,
charge_history,
court_appearance_history,
period_length_history,
sentence_history,
uploaded_document,
court_case_history,
immigration_detention,
recall_sentence_history,
recall_history
 RESTART IDENTITY CASCADE;

DELETE FROM charge_outcome where nomis_code in ('1', '56');
