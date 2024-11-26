CREATE TABLE draft_appearance(
    id SERIAL PRIMARY KEY,
    draft_uuid UUID NOT NULL,
    court_case_id int references court_case(id) NOT NULL,
    created_at timestamp with time zone not null,
    created_by_username VARCHAR NOT NULL,
    session_blob jsonb NOT NULL
)