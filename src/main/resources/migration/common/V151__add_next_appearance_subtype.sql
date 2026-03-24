alter table next_court_appearance add column court_appearance_subtype_id int;

ALTER TABLE next_court_appearance ADD CONSTRAINT next_appearance_subtype_fk FOREIGN KEY (court_appearance_subtype_id) references court_appearance_subtype(id);
