-- Version 6 of the Speaking rubric: every criterion is marked in whole bands only (0 to 9), the way the public IELTS Speaking
-- band descriptors are written. Versions 1 to 5 carried 19 descriptors per criterion: ten whole bands and nine generated "between
-- band N and band N+1" descriptors that are not in the IELTS descriptors. Version 6 drops the generated ones.
--   * FC, LR, GRA and PR get one descriptor for each of the whole bands 0 to 9, adapted from the public IELTS Speaking band
--     descriptors (the wording is a close paraphrase, not a copy). For bands 4 to 9 of FC, LR and GRA the counted limits that
--     versions 4 and 5 added (how often hesitation, wrong words and errors may occur, how many sentences may wander) stay in
--     the descriptor after "In this practice test:", because without them the grader marked too kind;
--   * the Fluency ceilings of version 5 were written for half bands (4.5 and 5.5); as whole bands they become: three or more
--     sentences unrelated to the question or topic limit FC to band 4, one or two such sentences limit it to band 5, and ideas
--     stated without a reason or example in most answers limit it to band 5 (the lower band, as version 4 asks when two bands
--     are close). These are practice-grade proposals for a harder marker, not official IELTS figures, and need re-checking
--     against the examiner calibration set;
--   * the system prompt is version 5 with one added sentence (give a whole band, never 5.5 or 6.5) and "the next half band"
--     changed to "the next band".
-- The criteria names, the FC/LR/GRA descriptions apart from those ceilings, the error codes and the Pronunciation criterion
-- description are copied from version 5. The Pronunciation band still comes from the speech analysis, not from the grader.
-- This migration does not edit versions 1 to 5: they stay readable for submissions that already used them.

UPDATE rubrics SET status = 'RETIRED', updated_at = CURRENT_TIMESTAMP
WHERE task = 'SPEAKING' AND status = 'ACTIVE';

