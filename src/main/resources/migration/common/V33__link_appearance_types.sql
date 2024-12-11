ALTER TABLE next_court_appearance ADD COLUMN appearance_type_id INT references appearance_type(id);
UPDATE next_court_appearance SET appearance_type_id=(select id from appearance_type where appearance_type_uuid='63e8fce0-033c-46ad-9edf-391b802d547a') where appearance_type='Court appearance';
UPDATE next_court_appearance SET appearance_type_id=(select id from appearance_type where appearance_type_uuid='1da09b6e-55cb-4838-a157-ee6944f2094c') where appearance_type='Video link';
ALTER TABLE next_court_appearance DROP COLUMN appearance_type;