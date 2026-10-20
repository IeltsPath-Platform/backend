-- Demo videos for video dictation at B1, B2, C1 and C2 (A2 is in V6). Each has 6-8 short sentences from its opening.
--
--   B1  Steve Jobs' 2005 Stanford Commencement Address   UF8uR6Z6KLc   (Stanford)
--   B2  Do schools kill creativity? - Sir Ken Robinson    iG9CE55wbtY   (TED)
--   C1  How Great Leaders Inspire Action - Simon Sinek    qp0HIF3SfI4   (TED)
--   C2  J.K. Rowling Speaks at Harvard Commencement       wHGqp8lz36c   (Harvard Magazine)
--
-- The video ids were checked against the titles above. The sentences were written down from the spoken opening and
-- start_ms / end_ms are ESTIMATED, not read from captions: play each video, then correct the wording and the times
-- (UPDATE video_segments ...) before using this beyond local development and demos. The levels are a rough
-- judgement of speech speed and vocabulary.
--
-- Ids are fixed and every insert is skipped when the row already exists. required_feature_key is NULL: all open.

INSERT INTO learning_videos (id, youtube_video_id, youtube_url, title, description, thumbnail_url, duration_seconds,
                             level, required_feature_key, status)
VALUES
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4d02', 'UF8uR6Z6KLc', 'https://www.youtube.com/watch?v=UF8uR6Z6KLc',
     'Steve Jobs'' 2005 Stanford Commencement Address',
     'Demo video for dictation practice: the opening of a graduation speech. Sentence timings are estimates.',
     'https://img.youtube.com/vi/UF8uR6Z6KLc/hqdefault.jpg', 874, 'B1', NULL, 'PUBLISHED'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4d03', 'iG9CE55wbtY', 'https://www.youtube.com/watch?v=iG9CE55wbtY',
     'Do schools kill creativity? - Sir Ken Robinson',
     'Demo video for dictation practice: the opening of a TED talk on education. Sentence timings are estimates.',
     'https://img.youtube.com/vi/iG9CE55wbtY/hqdefault.jpg', 1152, 'B2', NULL, 'PUBLISHED'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4d04', 'qp0HIF3SfI4', 'https://www.youtube.com/watch?v=qp0HIF3SfI4',
     'How Great Leaders Inspire Action - Simon Sinek',
     'Demo video for dictation practice: the opening of a TED talk on leadership. Sentence timings are estimates.',
     'https://img.youtube.com/vi/qp0HIF3SfI4/hqdefault.jpg', 1069, 'C1', NULL, 'PUBLISHED'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4d05', 'wHGqp8lz36c', 'https://www.youtube.com/watch?v=wHGqp8lz36c',
     'J.K. Rowling Speaks at Harvard Commencement',
     'Demo video for dictation practice: the opening of a graduation speech. Sentence timings are estimates.',
     'https://img.youtube.com/vi/wHGqp8lz36c/hqdefault.jpg', NULL, 'C2', NULL, 'PUBLISHED')
ON CONFLICT (youtube_video_id) DO NOTHING;

