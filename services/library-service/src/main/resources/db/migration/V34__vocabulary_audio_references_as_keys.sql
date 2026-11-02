-- Store pronunciation audio as an object key in the media bucket instead of a full URL.
--
-- V25 wrote https://<project>.supabase.co/storage/v1/object/public/media/vocabulary/pronunciation/{uk,us}/<slug>.mp3.
-- Library now builds the URL from the key (VocabularyAudioUrlResolver), so a new bucket or domain is a configuration
-- change and not an update of every row. Values that do not start with that media prefix are left as they are, and the
-- resolver still passes an https:// value through untouched.

UPDATE vocabulary_items
SET audio_uk_reference = regexp_replace(audio_uk_reference, '^https://[^/]+/storage/v1/object/public/media/', ''),
    audio_us_reference = regexp_replace(audio_us_reference, '^https://[^/]+/storage/v1/object/public/media/', ''),
    updated_at = CURRENT_TIMESTAMP
WHERE audio_uk_reference ~ '^https://[^/]+/storage/v1/object/public/media/'
   OR audio_us_reference ~ '^https://[^/]+/storage/v1/object/public/media/';
