ALTER TABLE charge RENAME COLUMN created_by_username TO created_by;
ALTER TABLE charge ADD COLUMN created_prison VARCHAR;
ALTER TABLE court_appearance RENAME COLUMN created_by_username TO created_by;
ALTER TABLE court_case RENAME COLUMN created_by_username TO created_by;
ALTER TABLE court_case ADD COLUMN created_prison VARCHAR;
ALTER TABLE sentence RENAME COLUMN created_by_username TO created_by;