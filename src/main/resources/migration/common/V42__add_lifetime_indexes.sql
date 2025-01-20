create index idx_court_appearance_lifetime on court_appearance(lifetime_uuid);
create index idx_charge_lifetime on charge(lifetime_charge_uuid);
create index idx_sentence_lifetime on sentence(lifetime_sentence_uuid);