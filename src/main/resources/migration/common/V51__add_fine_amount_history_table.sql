CREATE TABLE fine_amount_history (
	id SERIAL PRIMARY KEY,
	sentence_id int4 NULL,
	fine_amount numeric(11, 2) NULL,
	status_id smallint NOT NULL,
	created_at timestamp with time zone NOT NULL,
	created_by varchar NOT NULL,
	created_prison varchar NULL,
	updated_at timestamp with time zone NULL,
	updated_by varchar NULL,
	updated_prison varchar NULL,
	original_fine_amount_id int references fine_amount(id)
);