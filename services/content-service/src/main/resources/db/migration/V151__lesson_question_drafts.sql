-- One open question-draft request per author and lesson: a double click or a second tab cannot queue two.
-- Drafts asked for a knowledge point alone (no lessonId in the request) are not covered.
CREATE UNIQUE INDEX uq_ai_drafts_open_lesson
    ON ai_drafts (requested_by, (request ->> 'lessonId'))
    WHERE kind = 'QUESTIONS' AND request ->> 'lessonId' IS NOT NULL
      AND status IN ('QUEUED', 'GENERATING', 'READY');
