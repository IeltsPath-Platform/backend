-- AI-authored draft practice material; an IELTS-qualified examiner must review it before learner release.
-- Practice sets for the grammar error codes that V118 gave a trap point (ARTICLE, SUBJECT_VERB_AGREEMENT,
-- SENTENCE_FRAGMENT, TENSE). Each set has four multiple-choice questions mapped to the grammar point; two carry the trap
-- option, so the error-trap review can find at least two matching traps in a set, as the coverage rule requires.
-- Correct keys within a set are spread (A, B, C, A) so no key covers more than half of a set.

CREATE TEMP TABLE grammar_trap_targets (
    seed_no INT PRIMARY KEY,
    error_code TEXT NOT NULL,
    set_no INT NOT NULL,
    parent_code TEXT NOT NULL,
    package_title TEXT NOT NULL,
    skill TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO grammar_trap_targets VALUES
(301, 'ARTICLE', 1, 'G55_ARTICLES', 'Choosing a, an or the for a first mention', 'WRITING'),
(302, 'ARTICLE', 2, 'G55_ARTICLES', 'Articles before vowel sounds and repeated nouns', 'WRITING'),
(303, 'SUBJECT_VERB_AGREEMENT', 1, 'G55_SUBJECT_VERB_AGREEMENT', 'Subject and verb separated by a phrase', 'WRITING'),
(304, 'SUBJECT_VERB_AGREEMENT', 2, 'G55_SUBJECT_VERB_AGREEMENT', 'Each, results and neither: choosing the verb', 'WRITING'),
(305, 'SENTENCE_FRAGMENT', 1, 'G55_COMPLEX_SENTENCES', 'Complete sentence or fragment: a clause alone', 'WRITING'),
(306, 'SENTENCE_FRAGMENT', 2, 'G55_COMPLEX_SENTENCES', 'Participle phrases and relative clauses as fragments', 'WRITING'),
(307, 'TENSE', 1, 'G60_PERFECT_TENSES', 'Present perfect or past with a time phrase', 'WRITING'),
(308, 'TENSE', 2, 'G60_PERFECT_TENSES', 'Future perfect and present perfect in context', 'WRITING');

CREATE TEMP TABLE grammar_trap_items (
    seed_no INT NOT NULL,
    question_no INT NOT NULL,
    question_type TEXT NOT NULL,
    stem TEXT NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    correct_key TEXT NOT NULL,
    trap_key TEXT,
    explanation TEXT NOT NULL,
    PRIMARY KEY (seed_no, question_no)
) ON COMMIT DROP;

-- ARTICLE, set 1
INSERT INTO grammar_trap_items VALUES
(301,1,'MULTIPLE_CHOICE','Our team visited ___ hospital in the city centre. The hospital opened last year.','a','the','an','A','B','This is the first mention of a singular countable noun, so "a" is right. "The" assumes both speakers already know which hospital is meant.'),
(301,2,'MULTIPLE_CHOICE','___ sun rises in the east every morning.','A','The','An','B','A','The sun is unique, so it takes "the". Without a definite article the noun would wrongly be read as one of many.'),
(301,3,'MULTIPLE_CHOICE','Students need ___ access to the internet to finish their research.','an','the','no article','C',NULL,'"Access" is used here as an uncountable noun, so no article comes before it.'),
(301,4,'MULTIPLE_CHOICE','___ bananas are a good source of potassium.','No article','The','A','A',NULL,'A plural noun used in a general sense takes no article.');

-- ARTICLE, set 2
INSERT INTO grammar_trap_items VALUES
(302,1,'MULTIPLE_CHOICE','She is ___ honest person I have ever met.','an','a','the','A','B','"Honest" starts with a silent h, so it begins with a vowel sound and takes "an".'),
(302,2,'MULTIPLE_CHOICE','He bought ___ new laptop last week. The laptop has a long battery life.','the','a','an','B','A','The first mention of the laptop takes "a". "The" is used only after the laptop has been identified.'),
(302,3,'MULTIPLE_CHOICE','___ Pacific Ocean is the largest ocean on Earth.','A','No article','The','C',NULL,'Unique geographical features such as this ocean take "the".'),
(302,4,'MULTIPLE_CHOICE','___ coffee is grown in many tropical countries.','No article','The','A','A',NULL,'An uncountable noun used in a general sense takes no article.');

-- SUBJECT_VERB_AGREEMENT, set 1
INSERT INTO grammar_trap_items VALUES
(303,1,'MULTIPLE_CHOICE','The box of pencils ___ on the desk.','is','are','was','A','B','The subject is "box", which is singular, so the verb is "is". "Pencils" sits inside a phrase and does not decide the verb.'),
(303,2,'MULTIPLE_CHOICE','Neither the teacher nor the students ___ ready to start.','is','are','was','B','A','With "neither ... nor", the verb agrees with the subject closer to it, here "students", so "are" is correct.'),
(303,3,'MULTIPLE_CHOICE','Economics ___ a popular subject at university.','are','were','is','C',NULL,'"Economics" is a singular noun in this sense, so it takes "is".'),
(303,4,'MULTIPLE_CHOICE','The number of applicants ___ increasing each year.','is','are','were','A',NULL,'"The number of" takes a singular verb, so "is" is correct.');

-- SUBJECT_VERB_AGREEMENT, set 2
INSERT INTO grammar_trap_items VALUES
(304,1,'MULTIPLE_CHOICE','Each of the managers ___ responsible for a team.','is','are','were','A','B','"Each" is singular, so the verb is "is" even though a plural noun follows.'),
(304,2,'MULTIPLE_CHOICE','The results of the survey ___ surprising.','is','are','was','B','A','"Results" is the plural subject here, so the verb is "are".'),
(304,3,'MULTIPLE_CHOICE','Mathematics ___ my favourite subject.','are','were','is','C',NULL,'"Mathematics" is treated as singular, so "is" is correct.'),
(304,4,'MULTIPLE_CHOICE','Neither of the two options ___ suitable for our budget.','is','are','were','A',NULL,'"Neither" is singular, so the verb is "is".');

-- SENTENCE_FRAGMENT, set 1
INSERT INTO grammar_trap_items VALUES
(305,1,'MULTIPLE_CHOICE','Which option is a complete sentence?','We missed the meeting because the train was late.','Because the train was late.','Missing the meeting because of the late train.','A','B','A subordinate clause such as "because the train was late" cannot stand alone. Option A has a subject and a main verb.'),
(305,2,'MULTIPLE_CHOICE','Pick the grammatically complete sentence.','Although it rained heavily.','Although it rained heavily, we went for a walk.','Heavy rain in the morning and at night.','B','A','"Although it rained heavily" needs a main clause to complete it.'),
(305,3,'MULTIPLE_CHOICE','Which of these is a complete sentence?','The report, which the team wrote.','Finished by the team yesterday.','The report is finished.','C',NULL,'Option C has a subject and a main verb. The others have no main verb.'),
(305,4,'MULTIPLE_CHOICE','Choose the option that forms a full sentence.','She ran fast along the river.','Running fast along the river.','Fast along the river, running.','A',NULL,'Option A has a subject "She" and a finite verb "ran". The others are only phrases.');

-- SENTENCE_FRAGMENT, set 2
INSERT INTO grammar_trap_items VALUES
(306,1,'MULTIPLE_CHOICE','Which sentence is complete?','Students who study every day pass the exams.','Students who study every day.','Students, studying every day, for the exams.','A','B','"Students who study every day" has no main verb. The correct option adds "pass" as the main verb.'),
(306,2,'MULTIPLE_CHOICE','Select the complete sentence in context.','Having finished the project.','Having finished the project, the team went home.','The team, having finished.','B','A','"Having finished the project" is a participle phrase. It needs a main clause with its own subject and verb.'),
(306,3,'MULTIPLE_CHOICE','Which option is a full sentence?','Opened in 1990 by the city council.','In 1990, the museum.','The museum opened in 1990.','C',NULL,'The complete option has a subject and a finite verb. The others lack one.'),
(306,4,'MULTIPLE_CHOICE','Choose the complete sentence.','It was cold, so we stayed inside.','Because it was cold.','Cold, and inside.','A',NULL,'The complete option has a subject, a verb and a complete thought.');

-- TENSE, set 1
INSERT INTO grammar_trap_items VALUES
(307,1,'MULTIPLE_CHOICE','Since 2015, the company ___ three new factories.','has opened','opened','opens','A','B','"Since" with a period that reaches up to now takes the present perfect, not the simple past.'),
(307,2,'MULTIPLE_CHOICE','By the time we arrived, the film ___.','has already started','had already started','started','B','A','An earlier past event before another past event takes the past perfect.'),
(307,3,'MULTIPLE_CHOICE','Last year, sales ___ by 20%.','have risen','are rising','rose','C',NULL,'"Last year" is a finished time, so the simple past is used.'),
(307,4,'MULTIPLE_CHOICE','She ___ in London for ten years and still likes it.','has lived','lived','is living','A',NULL,'A period up to now, with "still", takes the present perfect.');

-- TENSE, set 2
INSERT INTO grammar_trap_items VALUES
(308,1,'MULTIPLE_CHOICE','I ___ this book twice, and I want to read it again.','have read','read','am reading','A','B','Two readings up to now are an experience, so the present perfect is used, not the simple past.'),
(308,2,'MULTIPLE_CHOICE','The students ___ their essays by next Friday.','will finish','will have finished','finish','B','A','"By next Friday" names a point when the essays will already be complete, so the future perfect is needed.'),
(308,3,'MULTIPLE_CHOICE','They ___ on the project since January.','worked','are working','have been working','C',NULL,'"Since January" with an activity still going on takes the present perfect continuous.'),
(308,4,'MULTIPLE_CHOICE','Water ___ at 100 degrees Celsius.','boils','has boiled','boiled','A',NULL,'A general scientific fact takes the present simple.');

CREATE TEMP TABLE grammar_trap_packages ON COMMIT DROP AS
SELECT t.*,
       ('81000000-0000-4000-8000-' || lpad(t.seed_no::text, 12, '0'))::uuid AS package_id,
       ('82000000-0000-4000-8000-' || lpad(t.seed_no::text, 12, '0'))::uuid AS package_version_id,
       ('83000000-0000-4000-8000-' || lpad(t.seed_no::text, 12, '0'))::uuid AS section_id
FROM grammar_trap_targets t;

INSERT INTO content_packages (id, code, title, package_type, status, required_feature_key)
SELECT package_id, 'PS-TRAP-' || error_code || '-' || set_no, package_title, 'PRACTICE_SET', 'PUBLISHED', NULL
FROM grammar_trap_packages;

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at)
SELECT package_version_id, package_id, 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP
FROM grammar_trap_packages;

