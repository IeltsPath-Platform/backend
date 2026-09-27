-- A short, original reading passage for the demo Reading section seeded in V4, so the tutor's reading sessions can be
-- exercised locally. Paragraphs are separated by blank lines; the reading API labels them A, B, C...

INSERT INTO content_assets (id, asset_type, text_content, validation_status)
VALUES (
    '10000000-0000-4000-8000-000000000010',
    'PASSAGE',
    'Urban rooftops were once seen as wasted space. In many cities they held little more than water tanks and air-conditioning units, and few residents ever visited them.

Over the past two decades, however, planners have begun to treat roofs as a resource. Green roofs, covered with soil and low-growing plants, absorb rainwater that would otherwise flood drains during heavy storms.

Supporters also point to temperature. A planted roof stays noticeably cooler than bare concrete in summer, which reduces the energy needed to cool the rooms below and slightly lowers the temperature of the surrounding streets.

Critics accept these benefits but question the cost. Older buildings often need structural reinforcement before they can carry the extra weight of wet soil, and that work can take years to pay for itself.

Most researchers therefore conclude that green roofs are worthwhile mainly for new buildings, where the extra load can be planned from the start, rather than as a quick fix for existing ones.',
    'VALID'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order)
SELECT
    '10000000-0000-4000-8000-000000000011',
    '10000000-0000-4000-8000-000000000010',
    s.id,
    0
FROM content_sections s
JOIN content_package_versions pv ON pv.id = s.package_version_id
JOIN content_packages p ON p.id = pv.package_id
WHERE p.code = 'DEMO_MAIN_FLOW_READING'
  AND pv.version_number = 1
  AND s.sort_order = 1
ON CONFLICT DO NOTHING;
