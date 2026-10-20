-- "Baby Shark Dance" (V8) cannot be played in the dictation player: YouTube answers the embedded player with error 150
-- ("the owner does not allow this video on other sites"), so the learner never hears a sentence. It is taken out of the
-- library by archiving it. Nothing is deleted: the sentences and anyone's earlier answers stay, and setting the status
-- back to PUBLISHED brings it back if the owner allows embedding again.

UPDATE learning_videos
SET status = 'ARCHIVED'
WHERE youtube_video_id = 'XqZsoesa55w';
