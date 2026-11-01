-- Cover photos for Listening Practice Test 4 (PT-LISTENING-04): one per part, CC0 or public domain photos from Wikimedia Commons stored in
-- the public "media" bucket as practice/covers/<asset id>.webp (see scripts/practice-covers). This migration does not upload them.

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('2d000000-0000-4000-8000-010000003001', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000003001.webp', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000003002', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000003002.webp', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000003003', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000003003.webp', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000003004', 'IMAGE', NULL, 'practice/covers/2d000000-0000-4000-8000-010000003004.webp', NULL, 'VALID')
ON CONFLICT (id) DO UPDATE SET media_reference = EXCLUDED.media_reference, validation_status = EXCLUDED.validation_status;

UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000003001' WHERE id = '2d000000-0000-4000-8000-070000003001';
UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000003002' WHERE id = '2d000000-0000-4000-8000-070000003002';
UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000003003' WHERE id = '2d000000-0000-4000-8000-070000003003';
UPDATE content_sections SET cover_asset_id = '2d000000-0000-4000-8000-010000003004' WHERE id = '2d000000-0000-4000-8000-070000003004';
