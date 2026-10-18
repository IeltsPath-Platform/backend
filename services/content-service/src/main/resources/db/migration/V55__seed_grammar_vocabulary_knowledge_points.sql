WITH proposed(
    id, code, name, kind, target_code, cefr_level, can_do, scope_in, scope_out
) AS (VALUES
    ('45000000-0000-4000-8000-020000000001'::uuid, 'G55_COMPLEX_SENTENCES', 'Complex sentences', 'GRAMMAR', 'W70_COMPLEX_STRUCTURES', 'B1', 'Combine an independent clause with a subordinate clause to express a clear relationship.', 'Basic cause, contrast, time, and condition clauses.', 'Multi-clause punctuation edge cases and highly compressed academic syntax.'),
    ('45000000-0000-4000-8000-020000000002'::uuid, 'G55_RELATIVE_CLAUSES', 'Relative clauses', 'GRAMMAR', 'W70_IDEA_DEVELOPMENT', 'B1', 'Add defining information with an appropriate relative pronoun.', 'Defining clauses with who, which, that, where, and whose.', 'Reduced clauses and complex non-defining punctuation.'),
    ('45000000-0000-4000-8000-020000000003'::uuid, 'G55_PASSIVE_VOICE', 'Passive voice', 'GRAMMAR', 'W60_T1_KEY_FEATURES', 'B1', 'Describe a process or result while keeping the affected object in focus.', 'Present and past simple passive; common process instructions.', 'Perfect continuous passive and stylistic voice shifts within long texts.'),
    ('45000000-0000-4000-8000-020000000004'::uuid, 'G55_COMPARISON', 'Comparisons', 'GRAMMAR', 'W60_T1_KEY_FEATURES', 'B1', 'Compare two or more people, places, quantities, or changes accurately.', 'Comparative and superlative forms; as ... as; more/less.', 'Advanced hedging and multi-variable statistical comparisons.'),
    ('45000000-0000-4000-8000-020000000005'::uuid, 'G60_PERFECT_TENSES', 'Perfect tenses', 'GRAMMAR', 'W60_T1_KEY_FEATURES', 'B2', 'Relate a past event or trend to a reference time using perfect forms.', 'Present perfect and past perfect in common contexts.', 'Rare aspect combinations and literary tense shifts.'),
    ('45000000-0000-4000-8000-020000000006'::uuid, 'G55_ARTICLES', 'Articles and countability', 'GRAMMAR', 'W65_DISCUSS_BOTH_VIEWS', 'B1', 'Choose a, an, the, or no article for common count and uncountable nouns.', 'First/known mention, general plural nouns, and common uncountable nouns.', 'Fixed institutional names and advanced generic-reference exceptions.'),
    ('45000000-0000-4000-8000-020000000007'::uuid, 'G55_SUBJECT_VERB_AGREEMENT', 'Subject–verb agreement', 'GRAMMAR', 'W65_DISCUSS_BOTH_VIEWS', 'B1', 'Match a verb to its grammatical subject in common sentence patterns.', 'Singular/plural subjects, collective quantifiers, and intervening phrases.', 'Inversion, complex notional agreement, and rare collective-noun variants.'),
    ('45000000-0000-4000-8000-020000000008'::uuid, 'G55_LINKING_WORDS', 'Linking words', 'GRAMMAR', 'W60_PARAGRAPH_COHESION', 'B1', 'Connect ideas with a linker that expresses the intended logical relationship.', 'Addition, contrast, cause, result, example, and sequence.', 'Cohesion across long arguments and subtle register differences.'),
    ('45000000-0000-4000-8000-020000000009'::uuid, 'V55_ACADEMIC_COLLOCATIONS', 'Academic collocations', 'VOCABULARY', 'W70_LEXICAL_PRECISION', 'B1', 'Use frequent academic word combinations naturally and precisely.', 'Common verb–noun and adjective–noun pairs used across topics.', 'Specialist terminology and low-frequency idioms.'),
    ('45000000-0000-4000-8000-020000000010'::uuid, 'V60_DATA_TREND_WORDS', 'Describing data trends', 'VOCABULARY', 'W60_T1_KEY_FEATURES', 'B2', 'Describe direction, size, stability, and comparison in a chart.', 'Common verbs, nouns, and modifiers for rises, falls, and plateaus.', 'Forecasting, causal claims, and complex uncertainty descriptions.'),
    ('45000000-0000-4000-8000-020000000011'::uuid, 'V55_PARAPHRASE_WORDS', 'Paraphrase vocabulary', 'VOCABULARY', 'W70_LEXICAL_PRECISION', 'B1', 'Replace common words with context-appropriate alternatives without changing meaning.', 'Frequent topic vocabulary and safe grammatical transformations.', 'Forced synonym substitution and changes to claims or degree.'),
    ('45000000-0000-4000-8000-020000000012'::uuid, 'V55_WORD_FORMS', 'Word forms', 'VOCABULARY', 'W70_LEXICAL_PRECISION', 'B1', 'Select a noun, verb, adjective, or adverb form that fits a sentence.', 'Common derivational families and predictable suffixes.', 'Rare derivations and spelling exceptions not supported by a pattern.'),
    ('45000000-0000-4000-8000-020000000013'::uuid, 'V55_COMMON_TOPICS', 'Common IELTS topics', 'VOCABULARY', 'W70_IDEA_DEVELOPMENT', 'B1', 'Understand and use high-frequency vocabulary about everyday academic topics.', 'Education, work, health, environment, transport, and technology.', 'Subject-specific terminology and memorised essay templates.')
)
INSERT INTO knowledge_points (
    id, topic_id, code, name, kind, learning_type, skill, description, status,
    cefr_level, can_do, scope_in, scope_out
)
SELECT proposed.id, strategy.topic_id, proposed.code, proposed.name, proposed.kind, NULL, 'ALL',
       proposed.can_do, 'INACTIVE', proposed.cefr_level, proposed.can_do, proposed.scope_in, proposed.scope_out
