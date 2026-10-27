-- An IELTS expert has reviewed the grammar and vocabulary knowledge points that sit in the Writing topics of the
-- courses (levels B1-B2, can-do statements, scope, and the prerequisite edges that lead into the Writing strategies).
-- They become ACTIVE, so they count toward a course's expected coverage and can be taught as prerequisites.
-- An active point needs a learning type; the active grammar points are CONCEPT and the active vocabulary points are
-- MEMORY, so these follow the same mapping.
-- The entry-level (A1-A2) points in the Foundation topics and the Speaking points stay INACTIVE: their topics belong
-- to no course yet, so activating them would put points in the tree that no learner can reach.
UPDATE knowledge_points
SET status = 'ACTIVE',
    learning_type = CASE kind WHEN 'GRAMMAR' THEN 'CONCEPT' WHEN 'VOCABULARY' THEN 'MEMORY' END,
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'INACTIVE'
  AND kind IN ('GRAMMAR', 'VOCABULARY')
  AND cefr_level IS NOT NULL
  AND can_do IS NOT NULL AND btrim(can_do) <> ''
  AND code IN (
      'G55_ARTICLES', 'G55_COMPARISON', 'G55_COMPLEX_SENTENCES', 'G55_LINKING_WORDS', 'G55_PASSIVE_VOICE',
      'G55_RELATIVE_CLAUSES', 'G55_SUBJECT_VERB_AGREEMENT', 'G60_PERFECT_TENSES',
      'V55_ACADEMIC_COLLOCATIONS', 'V55_COMMON_TOPICS', 'V55_PARAPHRASE_WORDS', 'V55_WORD_FORMS',
      'V60_DATA_TREND_WORDS');
