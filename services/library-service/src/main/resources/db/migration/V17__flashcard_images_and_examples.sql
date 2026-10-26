ALTER TABLE flashcards ADD COLUMN image_key varchar(300);
ALTER TABLE flashcards ADD COLUMN example_sentence varchar(1000);
ALTER TABLE flashcard_decks ADD COLUMN cover_image_key varchar(300);
