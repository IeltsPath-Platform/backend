-- Takes the cartoon English lessons of English Phrases Hub out of video dictation. The words of what is said are printed
-- in the picture itself (a caption bar near the bottom, like "It is seven o'clock."), so they cannot be switched off like
-- YouTube's own captions and they give the answer of every sentence away.
--
-- ARCHIVED, not deleted: learners may already have attempts on their sentences (video_dictation_attempts refers to
-- them), which are kept; the library lists published videos only, so these no longer show. To bring one back,
-- set its status to PUBLISHED again.
--
--   hGt8njnS1R8  English Listening Practice for Beginners: Waking Up
--   Frq4TZf0F1s  Asking for Directions in English: Easy Conversation Practice for Beginners
--   reKgQh0E9kg  My Daily Routine: Easy English Listening Practice (A2)
--   dbnch1goJr8  My Family at Home: Easy English Listening Practice (A2)

UPDATE learning_videos
SET status = 'ARCHIVED'
WHERE youtube_video_id IN ('hGt8njnS1R8', 'Frq4TZf0F1s', 'reKgQh0E9kg', 'dbnch1goJr8');
