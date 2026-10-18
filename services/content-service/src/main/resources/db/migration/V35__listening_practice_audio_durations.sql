-- Real recordings for the Listening practice tests (text-to-speech renderings of the transcripts, served from the
-- front end at /audio/). The stored length now matches each file instead of the placeholder 90 seconds.
UPDATE content_assets SET duration_seconds = 50 WHERE asset_type = 'AUDIO' AND media_reference = 'listening/practice/pt-listening-01-s1.mp3';
UPDATE content_assets SET duration_seconds = 48 WHERE asset_type = 'AUDIO' AND media_reference = 'listening/practice/pt-listening-01-s2.mp3';
UPDATE content_assets SET duration_seconds = 59 WHERE asset_type = 'AUDIO' AND media_reference = 'listening/practice/pt-listening-01-s3.mp3';
UPDATE content_assets SET duration_seconds = 39 WHERE asset_type = 'AUDIO' AND media_reference = 'listening/practice/pt-listening-01-s4.mp3';
UPDATE content_assets SET duration_seconds = 41 WHERE asset_type = 'AUDIO' AND media_reference = 'listening/practice/pt-listening-02-s1.mp3';
UPDATE content_assets SET duration_seconds = 42 WHERE asset_type = 'AUDIO' AND media_reference = 'listening/practice/pt-listening-02-s2.mp3';
UPDATE content_assets SET duration_seconds = 48 WHERE asset_type = 'AUDIO' AND media_reference = 'listening/practice/pt-listening-02-s3.mp3';
UPDATE content_assets SET duration_seconds = 36 WHERE asset_type = 'AUDIO' AND media_reference = 'listening/practice/pt-listening-02-s4.mp3';
UPDATE content_assets SET duration_seconds = 39 WHERE asset_type = 'AUDIO' AND media_reference = 'listening/practice/pt-listening-03-s1.mp3';
UPDATE content_assets SET duration_seconds = 42 WHERE asset_type = 'AUDIO' AND media_reference = 'listening/practice/pt-listening-03-s2.mp3';
UPDATE content_assets SET duration_seconds = 46 WHERE asset_type = 'AUDIO' AND media_reference = 'listening/practice/pt-listening-03-s3.mp3';
UPDATE content_assets SET duration_seconds = 35 WHERE asset_type = 'AUDIO' AND media_reference = 'listening/practice/pt-listening-03-s4.mp3';
