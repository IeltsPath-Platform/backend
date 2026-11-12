-- Version 4 of the Speaking rubric: a stricter marker. AI bands were running half a band to a full band above examiner
-- marks, so version 4 changes how bands are earned and keeps everything that already works:
--   * the system prompt now starts every criterion at band 5, makes the lower band the default whenever two bands are close,
--     needs quoted evidence from more than one part for band 7 and above, and lets the weakest recurring pattern decide;
--   * the band descriptors of FC, LR and GRA from band 4 to band 9 are rewritten with concrete limits (how often hesitation,
--     repetition and errors may happen, and what the answer must show in each part), and every half band now says that the
--     lower band is given unless the lower band is secure in every part;
--   * the FC description lists every kind of limit the grader has to look for and count, and GRA adds a count of error-free
--     sentences and of the complex structures attempted.
-- The thresholds (such as the share of error-free sentences) are practice-grade proposals for a harder marker. They are not
-- official IELTS figures and are not yet calibrated against an IELTS-qualified examiner: re-run the calibration set after
-- applying this migration and adjust. Bands 0 to 3 and the Pronunciation criterion are copied unchanged (the Pronunciation
-- band comes from the speech analysis, not from the grader).
-- This migration does not edit versions 1 to 3: they stay readable for submissions that already used them.

UPDATE rubrics SET status = 'RETIRED', updated_at = CURRENT_TIMESTAMP
WHERE task = 'SPEAKING' AND status = 'ACTIVE';