WITH next_version AS (
    SELECT COALESCE(MAX(rubric_version), 0) + 1 AS version FROM rubrics WHERE code = 'IELTS_SPEAKING'
), new_rubric AS (
    INSERT INTO rubrics (id, code, task, rubric_version, status, source_note, system_prompt)
    SELECT '5d000000-0000-4000-8000-030000000006', 'IELTS_SPEAKING', 'SPEAKING', next_version.version, 'ACTIVE',
           'Band descriptors adapted from the public IELTS Speaking band descriptors (whole bands 0 to 9 only), with the counted limits of version 5 kept for bands 4 to 9 of FC, LR and GRA and the Fluency ceilings restated as whole bands. Not official IELTS descriptors and not yet calibrated by an IELTS-qualified examiner.',
           replace(replace(previous.system_prompt,
                           'When two bands are close, give the lower one.',
                           'Give every criterion a whole band from 0 to 9 (5, 6, 7 and so on), never 5.5 or 6.5. When two bands are close, give the lower one.'),
                   'earn the next half band', 'earn the next band')
    FROM next_version
    CROSS JOIN rubrics previous
    WHERE previous.id = '5d000000-0000-4000-8000-030000000005'
    RETURNING id
), inserted AS (
    INSERT INTO rubric_criteria (rubric_id, code, name, description, sort_order)
    SELECT new_rubric.id, previous.code, previous.name,
           CASE WHEN previous.code = 'FC' THEN
               replace(replace(replace(previous.description,
                   'limit FC to 4.5', 'limit FC to band 4'),
                   'limit it to 5.5; ideas', 'limit it to band 5; ideas'),
                   'in most answers limit it to 5.5', 'in most answers limit it to band 5')
               ELSE previous.description END,
           previous.sort_order
    FROM new_rubric
    CROSS JOIN rubric_criteria previous
    WHERE previous.rubric_id = '5d000000-0000-4000-8000-030000000005'
    RETURNING id, code
), bands(code, band, descriptor) AS (
    VALUES
        ('FC', 0, 'Does not attend or does not complete the test: nothing to assess.'),
        ('FC', 1, 'Essentially no fluency; speech is totally incoherent.'),
        ('FC', 2, 'Lengthy pauses before nearly every word; isolated words may be recognisable, but the speech has virtually no communicative value.'),
        ('FC', 3, 'Frequent, sometimes long, pauses while searching for words; limited ability to link simple sentences or to go beyond simple answers to questions; frequently unable to convey a basic message.'),
        ('FC', 4, 'Cannot keep going without noticeable pauses; speech may be slow, with frequent repetition and self-correction; links simple sentences but often with repetitive connectives; some breakdowns in coherence. In this practice test: most answers are one or two short sentences, and three or more sentences have nothing to do with the question or topic, so the listener often loses the thread.'),
        ('FC', 5, 'Usually keeps going, but relies on repetition, self-correction and/or slow speech to do so; hesitation is often a mid-sentence search for fairly basic words or grammar; overuses certain discourse markers and connectives (and, but, because, so); more complex speech usually causes disfluency, while simpler language may be fluent. In this practice test: answers are often short, with the reason or example missing or only half developed; at most one or two sentences wander away from the question or topic; Part 2 stops early or misses card points; Part 3 answers mostly restate the question. This is the usual band for a speaker who is understandable but limited.'),
        ('FC', 6, 'Keeps going and is willing to produce long turns, but coherence may be lost at times through hesitation, repetition and/or self-correction; uses a range of discourse markers and connectives, though not always appropriately. In this practice test: loses the thread, repeats ideas or restarts noticeably in several places in each answer; hesitation before words is still frequent; every sentence stays on the question or topic, though a few ideas are thin or weakly linked; most answers give a reason or example, though some stay thin; Part 2 covers the card, perhaps unevenly; Part 3 shows some explanation that is not sustained.'),
        ('FC', 7, 'Keeps going and produces long turns readily, without noticeable effort; some hesitation, repetition and/or self-correction, often mid-sentence, shows trouble finding language but does not affect coherence; uses discourse markers, connectives and cohesive features flexibly. In this practice test: hesitation is occasional and mostly about ideas, not words; no sentence leaves the topic and each idea follows from the one before; answers are developed with reasons and examples in every part; Part 3 reasoning is sustained and logical.'),
        ('FC', 8, 'Fluent, with only very occasional repetition or self-correction; hesitation may occasionally be used to find words or grammar, but most of it is about content; topic development is coherent, appropriate and relevant. In this practice test: linking is natural and varied, and Part 3 answers weigh ideas and speculate with ease.'),
        ('FC', 9, 'Fluent, with only very occasional repetition or self-correction; any hesitation is used only to prepare the content of the next utterance, not to find words or grammar; speech is situationally appropriate, cohesive features are fully acceptable and topic development is fully coherent and appropriately extended.'),

        ('LR', 0, 'Does not attend or does not complete the test: nothing to assess.'),
        ('LR', 1, 'No resource apart from a few isolated words; no communication is possible.'),
        ('LR', 2, 'Very limited resource: utterances consist of isolated words or memorised utterances; little communication is possible without the support of mime or gesture.'),
        ('LR', 3, 'Resource limited to simple vocabulary used mainly to give personal information; vocabulary is inadequate for unfamiliar topics.'),
        ('LR', 4, 'Resource is sufficient for familiar topics, but only basic meaning can be conveyed on unfamiliar topics; frequent inappropriacies and errors in word choice; rarely attempts to paraphrase. In this practice test: the vocabulary is very repetitive, with the same words used again and again, and the message is often unclear on anything unfamiliar.'),
        ('LR', 5, 'Resource is sufficient to discuss familiar and unfamiliar topics, but with limited flexibility; attempts to paraphrase, but not always successfully. In this practice test: wrong word choice, unnatural collocations, word-for-word translation from Vietnamese or vague words (thing, good, bad, very) are noticeable in most answers; when paraphrasing fails the speaker stops, repeats words from the question or falls back on a simpler idea; unfamiliar or abstract topics (Part 3) expose the limit.'),
        ('LR', 6, 'Resource is sufficient to discuss topics at length; vocabulary may be inappropriate but the meaning is clear; generally able to paraphrase successfully. In this practice test: precision is often lacking, with wrong word choice, unnatural collocations or vague words in many answers; less common words are few and not always right; Part 3 topics are handled with general words.'),
        ('LR', 7, 'Uses vocabulary flexibly to discuss a variety of topics, including abstract ones; shows some ability to use less common and idiomatic items and an awareness of style and collocation, though inappropriacies occur; paraphrases effectively as required. In this practice test: several less common or idiomatic items and collocations can be quoted; wrong word choice is occasional, not frequent.'),
        ('LR', 8, 'Wide resource, readily and flexibly used to discuss all topics and to convey precise meaning; skilful use of less common and idiomatic items despite occasional inaccuracies in word choice and collocation; effective use of paraphrase as required. In this practice test: wrong word choice is rare, and idiomatic language and collocation are natural with only occasional slips.'),
        ('LR', 9, 'Total flexibility and precise use in all contexts; sustained use of accurate and idiomatic language.'),

        ('GRA', 0, 'Does not attend or does not complete the test: nothing to assess.'),
        ('GRA', 1, 'No rateable language unless memorised.'),
        ('GRA', 2, 'No evidence of basic sentence forms; speech is mainly isolated words or memorised chunks.'),
        ('GRA', 3, 'Basic sentence forms are attempted, but grammatical errors are numerous except in apparently memorised utterances. In this practice test: errors are very frequent, meaning is often lost, and subordinate clauses are almost absent.'),
        ('GRA', 4, 'Produces basic sentence forms, and some short utterances are error-free; subordinate clauses are rare and, overall, turns are short, structures are repetitive and errors are frequent. In this practice test: fewer than about one in four sentences is error-free, subordinate clauses are usually wrong, and errors often cause misunderstanding.'),
        ('GRA', 5, 'Basic sentence forms are fairly well controlled for accuracy; complex structures are attempted, but they are limited in range, nearly always contain errors and may lead to the need for reformulation. In this practice test: fewer than about half of the sentences are error-free; the same errors (tense, subject-verb agreement, missing plural -s or past -ed, articles, missing verbs or subjects) recur in most answers; errors sometimes make the listener work to understand.'),
        ('GRA', 6, 'Produces a mix of short and complex sentence forms and a variety of structures with limited flexibility; errors frequently occur in complex structures, but these rarely impede communication. In this practice test: roughly half to two thirds of the sentences are error-free; basic errors (tense, agreement, articles) still appear in many answers; fewer than two kinds of complex structure are used with control.'),
        ('GRA', 7, 'Uses a range of structures flexibly; error-free sentences are frequent; both simple and complex sentences are used effectively despite some errors, and a few basic errors persist. In this practice test: about two thirds to three quarters of the sentences are error-free, several kinds of complex structure (relative clauses, conditionals, passives, comparisons, noun clauses) are used with some flexibility, and no error causes difficulty for the listener.'),
        ('GRA', 8, 'Uses a wide range of structures flexibly; the majority of sentences are error-free; occasional inappropriacies and non-systematic errors occur, and a few basic errors may persist. In this practice test: about nine in ten sentences are error-free and basic errors are essentially absent.'),
        ('GRA', 9, 'Structures are precise and accurate at all times, apart from mistakes characteristic of native speaker speech.'),

        ('PR', 0, 'Does not attend or does not complete the test: nothing to assess.'),
        ('PR', 1, 'Can produce occasional individual words and phonemes that are recognisable, but no overall meaning is conveyed; unintelligible.'),
        ('PR', 2, 'Uses few acceptable phonological features; overall problems with delivery impair attempts at connected speech; individual words and phonemes are mainly mispronounced and little meaning is conveyed; often unintelligible.'),
        ('PR', 3, 'Displays some features of band 2 and some, but not all, of the positive features of band 4.'),
        ('PR', 4, 'Uses some acceptable phonological features, but the range is limited; some acceptable chunking, but frequent lapses in overall rhythm; attempts to use intonation and stress, but control is limited; individual words or phonemes are frequently mispronounced, causing lack of clarity; understanding requires some effort and there may be patches of speech that cannot be understood.'),
        ('PR', 5, 'Displays all the positive features of band 4 and some, but not all, of the positive features of band 6.'),
        ('PR', 6, 'Uses a range of phonological features, but control is variable; chunking is generally appropriate, but rhythm may be affected by a lack of stress-timing and/or a rapid speech rate; some effective use of intonation and stress, but this is not sustained; individual words or phonemes may be mispronounced, but this causes only occasional lack of clarity; can generally be understood throughout without much effort.'),
        ('PR', 7, 'Displays all the positive features of band 6 and some, but not all, of the positive features of band 8.'),
        ('PR', 8, 'Uses a wide range of phonological features to convey precise and/or subtle meaning; can sustain appropriate rhythm; flexible use of stress and intonation across long utterances, despite occasional lapses; can be easily understood throughout; accent has minimal effect on intelligibility.'),
        ('PR', 9, 'Uses a full range of phonological features to convey precise and/or subtle meaning; flexible use of features of connected speech is sustained throughout; can be effortlessly understood throughout; accent has no effect on intelligibility.')
)
INSERT INTO rubric_band_descriptors (criterion_id, band, descriptor)
SELECT inserted.id, bands.band, bands.descriptor
FROM inserted
JOIN bands ON bands.code = inserted.code;

