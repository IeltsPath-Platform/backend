-- American IPA and pronunciation audio for the eight DEMO_READING L1 words.
--
-- Audio is one file per word (not per lesson) because it hangs on vocabulary_items and a word is shared by every
-- lesson that uses it. Files live in the public "media" bucket under vocabulary/pronunciation/us/; a "uk/" folder can
-- sit next to it later. The column holds the full https URL, which Library returns as it is. The files are generated
-- with eSpeak (voice en-us), so they have to be uploaded to the bucket separately; this migration does not upload them.
--
-- IPA follows the Cambridge Dictionary US entry; phrases are joined from the entries of their parts.
--
-- Six lesson words (layer, plant, reach, rarely, slowly, pressure) already existed as INACTIVE items from the
-- video-word pronunciation migration, and V15 reused those items without activating them. The lesson vocabulary API
-- only returns senses whose item and sense are both ACTIVE, so those words never showed up. The second statement
-- activates every item that carries a lesson sense from V15 (the senses themselves were inserted ACTIVE).

UPDATE vocabulary_items item
SET ipa = pronunciation.ipa,
    pronunciation_audio_reference =
        'https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/vocabulary/pronunciation/us/'
        || pronunciation.audio_file,
    updated_at = CURRENT_TIMESTAMP
FROM (VALUES
    ('city planner', '/ˈsɪt̬.i ˈplæn.ɚ/', 'city-planner.mp3'),
    ('cope', '/koʊp/', 'cope.mp3'),
    ('hotter', '/ˈhɑː.t̬ɚ/', 'hotter.mp3'),
    ('green roof', '/ˌɡriːn ˈruːf/', 'green-roof.mp3'),
    ('layer', '/ˈleɪ.ɚ/', 'layer.mp3'),
    ('soil', '/sɔɪl/', 'soil.mp3'),
    ('plant', '/plænt/', 'plant.mp3'),
    ('waterproof membrane', '/ˈwɑː.t̬ɚ.pruːf ˈmem.breɪn/', 'waterproof-membrane.mp3')
) AS pronunciation(normalized_lemma, ipa, audio_file)
WHERE item.normalized_lemma = pronunciation.normalized_lemma;

-- V15 joined the Copenhagen sense on 'Copenhagen' while the item is stored as 'copenhagen', so that one sense was
-- never inserted. Same id as the lesson link, added here.
INSERT INTO vocabulary_senses (
    id, vocabulary_item_id, part_of_speech, english_definition, vietnamese_meaning,
    example_sentence, image_url, sort_order, status
)
SELECT '7a000000-0000-4000-8000-000000000024'::uuid, item.id, 'NOUN',
       'The capital city of Denmark.', 'Copenhagen, thủ đô Đan Mạch',
       'In Copenhagen, new flat roofs must be planted for this reason.', NULL, 0, 'ACTIVE'
FROM (
    SELECT vocabulary_item.id
    FROM vocabulary_items vocabulary_item
    WHERE vocabulary_item.normalized_lemma = 'copenhagen'
    ORDER BY vocabulary_item.id
    LIMIT 1
) item
ON CONFLICT (id) DO NOTHING;

UPDATE vocabulary_items item
SET status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP
WHERE item.status <> 'ACTIVE'
  AND EXISTS (
      SELECT 1
      FROM vocabulary_senses sense
      WHERE sense.vocabulary_item_id = item.id
        AND sense.id::text LIKE '7a000000-0000-4000-8000-%'
  );