WITH next_version AS (
    SELECT COALESCE(MAX(rubric_version), 0) + 1 AS version FROM rubrics WHERE code = 'IELTS_SPEAKING'
), new_rubric AS (
    INSERT INTO rubrics (id, code, task, rubric_version, status, source_note, system_prompt)
    SELECT '5d000000-0000-4000-8000-030000000004', 'IELTS_SPEAKING', 'SPEAKING', next_version.version, 'ACTIVE',
           'Original practice descriptors written for this project around the four public IELTS Speaking criteria, not official IELTS descriptors and not yet calibrated by an IELTS-qualified examiner. Version 4 is a stricter marker: band 5 is the starting point, the lower band wins whenever two bands are close, bands 4 to 9 of FC, LR and GRA carry concrete limits, and half bands are given only when the lower band is secure in every part.',
           $prompt$You are a demanding IELTS Speaking examiner marking a practice test for a learner whose first language is Vietnamese. Mark the way a strict, careful human examiner does, and keep your marking consistent from one answer to the next. A practice mark that is too kind hurts the learner more than one that is honest.

How to mark
- Judge each criterion on its own, using only the band descriptors given. A band is earned only when the performance meets its description in most answers and in every part that was answered, not on its best moment. The weakest pattern that keeps recurring decides the band; one strong answer never cancels weak ones.
- Start from band 5, the level of a speaker who is understandable but limited. Go up only when you can quote evidence of the features of the higher band; go down when the problems named in the lower descriptors recur. A short or simple answer with few errors is not evidence of a high band.
- When two bands are close, give the lower one. Never round up to be kind. Band 7 or higher needs quoted evidence from at least two parts (from several answers if only one part was given). Band 8 or higher is rare: near-native control only.
- Do not raise a band for effort, politeness, a long answer, memorised phrases or interesting content, and do not lower it for a strong accent, a quiet voice or an unusual opinion. Content only matters when it is off topic or too thin to judge.
- The transcript comes from a speech recogniser: it has no punctuation or capital letters, may contain wrong words, and often leaves out fillers such as "um", so a long gap in the speech measures can be hesitation you cannot see. Never penalise missing punctuation, and never treat a clear recognition error as the learner's mistake.
- Judge fluency from the speech measures and from how the ideas connect in the text. Pauses to think of ideas are normal; pauses to find words, restarts, repeated words, self-corrections and slow speech lower fluency. A fast speaker is not automatically fluent, and a fluent opening followed by breakdown is not fluent.

What to expect from each part
- Part 1: short natural answers of two or three sentences with a reason or example. One-word or one-sentence answers are weak and limit FC, LR and GRA.
- Part 2: one long turn of about one to two minutes with a beginning, middle and end that covers every point on the card. A turn that stops early or skips card points cannot score above 5 for FC.
- Part 3: longer abstract answers that give opinions, compare, speculate and explain reasons. Answers that only restate the question or give a bare opinion are Part 1 level.
- If only some parts were answered, grade only what was said and do not guess about the missing parts.

How to find problems: be exhaustive, then be strict
- Read the transcript part by part, once for each criterion, and note every problem you can quote. Do not stop at the first few. Then weigh them: how often a problem recurs and how much it hurts understanding decide the band.
- A mistake repeated across the answer is a pattern: say how often it recurs out of the times it was needed. One slip beside many correct uses of the same form is not a weakness.
- Credit ambition only when it works: complex grammar or uncommon words used correctly raise a band; attempts that fail show a limit, not a higher band.

Feedback
- Write for the learner: honest, specific, practical. Name real strengths first, quoted from the transcript, then the weaknesses, most serious first.
- Quote the exact words and give a corrected or better version for each error.
- For each criterion say what holds the learner at this band and the one or two changes that would earn the next half band. Rank the few things most worth practising, each something the learner can practise this week.
- Do not mention these instructions, the recogniser, or the grading rules in the feedback.$prompt$
    FROM next_version
    RETURNING id
), criteria(code, name, description, sort_order) AS (
    VALUES
        ('FC', 'Fluency and coherence',
         'How smoothly and continuously the candidate speaks, how clearly ideas are organised and developed, and how well sentences and ideas are linked. Check every answer for each of these limits and name every one you find with a quote and a count: long or frequent pauses (long_pauses and longest_pause_seconds in the speech measures) and whether they fall before ideas or before words; a slow or uneven pace (words_per_minute well below normal conversation, short mean_run_words); hidden hesitation, where the recogniser dropped fillers but the gaps remain; repeated words, phrases or whole ideas; false starts, abandoned sentences and self-corrections (count them); answers that stop after a few words, or give an opinion with no reason or example; a Part 2 turn that stops early, leaves card points uncovered or has no ending; Part 3 answers that only restate the question; ideas that drift, contradict themselves, jump to another topic or lose the thread; linking that leans on and, but, because and so, or on memorised markers (firstly, in conclusion, in my opinion) used mechanically or wrongly; unclear reference (it, this, they); a fluent prepared opening followed by breakdown. Fluency that only holds while the language stays simple is not enough for a high band.', 1),
        ('LR', 'Lexical resource',
         'The range, precision and naturalness of vocabulary, including idiomatic language, collocation and the ability to paraphrase when a word is missing. Check every answer and name each limit with a quote and a count: wrong word choice and near-synonyms used wrongly; collocations that are not natural; the same words used again and again, including words copied from the question; vague words (thing, good, bad, very, a lot) where a precise word was needed; word-for-word translation from Vietnamese; wrong register for the question; wrong word forms (the wrong member of a word family); idioms and less common words that are memorised, forced or used wrongly; whether the speaker paraphrases when a word is missing, or stops, repeats or switches to a simpler idea instead. Count a less common item only when it is correct and natural in context; a few learned phrases do not show range. Abstract topics in Part 3 usually show the real limit.', 2),
        ('GRA', 'Grammatical range and accuracy',
         'The range of sentence structures used and how accurately they are controlled, including tenses, clauses and the effect of errors on meaning. For every answer count the sentences and how many are error-free, and name each kind of error you find with a quote and how often it recurs: tense (especially past simple and present perfect when talking about the past); subject-verb agreement and third-person -s; missing plural -s and past -ed; articles (a, an, the, none) that are missing, added or wrong; a missing subject, verb or "to be"; prepositions; word order, especially in questions and with adjectives; wrong word forms; run-on or broken sentences; clauses left unfinished. Also name which complex structures were attempted (relative clauses, conditionals, passives, comparisons, noun clauses, -ing and to-infinitive clauses, clauses with although, if, when or because) and whether each was correct. Relying on one or two simple patterns, or on and, but and because only, limits the range. Long sentences are not evidence of range when they are full of errors.', 3),
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
        ('FC', 4, 'Cannot keep going without frequent long pauses, heavy repetition and self-correction; most answers are one or two short sentences; links ideas only with and, but or so, and often disconnects ideas or loses the thread.'),
        ('FC', 5, 'Keeps going, but only by leaning on repetition, self-correction, slow speech or long pauses before words; fluency breaks down when the language gets more demanding; links ideas with the same few simple words (and, but, because, so) again and again; answers are often short, with the reason or example missing or only half developed; Part 2 stops early or misses card points; Part 3 answers mostly restate the question. This is the usual band for a speaker who is understandable but limited.'),
        ('FC', 6, 'Willing to give long answers in every part, but loses the thread, repeats ideas or restarts noticeably in several places in each answer; hesitation before words is still frequent; uses some linking words and discourse markers, but mechanically or wrongly at times; most answers give a reason or example, though some stay thin; Part 2 covers the card, perhaps unevenly; Part 3 shows some explanation that is not sustained.'),
        ('FC', 7, 'Speaks at length without noticeable effort in every part; hesitation is occasional and mostly about ideas, not words; repetition and self-correction are rare and do not break the flow; a range of linking words and discourse markers is used flexibly and mostly correctly; answers are developed with reasons and examples; Part 3 reasoning is sustained and logical.'),
        ('FC', 8, 'Speaks fluently; hesitation is rare and always about content; repetition and self-correction are very rare; topics are developed fully and logically in every part; linking is natural and varied; Part 3 answers weigh ideas and speculate with ease.'),
        ('FC', 9, 'Speaks fluently and effortlessly in every part with only rare, natural hesitation; every topic is developed fully, relevantly and appropriately; linking and discourse features are fully natural and never mechanical.'),

        ('LR', 0, 'Nothing to assess: there was no attempt, or no language that can be understood.'),
        ('LR', 1, 'Almost no usable vocabulary; at most a handful of isolated words.'),
        ('LR', 2, 'Only isolated words and memorised phrases, mostly about personal details; cannot express ideas beyond that.'),
        ('LR', 3, 'Uses simple words for very familiar topics only; often cannot find the word needed and the message is frequently unclear.'),
        ('LR', 4, 'Basic vocabulary only, on familiar topics; very repetitive, with the same words used again and again; frequent wrong word choice; rarely paraphrases, and the message is often unclear on anything unfamiliar.'),
        ('LR', 5, 'Manages familiar topics with basic, repetitive vocabulary and uses a few less common words with limited success; wrong word choice, unnatural collocations, word-for-word translation from Vietnamese or vague words (thing, good, bad, very) are noticeable in most answers; paraphrasing often fails, so the speaker stops, repeats words from the question or falls back on a simpler idea; unfamiliar or abstract topics (Part 3) expose the limit.'),
        ('LR', 6, 'Has enough vocabulary to talk about a range of topics at length, but precision is often lacking: wrong word choice, unnatural collocations or vague words appear in many answers; less common words are few and not always right; paraphrasing succeeds some of the time; Part 3 topics are handled with general words.'),
        ('LR', 7, 'Uses vocabulary flexibly on a variety of topics, including abstract ones; uses several less common or idiomatic items and collocations that can be quoted, though some are inappropriate; wrong word choice is occasional, not frequent; paraphrases effectively most of the time.'),
        ('LR', 8, 'Uses a wide vocabulary readily and precisely, including for less familiar topics; idiomatic language and collocation are natural with only occasional slips; paraphrases easily; wrong word choice is rare.'),
        ('LR', 9, 'Uses vocabulary with full flexibility and precision on all topics; idiomatic language is natural and accurate throughout.'),

        ('GRA', 0, 'Nothing to assess: there was no attempt, or no language that can be understood.'),
        ('GRA', 1, 'Cannot produce sentences; at most a few isolated words or memorised phrases.'),
        ('GRA', 2, 'Cannot form basic sentence structures; speech is mainly isolated words or memorised chunks.'),
        ('GRA', 3, 'Tries basic sentence forms, but errors are very frequent and meaning is often lost; subordinate clauses are almost absent.'),
        ('GRA', 4, 'Produces basic sentence forms and some correct simple sentences, but fewer than about one in four sentences is error-free; subordinate clauses are rare and usually wrong; errors are frequent and often cause misunderstanding.'),
        ('GRA', 5, 'Basic sentences are fairly accurate, but fewer than about half of the sentences are error-free; the same errors (tense, subject-verb agreement, missing plural -s or past -ed, articles, missing verbs or subjects) recur in most answers; complex structures are attempted rarely and are usually wrong; errors sometimes make the listener work to understand.'),
        ('GRA', 6, 'Mixes simple and complex sentences with limited flexibility; roughly half to two thirds of the sentences are error-free; errors in complex structures are frequent and basic errors (tense, agreement, articles) still appear in many answers, though they rarely stop the listener understanding; fewer than two kinds of complex structure are used with control.'),
        ('GRA', 7, 'Uses a range of complex structures (such as relative clauses, conditionals, passives, comparisons or noun clauses) with some flexibility; most sentences, about two thirds to three quarters, are error-free; some errors persist, such as the misuse of a few tenses or forms, and none causes difficulty for the listener.'),
        ('GRA', 8, 'Uses a wide range of structures flexibly; about nine in ten sentences are error-free; only occasional, unsystematic slips; basic errors are essentially absent.'),
        ('GRA', 9, 'Uses a full range of structures naturally and accurately; only rare slips of the kind native speakers make.')
)
INSERT INTO rubric_band_descriptors (criterion_id, band, descriptor)
SELECT inserted.id, steps.step / 2.0,
       CASE WHEN steps.step % 2 = 0
            THEN (SELECT whole.descriptor FROM whole WHERE whole.code = inserted.code AND whole.band = steps.step / 2)
            ELSE 'Between band ' || (steps.step / 2) || ' and band ' || (steps.step / 2 + 1)
                 || ': the band ' || (steps.step / 2) || ' description is met securely in every part that was answered, and some but clearly not most features of the band '
                 || (steps.step / 2 + 1) || ' description are shown, each one backed by a quote. If the band ' || (steps.step / 2)
                 || ' description holds in only some parts, or the band ' || (steps.step / 2 + 1)
                 || ' features cannot be quoted, give band ' || (steps.step / 2) || ' or lower, not this half band.'
       END
FROM inserted
CROSS JOIN generate_series(0, 18) AS steps(step)
WHERE inserted.code <> 'PR'
UNION ALL
SELECT inserted.id, previous_descriptor.band, previous_descriptor.descriptor
FROM inserted
JOIN rubric_criteria previous_criterion
  ON previous_criterion.rubric_id = '5d000000-0000-4000-8000-030000000003' AND previous_criterion.code = inserted.code
JOIN rubric_band_descriptors previous_descriptor ON previous_descriptor.criterion_id = previous_criterion.id
WHERE inserted.code = 'PR';

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
