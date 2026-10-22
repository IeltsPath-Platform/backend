-- DRAFT completion of the knowledge graph, pending review by an IELTS expert.
-- 1. Every active knowledge point gets a CEFR level and a can-do statement. Levels follow the course band
--    (<= 4.5 B1, <= 6.0 B2, <= 7.5 C1) except for entry-level skills that sit one level lower.
-- 2. The twelve active points that had no edge are connected to the graph.
-- 3. Six entry-level points (A1-B1) and eight Speaking points are added as INACTIVE drafts in INACTIVE topics that
--    belong to no course, so they never reach a learner until a reviewer attaches and activates them.
-- 4. An active point that is still missing a CEFR level or a can-do statement afterwards (for example one authored
--    after the seeds) is set to INACTIVE and keeps its data, because an active point now requires both.
-- An edge "A -> B" means A is a prerequisite of B and is stored as (knowledge_point_id = B, prerequisite_id = A).
-- A prerequisite never has a higher CEFR level than the point that depends on it.

UPDATE knowledge_points k
SET cefr_level = v.cefr, updated_at = CURRENT_TIMESTAMP
FROM (VALUES
    ('LS_NUM', 'B1'), ('LS_SPELL', 'B1'), ('LS_PARA', 'B1'), ('LS_TRAP', 'B2'), ('TR_L_DETAIL', 'B1'),
    ('L60_FORM_NOTE_COMPLETION', 'B2'), ('L60_SIGNPOSTING', 'B2'),
    ('L65_CORRECTION', 'C1'), ('L65_SPEAKER_PURPOSE', 'C1'),
    ('L70_MAP_DIAGRAM', 'C1'), ('L70_MULTI_SPEAKER_ATTITUDE', 'C1'),
    ('DR_TOPIC_SENTENCE', 'B1'), ('DEMO_READING_MAIN_IDEA', 'B1'), ('DR_IDEA_OR_DETAIL', 'B1'),
    ('DR_MATCHING_HEADINGS', 'B2'), ('TR_R_SCANNING', 'B1'), ('TR_R_WORD_IN_CONTEXT', 'B1'),
    ('PR_SENTENCE_COMPLETION', 'B2'), ('PR_MATCHING_INFORMATION', 'B2'), ('TFNG_FALSE_VS_NOT_GIVEN', 'B2'),
    ('R60_SKIM_STRUCTURE', 'B2'), ('R60_SUMMARY_COMPLETION', 'B2'),
    ('R65_EVIDENCE_INFERENCE', 'C1'), ('R65_PARAPHRASE', 'C1'),
    ('R70_COMPLEX_PARAPHRASE', 'C1'), ('R70_HEDGING_SCOPE', 'C1'), ('R70_WRITER_VIEW_YNNG', 'C1'),
    ('DEMO_READING_W1_CHART', 'B2'), ('DEMO_READING_W2_OPINION', 'B2'),
    ('DEMO_WRITING_CRITERION_TA', 'B2'), ('DEMO_WRITING_CRITERION_TR', 'B2'), ('DEMO_WRITING_CRITERION_CC', 'B2'),
    ('DEMO_WRITING_CRITERION_GRA', 'B2'), ('DEMO_WRITING_CRITERION_LR', 'B2'),
    ('DEMO_WRITING_LINKING', 'B2'), ('DEMO_WRITING_TOPIC_SENTENCE', 'B2'), ('DEMO_WRITING_TREND_LANGUAGE', 'B2'),
    ('TR_W_OPINION', 'B2'), ('W60_PARAGRAPH_COHESION', 'B2'), ('W60_T1_KEY_FEATURES', 'B2'),
    ('W65_DISCUSS_BOTH_VIEWS', 'C1'),
    ('W70_COMPLEX_STRUCTURES', 'C1'), ('W70_IDEA_DEVELOPMENT', 'C1'), ('W70_LEXICAL_PRECISION', 'C1')
) AS v(code, cefr)
WHERE k.code = v.code AND k.cefr_level IS NULL;

