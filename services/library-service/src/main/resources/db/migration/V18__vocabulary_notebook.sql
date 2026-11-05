CREATE UNIQUE INDEX uq_flashcards_user_vocabulary_sense
    ON flashcards (user_id, vocabulary_sense_id)
    WHERE source_type = 'VOCABULARY_SENSE' AND status <> 'DELETED';
