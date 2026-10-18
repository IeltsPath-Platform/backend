# Frontend handoff: knowledge tree and assessment journey

What each phase adds for the frontend. Contracts are in `docs/contracts/`; routes are the Gateway paths.

| Area | Screens and changes | Contract |
| --- | --- | --- |
| Knowledge tree (R&D) | Tree view and prerequisite editor with cycle refusal; KP activate/deactivate | `knowledge-tree-v1` |
| Error taxonomy | Error code catalogue (status, review status) used by grading and reports | `learning-content-internal-v1`, `answer-spec-error-tags-v1-vectors.json` |
| Lesson flow | Next lesson opens only after the earlier lessons' Practice is passed (`403 PRACTICE_REQUIRED` + `lessonIds`); a review of an earlier lesson blocks later ones | `lesson-learning-v1` |
| Time per question | Send optional `durationMs` per answer | `lesson-learning-v1`, `assessment-completed-v2` |
| Learner report | Per-KP score colour by tier, common errors, latest answers, next action (`REVIEW_REQUIRED`, `DIAGNOSING`, `CONSOLIDATE`, `MAINTAIN_DUE`) | `learner-report-v1` |
| Review basket | Required reviews, KPs to consolidate and due ones, start an optional review; review responses carry `blocking` | `lesson-learning-v1` |
| Suggestions | Up to five optional items with `reason` and `openPath` (null for a practice test) | `lesson-learning-v1` |
| Diagnosis | Probes to answer, result matrix, conclusion; the review shows `state` (`DIAGNOSING`, `WAITING_ON_PREREQUISITES`) and `waitingOnReviewIds` | `diagnostics-v1` |
| Practice tests | Milestone practice tests per course (list, assign) | `lesson-learning-v1` |
| Topic and course | Topic status `COMPLETED` or `PASSED`; course test opens when every topic test is taken; course score page (components, weights, total, threshold, error codes, remediation, retest) | `course-score-v1` |
| Admin | Mastery rule screen (thresholds, review schedule, recompute); score configuration screen (weights, threshold, per course) | `learner-report-v1`, `course-score-v1` |
| Essays | "Sửa và nộp lại" and version comparison with band chart; "Nhờ Mentor chữa" and the Mentor's feedback (`PENDING`/`DONE`, credits left from Access) | `essay-revision-v1`, `human-review-v1` |
| Staff area | Learner overview; flags (confirm/dismiss); work queues per role with action/result/note; question and tree quality tables; notes shown to the learner | `learning-staff-area-v1` |
| Examiner and QA | Examiner queue with course essays and human reviews; R&D blind re-review queue; Examiner quality table; essay AI evaluation screens | `assessment-grading-v1`, `admin-area-v1` |

## Rules the frontend must respect

- A grade, solution or transcript appears only when the response contains it; never infer it.
- A blocking review stops the lesson, Practice and topic test; an optional one never does.
- A passed course shows the total and weights it was decided with; an undecided course follows the current configuration.
- Names of courses, KPs and users are not in Learning responses (ids only): join with Content, User and Access.
- Mentor reviews and AI/automatic deltas are feedback; the automatic grade of a revision carries grader noise, label it.

## Error codes to handle

`PRACTICE_REQUIRED`, `REVIEW_REQUIRED`, `TOPIC_LOCKED`, `LESSON_LOCKED`, `COURSE_TEST_LOCKED`, `REMEDIATION_REQUIRED`,
`COURSE_ALREADY_PASSED`, `NO_UNREVEALED_SET`, `NOT_IN_REVIEW_BASKET`, `PROBE_CLOSED`, `REVISION_LIMIT`,
`REVISION_NOT_LATEST`, `NOT_REVISABLE`, `HUMAN_GRADING_CREDIT_REQUIRED`, `NOT_REVIEWABLE`, `INVALID_SCORE_CONFIG`,
`QUEUE_NOT_ALLOWED`.
