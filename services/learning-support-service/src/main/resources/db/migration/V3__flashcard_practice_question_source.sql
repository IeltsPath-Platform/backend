-- One live flashcard per practised question and learner; a deleted card may be saved again.
-- PRACTICE_QUESTION cards point at the AI Learning practice question id through source_reference_id.
CREATE UNIQUE INDEX uq_flashcards_user_practice_question
    ON flashcards (user_id, source_reference_id)
    WHERE source_type = 'PRACTICE_QUESTION' AND status <> 'DELETED';
