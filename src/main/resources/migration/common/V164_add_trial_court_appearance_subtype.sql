INSERT INTO court_appearance_subtype(appearance_subtype_uuid, description, display_order, status, nomis_code, appearance_type_id) VALUES
('8b72d4f1-6e3a-4c9b-a157-3f8d2e71c4ab','Trial',50,'ACTIVE','TRIAL',(select id from appearance_type at where at.appearance_type_uuid='63e8fce0-033c-46ad-9edf-391b802d547a'));
