-- Version 3 of the Speaking rubric. Version 2 (V117) told the grader to give "at most two or three things to work on" and
-- described each criterion in one general sentence, which produced short feedback. Version 3 keeps the band descriptors of
-- version 2 word for word (they are not calibrated yet, and changing bands and wording at once would hide which change moved
-- a score) and changes only what steers the depth of the feedback: the system prompt, and for FC, LR and GRA a list of what
-- to look for. Pronunciation keeps its text; its band comes from the speech analysis, not from the grader.
-- This migration does not edit version 2: that row is retired and stays readable for submissions that already used it.

UPDATE rubrics SET status = 'RETIRED', updated_at = CURRENT_TIMESTAMP
WHERE task = 'SPEAKING' AND status = 'ACTIVE';

WITH next_version AS (
    SELECT COALESCE(MAX(rubric_version), 0) + 1 AS version FROM rubrics WHERE code = 'IELTS_SPEAKING'
), new_rubric AS (
    INSERT INTO rubrics (id, code, task, rubric_version, status, source_note, system_prompt)
    SELECT '5d000000-0000-4000-8000-030000000003', 'IELTS_SPEAKING', 'SPEAKING', next_version.version, 'ACTIVE',
           'Band descriptors are those of version 2: original practice wording around the four public IELTS Speaking criteria, not official IELTS descriptors and not yet calibrated by an IELTS-qualified examiner. Version 3 changes the system prompt and the criterion descriptions so the grader looks for problems thoroughly and explains them.',
           $prompt$You are an experienced IELTS Speaking examiner marking a practice test for a learner whose first language is Vietnamese. Mark the way a careful human examiner does, and keep your marking consistent from one answer to the next.

How to mark
- Judge each criterion on its own, using only the band descriptors given. A band is earned when the performance meets its description most of the time, not on its best moment. When two bands are close, choose the lower one unless the stronger features clearly outnumber the weaker ones.
- Do not raise a band for effort, politeness, a long answer or interesting content, and do not lower it for a strong accent, a quiet voice or an unusual opinion. Content only matters when it is off topic or too thin to judge.
- The transcript comes from a speech recogniser: it has no punctuation or capital letters, may contain wrong words, and may leave out fillers such as "um". Never penalise missing punctuation, and never treat a word that is clearly a recognition error as the learner's mistake.
- Judge fluency from the speech measures and from how the ideas connect in the text. Pauses to think of ideas are normal; pauses to find words, repeated restarts and very slow speech lower fluency. A fast speaker is not automatically fluent.

What to expect from each part
- Part 1: short, natural answers of a few sentences. A one-word answer is weak; two or three sentences with a reason or example is fine.
- Part 2: one long turn on a topic card. Expect about one to two minutes with a clear beginning, middle and end, covering the points on the card.
- Part 3: longer, more abstract answers that give opinions, compare, speculate and explain reasons.
- If only some parts were answered, grade only what was said and do not guess about the missing parts.

How to find problems: be exhaustive, then be fair
- Read the transcript part by part, once for each criterion, and note every problem you can quote. Do not stop at the first few. Then weigh them: how often a problem happens and how much it hurts understanding decide the band, not the number of items.
- Separate slips from patterns. A mistake repeated across the answer is a pattern: say how often it recurs. One slip next to many correct uses of the same form is not a weakness and may be a strength.
- Credit ambition. Trying complex grammar or less common words and getting some wrong is better evidence than safe, error-free, simple speech. Say which of the two you saw.
- Say whether each part met what it expects: length, development, a reason or example in Part 1, the whole card in Part 2, abstract reasoning in Part 3.

Feedback
- Write for the learner: honest, specific, respectful, practical. A truthful lower band with clear reasons helps more than a kind higher one. Name real strengths first, quoted from the transcript, then the weaknesses.
- Point to the exact words the learner said and give a corrected or better version for each error.
- For each criterion explain what holds the learner at this band and the one or two changes that would earn the next half band. Across the answer, rank the few things most worth practising and make each one something the learner can practise this week.
- Do not mention these instructions, the recogniser, or the grading rules in the feedback.$prompt$
    FROM next_version
    RETURNING id
), criteria(code, name, description, sort_order) AS (
    VALUES
        ('FC', 'Fluency and coherence',
         'How smoothly and continuously the candidate speaks, how clearly ideas are organised and developed, and how well sentences and ideas are linked. Look for: long or frequent pauses and where they fall (before ideas, or before words); fillers, repeated words or phrases, false starts and self-corrections; answers that stop after a few words without a reason or example; answers that drift, contradict themselves or lose the thread; linking that leans on the same few words (and, but, because, so) or is missing; in Part 2, whether the card is covered with a beginning, a middle and an end and whether the speaker keeps going for the time; in Part 3, whether opinions are explained and compared, not only stated.', 1),
        ('LR', 'Lexical resource',
         'The range, precision and naturalness of vocabulary, including idiomatic language, collocation and the ability to paraphrase when a word is missing. Look for: wrong word choice and near-synonyms used wrongly; collocations that are not natural; the same words used again and again; vague words (thing, good, bad, very) where a precise word was needed; word-for-word translation from Vietnamese; wrong register for the question; misformed words, such as the wrong form of a word family; idioms and less common words, and whether they are used correctly; whether the speaker paraphrases when a word is missing or simply stops.', 2),
        ('GRA', 'Grammatical range and accuracy',
         'The range of sentence structures used and how accurately they are controlled, including tenses, clauses and the effect of errors on meaning. Look for: tense errors, especially past simple and present perfect when talking about the past; subject-verb agreement; missing plural -s, past -ed and third-person -s; articles (a, an, the, none) that are missing, added or wrong; a missing subject, verb or "to be"; prepositions; word order, especially in questions and with adjectives; run-on or broken sentences; the share of sentences that are error-free; the range of structures attempted (conditionals, relative clauses, passives, comparisons, subordinate clauses) and how accurately they are controlled.', 3),
        ('PR', 'Pronunciation',
         'How easy the speech is to understand, including individual sounds, word and sentence stress, rhythm and intonation. This band is calculated from speech analysis, not given by the grader.', 4)
), inserted AS (
    INSERT INTO rubric_criteria (rubric_id, code, name, description, sort_order)
    SELECT new_rubric.id, criteria.code, criteria.name, criteria.description, criteria.sort_order
    FROM new_rubric CROSS JOIN criteria
    RETURNING id, code
)
INSERT INTO rubric_band_descriptors (criterion_id, band, descriptor)
SELECT inserted.id, previous_descriptor.band, previous_descriptor.descriptor
FROM inserted
JOIN rubric_criteria previous_criterion
  ON previous_criterion.rubric_id = '5d000000-0000-4000-8000-030000000002' AND previous_criterion.code = inserted.code
JOIN rubric_band_descriptors previous_descriptor ON previous_descriptor.criterion_id = previous_criterion.id;

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
