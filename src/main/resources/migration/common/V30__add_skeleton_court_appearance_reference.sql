UPDATE court_appearance SET next_court_appearance_id = null where next_court_appearance_id is not null;
DROP TABLE next_court_appearance CASCADE;
CREATE TABLE next_court_appearance (
	id SERIAL PRIMARY KEY,
	appearance_date date NOT NULL,
	court_code varchar NULL,
	appearance_type varchar NULL,
	appearance_time time NULL,
	future_skeleton_appearance_id int references court_appearance(id) NOT NULL
);