UPDATE knowledge_points k
SET can_do = v.can_do, scope_in = v.scope_in, scope_out = v.scope_out, updated_at = CURRENT_TIMESTAMP
FROM (VALUES
    ('LS_NUM', 'Write down numbers, dates, times and prices accurately, including when the speaker changes their mind.',
        'Cardinal numbers, dates, times and prices in short phone-style recordings.', 'Calculating with the numbers heard.'),
    ('LS_PARA', 'Recognise an answer that is restated in different words from the recording.',
        'Simple synonyms and restatements in short everyday talks.', 'Dense academic lecture vocabulary.'),
    ('LS_SPELL', 'Write down names and codes that are spelled out letter by letter.',
        'Letter names and common surname and postcode patterns.', 'Proper names that are not spelled out.'),
    ('LS_TRAP', 'Ignore information that is mentioned and then corrected or rejected.',
        'Self-corrections and changed plans in short dialogues.', 'Corrections spread across several speakers.'),
    ('TR_L_DETAIL', 'Pick out and record a specific time, date, price or name from a short recording.',
        'Single-detail questions with a clear cue.', 'Multi-step note completion.'),
    ('L60_FORM_NOTE_COMPLETION', 'Complete a form or set of notes with the right information in the order it is heard.',
        'Form and note completion with a word limit.', 'Table and flow-chart completion.'),
    ('L60_SIGNPOSTING', 'Notice words and phrases that signal a change of topic or that an answer is coming.',
        'Sequencing and contrast markers such as first, however and finally.', 'Structure with no spoken markers.'),
    ('L65_CORRECTION', 'Record the final piece of information when a speaker corrects, changes or rejects a detail.',
        'Corrections within a short exchange between two speakers.', 'Corrections that need background knowledge.'),
    ('L65_SPEAKER_PURPOSE', 'Identify a speaker''s purpose, reason or conclusion even when it is expressed in different words.',
        'Stated and lightly implied purpose in everyday and study-related talks.', 'Long academic argument analysis.'),
    ('L70_MAP_DIAGRAM', 'Follow location directions to label a map or diagram correctly.',
        'Prepositions of place, directions and route language.', 'Maps with no spoken location cues.'),
    ('L70_MULTI_SPEAKER_ATTITUDE', 'Track each speaker''s attitude and opinion in a conversation with several speakers.',
        'Agreement, disagreement and hedged opinions among two to four speakers.', 'Non-verbal cues.'),
    ('DEMO_READING_MAIN_IDEA', 'Choose the option that best summarises the main idea of a passage.',
        'Short academic passages with one clear main idea.', 'Passages with several competing main ideas.'),
    ('DR_IDEA_OR_DETAIL', 'Tell a general claim apart from the figures or examples used to support it.',
        'Claim versus evidence inside a paragraph.', 'Judging the quality of the evidence.'),
    ('DR_MATCHING_HEADINGS', 'Choose the heading that matches the main idea of a whole paragraph.',
        'List-of-headings tasks on single paragraphs.', 'Headings that need several paragraphs to decide.'),
    ('DR_TOPIC_SENTENCE', 'Find the sentence that states the idea a whole paragraph develops.',
        'Topic sentences at the start, middle or end of a paragraph.', 'Paragraphs with no explicit topic sentence.'),
    ('PR_MATCHING_INFORMATION', 'Find the paragraph that contains a specific detail even when the detail is paraphrased.',
        'Matching-information tasks with paraphrased details.', 'Details spread over several paragraphs.'),
    ('PR_SENTENCE_COMPLETION', 'Fill a gap with the right word from the text, in the correct form and within the word limit.',
        'Sentence completion with a word limit.', 'Gaps that need words that are not in the text.'),
    ('R60_SKIM_STRUCTURE', 'Skim to grasp the structure and main idea of each paragraph before answering questions.',
        'Headings, first and last sentences and signal words.', 'Detailed reading for individual answers.'),
    ('R60_SUMMARY_COMPLETION', 'Complete a summary with words that fit the meaning, the grammar and the word limit.',
        'Summary completion from the passage.', 'Summary completion from a word bank.'),
    ('R65_EVIDENCE_INFERENCE', 'Distinguish conclusions supported by the text from plausible assumptions.',
        'Supported versus unsupported inference, with qualifications and limits kept.', 'Deciding with outside knowledge.'),
    ('R65_PARAPHRASE', 'Match restatements that preserve comparison, scope and certainty rather than repeating keywords.',
        'Meaning-preserving paraphrase of sentences.', 'Paraphrase of whole paragraphs.'),
    ('R70_COMPLEX_PARAPHRASE', 'Link a question to a passage when the idea is restated with very different structure and vocabulary.',
        'Heavy restructuring and synonym replacement.', 'Questions that need background knowledge.'),
    ('R70_HEDGING_SCOPE', 'Recognise hedging and scope-limiting words that change the meaning of a claim.',
        'Words such as may, tend to, some, most and only.', 'Irony and sarcasm.'),
    ('R70_WRITER_VIEW_YNNG', 'Separate the writer''s view from information that the passage only reports.',
        'Yes/No/Not Given on opinions and claims.', 'Judging whether the writer is right.'),
    ('TFNG_FALSE_VS_NOT_GIVEN', 'Tell information that the text contradicts from information the text does not mention.',
        'True/False/Not Given on facts.', 'Yes/No/Not Given on opinions.'),
    ('TR_R_SCANNING', 'Scan quickly for a number, time or detail named in a question, then read the sentence around it closely.',
        'Names, numbers and dates as scanning cues.', 'Skimming for overall structure.'),
    ('TR_R_WORD_IN_CONTEXT', 'Use the surrounding sentences to work out the meaning of an unfamiliar word instead of skipping it.',
        'Context clues, word parts and contrast signals.', 'Memorising word lists.'),
    ('DEMO_READING_W1_CHART', 'Describe a chart with an overview of the main trend, then support it with figures and comparisons.',
        'Line, bar and pie charts.', 'Maps and process diagrams.'),
    ('DEMO_READING_W2_OPINION', 'Write an opinion essay whose body paragraphs each open with a topic sentence followed by reasons and examples.',
        'Agree/disagree Task 2 essays.', 'Discussion and problem-solution essays.'),
    ('DEMO_WRITING_CRITERION_CC', 'Organise ideas into logical paragraphs and link sentences with cohesive devices used for the right function.',
        'Paragraphing, sequencing, linking and reference.', 'Style and tone.'),
    ('DEMO_WRITING_CRITERION_GRA', 'Combine simple and complex sentences with correct tense, agreement and punctuation and few meaning-changing errors.',
        'Sentence range and grammatical accuracy.', 'Vocabulary choice.'),
    ('DEMO_WRITING_CRITERION_LR', 'Use a range of accurate words, collocations and spelling, and paraphrase instead of repeating the prompt.',
        'Vocabulary range, collocation, spelling and paraphrase.', 'Grammatical accuracy.'),
    ('DEMO_WRITING_CRITERION_TA', 'Write a clear overview, select and compare the key features of a chart, and meet the minimum length.',
        'Task 1 achievement.', 'The Task 2 response.'),
    ('DEMO_WRITING_CRITERION_TR', 'Answer every part of the question, keep a clear and consistent position, and develop ideas with reasons and examples.',
        'Task 2 response.', 'Task 1 achievement.'),
    ('DEMO_WRITING_LINKING', 'Use linking words with the right function (contrast, cause, result, addition) so ideas flow smoothly.',
        'Common linkers and their position in a sentence.', 'Reference and substitution.'),
    ('DEMO_WRITING_TOPIC_SENTENCE', 'Paraphrase the question in the introduction, state a position, and open each body paragraph with a topic sentence.',
        'The introduction and topic sentences in Task 2.', 'Conclusions.'),
    ('DEMO_WRITING_TREND_LANGUAGE', 'Use verbs, nouns and adverbs for rises, falls, stability and peaks when describing a chart.',
        'Trend verbs and nouns, and adverbs of degree and speed.', 'Describing maps or processes.'),
    ('TR_W_OPINION', 'State a clear opinion in the introduction, give one reason with an example in each body paragraph, and restate the opinion in the conclusion.',
        'Opinion essay structure.', 'Balanced discussion essays.'),
    ('W60_PARAGRAPH_COHESION', 'Connect the sentences in a paragraph with linkers and reference so the ideas run smoothly.',
        'Linkers, pronouns and repetition inside a paragraph.', 'Cohesion between paragraphs.'),
    ('W60_T1_KEY_FEATURES', 'Select and compare the most striking features of a chart instead of listing every figure.',
        'Selecting and grouping key features.', 'Writing the overview sentence.'),
    ('W65_DISCUSS_BOTH_VIEWS', 'Present both views fairly in separate paragraphs with reasons and examples, then state your own opinion.',
        'Discuss-both-views Task 2 essays.', 'Problem-solution essays.'),
    ('W70_COMPLEX_STRUCTURES', 'Use a variety of complex sentences and subordinate clauses accurately and naturally.',
        'Relative, conditional and adverbial clauses.', 'Rare or literary structures.'),
    ('W70_IDEA_DEVELOPMENT', 'Extend each main idea with a clear explanation and a specific example without repeating yourself.',
        'Explanation and example in body paragraphs.', 'Choosing the main ideas.'),
    ('W70_LEXICAL_PRECISION', 'Choose words and phrases with the right shade of meaning and the right collocation for the context.',
        'Precision, collocation and register.', 'Idioms.')
) AS v(code, can_do, scope_in, scope_out)
WHERE k.code = v.code AND k.can_do IS NULL;