DO $$
BEGIN
    IF (SELECT count(*) FROM rubrics WHERE task = 'SPEAKING' AND status = 'ACTIVE') <> 1 THEN
        RAISE EXCEPTION 'Exactly one Speaking rubric must be active';
    END IF;
    IF EXISTS (
        SELECT 1 FROM rubric_criteria c JOIN rubrics r ON r.id = c.rubric_id
        WHERE r.task = 'SPEAKING' AND r.status = 'ACTIVE'
          AND ((SELECT count(*) FROM rubric_band_descriptors d WHERE d.criterion_id = c.id) <> 10
               OR (SELECT count(*) FROM rubric_band_descriptors d
                    WHERE d.criterion_id = c.id AND d.band = floor(d.band) AND d.band BETWEEN 0 AND 9) <> 10)
    ) THEN
        RAISE EXCEPTION 'Every Speaking criterion needs exactly one descriptor for each whole band 0 to 9 and no half band';
    END IF;
    IF EXISTS (
        SELECT 1 FROM rubrics
        WHERE task = 'SPEAKING' AND status = 'ACTIVE'
          AND (system_prompt NOT LIKE '%never 5.5 or 6.5%' OR system_prompt LIKE '%half%' OR length(system_prompt) > 4000)
    ) THEN
        RAISE EXCEPTION 'The Speaking system prompt must ask for whole bands, must not mention half bands and must stay within 4000 characters';
    END IF;
    IF EXISTS (
        SELECT 1 FROM rubric_criteria c JOIN rubrics r ON r.id = c.rubric_id
        WHERE r.task = 'SPEAKING' AND r.status = 'ACTIVE' AND c.code = 'FC'
          AND (c.description LIKE '%4.5%' OR c.description LIKE '%5.5%'
               OR c.description NOT LIKE '%limit FC to band 4%' OR c.description NOT LIKE '%limit it to band 5%')
    ) THEN
        RAISE EXCEPTION 'The Fluency and coherence ceilings must be written as whole bands';
    END IF;
END $$;
