# Acceptance run

Date: 2026-10-17

Branch: `feat/knowledge-tree-assessment-journey`

## Contract audit

Compared the contracts against the current controllers and service behavior:

| Contract | Result |
| --- | --- |
| `assessment-completed-v2.md` | Item `error_code` and per-KP `error_type` scopes are documented separately; the first error-analysis code may appear in both. |
| `answer-spec-v1.md` and shared vectors | Request/response examples and shared answer vectors match objective grading and error-tagging inputs. |
| `assessment-grading-v1.md` | Queue fields and source/reason semantics match examiner and AI-evaluation flows; rubric snapshots apply to standalone human reviews. |
| `learning-content-internal-v1.md` | Internal routes, suggestion candidate fields, and the new question-difficulty batch request/response match Content controllers. |
| `admin-area-v1.md` | QA queue fields and Access consume/refund response and error behavior match the controllers. |
| `human-review-v1.md` | Request, idempotency, pending/done response, and blind-review fields match the internal Assessment endpoints. |
| Access contracts | No separate Access contract file exists; the human-grading credit API is covered by `admin-area-v1.md`. |

## Database reference

Added the current Assessment, Content, and Access table definitions to `.sdd/database/DATABASE_V5.md`. The migration scan covers 25 Assessment tables, 27 Content tables, and 10 Access tables.

## Reactor verification

Command: `mvn -B -ntp verify`

Result: **BUILD FAILURE** at `library-service` `testCompile`; the reactor did not complete.

| Module completed in this run | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| common-security | 8 | 0 | 0 | 0 |
| api-gateway | 9 | 0 | 0 | 0 |
| config-server | 1 | 0 | 0 | 0 |
| eureka-server | 1 | 0 | 0 | 0 |
| user-service | 106 | 0 | 0 | 0 |
| access-service | 49 | 0 | 0 | 0 |
| content-service | 396 | 0 | 0 | 0 |
| **Total before failure** | **570** | **0** | **0** | **0** |

Testcontainers started successfully in the modules that ran, including Content; no completed module reported skipped tests. Content applied/validated Flyway through V39. The reactor stopped while compiling Library test sources, before Library tests and before Learning, Assessment, Game, Notification, or Community ran. Representative unresolved Library test references include `library.domain.vo`, `library.infrastructure.adapter`, `library.infrastructure.persistence.mapper`, `NoteRepository`, and `SavePracticeQuestionFlashcardUseCase`. Learning source was not changed. Existing older Learning/Assessment Surefire reports were not counted as results from this reactor run.

## Compose and demo

`docker compose down -v` completed. The first `up -d --build` attempt exposed an ambiguous `NamedParameterJdbcTemplate.query` overload in the new Content reader; the lambda was made an explicit block and the requested build was rerun. `docker compose up -d --build` then completed successfully, and all Compose containers reported healthy.

The demo was run through Gateway with the seeded accounts. Results below are the responses observed on this clean stack.

| Demo step | Result |
| --- | --- |
| 1. Tree edit and cycle refusal | `GET /api/content/knowledge-tree` returned 200. `PUT /api/content/knowledge-points/{B}/prerequisites` with `{"prerequisiteIds":[A]}` returned 200 and the GET showed A. The attempted cycle on A returned 422 and left the tree unchanged. The demo prose shows a raw `[A]` body; the controller requires the object wrapper. |
| 2. Practice review and diagnosis | Completed the prerequisite lessons and Practice, then failed the B set. The review entered `THEORY`; after two failed review sets, Learning created a diagnostic session. Failing A's probe concluded A, and B became `WAITING_ON_PREREQUISITES`. Passing A's review did not provide another B set: the remaining unrevealed sets were exhausted, so B became `SKIPPED`; L3 Practice then read `PASSED` with `REVIEW_FINISHED`. This differs from the script's expected new B set. The documented setup says to add unseen sets, but the Content HTTP controllers currently expose no section-question association operation for creating a populated set through that API. |
| 3. Learner report, basket, suggestions | All three GETs returned 200. The report showed B as `DIAGNOSING`, the basket showed its required review, and suggestions returned the next lesson (L4). |
| 4. Topic test and Examiner work | Completed the four lessons/Practice sets, assigned and submitted the topic test at 50% (2/4). The learner's topic became `COMPLETED` and the next topic became `IN_PROGRESS`. Course score recorded `MID_COURSE` with `open:false`; the open Examiner queue had a `LEARNER_FLAG` (`BURST_SUBMISSIONS`) but no `MID_COURSE_REMEDIATION` item. Thus the queue/claim/DONE/note portion did not run. |
| 5. Course test, essay grading, remediation, retest | The course had 1 of 7 topics passed and `testStatus=LOCKED`; score was `IN_PROGRESS`, total 0.121. Course-test assignment returned 422 `{"detail":"Invalid request"}` both with an empty JSON object and with no request body, rather than the documented `COURSE_TEST_LOCKED`. No course essay or human grading/retest could be performed. |
| 6. Score configuration | Admin GET/PUT succeeded. Changing weights moved the undecided course total from 0.121 to 0.1521; restoring defaults returned it to 0.121. No passed course was available to check score immutability. |
| 7. Gold essay evaluation and activation | Manual gold creation succeeded; evaluation returned 202 and completed with `n=1`, within-half-band 0.0, MAE 1.0, QWK 0.0, `passed:false`. The activation attempt returned 500 `Internal Server Error`; the contract expects 409 `EVALUATION_NOT_PASSED`. The single manually entered demo sample is not an expert-rated acceptance gold set. |
| 8. QA re-grade and Examiner quality | QA queue returned 200 with `totalElements=0`; examiner-quality returned an empty result. There was no sample to claim or re-grade. |

## Acceptance status

Not accepted yet. Content and the database reference changes are in place, and 570 tests passed with zero skips before the reactor stopped. The full reactor remains red because Library test compilation fails; Learning and Assessment were not reached in this run. Demo steps 4, 5, 7, and 8 have the gaps recorded above, and step 2 exhausted its seeded unseen sets.
