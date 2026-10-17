# Learning staff area v1

The Learning side of the admin area: learner overview, anomaly flags, work queues and statistics for content authors. The
Assessment and Access side (examiner quality review, paid human grading) is in [admin-area-v1.md](admin-area-v1.md).
Learning keeps ids only; names of courses, knowledge points and users come from Content, User and Access, and the
frontend joins them.

Roles: `ADMIN` sees everything. `EXAMINER` works the `EXAMINER` queue, `CONTENT_AUTHOR` (R&D) the `CONTENT_AUTHOR` queue.

## `GET /api/learning/admin/learners/{userId}/overview`

Roles `ADMIN`, `EXAMINER`. Read-only; it never refreshes the learner's data.

```json
{
  "userId": "…",
  "courses": [{ "courseId": "…", "topics": 8, "topicsDone": 5, "passedAt": null, "status": "REMEDIATION",
                "total": 0.62, "passThreshold": 0.7 }],
  "knowledgePointsByTier": { "PRIORITY": 3, "CONSOLIDATE": 5, "MAINTAIN": 12 },
  "pendingReviews": [{ "reviewId": "…", "knowledgePointId": "…", "blocking": true, "triggerKind": null }],
  "diagnoses": [{ "sessionId": "…", "rootKnowledgePointId": "…", "status": "PROBING", "probes": 2 }],
  "remediation": [{ "courseId": "…", "topicId": null, "kind": "POST_COURSE", "open": true, "createdAt": "…" }],
  "flags": [{ "id": "…", "kind": "BURST_SUBMISSIONS", "status": "OPEN", "evidence": "{…}" }],
  "openWork": []
}
```

`status` and `total` are the course score of [course-score-v1.md](course-score-v1.md). The full per-knowledge-point report
is `/api/learning/admin/learners/{userId}/report`.

## Anomaly flags

Checked for a learner right after their question events commit, in a separate transaction (a failure never affects the
submission). A kind already flagged for the learner in the last 24 hours is not raised again.

| Kind | Rule (defaults, `learning.flags.*`) |
| --- | --- |
| `TOO_FAST_HIGH_SCORE` | In one day, at least 10 graded answers with a duration, median duration under 3000 ms and accuracy at least 90%. |
| `BURST_SUBMISSIONS` | More than 30 events in 10 minutes. |
| `DUPLICATE_ESSAY` | A lesson essay whose normalised text (whitespace removed, lower case) has the SHA-256 of an essay of another learner. |

`PATCH /api/learning/admin/flags/{id}` (`ADMIN`, `EXAMINER`): `{ "status": "CONFIRMED" | "DISMISSED", "note": "…" }`;
records the reviewer. `422 INVALID_FLAG_STATUS` for `OPEN`, `404` when unknown.

## Work queues

`GET /api/learning/admin/work-items?queue=&status=`: the queues the caller may see, optionally one queue
(`403 QUEUE_NOT_ALLOWED` for another) and one status (`OPEN`, `IN_PROGRESS`, `DONE`). Listing first adds items for new
work, and a scheduled round does the same every 3 hours (`learning.work-items.sync-interval`, first run after
`sync-initial-delay`, off with `sync-enabled=false`); only one service instance runs a round at a time. Each source becomes
at most one item (`UNIQUE (kind, subject)`) and finished work is never reopened.

| Kind | Queue | Source |
| --- | --- | --- |
| `MID_COURSE_REMEDIATION`, `POST_COURSE_REMEDIATION` | EXAMINER | remediation flags with reviews still pending |
| `LEARNER_FLAG` | EXAMINER | open learner flags |
| `CONTENT_GAP` | CONTENT_AUTHOR | knowledge points whose reviews ran out of unseen sets |
| `QUESTION_TOO_EASY`, `QUESTION_TOO_HARD` | CONTENT_AUTHOR | questions with enough first submissions and a correct rate above / below the limits |
| `EDGE_SUSPECT` | CONTENT_AUTHOR | prerequisite edges that do not explain failures (below) |

`PATCH /api/learning/admin/work-items/{id}`: `{ "status", "action", "result", "note", "noteVisibleToLearner" }`, all
optional. Taking an item sets the assignee; `OPEN` clears it. A note shown to the learner needs text and an item about a
learner (`422 NOTE_REQUIRED`, `422 NO_LEARNER`). `404` when the item is not in a queue the caller may use.

`GET /api/learning/notes` (`CUSTOMER`): the notes staff chose to show the caller, newest first.

## Statistics for R&D (`ADMIN`, `CONTENT_AUTHOR`)

- `GET /api/learning/admin/question-stats?minAttempts=20`: per question with at least `minAttempts` first submissions:
  attempts, correct, correct rate, median duration, most frequent error codes. Questions are not compared with their
  tagged difficulty in this version.
- `GET /api/learning/admin/tree-quality?courseId=`: per prerequisite edge A→B over concluded diagnoses (probes of A made
  while B was failing, how many passed, how often A was the gap) and per knowledge point (learners with a profile, mean
  mastery, times it was the gap, probes and failed probes). With `courseId` only points of that course.

An edge is suspect, and becomes an `EDGE_SUSPECT` item with these numbers, when it has at least 20 probes and A is passed
at least 80% of the time (a firm base yet B still fails) or A is the gap in under 10% of them. Thresholds:
`learning.tree-quality.{min-sessions, pass-rate-at-least, root-rate-below, min-attempts, too-easy-above,
too-hard-below}`.