-- An active point must carry a CEFR level and a can-do statement. Authors who created points after the seeds
-- fill them in and activate the point again.
UPDATE knowledge_points
SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP
WHERE status = 'ACTIVE' AND (cefr_level IS NULL OR can_do IS NULL OR btrim(can_do) = '');

-- Draft topics for the new points. They are INACTIVE and attached to no course on purpose.
INSERT INTO topics (id, code, name, sort_order, status, skill, course_id) VALUES
    ('73000000-0000-4000-8000-010000000001', 'FOUNDATION_LISTENING', 'Foundation Listening (A1-B1)', 990, 'INACTIVE', 'LISTENING', NULL),
    ('73000000-0000-4000-8000-010000000002', 'FOUNDATION_READING', 'Foundation Reading (A1-B1)', 991, 'INACTIVE', 'READING', NULL),
    ('73000000-0000-4000-8000-010000000003', 'FOUNDATION_WRITING', 'Foundation Writing, Grammar and Vocabulary (A1-B1)', 992, 'INACTIVE', 'WRITING', NULL),
    ('73000000-0000-4000-8000-010000000004', 'SPEAKING_FRAMEWORK', 'Speaking framework (B1-C1)', 993, 'INACTIVE', 'SPEAKING', NULL)
ON CONFLICT DO NOTHING;

