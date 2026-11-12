-- Version 5 of the Speaking rubric: coherence is marked harder. In a test an answer with five sentences that had nothing to do
-- with the question (food, football, a supermarket, a school subject) still got FC 5.5 where the examiner gave 4.5, and the
-- grader listed only three of the five wandering sentences and one of them twice.
-- Only the FC criterion changes; the system prompt, LR, GRA and PR are copied from version 4 as they are:
--   * the FC description makes the grader read each answer one sentence at a time against the question, list each wandering
--     sentence once, and names the ceilings: three or more sentences unrelated to the question limit FC to 4.5, one or two
--     limit it to 5.5, and ideas stated without a reason or example in most answers limit it to 5.5;
--   * FC bands 4 to 7 say how many unrelated sentences each band can have.
-- The ceilings are practice-grade proposals for a harder marker, not official IELTS figures, and need re-checking against the
-- examiner calibration set. This migration does not edit versions 1 to 4: they stay readable for earlier submissions.
-- It also adds the codes the grader needs to list these mistakes: until now a sentence that wanders away from the question, a
-- repeated idea and an idea without a reason or example had no code, so they could not be listed with their quote and fix.

UPDATE rubrics SET status = 'RETIRED', updated_at = CURRENT_TIMESTAMP
WHERE task = 'SPEAKING' AND status = 'ACTIVE';

WITH next_version AS (
    SELECT COALESCE(MAX(rubric_version), 0) + 1 AS version FROM rubrics WHERE code = 'IELTS_SPEAKING'
), new_rubric AS (
    INSERT INTO rubrics (id, code, task, rubric_version, status, source_note, system_prompt)
    SELECT '5d000000-0000-4000-8000-030000000005', 'IELTS_SPEAKING', 'SPEAKING', next_version.version, 'ACTIVE',
           'Original practice descriptors written for this project around the four public IELTS Speaking criteria, not official IELTS descriptors and not yet calibrated by an IELTS-qualified examiner. Version 5 is version 4 with a stricter fluency and coherence criterion: sentences that wander away from the question are counted one by one and cap the band.',
           previous.system_prompt
    FROM next_version
    CROSS JOIN rubrics previous
    WHERE previous.id = '5d000000-0000-4000-8000-030000000004'
    RETURNING id
), inserted AS (
    INSERT INTO rubric_criteria (rubric_id, code, name, description, sort_order)
    SELECT new_rubric.id, previous.code, previous.name,
           CASE WHEN previous.code = 'FC' THEN
               'How smoothly and continuously the candidate speaks, how clearly ideas are organised and developed, and how well sentences and ideas are linked. Check coherence sentence by sentence: read each answer one sentence at a time against the question or the topic card and ask whether the sentence serves it. Name every limit you find with a quote and a count, and list each place once only, never the same words twice under different wording: sentences with nothing to do with the question or topic (a digression, such as moving to food, hobbies, school or shopping when the question was about something else); sudden jumps to another topic; ideas that are stated but not developed (no reason, example or explanation); the same idea repeated without adding anything; ideas that contradict each other or do not follow from the one before; answers that stop after a few words; a Part 2 turn that stops early, leaves card points uncovered or has no ending; Part 3 answers that only restate the question; linking that leans on and, but, because and so, or on memorised markers (firstly, in conclusion, in my opinion) used mechanically or wrongly; unclear reference (it, this, they). Check fluency in the same way: long or frequent pauses (long_pauses and longest_pause_seconds in the speech measures) and whether they fall before ideas or before words; a slow or uneven pace (words_per_minute well below normal conversation, short mean_run_words); hidden hesitation, where the recogniser dropped fillers but the gaps remain; repeated words or phrases; false starts, abandoned sentences and self-corrections (count them); a fluent prepared opening followed by breakdown. Ceilings that apply whatever the fluency: three or more sentences that have nothing to do with the question or topic limit FC to 4.5; one or two such sentences limit it to 5.5; ideas that are stated without a reason or example in most answers limit it to 5.5. Do not count a sentence that adds a relevant detail, even a weak one.'
               ELSE previous.description END,
           previous.sort_order
    FROM new_rubric
    CROSS JOIN rubric_criteria previous
    WHERE previous.rubric_id = '5d000000-0000-4000-8000-030000000004'
    RETURNING id, code
), fc_bands(band, descriptor) AS (
    VALUES
        (4.0, 'Cannot keep going without frequent long pauses, heavy repetition and self-correction; most answers are one or two short sentences; links ideas only with and, but or so; ideas are often disconnected: three or more sentences have nothing to do with the question or topic, and the listener often loses the thread.'),
        (5.0, 'Keeps going, but only by leaning on repetition, self-correction, slow speech or long pauses before words; fluency breaks down when the language gets more demanding; links ideas with the same few simple words (and, but, because, so) again and again; answers are often short, with the reason or example missing or only half developed; at most one or two sentences wander away from the question or topic; Part 2 stops early or misses card points; Part 3 answers mostly restate the question. This is the usual band for a speaker who is understandable but limited.'),
        (6.0, 'Willing to give long answers in every part, but loses the thread, repeats ideas or restarts noticeably in several places in each answer; hesitation before words is still frequent; uses some linking words and discourse markers, but mechanically or wrongly at times; every sentence stays on the question or topic, though a few ideas are thin or weakly linked; most answers give a reason or example, though some stay thin; Part 2 covers the card, perhaps unevenly; Part 3 shows some explanation that is not sustained.'),
        (7.0, 'Speaks at length without noticeable effort in every part; hesitation is occasional and mostly about ideas, not words; repetition and self-correction are rare and do not break the flow; no sentence leaves the topic and each idea follows from the one before; a range of linking words and discourse markers is used flexibly and mostly correctly; answers are developed with reasons and examples; Part 3 reasoning is sustained and logical.')
)
INSERT INTO rubric_band_descriptors (criterion_id, band, descriptor)
SELECT inserted.id, previous_descriptor.band,
       COALESCE((SELECT fc_bands.descriptor FROM fc_bands WHERE inserted.code = 'FC' AND fc_bands.band = previous_descriptor.band),
                previous_descriptor.descriptor)
