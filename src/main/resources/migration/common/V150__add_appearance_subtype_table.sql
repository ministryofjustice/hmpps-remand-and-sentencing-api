create table court_appearance_subtype(
    id SERIAL PRIMARY KEY,
    appearance_subtype_uuid UUID NOT NULL,
    description VARCHAR NOT NULL,
    display_order int NOT NULL,
    status VARCHAR NOT NULL,
    nomis_code VARCHAR NOT NULL,
    appearance_type_id int NOT NULL
);

ALTER TABLE court_appearance_subtype ADD CONSTRAINT appearance_subtype_appearance_type_fk FOREIGN KEY (appearance_type_id) references appearance_type(id);

INSERT INTO court_appearance_subtype(appearance_subtype_uuid, description, display_order, status, nomis_code, appearance_type_id) VALUES
('3f1c9e42-7c8a-4c1e-9a5d-2f6b8d1a9e73','Discharged to court',10,'ACTIVE','DC',(select id from appearance_type at where at.appearance_type_uuid='63e8fce0-033c-46ad-9edf-391b802d547a')),
('8b7d2a91-5e3c-4f6f-8c2d-9a1b7e4c5d20','Production of unsentenced prisoner',20,'ACTIVE','PR',(select id from appearance_type at where at.appearance_type_uuid='63e8fce0-033c-46ad-9edf-391b802d547a')),
('c2a9f5d4-1e6b-4a9c-b8f7-3d2e1c6a9b84','Production Sentenced',30,'ACTIVE','PS',(select id from appearance_type at where at.appearance_type_uuid='63e8fce0-033c-46ad-9edf-391b802d547a')),
('5d9a3b7c-8e1f-4c2a-9b6d-7f3e2a1c8d55','Discharged to court on appeal',40,'ACTIVE','AP',(select id from appearance_type at where at.appearance_type_uuid='63e8fce0-033c-46ad-9edf-391b802d547a'));