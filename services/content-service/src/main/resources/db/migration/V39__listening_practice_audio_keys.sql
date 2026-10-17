-- The Listening practice recordings are object keys under CONTENT_MEDIA_BASE_URL (the media bucket), never audio data
-- inside the database: a data URI of about 150 kB would be copied into every attempt snapshot and travel through
-- Content, Assessment and the Gateway on each request. This puts the key back on any recording that still holds a
-- data URI; where the key is already set it changes nothing. Upload the 12 files to the bucket under these keys.

UPDATE content_assets SET media_reference = 'listening/practice/pt-listening-01-s1.mp3'
WHERE id = '2d000000-0000-4000-8000-020000001005' AND asset_type = 'AUDIO' AND media_reference LIKE 'data:audio/%';
UPDATE content_assets SET media_reference = 'listening/practice/pt-listening-01-s2.mp3'
WHERE id = '2d000000-0000-4000-8000-020000001006' AND asset_type = 'AUDIO' AND media_reference LIKE 'data:audio/%';
UPDATE content_assets SET media_reference = 'listening/practice/pt-listening-01-s3.mp3'
WHERE id = '2d000000-0000-4000-8000-020000001007' AND asset_type = 'AUDIO' AND media_reference LIKE 'data:audio/%';
UPDATE content_assets SET media_reference = 'listening/practice/pt-listening-01-s4.mp3'
WHERE id = '2d000000-0000-4000-8000-020000001008' AND asset_type = 'AUDIO' AND media_reference LIKE 'data:audio/%';
UPDATE content_assets SET media_reference = 'listening/practice/pt-listening-02-s1.mp3'
WHERE id = '2d000000-0000-4000-8000-020000001009' AND asset_type = 'AUDIO' AND media_reference LIKE 'data:audio/%';
UPDATE content_assets SET media_reference = 'listening/practice/pt-listening-02-s2.mp3'
WHERE id = '2d000000-0000-4000-8000-020000001010' AND asset_type = 'AUDIO' AND media_reference LIKE 'data:audio/%';
UPDATE content_assets SET media_reference = 'listening/practice/pt-listening-02-s3.mp3'
WHERE id = '2d000000-0000-4000-8000-020000001011' AND asset_type = 'AUDIO' AND media_reference LIKE 'data:audio/%';
UPDATE content_assets SET media_reference = 'listening/practice/pt-listening-02-s4.mp3'
WHERE id = '2d000000-0000-4000-8000-020000001012' AND asset_type = 'AUDIO' AND media_reference LIKE 'data:audio/%';
UPDATE content_assets SET media_reference = 'listening/practice/pt-listening-03-s1.mp3'
WHERE id = '2d000000-0000-4000-8000-020000001013' AND asset_type = 'AUDIO' AND media_reference LIKE 'data:audio/%';
UPDATE content_assets SET media_reference = 'listening/practice/pt-listening-03-s2.mp3'
WHERE id = '2d000000-0000-4000-8000-020000001014' AND asset_type = 'AUDIO' AND media_reference LIKE 'data:audio/%';
UPDATE content_assets SET media_reference = 'listening/practice/pt-listening-03-s3.mp3'
WHERE id = '2d000000-0000-4000-8000-020000001015' AND asset_type = 'AUDIO' AND media_reference LIKE 'data:audio/%';
UPDATE content_assets SET media_reference = 'listening/practice/pt-listening-03-s4.mp3'
WHERE id = '2d000000-0000-4000-8000-020000001016' AND asset_type = 'AUDIO' AND media_reference LIKE 'data:audio/%';
