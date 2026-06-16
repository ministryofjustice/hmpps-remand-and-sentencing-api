-- Create lookup table
CREATE TABLE aggravating_factor (
                                    id SERIAL PRIMARY KEY,
                                    code VARCHAR(100) NOT NULL UNIQUE,
                                    title VARCHAR(255) NOT NULL,
                                    description TEXT NULL,
                                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                                    display_order int NOT NULL DEFAULT 0;
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
    ('OAFPC', 'Offence aggravated by foreign power condition being met', 'Offence aggravated by foreign power condition being met', 10),
    ('OATC', 'Offence Aggravated by Terrorist Connection', 'Offence Aggravated by Terrorist Connection', 20),
    ('RA', 'Racially Aggravated Offence', 'Racially Aggravated Offence', 30),
    ('RARE', 'Racially and Religiously Aggravated Offence', 'Racially and Religiously Aggravated Offence', 40),
    ('RE', 'Religiously Aggravated Offence', 'Religiously Aggravated Offence', 50),
    ('SEXO', 'Aggravated due to sexual orientation', 'Aggravated due to sexual orientation', 60),
    ('SEXOV', 'Aggravated due to sexual orientation of victim', 'Aggravated due to sexual orientation of victim', 70),
    ('TGG', 'Aggravated due to transgender identity', 'Aggravated due to transgender identity', 80),
    ('TGV', 'Aggravated due to transgender identity of victim', 'Aggravated due to transgender identity of victim', 90),
    ('EWA', 'Emergency Worker Offence Aggravation', 'Emergency Worker Offence Aggravation', 100),
    ('DISG', 'Disability', 'Disability in general', 110),
    ('DISV', 'Disability of victim', 'Disability of victim', 120);