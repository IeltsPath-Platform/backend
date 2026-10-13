-- A lesson is now its title, a Markdown summary and its slide and document links (V28); the block pipeline is removed.
-- Before the block tables go, their content moves to where the lesson keeps it:
--   * knowledge points taught or measured by blocks join lesson_knowledge_points, which now decides a lesson's skills;
--   * TEXT blocks become the summary of lessons that have none (V29 summaries stay).

INSERT INTO lesson_knowledge_points (lesson_id, knowledge_point_id)
SELECT b.lesson_id, bk.knowledge_point_id
FROM lesson_block_knowledge_points bk
JOIN lesson_blocks b ON b.id = bk.block_id
UNION
SELECT b.lesson_id, qkp.knowledge_point_id
FROM lesson_blocks b
JOIN lesson_block_questions bq ON bq.block_id = b.id
JOIN question_knowledge_points qkp ON qkp.question_version_id = bq.question_version_id
ON CONFLICT DO NOTHING;

UPDATE lessons l SET summary_markdown = t.markdown
FROM (SELECT lesson_id, string_agg(text_content, E'\n\n' ORDER BY sort_order) AS markdown
      FROM lesson_blocks WHERE block_type = 'TEXT' GROUP BY lesson_id) t
WHERE t.lesson_id = l.id AND l.summary_markdown IS NULL;

DROP TABLE lesson_block_knowledge_points;
DROP TABLE lesson_block_questions;
DROP TABLE lesson_block_vocabulary;
DROP TABLE lesson_blocks;
