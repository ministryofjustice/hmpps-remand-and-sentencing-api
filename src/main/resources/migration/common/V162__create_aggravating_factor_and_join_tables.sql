-- Create lookup table
CREATE TABLE aggravating_factor (
                                    id SERIAL PRIMARY KEY,
                                    code VARCHAR(100) NOT NULL UNIQUE,
                                    title VARCHAR(255) NOT NULL,
                                    description TEXT NULL
);

-- Create join table
CREATE TABLE charge_aggravating_factor (
                                           charge_id INT NOT NULL,
                                           aggravating_factor_id INT NOT NULL,

                                           CONSTRAINT pk_charge_aggravating_factor
                                               PRIMARY KEY (charge_id, aggravating_factor_id),

                                           CONSTRAINT fk_charge_aggravating_factor_charge
                                               FOREIGN KEY (charge_id)
                                                   REFERENCES charge(id),

                                           CONSTRAINT fk_charge_aggravating_factor_aggravating_factor
                                               FOREIGN KEY (aggravating_factor_id)
                                                   REFERENCES aggravating_factor(id)
);

-- Seed default aggravating factors
INSERT INTO aggravating_factor (code, title, description)
VALUES
    ('OAFPC', 'Offence aggravated by foreign power condition being met', 'Offence aggravated by foreign power condition being met'),
    ('OATC', 'Offence Aggravated by Terrorist Connection', 'Offence Aggravated by Terrorist Connection'),
    ('RA', 'Racially Aggravated Offence', 'Racially Aggravated Offence'),
    ('RARE', 'Racially and Religiously Aggravated Offence', 'Racially and Religiously Aggravated Offence'),
    ('RE', 'Religiously Aggravated Offence', 'Religiously Aggravated Offence'),
    ('SEXO', 'Aggravated due to sexual orientation', 'Aggravated due to sexual orientation'),
    ('SEXOV', 'Aggravated due to sexual orientation of victim', 'Aggravated due to sexual orientation of victim'),
    ('TGG', 'Aggravated due to transgender', 'Aggravated due to transgender'),
    ('TGV', 'Aggravated due to transgender of victim', 'Aggravated due to transgender of victim'),
    ('EWA', 'Emergency Worker Offence Aggravation', 'Emergency Worker Offence Aggravation'),
    ('DISG', 'Disability in general', 'Disability in general'),
    ('DISV', 'Disability of victim', 'Disability of victim');