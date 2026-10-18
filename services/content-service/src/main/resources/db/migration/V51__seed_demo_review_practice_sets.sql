-- Fresh, unrevealed review choices for the Reading lessons used in the learner journey demo.
CREATE TEMP TABLE demo_review_seed_sets (
    code VARCHAR(100) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    knowledge_point_code VARCHAR(100) NOT NULL,
    lesson_id UUID NOT NULL,
    passage TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_review_seed_sets (code, title, knowledge_point_code, lesson_id, passage) VALUES
    ('PS-KP1-E', 'Luyen them PS-KP1-E: Electric buses', 'DEMO_READING_MAIN_IDEA',
     '20000000-0000-4000-8000-030000000002',
     'A. Several towns are adding electric buses because they reduce street-level exhaust.
B. New batteries let buses travel farther, but cold weather can shorten their range.
C. Transport teams also need charging points and trained mechanics before a full switch.'),
    ('PS-KP2-D', 'Luyen them PS-KP2-D: Shaded streets', 'DR_IDEA_OR_DETAIL',
     '20000000-0000-4000-8000-030000000003',
     'A. Neighbourhoods with trees and shaded paths can feel cooler on hot days.
B. In one survey, 62 percent of walkers chose the shaded route even when it was longer.
C. However, young trees need years of care before they provide a wide canopy.'),
    ('PS-KP3-D', 'Luyen them PS-KP3-D: Community gardens', 'DR_TOPIC_SENTENCE',
     '20000000-0000-4000-8000-030000000001',
     'A. Community gardens offer more than fresh vegetables. Neighbours share tools, learn from one another, and turn unused lots into meeting places.
B. Some gardens also teach children how food grows. Classes plant seeds, watch them develop, and harvest vegetables together.
C. These projects need volunteers, but local groups often keep them running for years.'),
    ('PS-KP4-D', 'Luyen them PS-KP4-D: Public libraries', 'DR_MATCHING_HEADINGS',
     '20000000-0000-4000-8000-030000000004',
     'A. Many libraries now lend more than books. Visitors can borrow tools, musical instruments, and devices for short periods.
B. Libraries also provide shared rooms. Students meet for group work, while local clubs hold evening events.
C. These new services have not replaced reading. Most libraries still protect quiet areas for people who want to study.' );

INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, status, required_feature_key)
SELECT md5('demo-review-package:' || code)::uuid, code, title, 'PRACTICE_SET', NULL, lesson_id, 'PUBLISHED', NULL
FROM demo_review_seed_sets;

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at)
SELECT md5('demo-review-version:' || code)::uuid, md5('demo-review-package:' || code)::uuid,
       1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP
FROM demo_review_seed_sets;

UPDATE content_packages p
SET current_published_version_id = md5('demo-review-version:' || seed.code)::uuid
FROM demo_review_seed_sets seed
WHERE p.id = md5('demo-review-package:' || seed.code)::uuid;

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order)
SELECT md5('demo-review-section:' || code)::uuid, md5('demo-review-version:' || code)::uuid,
       title, 'READING', 1
FROM demo_review_seed_sets;

INSERT INTO content_assets (id, asset_type, text_content, validation_status)
SELECT md5('demo-review-passage:' || code)::uuid, 'PASSAGE', passage, 'VALID'
FROM demo_review_seed_sets;

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order)
SELECT md5('demo-review-asset-link:' || code)::uuid, md5('demo-review-passage:' || code)::uuid,
       md5('demo-review-section:' || code)::uuid, 0
FROM demo_review_seed_sets;

CREATE TEMP TABLE demo_review_seed_questions (
    package_code VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL,
    stem TEXT NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    correct_key VARCHAR(1) NOT NULL,
    explanation TEXT NOT NULL,
    hint TEXT NOT NULL,
    PRIMARY KEY (package_code, sort_order)
) ON COMMIT DROP;

INSERT INTO demo_review_seed_questions VALUES
    ('PS-KP1-E', 1, 'Which statement best summarizes the whole passage?',
     'Electric buses are being adopted, but cities must prepare for their limits.',
     'Cold weather has stopped every town from using buses.',
     'Mechanics prefer buses that produce more exhaust.', 'A',
     'The passage gives a reason for adopting electric buses, then describes limits and preparation needs.',
     'Check whether the answer includes the benefits and the cautions.'),
    ('PS-KP1-E', 2, 'What is the main idea of paragraph C?',
     'A full switch also requires charging equipment and trained staff.',
     'Every town already has enough charging points.',
     'Mechanics are replacing all bus drivers.', 'A',
     'The paragraph groups charging points and trained mechanics as practical requirements.',
     'The paragraph starts with what transport teams need.'),
    ('PS-KP1-E', 3, 'Which title best covers all three paragraphs?',
     'Electric Buses: Promise and Preparation',
     'A History of Cold Weather',
     'Why Towns Are Closing Bus Routes', 'A',
     'The first paragraph gives a benefit; the next two explain limits and preparation.',
     'Choose a title broad enough for paragraphs A, B, and C.'),
    ('PS-KP2-D', 1, 'Which is a specific detail supporting the point that walkers prefer shade?',
     'Trees can make neighbourhoods feel cooler.',
     'Sixty-two percent chose the shaded route even when it was longer.',
     'Young trees take time to grow.', 'B',
     'The survey percentage is a specific piece of evidence; the other choices are broader points or a limitation.',
     'Look for a measurable example rather than a general claim.'),
    ('PS-KP2-D', 2, 'Which statement is the general idea of paragraph A?',
     'Shaded neighbourhood paths can feel cooler on hot days.',
     'One survey included 62 percent of walkers.',
     'Every shaded route is longer.', 'A',
     'Paragraph A makes a broad claim about the effect of trees and shade.',
     'A general idea covers the paragraph; a detail gives one example.'),
    ('PS-KP2-D', 3, 'Which limitation is stated in paragraph C?',
     'Young trees need years of care before making a wide canopy.',
     'Walkers refuse to use shaded paths.',
     'Trees lower the temperature in every building.', 'A',
     'Paragraph C directly says that young trees need years of care before the canopy is wide.',
     'Find the sentence introduced by “However”.'),
    ('PS-KP3-D', 1, 'Which sentence is the topic sentence of paragraph A?',
     'Community gardens offer more than fresh vegetables.',
     'Neighbours share tools.',
     'Unused lots can be meeting places.', 'A',
     'The first sentence states the broad point; the next sentence gives examples.',
     'Look for the sentence that the other details explain.'),
    ('PS-KP3-D', 2, 'Which sentence is the topic sentence of paragraph B?',
     'Children plant seeds.',
     'Some gardens also teach children how food grows.',
     'Classes harvest vegetables together.', 'B',
     'The first sentence introduces the educational role; the rest lists class activities.',
     'The next two sentences explain what the classes do.'),
    ('PS-KP3-D', 3, 'Which sentence best summarizes paragraph C?',
     'Local groups can keep garden projects going despite the need for volunteers.',
     'Every project has enough volunteers.',
     'Vegetables grow without anyone caring for them.', 'A',
     'The sentence combines the need for volunteers with the fact that local groups sustain projects.',
     'Include both the challenge and the outcome.'),
    ('PS-KP4-D', 1, 'Choose the best heading for paragraph A.',
     'Libraries lend useful items beyond books',
     'Libraries remove all books from their shelves',
     'How to repair musical instruments', 'A',
     'The paragraph lists tools, instruments, and devices that libraries lend.',
     'The heading should cover the whole list of borrowed items.'),
    ('PS-KP4-D', 2, 'Choose the best heading for paragraph B.',
     'Shared spaces for study and local groups',
     'Why students avoid group work',
     'Libraries close in the evening', 'A',
     'The paragraph describes rooms used by students and local clubs.',
     'Match both examples in the paragraph.'),
    ('PS-KP4-D', 3, 'Choose the best heading for paragraph C.',
     'Reading remains part of the modern library',
     'Quiet study areas have disappeared',
     'New services have ended borrowing', 'A',
     'The paragraph says new services coexist with quiet reading and study areas.',
     'The final paragraph corrects the idea that new services replaced reading.');

INSERT INTO questions (id, question_type, skill, status)
SELECT md5('demo-review-question:' || package_code || ':' || sort_order)::uuid,
       'MULTIPLE_CHOICE', 'READING', 'PUBLISHED'
FROM demo_review_seed_questions;

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version,
                               explanation, hint, status)