WITH proposed(id, topic_code, code, name, kind, skill, cefr_level, can_do, scope_in, scope_out) AS (VALUES
    ('73000000-0000-4000-8000-020000000001'::uuid, 'FOUNDATION_READING', 'R55_READ_SHORT_TEXTS', 'Read short texts for the main topic', 'STRATEGY', 'READING', 'A2',
        'Read a short simple text and say what it is mainly about.', 'Short factual texts on familiar topics.', 'Academic passages.'),
    ('73000000-0000-4000-8000-020000000002'::uuid, 'FOUNDATION_LISTENING', 'L55_LETTERS_AND_NAMES', 'Recognise letters and spelled names', 'STRATEGY', 'LISTENING', 'A1',
        'Recognise the names of English letters and write down a name that is spelled out.', 'Letter names and short spelled names.', 'Unspelled proper names.'),
    ('73000000-0000-4000-8000-020000000003'::uuid, 'FOUNDATION_LISTENING', 'L55_FAMILIAR_DETAILS', 'Follow short talk on familiar topics', 'STRATEGY', 'LISTENING', 'A2',
        'Pick out simple details such as numbers and times from short, slow speech on familiar topics.', 'Short slow recordings with one detail at a time.', 'Fast or multi-speaker recordings.'),
    ('73000000-0000-4000-8000-020000000004'::uuid, 'FOUNDATION_WRITING', 'W55_SIMPLE_PARAGRAPH', 'Write a simple paragraph', 'STRATEGY', 'WRITING', 'B1',
        'Write a short paragraph with a clear main idea and a supporting example.', 'One paragraph on a familiar topic.', 'Full essays.'),
    ('73000000-0000-4000-8000-020000000005'::uuid, 'FOUNDATION_WRITING', 'G55_BASIC_SENTENCE_PATTERNS', 'Basic sentence patterns', 'GRAMMAR', 'ALL', 'A2',
        'Form simple affirmative, negative and question sentences with correct word order in the present and past.', 'Simple present and past, basic word order.', 'Complex sentences.'),
    ('73000000-0000-4000-8000-020000000006'::uuid, 'FOUNDATION_WRITING', 'V55_BASIC_WORDS', 'High-frequency everyday words', 'VOCABULARY', 'ALL', 'A1',
        'Understand and use the most common everyday English words for people, places, time and actions.', 'The most frequent everyday words.', 'Academic vocabulary.'),
    ('73000000-0000-4000-8000-020000000007'::uuid, 'SPEAKING_FRAMEWORK', 'S55_PART1_FAMILIAR_ANSWERS', 'Answer familiar questions (Part 1)', 'STRATEGY', 'SPEAKING', 'B1',
        'Answer familiar personal questions with a direct answer and a reason.', 'Part 1 questions about home, work, study and hobbies.', 'Abstract topics.'),
    ('73000000-0000-4000-8000-020000000008'::uuid, 'SPEAKING_FRAMEWORK', 'S55_PRONUNCIATION_BASICS', 'Pronunciation basics', 'PRONUNCIATION', 'SPEAKING', 'B1',
        'Pronounce common words and sentence stress clearly enough to be understood.', 'Individual sounds and word stress in common words.', 'Connected speech and intonation.'),
    ('73000000-0000-4000-8000-020000000009'::uuid, 'SPEAKING_FRAMEWORK', 'S60_FLUENCY_COHERENCE', 'Fluency and coherence', 'STRATEGY', 'SPEAKING', 'B2',
        'Speak at length with few hesitations, using simple linking devices to connect ideas.', 'Extended answers with basic linkers.', 'Complex discourse markers.'),
    ('73000000-0000-4000-8000-020000000010'::uuid, 'SPEAKING_FRAMEWORK', 'S60_PART2_LONG_TURN', 'Long turn on a cue card (Part 2)', 'STRATEGY', 'SPEAKING', 'B2',
        'Plan and deliver a talk of one to two minutes on a cue card with a clear beginning, middle and end.', 'Part 2 cue cards and one minute of preparation.', 'Part 3 discussion.'),
    ('73000000-0000-4000-8000-020000000011'::uuid, 'SPEAKING_FRAMEWORK', 'S60_SPEAKING_VOCABULARY', 'Topic vocabulary and paraphrase in speech', 'VOCABULARY', 'SPEAKING', 'B2',
        'Use topic vocabulary and simple paraphrase when a word is missing.', 'Everyday and common IELTS topics.', 'Idiomatic and low-frequency language.'),
    ('73000000-0000-4000-8000-020000000012'::uuid, 'SPEAKING_FRAMEWORK', 'S65_PART3_DISCUSSION', 'Discuss abstract topics (Part 3)', 'STRATEGY', 'SPEAKING', 'C1',
        'Give and justify opinions on abstract topics and compare different viewpoints.', 'Part 3 questions on society and trends.', 'Personal Part 1 questions.'),
    ('73000000-0000-4000-8000-020000000013'::uuid, 'SPEAKING_FRAMEWORK', 'S65_GRAMMAR_RANGE', 'Grammatical range in speech', 'GRAMMAR', 'SPEAKING', 'C1',
        'Use a mix of simple and complex structures with generally good control when speaking.', 'Subordinate clauses, conditionals and relative clauses in speech.', 'Written-only structures.'),
    ('73000000-0000-4000-8000-020000000014'::uuid, 'SPEAKING_FRAMEWORK', 'S70_PRONUNCIATION_FEATURES', 'Intonation and connected speech', 'PRONUNCIATION', 'SPEAKING', 'C1',
        'Use intonation, stress and connected speech to support meaning.', 'Sentence stress, intonation and linking sounds.', 'Regional accent training.')
)
INSERT INTO knowledge_points (id, topic_id, code, name, kind, learning_type, skill, description, status,
                              cefr_level, can_do, scope_in, scope_out)
