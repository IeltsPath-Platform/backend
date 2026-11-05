ALTER TABLE vocabulary_items
    ADD COLUMN ipa_uk VARCHAR(255),
    ADD COLUMN ipa_us VARCHAR(255),
    ADD COLUMN audio_uk_reference TEXT,
    ADD COLUMN audio_us_reference TEXT,
    ADD COLUMN frequency_rank INT,
    ADD COLUMN academic_word_list BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN word_family TEXT;

CREATE TABLE vocabulary_topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    name_vi VARCHAR(120) NOT NULL,
    cover_key TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE vocabulary_senses
    ADD COLUMN cefr_level VARCHAR(2) CHECK (cefr_level IS NULL OR cefr_level IN ('A1','A2','B1','B2','C1','C2')),
    ADD COLUMN topic_id UUID REFERENCES vocabulary_topics(id),
    ADD COLUMN register VARCHAR(30),
    ADD COLUMN grammar_note TEXT,
    ADD COLUMN usage_note TEXT,
    ADD COLUMN example_translation TEXT;

CREATE INDEX idx_vocabulary_senses_cefr ON vocabulary_senses(cefr_level);
CREATE INDEX idx_vocabulary_senses_topic ON vocabulary_senses(topic_id);

CREATE TABLE vocabulary_collocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sense_id UUID NOT NULL REFERENCES vocabulary_senses(id) ON DELETE CASCADE,
    phrase VARCHAR(255) NOT NULL,
    meaning_vi TEXT,
    example TEXT,
    sort_order INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_vocabulary_collocations_sense ON vocabulary_collocations(sense_id);

CREATE TABLE vocabulary_relations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sense_id UUID NOT NULL REFERENCES vocabulary_senses(id) ON DELETE CASCADE,
    related_lemma VARCHAR(255) NOT NULL,
    related_item_id UUID REFERENCES vocabulary_items(id) ON DELETE SET NULL,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('SYNONYM','ANTONYM','RELATED')),
    sort_order INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_vocabulary_relations_sense ON vocabulary_relations(sense_id);

CREATE TABLE vocabulary_common_mistakes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sense_id UUID NOT NULL REFERENCES vocabulary_senses(id) ON DELETE CASCADE,
    wrong_usage TEXT NOT NULL,
    correct_usage TEXT NOT NULL,
    explanation TEXT,
    sort_order INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_vocabulary_common_mistakes_sense ON vocabulary_common_mistakes(sense_id);

CREATE TABLE vocabulary_examples (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sense_id UUID NOT NULL REFERENCES vocabulary_senses(id) ON DELETE CASCADE,
    sentence TEXT NOT NULL,
    translation_vi TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_vocabulary_examples_sense ON vocabulary_examples(sense_id);

CREATE TABLE vocabulary_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(180) NOT NULL,
    description TEXT,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('BAND','TOPIC')),
    cefr_level VARCHAR(2) CHECK (cefr_level IS NULL OR cefr_level IN ('A1','A2','B1','B2','C1','C2')),
    topic_id UUID REFERENCES vocabulary_topics(id),
    cover_key TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((kind = 'BAND' AND cefr_level IS NOT NULL AND topic_id IS NULL) OR
           (kind = 'TOPIC' AND topic_id IS NOT NULL AND cefr_level IS NULL))
);

CREATE TABLE vocabulary_set_items (
    set_id UUID NOT NULL REFERENCES vocabulary_sets(id) ON DELETE CASCADE,
    sense_id UUID NOT NULL REFERENCES vocabulary_senses(id) ON DELETE CASCADE,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (set_id, sense_id)
);
CREATE INDEX idx_vocabulary_set_items_order ON vocabulary_set_items(set_id, sort_order);
CREATE INDEX idx_vocabulary_set_items_sense ON vocabulary_set_items(sense_id);