FROM inserted
JOIN rubric_criteria previous_criterion
  ON previous_criterion.rubric_id = '5d000000-0000-4000-8000-030000000004' AND previous_criterion.code = inserted.code
JOIN rubric_band_descriptors previous_descriptor ON previous_descriptor.criterion_id = previous_criterion.id;

INSERT INTO error_types (id, code, name, category, severity, knowledge_point_id, fix_hint, status, review_status, trap_knowledge_point_id, created_at, updated_at)
SELECT gen_random_uuid(), t.code, t.name, t.category, t.severity, kp.id, t.fix_hint, 'ACTIVE', 'APPROVED', NULL, now(), now()
  FROM (VALUES
    ('SPK_OFF_TOPIC_DIGRESSION', 'Câu hoặc ý không liên quan đến câu hỏi hay chủ đề', 'COHERENCE', 2, 'S60_FLUENCY_COHERENCE', 'Trước khi nói một câu, tự hỏi câu đó có giúp trả lời câu hỏi không; bỏ các câu ngoài lề (món ăn, sở thích, trường học, mua sắm) hoặc nối chúng về chủ đề bằng một lý do rõ ràng.'),
    ('SPK_REPEATED_IDEA',        'Lặp lại cùng một ý mà không thêm thông tin',         'COHERENCE', 1, 'S60_FLUENCY_COHERENCE', 'Mỗi câu nên thêm một thông tin mới như lý do, ví dụ hoặc chi tiết, thay vì nhắc lại ý cũ bằng từ khác.'),
    ('SPK_UNDEVELOPED_IDEA',     'Nêu ý nhưng chưa phát triển (thiếu lý do hoặc ví dụ)', 'COHERENCE', 2, 'S60_FLUENCY_COHERENCE', 'Sau mỗi ý chính, thêm "because ..." để nêu lý do hoặc "for example ..." để đưa ví dụ cụ thể.')
  ) AS t(code, name, category, severity, kp_code, fix_hint)
  JOIN knowledge_points kp ON kp.code = t.kp_code
 WHERE NOT EXISTS (SELECT 1 FROM error_types e WHERE e.code = t.code);

DO $$
BEGIN
    IF (SELECT count(*) FROM rubrics WHERE task = 'SPEAKING' AND status = 'ACTIVE') <> 1 THEN
        RAISE EXCEPTION 'Exactly one Speaking rubric must be active (is version 4 present?)';
    END IF;
    IF EXISTS (
        SELECT 1 FROM rubric_criteria c JOIN rubrics r ON r.id = c.rubric_id
        WHERE r.task = 'SPEAKING' AND r.status = 'ACTIVE'
        GROUP BY c.id HAVING (SELECT count(*) FROM rubric_band_descriptors d WHERE d.criterion_id = c.id) <> 19
    ) THEN
        RAISE EXCEPTION 'Every Speaking criterion needs a descriptor for each of the 19 bands';
    END IF;
    IF (SELECT count(*) FROM error_types WHERE status = 'ACTIVE'
         AND code IN ('SPK_OFF_TOPIC_DIGRESSION', 'SPK_REPEATED_IDEA', 'SPK_UNDEVELOPED_IDEA')) <> 3 THEN
        RAISE EXCEPTION 'The 3 coherence error codes must exist and be active';
    END IF;
END $$;