SELECT proposed.id, topic.id, proposed.code, proposed.name, proposed.kind, NULL, proposed.skill, proposed.can_do, 'INACTIVE',
       proposed.cefr_level, proposed.can_do, proposed.scope_in, proposed.scope_out
FROM proposed
JOIN topics topic ON topic.code = proposed.topic_code
ON CONFLICT (code) DO NOTHING;

-- Edges are resolved by knowledge point code, so they do not depend on the ids of older seed rows.
CREATE TEMP TABLE graph_completion_edges (prerequisite_id UUID NOT NULL, target_id UUID NOT NULL) ON COMMIT DROP;

WITH edge(prerequisite_code, target_code) AS (VALUES
    ('DR_TOPIC_SENTENCE', 'DEMO_READING_MAIN_IDEA'),
    ('DEMO_READING_MAIN_IDEA', 'DR_IDEA_OR_DETAIL'),
    ('DR_IDEA_OR_DETAIL', 'DR_MATCHING_HEADINGS'),
    ('TR_R_SCANNING', 'PR_MATCHING_INFORMATION'),
    ('R55_READ_SHORT_TEXTS', 'DR_TOPIC_SENTENCE'),
    ('R55_READ_SHORT_TEXTS', 'TR_R_SCANNING'),
    ('L55_LETTERS_AND_NAMES', 'LS_SPELL'),
    ('L55_FAMILIAR_DETAILS', 'LS_NUM'),
    ('L55_FAMILIAR_DETAILS', 'TR_L_DETAIL'),
    ('TR_W_OPINION', 'DEMO_READING_W2_OPINION'),
    ('DEMO_READING_W2_OPINION', 'DEMO_WRITING_CRITERION_TR'),
    ('DEMO_READING_W1_CHART', 'DEMO_WRITING_CRITERION_TA'),
    ('DEMO_WRITING_LINKING', 'DEMO_WRITING_CRITERION_CC'),
    ('DEMO_WRITING_TOPIC_SENTENCE', 'DEMO_WRITING_CRITERION_CC'),
    ('G55_SUBJECT_VERB_AGREEMENT', 'DEMO_WRITING_CRITERION_GRA'),
    ('G60_PERFECT_TENSES', 'DEMO_WRITING_CRITERION_GRA'),
    ('DEMO_WRITING_CRITERION_GRA', 'W70_COMPLEX_STRUCTURES'),
    ('DEMO_WRITING_TREND_LANGUAGE', 'DEMO_WRITING_CRITERION_LR'),
    ('V55_ACADEMIC_COLLOCATIONS', 'DEMO_WRITING_CRITERION_LR'),
    ('DEMO_WRITING_CRITERION_LR', 'W70_LEXICAL_PRECISION'),
    ('W55_SIMPLE_PARAGRAPH', 'TR_W_OPINION'),
    ('G55_BASIC_SENTENCE_PATTERNS', 'W55_SIMPLE_PARAGRAPH'),
    ('G55_BASIC_SENTENCE_PATTERNS', 'G55_SUBJECT_VERB_AGREEMENT'),
    ('G55_BASIC_SENTENCE_PATTERNS', 'G55_COMPLEX_SENTENCES'),
    ('V55_BASIC_WORDS', 'V55_COMMON_TOPICS'),
    ('S55_PART1_FAMILIAR_ANSWERS', 'S60_PART2_LONG_TURN'),
    ('S55_PRONUNCIATION_BASICS', 'S60_FLUENCY_COHERENCE'),
    ('S55_PRONUNCIATION_BASICS', 'S70_PRONUNCIATION_FEATURES'),
    ('S60_FLUENCY_COHERENCE', 'S60_PART2_LONG_TURN'),
    ('S60_FLUENCY_COHERENCE', 'S65_PART3_DISCUSSION'),
    ('S60_PART2_LONG_TURN', 'S65_PART3_DISCUSSION'),
    ('S60_SPEAKING_VOCABULARY', 'S65_PART3_DISCUSSION'),
    ('G55_BASIC_SENTENCE_PATTERNS', 'S65_GRAMMAR_RANGE'),
    ('V55_BASIC_WORDS', 'S60_SPEAKING_VOCABULARY')
)
INSERT INTO graph_completion_edges (prerequisite_id, target_id)
SELECT prerequisite.id, target.id
FROM edge
JOIN knowledge_points prerequisite ON prerequisite.code = edge.prerequisite_code
JOIN knowledge_points target ON target.code = edge.target_code;

