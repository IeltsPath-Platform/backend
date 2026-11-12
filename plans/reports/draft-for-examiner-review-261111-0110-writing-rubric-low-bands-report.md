# Writing rubric: draft descriptors for bands below 4.0

Status: DRAFT, not reviewed by an IELTS examiner, not in the database.

## Why

Active Writing rubrics (`IELTS_WRITING_TASK_1` v1, `IELTS_WRITING_TASK_2` v1) have 11 descriptors per criterion, bands
4.0 to 9.0. The grader prompt only shows these. A very weak essay has no band below 4.0 to match, so the model returns
4.0 even when its own comment says the language is "very limited with serious errors". Example: h3's Task 1 free writing
scored 4.0 on all four criteria.

Code accepts 0 to 9 in 0.5 steps (`WritingScore.isBand`, prompt template `<0-9 in steps of 0.5>`), so no code change is
needed. Only rubric data.

## Style

Existing text = one band sentence + one criterion tail. The tails below are copied unchanged from the active rubric so
the new rows read the same as the old ones. Band sentences are new, written per criterion.

Bands drafted: 3.5, 3.0, 2.5, 2.0, 1.5, 1.0, 0. Count per criterion goes from 11 to 18.

## Tails (unchanged from the active rubric)

| Criterion | Tail |
| --- | --- |
| TA (Task 1) | For this visual report, select the most important data, describe the overall pattern and make accurate comparisons. |
| TR (Task 2) | For this essay, answer every part of the question, present a clear position and extend relevant reasons or examples. |
| CC | Order ideas so the progression is easy to follow and use linking and reference without obscuring meaning. |
| LR | Choose vocabulary that is sufficiently varied, accurate and natural for the ideas being expressed. |
| GRA | Use a suitable range of sentence forms and maintain control of grammar, punctuation and sentence boundaries. |

Each final descriptor = band sentence + one space + tail.

## Band sentences

### TA, Task Achievement (Task 1)

| Band | Band sentence |
| --- | --- |
| 3.5 | Performance is very limited: only a few figures are reported, several are wrong or irrelevant, and no clear overall pattern is given. |
| 3.0 | Performance is poor: the visual is barely addressed, most key figures are missing or misreported, and there is no overview. |
| 2.5 | Performance is very poor: only isolated, mostly inaccurate references to the visual appear and they are hardly connected to the task. |
| 2.0 | Performance is minimal: the response is mostly unrelated to the visual, or is a few inaccurate fragments about it. |
| 1.5 | Performance is almost absent: at most one or two disconnected references to the visual can be found. |
| 1.0 | The response is irrelevant to the task, apart from at most a word or two that touch the visual. |
| 0 | No assessable response: not attempted, memorised or copied text, or not in English. |

### TR, Task Response (Task 2)

| Band | Band sentence |
| --- | --- |
| 3.5 | Performance is very limited: the question is only partly addressed, no clear position is given, and ideas are few, repeated or barely supported. |
| 3.0 | Performance is poor: the question is hardly addressed, ideas are irrelevant or unclear, and there is no real support. |
| 2.5 | Performance is very poor: only isolated ideas touch the topic and they are not developed or linked to a position. |
| 2.0 | Performance is minimal: the response is mostly off topic, or a few unrelated statements. |
| 1.5 | Performance is almost absent: at most one or two phrases connect to the topic. |
| 1.0 | The response is irrelevant to the question, apart from at most a word or two that touch the topic. |
| 0 | No assessable response: not attempted, memorised or copied text, or not in English. |

### CC, Coherence and Cohesion (both tasks)

| Band | Band sentence |
| --- | --- |
| 3.5 | Performance is very limited: ideas are not clearly organised, there is little progression, and linking words are rare, repeated or misused. |
| 3.0 | Performance is poor: ideas are disconnected, paragraphing is absent or arbitrary, and the reader has to work hard to follow the message. |
| 2.5 | Performance is very poor: sentences stand alone with almost no linking, and any order of ideas is hard to see. |
| 2.0 | Performance is minimal: there is almost no organisation, and only a few words show any connection between ideas. |
| 1.5 | Performance is almost absent: no organisation can be identified beyond individual phrases. |
| 1.0 | No organisation or cohesion can be identified, because there is almost no connected text. |
| 0 | No assessable response: not attempted, memorised or copied text, or not in English. |

### LR, Lexical Resource (both tasks)

| Band | Band sentence |
| --- | --- |
| 3.5 | Performance is very limited: only basic words are used and often repeated, and frequent errors in word choice, form or spelling distort meaning. |
| 3.0 | Performance is poor: vocabulary is restricted to simple, familiar words, and errors in word choice or spelling often make the meaning unclear. |
| 2.5 | Performance is very poor: only a small stock of isolated words and fixed phrases is used, often incorrectly. |
| 2.0 | Performance is minimal: only a few words are used correctly and meaning is rarely clear. |
| 1.5 | Performance is almost absent: only isolated words can be understood. |
| 1.0 | No control of vocabulary can be shown beyond isolated words. |
| 0 | No assessable response: not attempted, memorised or copied text, or not in English. |

### GRA, Grammatical Range and Accuracy (both tasks)

| Band | Band sentence |
| --- | --- |
| 3.5 | Performance is very limited: sentences are attempted but most contain errors, complex forms are rare or wrong, and meaning is often unclear. |
| 3.0 | Performance is poor: control of sentence structure is weak, errors dominate and meaning is rarely clear. |
| 2.5 | Performance is very poor: only a few short sentences or fragments are correct, and the rest cannot be reliably understood. |
| 2.0 | Performance is minimal: sentence structure is almost absent apart from memorised phrases. |
| 1.5 | Performance is almost absent: no sentence forms can be identified beyond single phrases. |
| 1.0 | No sentence forms can be identified. |
| 0 | No assessable response: not attempted, memorised or copied text, or not in English. |

## How to apply (needs a decision from the owner)

1. Examiner review of the 35 band sentences above (5 groups x 7 bands: TA, TR, CC, LR, GRA; CC, LR and GRA are shared by both tasks).
2. Create a new rubric version for TASK_1 and TASK_2 (rubrics are versioned and the active one must not be edited),
   copy the 4.0 to 9.0 rows unchanged, add the new rows, then activate. The Writing prompt activation for each task pins
   a `code@version+tpl` string, so the new version must also be activated there or the grader keeps using v1.
3. Assessment-service reads the same active Writing rubric for TOPIC_GATE and COURSE_GATE essays, so those grades will
   also be able to go below 4.0.

## Expected effect and risks

- Weak essays can now score 3.5 or lower. Borderline essays around 4.0 may also move, because the model now has a lower
  option. Run the gold essays (`ai-gold-essays` in assessment) against the new version before activating.
- Existing graded submissions keep their stored band and `prompt_version`.
- `passBand` thresholds and the overall band formula are unchanged.
- Very short responses: the descriptors do not set a word-count rule. Under-length handling stays with the existing
  `minWords` check and the Task 1 / Task 2 criteria.

## Unresolved questions

- Who reviews the text (an IELTS examiner on the team)?
- Should the rubric keep a 0.5 scale at 1.5 and 2.5, or move to whole bands below 3.0? Official IELTS awards half bands
  only down to a point, so the owner may prefer to drop 2.5 and 1.5.
- Is the admin UI the intended way to create the new rubric version, or a Flyway migration like V50?
