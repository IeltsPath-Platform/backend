-- Allow level A1 for learning videos (V1 only allowed A2 to C2) and seed one A1 demo video.
--
-- The check constraint V1 created inline is named by PostgreSQL as learning_videos_level_check; it is replaced by one
-- with the same name that also lists A1. Existing rows keep their level.
--
-- The A1 video is "Baby Shark Dance" by Pinkfong (YouTube id XqZsoesa55w, checked against its title). Its sentences are
-- the spoken / sung lines without the repeated "doo doo doo" and the start_ms / end_ms are ESTIMATED, not read from
-- captions: play the video and correct the times (UPDATE video_segments ...) before using this beyond local
-- development and demos. Ids are fixed and every insert is skipped when the row already exists.

ALTER TABLE learning_videos DROP CONSTRAINT IF EXISTS learning_videos_level_check;
ALTER TABLE learning_videos ADD CONSTRAINT learning_videos_level_check
    CHECK (level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2'));

INSERT INTO learning_videos (id, youtube_video_id, youtube_url, title, description, thumbnail_url, duration_seconds,
                             level, required_feature_key, status)
VALUES ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4d06', 'XqZsoesa55w', 'https://www.youtube.com/watch?v=XqZsoesa55w',
        'Baby Shark Dance - Pinkfong',
        'Demo video for dictation practice: very short, simple sentences from a children''s song. Sentence timings are estimates.',
        'https://img.youtube.com/vi/XqZsoesa55w/hqdefault.jpg', 137, 'A1', NULL, 'PUBLISHED')
ON CONFLICT (youtube_video_id) DO NOTHING;

INSERT INTO video_segments (id, video_id, sequence_no, start_ms, end_ms, transcript, translation_vi)
SELECT x.id::uuid, v.id, x.sequence_no, x.start_ms, x.end_ms, x.transcript, x.translation_vi
FROM (VALUES
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c6001', 1,   6000,   8000, 'Baby shark',          'Cá mập con'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c6002', 2,  20000,  22000, 'Mommy shark',         'Mẹ cá mập'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c6003', 3,  34000,  36000, 'Daddy shark',         'Bố cá mập'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c6004', 4,  48000,  50000, 'Grandma shark',       'Bà cá mập'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c6005', 5,  62000,  64000, 'Grandpa shark',       'Ông cá mập'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c6006', 6,  76000,  78500, 'Let''s go hunt',      'Cùng đi săn nào'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c6007', 7,  90000,  92000, 'Run away',            'Chạy đi'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c6008', 8, 104000, 106500, 'Safe at last',        'Cuối cùng cũng an toàn'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c6009', 9, 118000, 120000, 'It''s the end',       'Hết rồi')
) AS x(id, sequence_no, start_ms, end_ms, transcript, translation_vi)
JOIN learning_videos v ON v.youtube_video_id = 'XqZsoesa55w'
ON CONFLICT (video_id, sequence_no) DO NOTHING;
