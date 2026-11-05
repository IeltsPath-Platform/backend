UPDATE content_assets
SET access_tier = 'PREMIUM'
WHERE id = '23000000-0000-4000-8000-050000000001'
  AND asset_type = 'AUDIO'
  AND media_reference = 'listening/demo/ls1.mp3';

-- Placement has its own asset row; scope the update through its section and package link.
UPDATE content_assets placement_asset
SET media_reference = 'listening/placement/placement-ls1.mp3',
    access_tier = 'PUBLIC'
WHERE placement_asset.id = '2b000000-0000-4000-8000-050000000001'
  AND placement_asset.media_reference = 'listening/demo/ls1.mp3'
  AND EXISTS (
      SELECT 1 FROM content_asset_links al
      JOIN content_sections s ON s.id = al.section_id
      JOIN content_package_versions pv ON pv.id = s.package_version_id
      JOIN content_packages p ON p.id = pv.package_id
      WHERE al.id = '2b000000-0000-4000-8000-0c0000000001'
        AND al.asset_id = placement_asset.id
        AND s.id = '2b000000-0000-4000-8000-0a0000000001'
        AND pv.id = '2b000000-0000-4000-8000-090000000001'
        AND p.id = '2b000000-0000-4000-8000-080000000001'
        AND p.code = 'PLACEMENT-4SKILLS'
  );
