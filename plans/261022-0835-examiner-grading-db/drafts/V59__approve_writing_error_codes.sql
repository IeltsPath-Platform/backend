-- DRAFT, NOT IN THE MIGRATION FOLDER: copy to services/content-service/src/main/resources/db/migration only after an
-- IELTS-qualified reviewer has confirmed each code below. Approving a code is visible to learners: the AI essay grader starts
-- keeping corrections with these codes, and learner reports can name them.
-- Reviewer: <name>, reviewed on <date>. Codes the reviewer did not accept must be removed from the list before copying.
UPDATE error_types SET review_status = 'APPROVED', updated_at = CURRENT_TIMESTAMP
WHERE code IN ('TENSE', 'ARTICLE', 'SUBJECT_VERB_AGREEMENT', 'SENTENCE_FRAGMENT',
               'WORD_CHOICE', 'COLLOCATION',
               'LINKER_MISUSE', 'NO_CLEAR_POSITION',
               'UNDERDEVELOPED_IDEA', 'OFF_TOPIC', 'T1_NO_OVERVIEW', 'T1_DATA_MISREPORTED');
