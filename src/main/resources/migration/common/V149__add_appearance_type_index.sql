CREATE INDEX appearance_type_nomis_codes_idx ON appearance_type
    USING gin ((nomis_to_dps_mapping_codes->'codes') jsonb_path_ops);