INSERT INTO video_segments (id, video_id, sequence_no, start_ms, end_ms, transcript, translation_vi)
SELECT x.id::uuid, v.id, x.sequence_no, x.start_ms, x.end_ms, x.transcript, x.translation_vi
FROM (VALUES
    -- B1 · Steve Jobs, Stanford 2005
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4f01', 'UF8uR6Z6KLc', 1,  8000,  9500, 'Thank you.',
        'Cảm ơn mọi người.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4f02', 'UF8uR6Z6KLc', 2,  9500, 16500,
        'I am honored to be with you today at your commencement from one of the finest universities in the world.',
        'Tôi rất vinh dự được có mặt cùng các bạn hôm nay trong lễ tốt nghiệp của một trong những trường đại học tốt nhất thế giới.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4f03', 'UF8uR6Z6KLc', 3, 16500, 20500, 'Truth be told, I never graduated from college.',
        'Nói thật, tôi chưa bao giờ tốt nghiệp đại học.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4f04', 'UF8uR6Z6KLc', 4, 20500, 25500,
        'And this is the closest I''ve ever gotten to a college graduation.',
        'Và đây là lần tôi đến gần nhất với một lễ tốt nghiệp đại học.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4f05', 'UF8uR6Z6KLc', 5, 25500, 30000,
        'Today I want to tell you three stories from my life.',
        'Hôm nay tôi muốn kể cho các bạn nghe ba câu chuyện trong cuộc đời tôi.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4f06', 'UF8uR6Z6KLc', 6, 30000, 34000, 'That''s it. No big deal. Just three stories.',
        'Chỉ vậy thôi. Không có gì to tát. Chỉ là ba câu chuyện.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c4f07', 'UF8uR6Z6KLc', 7, 34000, 38000, 'The first story is about connecting the dots.',
        'Câu chuyện đầu tiên là về việc nối các dấu chấm.'),

    -- B2 · Sir Ken Robinson, TED
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5001', 'iG9CE55wbtY', 1, 12000, 15000, 'Good morning. How are you?',
        'Chào buổi sáng. Mọi người khỏe không?'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5002', 'iG9CE55wbtY', 2, 15000, 17500, 'It''s been great, hasn''t it?',
        'Thật tuyệt phải không?'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5003', 'iG9CE55wbtY', 3, 17500, 21000, 'I''ve been blown away by the whole thing.',
        'Tôi hoàn toàn choáng ngợp bởi mọi thứ.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5004', 'iG9CE55wbtY', 4, 21000, 23500, 'In fact, I''m leaving.',
        'Thực ra, tôi sẽ bỏ đi đây.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5005', 'iG9CE55wbtY', 5, 23500, 31500,
        'There have been three themes, haven''t there, running through the conference, which are relevant to what I want to talk about.',
        'Có ba chủ đề xuyên suốt hội nghị, đúng không, liên quan đến điều tôi muốn nói.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5006', 'iG9CE55wbtY', 6, 31500, 40000,
        'One is the extraordinary evidence of human creativity in all of the presentations that we''ve had and in all of the people here.',
        'Một là bằng chứng phi thường về sự sáng tạo của con người trong mọi bài thuyết trình và nơi tất cả những người ở đây.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5007', 'iG9CE55wbtY', 7, 40000, 44000, 'Just the variety of it and the range of it.',
        'Chỉ riêng sự đa dạng và phạm vi của nó.'),

    -- C1 · Simon Sinek, TED
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5101', 'qp0HIF3SfI4', 1, 14000, 18000,
        'How do you explain when things don''t go as we assumed?',
        'Bạn giải thích thế nào khi mọi việc không diễn ra như ta tưởng?'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5102', 'qp0HIF3SfI4', 2, 18000, 26000,
        'Or better, how do you explain when others are able to achieve things that seem to defy all of the assumptions?',
        'Hay đúng hơn, bạn giải thích thế nào khi người khác làm được những điều dường như đi ngược mọi giả định?'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5103', 'qp0HIF3SfI4', 3, 26000, 30000, 'For example: Why is Apple so innovative?',
        'Ví dụ: Tại sao Apple lại sáng tạo đến vậy?'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5104', 'qp0HIF3SfI4', 4, 30000, 35500,
        'Year after year after year, they''re more innovative than all their competition.',
        'Năm này qua năm khác, họ sáng tạo hơn mọi đối thủ.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5105', 'qp0HIF3SfI4', 5, 35500, 39000, 'And yet, they''re just a computer company.',
        'Vậy mà họ chỉ là một công ty máy tính.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5106', 'qp0HIF3SfI4', 6, 39000, 41500, 'They''re just like everyone else.',
        'Họ cũng giống như mọi công ty khác.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5107', 'qp0HIF3SfI4', 7, 41500, 49000,
        'They have the same access to the same talent, the same agencies, the same consultants, the same media.',
        'Họ có cùng khả năng tiếp cận cùng nhân tài, cùng công ty quảng cáo, cùng tư vấn, cùng truyền thông.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5108', 'qp0HIF3SfI4', 8, 49000, 53000,
        'Then why is it that they seem to have something different?',
        'Vậy tại sao họ dường như có điều gì đó khác biệt?'),

    -- C2 · J.K. Rowling, Harvard
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5201', 'wHGqp8lz36c', 1, 25000, 34000,
        'President Faust, members of the Harvard Corporation and the Board of Overseers, members of the faculty, proud parents, and, above all, graduates.',
        'Thưa Chủ tịch Faust, các thành viên Hội đồng Harvard và Ban Giám sát, các giảng viên, các bậc phụ huynh tự hào và trên hết là các tân cử nhân.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5202', 'wHGqp8lz36c', 2, 34000, 38000,
        'The first thing I would like to say is thank you.',
        'Điều đầu tiên tôi muốn nói là lời cảm ơn.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5203', 'wHGqp8lz36c', 3, 38000, 42500,
        'Not only has Harvard given me an extraordinary honour,',
        'Harvard không chỉ trao cho tôi một vinh dự phi thường,'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5204', 'wHGqp8lz36c', 4, 42500, 50500,
        'but the weeks of fear and nausea I''ve experienced at the thought of giving this commencement address have made me lose weight.',
        'mà những tuần lo sợ và buồn nôn khi nghĩ đến việc đọc bài phát biểu này còn giúp tôi giảm cân.'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5205', 'wHGqp8lz36c', 5, 50500, 53000, 'A win-win situation!',
        'Một tình huống đôi bên cùng có lợi!'),
    ('c3f7a2d4-6b1e-4f8a-9d2c-5e7a1b3c5206', 'wHGqp8lz36c', 6, 53000, 62000,
        'Now all I have to do is take deep, calming breaths, squint at the red banners and convince myself that I am at the world''s largest Gryffindor reunion.',
        'Giờ tôi chỉ cần hít thở sâu, nheo mắt nhìn những lá cờ đỏ và tự thuyết phục rằng mình đang ở buổi họp mặt Gryffindor lớn nhất thế giới.')
) AS x(id, youtube_id, sequence_no, start_ms, end_ms, transcript, translation_vi)
JOIN learning_videos v ON v.youtube_video_id = x.youtube_id
ON CONFLICT (video_id, sequence_no) DO NOTHING;
