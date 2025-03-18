UPDATE court_appearance set appearance_outcome_id = 29
where appearance_outcome_id in (36,37,38,39,42,43,44,45,51);

delete from appearance_outcome where id in (36,37,38,39,42,43,44,45,51);