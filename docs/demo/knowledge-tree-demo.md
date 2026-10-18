# Knowledge tree and assessment journey: demo script

Eight steps, each through the Gateway (Swagger or curl). Roles: `CONTENT_AUTHOR` (R&D), `CUSTOMER` (learner), `EXAMINER`
(Mentor), `ADMIN`. Start from a clean stack: `docker compose down -v && docker compose up -d --build`. Demo accounts exist
when `DEMO_DATA_ENABLED=true`. Every request carries the Gateway JWT of the role named in the step. Request bodies are in
the linked contracts.

Preparation: a learner with the Reading course open, a lesson with a Practice set per knowledge point (KP), a topic test
and a course test, with enough unseen Practice sets per KP for steps 2 to 5 (the seed content of the stack covers a
subset; add sets through the Content API when a step answers `NO_UNREVEALED_SET`).

## 1. R&D views and edits the tree, and a cycle is refused

`CONTENT_AUTHOR`. Contract: [knowledge-tree-v1](../contracts/knowledge-tree-v1.md).

1. `GET /api/content/knowledge-tree` — the tree of active KPs with their prerequisites.
2. `PUT /api/content/knowledge-points/{B}/prerequisites` with `[A]` — saved; `GET .../{B}/prerequisites` shows A.
3. `PUT /api/content/knowledge-points/{A}/prerequisites` with `[B]` — refused (cycle); the tree is unchanged.

## 2. A wrong Practice leads to a review, a diagnosis and a review of the root KP

`CUSTOMER`. Contracts: [lesson-learning-v1](../contracts/lesson-learning-v1.md), [diagnostics-v1](../contracts/diagnostics-v1.md).

1. `POST /api/learning/lessons/{id}/practice-attempts`, then `POST /practice-attempts/{id}/submissions` with wrong answers
   for KP B — a blocking review of B is created.
2. `GET /api/learning/reviews/{reviewId}` — a set of B; submit it wrong (`POST .../submissions`), do the theory check, get
   the next set and fail it too (`failedSets = 2`).
3. The review now shows `state = DIAGNOSING` and a `diagnosticSessionId`. `GET /api/learning/diagnostics/{id}` lists the
   probes (A, X, ...); `GET .../probes/{probeId}` gives the questions; `POST .../probes/{probeId}/submissions` answers
   them. Fail A's probe.
4. The session concludes with A as the gap: `GET /api/learning/diagnostics/{id}` shows `conclusion = [A]`; a blocking review
   of A (`trigger_kind = DIAGNOSTIC`) exists and B's review reads `WAITING_ON_PREREQUISITES`.
5. Pass A's review; B's review gives a new set again.

## 3. The report by knowledge point

`CUSTOMER`. Contract: [learner-report-v1](../contracts/learner-report-v1.md).

`GET /api/learning/report` — per KP: mastery and tier, common errors, the five latest answers with time and error code, and
the next action (`REVIEW_REQUIRED`, `DIAGNOSING`, `CONSOLIDATE`, `MAINTAIN_DUE`). Then `GET /api/learning/review-basket` and
`GET /api/learning/suggestions` for optional work.

## 4. A topic test of 50% opens the next topic and raises a task for the Examiner

`CUSTOMER`, then `EXAMINER`. Contract: [course-score-v1](../contracts/course-score-v1.md).

1. `POST /api/learning/topics/{id}/test-assignments`, take the test through Assessment and score 50%.
2. `GET /api/learning/topics` — the topic is `COMPLETED`, the next one `IN_PROGRESS`.
3. `EXAMINER`: `GET /api/learning/admin/work-items?queue=EXAMINER` — a `MID_COURSE_REMEDIATION` item; take it with
   `PATCH /api/learning/admin/work-items/{id}` (`status: IN_PROGRESS`, `action: "..."`), later `DONE` with a `result` and,
   if wanted, a note shown to the learner. The learner reads it at `GET /api/learning/notes`.

## 5. Course test, examiner grading, total score, remediation, retest, pass

`CUSTOMER`, `EXAMINER`. Contracts: [course-score-v1](../contracts/course-score-v1.md), [assessment-grading-v1](../contracts/assessment-grading-v1.md).

1. Take every topic test, then `POST /api/learning/courses/{id}/test-assignments` — allowed whatever the scores were.
2. Take the course test; its essay waits in the Examiner queue, the Examiner grades it blind.
3. `GET /api/learning/courses/{id}/score` — components, weights, total. Below 70%: `status = REMEDIATION`, error codes of
   the test, a `POST_COURSE` flag; assigning the test again answers `409 REMEDIATION_REQUIRED`.
4. Finish the reviews: `status = RETEST_READY`. `POST .../test-assignments` gives another code; take it and score well.
5. `GET .../score` — `status = PASSED`, the total of the retest.

## 6. Admin changes the weights

`ADMIN`. `GET /api/learning/admin/score-config`, then `PUT` with other weights (they add up to 1). An undecided course's
total changes at the next `GET .../score`; the course passed in step 5 keeps its total and weights.

## 7. AI evaluation and its metrics

`ADMIN`. Contract: [assessment-grading-v1](../contracts/assessment-grading-v1.md), policy: [ai-policy](../ai-policy.md).

1. `POST /api/assessments/admin/ai-gold-essays` (or `.../import-comparisons`) to add gold essays with an examiner band.
2. `POST /api/assessments/admin/ai-evaluations` with `{"task":"TASK_2"}` — `202 {id}`; `GET .../{id}` shows the metrics and
   per-essay comparison.
3. `POST /api/assessments/admin/ai-prompt-activations` with `{runId}` — `204` for a passed run, `409 EVALUATION_NOT_PASSED`
   otherwise. Without an activation the seeded `ACTIVE` rubric keeps essay grading working.

## 8. R&D re-grades a sample to evaluate the Mentor

`CONTENT_AUTHOR`, `ADMIN`. Contract: [admin-area-v1](../contracts/admin-area-v1.md).

1. `GET /api/assessments/qa-reviews/queue` — sampled essays (rate `assessment.qa.sample-rate`, default 10%) without the
   Examiner's band; claim one with `POST .../{jobId}/claim`, read `GET .../{jobId}/essay`.
2. `POST /api/assessments/qa-reviews` with the blind band.
3. `ADMIN`: `GET /api/assessments/admin/examiner-quality` — per Examiner, the number re-graded, the mean difference and the
   share differing by more than one band.

Also worth showing: `GET /api/learning/admin/learners/{userId}/overview`, `GET /api/learning/admin/tree-quality`, a learner's
`POST /api/learning/writing-submissions/{id}/revisions` ([essay-revision-v1](../contracts/essay-revision-v1.md)) and
`POST /api/learning/writing-submissions/{id}/human-review` ([human-review-v1](../contracts/human-review-v1.md)).
