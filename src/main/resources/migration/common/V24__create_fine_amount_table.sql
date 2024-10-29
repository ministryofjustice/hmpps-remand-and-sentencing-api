CREATE TABLE fine_amount(
    id SERIAL PRIMARY KEY,
    sentence_id int references sentence(id),
    fine_amount NUMERIC
);