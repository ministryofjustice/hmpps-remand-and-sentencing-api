UPDATE sentence_type
SET description = 'Violent offender order'
WHERE description = 'Violent Offender Order';

UPDATE sentence_type
SET description = 'Youth rehabilitation order'
WHERE description = 'Youth Rehabilitation Order';

UPDATE sentence_type
SET description = 'EDS (Extended determinate sentence)'
WHERE description = 'EDS (Extended Determinate Sentence)';

UPDATE sentence_type
SET description = 'Extended sentence for public protection'
WHERE description = 'Extended Sentence for Public Protection';

UPDATE sentence_type
SET description = 'EDS LASPO Automatic release'
WHERE description = 'EDS LASPO Automatic Release';

UPDATE sentence_type
SET description = 'EDS LASPO Discretionary release'
WHERE description = 'EDS LASPO Discretionary Release';

UPDATE sentence_type
SET description = 'Serious terrorism sentence'
WHERE description = 'Serious Terrorism Sentence';

UPDATE sentence_type
SET description = 'SDS (Standard determinate sentence)'
WHERE description = 'SDS (Standard Determinate Sentence)';

UPDATE sentence_type
SET description = 'Serious offence -18 CJA03 POCCA 2000'
WHERE description = 'Serious Offence -18 CJA03 POCCA 2000';

UPDATE sentence_type
SET description = 'YOI ORA (Young offender institution offender rehabilitation act)'
WHERE description = 'YOI ORA (Young offender Institution offender rehabilitation act)';

UPDATE sentence_type
SET description = 'SOPC (Offenders of a particular concern)'
WHERE description = 'SOPC (offenders of a particular concern)';

UPDATE sentence_type
SET description = 'Automatic life'
WHERE description = 'Automatic Life';

UPDATE sentence_type
SET description = 'Automatic life sec 273 sentencing code (18 - 20)'
WHERE description = 'Automatic Life Sec 273 Sentencing Code (18 - 20)';

UPDATE sentence_type
SET description = 'Automatic life sec 224A 03'
WHERE description = 'Automatic Life Sec 224A 03';

UPDATE sentence_type
SET description = 'Detention for life'
WHERE description = 'Detention For Life';

UPDATE sentence_type
SET description = 'Adult discretionary life'
WHERE description = 'Adult Discretionary Life';

UPDATE sentence_type
SET description = 'Detention for public protection'
WHERE description = 'Detention For Public Protection';

UPDATE sentence_type
SET description = 'Detention during his majesty''s pleasure'
WHERE description = 'Detention During His Majesty''s Pleasure';

UPDATE sentence_type
SET description = 'Indeterminate sentence for the public protection'
WHERE description = 'Indeterminate Sentence for the Public Protection';

UPDATE sentence_type
SET description = 'Adult mandatory life'
WHERE description = 'Adult Mandatory Life';

UPDATE sentence_type
SET description = 'Custody for life sec 272 sentencing code (18 - 20)'
WHERE description = 'Custody For Life Sec 272 Sentencing Code (18 - 20)';

UPDATE sentence_type
SET description = 'Custody for life sec 275 sentencing code (murder) (U21)'
WHERE description = 'Custody For Life Sec 275 Sentencing Code (Murder) (U21)';

UPDATE sentence_type
SET description = 'Custody for life - under 21 CJA03'
WHERE description = 'Custody For Life - Under 21 CJA03';

UPDATE sentence_type
SET description = 'Custody life (18-21 years old)'
WHERE description = 'Custody Life (18-21 Years Old)';

UPDATE sentence_type
SET description = 'ORA Breach top up supervision'
WHERE description = 'ORA Breach Top Up Supervision';

UPDATE sentence_type
SET description = 'Civil imprisonment'
WHERE description = 'Civil Imprisonment';

UPDATE sentence_type
SET description = 'Detention and training order'
WHERE description = 'Detention and Training Order';

UPDATE sentence_type
SET description = 'ORA Detention and training order'
WHERE description = 'ORA Detention and Training Order';

UPDATE sentence_type
SET description = 'Imprisonment in default of fine'
WHERE description = 'Imprisonment in Default of Fine';

UPDATE sentence_type
SET description = 'Legacy (1967 act)'
WHERE description = 'Legacy (1967 Act)';

UPDATE sentence_type
SET description = 'Legacy (1991 act)'
WHERE description = 'Legacy (1991 Act)';

ALTER TABLE sentence_type ADD COLUMN hint_text VARCHAR NULL;

UPDATE sentence_type
SET hint_text = 'A mandatory licence period of 12 months will be automatically added to the sentence'
WHERE description = 'SOPC (Offenders of a particular concern)';
