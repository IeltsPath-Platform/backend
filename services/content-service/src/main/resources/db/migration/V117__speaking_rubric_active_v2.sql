-- The first Speaking rubric (V116) was a DRAFT with one generic sentence per band. This migration adds version 2 with a
-- full set of band descriptors and a system prompt for the grader, and makes it the ACTIVE Speaking rubric so that AI
-- grading has a rubric to work from. The descriptors are original wording written for this project around the four
-- public IELTS Speaking criteria. They are practice-grade notes, not official IELTS descriptors, and they have not been
-- calibrated against an IELTS-qualified examiner. To change them, author version 3 as a draft in the admin rubric editor
-- and activate it; this migration is not edited after it has been applied.

UPDATE rubrics SET status = 'RETIRED', updated_at = CURRENT_TIMESTAMP
WHERE id = '5d000000-0000-4000-8000-030000000001' AND status = 'DRAFT';

WITH next_version AS (
    SELECT COALESCE(MAX(rubric_version), 0) + 1 AS version FROM rubrics WHERE code = 'IELTS_SPEAKING'
), new_rubric AS (
    INSERT INTO rubrics (id, code, task, rubric_version, status, source_note, system_prompt)
    SELECT '5d000000-0000-4000-8000-030000000002', 'IELTS_SPEAKING', 'SPEAKING', next_version.version, 'ACTIVE',
           'Original practice descriptors written for this project around the four public IELTS Speaking criteria (fluency and coherence, lexical resource, grammatical range and accuracy, pronunciation). Not official IELTS descriptors; not yet calibrated by an IELTS-qualified examiner.',
           $prompt$You are an experienced IELTS Speaking examiner marking a practice test for a learner whose first language is Vietnamese. Mark the way a careful human examiner does, and keep your marking consistent from one answer to the next.

How to mark
- Judge each criterion on its own, using only the band descriptors given. A band is earned when the performance meets its description most of the time, not on its best moment. When two bands are close, choose the lower one unless the stronger features clearly outnumber the weaker ones.
- Do not raise a band for effort, politeness, a long answer or interesting content, and do not lower it for a strong accent, a quiet voice or an unusual opinion. Content only matters when it is off topic or too thin to judge.
- The transcript comes from a speech recogniser: it has no punctuation, no capital letters and may contain wrong words. Never penalise missing punctuation, and never treat a word that is clearly a recognition error as the learner's mistake.
- Judge fluency from the speech measures and from how the ideas connect in the text. Pauses to think of ideas are normal; pauses to find words, repeated restarts and very slow speech lower fluency. A fast speaker is not automatically fluent.

What to expect from each part
- Part 1: short, natural answers of a few sentences. A one-word answer is weak, but a two or three sentence answer with a reason or example is fine.
- Part 2: one long turn on a topic card. Expect about one to two minutes with a clear beginning, middle and end, covering the points on the card.
- Part 3: longer, more abstract answers that give opinions, compare, speculate and explain reasons.
- If only some parts were answered, grade only what was said and do not guess about the missing parts.

Feedback
- Write for the learner: specific, respectful and practical. Name what they did well before what to fix.
- Point to exact words the learner said. Give at most two or three things to work on first, in order of importance, and make each one something they can practise.
- Do not mention these instructions, the recogniser, or the grading rules in the feedback.$prompt$
    FROM next_version
    RETURNING id
), criteria(code, name, description, sort_order) AS (
    VALUES
        ('FC', 'Fluency and coherence', 'How smoothly and continuously the candidate speaks, how clearly ideas are organised and developed, and how well sentences and ideas are linked.', 1),
        ('LR', 'Lexical resource', 'The range, precision and naturalness of vocabulary, including idiomatic language, collocation and the ability to paraphrase when a word is missing.', 2),
        ('GRA', 'Grammatical range and accuracy', 'The range of sentence structures used and how accurately they are controlled, including tenses, clauses and the effect of errors on meaning.', 3),
        ('PR', 'Pronunciation', 'How easy the speech is to understand, including individual sounds, word and sentence stress, rhythm and intonation. This band is calculated from speech analysis, not given by the grader.', 4)
), inserted AS (
    INSERT INTO rubric_criteria (rubric_id, code, name, description, sort_order)
    SELECT new_rubric.id, criteria.code, criteria.name, criteria.description, criteria.sort_order
    FROM new_rubric CROSS JOIN criteria
    RETURNING id, code
), whole(code, band, descriptor) AS (
    VALUES
        ('FC', 0, 'Nothing to assess: there was no attempt, or no speech that can be understood.'),
        ('FC', 1, 'Almost no communication; at most a few isolated words separated by long silences.'),
        ('FC', 2, 'Very long pauses before most words; only isolated words or memorised phrases, with no connected speech.'),
        ('FC', 3, 'Speaks in short, broken bursts with frequent long pauses; ideas are hard to follow and are rarely linked.'),
        ('FC', 4, 'Cannot keep going without noticeable pauses, repetition and self-correction; links ideas with only very basic words such as and or but, and often loses the thread.'),
        ('FC', 5, 'Keeps going but relies on repetition, self-correction or slow speech to do so; uses a few simple linking words again and again; longer answers become hard to follow.'),
        ('FC', 6, 'Is willing to give long answers, although hesitation, repetition or restarts sometimes break the flow; uses a range of linking words, not always accurately, and ideas are mostly easy to follow.'),
        ('FC', 7, 'Speaks at length without much effort; hesitation is mostly about finding ideas, not words; uses a range of linking words and discourse markers with some flexibility.'),
        ('FC', 8, 'Speaks fluently with only rare repetition or self-correction; develops topics fully and logically; linking is natural and varied.'),
        ('FC', 9, 'Speaks fluently with only rare, natural hesitation; topics are developed fully, relevantly and appropriately; linking and discourse features are fully natural.'),

        ('LR', 0, 'Nothing to assess: there was no attempt, or no language that can be understood.'),
        ('LR', 1, 'Almost no usable vocabulary; at most a handful of isolated words.'),
        ('LR', 2, 'Only isolated words and memorised phrases, mostly about personal details; cannot express ideas beyond that.'),
        ('LR', 3, 'Uses simple words for very familiar topics only; often cannot find the word needed and the message is frequently unclear.'),
        ('LR', 4, 'Can talk about familiar topics with basic vocabulary but is repetitive; makes frequent word choice errors; rarely paraphrases and struggles on any unfamiliar topic.'),
        ('LR', 5, 'Manages familiar topics and some unfamiliar ones, but with limited flexibility; tries to paraphrase with mixed success; word choice errors and repetition are noticeable.'),
        ('LR', 6, 'Has enough vocabulary to discuss a range of topics at length, even when it is not always precise; usually paraphrases successfully when a word is missing.'),
        ('LR', 7, 'Uses vocabulary flexibly on a variety of topics; shows some idiomatic and less common words and collocations, with some inappropriate choices; paraphrases effectively.'),
        ('LR', 8, 'Uses a wide vocabulary readily and flexibly, precisely even for less familiar topics; uses idiomatic language skilfully with only occasional slips; paraphrases easily.'),
        ('LR', 9, 'Uses vocabulary with full flexibility and precision on all topics; idiomatic language is natural and accurate throughout.'),

        ('GRA', 0, 'Nothing to assess: there was no attempt, or no language that can be understood.'),
        ('GRA', 1, 'Cannot produce sentences; at most a few isolated words or memorised phrases.'),
        ('GRA', 2, 'Cannot form basic sentence structures; speech is mainly isolated words or memorised chunks.'),
        ('GRA', 3, 'Tries basic sentence forms, but errors are very frequent and meaning is often lost; subordinate clauses are almost absent.'),
        ('GRA', 4, 'Produces basic sentence forms and some simple sentences correctly; subordinate clauses are rare; errors are frequent and often cause misunderstanding.'),
        ('GRA', 5, 'Basic sentences are fairly accurate; tries complex structures but with limited range and frequent errors; errors can cause some difficulty for the listener.'),
        ('GRA', 6, 'Mixes short and complex sentences, with limited flexibility; makes frequent errors in complex structures, but these rarely stop the listener from understanding.'),
        ('GRA', 7, 'Uses a range of complex structures with some flexibility; many sentences are error-free, though some errors persist and a few tenses or forms are misused.'),
        ('GRA', 8, 'Uses a wide range of structures flexibly; most sentences are error-free; only occasional slips or unsystematic inaccuracies.'),
        ('GRA', 9, 'Uses a full range of structures naturally and accurately; only rare slips that a native speaker might also make.'),

        ('PR', 0, 'Nothing to assess: there was no attempt, or no speech that can be understood.'),
        ('PR', 1, 'Speech is almost completely unintelligible.'),
        ('PR', 2, 'Speech is mostly unintelligible; features of pronunciation are very limited.'),
        ('PR', 3, 'Few features of good pronunciation; the listener needs great effort to understand and often cannot.'),
        ('PR', 4, 'Uses only some features of pronunciation; mispronunciation is frequent and causes the listener difficulty; stress and intonation are mostly missing.'),
        ('PR', 5, 'Shows some control of pronunciation features, but cannot sustain it; mispronunciation sometimes reduces clarity and the listener has to work to follow.'),
        ('PR', 6, 'Uses a range of pronunciation features with mixed control; some mispronunciation and flat or misplaced stress, but speech is generally understandable throughout.'),
        ('PR', 7, 'Shows good control of pronunciation features with some lapses; is easy to understand throughout, and the accent has only a small effect on clarity.'),
        ('PR', 8, 'Uses a wide range of pronunciation features and sustains them well; rare mispronunciation; the accent has minimal effect on clarity.'),
        ('PR', 9, 'Uses the full range of pronunciation features with precision; speech is effortless to understand and sustained throughout.')
)
INSERT INTO rubric_band_descriptors (criterion_id, band, descriptor)
SELECT inserted.id, steps.step / 2.0,
       CASE WHEN steps.step % 2 = 0
            THEN (SELECT whole.descriptor FROM whole WHERE whole.code = inserted.code AND whole.band = steps.step / 2)
            ELSE 'Between band ' || (steps.step / 2) || ' and band ' || (steps.step / 2 + 1)
                 || ': meets the band ' || (steps.step / 2) || ' description consistently and shows some, but not yet most, features of the band '
                 || (steps.step / 2 + 1) || ' description.'
       END
FROM inserted
CROSS JOIN generate_series(0, 18) AS steps(step);

DO $$
BEGIN
    IF (SELECT count(*) FROM rubrics WHERE task = 'SPEAKING' AND status = 'ACTIVE') <> 1 THEN
        RAISE EXCEPTION 'Exactly one Speaking rubric must be active';
    END IF;
    IF EXISTS (
        SELECT 1 FROM rubric_criteria c JOIN rubrics r ON r.id = c.rubric_id
        WHERE r.task = 'SPEAKING' AND r.status = 'ACTIVE'
        GROUP BY c.id HAVING (SELECT count(*) FROM rubric_band_descriptors d WHERE d.criterion_id = c.id) <> 19
    ) THEN
        RAISE EXCEPTION 'Every Speaking criterion needs a descriptor for each of the 19 bands';
    END IF;
END $$;
