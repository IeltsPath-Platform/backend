ALTER TABLE content_packages ADD COLUMN placement_pool VARCHAR(20);

UPDATE content_packages SET placement_pool = 'BAND_5_9' WHERE package_type = 'PLACEMENT_TEST';

ALTER TABLE content_packages ADD CONSTRAINT content_packages_placement_pool_check CHECK (
    placement_pool IS NULL OR (
        package_type = 'PLACEMENT_TEST' AND placement_pool IN ('BAND_0_3', 'BAND_3_5', 'BAND_5_9')
    )
);