FROM proposed
JOIN knowledge_points strategy ON strategy.code = proposed.target_code
ON CONFLICT (code) DO NOTHING;

WITH edge(prerequisite_code, target_code) AS (VALUES
    ('G55_COMPLEX_SENTENCES', 'W70_COMPLEX_STRUCTURES'),
    ('G55_RELATIVE_CLAUSES', 'W70_IDEA_DEVELOPMENT'),
    ('G55_PASSIVE_VOICE', 'W60_T1_KEY_FEATURES'),
    ('G55_COMPARISON', 'W60_T1_KEY_FEATURES'),
    ('G60_PERFECT_TENSES', 'W60_T1_KEY_FEATURES'),
    ('G55_ARTICLES', 'W65_DISCUSS_BOTH_VIEWS'),
    ('G55_SUBJECT_VERB_AGREEMENT', 'W65_DISCUSS_BOTH_VIEWS'),
    ('G55_LINKING_WORDS', 'W60_PARAGRAPH_COHESION'),
    ('V60_DATA_TREND_WORDS', 'W60_T1_KEY_FEATURES'),
    ('V55_ACADEMIC_COLLOCATIONS', 'W70_LEXICAL_PRECISION'),
    ('V55_PARAPHRASE_WORDS', 'W70_LEXICAL_PRECISION'),
    ('V55_WORD_FORMS', 'W70_LEXICAL_PRECISION'),
    ('V55_COMMON_TOPICS', 'W70_IDEA_DEVELOPMENT')
)
INSERT INTO knowledge_point_prerequisites (knowledge_point_id, prerequisite_id)
SELECT target.id, prerequisite.id
FROM edge
JOIN knowledge_points prerequisite ON prerequisite.code = edge.prerequisite_code
JOIN knowledge_points target ON target.code = edge.target_code
ON CONFLICT DO NOTHING;