INSERT INTO knowledge_point_prerequisites (knowledge_point_id, prerequisite_id)
SELECT target_id, prerequisite_id FROM graph_completion_edges
ON CONFLICT DO NOTHING;

DO $$
DECLARE
    levels CONSTANT text[] := ARRAY['A1', 'A2', 'B1', 'B2', 'C1', 'C2'];
BEGIN
    IF (SELECT count(*) FROM graph_completion_edges) <> 34 THEN
        RAISE EXCEPTION 'expected 34 prerequisite edges, resolved %', (SELECT count(*) FROM graph_completion_edges);
    END IF;
    IF EXISTS (SELECT 1 FROM knowledge_points
               WHERE status = 'ACTIVE' AND (cefr_level IS NULL OR can_do IS NULL OR btrim(can_do) = '')) THEN
        RAISE EXCEPTION 'an active knowledge point must have a CEFR level and a can-do statement';
    END IF;
    IF EXISTS (SELECT 1 FROM graph_completion_edges e
               JOIN knowledge_points target ON target.id = e.target_id
               JOIN knowledge_points prerequisite ON prerequisite.id = e.prerequisite_id
               WHERE array_position(levels, prerequisite.cefr_level) > array_position(levels, target.cefr_level)) THEN
        RAISE EXCEPTION 'a prerequisite must not have a higher CEFR level than the point that depends on it';
    END IF;
    IF EXISTS (
        WITH RECURSIVE walk(start_id, current_id, depth) AS (
            SELECT prerequisite_id, knowledge_point_id, 1 FROM knowledge_point_prerequisites
            UNION ALL
            SELECT walk.start_id, p.knowledge_point_id, walk.depth + 1
            FROM walk JOIN knowledge_point_prerequisites p ON p.prerequisite_id = walk.current_id
            WHERE walk.depth < 100)
        SELECT 1 FROM walk WHERE start_id = current_id) THEN
        RAISE EXCEPTION 'the prerequisite graph must stay acyclic';
    END IF;
    IF EXISTS (SELECT 1 FROM knowledge_points
               WHERE id::text LIKE '73000000-0000-4000-8000-02%' AND status <> 'INACTIVE') THEN
        RAISE EXCEPTION 'knowledge points added as drafts must stay INACTIVE';
    END IF;
END $$;
