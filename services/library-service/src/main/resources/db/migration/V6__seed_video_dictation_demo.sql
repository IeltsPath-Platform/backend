-- Demo data for video dictation: one published A2 video split into 7 short sentences.
--
-- The video is "Me at the zoo" (YouTube id jNQXAC9IVRw, 19 seconds, English speech) and the sentences are its spoken
-- words. start_ms / end_ms are ESTIMATED, not read from official captions: check them against the real video and
-- correct them (UPDATE video_segments ...) before using this beyond local development and demos.
--
-- Ids are fixed and every insert is skipped when the row already exists, so videos added through the admin API are
-- never duplicated or overwritten. required_feature_key is NULL: the video is open to every learner.

INSERT INTO learning_videos (id, youtube_video_id, youtube_url, title, description, thumbnail_url, duration_seconds,
                             level, required_feature_key, status)
VALUES ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4d01',
        'jNQXAC9IVRw',
        'https://www.youtube.com/watch?v=jNQXAC9IVRw',
        'Me at the zoo',
        'Demo video for dictation practice: a short visit to the elephants. Sentence timings are estimates.',
        'https://img.youtube.com/vi/jNQXAC9IVRw/hqdefault.jpg',
        19,
        'A2',
        NULL,
        'PUBLISHED')
ON CONFLICT (youtube_video_id) DO NOTHING;

INSERT INTO video_segments (id, video_id, sequence_no, start_ms, end_ms, transcript, translation_vi)
SELECT x.id::uuid, v.id, x.sequence_no, x.start_ms, x.end_ms, x.transcript, x.translation_vi
FROM (VALUES
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4e01', 1,     0,  3000, 'All right, so here we are',
        'Được rồi, chúng ta đang ở đây'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4e02', 2,  3000,  5500, 'in front of the elephants',
        'ngay trước mặt những chú voi'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4e03', 3,  5500,  8500, 'The cool thing about these guys',
        'Điều tuyệt về những chú này'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4e04', 4,  8500, 12500, 'is that they have really, really, really long trunks',
        'là chúng có cái vòi thật sự rất rất dài'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4e05', 5, 12500, 14500, 'And that''s cool',
        'Và điều đó thật tuyệt'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4e06', 6, 14500, 16500, 'And that''s pretty much all',
        'Và về cơ bản chỉ có vậy'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4e07', 7, 16500, 19000, 'there is to say',
        'để nói')
) AS x(id, sequence_no, start_ms, end_ms, transcript, translation_vi)
JOIN learning_videos v ON v.youtube_video_id = 'jNQXAC9IVRw'
ON CONFLICT (video_id, sequence_no) DO NOTHING;
