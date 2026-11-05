# Error-trap seed scope

Measured against the complete Flyway schema through V79 using Testcontainers PostgreSQL 15 on 2026-10-24, before authoring the seed migration. The in-scope set uses the phase definition: active error type with an active trap point and active parent, except the four specified exclusions, and at least one published `CHOICE` question mapped to the parent knowledge point.

## In-scope codes and current coverage

An eligible set is a published practice set with at least three questions, at least one question mapped to the parent point, and at least two questions carrying the error code as a distractor.

| Error code | Parent knowledge point | Existing eligible sets | Additional sets needed |
|---|---|---:|---:|
| `DETAIL_AS_MAIN_IDEA` | `DR_IDEA_OR_DETAIL` | 0 | 2 |
| `DISTRACTOR_CORRECTED` | `L65_CORRECTION` | 0 | 2 |
| `INFERENCE_UNSUPPORTED` | `R65_EVIDENCE_INFERENCE` | 1 | 1 |
| `KEYWORD_MATCH_ONLY` | `TR_R_SCANNING` | 0 | 2 |
| `LINKER_MISUSE` | `DEMO_WRITING_LINKING` | 0 | 2 |
| `PARAPHRASE_TRAP` | `R65_PARAPHRASE` | 0 | 2 |
| `TFNG_FALSE_AS_NG` | `TFNG_FALSE_VS_NOT_GIVEN` | 0 | 2 |
| `TFNG_NG_AS_FALSE` | `TFNG_FALSE_VS_NOT_GIVEN` | 0 | 2 |
| `WRONG_PARAGRAPH` | `PR_MATCHING_INFORMATION` | 0 | 2 |
| `WRONG_SPEAKER` | `L65_SPEAKER_PURPOSE` | 0 | 2 |

Ten error codes require new practice data (within the stop threshold of 15). Nineteen sets are needed in total: two per code except `INFERENCE_UNSUPPORTED`, which needs one. The two T/F/NG codes independently require two sets each with at least two matching questions.

## Out-of-scope codes and reasons

- Explicit exclusions: `WORD_LIMIT_EXCEEDED`, `NUMBER_FORMAT`, `PLURAL_FORM`, `SPELLING`.
- Active parent codes without a qualifying published `CHOICE` question mapped to that parent: `COLLOCATION`, `SCOPE_OVERGENERALISED`, `T1_DATA_MISREPORTED`, `T1_NO_OVERVIEW`, `UNDERDEVELOPED_IDEA`, `WORD_CHOICE`. These are essay/other-skill points under the current published content and do not meet the phase scope query.
- Active codes without a parent knowledge point (and therefore without an active trap child): `ARTICLE`, `CONTRADICTED_BY_PASSAGE`, `NO_CLEAR_POSITION`, `OFF_TOPIC`, `SENTENCE_FRAGMENT`, `SUBJECT_VERB_AGREEMENT`, `TENSE`, `WRONG_TIME_FRAME`.

## Evidence and before counts

- `ErrorTrapCoverageMigrationTest` dynamically selected exactly the ten in-scope codes above. Its red run reported nine codes with zero eligible sets and `INFERENCE_UNSUPPORTED` with one eligible set.
- The separate T/F/NG assertion found zero sets with at least two `TFNG_NG_AS_FALSE` questions; the scope inventory independently found zero qualifying sets for both T/F/NG codes.
- The phase-1 baseline inventory recorded 28 active error codes, 19 with parent points and 9 without. The V79 scope query narrows the parent-point codes to the ten listed above.
- The initial phase-9 coverage test failed only because the required trap practice coverage is absent; the distractor option/active-code and no-child-assignment checks were also run by that test class.

## After counts

Verified by `ErrorTrapCoverageMigrationTest` against V80 in Testcontainers after seeding:

| Error code | Eligible sets after seed | Matching trap questions per new set | Result |
|---|---:|---:|---|
| `DETAIL_AS_MAIN_IDEA` | 2 | 2 | Meets threshold |
| `DISTRACTOR_CORRECTED` | 2 | 2 | Meets threshold |
| `INFERENCE_UNSUPPORTED` | 2 (one existing, one new) | 2 in the new set | Meets threshold |
| `KEYWORD_MATCH_ONLY` | 2 | 2 | Meets threshold |
| `LINKER_MISUSE` | 2 | 2 | Meets threshold |
| `PARAPHRASE_TRAP` | 2 | 2 | Meets threshold |
| `TFNG_FALSE_AS_NG` | 2 | 2 actual `TRUE_FALSE_NOT_GIVEN` questions with correct answer `FALSE` | Meets separate T/F/NG threshold |
| `TFNG_NG_AS_FALSE` | 2 | 2 actual `TRUE_FALSE_NOT_GIVEN` questions with correct answer `NOT_GIVEN` | Meets separate T/F/NG threshold |
| `WRONG_PARAGRAPH` | 2 | 2 | Meets threshold |
| `WRONG_SPEAKER` | 2 | 2 | Meets threshold |

The same test verifies every published distractor key exists among its question’s options, every distractor code is active, and no question is mapped to a trap child point.

## Data-quality follow-up

Quality verification against V80 in Testcontainers passed after the acceptance corrections. The migration contains 76 distinct question rows across 19 sets. Each of the seven codes with two new sets has eight unique stems across those sets; the single new `INFERENCE_UNSUPPORTED` set has four stems not reused from its pre-existing eligible set. Multiple-choice keys are distributed across A/B/C without any key exceeding half of a qualifying set, and trap tags vary across wrong-option keys within each multiple-choice set.

Each TFNG set contains four `TRUE_FALSE_NOT_GIVEN` questions and at least one untagged `TRUE`. In the two `TFNG_NG_AS_FALSE` sets, two questions each have correct `NOT_GIVEN` and tag `FALSE`; in the two `TFNG_FALSE_AS_NG` sets, two questions each have correct `FALSE` and tag `NOT_GIVEN`.

The added migration coverage checks for duplicate stems across same-code trap sets and answer-key imbalance. The TFNG check verifies set composition and untagged TRUE. Full content-service Testcontainers run passed with 957 tests, 0 failures, 0 errors, and 1 skipped (168 suites); the stale report file for the already absent `ErrorTrapBaselineDataTest` was excluded.
