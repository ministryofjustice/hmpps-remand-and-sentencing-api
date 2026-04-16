create table charge_outcome_sentence_type(
    charge_outcome_id int references charge_outcome(id) NOT NULL,
    sentence_type_id int references sentence_type(id) NOT NULL,
    primary key (charge_outcome_id, sentence_type_id)
);