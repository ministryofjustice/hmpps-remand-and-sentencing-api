ALTER TABLE sentence_type
ADD COLUMN min_offence_date_inclusive date,
ADD COLUMN max_offence_date_exclusive date;

UPDATE sentence_type
set min_offence_date_inclusive = '2015-02-01'
where id in (15, 16, 18, 20, 23, 24, 47, 48, 53, 54);

UPDATE sentence_type
set max_offence_date_exclusive = '2015-02-01'
where id in (13, 14, 17, 21, 22, 51, 52);

UPDATE sentence_type
set description = 'Serious Offence Sec 250 Sentencing Code (U18)'
where id = 17;

UPDATE sentence_type
set description = 'ORA Serious Offence Sec 250 Sentencing Code (U18)'
where id = 18;