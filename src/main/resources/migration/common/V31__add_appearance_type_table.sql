CREATE TABLE appearance_type(
    id SERIAL PRIMARY KEY,
    appearance_type_uuid UUID NOT NULL,
    description VARCHAR NOT NULL,
    display_order int NOT NULL
);

INSERT INTO appearance_type(appearance_type_uuid, description, display_order) VALUES
('63e8fce0-033c-46ad-9edf-391b802d547a', 'Court appearance', 10),
('1da09b6e-55cb-4838-a157-ee6944f2094c', 'Video link', 20);