UPDATE content_packages p
SET current_published_version_id = t.package_version_id
FROM grammar_trap_packages t
WHERE p.id = t.package_id;

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, instructions)
SELECT section_id, package_version_id, package_title, skill, 1,
       'Choose the answer that is correct in context. Review the explanation after each question.'
FROM grammar_trap_packages;

CREATE TEMP TABLE grammar_trap_questions ON COMMIT DROP AS
SELECT p.seed_no, p.error_code, p.parent_code, p.skill, p.package_id, p.section_id,
       i.question_no, i.question_type, i.stem, i.option_a, i.option_b, i.option_c,
       i.correct_key, i.trap_key, i.explanation,
       ('84000000-0000-4000-8000-' || lpad((p.seed_no * 10 + i.question_no)::text, 12, '0'))::uuid AS question_id,
       ('85000000-0000-4000-8000-' || lpad((p.seed_no * 10 + i.question_no)::text, 12, '0'))::uuid AS question_version_id,
       ('86000000-0000-4000-8000-' || lpad((p.seed_no * 10 + i.question_no)::text, 12, '0'))::uuid AS section_question_id
FROM grammar_trap_packages p
JOIN grammar_trap_items i ON i.seed_no = p.seed_no;

INSERT INTO questions (id, question_type, skill, purpose, status)
SELECT question_id, question_type, skill, 'LEARNING', 'PUBLISHED'
FROM grammar_trap_questions;

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, difficulty, status)
SELECT question_version_id, question_id, 1,
       stem,
       jsonb_build_array(
           jsonb_build_object('optionKey', 'A', 'content', option_a, 'sortOrder', 1),
           jsonb_build_object('optionKey', 'B', 'content', option_b, 'sortOrder', 2),
           jsonb_build_object('optionKey', 'C', 'content', option_c, 'sortOrder', 3)),
       jsonb_build_object('type', 'CHOICE', 'correct', correct_key) ||
           CASE WHEN trap_key IS NULL THEN '{}'::jsonb
                ELSE jsonb_build_object('distractors', jsonb_build_object(trap_key, error_code)) END,
       1, explanation, 'MEDIUM', 'PUBLISHED'
FROM grammar_trap_questions;

UPDATE questions q
SET current_published_version_id = s.question_version_id
FROM grammar_trap_questions s
WHERE q.id = s.question_id;

INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight)
SELECT s.question_version_id, parent.id, 1.00
FROM grammar_trap_questions s
JOIN knowledge_points parent ON parent.code = s.parent_code;

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score)
SELECT section_question_id, section_id, question_version_id, question_no, 1.00
FROM grammar_trap_questions;