SELECT md5('demo-review-question-version:' || q.package_code || ':' || q.sort_order)::uuid,
       md5('demo-review-question:' || q.package_code || ':' || q.sort_order)::uuid,
       1, q.stem,
       jsonb_build_array(
           jsonb_build_object('optionKey', 'A', 'content', q.option_a, 'sortOrder', 1),
           jsonb_build_object('optionKey', 'B', 'content', q.option_b, 'sortOrder', 2),
           jsonb_build_object('optionKey', 'C', 'content', q.option_c, 'sortOrder', 3)),
       jsonb_build_object('type', 'CHOICE', 'correct', q.correct_key), 1,
       q.explanation, q.hint, 'PUBLISHED'
FROM demo_review_seed_questions q;

UPDATE questions q
SET current_published_version_id = qv.id
FROM question_versions qv
JOIN demo_review_seed_questions seed
  ON qv.id = md5('demo-review-question-version:' || seed.package_code || ':' || seed.sort_order)::uuid
WHERE qv.question_id = q.id
  AND q.id = md5('demo-review-question:' || seed.package_code || ':' || seed.sort_order)::uuid;

-- Assign each question to only the knowledge point whose lesson owns the practice set.
INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight)
SELECT md5('demo-review-question-version:' || questions.package_code || ':' || questions.sort_order)::uuid,
       kp.id, 1.00
FROM demo_review_seed_questions questions
JOIN demo_review_seed_sets sets ON sets.code = questions.package_code
JOIN knowledge_points kp ON kp.code = sets.knowledge_point_code;

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score)
SELECT md5('demo-review-section-question:' || q.package_code || ':' || q.sort_order)::uuid,
       md5('demo-review-section:' || q.package_code)::uuid,
       md5('demo-review-question-version:' || q.package_code || ':' || q.sort_order)::uuid,
       q.sort_order, 1.00
FROM demo_review_seed_questions q;
