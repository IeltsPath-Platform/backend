CREATE TABLE game_wordly_dictionaries (
 version VARCHAR(64) PRIMARY KEY,
 word_count INTEGER NOT NULL CHECK(word_count>0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE game_wordly_dictionary_words (
 version VARCHAR(64) NOT NULL REFERENCES game_wordly_dictionaries(version),
 position INTEGER NOT NULL CHECK(position>0),
 source_word_id UUID NOT NULL,
 answer VARCHAR(100) NOT NULL CHECK(answer ~ '^[A-Z]+$'),
 meaning TEXT NOT NULL,
 PRIMARY KEY(version,position), UNIQUE(version,answer)
);
ALTER TABLE game_matches ADD COLUMN wordly_dictionary_version VARCHAR(64)
 REFERENCES game_wordly_dictionaries(version);
ALTER TABLE game_matches ADD COLUMN wordly_initial_word_count INTEGER NOT NULL DEFAULT 0 CHECK(wordly_initial_word_count>=0);
-- Legacy per-match pools remain available to matches started before this migration.
